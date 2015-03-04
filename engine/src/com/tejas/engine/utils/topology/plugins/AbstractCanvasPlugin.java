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

package com.tejas.engine.utils.topology.plugins;

import com.tejas.engine.utils.topology.plugins.ITopologyCanvas;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvasPlugin;

import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class AbstractCanvasPlugin extends AbstractGraphMousePlugin implements ITopologyCanvasPlugin
{
    /**
     * Reference to the topology canvas.
     * 
     * @since 0.2.3
     */
    protected ITopologyCanvas canvas = null;

    /**
     * Default constructor.
     * 
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.2.3
     */
    public AbstractCanvasPlugin(int modifiers)
    {
	super(modifiers);
    }

    /**
     * Checks a canvas has been assigned to the plugin.
     * 
     * @since 0.2.3
     */
    public void checkCanvas()
    {
	if (canvas == null) throw new RuntimeException("No canvas attached!");
    }

    /**
     * Sets the canvas for this plugin.
     * 
     * @param canvas Reference to the canvas
     * @since 0.2.3
     */
    public void setCanvas(ITopologyCanvas canvas)
    {
	this.canvas = canvas;
    }

    /**
     * Returns a reference to the canvas.
     * 
     * @return Reference to the canvas
     * @since 0.2.3
     */
    public ITopologyCanvas getCanvas()
    {
        checkCanvas();
	return canvas;
    }
}