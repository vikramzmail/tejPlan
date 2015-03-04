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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JSplitPane;

/**
 *
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ProportionalResizeJSplitPaneListener implements PropertyChangeListener
{
    @Override
    public void propertyChange(PropertyChangeEvent changeEvent)
    {
	JSplitPane src = (JSplitPane) changeEvent.getSource();
	String propertyName = changeEvent.getPropertyName();
	if (propertyName.equals(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY))
	{
	    if (src.getOrientation() == JSplitPane.HORIZONTAL_SPLIT)
		src.setResizeWeight(src.getDividerLocation() / src.getSize().getWidth());
	    else
		src.setResizeWeight(src.getDividerLocation() / src.getSize().getHeight());
	}
    }
}
