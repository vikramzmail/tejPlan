/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.engine.utils;

import com.jom.JOMException;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.ErrorHandling;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/**
 * Class in charge of executing some tasks, using a dialog waiting for completion.
 * It allows to stop the execution of the task.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ThreadExecutionController
{
    private final ThreadExecutionController.IThreadExecutionHandler handler;

    /**
     * Default constructor.
     * 
     * @param handler Reference to the handler
     * @since 0.2.0
     */
    public ThreadExecutionController(ThreadExecutionController.IThreadExecutionHandler handler)
    {
	this.handler = handler;
    }

    /**
     * Executes the code into a separated thread.
     * 
     * @since 0.2.0
     */
    public void execute()
    {
	final JDialog dialog;

	if (handler instanceof JComponent)
	{
	    Container topLevel = ((JComponent) handler).getTopLevelAncestor();
	    dialog = (topLevel instanceof Frame) ? new JDialog((Frame) topLevel) : new JDialog();
	}
	else
	{
	    dialog = new JDialog();
	}

	dialog.setTitle("Executing algorithm (press stop to abort)");
	dialog.setLayout(new BorderLayout());
	dialog.setSize(new Dimension(200, 200));
	dialog.setLocationRelativeTo(null);
	dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

	((JComponent) dialog.getContentPane()).registerKeyboardAction(new ThreadExecutionController.ShowConsole(), KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

	final SwingWorker worker = new SwingWorkerCompletionWaiter(dialog, handler);

//        final ThreadExecutionController.MySwingWorkerCompletionWaiter worker2 = new ThreadExecutionController.MySwingWorkerCompletionWaiter(dialog, handler);

	JButton button = new JButton();
	button.setText("Stop");
	button.addActionListener(new ThreadExecutionController.StopExecution(worker));
	dialog.add(button, BorderLayout.SOUTH);
	dialog.pack();
        
	worker.execute();
	dialog.setVisible(true);

	if (worker.isCancelled()) return;

	Object out;
	try { out = worker.get(); }
	catch(Throwable e) { out = e; }

	if (out instanceof ExecutionException)
	{
	    Throwable cause = ((ExecutionException) out).getCause();
	    if (cause != null)
		out = cause;
	}

	if (out instanceof Throwable)
	{
	    if (out instanceof Net2PlanException)
	    {
		ErrorHandling.showErrorDialog(((Throwable) out).getMessage(), "An error happened");
	    }
	    else if (out instanceof UnsatisfiedLinkError)
	    {
		ErrorHandling.showErrorDialog(((Throwable) out).getMessage(), "Error loading dynamic library");
	    }
	    else if (out instanceof JOMException)
	    {
		ErrorHandling.showErrorDialog(((Throwable) out).getMessage(), "Error executing JOM");
	    }
	    else if (out instanceof InterruptedException)
	    {
	    }
	    else
	    {
		ErrorHandling.addErrorOrException(((Throwable) out), ThreadExecutionController.class);
		handler.executionFailed(ThreadExecutionController.this);
	    }
	}
	else
	{
	    handler.executionFinished(ThreadExecutionController.this, out);
	}
    }
    
    private Thread thread;

    /**
     * Interface for the handlers.
     * 
     * @since 0.2.0
     */
    public interface IThreadExecutionHandler
    {
	/**
         * Executes the handler and returns an object.
         * 
         * @param controller Reference to the controller
         * @return An object
         * @since 0.2.0
         */
        public Object execute(ThreadExecutionController controller);

        /**
         * Reports the end of execution.
         * 
         * @param controller Reference to the controller
         * @param out Object returned by the {@link #execute(com.net2plan.utils.ThreadExecutionController) execute} method
         * @since 0.2.0
         */
        public void executionFinished(ThreadExecutionController controller, Object out);

        /**
         * Reports the end of execution with errors.
         *
         * @param controller Reference to the controller
         * @since 0.2.0
         */
        public void executionFailed(ThreadExecutionController controller);
    }

    private class SwingWorkerCompletionWaiter extends SwingWorker implements PropertyChangeListener
    {
	private JDialog dialog;
	private ThreadExecutionController.IThreadExecutionHandler handler;

	public SwingWorkerCompletionWaiter(JDialog dialog, ThreadExecutionController.IThreadExecutionHandler handler)
	{
	    super();

	    this.dialog = dialog;
	    this.handler = handler;
	    addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event)
	{
	    if ("state".equals(event.getPropertyName()) && event.getNewValue() == SwingWorker.StateValue.DONE)
	    {
		dialog.setVisible(false);
		dialog.dispose();
	    }
	}

	@Override
	protected Object doInBackground() throws Exception
	{
            thread = Thread.currentThread();
            
            Object out;
            try { out = handler.execute(ThreadExecutionController.this); }
            catch(Throwable e) { out = e; }
            
	    return out;
	}
    }
    
    private static class ShowConsole implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ErrorHandling.showConsole();
        }
    }

    private class StopExecution implements ActionListener
    {
        private final SwingWorker worker;

        public StopExecution(SwingWorker worker)
        {
            this.worker = worker;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
//            if (System.out instanceof ThreadPrintStream)
//                ((ThreadPrintStream) System.out).setThreadOut(new NullPrintStream());
            
            worker.cancel(true);
            
            try { thread.stop(); }
            catch(Throwable ex) { }
        }
    }
}
