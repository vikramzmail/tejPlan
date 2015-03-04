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

package com.tejas.engine.internal;

import com.tejas.engine.utils.StringUtils;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

/**
 * Class handling errors within Net2Plan.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ErrorHandling
{
    public final static OutputStream stream;
    private final static JFrame consoleDialog;
    private final static JTextArea log;
    private final static String NEWLINE = StringUtils.getLineSeparator();
    
    private final static boolean DEBUG = false;

    static
    {
	stream = new OutputStream() {

	    @Override
	    public void write(int b) throws IOException
	    {
		log.append(new String(new byte[] { (byte) b }));
	    }
	};

        consoleDialog = new JFrame();
	consoleDialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
	((JComponent) consoleDialog.getContentPane()).registerKeyboardAction(new ActionListener()
		{
		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
			consoleDialog.setVisible(false);
		    }
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        consoleDialog.setSize(new Dimension(500, 200));
        consoleDialog.setLocationRelativeTo(null);
        consoleDialog.setTitle("Console (close to hide)");
	consoleDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        consoleDialog.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow][]"));

	log = new JTextArea();
	log.setFont(new JLabel().getFont());
        consoleDialog.add(new JScrollPane(log), "grow, wmin 10, wrap");

        JButton btn_reset = new JButton("Reset"); btn_reset.setToolTipText("Clear the console");
        btn_reset.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent ae)
                {
                    log.setText("");
                }
            });

        consoleDialog.add(btn_reset, "center");
    }

    public static void addErrorOrException(Throwable e)
    {
	addErrorOrException(e, new HashSet<Class>());
    }

    public static void addErrorOrException(Throwable e, final Class _class)
    {
	addErrorOrException(e, new HashSet<Class>() {{ add(_class); }});
    }

    public static void addErrorOrException(Throwable e, Set<Class> _classes)
    {
        StringBuilder text = new StringBuilder();
        text.append(e.toString()).append(NEWLINE);
        StackTraceElement[] stack = e.getStackTrace();

	Set<String> classNames = new HashSet<String>();
	Iterator<Class> it = _classes.iterator();
	while(it.hasNext()) classNames.add(it.next().getName());

        for (StackTraceElement line : stack)
        {
	    String className = line.getClassName();
	    int pos = className.indexOf("$");
	    if (pos != -1) className = className.substring(0, pos);

	    if (!DEBUG && classNames.contains(className)) break;

            text.append(line.toString()).append(NEWLINE);
        }

        addText(text.toString());
    }

    private static void addText(String text)
    {
        text = new Date().toString() + NEWLINE + NEWLINE + text;
	if (!log.getText().isEmpty()) text = NEWLINE + NEWLINE + text;
	log.append(text);
    }

    public static void showConsole()
    {
        if (!consoleDialog.isVisible())
        {
            consoleDialog.setVisible(true);
        }

	SwingUtilities.invokeLater(new Runnable() {
	private final WindowListener l = new WindowAdapter() {
	    @Override
	    public void windowDeiconified(WindowEvent e) {
	    // Window now deiconified so bring it to the front.
	    bringToFront();

	    // Remove "one-shot" WindowListener to prevent memory leak.
	    consoleDialog.removeWindowListener(this);
	    }
	};

	@Override
	public void run()
	{
	    if (consoleDialog.getExtendedState() == Frame.ICONIFIED)
	    {
		consoleDialog.addWindowListener(l);
		consoleDialog.setExtendedState(Frame.NORMAL);
	    }
	    else
	    {
		bringToFront();
	    }
	}

	private void bringToFront() {
	    consoleDialog.getGlassPane().setVisible(!consoleDialog.getGlassPane().isVisible());
	    consoleDialog.toFront();
	    // Note: Calling repaint explicitly should not be necessary.
	}
	});
    }

    public static void showErrorDialog(String title)
    {
        showErrorDialog("Please, check console for more information", title);
    }

    private static void showMessage(String message, String title, int type)
    {
	boolean wasVisible = consoleDialog.isVisible();
	if (wasVisible) consoleDialog.setVisible(false);
        JOptionPane.showMessageDialog(null, message, title, type);
	if (wasVisible) consoleDialog.setVisible(true);
    }

    public static void showErrorDialog(String message, String title)
    {
        showMessage(message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showInformationDialog(String message, String title)
    {
        showMessage(message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showMessageDialog(String message, String title)
    {
        showMessage(message, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showWarningDialog(String message, String title)
    {
        showMessage(message, title, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Returns the most internal cause of a {@code Throwable}.
     * 
     * @param e Internal {@code Throwable}
     * @return Internal cause of a {@code Throwable}
     * @since 0.2.3
     */
    public static Throwable getInternalThrowable(Throwable e)
    {
        Throwable cause = e.getCause();
        return cause == null ? e : getInternalThrowable(cause);
    }
}