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

import java.text.Collator;
import java.util.Comparator;
import javax.swing.DefaultRowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @param <M>
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CurrentAndPlannedStateTableSorter<M extends TableModel> extends TableRowSorter<M>
{

    /**
     *
     * @param model
     */
    public CurrentAndPlannedStateTableSorter(M model) { super(model); }

    // overridden to use SummaryModelWrapper
    @Override
    public void setModel(M model)
    {
	super.setModel(model);
	setModelWrapper(new CurrentAndPlannedStateModelWrapper<M>(getModelWrapper()));
    }

    // overridden to use TableCellValueComparator always
    @Override
    protected boolean useToString(int column) { return false; }

    // overridden to returns TableCellValueComparator
    @Override
    public Comparator<?> getComparator(int column)
    {
//	Comparator comparator = super.getComparator(column);
//	if (comparator instanceof Collator) comparator = null;

        return new CurrentAndPlannedStateTableCellValueComparator(super.getComparator(column));
    }

    private class CurrentAndPlannedStateModelWrapper<M extends TableModel> extends DefaultRowSorter.ModelWrapper<M, Integer>
    {

	private DefaultRowSorter.ModelWrapper<M, Integer> delegate;

	public CurrentAndPlannedStateModelWrapper(DefaultRowSorter.ModelWrapper<M, Integer> delegate)
	{
	    this.delegate = delegate;
	}

	@Override
	public M getModel() { return delegate.getModel(); }

	@Override
	public int getColumnCount() { return delegate.getColumnCount(); }

	@Override
	public int getRowCount() { return delegate.getRowCount(); }

	@Override
	public String getStringValueAt(int row, int column) { return delegate.getStringValueAt(row, column); }

	// returns TableCellValue instances always
	@Override
	public Object getValueAt(int row, int column) { return new CurrentAndPlannedStateTableCellValue(row, column, delegate.getValueAt(row, column)); }

	@Override
	public Integer getIdentifier(int row) { return delegate.getIdentifier(row); }
    }

    private class CurrentAndPlannedStateTableCellValue
    {
	public int row;
	public int column;
	public Object value;

	public CurrentAndPlannedStateTableCellValue(int row, int column, Object value)
	{
	    this.row = row;
	    this.column = column;
	    this.value = value;
	}
    }

    private class CurrentAndPlannedStateTableCellValueComparator implements Comparator<CurrentAndPlannedStateTableCellValue>
    {
	private final Comparator delegate;

	public CurrentAndPlannedStateTableCellValueComparator(Comparator delegate) { this.delegate = delegate; if (delegate == null) throw new RuntimeException("Bad"); }

	@Override
	public int compare(CurrentAndPlannedStateTableCellValue cell1, CurrentAndPlannedStateTableCellValue cell2)
	{
	    Object value1 = cell1.value;
	    Object value2 = cell2.value;
	    int row1 = cell1.row;
	    int row2 = cell2.row;

	    if (row1 >= 0 || row2 >= 0)
	    {
		if (row1 % 2 == 1)
		{
		    row1--;
		    value1 = getModel().getValueAt(row1, cell1.column);
		}

		if (row2 % 2 == 1)
		{
		    row2--;
		    value2 = getModel().getValueAt(row2, cell2.column);
		}
	    }

	    return delegate.compare(value1, value2);
	}
    }
}
