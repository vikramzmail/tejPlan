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

import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.ParameterValueDescriptionPanel;
import com.tejas.engine.utils.StringLabeller;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.WiderJComboBox;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.SystemUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class RunnableSelector extends JPanel
{
    private static final long serialVersionUID = 1L;
    
    private JButton load;
    private JComboBox algorithmSelector;
    private static JFileChooser fileChooser;
    
    static
    {
        try
        {
            String defaultRunnableCodePath = Configuration.getOption("defaultRunnableCodePath");
            
            if (!defaultRunnableCodePath.isEmpty())
            {
                File file = new File(defaultRunnableCodePath);
                if (file.getCanonicalPath().endsWith(".jar")) { fileChooser = new JFileChooser(file); }
                else { fileChooser = new JFileChooser(file); }
            }
        }
        catch(Throwable e)
        {
            fileChooser = null;
        }
    }
    
    
    private JTextField file;
    private JTextArea description;
    private ParameterValueDescriptionPanel parametersPanel;
    private String label;
    private Set<Class<? extends IExternal>> _classes;
    private Map<String, Class> implementations;

    @Override
    public void setEnabled(boolean enabled)
    {
	algorithmSelector.setEnabled(enabled);
	load.setEnabled(enabled);
	parametersPanel.setEnabled(enabled);
    }

    /**
     * Resets the component.
     * 
     * @since 0.2.0
     */
    public void reset()
    {
	algorithmSelector.removeAllItems();
	file.setText("");
	description.setText("");
	parametersPanel.reset();

	getDefaults();
    }

    /**
     * Tries to load default executable code from the built-in examples file.
     * 
     * @since 0.2.0
     */
    public final void getDefaults()
    {
        try
        {
            String defaultRunnableCodePath = Configuration.getOption("defaultRunnableCodePath");
            if (defaultRunnableCodePath.endsWith(".jar")) { loadImplementations(new File(defaultRunnableCodePath)); }
            
//            if (fileChooser != null) loadImplementations();
            
//            File defaultFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "BuiltInExamples.jar");
//            loadImplementations(defaultFile);
        }
        catch(Throwable e)
        {
//		    throw new RuntimeException(e);
        }
    }
    
    private static class SortFQCN implements Comparator<String>
    {
        @Override
        public int compare(String fqcn1, String fqcn2)
        {
            String[] aux1 = ClassLoaderUtils.getPackageAndClassName(fqcn1);
            String[] aux2 = ClassLoaderUtils.getPackageAndClassName(fqcn2);
            
            int compare1 = aux1[1].compareTo(aux2[1]);
            if (compare1 != 0) return compare1;
            
            return aux1[0].compareTo(aux2[0]);
        }
    }
    
    private void loadImplementations(File f)
    {
        try
        {
            if (!f.isAbsolute()) f = new File(SystemUtils.getCurrentDir(), f.getPath());

            Map<String, Class> aux_implementations = new TreeMap<String, Class>();

            List<Class<IExternal>> aux = ClassLoaderUtils.getClassesFromFile(f, IExternal.class);
            for(Class<IExternal> implementation : aux)
            {
                Iterator<Class<? extends IExternal>> it = _classes.iterator();

                while(it.hasNext())
                {
                    Class<? extends IExternal> _class = it.next();

                    if (_class.isAssignableFrom(implementation))
                    {
                        aux_implementations.put(implementation.getName(), _class);
                        break;
                    }
                }
            }

            if (aux_implementations.isEmpty())
                throw new RunnableSelector.NoRunnableCodeFound("No runnable code found in file " + fileChooser.getSelectedFile().getCanonicalPath());

            implementations = aux_implementations;

            file.setText(f.getCanonicalPath());
            description.setText("");
            parametersPanel.reset();

            algorithmSelector.removeAllItems();

            Set<String> sortedSet = new TreeSet<String>(new SortFQCN());
            sortedSet.addAll(aux_implementations.keySet());

            ActionListener[] listeners = algorithmSelector.getActionListeners();
            for(ActionListener listener : listeners) algorithmSelector.removeActionListener(listener);

            for (String implementation : sortedSet)
            {
                String[] aux1 = ClassLoaderUtils.getPackageAndClassName(implementation);

                String implementationLabel;

                if (aux1[0].isEmpty())
                    implementationLabel = aux1[1];
                else
                    implementationLabel = aux1[1] + " (" + aux1[0] + ")";

                algorithmSelector.addItem(new StringLabeller(implementation, implementationLabel));
            }

            if (algorithmSelector.getItemCount() > 1) algorithmSelector.setSelectedIndex(-1);
            for(ActionListener listener : listeners) algorithmSelector.addActionListener(listener);

            if (algorithmSelector.getItemCount() == 1) algorithmSelector.setSelectedIndex(0);
        }
        catch(Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param label
     * @param _class
     * @param currentFolder
     * @param parametersPanel
     * @since 0.2.0
     */
    public RunnableSelector(final String label, final String labelForField, final Class<? extends IExternal> _class, File currentFolder, final ParameterValueDescriptionPanel parametersPanel)
    {
	this(label, labelForField, new HashSet<Class<? extends IExternal>>() {{ add(_class); }}, currentFolder, parametersPanel);
    }

    /**
     *
     * @param label
     * @param _classes
     * @param currentFolder
     * @param parametersPanel
     * @since 0.2.0
     */
    public RunnableSelector(final String label, final String labelForField, final Set<Class<? extends IExternal>> _classes, final File currentFolder, final ParameterValueDescriptionPanel parametersPanel)
    {
        this.label = label;
	this.parametersPanel = parametersPanel;

	this._classes = new HashSet<Class<? extends IExternal>>(_classes);

	description = new JTextArea();
	description.setFont(new JLabel().getFont());
	description.setLineWrap(true);
	description.setWrapStyleWord(true);
	description.setEditable(false);
        
	file = new JTextField();
	file.setEditable(false);

	algorithmSelector = new WiderJComboBox();
	algorithmSelector.addActionListener(new ActionListener()
	{

	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		if (algorithmSelector.getItemCount() == 0 || algorithmSelector.getSelectedIndex() == -1) return;

		try
		{
		    File fileName = new File(file.getText());
		    String className = (String) ((StringLabeller) algorithmSelector.getSelectedItem()).getObject();
		    Class<? extends IExternal> _class = implementations.get(className);

		    IExternal instance = ClassLoaderUtils.getInstance(fileName, className, _class);
		    String aux_description = instance.getDescription();
		    List<Triple<String, String, String>> aux_parameters = instance.getParameters();
                    if (aux_description == null) aux_description = "No description";
                    if (aux_parameters == null) aux_parameters = new LinkedList<Triple<String, String, String>>();
		    ((Closeable) instance.getClass().getClassLoader()).close();

		    String solverName = null;
		    Triple<String, String, String> solverLibraryNameItem = null;

		    Iterator<Triple<String, String, String>> it = aux_parameters.iterator();
		    while(it.hasNext())
		    {
			Triple<String, String, String> aux = it.next();
			String paramName = aux.getFirst();
			if (paramName.equals("solverName"))
			    solverName = aux.getSecond();

			if (paramName.equals("solverLibraryName"))
			    solverLibraryNameItem = aux;
		    }

		    if (solverName != null && solverLibraryNameItem != null && solverLibraryNameItem.getSecond().isEmpty())
		    {
                        try
                        {
                            String solverLibraryName = Configuration.getOption(solverName + "SolverLibraryName");
                            if (solverLibraryName != null)
                                solverLibraryNameItem.setSecond(solverLibraryName);
                        }
                        catch(Throwable ex) { }
		    }
                    
		    description.setText(aux_description);
		    if (!description.getText().isEmpty())
			description.setCaretPosition(0);
                    
		    parametersPanel.setParameters(aux_parameters);
		}
		catch (Exception ex)
		{
                    ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                    ErrorHandling.showErrorDialog("Error selecting " + label.toLowerCase(getLocale()));
		}
	    }
	});

        load = new JButton("Load");
	load.addActionListener(new ActionListener()
	{

	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
                try
                {
                    if (fileChooser == null)
                    {
                        fileChooser = new JFileChooser(currentFolder);
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    }
                    
		    for (FileFilter aux : fileChooser.getChoosableFileFilters())
			fileChooser.removeChoosableFileFilter(aux);

		    for (FileFilter aux : ClassLoaderUtils.getFileFilters())
			fileChooser.addChoosableFileFilter(aux);

		    fileChooser.setAcceptAllFileFilterUsed(false);

		    int rc = fileChooser.showOpenDialog(null);
		    if (rc != JFileChooser.APPROVE_OPTION) return;

		    loadImplementations(fileChooser.getSelectedFile());
		}
		catch (RunnableSelector.NoRunnableCodeFound ex)
		{
		    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to load");
		}
		catch (Exception ex)
		{
                    ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                    ErrorHandling.showErrorDialog("Error loading runnable code");
		}
	    }
	});

        setLayout(new MigLayout("", "[][grow][]", "[][][][][grow]"));
	add(new JLabel(labelForField == null ? label : labelForField));
	add(file, "growx");
	add(load, "wrap");
	add(algorithmSelector, "skip, growx, spanx 2, wrap, wmin 100");
	add(new JLabel("Description"), "top");
	add(new JScrollPane(description), "height 100::, spanx 2, grow, wrap");
	add(new JLabel("Parameters"), "spanx 3, wrap");
	add(parametersPanel, "spanx 3, grow");

	getDefaults();
    }

    /**
     * Returns the information required to call a runnable code.
     * 
     * @return Runnable information
     * @since 0.2.0
     */
    public Triple<File, String, Class> getRunnable()
    {
	String filename = file.getText();
	if (filename.isEmpty() || algorithmSelector.getSelectedIndex() == -1)
	    throw new Net2PlanException(label + " must be selected");
        
	String algorithm = (String) ((StringLabeller) algorithmSelector.getSelectedItem()).getObject();

	return Triple.of(new File(filename), algorithm, implementations.get(algorithm));
    }

    /**
     * Returns the parameters introduced by user.
     * 
     * @return Key-value map
     * @since 0.2.0
     */
    public Map<String, String> getRunnableParameters()
    {
	return new HashMap<String, String>(parametersPanel.getParameters());
    }

    private static class NoRunnableCodeFound extends RuntimeException
    {
	public NoRunnableCodeFound(String message)
	{
	    super(message);
	}
    }
}
