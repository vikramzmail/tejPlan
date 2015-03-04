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

import com.tejas.engine.utils.topology.plugins.AbstractCanvasPopupMousePlugin;
import com.tejas.engine.utils.topology.plugins.JUNGCanvas;
import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.utils.topology.Link;
import com.tejas.engine.utils.topology.Node;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class PopupEditGraphPlugin extends AbstractCanvasPopupMousePlugin
{
    private ITopologyCallback callback;

    /**
     * Default constructor.
     * 
     * @param callback Reference to the class handling change events.
     * @since 0.2.0
     */
    public PopupEditGraphPlugin(ITopologyCallback callback)
    {
	this(callback, MouseEvent.BUTTON3_MASK);
    }

    /**
     * Constructor that allows to change modifiers to activate its usage.
     * 
     * @param callback Reference to the class handling change events.
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.2.0
     */
    public PopupEditGraphPlugin(ITopologyCallback callback, int modifiers)
    {
	super(modifiers);
	this.callback = callback;
    }

    @Override
    protected void handlePopup(MouseEvent e)
    {
	checkCanvas();

	final VisualizationViewer<Node, Link> vv = (VisualizationViewer<Node, Link>) e.getSource();
        final Point2D p = e.getPoint();
        GraphElementAccessor<Node,Link> pickSupport = vv.getPickSupport();
        if (pickSupport != null)
        {
            Graph<Node,Link> graph = vv.getModel().getGraphLayout().getGraph();

            final Node vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            final Link edge = pickSupport.getEdge(vv.getModel().getGraphLayout(), p.getX(), p.getY());

	    JPopupMenu popup = new JPopupMenu();

	    if (vertex != null)
	    {
		popup.add(new RemoveNodeAction("Remove node", vertex));

		int N = graph.getVertexCount();

		if (N > 1)
		{
		    popup.addSeparator();
		    JMenu unidirectionalMenu = new JMenu("Create unidirectional link");
		    JMenu bidirectionalMenu = new JMenu("Create bidirectional link");

		    Iterator<Node> it = graph.getVertices().iterator();
		    while(it.hasNext())
		    {
			Node aux = it.next();
			if (aux.getId() == vertex.getId()) continue;

			AbstractAction unidirectionalAction = new AddUnidirectionalLinkAction(String.format("%s (%d) => %s (%d)", vertex.toString(), vertex.getId(), aux.toString(), aux.getId()), vertex);

			unidirectionalAction.putValue("nodeId", aux.getId());
			unidirectionalMenu.add(unidirectionalAction);

			AbstractAction bidirectionalAction = new AddBidirectionalLinkAction(String.format("%s (%d) <=> %s (%d)", vertex.toString(), vertex.getId(), aux.toString(), aux.getId()), vertex);
			bidirectionalAction.putValue("nodeId", aux.getId());
			bidirectionalMenu.add(bidirectionalAction);

		    }
		    popup.add(unidirectionalMenu);
		    popup.add(bidirectionalMenu);
		}
	    }
	    else if (edge != null)
	    {
		popup.add(new RemoveLinkAction("Remove link", edge));
	    }
	    else
            {
		popup.add(new AddNodeAction("Add node here", p));
            }

	    popup.show(vv, e.getX(), e.getY());
        }
    }

    private class RemoveNodeAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        private final Node vertex;

        public RemoveNodeAction(String name, Node vertex)
        {
            super(name);
            this.vertex = vertex;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            callback.removeNode(vertex.getId());
            getCanvas().resetPickedState();
        }
    }

    private class AddUnidirectionalLinkAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        private final Node vertex;

        public AddUnidirectionalLinkAction(String name, Node vertex)
        {
            super(name);
            this.vertex = vertex;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            callback.addLink(vertex.getId(), Integer.parseInt(this.getValue("nodeId").toString()));
            getCanvas().resetPickedState();
        }
    }

    private class AddBidirectionalLinkAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        private final Node vertex;

        public AddBidirectionalLinkAction(String name, Node vertex)
        {
            super(name);
            this.vertex = vertex;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            callback.addLink(vertex.getId(), Integer.parseInt(this.getValue("nodeId").toString()));
            callback.addLink(Integer.parseInt(this.getValue("nodeId").toString()), vertex.getId());
            getCanvas().resetPickedState();
        }
    }

    private class RemoveLinkAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        private final Link edge;

        public RemoveLinkAction(String name, Link edge)
        {
            super(name);
            this.edge = edge;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            callback.removeLink(edge.getId());
            getCanvas().resetPickedState();
        }
    }

    private class AddNodeAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        private final Point2D p;

        public AddNodeAction(String name, Point2D p)
        {
            super(name);
            this.p = p;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            callback.addNode(((JUNGCanvas) getCanvas()).getRealXYCoordinates(p));
            getCanvas().resetPickedState();
        }
    }
}
