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
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;


/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class ActionTableCellEditor implements TableCellEditor, ActionListener
{

    private TableCellEditor editor;
    private JButton customEditorButton = new JButton("...");
    /**
     *
     */
    protected JTable table;
    /**
     *
     */
    /**
     *
     */
    protected int row, column;

    /**
     *
     * @param editor
     */
    public ActionTableCellEditor(TableCellEditor editor)
    {
	this.editor = editor;
	customEditorButton.addActionListener(this);

	// ui-tweaking
	customEditorButton.setFocusable(false);
	customEditorButton.setFocusPainted(false);
	customEditorButton.setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
	JPanel panel = new JPanel(new BorderLayout());
	panel.add(editor.getTableCellEditorComponent(table, value, isSelected, row, column));
	panel.add(customEditorButton, BorderLayout.EAST);
	this.table = table;
	this.row = row;
	this.column = column;

	panel.addFocusListener(new FocusAdapterImpl());

	return panel;
    }

    @Override
    public Object getCellEditorValue()
    {
	return editor.getCellEditorValue();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent)
    {
	return editor.isCellEditable(anEvent);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent)
    {
	return editor.shouldSelectCell(anEvent);
    }

    @Override
    public boolean stopCellEditing()
    {
	return editor.stopCellEditing();
    }

    @Override
    public void cancelCellEditing()
    {
	editor.cancelCellEditing();
    }

    @Override
    public void addCellEditorListener(CellEditorListener l)
    {
	editor.addCellEditorListener(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l)
    {
	editor.removeCellEditorListener(l);
    }

    @Override
    public final void actionPerformed(ActionEvent e)
    {
	editor.cancelCellEditing();
	editCell(table, row, column);
    }

    /**
     *
     * @param table
     * @param row
     * @param column
     */
    protected abstract void editCell(JTable table, int row, int column);

    private static class FocusAdapterImpl extends FocusAdapter
    {
        @Override
        public void focusGained(FocusEvent e)
        {
            JPanel panel = (JPanel) e.getSource();
            panel.getComponent(0).requestFocus();
            panel.removeFocusListener(this);
        }
    }
}