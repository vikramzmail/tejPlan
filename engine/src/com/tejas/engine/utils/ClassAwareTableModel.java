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

import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ClassAwareTableModel extends DefaultTableModel
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public ClassAwareTableModel()
    {
	super();
    }

    /**
     *
     * @param dataVector
     * @param columnIdentifiers
     */
    public ClassAwareTableModel(Object[][] dataVector, Object [] columnIdentifiers)
    {
	super.setDataVector(dataVector, columnIdentifiers);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
	return false;
    }

    @Override
    public Class getColumnClass(int col)
    {
	// http://www.catalysoft.com/articles/ClassAwareTableModel.html
	if (getRowCount() == 0) return Object.class;

	Object aux = getValueAt(0, col);
	return aux == null ? Object.class : aux.getClass();
    }
}
