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

import cern.colt.list.tint.IntArrayList;
import cern.colt.list.tlong.LongArrayList;
import static com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction.ActionType.ADD_LINK_TO_SRG;
import static com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction.ActionType.REMOVE_ALL_PROTECTION_SEGMENTS;
import static com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction.ActionType.REMOVE_LINK_FROM_SRG;
import static com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction.ActionType.REMOVE_NODE_FROM_SRG;
import static com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction.ActionType.REMOVE_ROUTE;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.NetworkElement;
import com.tejas.engine.internal.NetworkElement.Demand;
import com.tejas.engine.internal.NetworkElement.Link;
import com.tejas.engine.internal.NetworkElement.Node;
import com.tejas.engine.internal.NetworkElement.Route;
import com.tejas.engine.internal.NetworkElement.SRG;
import com.tejas.engine.internal.NetworkElement.Segment;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;
import com.tejas.engine.internal.sim.impl.TimeVaryingTrafficSimulation;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public final class TimeVaryingNetState extends SimState
{
    private List<NetworkElement.Node> nodes;
    private BidiMap<Long, NetworkElement.Link> links;
    private List<NetworkElement.Demand> demands;
    private BidiMap<Long, NetworkElement.Route> routes;
    private BidiMap<Long, NetworkElement.Segment> segments;
    private BidiMap<Long, NetworkElement.SRG> srgs;
    private long nextLinkId;
    private long nextRouteId;
    private long nextSegmentId;
    private long nextSRGId;

    private TimeVaryingNetState() { super(null); }
    
    /**
     * Default constructor. Initializes the network state using existing information.
     *
     * @param netPlan Initial network design
     * @since 0.2.3
     */
    public TimeVaryingNetState(NetPlan netPlan)
    {
        super(netPlan);
        
	nodes = new ArrayList<NetworkElement.Node>();
	links = new DualHashBidiMap<Long, NetworkElement.Link>();
	demands = new ArrayList<NetworkElement.Demand>();
	routes = new DualHashBidiMap<Long, NetworkElement.Route>();
	segments = new DualHashBidiMap<Long, NetworkElement.Segment>();
        srgs = new DualHashBidiMap<Long, NetworkElement.SRG>();
        
	isModifiable = true;
        
        reset();
    }

    /**
     * Returns a network design from the current network state.
     * 
     * @return Network design
     * @since 0.2.1
     */
    @Override
    public NetPlan convertToNetPlan()
    {
	NetPlan aux = new NetPlan();

	aux.setNetworkAttributes(netPlan.getNetworkAttributes());
        aux.setNetworkDescription(netPlan.getNetworkDescription());
        aux.setNetworkName(netPlan.getNetworkName());
        
        int N = netPlan.getNumberOfNodes();
	for (int nodeId = 0; nodeId < N; nodeId++)
	    aux.addNode(netPlan.getNodeXYPosition(nodeId)[0], netPlan.getNodeXYPosition(nodeId)[1], netPlan.getNodeName(nodeId), netPlan.getNodeSpecificAttributes(nodeId));

        int D = netPlan.getNumberOfDemands();
	for (int demandId = 0; demandId < D; demandId++)
	    aux.addDemand(netPlan.getDemandIngressNode(demandId), netPlan.getDemandEgressNode(demandId), getDemand(demandId).offeredTrafficInErlangs, netPlan.getDemandSpecificAttributes(demandId));
        
        Map<Long, Integer> newLinkIds = new HashMap<Long, Integer>();
        Map<Long, Integer> newSegmentIds = new HashMap<Long, Integer>();
        
        long[] linkIds = getLinkIds();
        for(long linkId : linkIds)
        {
            int originNodeId = getLinkOriginNode(linkId);
            int destinationNodeId = getLinkDestinationNode(linkId);
            double linkLengthInKm = getLinkLengthInKm(linkId);
            double linkCapacityInErlangs = getLinkCapacityInErlangs(linkId);
            Map<String, String> linkAttributes = getLinkSpecificAttributes(linkId);
            
            int newLinkId = aux.addLink(originNodeId, destinationNodeId, linkCapacityInErlangs, linkLengthInKm, linkAttributes);
            newLinkIds.put(linkId, newLinkId);
        }
        
        long[] srgIds = getSRGIds();
        for(long srgId : srgIds)
        {
            int[] nodeIds_thisSRG = getSRGNodes(srgId);
            long[] aux_linkIds_thisSRG = getSRGLinks(srgId);
            
            IntArrayList linkIds_thisSRG = new IntArrayList();
            for(long linkId : aux_linkIds_thisSRG) linkIds_thisSRG.add(newLinkIds.get(linkId));
            linkIds_thisSRG.trimToSize();
            
            double mttf = getSRGMeanTimeToFailInHours(srgId);
            double mttr = getSRGMeanTimeToRepairInHours(srgId);
            Map<String, String> srgAttributes = getSRGSpecificAttributes(srgId);
            
            aux.addSRG(nodeIds_thisSRG, linkIds_thisSRG.elements(), mttf, mttr, srgAttributes);
        }
        
        long[] segmentIds = getProtectionSegmentIds();
        for(long segmentId : segmentIds)
        {
            long[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidthInErlangs = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            Map<String, String> segmentAttributes = getProtectionSegmentSpecificAttributes(segmentId);
            
            IntArrayList linkIds_thisSegment = new IntArrayList();
            for(long linkId : seqLinks) linkIds_thisSegment.add(newLinkIds.get(linkId));
            linkIds_thisSegment.trimToSize();

            int newSegmentId = aux.addProtectionSegment(linkIds_thisSegment.elements(), reservedBandwidthInErlangs, segmentAttributes);
            newSegmentIds.put(segmentId, newSegmentId);
        }
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int demandId = getRouteDemand(routeId);
            long[] seqLinks = getRouteSequenceOfLinks(routeId);
            long[] backupSegmentList = getRouteBackupSegmentList(routeId);
            double carriedTrafficInErlangs = getRouteCarriedTrafficInErlangs(routeId);
            Map<String, String> routeAttributes = getRouteSpecificAttributes(routeId);
            
            IntArrayList linkIds_thisRoute = new IntArrayList();
            for(long linkId : seqLinks) linkIds_thisRoute.add(newLinkIds.get(linkId));
            linkIds_thisRoute.trimToSize();

            IntArrayList backupSegmentList_thisRoute = new IntArrayList();
            for(long linkId : backupSegmentList) backupSegmentList_thisRoute.add(newSegmentIds.get(linkId));
            backupSegmentList_thisRoute.trimToSize();

            aux.addRoute(demandId, carriedTrafficInErlangs, linkIds_thisRoute.elements(), backupSegmentList_thisRoute.elements(), routeAttributes);
        }

	return aux;
    }

    /**
     * Returns a deep copy of the current state.
     * 
     * @return Deep copy of the current state
     * @since 0.2.1
     */
    @Override
    public SimState copy()
    {
        TimeVaryingNetState copy = new TimeVaryingNetState(netPlan);
        
        copy.isModifiable = true;
        copy.nextLinkId = nextLinkId;
        copy.nextRouteId = nextRouteId;
        copy.nextSegmentId = nextSegmentId;
        copy.nextSRGId = nextSRGId;
        
        int D = netPlan.getNumberOfDemands();
        for(int demandId = 0; demandId < D; demandId++)
            copy.getDemand(demandId).offeredTrafficInErlangs = getDemandOfferedTrafficInErlangs(demandId);
        
        copy.links.clear();
        copy.routes.clear();
        copy.segments.clear();
        copy.srgs.clear();
        
        long[] linkIds = getLinkIds();
        for(long linkId : linkIds)
        {
            int originNodeId = getLinkOriginNode(linkId);
            int destinationNodeId = getLinkDestinationNode(linkId);
            double linkCapacityInErlangs = getLinkCapacityInErlangs(linkId);
            double linkLengthInKm = getLinkLengthInKm(linkId);
            Map<String, String> attributes = getLinkSpecificAttributes(linkId);
            
            copy.addLink(linkId, originNodeId, destinationNodeId, linkCapacityInErlangs, linkLengthInKm, attributes);
        }
        
        long[] srgIds = getSRGIds();
        for(long srgId : srgIds)
        {
            int[] nodeIds = getSRGNodes(srgId);
            long[] linkIds_thisSRG = getSRGLinks(srgId);
            
            Set<Node> nodeList = new HashSet<Node>();
            for(int nodeId : nodeIds)
                nodeList.add(copy.getNode(nodeId));

            Set<Link> linkList = new HashSet<Link>();
            for(long linkId : linkIds_thisSRG)
                linkList.add(copy.getLink(linkId));
            
            NetworkElement.SRG srg = new NetworkElement.SRG();
            srg.mttf = getSRGMeanTimeToFailInHours(srgId);
            srg.mttr = getSRGMeanTimeToRepairInHours(srgId);
            srg.nodes = nodeList;
            srg.links = linkList;
            srg.setAttributes(getSRGSpecificAttributes(srgId));
            
            copy.srgs.put(srgId, srg);
        }
        
        long[] segmentIds = getProtectionSegmentIds();
        for(long segmentId : segmentIds)
        {
            long[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidthInErlangs = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            
            List<Link> linkList = new LinkedList<Link>();
            for(long linkId : seqLinks)
                linkList.add(copy.getLink(linkId));
            
            NetworkElement.Segment segment = new NetworkElement.Segment(linkList, reservedBandwidthInErlangs);
            segment.setAttributes(getProtectionSegmentSpecificAttributes(segmentId));
            
            copy.segments.put(segmentId, segment);
        }
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int demandId = getRouteDemand(routeId);
            double carriedTrafficInErlangs = getRouteCarriedTrafficInErlangs(routeId);
            long[] seqLinks = getRouteSequenceOfLinks(routeId);
            long[] backupSegmentIds = getRouteBackupSegmentList(routeId);
            Map<String, String> attributes = getRouteSpecificAttributes(routeId);
            
            copy.addRoute(routeId, demandId, carriedTrafficInErlangs, seqLinks, attributes);
            
            for(long segmentId : backupSegmentIds)
                copy.addProtectionSegmentToRouteBackupSegmentList(segmentId, routeId);
        }
        
        return copy;
    }

    /**
     * Returns an unmodifiable view of the network.
     *
     * @return An unmodifiable view of the network
     * @since 0.2.1
     */
    @Override
    public SimState unmodifiableView()
    {
        TimeVaryingNetState view = new TimeVaryingNetState();
        
        view.netPlan = netPlan;
        view.nodes = nodes;
        view.links = links;
        view.demands = demands;
        view.routes = routes;
        view.segments = segments;
        view.srgs = srgs;

        view.isModifiable = false;
        view.nextLinkId = nextLinkId;
        view.nextRouteId = nextRouteId;
        view.nextSegmentId = nextSegmentId;
        view.nextSRGId = nextSRGId;

        return view;
    }

    private void addLink(long linkId, int originNodeId, int destinationNodeId, double linkCapacityInErlangs, double linkLengthInKm, Map<String, String> attributes)
    {
        Node newLink_originNode = getNode(originNodeId);
        Node newLink_destinationNode = getNode(destinationNodeId);
        Link newLink = new Link(newLink_originNode, newLink_destinationNode);
        newLink.linkCapacityInErlangs = linkCapacityInErlangs;
        newLink.linkLengthInKm = linkLengthInKm;
        newLink.setAttributes(attributes);
        
        if (linkId == -1) linkId = nextLinkId++;
        
        links.put(linkId, newLink);
    } 
    
    private void addRoute(long routeId, int demandId, double carriedTrafficInErlangs, long[] seqLinks, Map<String, String> attributes)
    {
        NetworkElement.Demand newRoute_demand = getDemand(demandId);
        double newRoute_carriedTrafficInErlangs = carriedTrafficInErlangs;
        long[] newRoute_seqLinks = seqLinks;
        Map<String, String> newRoute_attributes = attributes;

        List<Link> linkList = new LinkedList<Link>();
        for(long linkId : newRoute_seqLinks)
            linkList.add(getLink(linkId));

        NetworkElement.Route route = new NetworkElement.Route(newRoute_demand, linkList, newRoute_carriedTrafficInErlangs);
        route.backupSegments = new LinkedList<Segment>();
        route.setAttributes(newRoute_attributes);
        
        if (routeId == -1) routeId = nextRouteId++;

        routes.put(routeId, route);
    }
    
    private void addProtectionSegmentToRouteBackupSegmentList(long segmentId, long routeId)
    {
        checkProtectionSegmentMergeabilityToRoute(routeId, segmentId);

        Segment segment = getSegment(segmentId);
        Route route = getRoute(routeId);
        route.backupSegments.add(segment);
    }
    
    /**
     * Update the network state.
     *
     * @param event Current simulation event
     * @param actions List of actions to perform
     * @return A <code>null</code> object
     * @since 0.2.1
     */
    @Override
    public Object update(SimEvent event, List actions)
    {
        checkIsModifiable();
        
        if (!(event instanceof TimeVaryingTrafficSimulation.__INTERNAL_TrafficEvent)) throw new Net2PlanException("Not a valid event");
        TimeVaryingTrafficSimulation.__INTERNAL_TrafficEvent tcEvent = (TimeVaryingTrafficSimulation.__INTERNAL_TrafficEvent) event;
        double[] h_d = tcEvent.h_d;
        
        int D = netPlan.getNumberOfDemands();
        for(int demandId = 0; demandId < D; demandId++)
        {
            Demand demand = getDemand(demandId);
            demand.offeredTrafficInErlangs = h_d[demandId];
        }
        
        for(Object action : actions)
        {
            if (!(action instanceof TrafficAllocationAction)) throw new Net2PlanException("Not a valid action");
            
            TrafficAllocationAction taAction = (TrafficAllocationAction) action;
            
            switch(taAction.getActionType())
            {
                case ADD_LINK:
                    int addLink_originNodeId = taAction.addLink_originNodeId;
                    int addLink_destinationNodeId = taAction.addLink_destinationNodeId;
                    double addLink_linkCapacityInErlangs = taAction.addLink_linkCapacityInErlangs;
                    double addLink_linkLengthInKm = taAction.addLink_linkLengthInKm;
                    Map<String, String> addLink_attributes = taAction.addLink_attributes;
                    
                    addLink(-1, addLink_originNodeId, addLink_destinationNodeId, addLink_linkCapacityInErlangs, addLink_linkLengthInKm, addLink_attributes);
                    break;
                    
                case ADD_LINK_TO_SRG:
                    Link addLinkToSRG_link = getLink(taAction.addLinkToSRG_linkId);
                    SRG addLinkToSRG_srg = getSRG(taAction.addLinkToSRG_srgId);
                    addLinkToSRG_srg.links.add(addLinkToSRG_link);
                    break;
                    
                case ADD_NODE_TO_SRG:
                    Node addNodeToSRG_node = getNode(taAction.addNodeToSRG_nodeId);
                    SRG addNodeToSRG_srg = getSRG(taAction.addNodeToSRG_srgId);
                    addNodeToSRG_srg.nodes.add(addNodeToSRG_node);
                    break;
                    
                case ADD_PROTECTION_SEGMENT:
                    long[] newSegment_seqLinks = taAction.addProtectionSegment_seqLinks;
                    double newSegment_reservedBandwidthInErlangs = taAction.addProtectionSegment_reservedBandwidthInErlangs;
                    Map<String, String> newSegment_attributes = taAction.addProtectionSegment_attributes;
            
                    List<Link> newSegment_linkList = new LinkedList<Link>();
                    for(long linkId : newSegment_seqLinks)
                        newSegment_linkList.add(getLink(linkId));

                    NetworkElement.Segment segment = new NetworkElement.Segment(newSegment_linkList, newSegment_reservedBandwidthInErlangs);
                    segment.setAttributes(newSegment_attributes);

                    segments.put(nextSegmentId++, segment);
                    break;
                    
                case ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST:
                    long addProtectionSegmentToRouteBackupList_segmentId = taAction.addProtectionSegmentToRouteBackupList_segmentId;
                    long addProtectionSegmentToRouteBackupList_routeId = taAction.addProtectionSegmentToRouteBackupList_routeId;
                    
                    addProtectionSegmentToRouteBackupSegmentList(addProtectionSegmentToRouteBackupList_segmentId, addProtectionSegmentToRouteBackupList_routeId);
                    break;
                    
                case ADD_ROUTE:
                    
                    int addRoute_demandId = taAction.addRoute_demandId;
                    double addRoute_trafficVolumeInErlangs = taAction.addRoute_trafficVolumeInErlangs;
                    long[] addRoute_seqLinks = taAction.addRoute_seqLinks;
                    Map<String, String> addRoute_attributes = taAction.addRoute_attributes;
                    
                    addRoute(-1, addRoute_demandId, addRoute_trafficVolumeInErlangs, addRoute_seqLinks, addRoute_attributes);
                    break;
                    
                case ADD_SRG:
                    int[] addSRG_nodeIds = taAction.addSRG_nodeIds;
                    long[] addSRG_linkIds = taAction.addSRG_linkIds;
                    
                    Set<Node> addSRG_nodeList = new HashSet<Node>();
                    for(int nodeId : addSRG_nodeIds)
                        addSRG_nodeList.add(getNode(nodeId));

                    Set<Link> addSRG_linkList = new HashSet<Link>();
                    for(long linkId : addSRG_linkIds)
                        addSRG_linkList.add(getLink(linkId));

                    NetworkElement.SRG srg = new NetworkElement.SRG();
                    srg.mttf = taAction.addSRG_mttf;
                    srg.mttr = taAction.addSRG_mttr;
                    srg.nodes = addSRG_nodeList;
                    srg.links = addSRG_linkList;
                    srg.setAttributes(taAction.addSRG_attributes);
                    srgs.put(nextSRGId++, srg);
                    
                    break;
                    
                case MODIFY_LINK:
                    Link modifyLink = getLink(taAction.modifyLink_linkId);
                    double modifyLink_linkCapacityInErlangs = taAction.modifyLink_linkCapacityInErlangs;
                    double modifyLink_linkLengthInKm = taAction.modifyLink_linkLengthInKm;
                    Map<String, String> modifyLink_attributes = taAction.modifyLink_attributes;
                    if (modifyLink_linkCapacityInErlangs != -1) modifyLink.linkCapacityInErlangs = modifyLink_linkCapacityInErlangs;
                    if (modifyLink_linkLengthInKm != -1) modifyLink.linkLengthInKm = modifyLink_linkLengthInKm;
                    if (modifyLink_attributes != null) modifyLink.setAttributes(modifyLink_attributes);
                    
                    break;
                    
                case MODIFY_PROTECTION_SEGMENT:
                    Segment modifySegment = getSegment(taAction.modifyProtectionSegment_segmentId);
                    double modifySegment_reservedBandwidthInErlangs = taAction.modifyProtectionSegment_reservedBandwidthInErlangs;
                    Map<String, String> modifySegment_attributes = taAction.modifyProtectionSegment_attributes;
                    if (modifySegment_reservedBandwidthInErlangs != -1) modifySegment.reservedBandwithInErlangs = modifySegment_reservedBandwidthInErlangs;
                    if (modifySegment_attributes != null) modifySegment.setAttributes(modifySegment_attributes);

                    break;
                    
                case MODIFY_ROUTE:
                    Route modifyRoute = getRoute(taAction.modifyRoute_routeId);
                    double modifyRoute_carriedTrafficInErlangs = taAction.modifyRoute_carriedTrafficInErlangs;
                    Map<String, String> modifyRoute_attributes = taAction.modifyRoute_attributes;
                    if (modifyRoute_carriedTrafficInErlangs != -1) modifyRoute.carriedTrafficInErlangs = modifyRoute_carriedTrafficInErlangs;
                    if (modifyRoute_attributes != null) modifyRoute.setAttributes(modifyRoute_attributes);
                    break;
                    
                case MODIFY_SRG:
                    SRG modifySRG = getSRG(taAction.modifySRG_srgId);
                    double modifySRG_mttf = taAction.modifySRG_mttf;
                    double modifySRG_mttr = taAction.modifySRG_mttr;
                    Map<String, String> modifySRG_attributes = taAction.modifySRG_attributes;
                    if (modifySRG_mttf != -1) modifySRG.mttf = modifySRG_mttf;
                    if (modifySRG_mttf != -1) modifySRG.mttr = modifySRG_mttr;
                    if (modifySRG_attributes != null) modifySRG.setAttributes(modifySRG_attributes);
                    break;
                    
                case REMOVE_ALL_LINKS:
                    segments.clear();
                    routes.clear();
                    for(SRG removeAllLinks_srg : srgs.values()) removeAllLinks_srg.links.clear();
                    break;
                    
                case REMOVE_ALL_PROTECTION_SEGMENTS:
                    segments.clear();
                    for(Route removeAllProtectionSegments_route : routes.values()) removeAllProtectionSegments_route.backupSegments.clear();
                    break;
                    
                case REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST:
                    getRoute(taAction.removeAllProtectionSegmentsFromRouteBackupList_routeId).backupSegments.clear();
                    break;
                    
                case REMOVE_ALL_SRGS:
                    srgs.clear();
                    break;
                    
                case REMOVE_ALL_ROUTES:
                    routes.clear();
                    break;
                    
                case REMOVE_LINK:
                    long removeLink_linkId = taAction.removeLink_linkId;
                    Link removeLink_link = getLink(removeLink_linkId);
                    long[] removeLink_routeIds = getLinkTraversingRoutes(removeLink_linkId);
                    for(long routeId : removeLink_routeIds)
                        routes.remove(routeId);
                    long[] removeLink_segmentIds = getLinkTraversingProtectionSegments(removeLink_linkId);
                    for(long segmentId : removeLink_segmentIds)
                    {
                        Segment removeProtectionSegment_segment = getSegment(segmentId);
                        long[] removeProtectionSegment_routeIds = getProtectionSegmentRoutes(segmentId);
                        for(long routeId : removeProtectionSegment_routeIds)
                            getRoute(routeId).backupSegments.remove(removeProtectionSegment_segment);
                        segments.remove(segmentId);
                    }
                    long[] removeLink_srgIds = getLinkSRGs(removeLink_linkId);
                    for(long srgId : removeLink_srgIds) getSRG(srgId).links.remove(removeLink_link);
                    
                    links.remove(removeLink_linkId);
                    break;
                    
                case REMOVE_LINK_FROM_SRG:
                    Link removeLinkFromSRG_link = getLink(taAction.removeLinkFromSRG_linkId);
                    SRG removeLinkFromSRG_srg = getSRG(taAction.removeLinkFromSRG_srgId);
                    removeLinkFromSRG_srg.links.remove(removeLinkFromSRG_link);
                    break;
                    
                case REMOVE_NODE_FROM_SRG:
                    Node removeNodeFromSRG_node = getNode(taAction.removeNodeFromSRG_nodeId);
                    SRG removeNodeFromSRG_srg = getSRG(taAction.removeNodeFromSRG_srgId);
                    removeNodeFromSRG_srg.nodes.remove(removeNodeFromSRG_node);
                    break;
                    
                case REMOVE_PROTECTION_SEGMENT:
                    long removeProtectionSegment_segmentId = taAction.removeProtectionSegment_segmentId;
                    Segment removeProtectionSegment_segment = getSegment(removeProtectionSegment_segmentId);
                    long[] removeProtectionSegment_routeIds = getProtectionSegmentRoutes(removeProtectionSegment_segmentId);
                    for(long routeId : removeProtectionSegment_routeIds)
                        getRoute(routeId).backupSegments.remove(removeProtectionSegment_segment);
                    
                    segments.remove(removeProtectionSegment_segmentId);
                    break;
                    
                case REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST:
                    long removeProtectionSegmentFromRouteBackupList_segmentId = taAction.removeProtectionSegmentFromRouteBackupList_segmentId;
                    Segment removeProtectionSegmentFromRouteBackupList_segment = getSegment(removeProtectionSegmentFromRouteBackupList_segmentId);
                    long removeProtectionSegmentFromRouteBackupList_routeId = taAction.removeProtectionSegmentFromRouteBackupList_routeId;
                    getRoute(removeProtectionSegmentFromRouteBackupList_routeId).backupSegments.remove(removeProtectionSegmentFromRouteBackupList_segment);
                    break;
                    
                case REMOVE_ROUTE:
                    routes.remove(taAction.removeRoute_routeId);
                    break;
                    
                case REMOVE_SRG:
                    srgs.remove(taAction.removeSRG_srgId);
                    break;
                    
                default:
                    throw new Net2PlanException("Unknown traffic allocation action");
            }
        }
        
        return null;
    }
    
    /**
     * Returns the link identifier for the next created link.
     *
     * @return Link identifier for the next created link
     * @since 0.2.3
     */
    public long getLinkNextId() { return nextLinkId; }
    
    /**
     * Returns the route identifier for the next created route.
     *
     * @return Route identifier for the next created route
     * @since 0.2.3
     */
    public long getRouteNextId() { return nextRouteId; }
    
    /**
     * Returns the connection identifier for the next created connection.
     *
     * @return Protection segment identifier for the next created protection segment
     * @since 0.2.3
     */
    public long getProtectionSegmentNextId() { return nextSegmentId; }
    
    /**
     * Returns the SRG identifier for the next created SRG.
     *
     * @return SRG identifier for the next created SRG
     * @since 0.2.3
     */
    public long getSRGNextId() { return nextSRGId; }

    @Override
    public void checkValidity(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic)
    {
	double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

        if (!allowLinkOversubscription)
        {
            long[] linkIds = getLinkIds();
            
            for(long linkId : linkIds)
            {
                double u_e = getLinkCapacityInErlangs(linkId);
                double y_e = getLinkCarriedTrafficInErlangs(linkId);
                double r_e = getLinkCapacityReservedForProtectionInErlangs(linkId);
                double rho_e = (y_e + r_e) / u_e;
                
                if (rho_e > 1 + PRECISIONFACTOR)
                    throw new RuntimeException(String.format("Carried traffic (%f E) and reserved bandwidth for protection (%f E) for link %d overcomes link capacity (%f E)", y_e, r_e, linkId, u_e));
            }
        }
        
        if (!allowExcessCarriedTraffic)
        {
            int D = netPlan.getNumberOfDemands();
            double[] h_d = getDemandOfferedTrafficInErlangsVector();
            double[] r_d = getDemandCarriedTrafficInErlangsVector();
            
            for(int demandId = 0; demandId < D; demandId++)
                if (r_d[demandId] > h_d[demandId] * (1+PRECISIONFACTOR))
                    throw new Net2PlanException(String.format("Carried traffic for demand %d overcomes the offered traffic (offered = %f E, carried = %f E)", demandId, h_d[demandId], r_d[demandId]));
        }
    }
    
    /**
     * Resets the state of the network.
     * 
     * @since 0.2.1
     */
    @Override
    public void reset()
    {
        checkIsModifiable();

        nodes.clear();
        links.clear();
        demands.clear();
        routes.clear();
        segments.clear();
        srgs.clear();
        
        int N = netPlan.getNumberOfNodes();
        for(int nodeId = 0; nodeId < N; nodeId++)
        {
            double[] pos = netPlan.getNodeXYPosition(nodeId);

            NetworkElement.Node node = new NetworkElement.Node();
            node.name = netPlan.getNodeName(nodeId);
            node.position = new Point2D.Double(pos[0], pos[1]);
            node.setAttributes(netPlan.getNodeSpecificAttributes(nodeId));
            
            nodes.add(node);
        }
        
        int E = netPlan.getNumberOfLinks();
        for(int linkId = 0; linkId < E; linkId++)
        {
            int originNodeId = netPlan.getLinkOriginNode(linkId);
            int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
            NetworkElement.Node ingressNode = getNode(originNodeId);
            NetworkElement.Node egressNode = getNode(destinationNodeId);
            
            NetworkElement.Link link = new NetworkElement.Link(ingressNode, egressNode);
            link.linkCapacityInErlangs = netPlan.getLinkCapacityInErlangs(linkId);
            link.linkLengthInKm = netPlan.getLinkLengthInKm(linkId);
            link.setAttributes(netPlan.getLinkSpecificAttributes(linkId));
            
            links.put(Long.valueOf(linkId), link);
        }
        
        int D = netPlan.getNumberOfDemands();
        for(int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);
            NetworkElement.Node ingressNode = getNode(ingressNodeId);
            NetworkElement.Node egressNode = getNode(egressNodeId);
            
            NetworkElement.Demand demand = new NetworkElement.Demand(ingressNode, egressNode);
            demand.offeredTrafficInErlangs = netPlan.getDemandOfferedTrafficInErlangs(demandId);
            demand.setAttributes(netPlan.getDemandSpecificAttributes(demandId));
            
            demands.add(demand);
        }
        
        int S = netPlan.getNumberOfProtectionSegments();
        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            int[] seqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidthInErlangs = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            
            List<Link> linkList = new LinkedList<Link>();
            for(int linkId : seqLinks)
                linkList.add(getLink(linkId));
            
            NetworkElement.Segment segment = new NetworkElement.Segment(linkList, reservedBandwidthInErlangs);
            segment.setAttributes(netPlan.getProtectionSegmentSpecificAttributes(segmentId));
            
            segments.put(new Long(segmentId), segment);
        }

        int R = netPlan.getNumberOfRoutes();
        for(int routeId = 0; routeId < R; routeId++)
        {
            int demandId = netPlan.getRouteDemand(routeId);
            double carriedTrafficInErlangs = netPlan.getRouteCarriedTrafficInErlangs(routeId);
            int[] seqLinks = netPlan.getRouteSequenceOfLinks(routeId);
            int[] backupSegmentIds = netPlan.getRouteBackupSegmentList(routeId);
            Map<String, String> attributes = netPlan.getRouteSpecificAttributes(routeId);
            
            addRoute(-1, demandId, carriedTrafficInErlangs, IntUtils.toLongArray(seqLinks), attributes);
            
            for(long segmentId : backupSegmentIds)
                addProtectionSegmentToRouteBackupSegmentList(segmentId, routeId);
        }
        
        int numSRGs = netPlan.getNumberOfSRGs();
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = netPlan.getSRGNodes(srgId);
            int[] linkIds = netPlan.getSRGLinks(srgId);
            
            Set<Node> nodeList = new HashSet<Node>();
            for(int nodeId : nodeIds)
                nodeList.add(getNode(nodeId));

            Set<Link> linkList = new HashSet<Link>();
            for(int linkId : linkIds)
                linkList.add(getLink(linkId));
            
            NetworkElement.SRG srg = new NetworkElement.SRG();
            srg.mttf = netPlan.getSRGMeanTimeToFailInHours(srgId);
            srg.mttr = netPlan.getSRGMeanTimeToRepairInHours(srgId);
            srg.nodes = nodeList;
            srg.links = linkList;
            srg.setAttributes(netPlan.getSRGSpecificAttributes(srgId));
            
            srgs.put(new Long(srgId), srg);
        }
    }
    
    private int getId_int(Object elem)
    {
        if (elem instanceof NetworkElement.Node) return nodes.indexOf(elem);
        else if (elem instanceof NetworkElement.Demand) return demands.indexOf(elem);
        
        throw new RuntimeException("Bad");
    }

    private long getId_long(Object elem)
    {
	if (elem instanceof NetworkElement.Link) return links.getKey(elem);
	else if (elem instanceof NetworkElement.Route) return routes.getKey(elem);
        else if (elem instanceof NetworkElement.Segment) return segments.getKey(elem);
        else if (elem instanceof NetworkElement.SRG) return srgs.getKey(elem);
        
        throw new RuntimeException("Bad");
    }

    private List<Integer> getIds_int(Collection list)
    {
	List<Integer> ids = new LinkedList<Integer>();
	Iterator it = list.iterator();
	while (it.hasNext()) ids.add(getId_int(it.next()));

        return ids;
    }

    private List<Long> getIds_long(Collection list)
    {
	List<Long> ids = new LinkedList<Long>();
	Iterator it = list.iterator();
	while (it.hasNext()) ids.add(getId_long(it.next()));

        return ids;
    }
    
    private NetworkElement.Link getLink(long linkId)
    {
	try { return links.get(linkId); }
	catch (Exception ex) { throw new Net2PlanException(String.format("Link %d is not present in the network", linkId)); }
    }

    /**
     * Returns the value of a given attribute for a link. If not defined,
     * it is search for the network.
     *
     * @param linkId Link identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getLinkAttribute(long linkId, String key)
    {
	NetworkElement.Link link = getLink(linkId);
	String value = link.getAttribute(key);
	return value == null ? netPlan.getNetworkAttribute(key) : value;
    }

    /**
     * Returns the link capacity.
     *
     * @param linkId Link identifier
     * @return Link capacity in Erlangs
     * @since 0.2.3
     */
    public double getLinkCapacityInErlangs(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	return link.linkCapacityInErlangs;
    }

    /**
     * Returns the link capacity reserved for protection.
     *
     * @param linkId Link identifier
     * @return Link capacity reserved for protection
     * @since 0.2.3
     */
    public double getLinkCapacityReservedForProtectionInErlangs(long linkId)
    {
	double u_e = 0;
	long[] segmentList = getLinkTraversingProtectionSegments(linkId);
	for (long segmentId : segmentList) u_e += getProtectionSegmentReservedBandwidthInErlangs(segmentId);

	return u_e;
    }

    /**
     * Returns the link capacity not reserved for protection.
     *
     * @param linkId Link identifier
     * @return Link capacity not reserved for protection
     * @since 0.2.3
     */
    public double getLinkCapacityNotReservedForProtectionInErlangs(long linkId)
    {
	return getLinkCapacityInErlangs(linkId) - getLinkCapacityReservedForProtectionInErlangs(linkId);
    }

    /**
     * Returns the identifier of the destination node of the link.
     *
     * @param linkId Link identifier
     * @return Destination node identifier
     * @since 0.2.3
     */
    public int getLinkDestinationNode(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	return getId_int(link.destinationNode);
    }

    /**
     * Returns the length of the specified link.
     *
     * @param linkId Link identifier
     * @return Link length (measured in km)
     * @since 0.2.3
     */
    public double getLinkLengthInKm(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	return link.linkLengthInKm;
    }

    /**
     * Returns the identifier of the origin node of the link.
     *
     * @param linkId Link identifier
     * @return Origin node identifier
     * @since 0.2.3
     */
    public int getLinkOriginNode(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	return getId_int(link.originNode);
    }

    /**
     * Returns the links starting from a given node.
     *
     * @param nodeId Node identifiers
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getNodeOutgoingRoutes(int nodeId)
    {
	List<Long> outgoingRouteIds = new LinkedList<Long>();
	int[] demandIds = netPlan.getNodeOutgoingDemands(nodeId);
	for(int demandId : demandIds)
	{
	    long[] routeIds = getDemandRoutes(demandId);
	    for(long routeId : routeIds) outgoingRouteIds.add(routeId);
	}

	return LongUtils.toArray(outgoingRouteIds);
    }

    /**
     * Returns the routes ending in a given node.
     *
     * @param nodeId Node identifiers
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getNodeIncomingRoutes(int nodeId)
    {
	List<Long> incomingRouteIds = new LinkedList<Long>();
	int[] demandIds = netPlan.getNodeOutgoingDemands(nodeId);
	for(int demandId : demandIds)
	{
	    long[] routeIds = getDemandRoutes(demandId);
	    for(long routeId : routeIds) incomingRouteIds.add(routeId);
	}

	return LongUtils.toArray(incomingRouteIds);
    }

    /**
     * Returns the links ending in a given node.
     *
     * @param nodeId Node identifiers
     * @return Link identifiers
     * @since 0.2.3
     */
    public long[] getNodeIncomingLinks(int nodeId)
    {
	List<Long> incomingLinkIds = new LinkedList<Long>();
        long[] linkIds = getLinkIds();

	for (long linkId : linkIds)
	{
	    if (getLinkDestinationNode(linkId) == nodeId)
		incomingLinkIds.add(linkId);
	}

	return LongUtils.toArray(incomingLinkIds);
    }

    /**
     * Returns the links starting from a given node.
     *
     * @param nodeId Node identifiers
     * @return Link identifiers
     * @since 0.2.3
     */
    public long[] getNodeOutgoingLinks(int nodeId)
    {
	List<Long> outgoingLinkIds = new LinkedList<Long>();

        long[] linkIds = getLinkIds();
	for (long linkId : linkIds)
	{
	    if (getLinkOriginNode(linkId) == nodeId) outgoingLinkIds.add(linkId);
	}

	return LongUtils.toArray(outgoingLinkIds);
    }

    /**
     * Returns the number of unidirectional links defined within the network.
     *
     * @return The number of unidirectional links defined within the network
     * @since 0.2.3
     */
    public int getNumberOfLinks() { return links.size(); }
    
    /**
     * Returns the number of routes for traffic demands defined within the network.
     *
     * @return The number of routes for traffic demands defined within the network
     * @since 0.2.3
     */
    public int getNumberOfRoutes() { return routes.size(); }

    /**
     * Returns the number of protection segments defined within the network.
     *
     * @return The number of protection segments defined within the network
     * @since 0.2.3
     */
    public int getNumberOfProtectionSegments() { return segments.size(); }

    private NetworkElement.Route getRoute(long routeId)
    {
	try { return routes.get(routeId); }
	catch (Exception ex) { throw new Net2PlanException(String.format("Route %d is not defined in the network", routeId)); }
    }

    /**
     * Returns the value of a given attribute for a route. If not defined,
     * it is search for the network.
     *
     * @param routeId Route identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getRouteAttribute(long routeId, String key)
    {
	NetworkElement.Route route = getRoute(routeId);
	String value = route.getAttribute(key);
	return value == null ? netPlan.getNetworkAttribute(key) : value;
    }

    /**
     * Returns the attributes for a given route.
     *
     * @param routeId Route identifier
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String, String> getRouteSpecificAttributes(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	return route.getAttributes();
    }

    /**
     * Returns the protection segments defined for a given route.
     *
     * @param routeId Route identifier
     * @return Protection segment identifiers
     * @since 0.2.3
     */
    public long[] getRouteBackupSegmentList(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	List<Long> segmentList = getIds_long(getRouteBackupSegments(route));
	return LongUtils.toArray(segmentList);
    }

    private List<NetworkElement.Segment> getRouteBackupSegments(NetworkElement.Route route)
    {
	return route.backupSegments;
    }

    /**
     * Returns the associated demand for a given route.
     *
     * @param routeId Route identifier
     * @return Demand identifier
     * @since 0.2.3
     */
    public int getRouteDemand(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	return getId_int(route.demand);
    }

    /**
     * Returns the routes which can carry traffic from a given demand.
     *
     * @param demandId Demand identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getDemandRoutes(int demandId)
    {
	NetworkElement.Demand demand = getDemand(demandId);
	List<Long> routeList = getIds_long(getRoutesFromDemand(demand));
	return LongUtils.toArray(routeList);
    }

    private Set<NetworkElement.Route> getRoutesFromDemand(NetworkElement.Demand demand)
    {
	Set<NetworkElement.Route> routesTraversingDemand = new HashSet<NetworkElement.Route>();

	Iterator<NetworkElement.Route> it = routes.values().iterator();
	while (it.hasNext())
	{
	    NetworkElement.Route route = it.next();
	    if (route.demand == demand)
	    {
		routesTraversingDemand.add(route);
	    }
	}

	return routesTraversingDemand;
    }

    /**
     * Returns the carried traffic for a given route.
     *
     * @param routeId Route identifier
     * @return Carried traffic in Erlangs
     * @since 0.2.3
     */
    public double getRouteCarriedTrafficInErlangs(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	return route.carriedTrafficInErlangs;
    }

    /**
     * Returns the length in kilometers for a given protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Length in km
     * @since 0.2.3
     */
    public double getProtectionSegmentLengthInKm(long segmentId)
    {
	double segmentLength = 0;
	long[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
	for(long linkId : seqLinks)
	    segmentLength += getLinkLengthInKm(linkId);

	return segmentLength;
    }

    /**
     * Returns the number of hops for a given protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Number of hops
     * @since 0.2.3
     */
    public int getProtectionSegmentNumberOfHops(long segmentId)
    {
	return getProtectionSegmentSequenceOfLinks(segmentId).length;
    }

    /**
     * Returns the length in kilometers for each route.
     *
     * @param routeId Route identifier
     * @return Length in km
     * @since 0.2.3
     */
    public double getRouteLengthInKm(long routeId)
    {
	double routeLength = 0;
	long[] seqLinks = getRouteSequenceOfLinks(routeId);
	for(long linkId : seqLinks)
	    routeLength += getLinkLengthInKm(linkId);

	return routeLength;
    }

    /**
     * Returns the number of hops for a given route.
     *
     * @param routeId Route identifier
     * @return Number of hops
     * @since 0.2.3
     */
    public int getRouteNumberOfHops(long routeId) { return getRouteSequenceOfLinks(routeId).length; }

    /**
     * Returns the sequence of links traversed by a route.
     *
     * @param routeId Route identifier
     * @return Sequence of links
     * @since 0.2.3
     */
    public long[] getRouteSequenceOfLinks(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	List<Long> sequenceOfLinks = getIds_long(getRoutePlannedSequenceOfLinks(route));
	return LongUtils.toArray(sequenceOfLinks);
    }

    private List<NetworkElement.Link> getRoutePlannedSequenceOfLinks(NetworkElement.Route route)
    {
	return sequenceOfPathElements2sequenceOfLinks(route.plannedRoute);
    }

    /**
     * Returns the sequence of nodes traversed by a route.
     *
     * @param routeId Route identifier
     * @return Sequence of nodes
     * @since 0.2.3
     */
    public int[] getRouteSequenceOfNodes(long routeId)
    {
	NetworkElement.Route route = getRoute(routeId);
	List<Integer> sequenceOfNodes = getIds_int(getRoutePlannedSequenceOfNodes(route));
	return IntUtils.toArray(sequenceOfNodes);
    }

    private List<NetworkElement.Node> getRoutePlannedSequenceOfNodes(NetworkElement.Route route)
    {
	return sequenceOfPathElements2sequenceOfNodes(getRoutePlannedSequenceOfLinks(route));
    }

    /**
     * Returns the routes traversing a given link.
     *
     * @param linkId Link identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getLinkTraversingRoutes(long linkId)
    {
	LongArrayList out = new LongArrayList();
        long[] routeIds = getRouteIds();

        for(long routeId : routeIds)
	    if (LongUtils.contains(getRouteSequenceOfLinks(routeId), linkId))
		out.add(routeId);

	out.trimToSize();
	return out.elements();
    }

    private NetworkElement.Segment getSegment(long segmentId)
    {
	try { return segments.get(segmentId); }
	catch (Exception ex) { throw new Net2PlanException(String.format("Segment %d is not defined in the network", segmentId)); }
    }

    /**
     * Returns the value of a given attribute for a protection segment. If not defined,
     * it is search for the network.
     *
     * @param segmentId Protection segment identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getProtectionSegmentAttribute(long segmentId, String key)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	String value = segment.getAttribute(key);
	return value == null ? netPlan.getNetworkAttribute(key) : value;
    }

    /**
     * Returns the attributes for a given protection segment.
     *
     * @param segmentId Segment identifier
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String, String> getProtectionSegmentSpecificAttributes(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	return segment.getAttributes();
    }

    /**
     * Returns the identifier of the destination node of the protection segment.
     *
     * @param segmentId Segment identifier
     * @return Destination node identifier
     * @since 0.2.3
     */
    public int getProtectionSegmentDestinationNode(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	return getId_int(segment.getLastNode());
    }

    /**
     * Returns the identifier of the origin node of the protection segment.
     *
     * @param segmentId Segment identifier
     * @return Origin node identifier
     * @since 0.2.3
     */
    public int getProtectionSegmentOriginNode(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	return getId_int(segment.getFirstNode());
    }

    /**
     * Returns the reserved bandwidth for a given protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Reserved bandwidth in Erlangs
     * @since 0.2.3
     */
    public double getProtectionSegmentReservedBandwidthInErlangs(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	return segment.reservedBandwithInErlangs;
    }

    /**
     * Returns the routes which share a given protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getProtectionSegmentRoutes(long segmentId)
    {
        LongArrayList out = new LongArrayList();
        long[] routeIds = getRouteIds();

        for (long routeId : routeIds)
        {
            long[] backupSegments = getRouteBackupSegmentList(routeId);
            if (LongUtils.find(backupSegments, segmentId, Constants.SearchType.FIRST).length != 0)
                out.add(routeId);
        }
        
        out.trimToSize();
        
        return out.elements();
    }

    /**
     * Returns the sequence of links traversed by a protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Sequence of links
     * @since 0.2.3
     */
    public long[] getProtectionSegmentSequenceOfLinks(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	List<Long> sequenceOfLinks = getIds_long(getSegmentSequenceOfLinks(segment));
	return LongUtils.toArray(sequenceOfLinks);
    }

    private List<NetworkElement.Link> getSegmentSequenceOfLinks(NetworkElement.Segment segment)
    {
	List<NetworkElement.Link> sequenceOfLinks = segment.route;
	return sequenceOfLinks;
    }

    /**
     * Returns the sequence of nodes traversed by a protection segment.
     *
     * @param segmentId Protection segment identifier
     * @return Sequence of nodes
     * @since 0.2.3
     */
    public int[] getProtectionSegmentSequenceOfNodes(long segmentId)
    {
	NetworkElement.Segment segment = getSegment(segmentId);
	List<Integer> sequenceOfNodes = getIds_int(getSegmentSequenceOfNodes(segment));
	return IntUtils.toArray(sequenceOfNodes);
    }

    private List<NetworkElement.Node> getSegmentSequenceOfNodes(NetworkElement.Segment segment)
    {
	return sequenceOfPathElements2sequenceOfNodes(segment.route);
    }

    /**
     * Returns the protection segments traversing a given link.
     *
     * @param linkId Link identifier
     * @return Protection segment identifiers
     * @since 0.2.3
     */
    public long[] getLinkTraversingProtectionSegments(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	List<Long> segmentList = getIds_long(getSegmentsTraversingLink(link));
	return LongUtils.toArray(segmentList);
    }

    /**
     * Returns the routes traversing a given node.
     *
     * @param nodeId Node identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getNodeTraversingRoutes(int nodeId)
    {
	List<Long> routeList = new LinkedList<Long>();
        long[] routeIds = getRouteIds();

	for(long routeId : routeIds)
	{
	    int[] seqNodes = getRouteSequenceOfNodes(routeId);
	    if (IntUtils.contains(seqNodes, nodeId))
		routeList.add(routeId);
	}

	return LongUtils.toArray(routeList);
    }

    /**
     * Returns the protection segments traversing a given node.
     *
     * @param nodeId Node identifier
     * @return Protection segment identifiers
     * @since 0.2.3
     */
    public long[] getNodeTraversingProtectionSegments(int nodeId)
    {
	List<Long> segmentList = new LinkedList<Long>();

        long[] segmentIds = getProtectionSegmentIds();
	for(long segmentId : segmentIds)
	{
	    int[] seqNodes = getProtectionSegmentSequenceOfNodes(segmentId);
	    if (IntUtils.contains(seqNodes, nodeId))
		segmentList.add(segmentId);
	}

	return LongUtils.toArray(segmentList);
    }

    private Set<NetworkElement.Segment> getSegmentsTraversingLink(NetworkElement.Link link)
    {
	Set<NetworkElement.Segment> segmentsTraversingLink = new HashSet<NetworkElement.Segment>();
	for (NetworkElement.Segment segment : segments.values())
	{
	    if (segment.route.contains(link))
	    {
		segmentsTraversingLink.add(segment);
	    }
	}

	return segmentsTraversingLink;
    }

    /**
     * Returns <code>true</code> if the network has at least one unidirectional link. It is equivalent to <code>{@link #getNumberOfLinks getNumberOfLinks()} > 0</code>.
     * @return <code>true</code> if there are links within the network, and <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean hasLinks() { return !links.isEmpty(); }

    /**
     * Returns <code>true</code> if the network has at least one traffic route. It is equivalent to <code>{@link #getNumberOfRoutes getNumberOfRoutes()} > 0</code>.
     *
     * @return <code>true</code> if there are routes defined for any traffic demand in the network, and <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean hasRoutes() { return !routes.isEmpty(); }

    /**
     * Returns <code>true</code> if the network has at least one protection segment. It is equivalent to <code>{@link #getNumberOfProtectionSegments getNumberOfProtectionSegments()} > 0</code>.
     *
     * @return <code>true</code> if there are protection segments defined within the network, and <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean hasProtectionSegments() { return !segments.isEmpty(); }

    /**
     * Tests whether a given protection segment is applicable to a route. A protection segment is applicable is contained within the route.
     *
     * @param segmentId Segment identifier
     * @param routeId Route identifier
     * @return <code>true</code> is segment is applicable to route, <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean checkProtectionSegmentMergeabilityToRoute(long routeId, long segmentId)
    {
	NetworkElement.Route route = getRoute(routeId);
	NetworkElement.Segment segment = getSegment(segmentId);

	return isSegmentApplicableToRoute(route, segment);
    }

    private static boolean isSegmentApplicableToRoute(NetworkElement.Route route, NetworkElement.Segment segment)
    {
	NetworkElement.Node head = segment.getFirstNode();
	NetworkElement.Node tail = segment.getLastNode();

	List<NetworkElement.Node> sequenceOfNodes = sequenceOfPathElements2sequenceOfNodes(route.plannedRoute);

	int headPos = sequenceOfNodes.indexOf(head);
	int tailPos = sequenceOfNodes.lastIndexOf(tail);

	if (headPos == -1 || tailPos == -1 || tailPos <= headPos)
	{
	    return false;
	}

	return true;
    }
    
    /**
     * Returns the identifier of the ingress node of the route.
     *
     * @param routeId Route identifier
     * @return Ingress node identifier
     * @since 0.2.3
     */
    public int getRouteIngressNode(long routeId)
    {
	int demandId = getRouteDemand(routeId);

	return netPlan.getDemandIngressNode(demandId);
    }

    /**
     * Returns the identifier of the egress node of the route.
     *
     * @param routeId Route identifier
     * @return Egress node identifier
     * @since 0.2.3
     */
    public int getRouteEgressNode(long routeId)
    {
	int demandId = getRouteDemand(routeId);

	return netPlan.getDemandEgressNode(demandId);
    }

    /**
     * Returns the routes going from a node to other one.
     *
     * @param node1 Ingress node
     * @param node2 Egress node
     * @return Route vector
     * @since 0.2.3
     */
    public long[] getNodePairRoutes(int node1, int node2)
    {
	Set<Long> routeSet = new TreeSet<Long>();
	routeSet.addAll(LongUtils.toList(getNodeOutgoingRoutes(node1)));
	routeSet.retainAll(LongUtils.toList(getNodeIncomingRoutes(node2)));

	return LongUtils.toArray(routeSet);
    }

    /**
     * Returns links between two nodes (in both senses).
     *
     * @param node1 Node 1
     * @param node2 Node 2
     * @return Link vector
     * @since 0.2.3
     */
    public long[] getNodePairBidirectionalLinks(int node1, int node2)
    {
	long[] downstreamLinks = getNodePairLinks(node1, node2);
	long[] upstreamLinks = getNodePairLinks(node2, node1);

	return LongUtils.union(downstreamLinks, upstreamLinks);
    }

    /**
     * Returns the links from a node to other one.
     *
     * @param node1 Origin node
     * @param node2 Destination node
     * @return Link vector
     * @since 0.2.3
     */
    public long[] getNodePairLinks(int node1, int node2)
    {
	Set<Long> linkSet = new TreeSet<Long>();
	linkSet.addAll(LongUtils.toList(getNodeOutgoingLinks(node1)));
	linkSet.retainAll(LongUtils.toList(getNodeIncomingLinks(node2)));

	return LongUtils.toArray(linkSet);
    }
    
    /**
     * Returns all SRGs associated to a given link.
     * 
     * @param linkId Link identifier
     * @return SRGs associated to the link
     * @since 0.2.3
     */
    public long[] getLinkSRGs(long linkId)
    {
        List<Long> out = new LinkedList<Long>();
        long[] srgIds = getSRGIds();
        
        for(long srgId : srgIds)
            if (LongUtils.contains(getSRGLinks(srgId), linkId))
                out.add(srgId);
            
        return LongUtils.toArray(out);
    }

    /**
     * Returns all SRGs associated to a given node.
     * 
     * @param nodeId Node identifier
     * @return SRGs associated to the node
     * @since 0.2.3
     */
    public long[] getNodeSRGs(int nodeId)
    {
        List<Long> out = new LinkedList<Long>();
        long[] srgIds = getSRGIds();
        
        for(long srgId : srgIds)
            if (IntUtils.contains(getSRGNodes(srgId), nodeId))
                out.add(srgId);
            
        return LongUtils.toArray(out);
    }
    
    /**
     * Returns all SRGs associated to a given protection segment. It is equal to
     * the union of the SRGs associated to each traversed node/link.
     * 
     * @param segmentId Segment identifier
     * @return SRGs associated to the protection segment
     * @since 0.2.3
     */
    public long[] getProtectionSegmentSRGs(long segmentId)
    {
        int[] nodeIds = getProtectionSegmentSequenceOfNodes(segmentId);
        long[] linkIds = getProtectionSegmentSequenceOfLinks(segmentId);
        
        Set<Long> out = new HashSet<Long>();
        long[] srgIds = getSRGIds();
        
        for(long srgId : srgIds)
        {
            if (IntUtils.containsAny(getSRGNodes(srgId), nodeIds))
                out.add(srgId);

            if (LongUtils.containsAny(getSRGLinks(srgId), linkIds))
                out.add(srgId);
        }
            
        return LongUtils.toArray(out);
    }

    /**
     * Returns all SRGs associated to a given route. It is equal to the union
     * of the SRGs associated to each traversed node/link.
     * 
     * @param routeId Route identifier
     * @return SRGs associated to the route
     * @since 0.2.3
     */
    public long[] getRouteSRGs(long routeId)
    {
        int[] nodeIds = getRouteSequenceOfNodes(routeId);
        long[] linkIds = getRouteSequenceOfLinks(routeId);
        
        Set<Long> out = new HashSet<Long>();
        long[] srgIds = getSRGIds();
        
        for(long srgId : srgIds)
        {
            if (IntUtils.containsAny(getSRGNodes(srgId), nodeIds))
                out.add(srgId);

            if (LongUtils.containsAny(getSRGLinks(srgId), linkIds))
                out.add(srgId);
        }
            
        return LongUtils.toArray(out);
    }

    /**
     * Returns the number of SRGs defined within the network.
     * 
     * @return Number of SRGs
     * @since 0.2.3
     */
    public int getNumberOfSRGs() { return srgs.size(); }
    
    private NetworkElement.SRG getSRG(long srgId) { return srgs.get(srgId); }
    
    /**
     * Returns the value of a given attribute for a SRG. If not defined, it is
     * search for the network.
     * 
     * @param srgId SRG identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getSRGAttribute(long srgId, String key)
    {
        NetworkElement.SRG srg = getSRG(srgId);
        String value = srg.getAttribute(key);
        if (value == null) value = netPlan.getNetworkAttribute(key);
        
        return value;
    }
    
    /**
     * Returns the availability of the given SRG. The availability is given by 
     * the quotient of MTTF and the mean time between failures (MTBF), which is 
     * equal to the sum of MTTF and MTTR.
     * 
     * @param srgId SRG identifier
     * @return Availability
     * @since 0.2.3
     */
    public double getSRGAvailability(long srgId)
    {
        double mttf = getSRGMeanTimeToFailInHours(srgId);
        if (mttf == Double.MAX_VALUE) return 1;
        
        double mttr = getSRGMeanTimeToRepairInHours(srgId);
        return mttf / (mttf + mttr);
    }
    
    /**
     * Returns the set of links in a given SRG.
     * 
     * @param srgId SRG identifier
     * @return Set of links in the SRG
     * @since 0.2.3
     */
    public long[] getSRGLinks(long srgId)
    {
        long[] linkIds = LongUtils.toArray(getIds_long(getSRG(srgId).links));
        Arrays.sort(linkIds);
        
        return linkIds;
    }
    
    /**
     * Returns the Mean Time To Fail (in hours) of the given SRG.
     * 
     * @param srgId SRG identifier
     * @return Mean Time To Fail (in hours)
     * @since 0.2.3
     */
    public double getSRGMeanTimeToFailInHours(long srgId) { return getSRG(srgId).mttf; }

    /**
     * Returns the Mean Time To Repair (in hours) of the given SRG.
     * 
     * @param srgId SRG identifier
     * @return Mean Time To Repair (in hours)
     * @since 0.2.3
     */
    public double getSRGMeanTimeToRepairInHours(long srgId) { return getSRG(srgId).mttr; }
    
    /**
     * Returns the set of nodes in a given SRG.
     * 
     * @param srgId SRG identifier
     * @return Set of nodes in the SRG
     * @since 0.2.3
     */
    public int[] getSRGNodes(long srgId)
    {
        int[] nodeIds = IntUtils.toArray(getIds_int(getSRG(srgId).nodes));
        Arrays.sort(nodeIds);
        
        return nodeIds;
    }
    
    /**
     * Returns the set of protection segments affected by a given SRG.
     * 
     * @param srgId SRG identifier
     * @return Set of protection segments
     * @since 0.2.3
     */
    public long[] getSRGProtectionSegments(long srgId)
    {
        int[] nodeIds = getSRGNodes(srgId);
        long[] linkIds = getSRGLinks(srgId);
        
        Set<Long> out = new TreeSet<Long>();
        long[] segmentIds = getProtectionSegmentIds();

        for(long segmentId : segmentIds)
        {
            long[] sequenceOfLinks = getProtectionSegmentSequenceOfLinks(segmentId);
            if (LongUtils.containsAny(sequenceOfLinks, linkIds))
                out.add(segmentId);
            
            int[] sequenceOfNodes = getProtectionSegmentSequenceOfNodes(segmentId);
            if (IntUtils.containsAny(sequenceOfNodes, nodeIds))
                out.add(segmentId);
        }
        
        return LongUtils.toArray(out);
    }

    /**
     * Returns the set of routes affected by a given SRG.
     * 
     * @param srgId SRG identifier
     * @return Set of routes
     * @since 0.2.3
     */
    public long[] getSRGRoutes(long srgId)
    {
        int[] nodeIds = getSRGNodes(srgId);
        long[] linkIds = getSRGLinks(srgId);
        
        Set<Long> out = new HashSet<Long>();
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            long[] sequenceOfLinks = getRouteSequenceOfLinks(routeId);
            if (LongUtils.containsAny(sequenceOfLinks, linkIds))
                out.add(routeId);
            
            int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);
            if (IntUtils.containsAny(sequenceOfNodes, nodeIds))
                out.add(routeId);
        }
        
        return LongUtils.toArray(out);
    }
    
    /**
     * Returns the attributes for a given SRG.
     * 
     * @param srgId SRG identifier
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String,String> getSRGSpecificAttributes(long srgId)
    {
	NetworkElement.SRG srg = getSRG(srgId);
	return srg.getAttributes();
    }
    
    /**
     * Returns <code>true</code> if the network has at least one SRG. 
     * It is equivalent to {@link #getNumberOfSRGs() getNumberOfSRGs()} > 0.
     * 
     * @return <code>true</code> if there are SRGs defined for the network, and <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean hasSRGs() { return getNumberOfSRGs() > 0; }

    /**
     * Returns the egress traffic per node.
     *
     * @return Egress traffic per node
     * @since 0.2.3
     */
    public double[] getNodeEgressTrafficInErlangsVector()
    {
	int N = netPlan.getNumberOfNodes();
	double[] egressTraffic = new double[N];
        
        long[] routeIds = getRouteIds();
	for (long routeId: routeIds)
	{
	    int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);

	    egressTraffic[sequenceOfNodes[sequenceOfNodes.length - 1]] += getRouteCarriedTrafficInErlangs(routeId);
	}

	return egressTraffic;
    }

    /**
     * Returns the ingress traffic per node.
     *
     * @return Ingress traffic per node
     * @since 0.2.3
     */
    public double[] getNodeIngressTrafficInErlangsVector()
    {
	int N = netPlan.getNumberOfNodes();
	double[] ingressTraffic = new double[N];
        
        long[] routeIds = getRouteIds();
	for (long routeId: routeIds)
	{
	    int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);

	    ingressTraffic[sequenceOfNodes[0]] += getRouteCarriedTrafficInErlangs(routeId);
	}

	return ingressTraffic;
    }

    /**
     * Returns the traversing traffic (no ingress, no egress) per node.
     *
     * @return Traversing traffic per node
     * @since 0.2.3
     */
    public double[] getNodeTraversingTrafficInErlangsVector()
    {
	int N = netPlan.getNumberOfNodes();
	double[] traversingTraffic = new double[N];
        
        long[] routeIds = getRouteIds();
	for (long routeId: routeIds)
	{
	    int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);
	    double carriedTraffic = getRouteCarriedTrafficInErlangs(routeId);

	    for (int seqId = 1; seqId < sequenceOfNodes.length - 1; seqId++)
	    {
		traversingTraffic[sequenceOfNodes[seqId]] += carriedTraffic;
	    }
	}

	return traversingTraffic;
    }
    
    /**
     * Returns the set of current link identifiers.
     * 
     * @return Current link identifiers
     * @since 0.2.3
     */
    public long[] getLinkIds() { return LongUtils.toArray(new TreeSet<Long>(links.keySet())); }

    /**
     * Returns the set of current route identifiers.
     * 
     * @return Current route identifiers
     * @since 0.2.3
     */
    public long[] getRouteIds() { return LongUtils.toArray(new TreeSet<Long>(routes.keySet())); }

    /**
     * Returns the set of current protection segment identifiers.
     * 
     * @return Current protection segment identifiers
     * @since 0.2.3
     */
    public long[] getProtectionSegmentIds() { return LongUtils.toArray(new TreeSet<Long>(segments.keySet())); }
    
    /**
     * Returns the set of current SRG identifiers.
     * 
     * @return Current SRG identifiers
     * @since 0.2.3
     */
    public long[] getSRGIds() { return LongUtils.toArray(new TreeSet<Long>(srgs.keySet())); }

    /**
     * Returns the attributes of a given link.
     *
     * @param linkId Link identifier
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String, String> getLinkSpecificAttributes(long linkId)
    {
	NetworkElement.Link link = getLink(linkId);
	return link.getAttributes();
    }

    private static List<NetworkElement.Node> sequenceOfPathElements2sequenceOfNodes(List<? extends NetworkElement.PathElement> sequenceOfPathElements)
    {
	LinkedList<NetworkElement.Node> sequenceOfNodes = new LinkedList<NetworkElement.Node>();
	int ct = 0;
	for (Iterator<? extends NetworkElement.PathElement> it = sequenceOfPathElements.iterator(); it.hasNext();)
	{
	    if (ct > 0) sequenceOfNodes.removeLast();
	    sequenceOfNodes.addAll(it.next().getSequenceOfNodes());
	    ct++;
	}

	return sequenceOfNodes;
    }

    /**
     * Returns the carried traffic for a given demand.
     *
     * @param demandId Demand identifier
     * @return Carried traffic in Erlangs
     * @since 0.2.3
     */
    public double getDemandCarriedTrafficInErlangs(int demandId)
    {
	long[] routeIds = getDemandRoutes(demandId);
	double carriedTraffic = 0;

	for(long routeId : routeIds)
	    carriedTraffic += getRouteCarriedTrafficInErlangs(routeId);

	return carriedTraffic;
    }

    /**
     * Returns the offered traffic for a given demand.
     *
     * @param demandId Demand identifier
     * @return Offered traffic in Erlangs
     * @since 0.2.3
     */
    public double getDemandOfferedTrafficInErlangs(int demandId)
    {
        return getDemand(demandId).offeredTrafficInErlangs;
    }
    
    /**
     * Returns the carried traffic by a given link.
     *
     * @param linkId Link identifier
     * @return Carried traffic in Erlangs
     * @since 0.2.3
     */
    public double getLinkCarriedTrafficInErlangs(long linkId)
    {
	long[] routeIds = getLinkTraversingRoutes(linkId);
	double carriedTraffic = 0;

	for(long routeId : routeIds)
	    carriedTraffic += getRouteCarriedTrafficInErlangs(routeId);

	return carriedTraffic;
    }

    /**
     * Returns the carried traffic for each demand.
     *
     * @return Carried traffic in Erlangs vector
     * @since 0.2.3
     */
    public double[] getDemandCarriedTrafficInErlangsVector()
    {
	int D = netPlan.getNumberOfDemands();

	double[] carriedTraffic = new double[D];
        long[] routeIds = getRouteIds();
	for (long routeId : routeIds)
	{
	    int demandId = getRouteDemand(routeId);
	    carriedTraffic[demandId] += getRouteCarriedTrafficInErlangs(routeId);
	}

	return carriedTraffic;
    }

    /**
     * Returns the offered traffic for each demand.
     *
     * @return Offered traffic in Erlangs vector
     * @since 0.2.3
     */
    public double[] getDemandOfferedTrafficInErlangsVector()
    {
	int D = netPlan.getNumberOfDemands();
	double[] h_d = new double[D];
        for(int demandId = 0; demandId < D; demandId++)
            h_d[demandId] = getDemand(demandId).offeredTrafficInErlangs;

	return h_d;
    }
    
    private NetworkElement.Demand getDemand(int demandId)
    {
	try { return demands.get(demandId); }
	catch (Exception ex) { throw new Net2PlanException(String.format("Demand %d is not defined in the network", demandId)); }
    }

    private NetworkElement.Node getNode(int nodeId)
    {
	try { return nodes.get(nodeId); }
	catch (Exception ex) { throw new Net2PlanException(String.format("Node %d is not defined in the network", nodeId)); }
    }

    private static List<NetworkElement.Link> sequenceOfPathElements2sequenceOfLinks(List<? extends NetworkElement.PathElement> sequenceOfPathElements)
    {
	List<NetworkElement.Link> sequenceOfLinks = new LinkedList<NetworkElement.Link>();

	for (NetworkElement.PathElement pathElement : sequenceOfPathElements)
	{
	    sequenceOfLinks.addAll(pathElement.getSequenceOfLinks());
	}

	return sequenceOfLinks;
    }
}
