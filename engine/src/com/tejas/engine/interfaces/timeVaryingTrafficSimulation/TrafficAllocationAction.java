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

package com.tejas.engine.interfaces.timeVaryingTrafficSimulation;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.StringUtils;

import java.util.*;

/**
 * <p>Provides a set of actions to be returned by allocation algorithms. These actions are:</p>
 *
 * <ul>
 * <li>Add/modify/remove link/route/protection segment/SRG</li>
 * <li>Remove all links/routes/protection segments/SRGs</li>
 * <li>Add/remove protection segment to/from route backup list</li>
 * <li>Remove all protection segments from route backup list</li>
 * <li>Add/remove node/link to/from SRG</li>
 * <li>Remove all nodes/links from SRG</li>
 * </ul>
 *
 * <p>Although the <code>TrafficAllocationAction</code> class is common for all actions, its meaning (i.e.
 * action type) depends on the static method used to get an instance. Take a look
 * on the description of the static methods to obtain more information.</p>
 *
 * <p><b>Important</b>: Actions don't take effect within the algorithm. This means,
 * for example, that if you remove a link, methods like <code>netState.getNumberOfLinks()</code> will return
 * the previous value including that link, instead of the current according
 * to the actions. Network state is actually modified by the kernel after
 * the execution of the algorithm, so users should deal with current network state
 * by their own.</p>
 *
 * <p><b>Important</b>: Identifiers for new elements (i.e. link, route, protection segment, 
 * SRG) follow an incremental scheme, and no index is reused when some element is removed. 
 * The (first) next identifier of each element can be accessed via <code>netState.getNextXXXId())</code> method.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public class TrafficAllocationAction
{
    /**
     * Type of action.
     *
     * @since 0.2.1
     */
    public enum ActionType
    {
	/**
	 * Add link.
	 *
	 * @since 0.2.1
	 */
	ADD_LINK,
        
	/**
	 * Add link to SRG.
	 *
	 * @since 0.2.3
	 */
        ADD_LINK_TO_SRG,
        
	/**
	 * Add node to SRG.
	 *
	 * @since 0.2.3
	 */
        ADD_NODE_TO_SRG,

	/**
	 * Add protection segment.
	 *
	 * @since 0.2.1
	 */
	ADD_PROTECTION_SEGMENT,
        
	/**
	 * Add protection segment to route backup list.
	 *
	 * @since 0.2.3
	 */
        ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST,

	/**
	 * Add route.
	 *
	 * @since 0.2.1
	 */
	ADD_ROUTE,

	/**
	 * Add SRG.
	 *
	 * @since 0.2.3
	 */
	ADD_SRG,
        
	/**
	 * Modify link.
	 *
	 * @since 0.2.1
	 */
	MODIFY_LINK,

	/**
	 * Modify protection segment.
	 *
	 * @since 0.2.1
	 */
	MODIFY_PROTECTION_SEGMENT,

	/**
	 * Modify route traffic volume.
	 *
	 * @since 0.2.1
	 */
	MODIFY_ROUTE,

	/**
	 * Modify SRG.
	 *
	 * @since 0.2.3
	 */
	MODIFY_SRG,

        /**
	 * Remove all links.
	 *
	 * @since 0.2.3
	 */
	REMOVE_ALL_LINKS,

	/**
	 * Remove all protection segments.
	 *
	 * @since 0.2.3
	 */
	REMOVE_ALL_PROTECTION_SEGMENTS,

	/**
	 * Remove all protection segments from a route backup list.
	 *
	 * @since 0.2.3
	 */
        REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST,

	/**
	 * Remove all routes.
	 *
	 * @since 0.2.3
	 */
	REMOVE_ALL_ROUTES,

	/**
	 * Remove all SRGs.
	 *
	 * @since 0.2.3
	 */
	REMOVE_ALL_SRGS,
        
        /**
	 * Remove link.
	 *
	 * @since 0.2.1
	 */
	REMOVE_LINK,

        /**
	 * Remove link from SRG.
	 *
	 * @since 0.2.3
	 */
	REMOVE_LINK_FROM_SRG,
        
        /**
	 * Remove node from SRG.
	 *
	 * @since 0.2.3
	 */
	REMOVE_NODE_FROM_SRG,
        
	/**
	 * Remove protection segment.
	 *
	 * @since 0.2.1
	 */
	REMOVE_PROTECTION_SEGMENT,

	/**
	 * Remove all existing routes.
	 *
	 * @since 0.2.3
	 */
        REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST,
        
	/**
	 * Remove route.
	 *
	 * @since 0.2.1
	 */
	REMOVE_ROUTE,

	/**
	 * Remove SRG.
	 *
	 * @since 0.2.3
	 */
	REMOVE_SRG
    };

    private ActionType type;

    protected int addLink_originNodeId;
    protected int addLink_destinationNodeId;
    protected double addLink_linkCapacityInErlangs;
    protected double addLink_linkLengthInKm;
    protected Map<String, String> addLink_attributes;
        
    protected long[] addProtectionSegment_seqLinks;
    protected double addProtectionSegment_reservedBandwidthInErlangs;
    protected Map<String, String> addProtectionSegment_attributes;
        
    protected int addRoute_demandId;
    protected long[] addRoute_seqLinks;
    protected double addRoute_trafficVolumeInErlangs;
    protected Map<String, String> addRoute_attributes;

    protected int[] addSRG_nodeIds;
    protected long[] addSRG_linkIds;
    protected double addSRG_mttf;
    protected double addSRG_mttr;
    protected Map<String, String> addSRG_attributes;

    protected long modifyLink_linkId;
    protected double modifyLink_linkCapacityInErlangs;
    protected double modifyLink_linkLengthInKm;
    protected Map<String, String> modifyLink_attributes;

    protected long modifyProtectionSegment_segmentId;
    protected double modifyProtectionSegment_reservedBandwidthInErlangs;
    protected Map<String, String> modifyProtectionSegment_attributes;

    protected long modifyRoute_routeId;
    protected double modifyRoute_carriedTrafficInErlangs;
    protected Map<String, String> modifyRoute_attributes;

    protected long modifySRG_srgId;
    protected double modifySRG_mttf;
    protected double modifySRG_mttr;
    protected Map<String, String> modifySRG_attributes;

    protected long removeLink_linkId;
    protected long removeProtectionSegment_segmentId;
    protected long removeRoute_routeId;
    protected long removeSRG_srgId;
    
    protected long addProtectionSegmentToRouteBackupList_segmentId;
    protected long addProtectionSegmentToRouteBackupList_routeId;
    
    protected long removeProtectionSegmentFromRouteBackupList_segmentId;
    protected long removeProtectionSegmentFromRouteBackupList_routeId;
    
    protected long removeAllProtectionSegmentsFromRouteBackupList_routeId;
    
    protected long addLinkToSRG_linkId;
    protected long addLinkToSRG_srgId;
    protected int addNodeToSRG_nodeId;
    protected long addNodeToSRG_srgId;

    protected long removeLinkFromSRG_linkId;
    protected long removeLinkFromSRG_srgId;
    protected int removeNodeFromSRG_nodeId;
    protected long removeNodeFromSRG_srgId;
    
    private TrafficAllocationAction(ActionType type)
    {
        this.type = type;
        reset();
    }
    
    private void reset()
    {
        addLink_originNodeId = -1;
        addLink_destinationNodeId = -1;
        addLink_linkCapacityInErlangs = -1;
        addLink_linkLengthInKm = -1;
        addLink_attributes = new HashMap<String, String>();
        
        addProtectionSegment_seqLinks = new long[0];
        addProtectionSegment_reservedBandwidthInErlangs = -1;
        addProtectionSegment_attributes = new HashMap<String, String>();
        
        addProtectionSegmentToRouteBackupList_segmentId = -1;
        addProtectionSegmentToRouteBackupList_routeId = -1;
    
        addRoute_demandId = -1;
        addRoute_seqLinks = new long[0];
        addRoute_trafficVolumeInErlangs = -1;
        addRoute_attributes = new HashMap<String, String>();

        addSRG_nodeIds = new int[0];
        addSRG_linkIds = new long[0];
        addSRG_mttf = -1;
        addSRG_mttr = -1;
        addSRG_attributes = new HashMap<String, String>();

        modifyLink_linkId = -1;
        modifyLink_linkCapacityInErlangs = -1;
        modifyLink_linkLengthInKm = -1;
        modifyLink_attributes = null;

        modifyProtectionSegment_segmentId = -1;
        modifyProtectionSegment_reservedBandwidthInErlangs = -1;
        modifyProtectionSegment_attributes = null;

        modifyRoute_routeId = -1;
        modifyRoute_carriedTrafficInErlangs = -1;
        modifyRoute_attributes = null;

        modifySRG_srgId = -1;
        modifySRG_mttf = -1;
        modifySRG_mttr = -1;
        modifySRG_attributes = null;

        removeLink_linkId = -1;
        removeProtectionSegment_segmentId = -1;
        removeRoute_routeId = -1;
        removeSRG_srgId = -1;
        
        removeProtectionSegmentFromRouteBackupList_segmentId = -1;
        removeProtectionSegmentFromRouteBackupList_routeId = -1;
        
        removeAllProtectionSegmentsFromRouteBackupList_routeId = -1;
        
        addLinkToSRG_linkId = -1;
        addLinkToSRG_srgId = -1;
        addNodeToSRG_nodeId = -1;
        addNodeToSRG_srgId = -1;

        removeLinkFromSRG_linkId = -1;
        removeLinkFromSRG_srgId = -1;
        removeNodeFromSRG_nodeId = -1;
        removeNodeFromSRG_srgId = -1;
    }
    
    /**
     * 'Add link' action.
     * 
     * @param originNodeId Origin node identifier
     * @param destinationNodeId Destination node identifier
     * @param linkCapacityInErlangs Link capacity in Erlangs
     * @param linkLengthInKm Link length in km
     * @param attributes Attributes (null means empty)
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addLink(int originNodeId, int destinationNodeId, double linkCapacityInErlangs, double linkLengthInKm, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_LINK);
        action.addLink_originNodeId = originNodeId;
        action.addLink_destinationNodeId = destinationNodeId;
        action.addLink_linkCapacityInErlangs = linkCapacityInErlangs;
        action.addLink_linkLengthInKm = linkLengthInKm;
        if (attributes != null) action.addLink_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Add link to SRG' action.
     * 
     * @param linkId Link identifier
     * @param srgId SRG identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addLinkToSRG(long linkId, long srgId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_LINK_TO_SRG);
        action.addLinkToSRG_linkId = linkId;
        action.addLinkToSRG_srgId = srgId;
        
        return action;
    }
    
    /**
     * 'Add node to SRG' action.
     * 
     * @param nodeId Node identifier
     * @param srgId SRG identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addNodeToSRG(int nodeId, long srgId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_NODE_TO_SRG);
        action.addNodeToSRG_nodeId = nodeId;
        action.addNodeToSRG_srgId = srgId;
        
        return action;
    }
    
    /**
     * 'Add protection segment' action.
     * 
     * @param seqLinks Sequence of links
     * @param reservedBandwidthInErlangs Reserved bandwidth in Erlangs
     * @param attributes Attributes (null means empty)
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addProtectionSegment(long[] seqLinks, double reservedBandwidthInErlangs, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_PROTECTION_SEGMENT);
        action.addProtectionSegment_seqLinks = seqLinks;
        action.addProtectionSegment_reservedBandwidthInErlangs = reservedBandwidthInErlangs;
        if (attributes != null) action.addProtectionSegment_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Add protection segment to route backup list' action.
     * 
     * @param segmentId Protection segment identifier
     * @param routeId Route identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addProtectionSegmentTosRouteBackupSegmentList(long segmentId, long routeId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST);
        action.addProtectionSegmentToRouteBackupList_segmentId = segmentId;
        action.addProtectionSegmentToRouteBackupList_routeId = routeId;
        
        return action;
    }
    
    /**
     * 'Add route' action.
     * 
     * @param demandId Demand identifier
     * @param trafficVolumeInErlangs Carried traffic in Erlangs
     * @param seqLinks Sequence of links
     * @param backupSegmentList Backup segment list (null means empty)
     * @param attributes Attributes (null means empty)
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addRoute(int demandId, double trafficVolumeInErlangs, long[] seqLinks, long[] backupSegmentList, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_ROUTE);
        action.addRoute_demandId = demandId;
        action.addRoute_seqLinks = seqLinks;
        action.addRoute_trafficVolumeInErlangs = trafficVolumeInErlangs;
        if (attributes != null) action.addRoute_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Add SRG' action.
     * 
     * @param nodeIds Node identifiers (null means empty)
     * @param linkIds Link identifiers (null means empty)
     * @param mttf Mean time to fail in hours
     * @param mttr Mean time to repair in hours
     * @param attributes Attributes (null means empty)
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction addSRG(int[] nodeIds, long[] linkIds, double mttf, double mttr, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.ADD_SRG);
        action.addSRG_nodeIds = nodeIds;
        action.addSRG_linkIds = linkIds;
        action.addSRG_mttf = mttf;
        action.addSRG_mttr = mttr;
        if (attributes != null) action.addSRG_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Modify link' action.
     * 
     * @param linkId Link identifier
     * @param linkCapacityInErlangs Link capacity in Erlangs (-1 means 'no change')
     * @param linkLengthInKm  Link length in km (-1 means 'no change')
     * @param attributes New attributes (null means 'no change')
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction modifyLink(long linkId, double linkCapacityInErlangs, double linkLengthInKm, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.MODIFY_LINK);
        action.modifyLink_linkId = linkId;
        action.modifyLink_linkCapacityInErlangs = linkCapacityInErlangs;
        action.modifyLink_linkLengthInKm = linkLengthInKm;
        if (attributes != null) action.modifyLink_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Modify protection segment' action.
     * 
     * @param segmentId Protection segment identifier
     * @param reservedBandwidthInErlangs Reserved bandwidth in Erlangs (-1 means 'no change')
     * @param attributes New attributes (null means 'no change')
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction modifyProtectionSegment(long segmentId, double reservedBandwidthInErlangs, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.MODIFY_PROTECTION_SEGMENT);
        action.modifyProtectionSegment_segmentId = segmentId;
        action.modifyProtectionSegment_reservedBandwidthInErlangs = reservedBandwidthInErlangs;
        if (attributes != null) action.modifyProtectionSegment_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Modify route' action.
     * 
     * @param routeId Route identifier
     * @param trafficVolumeInErlangs Carried traffic in Erlangs (-1 means 'no change')
     * @param attributes New attributes (null means 'no change')
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction modifyRoute(long routeId, double trafficVolumeInErlangs, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.MODIFY_ROUTE);
        action.modifyRoute_routeId = routeId;
        action.modifyRoute_carriedTrafficInErlangs = trafficVolumeInErlangs;
        if (attributes != null) action.modifyRoute_attributes.putAll(attributes);
        
        return action;
    }

    /**
     * 'Modify SRG' action.
     * 
     * @param srgId SRG identifier
     * @param mttf Mean time to fail in hours (-1 means 'no change')
     * @param mttr Mean time to repair in hours (-1 means 'no change')
     * @param attributes New attributes (null means 'no change')
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction modifySRG(long srgId, double mttf, double mttr, Map<String, String> attributes)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.MODIFY_SRG);
        action.modifySRG_srgId = srgId;
        action.modifySRG_mttf = mttf;
        action.modifySRG_mttr = mttr;
        if (attributes != null) action.modifySRG_attributes.putAll(attributes);
        
        return action;
    }
    
    /**
     * 'Remove all links' action.
     * 
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeAllLinks() { return new TrafficAllocationAction(ActionType.REMOVE_ALL_LINKS); }
    
    /**
     * 'Remove all protection segments' action.
     * 
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeAllProtectionSegments() { return new TrafficAllocationAction(ActionType.REMOVE_ALL_PROTECTION_SEGMENTS); }

    /**
     * 'Remove all protection segments from route backup list' action.
     * 
     * @param routeId Route identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeAllProtectionSegmentsFromRouteBackupList(long routeId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST);
        action.removeAllProtectionSegmentsFromRouteBackupList_routeId = routeId;
        
        return action;
    }
    
    /**
     * 'Remove all routes' action.
     * 
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeAllRoutes() { return new TrafficAllocationAction(ActionType.REMOVE_ALL_ROUTES); }

    /**
     * 'Remove all SRGs' action.
     * 
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeAllSRGs() { return new TrafficAllocationAction(ActionType.REMOVE_ALL_SRGS); }

    /**
     * 'Remove link' action.
     * 
     * @param linkId Link identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeLink(long linkId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_LINK);
        action.removeLink_linkId = linkId;
        
        return action;
    }

    /**
     * 'Remove link from SRG' action.
     * 
     * @param linkId Link identifier
     * @param srgId SRG identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeLinkFromSRG(long linkId, long srgId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_LINK_FROM_SRG);
        action.removeLinkFromSRG_linkId = linkId;
        action.removeLinkFromSRG_srgId = srgId;
        
        return action;
    }
    
    /**
     * 'Remove node from SRG' action.
     * 
     * @param nodeId Node identifier
     * @param srgId SRG identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeNodeFromSRG(int nodeId, long srgId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_NODE_FROM_SRG);
        action.removeNodeFromSRG_nodeId = nodeId;
        action.removeNodeFromSRG_srgId = srgId;
        
        return action;
    }
    
    /**
     * 'Remove protection segment' action.
     * 
     * @param segmentId Protection segment identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeProtectionSegment(long segmentId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_PROTECTION_SEGMENT);
        action.removeProtectionSegment_segmentId = segmentId;
        
        return action;
    }

    /**
     * 'Remove protection segment from route backup list' action.
     * 
     * @param segmentId Protection segment identifier
     * @param routeId Route identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeProtectionSegmentFromRouteBackupSegmentList(long segmentId, long routeId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST);
        action.removeProtectionSegmentFromRouteBackupList_segmentId = segmentId;
        action.removeProtectionSegmentFromRouteBackupList_routeId = routeId;
        
        return action;
    }

    /**
     * 'Remove route' action.
     * 
     * @param routeId Route identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeRoute(long routeId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_ROUTE);
        action.removeRoute_routeId = routeId;
        
        return action;
    }

    /**
     * 'Remove SRG' action.
     * 
     * @param srgId SRG identifier
     * @return Traffic allocation action
     * @since 0.2.3
     */
    public static TrafficAllocationAction removeSRG(long srgId)
    {
        TrafficAllocationAction action = new TrafficAllocationAction(ActionType.REMOVE_SRG);
        action.removeSRG_srgId = srgId;
        
        return action;
    }

    /**
     * Returns the action type.
     *
     * @return Action type
     * @since 0.2.1
     */
    public ActionType getActionType() { return type; }

    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.1
     */
    @Override
    public String toString()
    {
        switch(getActionType())
        {
            case ADD_LINK:
                return String.format("Add link (origin node = %d, destination node = %d, capacity = %.3f E, length = %.3f km, attributes = %s)", addLink_originNodeId, addLink_destinationNodeId, addLink_linkCapacityInErlangs, addLink_linkLengthInKm, StringUtils.mapToString(addLink_attributes, "=", ", "));

            case ADD_LINK_TO_SRG:
                return "Add link " + addLinkToSRG_linkId + " to SRG " + addLinkToSRG_srgId;

            case ADD_NODE_TO_SRG:
                return "Add node " + addNodeToSRG_nodeId + " to SRG " + addNodeToSRG_srgId;

            case ADD_PROTECTION_SEGMENT:
                return String.format("Add protection segment (seq. links = %s, reserved bandwidth = %.3f E, attributes = %s)", LongUtils.join(addProtectionSegment_seqLinks, " => "), addProtectionSegment_reservedBandwidthInErlangs, StringUtils.mapToString(addProtectionSegment_attributes, "=", ", "));

            case ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST:
                return "Add protection segment " + addProtectionSegmentToRouteBackupList_segmentId + " to route " + addProtectionSegmentToRouteBackupList_routeId + "'s backup list";

            case ADD_ROUTE:
                return String.format("Add route (demand = %d, carried traffic = %.3f E, seq. links = %s, attributes = %s)", addRoute_demandId, addRoute_trafficVolumeInErlangs, LongUtils.join(addRoute_seqLinks, " => "), StringUtils.mapToString(addRoute_attributes, "=", ", "));

            case ADD_SRG:
                return String.format("Add SRG (nodes = %s, links = %s, mttf = %.3f, mttr = %.3f, attributes = %s", IntUtils.join(addSRG_nodeIds, ", "), LongUtils.join(addSRG_linkIds, ", "), addSRG_mttf, addSRG_mttr, StringUtils.mapToString(addSRG_attributes, "=", ", "));

            case MODIFY_LINK:
                String aux_modifyLink_linkCapacityInErlangs = modifyLink_linkCapacityInErlangs == -1 ? "no change" : Double.toString(modifyLink_linkCapacityInErlangs) + " E";
                String aux_modifyLink_linkLengthInKm = modifyLink_linkLengthInKm == -1 ? "no change" : Double.toString(modifyLink_linkLengthInKm) + " km";
                String aux_modifyLink_attributes = modifyLink_attributes == null ? "no change" : StringUtils.mapToString(modifyLink_attributes, "=", ", ");
                return "Modify link " + modifyLink_linkId + " (capacity = " + aux_modifyLink_linkCapacityInErlangs + ", length = " + aux_modifyLink_linkLengthInKm + ", attributes = " + aux_modifyLink_attributes + ")";

            case MODIFY_PROTECTION_SEGMENT:
                String aux_modifyProtectionSegment_carriedTrafficInErlangs = modifyProtectionSegment_reservedBandwidthInErlangs == -1 ? "no change" : Double.toString(modifyProtectionSegment_reservedBandwidthInErlangs) + " E";
                String aux_modifyProtectionSegment_attributes = modifyProtectionSegment_attributes == null ? "no change" : StringUtils.mapToString(modifyProtectionSegment_attributes, "=", ", ");
                return "Modify protection segment " + modifyProtectionSegment_segmentId + " (reserved bandwidth = " + aux_modifyProtectionSegment_carriedTrafficInErlangs + ", attributes = " + aux_modifyProtectionSegment_attributes + ")";

            case MODIFY_ROUTE:
                String aux_modifyRoute_carriedTrafficInErlangs = modifyRoute_carriedTrafficInErlangs == -1 ? "no change" : Double.toString(modifyRoute_carriedTrafficInErlangs) + " E";
                String aux_modifyRoute_attributes = modifyRoute_attributes == null ? "no change" : StringUtils.mapToString(modifyRoute_attributes, "=", ", ");
                return "Modify route " + modifyRoute_routeId + " (carried traffic = " + aux_modifyRoute_carriedTrafficInErlangs + ", attributes = " + aux_modifyRoute_attributes + ")";

            case MODIFY_SRG:
                String aux_modifySRG_mttf = modifySRG_mttf == -1 ? "no change" : Double.toString(modifySRG_mttf);
                String aux_modifySRG_mttr = modifySRG_mttr == -1 ? "no change" : Double.toString(modifySRG_mttr);
                String aux_modifySRG_attributes = modifySRG_attributes == null ? "no change" : StringUtils.mapToString(modifySRG_attributes, "=", ", ");
                return "Modify SRG " + modifySRG_srgId + " (mttf = " + aux_modifySRG_mttf + ", mttr = " + aux_modifySRG_mttr + ", attributes = " + aux_modifySRG_attributes + ")";

            case REMOVE_ALL_LINKS:
                return "Remove all links";

            case REMOVE_ALL_PROTECTION_SEGMENTS:
                return "Remove all protection segments";

            case REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST:
                return "Remove all protection segments from route " + removeAllProtectionSegmentsFromRouteBackupList_routeId + "'s backup list";

            case REMOVE_ALL_SRGS:
                return "Remove all SRGs";

            case REMOVE_ALL_ROUTES:
                return "Remove all routes";

            case REMOVE_LINK:
                return "Remove link " + removeLink_linkId;

            case REMOVE_LINK_FROM_SRG:
                return "Remove link " + removeLinkFromSRG_linkId + " from " + removeLinkFromSRG_srgId;

            case REMOVE_NODE_FROM_SRG:
                return "Remove node " + removeNodeFromSRG_nodeId + " from SRG " + removeNodeFromSRG_srgId;

            case REMOVE_PROTECTION_SEGMENT:
                return "Remove protection segment " + removeProtectionSegment_segmentId;

            case REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST:
                return "Remove protection segment " + removeProtectionSegmentFromRouteBackupList_segmentId + " from route " + removeProtectionSegmentFromRouteBackupList_routeId + "'s backup list";

            case REMOVE_ROUTE:
                return "Remove route " + removeRoute_routeId;

            case REMOVE_SRG:
                return "Remove SRG " + removeSRG_srgId;

            default:
                throw new Net2PlanException("Unknown traffic allocation action");
        }
    }
}