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

import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.interfaces.networkDesign.Configuration;

import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Set of several cell renderers used into the GUI.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CellRenderers
{
    /**
     * Shades cell in red or orange if value is greater or equal than one, respectively.
     * 
     * @since 0.2.0
     */
    public static class LinkUtilizationCellRenderer extends NumberCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public LinkUtilizationCellRenderer() { super(); }
        
        public LinkUtilizationCellRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null)
	    {
		double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

		double aux = (Double) value;
		if (DoubleUtils.isEqualWithinAbsoluteTolerance(aux, 1, PRECISIONFACTOR))
		    c.setBackground(Color.ORANGE);
		else
		    if (aux > 1) c.setBackground(Color.RED);
	    }

	    return c;
	}
    }

    /**
     * Shades cell in red if value is greater than zero.
     * 
     * @since 0.2.0
     */
    public static class LostTrafficCellRenderer extends NumberCellRenderer
    {
        private static final long serialVersionUID = 1L;

        private final int offeredTrafficColumnModelIndex;
        
        /**
         * Default constructor.
         * 
         * @param offeredTrafficColumnModelIndex Column index in model order in which is found the offered traffic
         * @since 0.2.0
         */
        public LostTrafficCellRenderer(int offeredTrafficColumnModelIndex)
        {
            super();
            this.offeredTrafficColumnModelIndex = offeredTrafficColumnModelIndex;
        }
        
        public LostTrafficCellRenderer(int offeredTrafficColumnModelIndex, boolean markNonEditable)
        {
            super(markNonEditable);
            this.offeredTrafficColumnModelIndex = offeredTrafficColumnModelIndex;
        }

        @Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	    if (!isSelected && value != null)
	    {
		int demandId = table.convertRowIndexToModel(row);
                double lostTraffic = (Double) value;
                double h_d = (Double) table.getModel().getValueAt(demandId, offeredTrafficColumnModelIndex);
                if (h_d > 0)
                    c.setBackground(lostTraffic > 0 ? Color.RED : Color.GREEN);
	    }

	    return c;
	}
    }
    
    public static class CheckBoxRenderer extends NonEditableCellRenderer
    {
        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        
        public CheckBoxRenderer() { super(); }
        
        public CheckBoxRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            JCheckBox checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPainted(false);
            checkBox.setSelected((value != null && ((Boolean) value).booleanValue()));
            checkBox.setForeground(c.getForeground());
	    checkBox.setBackground(c.getBackground());

            if (hasFocus)  setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            else setBorder(noFocusBorder);
            
            return checkBox;
	}
    }
    
    public static class NonEditableCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;
        
        private static Color bgColorNonEditable = new Color(245, 245, 245);
        private final boolean markNonEditable;
        
        public NonEditableCellRenderer()
        {
            this(false);
        }
        
        public NonEditableCellRenderer(boolean markNonEditable)
        {
            super();
            
            this.markNonEditable = markNonEditable;
        } 

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	    // When you call setForeground() or setBackground(), the selected colors stay
	    // in effect until subsequent calls are made to those methods. Without care,
	    // you might inadvertently color cells that should not be colored. To prevent
	    // that from happening, you should always include calls to the aforementioned
	    // methods that set a cell's colors to their look and feel–specific defaults,
	    // if that cell is not being colored.
            c.setForeground(UIManager.getColor(isSelected ? "Table.selectionForeground" : "Table.foreground"));
	    c.setBackground(UIManager.getColor(isSelected ? "Table.selectionBackground" : "Table.background"));
            
            if (!isSelected && markNonEditable)
            {
                if (row != -1 && column != -1)
                {
                    row = table.convertRowIndexToModel(row);
                    column = table.convertColumnIndexToModel(column);

                    if (!table.getModel().isCellEditable(row, column))
                        c.setBackground(isSelected ? UIManager.getColor("Table.selectionBackground") : bgColorNonEditable);
                }
            }
            
	    return c;
	}
    }

    /**
     * Gives the current number format to cells, and fixes the
     * <code>setForeground()</code>/<code>setBackground()</code> issue.
     * 
     * @since 0.2.0
     */
    public static class NumberCellRenderer extends NonEditableCellRenderer
    {
        private static final long serialVersionUID = 1L;
        
        public NumberCellRenderer() { super(); }
        
        public NumberCellRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    if (value instanceof Number)
	    {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		value = nf.format(((Number) value).doubleValue() == 0 ? 0 : value);
		setHorizontalAlignment(SwingConstants.RIGHT);
	    }

	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            return c;
	}
    }
    
    /**
     * Shades in gray rows referencing to the planned state, in tables where
     * current vs planned state live together. It assumes that odd rows are for
     * the current state, while even ones are for the planned state.
     * 
     * @since 0.2.0
     */
    public static class CurrentAndPlannedStateCellRenderer extends CellRenderers.NumberCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public CurrentAndPlannedStateCellRenderer() { super(); }

        public CurrentAndPlannedStateCellRenderer(boolean markNonEditable) { super(markNonEditable); }
        
        /**
         * Default cell background color for planned state.
         * 
         * @since 0.2.0
         */
        public final static Color PLANNED_BACKGROUND_COLOR = new Color(240, 240, 240);
        
	@Override
	public final Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	    if (!isSelected && row != -1 && column != -1)
	    {
		row = table.convertRowIndexToModel(row);
		column = table.convertColumnIndexToModel(column);
                
                int itemId = (int) Math.floor(row / 2.0);
                
		if (row % 2 == 0) setCurrentState(c, table, itemId, row, column, isSelected);
                else setPlannedState(c, table, itemId, row, column, isSelected);
	    }

	    return c;
	}
        
        /**
         * Sets the cell properties for the current state.
         * 
         * @param c Component
         * @param table Table
         * @param itemId Actual item id (node identifier...)
         * @param rowIndexModel Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public void setCurrentState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
        {
        }
        
        /**
         * Sets the cell properties for the planned state.
         * 
         * @param c Component
         * @param table Table
         * @param itemId Actual item id (node identifier...)
         * @param rowIndexModel Row index in model order
         * @param columnIndexModel Column index in model order
         * @param isSelected Indicates whether or not the cell is selected
         * @since 0.2.0
         */
        public void setPlannedState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
        {
            c.setBackground(PLANNED_BACKGROUND_COLOR);
        }
    }
}
