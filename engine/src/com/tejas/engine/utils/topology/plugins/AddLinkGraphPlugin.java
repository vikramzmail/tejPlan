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
import edu.uci.ics.jung.visualization.VisualizationServer.Paintable;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.util.ArrowFactory;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class AddLinkGraphPlugin extends AbstractCanvasPlugin implements MouseListener, MouseMotionListener
{
    private ITopologyCallback callback;
    private Node startVertex;
    private Paintable edgePaintable;
    private Paintable arrowPaintable;
    private CubicCurve2D rawEdge;
    private Shape edgeShape;
    private Shape rawArrowShape;
    private Shape arrowShape;
    private int modifiersBidirectional;

    /**
     * Default constructor. By default the modifier to add unidirectional links
     * is the left button of the mouse, while to add bidirectional links required
     * to hold the SHIFT key also.
     * 
     * @param callback Topology callback
     * @since 0.2.0
     */
    public AddLinkGraphPlugin(ITopologyCallback callback)
    {
	this(callback, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK);
    }

    /**
     * Constructor that allows to set the modifiers to activate both
     * 'add unidirectional link' and 'add bidirectional link' modes.
     * 
     * @param callback Topology callback
     * @param modifiers Modifier to activate the plugin to add unidirectional links
     * @param modifiersBidirectional Modifier to activate the plugin to add bidirectional links
     * @since 0.2.0
     */
    public AddLinkGraphPlugin(ITopologyCallback callback, int modifiers, int modifiersBidirectional)
    {
	super(modifiers);

	this.modifiersBidirectional = modifiersBidirectional;
	this.callback = callback;
	down = null;
	startVertex = null;
	rawEdge = new CubicCurve2D.Float();
	rawEdge.setCurve(0.0f, 0.0f, 0.33f, 100, .66f, -50, 1.0f, 0.0f);
	rawArrowShape = ArrowFactory.getNotchedArrow(20, 16, 8);
	edgePaintable = new EdgePaintable();
	arrowPaintable = new ArrowPaintable();
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
	checkCanvas();

	if (startVertex != null)
	{
	    VisualizationViewer<Node, Link> vv = (VisualizationViewer<Node, Link>) e.getSource();
	    transformArrowShape(down, e.getPoint());
	    transformEdgeShape(down, e.getPoint());
	    e.consume();
	    vv.repaint();
	}
    }

    @Override
    public boolean checkModifiers(MouseEvent e)
    {
	return (e.getModifiers() == modifiers) || e.getModifiers() == modifiersBidirectional;
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

		startVertex = null;
		down = null;

		if (vertex != null)
		{
		    startVertex = vertex;
		    down = e.getPoint();
		    transformEdgeShape(down, down);
		    vv.addPostRenderPaintable(edgePaintable);
		    transformArrowShape(down, e.getPoint());
		    vv.addPostRenderPaintable(arrowPaintable);
		    e.consume();
		}
	    }
	}
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
	checkCanvas();

	if (startVertex != null)
	{
	    VisualizationViewer<Node, Link> vv = (VisualizationViewer<Node, Link>) e.getSource();
	    Point2D p = e.getPoint();
	    GraphElementAccessor<Node, Link> pickSupport = vv.getPickSupport();
	    if (pickSupport != null)
	    {
		Node vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());

		vv.removePostRenderPaintable(edgePaintable);
		vv.removePostRenderPaintable(arrowPaintable);

		if (vertex != null && !startVertex.equals(vertex))
		{
		    if ((e.getModifiers() & MouseEvent.SHIFT_MASK) == 0)
		    {
			callback.addLink(startVertex.getId(), vertex.getId());
		    }
		    else
		    {
			callback.addLink(startVertex.getId(), vertex.getId());
			callback.addLink(vertex.getId(), startVertex.getId());
		    }
		}

		startVertex = null;
		down = null;

		callback.resetView();
		vv.repaint();
	    }
	}
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
	checkCanvas();
        
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
                
                if (vertex != null)
                {
                    callback.showNode(vertex.getId());
                    e.consume();
                }
                else if (edge != null)
                {
                    callback.showLink(edge.getId());
                    e.consume();
                }
	    }
	}
        
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
	checkCanvas();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
	checkCanvas();
    }

    private void transformArrowShape(Point2D down, Point2D out)
    {
	float x1 = (float) down.getX();
	float y1 = (float) down.getY();
	float x2 = (float) out.getX();
	float y2 = (float) out.getY();

	AffineTransform xform = AffineTransform.getTranslateInstance(x2, y2);

	float dx = x2 - x1;
	float dy = y2 - y1;
	float thetaRadians = (float) Math.atan2(dy, dx);
	xform.rotate(thetaRadians);
	arrowShape = xform.createTransformedShape(rawArrowShape);
    }

    private void transformEdgeShape(Point2D down, Point2D out)
    {
	float x1 = (float) down.getX();
	float y1 = (float) down.getY();
	float x2 = (float) out.getX();
	float y2 = (float) out.getY();

	AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

	float dx = x2 - x1;
	float dy = y2 - y1;
	float thetaRadians = (float) Math.atan2(dy, dx);
	xform.rotate(thetaRadians);
	float dist = (float) Math.sqrt(dx * dx + dy * dy);
	xform.scale(dist / rawEdge.getBounds().getWidth(), 1.0);
	edgeShape = xform.createTransformedShape(rawEdge);
    }

    private class ArrowPaintable implements Paintable
    {
	@Override
	public void paint(Graphics g)
	{
	    if (!(g instanceof Graphics2D)) throw new IllegalArgumentException("A Graphics2D object is required");

	    if (arrowShape != null)
	    {
		Color oldColor = g.getColor();
		g.setColor(Color.black);
		((Graphics2D) g).fill(arrowShape);
		g.setColor(oldColor);
	    }
	}

	@Override
	public boolean useTransform() { return false; }
    }

    private class EdgePaintable implements Paintable
    {
	@Override
	public void paint(Graphics g)
	{
	    if (!(g instanceof Graphics2D)) throw new IllegalArgumentException("A Graphics2D object is required");

	    if (edgeShape != null)
	    {
		Color oldColor = g.getColor();
		g.setColor(Color.black);
		((Graphics2D) g).draw(edgeShape);
		g.setColor(oldColor);
	    }
	}

	@Override
	public boolean useTransform() { return false; }
    }
}