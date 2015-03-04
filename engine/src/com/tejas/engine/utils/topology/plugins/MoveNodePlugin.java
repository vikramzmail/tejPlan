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
import com.tejas.engine.utils.topology.plugins.JUNGCanvas;
import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.utils.topology.Link;
import com.tejas.engine.utils.topology.Node;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class MoveNodePlugin extends AbstractCanvasPlugin implements MouseListener, MouseMotionListener
{
    private ITopologyCallback callback;
    private Node startVertex;

    /**
     *
     * @param callback
     */
    public MoveNodePlugin(ITopologyCallback callback)
    {
	this(callback, MouseEvent.BUTTON1_MASK);
    }

    /**
     *
     * @param callback
     * @param modifiers
     */
    public MoveNodePlugin(ITopologyCallback callback, int modifiers)
    {
	super(modifiers);

	this.callback = callback;
	down = null;
	startVertex = null;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
	checkCanvas();
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
	checkCanvas();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
	checkCanvas();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
	checkCanvas();

	boolean accepted = checkModifiers(e);
	if (accepted == true)
	{
	    final VisualizationViewer<Node, Link> vv = (VisualizationViewer<Node, Link>) e.getSource();
	    final Point2D p = e.getPoint();
	    GraphElementAccessor<Node, Link> pickSupport = vv.getPickSupport();
	    if (pickSupport != null)
	    {
		final Node vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());

		if (vertex != null)
		{
		    down = e.getPoint();
		    callback.showNode(vertex.getId());
		    startVertex = vertex;
		    e.consume();
		}
		else
		{
		    callback.resetView();
		}
	    }
	}
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
	checkCanvas();

	startVertex = null;
	down = null;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
	checkCanvas();

	VisualizationViewer<Node, Link> vv = (VisualizationViewer) e.getSource();
	if (startVertex != null)
	{
	    Point p = e.getPoint();
	    Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
	    Point2D graphDown = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(down);
	    Layout<Node, Link> layout = vv.getGraphLayout();
	    double dx = graphPoint.getX() - graphDown.getX();
	    double dy = graphPoint.getY() - graphDown.getY();
	    Point2D vp = layout.transform(startVertex);
	    vp.setLocation(vp.getX() + dx, vp.getY() + dy);
	    layout.setLocation(startVertex, vp);
	    callback.moveNode(startVertex.getId(), ((JUNGCanvas) getCanvas()).getRealXYCoordinates(p));
	    e.consume();
	    vv.repaint();
	    down = p;
	}
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
	checkCanvas();
    }
}