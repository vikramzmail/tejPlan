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

import com.tejas.engine.utils.topology.plugins.AbstractCanvasPlugin;
import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.utils.topology.Link;
import com.tejas.engine.utils.topology.Node;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

/**
 *
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class PanGraphPlugin extends AbstractCanvasPlugin implements MouseListener, MouseMotionListener
{
    private ITopologyCallback callback;
    private Point initialPoint;

    /**
     * Default constructor.
     * 
     * @param callback Topology callback listening plugin events
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.2.0
     */
    public PanGraphPlugin(ITopologyCallback callback, int modifiers)
    {
	super(modifiers);
        
        this.callback = callback;
        this.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
	down = null;
        initialPoint = null;
    }
    
    @Override
    public void mousePressed(MouseEvent e)
    {
	boolean accepted = checkModifiers(e);
	if (accepted)
	{
	    VisualizationViewer<Node, Link> vv = (VisualizationViewer<Node, Link>) e.getSource();
	    Point2D p = e.getPoint();
	    GraphElementAccessor<Node, Link> pickSupport = vv.getPickSupport();
	    if (pickSupport != null)
	    {
		final Node vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
		final Link edge = pickSupport.getEdge(vv.getModel().getGraphLayout(), p.getX(), p.getY());

		if (vertex == null && edge == null)
		{
		    down = e.getPoint();
                    initialPoint = e.getPoint();
		    vv.setCursor(cursor);

                    e.consume();
		}
	    }
	}
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
	if (down != null)
	{
	    VisualizationViewer<?, ?> vv = (VisualizationViewer<?, ?>) e.getSource();
	    MutableTransformer viewTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW);
	    vv.setCursor(cursor);
	    try
	    {
		Point2D q = viewTransformer.inverseTransform(down);
		Point2D p = viewTransformer.inverseTransform(e.getPoint());
		float dx = (float) (p.getX() - q.getX());
		float dy = (float) (p.getY() - q.getY());

		viewTransformer.translate(dx, dy);
		down.x = e.getX();
		down.y = e.getY();
	    }
	    catch (RuntimeException ex)
	    {
		System.err.println("down = " + down + ", e = " + e);
		throw ex;
	    }

	    e.consume();
	}
    }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (initialPoint != null && initialPoint.equals(e.getPoint()))
            callback.resetView();
        
        VisualizationViewer<?,?> vv = (VisualizationViewer<?,?>)e.getSource();
        down = null;
        initialPoint = null;
        vv.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) { }
}
