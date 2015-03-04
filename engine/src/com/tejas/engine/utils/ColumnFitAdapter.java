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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * Allows to adjust the column width of a table to the content by double-clicking
 * the column border.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ColumnFitAdapter extends MouseAdapter
{
    @Override
    public void mouseClicked(MouseEvent e)
    {
        e.consume();
        
        if (e.getClickCount() == 2)
        {
            JTableHeader header = (JTableHeader) e.getSource();
            TableColumn tableColumn = getResizingColumn(header, e.getPoint());
            if (tableColumn == null) return;

            int col = header.getColumnModel().getColumnIndex(tableColumn.getIdentifier());
            JTable table = header.getTable();
            int rowCount = table.getRowCount();
            int width = (int) header.getDefaultRenderer()
                    .getTableCellRendererComponent(table, tableColumn.getIdentifier(), false, false, -1, col).getPreferredSize().getWidth();
            for (int row = 0; row < rowCount; row++)
            {
                int preferedWidth = (int) table.getCellRenderer(row, col).getTableCellRendererComponent(table,
                        table.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
                width = Math.max(width, preferedWidth);
            }
            header.setResizingColumn(tableColumn); // this line is very important
            tableColumn.setWidth(width + table.getIntercellSpacing().width);
        }
    }

    private TableColumn getResizingColumn(JTableHeader header, Point p)
    {
        return getResizingColumn(header, p, header.columnAtPoint(p));
    }

    private TableColumn getResizingColumn(JTableHeader header, Point p, int column)
    {
        if (column == -1) return null;

        Rectangle r = header.getHeaderRect(column);
        r.grow(-3, 0);
        
        if (r.contains(p)) return null;

        int midPoint = r.x + r.width / 2;
        int columnIndex;

        if (header.getComponentOrientation().isLeftToRight())
            columnIndex = (p.x < midPoint) ? column - 1 : column;
        else
            columnIndex = (p.x < midPoint) ? column : column - 1;

        if (columnIndex == -1) return null;

        return header.getColumnModel().getColumn(columnIndex);
    }
}