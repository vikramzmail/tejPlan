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

import java.awt.BorderLayout;
import java.util.*;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.Triple;

/**
 * Allows to define parameters.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ParameterValueDescriptionPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    /**
     * Reference to the parameter table.
     * 
     * @since 0.2.0
     */
    protected AdvancedJTable table;

    /**
     * Reference to the column title array.
     * 
     * @since 0.2.0
     */
    protected String[] header;
    private boolean haveData;

    @Override
    public void setEnabled(boolean enabled)
    {
	super.setEnabled(enabled);
	table.setEnabled(enabled);
	if (!haveData) table.setEnabled(false);
    }

    /**
     *
     */
    public ParameterValueDescriptionPanel()
    {
	setLayout(new BorderLayout());
	Object[][] data = { { null, null, null } };
	header = new String[] { "Parameter", "Value", "Description" };

	TableModel model = new ClassAwareTableModelImpl(data, header);

	table = new AdvancedJTable(model);
	table.setEnabled(false);
	haveData = false;
	add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     *
     * @param parameters
     */
    public void setParameters(List<Triple<String, String, String>> parameters)
    {
	DefaultTableModel model = (DefaultTableModel) table.getModel();

	int numParameters = parameters.size();
	Object[][] data;
	if (numParameters > 0)
	{
            List<Triple<String, String, String>> sortedList = new ArrayList<Triple<String, String, String>>(parameters);
            Collections.sort(sortedList, new SortByParameterNameComparator());
            
	    data = new Object[numParameters][header.length];
	    Iterator<Triple<String, String, String>> it = sortedList.iterator();
	    int paramId = 0;
	    while(it.hasNext())
	    {
		Triple<String, String, String> aux = it.next();

		data[paramId][0] = aux.getFirst();
		data[paramId][1] = aux.getSecond();
		data[paramId++][2] = aux.getThird();
	    }
	    table.setEnabled(true);
	    haveData = true;
	}
	else
	{
	    data = new Object[][] { { null, null, null } };
	    table.setEnabled(false);
	    haveData = false;
	}

	model.setDataVector(data, header);
    }

    /**
     *
     * @return
     */
    public Map<String, String> getParameters()
    {
	Map<String, String> out = new HashMap<String, String>();

	if (haveData)
	{
	    TableModel model = table.getModel();

	    int numRows = model.getRowCount();
	    for(int rowId = 0; rowId < numRows; rowId++)
		out.put(model.getValueAt(rowId, 0).toString(), model.getValueAt(rowId, 1).toString());
	}

	return out;
    }

    /**
     *
     */
    public void reset()
    {
	setParameters(new ArrayList<Triple<String, String, String>>());
    }

    private static class ClassAwareTableModelImpl extends ClassAwareTableModel
    {
        private static final long serialVersionUID = 1L;

        public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
        {
            super(dataVector, columnIdentifiers);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == 1 ? true : false;
        }
    }

    private static class SortByParameterNameComparator implements Comparator<Triple<String, String, String>>
    {
        @Override
        public int compare(Triple<String, String, String> o1, Triple<String, String, String> o2)
        {
            return o1.getFirst().compareTo(o2.getFirst());
        }
    }
}
