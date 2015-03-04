package com.tejas.engine.utils.topology.plugins;

import com.tejas.engine.utils.topology.plugins.AbstractCanvasPlugin;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvas;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvasPlugin;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ImageUtils;
import com.tejas.engine.utils.topology.Link;
import com.tejas.engine.utils.topology.Node;

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
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import static edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position.CNTR;
import static edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position.NE;
import edu.uci.ics.jung.visualization.transform.BidirectionalTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.transform.shape.ShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.TransformingGraphics;
import edu.uci.ics.jung.visualization.util.ArrowFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

/**
 * Topology canvas using JUNG library [<a href='#jung'>JUNG</a>].
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 * @see <a name='jung' /><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 */
public class JUNGCanvas implements ITopologyCanvas
{
    private JFileChooser fc;
    private boolean showNodeName;
    private final double NODE_SIZE = 30;
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
    
    /**
     * Default constructor.
     * 
     * @since 0.2.3
     */
    public JUNGCanvas()
    {
        xmin = 0;
        ymin = 0;
        scale = 1;
        
	showNodeName = true;

	backupPath = new HashSet<Link>();

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
	showNodeName();

	gm = new PluggableGraphMouse();

	vv.setGraphMouse(gm);

	scalingControl = new CrossoverScalingControl();
        
	ITopologyCanvasPlugin scalingPlugin = new ScalingCanvasPlugin(scalingControl, MouseEvent.NOBUTTON);
	addPlugin(scalingPlugin);
        
	vv.setBackground(new Color(212, 208, 200));

        reset();
    }
    
    private static class ScalingCanvasPlugin extends ScalingGraphMousePlugin implements ITopologyCanvasPlugin
    {
        public ScalingCanvasPlugin(ScalingControl scaler, int modifiers)
        {
            super(scaler, modifiers);
        }
    }
    
    @Override
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

    @Override
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
    
    @Override
    public void addPlugin(ITopologyCanvasPlugin plugin)
    {
	if (plugin instanceof AbstractCanvasPlugin)
        {
	    ((AbstractCanvasPlugin) plugin).setCanvas(this);
        }
        
        if (plugin instanceof GraphMousePlugin)
            gm.add((GraphMousePlugin) plugin);
        else
            throw new RuntimeException("Plugin is not valid for JUNG canvas");
    }
    
    @Override
    public JComponent getComponent() { return vv; }

    /**
     * Returns the real coordinates in the topology for a given screen point.
     * 
     * @param screenPoint Screen location
     * @return Coordinates in the topology system for the screen point
     * @since 0.2.3
     */
    public Point2D getRealXYCoordinates(Point2D screenPoint)
    {
	Point2D scaledPosition = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(screenPoint);

	double x = xmin + (scaledPosition.getX() - xmin) / scale;
	double y = ymin + (-scaledPosition.getY() - ymin) / scale;

	return new Point2D.Double(x, y);
    }
    
    @Override
    public void refresh() { vv.repaint(); }

    @Override
    public void removePlugin(ITopologyCanvasPlugin plugin)
    {
        if (plugin instanceof GraphMousePlugin)
            gm.remove((GraphMousePlugin) plugin);
    }

    @Override
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

    @Override
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
		while (linkIt.hasNext()) removeLink(linkIt.next().getId(), false);

		linkIt = g.getInEdges(aux).iterator();
		while (linkIt.hasNext()) removeLink(linkIt.next().getId(), false);

