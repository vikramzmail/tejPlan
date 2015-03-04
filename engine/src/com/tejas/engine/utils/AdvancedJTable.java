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

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;

import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.TableCursorNavigation;

/**
 * <p>Extended version of the <code>JTable</code> class. It presents the following
 * additional features:</p>
 * 
 * <ul>
 * <li>Reordering of table columns is not allowed</li>
 * <li>Auto-resize of columns is disabled</li>
 * <li>It allows to set per-cell editors</li>
 * <li>It allows to navigate the table with the cursor</li>
 * </ul>
 * 
 * <p>Credits to Santhosh Kumar for his methods to solve partially visible cell
 * issues (<a href='http://www.jroller.com/santhosh/entry/partially_visible_tablecells'>Partially Visible TableCells</a>)</p>
 * 
 * <p>Credits to "Kah - The Developer" for his static method to set column widths
 * in proportion to each other (<a href='http://kahdev.wordpress.com/2011/10/30/java-specifying-the-column-widths-of-a-jtable-as-percentages/'>Specifying the column widths of a JTable as percentages</a>)
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class AdvancedJTable extends JTable
{
    private static final long serialVersionUID = 1L;

    private boolean disableSetAutoResizeMode;
    private final Map<Pair<Integer, Integer>, TableCellEditor> cellEditorMap;
    private final Map<Pair<Integer, Integer>, String> tooltipMap;

    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public AdvancedJTable()
    {
	super();
        
	getTableHeader().setReorderingAllowed(false);
	setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	addKeyListener(new TableCursorNavigation());
	cellEditorMap = new HashMap<Pair<Integer, Integer>, TableCellEditor>();
	tooltipMap = new HashMap<Pair<Integer, Integer>, String>();
        
        disableSetAutoResizeMode = true;
    }

    /**
     * Constructor that allows to set the table model.
     * 
     * @param model Table model
     * @since 0.2.0
     */
    public AdvancedJTable(TableModel model)
    {
	this();
        
	setModel(model);
    }

    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column)
    {
	final Component prepareRenderer = super.prepareRenderer(renderer, row, column);
	final TableColumn tableColumn = getColumnModel().getColumn(column);

	if (column != -1 && tableColumn.getHeaderValue().toString().startsWith("Attrib"))
	    tableColumn.setPreferredWidth(Math.max(prepareRenderer.getPreferredSize().width, tableColumn.getPreferredWidth()));
 
	return prepareRenderer;
    }
    
    @Override
    public void doLayout()
    {
	TableColumn resizingColumn = null;

	if (tableHeader != null) resizingColumn = tableHeader.getResizingColumn();

	if (resizingColumn == null)
	{
            //  Viewport size changed. May need to increase columns widths
	    super.doLayout();
	}
	else
	{
            //  Specific column resized. Reset preferred widths
	    TableColumnModel tcm = getColumnModel();

	    for (int i = 0; i < tcm.getColumnCount(); i++)
	    {
		TableColumn tc = tcm.getColumn(i);
		tc.setPreferredWidth(tc.getWidth());
	    }
            
            disableSetAutoResizeMode = false;

            // Columns don't fill the viewport, invoke default layout
	    if (tcm.getTotalColumnWidth() < getParent().getWidth())
            {
                setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                super.doLayout();
            }
            
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            disableSetAutoResizeMode = true;
	}
    }
    
    @Override
    public final void setAutoResizeMode(int mode)
    {
        if (disableSetAutoResizeMode) throw new UnsupportedOperationException("Forbidden operation");
        
        super.setAutoResizeMode(mode);
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
	return getPreferredSize().width < getParent().getWidth();
    }

    /**
     *
     * @param row Model row
     * @param column Model column
     * @param tableCellEditor
     */
    public void setCellEditor(int row, int column, TableCellEditor tableCellEditor)
    {
	cellEditorMap.put(Pair.of(row, column), tableCellEditor);
    }

    public void setToolTipText(int row, int column, String tooltipText)
    {
	tooltipMap.put(Pair.of(row, column), tooltipText);
    }
    
    /**
     *
     * @param row Model row
     * @param column Model column
     */
    @Override
    public TableCellEditor getCellEditor(int row, int column)
    {
	if (cellEditorMap.containsKey(Pair.of(row, column)))
	    return cellEditorMap.get(Pair.of(row, column));

	return super.getCellEditor(row, column);
    }

    /**
     * Sets the number of visible rows in a table.
     * 
     * @param table Table
     * @param rows Number of rows to show
     * @since 0.2.0
     */
    public static void setVisibleRowCount(JTable table, int rows)
    {
	int height = 0;
	for (int row = 0; row < rows; row++) height += table.getRowHeight(row);

	table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredScrollableViewportSize().width, height));
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
	return getPreferredSize().height < getParent().getHeight();
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
	Point p = event.getPoint();

	int hitColumnIndex = columnAtPoint(p);
	int hitRowIndex = rowAtPoint(p);

	if (hitRowIndex == -1 || hitColumnIndex == -1) return null;
        
	TableCellRenderer renderer = getCellRenderer(hitRowIndex, hitColumnIndex);
	Component component = prepareRenderer(renderer, hitRowIndex, hitColumnIndex);

        int rowModelIndex = convertRowIndexToModel(hitRowIndex);
        int columnModelIndex = convertColumnIndexToModel(hitColumnIndex);
            
        String tip = tooltipMap.get(Pair.of(rowModelIndex, columnModelIndex));

	if (tip == null && component instanceof JComponent)
	{
	    Rectangle cellRect = getCellRect(hitRowIndex, hitColumnIndex, false);
	    if (cellRect.width >= component.getPreferredSize().width) return null;

            p.translate(-cellRect.x, -cellRect.y);
	    MouseEvent newEvent = new MouseEvent(component, event.getID(), event.getWhen(), event.getModifiers(), p.x, p.y, event.getClickCount(), event.isPopupTrigger());
	    tip = ((JComponent) component).getToolTipText(newEvent);
	}
        
	if (tip == null) tip = getToolTipText();

	if (tip == null)
	{
	    Object value = getValueAt(hitRowIndex, hitColumnIndex);
            
            if (value != null)
            {
                String stringValue = value.toString();
                if (stringValue != null && stringValue.length() > 0) tip = stringValue;
            }
	}

	return tip;
    }

    @Override
    public Point getToolTipLocation(MouseEvent event)
    {
	int row = rowAtPoint(event.getPoint());
	if (row == -1) return null;

	int col = columnAtPoint(event.getPoint());
	if (col == -1) return null;

	boolean hasTooltip = getToolTipText() == null ? getToolTipText(event) != null : true;
	return hasTooltip ? getCellRect(row, col, false).getLocation() : null;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column)
    {
	final Component c = super.prepareEditor(editor, row, column);
	if (c instanceof JTextComponent)
	{
	    SwingUtilities.invokeLater(new SelectAllText(c));
	}
        
	return c;
    }

    @Override
    public void tableChanged(TableModelEvent e)
    {
	super.tableChanged(e);

	int column = e.getColumn();
	int firstRow = e.getFirstRow();
	int lastRow = e.getLastRow();

	if (column == -1 || firstRow == -1 || lastRow == -1) return;

	column = convertColumnIndexToView(column);
	firstRow = convertRowIndexToView(firstRow);
	lastRow = convertRowIndexToView(lastRow);
	int numRows = lastRow - firstRow + 1;

	for (int row = firstRow; row <= lastRow; row++)
	{
	    Pair<Integer, Integer> key = Pair.of(row, column);
	    if (cellEditorMap.containsKey(key)) cellEditorMap.remove(Pair.of(row, column));
	    if (tooltipMap.containsKey(key)) tooltipMap.remove(Pair.of(row, column));
	}

	Map<Pair<Integer, Integer>, TableCellEditor> newEntries = new HashMap<Pair<Integer, Integer>, TableCellEditor>();

	for (Iterator<Map.Entry<Pair<Integer, Integer>, TableCellEditor>> it = cellEditorMap.entrySet().iterator(); it.hasNext();)
	{
	    Map.Entry<Pair<Integer, Integer>, TableCellEditor> entry = it.next();

	    int row2 = entry.getKey().getFirst();
	    int col2 = entry.getKey().getSecond();

	    if (column == col2 && row2 > lastRow)
	    {
		newEntries.put(Pair.of(row2 - numRows, col2), entry.getValue());
		it.remove();
	    }
	}

	cellEditorMap.putAll(newEntries);
    }

    /**
     * Sets the width of the columns as percentages.
     *
     * @param table       the <code>JTable</code> whose columns will be set
     * @param percentages thee widths of the columns as percentages; note: this
     *   method does NOT verify that all percentages add up to 100% and for
     *   the columns to appear properly, it is recommended that the widths for
     *   ALL columns be specified
     * @since 0.2.0
     */
    public static void setWidthAsPercentages(JTable table, double... percentages)
    {
	final double factor = table.getPreferredScrollableViewportSize().getWidth();
	TableColumnModel model = table.getColumnModel();
	for (int columnIndex = 0; columnIndex < percentages.length; columnIndex++)
	{
	    TableColumn column = model.getColumn(columnIndex);
	    column.setPreferredWidth((int) (percentages[columnIndex] * factor));
	}
    }

    private static class SelectAllText implements Runnable
    {
        private final Component c;

        public SelectAllText(Component c) { this.c = c; }

        @Override
        public void run() { ((JTextComponent) c).selectAll(); }
    }

    public static class ColumnHeaderToolTips extends MouseMotionAdapter
    {
        private TableColumn curCol;
        private Map tips = new HashMap();
        
        public void setToolTip(TableColumn col, String tooltip)
        {
            if (tooltip == null) tips.remove(col);
            else tips.put(col, tooltip);
        }
        
        @Override
        public void mouseMoved(MouseEvent evt)
        {
            JTableHeader header = (JTableHeader) evt.getSource();
            JTable table = header.getTable();
            TableColumnModel colModel = table.getColumnModel();
            int vColIndex = colModel.getColumnIndexAtX(evt.getX());

            TableColumn col = null;
            if (vColIndex >= 0) col = colModel.getColumn(vColIndex);

            if (col != curCol)
            {
                header.setToolTipText((String) tips.get(col));
                curCol = col;
            }
        }
    }
}
