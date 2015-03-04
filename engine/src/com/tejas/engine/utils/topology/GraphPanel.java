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

package com.tejas.engine.utils.topology;

import com.tejas.engine.utils.topology.Link;
import com.tejas.engine.utils.topology.Node;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.GraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.transform.BidirectionalTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.transform.shape.ShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.TransformingGraphics;
import edu.uci.ics.jung.visualization.util.ArrowFactory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class GraphPanel extends JPanel
{
    private static final long serialVersionUID = 1L;

    private JFileChooser fc;
    private boolean showNodeName;
    private static double NODE_SIZE = 30;
    private final Graph<Node, Link> g;
    private final Layout<Node, Link> l;
    private final VisualizationViewer<Node, Link> vv;
    private final List<Node> nodeTable;
    private final List<Link> linkTable;
    private Set<Link> backupPath;
    private double xmin, ymin, scale;
    private final PluggableGraphMouse gm;
    private final static float scaleIn = 1.1f;
    private final static float scaleOut = 1/scaleIn;
    private final ScalingControl scalingControl;
    
    private final Set<Link> linksDown;
    private final Set<Node> nodesDown;
    
    @Override
    public void setOpaque(boolean isOpaque)
    {
        super.setOpaque(isOpaque);
        
        if (vv != null) vv.setOpaque(isOpaque);
    }
    
    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public GraphPanel()
    {
        xmin = 0;
        ymin = 0;
        scale = 1;
        
	showNodeName = true;

	backupPath = new HashSet<Link>();

	setLayout(new MigLayout("fill, insets 0 0 0 0"));

	nodeTable = new ArrayList<Node>();
	linkTable = new ArrayList<Link>();
        
        linksDown = new HashSet<Link>();
        nodesDown = new HashSet<Node>();

	g = new DirectedOrderedSparseMultigraph<Node, Link>();
	l = new StaticLayout<Node, Link>(g, new TransformerImpl());
	vv = new VisualizationViewer<Node, Link>(l);

	/*
	 * Customize the graph
	 */
	vv.getRenderContext().setVertexDrawPaintTransformer(new StateAwareTransformer(new ConstantTransformer(Color.BLACK)));
	vv.getRenderContext().setVertexFillPaintTransformer(new StateAwareTransformer(new PickableVertexPaintTransformer<Node>(vv.getPickedVertexState(), Color.BLACK, Color.BLUE)));
	vv.getRenderContext().setVertexFontTransformer(new ConstantTransformer(new Font("Helvetica", Font.BOLD, 11)));
	vv.getRenderContext().setVertexShapeTransformer(new Transformer<Node, Shape>()
	{

	    @Override
	    public Shape transform(Node i)
	    {
		double scaleFactor = vv.getPickedVertexState().isPicked(i) ? 1.2 : 1;
		return new Ellipse2D.Double(-scaleFactor * NODE_SIZE / 2, -scaleFactor * NODE_SIZE / 2, scaleFactor * NODE_SIZE, scaleFactor * NODE_SIZE);
	    }
	});

	vv.getRenderContext().setEdgeArrowStrokeTransformer(new Transformer<Link, Stroke>()
	{

	    @Override
	    public Stroke transform(Link i)
	    {
		return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1);
	    }
	});
	vv.getRenderContext().setEdgeArrowTransformer(new ConstantTransformer(ArrowFactory.getNotchedArrow(7, 10, 5)));
	vv.getRenderContext().setEdgeDrawPaintTransformer(new StateAwareTransformer(new PickableEdgePaintTransformer<Link>(vv.getPickedEdgeState(), Color.BLACK, Color.BLUE)));
	vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.5, .5));
//	vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());

