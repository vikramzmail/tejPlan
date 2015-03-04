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

import com.tejas.engine.utils.ActionTableCellEditor;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.ParameterValueDescriptionPanel;
import com.tejas.engine.utils.RunnableSelector;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.SystemUtils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Allows to define parameters for a report.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ReportParameterPanel extends ParameterValueDescriptionPanel
{
    private static final long serialVersionUID = 1L;

    private Map<String, Pair<RunnableSelector, Integer>> algorithms;
    private File ALGORITHM_DIRECTORY;

    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public ReportParameterPanel()
    {
	File CURRENT_DIRECTORY = SystemUtils.getCurrentDir();
	ALGORITHM_DIRECTORY = new File(CURRENT_DIRECTORY + SystemUtils.getDirectorySeparator() + "workspace");
	ALGORITHM_DIRECTORY = ALGORITHM_DIRECTORY.isDirectory() ? ALGORITHM_DIRECTORY : CURRENT_DIRECTORY;

	algorithms = new HashMap<String, Pair<RunnableSelector, Integer>>();

	setLayout(new BorderLayout());
	Object[][] data = { {null, null, null} };
	header = new String[] {"Parameter", "Value", "Description"};

	final TableModel model = new ReportParameterTableModel(data, header);

	table = new AdvancedJTable(model);
	table.setEnabled(false);
	add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     *
     * @param parameters
     */
    @Override
    public void setParameters(List<Triple<String, String, String>> parameters)
    {
	DefaultTableModel model = (DefaultTableModel) table.getModel();

	int numParameters = parameters.size();

	model.setDataVector(new Object[][]
		{
		    {
			null, null, null
		    }
		}, header);

	algorithms.clear();

	if (numParameters > 0)
	{
	    model.removeRow(0);

	    Iterator<Triple<String, String, String>> it = parameters.iterator();

	    while (it.hasNext())
	    {
		Triple<String, String, String> aux = it.next();

		if (aux.getFirst().startsWith("alg_"))
		{
                    String fileName, algorithmName, algorithmParameters;

                    RunnableSelector runnable = new RunnableSelector("Provisioning algorithm", "File", IProvisioningAlgorithm.class, ALGORITHM_DIRECTORY, new ParameterValueDescriptionPanel());
                    runnable.setPreferredSize(new Dimension(640, 480));
                    
                    try
                    {
                        Triple<File, String, Class> aux1 = runnable.getRunnable();
                        fileName = aux1.getFirst().getCanonicalPath();
                        algorithmName = aux1.getSecond();
                        algorithmParameters = StringUtils.mapToString(runnable.getRunnableParameters(), "=", ", ");
                    }
                    catch(Throwable e)
                    {
                        fileName = "";
                        algorithmName = "";
                        algorithmParameters = "";
                    }
                   
		    model.addRow(new Object[] { aux.getFirst() + "_File", fileName, aux.getThird() });
		    model.addRow(new Object[] { aux.getFirst() + "_Algorithm", algorithmName, "Algorithm" });
		    model.addRow(new Object[] { aux.getFirst() + "_Parameters", algorithmParameters, "Algorithm parameters" });
		    algorithms.put(aux.getFirst() + "_File", Pair.of(runnable, model.getRowCount() - 3));

		    addCellEditor(table, model.getRowCount() - 3, 1);
		}
		else
		{
		    model.addRow(new Object[] { aux.getFirst(), aux.getSecond(), aux.getThird() });
		}
	    }

	    table.setEnabled(true);
	}
	else
	{
	    table.setEnabled(false);
	}

    }

    private void addCellEditor(AdvancedJTable table, int rowTable, int columnTable)
    {
	JTextField textField = new JTextField();
	textField.setEnabled(false);
	textField.setBorder(BorderFactory.createEmptyBorder());
	DefaultCellEditor editor = new DefaultCellEditor(textField);
	editor.setClickCountToStart(1);

	table.setCellEditor(rowTable, 1, new ActionTableCellEditor(editor)
	{

	    @Override
	    protected void editCell(JTable table, int row, int column)
	    {
		try
		{
		    int rowModel = table.convertRowIndexToModel(row);
		    String algorithm = table.getModel().getValueAt(rowModel, 0).toString();

		    RunnableSelector current = algorithms.get(algorithm).getFirst();

                    while(true)
                    {
                        try
                        {
                            int result = JOptionPane.showOptionDialog(null, current, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
                            if (result != JOptionPane.OK_OPTION) return;

                            TableModel model = table.getModel();
                            model.setValueAt(current.getRunnable().getFirst().getCanonicalPath(), rowModel, 1);
                            model.setValueAt(current.getRunnable().getSecond(), rowModel + 1, 1);
                            model.setValueAt(StringUtils.mapToString(current.getRunnableParameters(), "=", ", "), rowModel + 2, 1);
                            addCellEditor(((AdvancedJTable) table), row, column);
                            
                            break;
                        }
                        catch(Net2PlanException e)
                        {
                            ErrorHandling.showErrorDialog(e.getMessage(), "Error");
                        }
                    }
		}
		catch (Exception ex)
		{
		    ErrorHandling.addErrorOrException(ex, ReportParameterPanel.class);
		    ErrorHandling.showErrorDialog("An error happened");
		}
	    }
	});
    }

    /**
     *
     * @return
     */
    @Override
    public Map<String, String> getParameters()
    {
	Map<String, String> out = new HashMap<String, String>();

	if (table.isEnabled())
	{
	    TableModel model = table.getModel();

	    int numRows = model.getRowCount();
	    for (int rowId = 0; rowId < numRows; rowId++)
	    {
		out.put(model.getValueAt(rowId, 0).toString(), model.getValueAt(rowId, 1).toString());
	    }
	}

	return out;
    }

    /**
     *
     */
    @Override
    public void reset()
    {
	setParameters(new ArrayList<Triple<String, String, String>>());
    }

    private static class ReportParameterTableModel extends ClassAwareTableModel
    {
        private static final long serialVersionUID = 1L;
        
        public ReportParameterTableModel(Object[][] dataVector, Object[] columnIdentifiers) { super(dataVector, columnIdentifiers); }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            if (column == 0 || column == 2) return false;

            String value = getValueAt(row, 0).toString();

            if (value.startsWith("alg_") && !value.endsWith("File")) return false;

            return true;
        }
    }
}
