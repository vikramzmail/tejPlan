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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FixedColumnDecorator implements ChangeListener, PropertyChangeListener
{
    private JTable mainTable;
    private JTable fixedTable;
    private JScrollPane scrollPane;

    private class TableColumnWidthListener implements TableColumnModelListener
    {

	@Override
	public void columnMarginChanged(ChangeEvent e)
	{
	    TableColumnModel tcm = (TableColumnModel) e.getSource();
	    fixedTable.setPreferredScrollableViewportSize(new Dimension(tcm.getTotalColumnWidth(), fixedTable.getSize().height));
	}

	@Override
	public void columnMoved(TableColumnModelEvent e)
	{
	}

	@Override
	public void columnAdded(TableColumnModelEvent e)
	{
	}

	@Override
	public void columnRemoved(TableColumnModelEvent e)
	{
	}

	@Override
	public void columnSelectionChanged(ListSelectionEvent e)
	{
	}
    }

    /**
     *
     * @param columnsToBeFix
     * @param scrollPaneOfMainTable
     */
    public FixedColumnDecorator(int columnsToBeFix, JScrollPane scrollPaneOfMainTable)
    {
	this.scrollPane = scrollPaneOfMainTable;

	mainTable = ((JTable) scrollPaneOfMainTable.getViewport().getView());
	mainTable.setAutoCreateColumnsFromModel(false);
	mainTable.addPropertyChangeListener(this);

	fixedTable = new JTableImpl();

	fixedTable.setAutoCreateColumnsFromModel(false);
	fixedTable.setModel(mainTable.getModel());
	fixedTable.setSelectionModel(mainTable.getSelectionModel());
	fixedTable.setFocusable(false);

	for (int i = 0; i < columnsToBeFix; i++)
	{
	    TableColumnModel columnModel = mainTable.getColumnModel();
	    TableColumn column = columnModel.getColumn(0);
	    columnModel.removeColumn(column);
	    fixedTable.getColumnModel().addColumn(column);
	}

	fixedTable.setPreferredScrollableViewportSize(fixedTable.getPreferredSize());
	scrollPaneOfMainTable.setRowHeaderView(fixedTable);
	scrollPaneOfMainTable.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());

	//Sinkronasi scrolling dari row header milik fixed table dengan main table
	scrollPaneOfMainTable.getRowHeader().addChangeListener(this);

	fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	fixedTable.setRowSorter(mainTable.getRowSorter());
	mainTable.setUpdateSelectionOnSort(true);
	fixedTable.setUpdateSelectionOnSort(false);
        
        for(MouseMotionListener listener : mainTable.getTableHeader().getMouseMotionListeners())
            fixedTable.getTableHeader().addMouseMotionListener(listener);

        for(MouseListener listener : mainTable.getMouseListeners())
            fixedTable.addMouseListener(listener);

        fixedTable.getColumnModel().addColumnModelListener(new FixedColumnDecorator.TableColumnWidthListener());

	MouseAdapter ma = new FixedColumnMouseAdapter();
	fixedTable.getTableHeader().addMouseListener(ma);
    }

    /**
     *
     * @return
     */
    public JTable getFixedTable()
    {
	return fixedTable;
    }

    /**
     *
     * @return
     */
    public JTable getMainTable()
    {
	return mainTable;
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
	//menjaga agar fixed table tetap sinkron dengan main table saat stateChanged
	JViewport viewport = (JViewport) e.getSource();
	scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
	//menjaga agar fixed table sinkronasi dengan main table saat propertyChanged
	if ("selectionModel".equals(e.getPropertyName()))
	{
	    fixedTable.setSelectionModel(mainTable.getSelectionModel());
	}

	if ("model".equals(e.getPropertyName()))
	{
	    fixedTable.setModel(mainTable.getModel());
	}
    }

    private static class JTableImpl extends JTable
    {
        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return getPreferredSize().height < getParent().getHeight();
        }
    }

    private static class FixedColumnMouseAdapter extends MouseAdapter
    {
        private TableColumn column = null;
        private int columnWidth;
        private int pressedX;

        @Override
        public void mousePressed(MouseEvent e)
        {
            JTableHeader header = (JTableHeader) e.getComponent();
            TableColumnModel tcm = header.getColumnModel();
            int columnIndex = tcm.getColumnIndexAtX(e.getX());
            Cursor cursor = header.getCursor();

            if (columnIndex == tcm.getColumnCount() - 1 && cursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
            {
                column = tcm.getColumn(columnIndex);
                columnWidth = column.getWidth();
                pressedX = e.getX();
                header.addMouseMotionListener(this);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            column = null;

            JTableHeader header = (JTableHeader) e.getComponent();
            header.removeMouseMotionListener(this);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (column == null) return;

            int width = columnWidth - pressedX + e.getX();
            column.setPreferredWidth(width);
            JTableHeader header = (JTableHeader) e.getComponent();
            JTable table = header.getTable();
            table.setPreferredScrollableViewportSize(table.getPreferredSize());
            JScrollPane scrollPane = (JScrollPane) table.getParent().getParent();
            scrollPane.revalidate();
        }
    }
}