		it.remove();
		g.removeVertex(aux);
	    }
	    else if (aux.getId() > nodeId)
	    {
		aux.setId(aux.getId() - 1);
	    }
	}
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

	if (alsoFromGraph) refresh();
    }
    
    @Override
    public void removeLink(int linkId)
    {
	removeLink(linkId, true);
    }

    @Override
    public void resetDownState()
    {
        nodesDown.clear();
        linksDown.clear();
    }
    
    @Override
    public void resetPickedState()
    {
	backupPath.clear();

	vv.getPickedVertexState().clear();
	vv.getPickedEdgeState().clear();
    }

    @Override
    public void setNodeDown(int nodeId)
    {
        nodesDown.add(nodeTable.get(nodeId));
    }

    @Override
    public void setLinkDown(int linkId)
    {
        linksDown.add(linkTable.get(linkId));
    }

    @Override
    public void setNodeUp(int nodeId)
    {
        nodesDown.remove(nodeTable.get(nodeId));
    }

    @Override
    public void setLinkUp(int linkId)
    {
        linksDown.remove(linkTable.get(linkId));
    }

    private void setScale(double xmin, double ymin, double scale)
    {
	this.xmin = xmin;
	this.ymin = ymin;
	this.scale = scale;
    }
    
    @Override
    public void showLink(int linkId) { showNodesAndLinks(null, new int[] { linkId }); }

    @Override
    public void showNode(int nodeId) { showNodesAndLinks(new int[] { nodeId }, null); }
    
    @Override
    public void showNodes(int[] nodeIds) { showNodesAndLinks(nodeIds, null); }
    
    @Override
    public void showNodesAndLinks(int[] nodeIds, int[] linkIds)
    {
	resetPickedState();

        if (nodeIds != null)
        {
            for (int nodeId : nodeIds)
            {
                Node aux = nodeTable.get(nodeId);
                vv.getPickedVertexState().pick(aux, true);
            }
        }
        
        if (linkIds != null)
        {
            for (int linkId : linkIds)
            {
                Link aux = linkTable.get(linkId);
                vv.getPickedEdgeState().pick(aux, true);
            }
        }
    }


    @Override
    public void showNodeName()
    {
	showNodeName = showNodeName ? false : true;
	refresh();
    }
    
    @Override
    public void showRoute(int[] linkIds) { showNodesAndLinks(null, linkIds); }

    @Override
    public void showRoutes(int[] primaryRouteLinks, int[] secondaryRouteLinks)
    {
	resetPickedState();

	for(int linkId : secondaryRouteLinks)
	    backupPath.add(linkTable.get(linkId));

	for(int linkId : primaryRouteLinks)
	    backupPath.remove(linkTable.get(linkId));

	for(int linkId : primaryRouteLinks)
	    vv.getPickedEdgeState().pick(linkTable.get(linkId), true);

	for(int linkId : secondaryRouteLinks)
	    vv.getPickedEdgeState().pick(linkTable.get(linkId), true);
    }

    @Override
    public void takeSnapshot()
    {
	Dimension currentSize = vv.getSize();

//        Dimension desiredSize = vv.getSize();
//        VisualizationImageServer<Node, Link> vi = new VisualizationImageServer<Node, Link>(l, new Dimension(desiredSize.width, desiredSize.height));
//        BufferedImage bi = ImageUtils.imageToBufferedImage(vi.getImage(new Point2D.Double(0, 0), desiredSize));

        BufferedImage bi = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
	Graphics2D graphic = bi.createGraphics();
	graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	graphic.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	graphic.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

	boolean db = vv.isDoubleBuffered();
	vv.setDoubleBuffered(false);
        
        Color bgColor = vv.getBackground();

        vv.setBackground(Color.WHITE);
	vv.paint(graphic);
        vv.setBackground(bgColor);
        
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
            File f = fc.getSelectedFile();
            ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
	}

	graphic.dispose();
    }

    @Override
    public void updateTopology(NetPlan netPlan)
    {
	reset();

	double[][] auxNodeXYPositionTable = netPlan.getNodeXYPositionTable();
	String[] auxNodeNameVector = netPlan.getNodeNameVector();
	int[][] auxLinkTable = netPlan.getLinkTable();
        
	int N = auxNodeXYPositionTable.length;
	if (N == 0) return;

	int E = auxLinkTable.length;

	double aux_xmax = -Double.MAX_VALUE;
	double aux_xmin = Double.MAX_VALUE;
	double aux_ymax = -Double.MAX_VALUE;
	double aux_ymin = Double.MAX_VALUE;

	for(int nodeId = 0; nodeId < N; nodeId++)
	{
	    if (auxNodeXYPositionTable[nodeId][0] < aux_xmin) aux_xmin = auxNodeXYPositionTable[nodeId][0];
	    if (auxNodeXYPositionTable[nodeId][0] > aux_xmax) aux_xmax = auxNodeXYPositionTable[nodeId][0];
	    if (auxNodeXYPositionTable[nodeId][1] < aux_ymin) aux_ymin = auxNodeXYPositionTable[nodeId][1];
	    if (auxNodeXYPositionTable[nodeId][1] > aux_ymax) aux_ymax = auxNodeXYPositionTable[nodeId][1];
	}

	double scaleX = aux_xmax - aux_xmin != 0 ? 1 / (aux_xmax - aux_xmin) : 1;
	double scaleY = aux_ymax - aux_ymin != 0 ? 1 / (aux_ymax - aux_ymin) : 1;
	double aux_scale = Math.max(scaleX, scaleY);

	setScale(aux_xmin, aux_ymin, aux_scale);

	for (int nodeId = 0; nodeId < N; nodeId++)
	    addNode(new Point2D.Double(auxNodeXYPositionTable[nodeId][0], auxNodeXYPositionTable[nodeId][1]), auxNodeNameVector[nodeId]);

	for (int linkId = 0; linkId < E; linkId++)
	    addLink(auxLinkTable[linkId][0], auxLinkTable[linkId][1]);

	refresh();
    }

    @Override
    public void zoomAll()
    {
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

    @Override
    public void zoomIn() { zoomIn(vv.getCenter()); }

    private void zoomIn(Point2D point) { scalingControl.scale(vv, scaleIn, point); }
    
    @Override
    public void zoomOut() { zoomOut(vv.getCenter()); }

    private void zoomOut(Point2D point) { scalingControl.scale(vv, scaleOut, point); }
    
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

	    Point p = getAnchorPoint(bounds, d, Renderer.VertexLabel.Position.CNTR);
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

		p = getAnchorPoint(bounds, d, Renderer.VertexLabel.Position.NE);
		g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);
	    }
	}

	@Override
	protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Renderer.VertexLabel.Position position)
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
