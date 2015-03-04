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

import java.awt.Dimension;
import javax.swing.JComboBox;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class WiderJComboBox extends JComboBox
{
    private static final long serialVersionUID = 1L;

    private boolean layingOut = false;

    /**
     *
     */
    public WiderJComboBox()
    {
    }


    @Override
    public void doLayout()
    {
	try
	{
	    layingOut = true;
	    super.doLayout();
	}
	finally
	{
	    layingOut = false;
	}
    }

    @Override
    public Dimension getSize()
    {
	Dimension dim = super.getSize();
	if (!layingOut)
	{
	    dim.width = Math.max(dim.width, getPreferredSize().width);
	}
	return dim;
    }
}