// TODO: If parallel links, quad line; otherwise, straight line
//	vv.getRenderContext().setEdgeShapeTransformer(new AbstractEdgeShapeTransformer<Node,Link>() {
//
//	    @Override
//	    public Shape transform(Context<Graph<Node, Link>, Link> i)
//	    {
//		Link e = i.element;
//		Pair<Node> endpoints = i.graph.getEndpoints(e);
//
//		if (i.graph.findEdgeSet(endpoints.getFirst(), endpoints.getSecond()).size() > 1)
//		{
//		    return new EdgeShape.QuadCurve<Node, Link>().transform(i);
//		}
//		else
//		{
//		    return new EdgeShape.Line<Node, Link>().transform(i);
//		}
//	    }
//	});

//	vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
	vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<Link, Stroke>()
	{

	    @Override
	    public Stroke transform(Link i)
	    {
		if (backupPath.contains(i))
		    return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {10}, 0.0f);
		else
		    return new BasicStroke(vv.getPickedEdgeState().isPicked(i) ? 2 : 1);
	    }
	});

	vv.getRenderContext().setArrowDrawPaintTransformer(new StateAwareTransformer(new PickableEdgePaintTransformer<Link>(vv.getPickedEdgeState(), Color.BLACK, Color.BLUE)));
	vv.getRenderContext().setArrowFillPaintTransformer(new StateAwareTransformer(new PickableEdgePaintTransformer<Link>(vv.getPickedEdgeState(), Color.BLACK, Color.BLUE)));

	vv.getRenderer().setVertexLabelRenderer(new MyNodeLabelRenderer());

	showNodeName = true;
	toggleNodeLabel();

	gm = new PluggableGraphMouse();

	vv.setGraphMouse(gm);
        System.out.println("-" + vv + "-");
	add(vv, "grow");

	scalingControl = new CrossoverScalingControl();

	setBackground(new Color(212, 208, 200));

        reset();
    }
    
    @Override
    public void setBackground(Color bg)
    {
        super.setBackground(bg);
        
        if(vv != null)
            vv.setBackground(bg);
    }
    
    public void resetDownState()
    {
        nodesDown.clear();
        linksDown.clear();
    }
    
    public void setNodeDown(int nodeId)
    {
        nodesDown.add(nodeTable.get(nodeId));
    }

    public void setLinkDown(int linkId)
    {
        linksDown.add(linkTable.get(linkId));
    }
    
    public void setNodeUp(int nodeId)
    {
        nodesDown.remove(nodeTable.get(nodeId));
    }

    public void setLinkUp(int linkId)
    {
        linksDown.remove(linkTable.get(linkId));
    }

    private class StateAwareTransformer<Object, Paint> implements Transformer<Object, Paint>
    {
        private final Transformer<Object, Paint> transformer;
        
        public StateAwareTransformer(Transformer<Object, Paint> transformer)
        {
            this.transformer = transformer;
        }
        
        @Override
        public Paint transform(Object o)
        {
            if ( (o instanceof Node && nodesDown.contains((Node) o)) || (o instanceof Link && linksDown.contains((Link) o)) )
                return (Paint) Color.RED;
            
            return transformer.transform(o);
        }
    }

    /**
     * Returns a reference to the scaling control.
     * 
     * @return Reference to the scaling control
     * @since 0.2.0
     */
    public ScalingControl getScalingControl()
    {
	return scalingControl;
    }

    /**
     * Includes a new mouse plugin to the canvas.
     * 
     * @param plugin Plugin to be included
     * @since 0.2.0
     */
    public void addPlugin(GraphMousePlugin plugin)
    {
//	if (plugin instanceof AbstractGraphPanelMousePlugin)
//	    ((AbstractGraphPanelMousePlugin) plugin).setGraphPanel(this);

	gm.add(plugin);
    }

    /**
     * Toggles between hide and show node labels.
     * 
     * @since 0.2.0
     */
    public void toggleNodeLabel()
    {
	showNodeName = showNodeName ? false : true;
	refresh();
    }

    private void zoomIn(Point2D point) { scalingControl.scale(vv, scaleIn, point); }

    /**
     * Makes a zoom-in from the center of the view.
     * 
     * @since 0.2.0
     */
    public void zoomIn() { zoomIn(vv.getCenter()); }

    private void zoomOut(Point2D point) { scalingControl.scale(vv, scaleOut, point); }

    /**
     * Makes a zoom-out from the center of the view.
     * 
     * @since 0.2.0
     */
    public void zoomOut() { zoomOut(vv.getCenter()); }

    /**
     * Makes a zoom-all.
     * 
     * @since 0.2.0
     */
    public void zoomAll()
    {
        // Compute graph bounds
        Iterator<Node> it = g.getVertices().iterator();

	if (!it.hasNext()) return;

	double aux_xmax = Double.NEGATIVE_INFINITY;
	double aux_xmin = Double.POSITIVE_INFINITY;
	double aux_ymax = Double.NEGATIVE_INFINITY;
	double aux_ymin = Double.POSITIVE_INFINITY;

        while(it.hasNext())
	{
	    Point2D aux = it.next().getPosition();
	    if (aux_xmax < aux.getX()) aux_xmax = aux.getX();
	    if (aux_xmin > aux.getX()) aux_xmin = aux.getX();
	    if (aux_ymax < aux.getY()) aux_ymax = aux.getY();
	    if (aux_ymin > aux.getY()) aux_ymin = aux.getY();
	}

        // Scale to view the whole graph
	vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
	vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
        
        double PRECISION_FACTOR = 0.00001;

	Rectangle viewInLayoutUnits = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();
	float ratio_h = Math.abs(aux_xmax - aux_xmin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (aux_xmax-aux_xmin));
	float ratio_v = Math.abs(aux_ymax - aux_ymin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (aux_ymax-aux_ymin));
	float ratio = (float) (0.8 * Math.min(ratio_h, ratio_v));
	scalingControl.scale(vv, ratio, vv.getCenter());

	// Generate an auxiliary node at center of the graph
	Node aux = new Node(-1, new Point2D.Double((aux_xmin+aux_xmax)/2, (aux_ymin+aux_ymax)/2));
	Point2D q = l.transform(aux);
        Point2D lvc = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getCenter());
        double dx = (lvc.getX() - q.getX());
        double dy = (lvc.getY() - q.getY());
	vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);
    }

    /**
     * Resets the emphasizes elements.
     * 
     * @since 0.2.0
     */
    public void resetPickedState()
    {
	backupPath.clear();

	vv.getPickedVertexState().clear();
	vv.getPickedEdgeState().clear();
    }

    /**
     * Emphasizes a link.
     * 
     * @param linkId Link identifier
     * @since 0.2.0
     */
    public void showLink(int linkId)
    {
	resetPickedState();
	vv.getPickedEdgeState().pick(linkTable.get(linkId), true);
    }

    /**
     * Emphasizes a node.
     * 
     * @param nodeId Node identifier
     * @since 0.2.0
     */
    public void showNode(int nodeId)
    {
	resetPickedState();

	Node aux = nodeTable.get(nodeId);
	vv.getPickedVertexState().pick(aux, true);
    }

    /**
     * Emphasizes two sets of links. The first one will show a heavy line, while
     * the second one will show a heavy dashed line.
     * 
     * @param primaryRouteLinks Link identifiers for the first set of links
     * @param secondaryRouteLinks Link identifiers for the second set of links
     * @since 0.2.0
     */
    public void showRoutes(int[] primaryRouteLinks, int[] secondaryRouteLinks)
    {
	resetPickedState();

	for(int linkId : secondaryRouteLinks)
	{
	    Link aux = linkTable.get(linkId);
	    backupPath.add(aux);
	}

	for(int linkId : primaryRouteLinks)
	{
	    Link aux = linkTable.get(linkId);
	    backupPath.remove(aux);
	}

	for(int linkId : primaryRouteLinks)
	{
	    Link aux = linkTable.get(linkId);
	    vv.getPickedEdgeState().pick(aux, true);
	}

	for(int linkId : secondaryRouteLinks)
	{
	    Link aux = linkTable.get(linkId);
	    vv.getPickedEdgeState().pick(aux, true);
	}
    }

    /**
     * Emphasizes a set of links.
     * 
     * @param linkIds Links identifiers
     * @since 0.2.0
     */
    public void showRoute(int[] linkIds)
    {
	resetPickedState();

	for (int linkId : linkIds)
	{
	    Link aux = linkTable.get(linkId);
	    vv.getPickedEdgeState().pick(aux, true);
	}

    }

    /**
     * Emphasizes a set of nodes.
     * 
     * @param nodeIds Node identifiers
     * @since 0.2.0
     */
    public void showNodes(int[] nodeIds)
    {
	resetPickedState();

	for (int nodeId : nodeIds)
	{
	    Node aux = nodeTable.get(nodeId);
	    vv.getPickedVertexState().pick(aux, true);
	}
    }

    /**
     * Adds a new unidirectional link between two nodes.
     * 
     * @param originNodeId Origin node identifier
     * @param destinationNodeId Destinatin node identifier
     * @return Link identifier
     * @since 0.2.0
     */
    public int addLink(int originNodeId, int destinationNodeId)
    {
	return addLink(originNodeId, destinationNodeId, null);
    }

    private int addLink(int originNodeId, int destinationNodeId, String label)
    {
        if (originNodeId == destinationNodeId) throw new Net2PlanException("Self-links are not allowed");
        
	int linkId = linkTable.size();
	Link aux = new Link(linkId, nodeTable.get(originNodeId), nodeTable.get(destinationNodeId), label);
	linkTable.add(aux);
	g.addEdge(aux, aux.getOriginNode(), aux.getDestinationNode());

	return linkId;
    }

    /**
     * Adds a new node.
     * 
     * @param pos   Node position
     * @param label Node label (it can be null)
     * @return Node identifier
     * @since 0.2.0
     */
    public int addNode(Point2D pos, String label)
    {
	int nodeId = nodeTable.size();
	if (label == null) label = "Node " + nodeId;

	Point2D auxPos = new Point2D.Double(xmin + scale * (pos.getX() - xmin), ymin + scale * (pos.getY() - ymin));
	Node aux = new Node(nodeId, auxPos, label);
	nodeTable.add(aux);
	g.addVertex(aux);


	return nodeId;
    }

    /**
     * Refreshes the canvas.
     * 
     * @since 0.2.0
     */
    public void refresh()
    {
	vv.repaint();
    }

    private void removeLink(int linkId, boolean alsoFromGraph)
    {
	Iterator<Link> it = linkTable.iterator();
	while (it.hasNext())
	{
	    Link aux = it.next();
	    if (aux.getId() == linkId)
	    {
		it.remove();
		if (alsoFromGraph)
		{
		    g.removeEdge(aux);
		}
	    }
	    else if (aux.getId() > linkId)
	    {
		aux.setId(aux.getId() - 1);
	    }
	}

	if (alsoFromGraph)
	{
	    vv.repaint();
	}
    }

    /**
     * Removes a link from the graph.
     * 
     * @param linkId Link identifier
     * @since 0.2.0
     */
    public void removeLink(int linkId)
    {
	removeLink(linkId, true);
    }

    /**
     * Removes a node from the graph, and all its incoming/outgoing links associated.
     * 
     * @param nodeId Node identifier
     * @since 0.2.0
     */
    public void removeNode(int nodeId)
    {
	Iterator<Node> it = nodeTable.iterator();
	while (it.hasNext())
	{
	    Node aux = it.next();
	    if (aux.getId() == nodeId)
	    {
		Iterator<Link> linkIt;

		linkIt = g.getOutEdges(aux).iterator();
		while (linkIt.hasNext())
		{
		    removeLink(linkIt.next().getId(), false);
		}

		linkIt = g.getInEdges(aux).iterator();
		while (linkIt.hasNext())
		{
		    removeLink(linkIt.next().getId(), false);
		}

		it.remove();
		g.removeVertex(aux);
	    }
	    else if (aux.getId() > nodeId)
	    {
		aux.setId(aux.getId() - 1);
	    }
	}
    }

    /**
     * Resets the graph.
     * 
     * @since 0.2.0
     */
    public void reset()
    {
	setScale(0, 0, 1);

	Iterator<Link> linkIt = linkTable.iterator();
	while (linkIt.hasNext()) g.removeEdge(linkIt.next());

	Iterator<Node> nodeIt = nodeTable.iterator();
	while (nodeIt.hasNext()) g.removeVertex(nodeIt.next());

	nodeTable.clear();
	linkTable.clear();
        
        resetDownState();

	refresh();
    }

    /**
     * Returns the real coordinates in the topology for a given screen point.
     * 
     * @param screenPoint Screen location
     * @return Coordinates in the topology system for the screen point
     * @since 0.2.0
     */
    public Point2D getRealXYCoordinates(Point2D screenPoint)
    {
	Point2D scaledPosition = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(screenPoint);

	double x = xmin + (scaledPosition.getX() - xmin) / scale;
	double y = ymin + (-scaledPosition.getY() - ymin) / scale;

	return new Point2D.Double(x, y);
    }

    private void setScale(double xmin, double ymin, double scale)
    {
	this.xmin = xmin;
	this.ymin = ymin;
	this.scale = scale;
    }

    /**
     * Draws the given topology.
     * 
     * @param nodeXYPositionTable Set of nodes (first column: x-coordinate, second column: y-coordinate)
     * @param nodeName Node names
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @since 0.2.0
     */
    public void refreshTopology(double[][] nodeXYPositionTable, String[] nodeName, int[][] linkTable)
    {
	reset();

	int N = nodeXYPositionTable.length;
	if (N == 0) return;

	int E = linkTable.length;

	double aux_xmax = -Double.MAX_VALUE;
	double aux_xmin = Double.MAX_VALUE;
	double aux_ymax = -Double.MAX_VALUE;
	double aux_ymin = Double.MAX_VALUE;

	for(int nodeId = 0; nodeId < N; nodeId++)
	{
	    if (nodeXYPositionTable[nodeId][0] < aux_xmin) aux_xmin = nodeXYPositionTable[nodeId][0];
	    if (nodeXYPositionTable[nodeId][0] > aux_xmax) aux_xmax = nodeXYPositionTable[nodeId][0];
	    if (nodeXYPositionTable[nodeId][1] < aux_ymin) aux_ymin = nodeXYPositionTable[nodeId][1];
	    if (nodeXYPositionTable[nodeId][1] > aux_ymax) aux_ymax = nodeXYPositionTable[nodeId][1];
	}

	double scaleX = aux_xmax - aux_xmin != 0 ? 1 / (aux_xmax - aux_xmin) : 1;
	double scaleY = aux_ymax - aux_ymin != 0 ? 1 / (aux_ymax - aux_ymin) : 1;
	double aux_scale = Math.max(scaleX, scaleY);

	setScale(aux_xmin, aux_ymin, aux_scale);

	for (int nodeId = 0; nodeId < N; nodeId++)
	{
	    addNode(new Point2D.Double(nodeXYPositionTable[nodeId][0], nodeXYPositionTable[nodeId][1]), nodeName[nodeId]);
	}

	for (int linkId = 0; linkId < E; linkId++)
	{
	    addLink(linkTable[linkId][0], linkTable[linkId][1]);
	}

	refresh();
    }

    /**
     * Takes a snapshot of the topology and saves wherever user indicates.
     * 
     * @since 0.2.0
     */
    public void takeSnapshot()
    {
	Dimension currentSize = vv.getSize();
//	Dimension desiredSize = vv.getSize();

	BufferedImage bi = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
	Graphics2D graphic = bi.createGraphics();
	graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	graphic.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	graphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

	boolean db = vv.isDoubleBuffered();
	vv.setDoubleBuffered(false);
	vv.paint(graphic);
	vv.setDoubleBuffered(db);
        
        if (fc == null)
        {
            fc = new JFileChooser();
            FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files","png");
            fc.setFileFilter(pngFilter);
        }

	int s = fc.showSaveDialog(null);
	if (s == JFileChooser.APPROVE_OPTION)
	{
	    FileOutputStream out = null;
	    try
	    {
		File f = fc.getSelectedFile();
		out = new FileOutputStream(f.getPath());
		ImageIO.write(bi, "png", out);
		out.flush();
		out.close();
	    }
	    catch (FileNotFoundException ex)
	    {
	    }
	    catch (IOException ex)
	    {
		try {if (out != null) out.close(); }
		catch (IOException ex1)	{ }
	    }
	}

	graphic.dispose();
    }

    private class MyNodeLabelRenderer extends BasicVertexLabelRenderer<Node, Link>
    {
	@Override
	public void labelVertex(RenderContext<Node, Link> rc, Layout<Node, Link> layout, Node v, String label)
	{
	    Graph<Node, Link> graph = layout.getGraph();
	    if (rc.getVertexIncludePredicate().evaluate(Context.<Graph<Node, Link>, Node>getInstance(graph,v)) == false)
        	return;

	    Point2D pt = layout.transform(v);
	    pt = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, pt);

	    float x = (float) pt.getX();
	    float y = (float) pt.getY();

	    Component component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='white'>" + v.getId() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
	    GraphicsDecorator g = rc.getGraphicsContext();
	    Dimension d = component.getPreferredSize();
	    AffineTransform xform = AffineTransform.getTranslateInstance(x, y);

	    Shape shape = rc.getVertexShapeTransformer().transform(v);
	    shape = xform.createTransformedShape(shape);
	    if(rc.getGraphicsContext() instanceof TransformingGraphics)
	    {
		BidirectionalTransformer transformer = ((TransformingGraphics)rc.getGraphicsContext()).getTransformer();
		if(transformer instanceof ShapeTransformer)
		{
			ShapeTransformer shapeTransformer = (ShapeTransformer)transformer;
			shape = shapeTransformer.transform(shape);
		}
	    }

	    Rectangle2D bounds = shape.getBounds2D();

	    Point p = getAnchorPoint(bounds, d, Position.CNTR);
	    g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);

	    if (showNodeName)
	    {
		component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='black'>" + v.getLabel() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
		g = rc.getGraphicsContext();
		d = component.getPreferredSize();
		xform = AffineTransform.getTranslateInstance(x, y);

		shape = rc.getVertexShapeTransformer().transform(v);
		shape = xform.createTransformedShape(shape);
		if(rc.getGraphicsContext() instanceof TransformingGraphics)
		{
		    BidirectionalTransformer transformer = ((TransformingGraphics)rc.getGraphicsContext()).getTransformer();
		    if(transformer instanceof ShapeTransformer)
		    {
			    ShapeTransformer shapeTransformer = (ShapeTransformer)transformer;
			    shape = shapeTransformer.transform(shape);
		    }
		}

		bounds = shape.getBounds2D();

		p = getAnchorPoint(bounds, d, Position.NE);
		g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);
	    }
	}

	@Override
	protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Position position)
	{
	    double x;
	    double y;
	    int offset = 5;
	    switch(position)
	    {
		case NE:
			x = vertexBounds.getMaxX()-offset;
			y = vertexBounds.getMinY()+offset-labelSize.height;
			return new Point((int)x,(int)y);
		case CNTR:
			x = vertexBounds.getCenterX()-((double) labelSize.width/2);
			y = vertexBounds.getCenterY()-((double) labelSize.height/2);
			return new Point((int)x,(int)y);

		default:
			return new Point();
	    }

	}
    }

    private static class TransformerImpl implements Transformer<Node, Point2D>
    {
        @Override
        public Point2D transform(Node vertex)
        {
            Point2D pos = vertex.getPosition();
            return new Point2D.Double(pos.getX(), -pos.getY());
        }
    }
}