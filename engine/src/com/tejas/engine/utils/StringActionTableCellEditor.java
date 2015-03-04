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

import javax.swing.*;
import javax.swing.table.TableCellEditor;

import com.net2plan.utils.ActionTableCellEditor;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class StringActionTableCellEditor extends ActionTableCellEditor
{
    /**
     *
     * @param editor
     */
    public StringActionTableCellEditor(TableCellEditor editor){
        super(editor);
    }

    /**
     *
     * @param table
     * @param row
     * @param column
     */
    @Override
    protected void editCell(JTable table, int row, int column){
        JTextArea textArea = new JTextArea(10, 50);
        Object value = table.getValueAt(row, column);
        if(value!=null)
	{
            textArea.setText(value.toString());
            textArea.setCaretPosition(0);
        }

        int result = JOptionPane.showOptionDialog(table, new JScrollPane(textArea), (String)table.getColumnName(column), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if(result==JOptionPane.OK_OPTION)
            table.setValueAt(textArea.getText(), row, column);
    }
}
