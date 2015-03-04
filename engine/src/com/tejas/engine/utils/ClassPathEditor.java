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

import com.net2plan.utils.AdvancedJTable;
import com.net2plan.utils.ClassAwareTableModel;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.internal.ErrorHandling;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * <p>Class implementing the class-path editor.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ClassPathEditor
{
    private final static JFileChooser fc;
    private final static JDialog classPathEditor;
    private final static DefaultTableModel model;
    private final static AdvancedJTable table;
    private final static JButton addItem, removeSelected, removeAll;

    static
    {
	fc = new JFileChooser();
	fc.setCurrentDirectory(new java.io.File("."));
	fc.setDialogTitle("Select a JAR file");
	fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	fc.setAcceptAllFileFilterUsed(true);
	fc.setMultiSelectionEnabled(true);
	fc.setFileFilter(null);
	FileFilter filter = new FileNameExtensionFilter("JAR file", "jar");
	fc.addChoosableFileFilter(filter);

	classPathEditor = new JDialog();
        classPathEditor.setTitle("Classpath editor");
        ((JComponent) classPathEditor.getContentPane()).registerKeyboardAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) { classPathEditor.setVisible(false); }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	classPathEditor.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        classPathEditor.setSize(new Dimension(500, 300));
        classPathEditor.setLocationRelativeTo(null);
	classPathEditor.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        classPathEditor.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[][][grow][]"));

	model = new ClassAwareTableModel();
	table = new AdvancedJTable(model);

	JPanel pane = new JPanel();

	addItem = new JButton("Add file");
	addItem.addActionListener(new ActionListener()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		try
		{
		    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		    {
			for(File file : fc.getSelectedFiles())
			{
			    String path = file.getCanonicalPath();
			    Configuration.setOption("classpath", Configuration.getOption("classpath") + ";" + path);
			    addToClasspath(file);
			}

			Configuration.saveOptions();
		    }
		}
		catch (Throwable ex)
		{
		    ErrorHandling.addErrorOrException(ex);
		    ErrorHandling.showErrorDialog("Error adding classpath items");
		}
	    };
	});

	removeSelected = new JButton("Remove selected");
	removeSelected.addActionListener(new ActionListener()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		try
		{
		    int row = table.getSelectedRow();
		    if (row == -1) return;

		    row = table.convertRowIndexToModel(row);

		    String classpath = Configuration.getOption("classpath");
		    classpath = classpath.replaceFirst(Pattern.quote((String) model.getValueAt(row, 0)), "");
		    Configuration.setOption("classpath", classpath);
		    Configuration.saveOptions();
		    model.removeRow(row);

		    if (model.getRowCount() == 0) resetTable();
		}
		catch (Throwable ex)
		{
		    ErrorHandling.addErrorOrException(ex);
		    ErrorHandling.showErrorDialog("Error removing classpath items");
		}
	    }
	});

	removeAll = new JButton("Remove all");
	removeAll.addActionListener(new ActionListener()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		try
		{
		    Configuration.setOption("classpath", "");
		    Configuration.saveOptions();
		    while(model.getRowCount() > 0) model.removeRow(0);
		    resetTable();
		}
		catch (Throwable ex)
		{
		    ErrorHandling.addErrorOrException(ex);
		    ErrorHandling.showErrorDialog("Error removing classpath items");
		}
	    }
	});

	pane.add(addItem);
	pane.add(removeSelected);
	pane.add(removeAll);

	classPathEditor.add(new JLabel("<html>Algorithms and reports made by users may require external Java libraries<br />not included within Net2Plan. Use this option to include them</html>", JLabel.CENTER), "grow, wrap");
	classPathEditor.add(pane, "grow, wrap");
	classPathEditor.add(new JScrollPane(table), "grow, wrap");
	classPathEditor.add(new JLabel("<html>In the current version it is recommended to restart Net2Plan after removing libraries<br /> since they are not actually removed from memory</html", JLabel.CENTER), "grow");

	resetTable();
    }

    /**
     * Resets the classpath table.
     * 
     * @since 0.2.0
     */
    public static void resetTable()
    {
	model.setDataVector(new Object[1][], new String[] {"Path"});
	table.setEnabled(false);

	removeSelected.setEnabled(false);
	removeAll.setEnabled(false);
    }

    /**
     * Adds a given file to the classpath.
     * 
     * @param f File to be added
     * @since 0.2.0
     */
    public static void addToClasspath(File f)
    {
	try
	{
	    if (!table.isEnabled())
	    {
		model.removeRow(0);
		table.setEnabled(true);
		removeSelected.setEnabled(true);
		removeAll.setEnabled(true);
	    }

	    Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
	    addURL.setAccessible(true);
            
            URL url = f.toURI().toURL();
            
	    ClassLoader cl = ClassLoader.getSystemClassLoader();
            addURL.invoke(cl, new Object[] { url });
            model.addRow(new Object[] {f});
	}
	catch(NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
	{
	    throw new RuntimeException(e);
	}
    }

    /**
     * Shows the classpath GUI.
     * 
     * @since 0.2.0
     */
    public static void showGUI()
    {
	if (!classPathEditor.isVisible()) classPathEditor.setVisible(true);
    }
}
