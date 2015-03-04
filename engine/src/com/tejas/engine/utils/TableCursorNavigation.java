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

import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JTable;

/**
 * <p>Key listener which allows to navigate by a JTable using the directional keys and triggering actions equivalent to the positioning with the mouse</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class TableCursorNavigation extends KeyAdapter
{
    @Override
    public void keyReleased(KeyEvent e)
    {
	if (!(e.getSource() instanceof JTable)) throw new RuntimeException("This listener is only valid for JTable");

	JTable table = (JTable) e.getSource();

	if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)
	{
	    int row = table.getSelectedRow();
	    int column = table.getSelectedColumn();

	    Rectangle rect = table.getCellRect(row, column, true);
	    int x = rect.x + (rect.width / 2);
	    int y = rect.y + (rect.height / 2);

	    MouseEvent me = new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 1, MouseEvent.BUTTON1, x, y, 1, false);
	    table.dispatchEvent(me);
	}
    }
}
