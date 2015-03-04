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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import com.tejas.engine.utils.topology.plugins.AbstractCanvasPlugin;

/**
 *
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class AbstractCanvasPopupMousePlugin extends AbstractCanvasPlugin implements MouseListener
{
    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public AbstractCanvasPopupMousePlugin()
    {
        this(MouseEvent.BUTTON3_MASK);
    }

    /**
     * Constructor that allows to change modifiers to activate its usage.
     * 
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.2.0
     */
    public AbstractCanvasPopupMousePlugin(int modifiers)
    {
        super(modifiers);
    }

    @Override
    public final void mouseReleased(MouseEvent e)
    {
	checkCanvas();

	boolean accepted = checkModifiers(e);

	if(accepted == true)
	{
	    handlePopup(e);
	    e.consume();
	}
    }

    /**
     * Handles the mouse event.
     * 
     * @param e Mouse event
     * @since 0.2.0
     */
    protected abstract void handlePopup(MouseEvent e);

    @Override
    public final void mousePressed(MouseEvent e)
    {
    }

    @Override
    public final void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public final void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public final void mouseExited(MouseEvent e)
    {
    }
}
