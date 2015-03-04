package com.tejas.engine.utils.topology.plugins;


import com.tejas.engine.utils.topology.plugins.ITopologyCanvasPlugin;
import com.tejas.engine.interfaces.networkDesign.NetPlan;

import java.awt.geom.Point2D;
import javax.swing.JComponent;

/**
 *
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public interface ITopologyCanvas
{
    /**
     * Adds a new unidirectional link between two nodes.
     * 
     * @param originNodeId Origin node identifier
     * @param destinationNodeId Destinatin node identifier
     * @return Link identifier
     * @since 0.2.3
     */
    public int addLink(int originNodeId, int destinationNodeId);

    /**
     * Adds a new node.
     * 
     * @param pos   Node position
     * @param label Node label (it can be null)
     * @return Node identifier
     * @since 0.2.3
     */
    public int addNode(Point2D pos, String label);
    
    /**
     * Adds a new plugin to the canvas.
     * 
     * @param plugin Plugin
     * @since 0.2.3
     */
    public void addPlugin(ITopologyCanvasPlugin plugin);
    
    /**
     * Returns the top-level component of the canvas.
     * 
     * @return Top-level component of the canvas
     * @since 0.2.3
     */
    public JComponent getComponent();

    /**
     * Removes a node from the canvas, and all its associated incoming/outgoing links.
     * 
     * @param nodeId Node identifier
     * @since 0.2.3
     */
    public void removeNode(int nodeId);
    
    /**
     * Removes a link from the canvas.
     * 
     * @param linkId Link identifier
     * @since 0.2.3
     */
    public void removeLink(int linkId);

    /**
     * Refreshes the canvas.
     * 
     * @since 0.2.3
     */
    public void refresh();
    
    /**
     * Resets the graph.
     * 
     * @since 0.2.3
     */
    public void reset();
    
    /**
     * Removes a plugin from the canvas.
     * 
     * @param plugin Plugin
     * @since 0.2.3
     */
    public void removePlugin(ITopologyCanvasPlugin plugin);
    
    /**
     * Resets the elements down.
     * 
     * @since 0.2.3
     */
    public void resetDownState();
    
    /**
     * Resets the emphasized elements.
     * 
     * @since 0.2.3
     */
    public void resetPickedState();
    
    /**
     * Sets a link to down state.
     * 
     * @param linkId Link identifier
     * @since 0.2.3
     */
    public void setLinkDown(int linkId);
    
    /**
     * Sets a node to down state.
     * 
     * @param nodeId Node identifier
     * @since 0.2.3
     */
    public void setNodeDown(int nodeId);
            
    /**
     * Sets a link to up state.
     * 
     * @param linkId Link identifier
     * @since 0.2.3
     */
    public void setLinkUp(int linkId);
    
    /**
     * Sets a node to up state.
     *
     * @param nodeId Node identifier
     * @since 0.2.3
     */
    public void setNodeUp(int nodeId);

    /**
     * Emphasizes a link.
     * 
     * @param linkId Link identifier
     * @since 0.2.3
     */
    public void showLink(int linkId);

    /**
     * Emphasizes a node.
     * 
     * @param nodeId Node identifier
     * @since 0.2.3
     */
    public void showNode(int nodeId);
    
    /**
     * Emphasizes a set of nodes.
     * 
     * @param nodeIds Node identifiers
     * @since 0.2.0
     */
    public void showNodes(int[] nodeIds);
    
    /**
     * Emphasizes a set of nodes and/or links.
     * 
     * @param nodeIds Node identifiers (may be null)
     * @param linkIds Link identifiers (may be null)
     * @since 0.2.3
     */
    public void showNodesAndLinks(int[] nodeIds, int[] linkIds);
    
    /**
     * Shows/hide node names.
     * 
     * @since 0.2.3
     */
    public void showNodeName();
    
    /**
     * Emphasizes a set of links.
     * 
     * @param linkIds Links identifiers
     * @since 0.2.3
     */
    public void showRoute(int[] linkIds);
    
    /**
     * Emphasizes two sets of links. The first one will show a heavy line, while
     * the second one will show a heavy dashed line.
     * 
     * @param primaryRouteLinks Link identifiers for the first set of links
     * @param secondaryRouteLinks Link identifiers for the second set of links
     * @since 0.2.3
     */
    public void showRoutes(int[] primaryRouteLinks, int[] secondaryRouteLinks);
    
    /**
     * Takes a snapshot of the canvas.
     * 
     * @since 0.2.3
     */
    public void takeSnapshot();
    
    /**
     * Refresh the canvas with a new network design.
     * 
     * @param netPlan Network design
     * @since 0.2.3
     */
    public void updateTopology(NetPlan netPlan);

    /**
     * Makes zoom-all from the center of the view.
     * 
     * @since 0.2.3
     */
    public void zoomAll();

    /**
     * Makes zoom-in from the center of the view.
     * 
     * @since 0.2.3
     */
    public void zoomIn();

    /**
     * Makes zoom-all from the center of the view.
     * 
     * @since 0.2.3
     */
    public void zoomOut();
}
