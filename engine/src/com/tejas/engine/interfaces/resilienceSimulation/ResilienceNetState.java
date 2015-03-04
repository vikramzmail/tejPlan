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

package com.tejas.engine.interfaces.resilienceSimulation;


import cern.colt.list.tint.IntArrayList;
import cern.colt.list.tlong.LongArrayList;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.jet.math.tint.IntFunctions;

import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;

import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.LINK_FAILURE;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.LINK_REPARATION;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.NODE_FAILURE;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.NODE_REPARATION;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.SRG_FAILURE;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.SRG_REPARATION;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction.ActionType;
import com.tejas.engine.internal.NetworkElement;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Represents the current state of a network within the resilience simulation:
 * SRGs/nodes/links up/down, current carried traffic per link...</p>
 * 
 * <p><b>Important</b>: Users only should use this class only for queries via
 * <code>getX()</code> methods, since kernel is in charge of modifying the network
 * state using the events ({@link com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent ResilienceEvent})
 * scheduled by the event generator (see {@link com.tejas.engine.interfaces.resilienceSimulation.IResilienceEventGenerator IResilienceEventGenerator})
 * and the actions (see {@link com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction ProvisioningAction})
 * provided by the event processor (see {@link com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm IProvisioningAlgorithm}).</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class ResilienceNetState extends SimState
{
    private int N, E, D;
    private int[][] N_f, E_f;
    private double[] u_e, h_d;
    private int[][] linkTable;

    private Map<Long, ActiveRoute> activeRoutes;
    private Map<Long, ActiveProtectionSegment> activeSegments;
    
    private long nextRouteId;
    private long nextSegmentId;
    
    // State variables
    private Set<Integer> srgsDown;

    // Cache variables
    private Set<Integer> aux_linksDown;
    private Set<Integer> aux_nodesDown;
    private Set<Integer> aux_linksUp;
    private Set<Integer> aux_nodesUp;
    
    private ResilienceNetState() { super(null); }
    
    /**
     * Default constructor. Initializes the network state using existing information (SRGs, routes and protection segments).
     *
     * @param netPlan Initial network design
     * @since 0.2.0
     */
    public ResilienceNetState(NetPlan netPlan)
    {
        super(netPlan);
        
        u_e = netPlan.getLinkCapacityInErlangsVector();
        h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        N = netPlan.getNumberOfNodes();
        E = netPlan.getNumberOfLinks();
        D = netPlan.getNumberOfDemands();
        
        N_f = netPlan.getSRGNodesVector();
        E_f = netPlan.getSRGLinksVector();
        
        linkTable = netPlan.getLinkTable();
        
	isModifiable = true;

        activeRoutes = new HashMap<Long, ActiveRoute>();
        activeSegments = new HashMap<Long, ActiveProtectionSegment>();
        
	srgsDown = new HashSet<Integer>();
	aux_linksDown = new HashSet<Integer>();
	aux_nodesDown = new HashSet<Integer>();
	aux_linksUp = new HashSet<Integer>();
	aux_nodesUp = new HashSet<Integer>();

        reset();
    }

    private long addProtectionSegment(int[] sequenceOfLinks, double reservedBandwidthInErlangs, Map<String, String> attributes)
    {
        GraphUtils.checkRouteContinuity(linkTable, sequenceOfLinks, GraphUtils.CheckRoutingCycleType.NO_REPEAT_NODE);
        
        long segmentId = getNextProtectionSegmentId();
        nextSegmentId++;
        
        ActiveProtectionSegment segment = new ActiveProtectionSegment(reservedBandwidthInErlangs, sequenceOfLinks, attributes);
        activeSegments.put(segmentId, segment);
        
        return segmentId;
    }
    
    private long addRoute(int demandId, double carriedTrafficInErlangs, int[] sequenceOfLinks, long[] backupSegmentIds, Map<String, String> attributes)
    {
        long routeId = getNextRouteId();

        ActiveRoute route = new ActiveRoute(demandId, carriedTrafficInErlangs, sequenceOfLinks, backupSegmentIds, attributes);
        activeRoutes.put(routeId, route);
        
        netPlan.checkRouteValidityForDemand(sequenceOfLinks, demandId);
        for(long segmentId : backupSegmentIds)
            checkProtectionSegmentMergeabilityToRoute(routeId, segmentId);
        
        nextRouteId++;
        
        return routeId;
    }
    
    /**
     * Returns the current carried traffic per link. The carried traffic is equal to the
     * sum of the traffic of each route traversing the link, except for protection segments.
     * 
     * @return Current carried traffic per link (in Erlangs)
     * @since 0.2.3
     */
    public double[] getLinkCurrentCarriedTrafficInErlangsVector()
    {
        double[] y_e = new double[E];
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            double trafficVolume = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
            
            for(long itemId : seqLinksAndSegments)
            {
                if (itemId >= 0)
                {
                    int linkId = (int) itemId;
                    y_e[linkId] += trafficVolume;
                }
                else
                {
                    long segmentId = -1 - itemId;
                    if (getProtectionSegmentReservedBandwidthInErlangs(segmentId) == 0)
                    {
                        int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
                        for(int linkId : seqLinks)
                            y_e[linkId] += trafficVolume;
                    }
                }
            }
        }
        
        return y_e;
    }
    
    /**
     * Returns the set of over-subscribed links. A link is said to be over-subscribed 
     * when its carried traffic (sum of carried traffic for primary paths, reserved 
     * bandwidth for protection, and used capacity for restoration) is greater than the capacity.
     * 
     * @return Set of over-subscribed links
     * @since 0.2.3
     */
    public int[] getLinksOversubscribed()
    {
        double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
        
        double[] y_e = getLinkCurrentCarriedTrafficInErlangsVector();
        
        long[] segmentIds = getProtectionSegmentIds();
        for(long segmentId : segmentIds)
        {
            int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidthInErlangs = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            if (reservedBandwidthInErlangs == 0)
            {
                double carriedTraffic = getProtectionSegmentCurrentCarriedTrafficInErlangs(segmentId);
                for(int linkId : seqLinks) y_e[linkId] += carriedTraffic;
            }
            else
            {
                for(int linkId : seqLinks) y_e[linkId] += reservedBandwidthInErlangs;
            }
        }
        
        IntArrayList linkIds = new IntArrayList();
        for(int linkId = 0; linkId < E; linkId++)
            if (y_e[linkId] > u_e[linkId] + PRECISIONFACTOR)
                linkIds.add(linkId);
        
        linkIds.trimToSize();
        return linkIds.elements();
    }
    
    /**
     * Returns the current carried traffic of the given protection segment.
     * 
     * @param segmentId Segment identifier
     * @return Current traffic volume (in Erlangs)
     * @since 0.2.3
     */
    public double getProtectionSegmentCurrentCarriedTrafficInErlangs(long segmentId)
    {
        double r_s = 0;
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            double trafficVolume = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
            for(long itemId : seqLinksAndSegments)
            {
                if (itemId >= 0) continue;
                
                long aux_segmentId = -1 - itemId;
                if (aux_segmentId == segmentId)
                {
                    r_s += trafficVolume;
                    break;
                }
            }
        }
        
        return r_s;
    }

    @Override
    public void checkValidity(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic)
    {
	double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
        
        if (!allowLinkOversubscription)
        {
            int[] linkIds = getLinksOversubscribed();
            if (linkIds.length > 0) throw new Net2PlanException("Some links are over-subscribed");
        }
        
        if (!allowExcessCarriedTraffic)
        {
            double[] r_d = getDemandCurrentCarriedTrafficInErlangsVector();
            
            for(int demandId = 0; demandId < D; demandId++)
		if (h_d[demandId] < r_d[demandId] - PRECISIONFACTOR)
                    throw new Net2PlanException(String.format("Carried traffic for demand %d overcomes the offered traffic (offered = %f E, carried = %f E)", demandId, h_d[demandId], r_d[demandId]));
        }
    }

    /**
     * Returns a network design from the current network state.
     * 
     * @return Network design
     * @since 0.2.0
     */
    @Override
    public NetPlan convertToNetPlan()
    {
        NetPlan aux_netPlan = netPlan.copy();
        
        aux_netPlan.removeAllRoutes();
        aux_netPlan.removeAllProtectionSegments();

        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int demandId = getRouteDemand(routeId);
            int[] aux_seqLinks = convertBackupRoute2SequenceOfLinks(getRouteCurrentSequenceOfLinksAndSegments(routeId));
            double carriedTrafficInErlangs = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            Map<String, String> aux_routeAttributes = getRouteSpecificAttributes(routeId);
            
            aux_netPlan.addRoute(demandId, carriedTrafficInErlangs, aux_seqLinks, null, aux_routeAttributes);
        }
        
        return aux_netPlan;
    }
    
    /**
     * Returns the current carried traffic per demand.
     * 
     * @return Current carried traffic per demand (in Erlangs)
     * @since 0.2.3
     */
    public double[] getDemandCurrentCarriedTrafficInErlangsVector()
    {
        double[] r_d = new double[D];
        long[] routeIds = getRouteIds();
        
        for(long routeId : routeIds)
        {
            int demandId = getRouteDemand(routeId);
            double trafficVolume = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            
            r_d[demandId] += trafficVolume;
        }
        
        return r_d;
    }
    
    /**
     * Returns the routes carrying traffic for a given demand.
     * 
     * @param demandId Demand identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getDemandRoutes(int demandId)
    {
        Set<Long> aux = new TreeSet<Long>();
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int aux_demandId = getRouteDemand(routeId);
            if (aux_demandId == demandId)
                aux.add(routeId);
        }
        
        return LongUtils.toArray(aux);
    }
    
    /**
     * Checks if a sequence of links is valid for a given demand.
     *
     * @param demandId Demand identifier
     * @param sequenceOfLinksAndSegments Sequence of links and segments
     * @since 0.2.3
     */
    public void checkRouteValidityForDemand(long[] sequenceOfLinksAndSegments, int demandId)
    {
	int[] actualSeqLinks = convertBackupRoute2SequenceOfLinks(sequenceOfLinksAndSegments);
	GraphUtils.checkRouteContinuity(linkTable, actualSeqLinks, GraphUtils.CheckRoutingCycleType.NO_CHECK);

	int ingressNodeId = netPlan.getDemandIngressNode(demandId);
	int egressNodeId = netPlan.getDemandEgressNode(demandId);

	int originNodeIdFirstLink = netPlan.getLinkOriginNode(actualSeqLinks[0]);
	int destinationNodeIdLastLink = netPlan.getLinkDestinationNode(actualSeqLinks[actualSeqLinks.length - 1]);

	if (ingressNodeId != originNodeIdFirstLink)
	    throw new Net2PlanException("Ingress node of the demand and origin node of the first link in the route doesn't match");

	if (egressNodeId != destinationNodeIdLastLink)
	    throw new Net2PlanException("Egress node of the demand and destination node of the last link in the route doesn't match");
    }

    /**
     * Returns the new route resulting from applying a protection segment.
     *
     * @param currentPath Current sequence of links
     * @param segmentId   Protection segment identifier
     * @return New sequence of links
     * @since 0.2.3
     */
    public long[] getMergedBackupRoute(long[] currentPath, long segmentId)
    {
        int[] actualCurrentPath = convertBackupRoute2SequenceOfLinks(currentPath);
	GraphUtils.checkRouteContinuity(linkTable, actualCurrentPath, GraphUtils.CheckRoutingCycleType.NO_CHECK);

	int[] originalSeqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(actualCurrentPath);

	int headNode = getProtectionSegmentOriginNode(segmentId);
	int tailNode = getProtectionSegmentDestinationNode(segmentId);

	int[] headIndex = IntUtils.find(originalSeqNodes, headNode, Constants.SearchType.FIRST);
	int[] tailIndex = IntUtils.find(originalSeqNodes, tailNode, Constants.SearchType.LAST);

	if (headIndex.length == 0) throw new RuntimeException("Protection segment not applicable to the current route (first node is not in the path)");
	if (tailIndex.length == 0) throw new RuntimeException("Protection segment not applicable (last node is not in the original route)");
	if (tailIndex[0] < headIndex[0]) throw new RuntimeException("rotection segment goes upstream");

	long[] initialSegment = Arrays.copyOfRange(currentPath, 0, headIndex[0]);
	long[] finalSegment = Arrays.copyOfRange(currentPath, tailIndex[0], currentPath.length);

	long[] mergedRoute = LongUtils.concatenate(initialSegment, new long[]{-1 - segmentId}, finalSegment);
	int[] actualMergedRoute = convertBackupRoute2SequenceOfLinks(mergedRoute);
	GraphUtils.checkRouteContinuity(linkTable, actualMergedRoute, GraphUtils.CheckRoutingCycleType.NO_CHECK);

	return mergedRoute;
    }
    
    /**
     * Returns the origin node of a given protection segment.
     * 
     * @param segmentId Segment identifier
     * @return Node identifier
     * @since 0.2.3
     */
    public int getProtectionSegmentOriginNode(long segmentId)
    {
        ActiveProtectionSegment segment = getProtectionSegment(segmentId);
        int firstLinkId = segment.seqLinks[0];
        
        return netPlan.getLinkOriginNode(firstLinkId);
    }
    
    /**
     * Returns the destination node of a given protection segment.
     * 
     * @param segmentId Segment identifier
     * @return Node identifier
     * @since 0.2.3
     */
    public int getProtectionSegmentDestinationNode(long segmentId)
    {
        ActiveProtectionSegment segment = getProtectionSegment(segmentId);
        int lastLinkId = segment.seqLinks[segment.seqLinks.length - 1];
        
        return netPlan.getLinkDestinationNode(lastLinkId);
    }

    /**
     * Returns the effects of a failure event.
     * 
     * @param event Node/link failure event
     * @param nodesDown Set of nodes which are down
     * @param linksDown Set of links which are down
     * @param affectedRoutes Routes which are going down and might be rerouted
     * @param unrecoverableRoutes Routes which are going down but cannot be rerouted (ingress/egress node is down)
     * @param currentLinkAvailableCapacity Current available capacity per link (for links in down state it is equal to zero)
     * @param availableSegments Set of protection segments which are up and their corresponding available capacity
     * @since 0.2.0
     */
    public void getFailureEffects(ResilienceEvent event, Set<Integer> nodesDown, Set<Integer> linksDown, Set<Long> affectedRoutes, Set<Long> unrecoverableRoutes, List<Double> currentLinkAvailableCapacity, Map<Long, Double> availableSegments)
    {
	nodesDown.addAll(IntUtils.toList(getNodesDown()));
	linksDown.addAll(IntUtils.toList(getLinksDown()));

	switch(event.getEventType())
	{
	    case SRG_FAILURE:
	    case SRG_REPARATION:
		throw new RuntimeException("SRG events are not allowed here");

	    case NODE_REPARATION:
	    case LINK_REPARATION:
		throw new RuntimeException("Reparation events are not allowed");

	    case NODE_FAILURE:

		int nodeId = event.getId();
		nodesDown.add(nodeId);
		int[] linksAffected = netPlan.getNodeTraversingLinks(nodeId);
		linksDown.addAll(IntUtils.toList(linksAffected));

		break;

	    case LINK_FAILURE:

		int linkId = event.getId();
		linksDown.add(linkId);

		break;

	    default:
		throw new RuntimeException("Bad - Unknown event type");
	}
        
	int[] _nodesDown = IntUtils.toArray(nodesDown);
	int[] _linksDown = IntUtils.toArray(linksDown);
        
        double[] spareCapacity = DoubleUtils.copy(u_e);
        
        long[] segmentIds = getProtectionSegmentIds();
	Set<Long> _segmentsDown = getProtectionSegmentsDown(_nodesDown, _linksDown);
        
        for(long segmentId : segmentIds)
        {
            if (_segmentsDown.contains(segmentId)) continue;
            
            double reservedBandwidthInErlangs = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            if (reservedBandwidthInErlangs == 0)
            {
                availableSegments.put(segmentId, Double.MAX_VALUE);
            }
            else
            {
                availableSegments.put(segmentId, reservedBandwidthInErlangs);
                
                int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
                for(int linkId : seqLinks)
                    spareCapacity[linkId] = Math.max(0, spareCapacity[linkId] - reservedBandwidthInErlangs);
            }
        }
        
	long[] routeIds = getRouteIds();

	for(long routeId : routeIds)
	{
	    double trafficVolume = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
	    long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
            
            IntArrayList current_seqLinks = new IntArrayList();
            Set<Long> current_segments = new HashSet<Long>();
            
	    for(long itemId : seqLinksAndSegments)
            {
                if (itemId >= 0)
                {
                    int linkId = (int) itemId;
                    current_seqLinks.add(linkId);
                }
                else
                {
                    long segmentId = -1 - itemId;
                    current_segments.add(segmentId);
                }
            }
            
            current_seqLinks.trimToSize();
            
	    int[] unaffectedLinks = IntUtils.setdiff(current_seqLinks.elements(), _linksDown);
	    Set<Long> unaffectedSegments = new HashSet<Long>(current_segments);
            unaffectedSegments.removeAll(_segmentsDown);

	    if (unaffectedLinks.length < IntUtils.unique(current_seqLinks.elements()).length || unaffectedSegments.size() < current_segments.size())
	    {
		affectedRoutes.add(routeId);

		int demandId = getRouteDemand(routeId);
		int a_d = netPlan.getDemandIngressNode(demandId);
		int b_d = netPlan.getDemandEgressNode(demandId);

		if (nodesDown.contains(a_d) || nodesDown.contains(b_d))
		    unrecoverableRoutes.add(routeId);
	    }
            else
            {
		for(int linkId : current_seqLinks.elements())
                    spareCapacity[linkId] = Math.max(0, spareCapacity[linkId] - trafficVolume);

		for(long segmentId : current_segments)
                    availableSegments.put(segmentId, Math.max(0, availableSegments.get(segmentId) - trafficVolume));
            }                
	}

	currentLinkAvailableCapacity.addAll(DoubleUtils.toList(spareCapacity));
    }
    
    /**
     * Returns the first node available from the failure point to the egress node for a given route.
     *
     * @param routeId   Route identifier
     * @param nodesDown Set of nodes which are currently down
     * @param linksDown Set of links which are currently down
     * @return Node identifier (or -1, if no node is available after the failure point)
     * @since 0.2.0
     */
    public int getFirstAvailableNodeDownstream(long routeId, int[] nodesDown, int[] linksDown)
    {
	int demandId = getRouteDemand(routeId);
	int egressNodeId = netPlan.getDemandEgressNode(demandId);

	if (IntUtils.contains(nodesDown, egressNodeId)) return -1;

	int lastAvailableNode = egressNodeId;
	int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
	IntUtils.reverse(seqLinks);

	for(int linkId : seqLinks)
	{
	    int originNodeId = netPlan.getLinkOriginNode(linkId);
	    int destinationNodeId = netPlan.getLinkDestinationNode(linkId);

	    if (IntUtils.contains(linksDown, linkId) || IntUtils.contains(nodesDown, originNodeId))
		break;

	    lastAvailableNode = destinationNodeId;
	}

	return lastAvailableNode;
    }

    /**
     * Returns the first node available before the failure point for a given route.
     *
     * @param routeId   Route identifier
     * @param nodesDown Set of nodes which are currently down
     * @param linksDown Set of links which are currently down
     * @return Node identifier (or -1, if no node is available after the failure point)
     * @since 0.2.0
     */
    public int getFirstAvailableNodeUpstream(long routeId, int[] nodesDown, int[] linksDown)
    {
	int demandId = getRouteDemand(routeId);
	int ingressNodeId = netPlan.getDemandIngressNode(demandId);

	if (IntUtils.contains(nodesDown, ingressNodeId)) return -1;

	int firstAvailableNode = ingressNodeId;
	int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
	for(int linkId : seqLinks)
	{
	    int originNodeId = netPlan.getLinkOriginNode(linkId);
	    int destinationNodeId = netPlan.getLinkDestinationNode(linkId);

	    if (IntUtils.contains(linksDown, linkId) || IntUtils.contains(nodesDown, destinationNodeId))
		break;

	    firstAvailableNode = originNodeId;
	}

	return firstAvailableNode;
    }

    /**
     * Returns the set of protection segments traversing a given link.
     * 
     * @param linkId Link identifier
     * @return Protection segment identifiers
     * @since 0.2.3
     */
    public long[] getLinkCurrentTraversingProtectionSegments(int linkId)
    {
        Set<Long> aux = new TreeSet<Long>();
        
        for(Entry<Long, ActiveProtectionSegment> entry : activeSegments.entrySet())
        {
            long segmentId = entry.getKey();
            ActiveProtectionSegment segment = entry.getValue();
            
            int[] seqLinks = IntUtils.unique(segment.seqLinks);
            if (IntUtils.contains(seqLinks, linkId))
                aux.add(segmentId);
        }
        
        return LongUtils.toArray(aux);
    }

    /**
     * Returns the set of protection segments traversing each link.
     * 
     * @return Protection segment identifiers per link
     * @since 0.2.3
     */
    public long[][] getLinkCurrentTraversingProtectionSegmentsVector()
    {
        long[][] aux = new long[E][];
        
        for(int linkId = 0; linkId < E; linkId++)
            aux[linkId] = getLinkCurrentTraversingProtectionSegments(linkId);
        
        return aux;
    }

    /**
     * Returns the list of links which are down.
     *
     * @return List of links in state down
     * @since 0.2.0
     */
    public int[] getLinksDown()
    {
	Set<Integer> linksDown = new HashSet<Integer>();

	for(int srgId : srgsDown)
	    for(int linkId : E_f[srgId])
		linksDown.add(linkId);

	linksDown.addAll(aux_linksDown);
	linksDown.removeAll(aux_linksUp);

	return IntUtils.toArray(linksDown);
    }

    /**
     * Returns the identifier of the next added protection segment.
     * 
     * @return Protection segment identifier
     * @since 0.2.3
     */
    public long getNextProtectionSegmentId() { return nextSegmentId; }

    /**
     * Returns the identifier of the next added route.
     * 
     * @return Route identifier
     * @since 0.2.3
     */
    public long getNextRouteId() { return nextRouteId; }
    
    /**
     * Returns the set of current state changes in the network.
     *
     * @param newSRGsDown  Set of failure groups which are moving from up to down
     * @param newSRGsUp    Set of failure groups which are moving from down to up
     * @param nodesDown2Up Set of nodes changing from down to up
     * @param nodesUp2Down Set of nodes changing from up to down
     * @param linksDown2Up Set of links changing from down to up
     * @param linksUp2Down Set of links changing from up to down
     * @since 0.2.0
     */
    public void getNodeLinkStateChanges(int[] newSRGsDown, int[] newSRGsUp, Set<Integer> nodesDown2Up, Set<Integer> nodesUp2Down, Set<Integer> linksDown2Up, Set<Integer> linksUp2Down)
    {
	if(newSRGsDown.length > 0 && newSRGsUp.length > 0) throw new Net2PlanException("Failures and reparations cannot happen at the same time");
	if(newSRGsDown.length == 0 && newSRGsUp.length == 0) throw new Net2PlanException("No state changes");

	IntMatrix1D previous_nodeState = IntFactory1D.sparse.make(N);
	IntMatrix1D previous_linkState = IntFactory1D.sparse.make(E);

	for(int srgId : srgsDown)
	{
	    previous_nodeState.viewSelection(N_f[srgId]).assign(IntFunctions.plus(1));
	    previous_linkState.viewSelection(E_f[srgId]).assign(IntFunctions.plus(1));
	}

	if (newSRGsDown.length > 0)
	{
	    for(int srgId : IntUtils.unique(newSRGsDown))
	    {
		for(int nodeId : N_f[srgId])
		    if (previous_nodeState.getQuick(nodeId) == 0)
			nodesUp2Down.add(nodeId);

		for(int linkId : E_f[srgId])
		    if (previous_linkState.getQuick(linkId) == 0)
			linksUp2Down.add(linkId);
	    }
	}
	else
	{
	    for(int srgId : IntUtils.unique(newSRGsUp))
	    {
		for(int nodeId : N_f[srgId])
		    if (previous_nodeState.getQuick(nodeId) == 1)
			nodesDown2Up.add(nodeId);

		for(int linkId : E_f[srgId])
		    if (previous_linkState.getQuick(linkId) == 1)
			linksDown2Up.add(linkId);
	    }
	}
    }

    /**
     * Returns the list of nodes which are down.
     *
     * @return List of nodes in state down
     * @since 0.2.0
     */
    public int[] getNodesDown()
    {
	Set<Integer> nodesDown = new HashSet<Integer>();

	for(int srgId : srgsDown)
	    for(int nodeId : N_f[srgId])
		nodesDown.add(nodeId);

	nodesDown.addAll(aux_nodesDown);
	nodesDown.removeAll(aux_nodesUp);

	return IntUtils.toArray(nodesDown);
    }
    
    private ActiveProtectionSegment getProtectionSegment(long segmentId)
    {
        ActiveProtectionSegment segment = activeSegments.get(segmentId);
        if (segment == null) throw new Net2PlanException("Protection segment " + segmentId + " does not exist");
        
        return segment;
    }
    
    /**
     * Returns the reserved bandwidth for a given protection segment.
     * 
     * @param segmentId Segment identifier
     * @return Reserved bandwidth (in Erlangs)
     * @since 0.2.3
     */
    public double getProtectionSegmentReservedBandwidthInErlangs(long segmentId) { return getProtectionSegment(segmentId).reservedBandwidth; }
    
    /**
     * Returns the sequence of links of a given protection segment.
     * 
     * @param segmentId Segment identifier
     * @return Sequence of links
     * @since 0.2.3
     */
    public int[] getProtectionSegmentSequenceOfLinks(long segmentId) { return IntUtils.copy(getProtectionSegment(segmentId).seqLinks); }

    /**
     * Returns the identifiers of the current protection segments. Identifiers are sorted in ascending order.
     * 
     * @return Identifiers of the current protection segments
     * @since 0.2.3
     */
    public long[] getProtectionSegmentIds() { return LongUtils.toArray(new TreeSet<Long>(activeSegments.keySet()));  }
    
    /**
     * Returns the specific attributes of a given protection segment.
     * 
     * @param segmentId Protection segment identifier
     * @return Protection segment attributes
     * @since 0.2.3
     */
    public Map<String, String> getProtectionSegmentSpecificAttributes(long segmentId) { return getProtectionSegment(segmentId).getAttributes(); }
    
    /**
     * Returns the effects of a reparation event.
     * 
     * @param event Node/link reparation event
     * @param nodesDown Set of nodes which are down
     * @param linksDown Set of links which are down
     * @param reparableRoutes Routes using a backup path whose (planned) primary path is available
     * @param unreparableRoutes Routes using a backup path whose (planned) primary path is not available
     * @param currentLinkAvailableCapacity Current available capacity per link (for links in down state it is equal to zero)
     * @param availableSegments Set of protection segments which are up and their corresponding available capacity
     * @since 0.2.0
     */
    public void getReparationEffects(ResilienceEvent event, Set<Integer> nodesDown, Set<Integer> linksDown, Set<Long> reparableRoutes, Set<Long> unreparableRoutes, List<Double> currentLinkAvailableCapacity, Map<Long, Double> availableSegments)
    {
	nodesDown.addAll(IntUtils.toList(getNodesDown()));
	linksDown.addAll(IntUtils.toList(getLinksDown()));

	IntMatrix1D previous_nodeState = IntFactory1D.sparse.make(N);
	IntMatrix1D previous_linkState = IntFactory1D.sparse.make(E);

	for(int srgId : srgsDown)
	{
	    previous_nodeState.viewSelection(N_f[srgId]).assign(IntFunctions.plus(1));
	    previous_linkState.viewSelection(E_f[srgId]).assign(IntFunctions.plus(1));
	}

	switch(event.getEventType())
	{
	    case SRG_FAILURE:
	    case SRG_REPARATION:
		throw new RuntimeException("SRG events are not allowed here");

	    case NODE_FAILURE:
	    case LINK_FAILURE:
		throw new RuntimeException("Failure events are not allowed here");

	    case NODE_REPARATION:

		int nodeId = event.getId();
		if (previous_nodeState.getQuick(nodeId) == 1) nodesDown.remove(nodeId);

		break;

	    case LINK_REPARATION:

		int linkId = event.getId();
		if (previous_linkState.getQuick(linkId) == 1) linksDown.remove(linkId);

		break;

	    default:
		throw new RuntimeException("Bad - Unknown event type");
	}

	int[] _nodesDown = IntUtils.toArray(nodesDown);
	int[] _linksDown = IntUtils.toArray(linksDown);
        
        double[] spareCapacity = DoubleUtils.copy(u_e);
        
        long[] segmentIds = getProtectionSegmentIds();
	Set<Long> _segmentsDown = getProtectionSegmentsDown(_nodesDown, _linksDown);
        
        for(long segmentId : segmentIds)
        {
            if (_segmentsDown.contains(segmentId)) continue;
            
            double reservedBandwidthInErlangs = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            if (reservedBandwidthInErlangs == 0)
            {
                availableSegments.put(segmentId, Double.MAX_VALUE);
            }
            else
            {
                availableSegments.put(segmentId, reservedBandwidthInErlangs);
                
                int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
                for(int linkId : seqLinks)
                    spareCapacity[linkId] = Math.max(0, spareCapacity[linkId] - reservedBandwidthInErlangs);
            }
        }
        
	double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
	    int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
	    long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
            
            double planned_x_p = getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);
            double current_x_p = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            
	    for(long itemId : seqLinksAndSegments)
            {
                if (itemId >= 0)
                {
                    int linkId = (int) itemId;
                    spareCapacity[linkId] = Math.max(0, spareCapacity[linkId] - current_x_p);
                }
                else
                {
                    long segmentId = -1 - itemId;
                    availableSegments.put(segmentId, Math.max(0, availableSegments.get(segmentId) - current_x_p));
                }
            }

	    if (Arrays.equals(IntUtils.toLongArray(seqLinks), seqLinksAndSegments) && DoubleUtils.isEqualWithinRelativeTolerance(planned_x_p, current_x_p, PRECISIONFACTOR))
		continue;

	    int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);

	    if (!IntUtils.containsAny(_linksDown, seqLinks) && !IntUtils.containsAny(_nodesDown, seqNodes))
		reparableRoutes.add(routeId);
	    else
		unreparableRoutes.add(routeId);
        }

        currentLinkAvailableCapacity.addAll(DoubleUtils.toList(spareCapacity));
    }

    /**
     * Returns a modified route resulting after applying a partial segment to
     * the original route.
     *
     * @param originalRoute Sequence of links of the original route
     * @param partialRoute  Sequence of links of the partial segment
     * @return New sequence of links
     * @since 0.2.3
     */
    public int[] getMergedRoute(int[] originalRoute, int[] partialRoute)
    {
	GraphUtils.checkRouteContinuity(linkTable, originalRoute, GraphUtils.CheckRoutingCycleType.NO_CHECK);
	GraphUtils.checkRouteContinuity(linkTable, partialRoute, GraphUtils.CheckRoutingCycleType.NO_CHECK);

	int[] originalSeqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(convertBackupRoute2SequenceOfLinks(IntUtils.toLongArray(originalRoute)));

	int headNode = netPlan.getLinkOriginNode(partialRoute[0]);
	int tailNode = netPlan.getLinkDestinationNode(partialRoute[partialRoute.length-1]);

	int[] headIndex = IntUtils.find(originalSeqNodes, headNode, Constants.SearchType.FIRST);
	int[] tailIndex = IntUtils.find(originalSeqNodes, tailNode, Constants.SearchType.LAST);

	if (headIndex.length == 0) throw new RuntimeException("Partial route not applicable (first node is not in the original route)");
	if (tailIndex.length == 0) throw new RuntimeException("Partial route not applicable (last node is not in the original route)");
	if (tailIndex[0] < headIndex[0]) throw new RuntimeException("Partial route goes upstream");

	int[] initialSegment = Arrays.copyOfRange(originalRoute, 0, headIndex[0]);
	int[] finalSegment = Arrays.copyOfRange(originalRoute, tailIndex[0], originalRoute.length);

	int[] mergedRoute = IntUtils.concatenate(initialSegment, partialRoute, finalSegment);
	GraphUtils.checkRouteContinuity(linkTable, mergedRoute, GraphUtils.CheckRoutingCycleType.NO_CHECK);

	return mergedRoute;
    }

    private ActiveRoute getRoute(long routeId)
    {
        ActiveRoute route = activeRoutes.get(routeId);
        if (route == null) throw new RuntimeException("Route " + routeId + " does not exist");
        
        return route;
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
        ActiveRoute route = getRoute(routeId);
        String aux_value = route.getAttribute(key);
        if (aux_value == null) aux_value = netPlan.getNetworkAttribute(key);
        
        return aux_value;
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
        ActiveProtectionSegment segment = getProtectionSegment(segmentId);
        String aux_value = segment.getAttribute(key);
        if (aux_value == null) aux_value = netPlan.getNetworkAttribute(key);
        
        return aux_value;
    }
    
    /**
     * Returns the backup protection segments associated to a given route.
     * 
     * @param routeId Route identifier
     * @return Protection segment identifiers
     * @since 0.2.3
     */
    public long[] getRouteBackupSegmentList(long routeId) { return LongUtils.toArray(getRoute(routeId).backupSegments); }

    /**
     *
     * @param routeId Route identifier
     * @return
     * @since 0.2.3
     */
    public double getRouteCurrentCarriedTrafficVolumeInErlangs(long routeId) { return getRoute(routeId).currentTrafficVolume; }

    /**
     * Returns the current sequence of links and segments of a given route.
     * 
     * @param routeId Route identifier
     * @return Current sequence of links and segments. Segment identifiers are represented as <i>-1-segmentId</i>.
     * @since 0.2.3
     */
    public long[] getRouteCurrentSequenceOfLinksAndSegments(long routeId) { return LongUtils.copy(getRoute(routeId).currentSeqLinksAndSegments); }

    /**
     * Returns the demand associated to a given route.
     * 
     * @param routeId Route identifier
     * @return Demand identifier
     * @since 0.2.3
     */
    public int getRouteDemand(long routeId) { return getRoute(routeId).demandId; }

    /**
     * Returns the identifiers of the current routes. Identifiers are sorted in ascending order.
     * 
     * @return Identifiers of the current routes
     * @since 0.2.3
     */
    public long[] getRouteIds() { return LongUtils.toArray(new TreeSet<Long>(activeRoutes.keySet()));  }

    /**
     * Returns the carried traffic in the primary path for a given route.
     * 
     * @param routeId Route identifier
     * @return Traffic volume (in Erlangs)
     * @since 0.2.3
     */
    public double getRoutePrimaryPathCarriedTrafficVolumeInErlangs(long routeId) { return getRoute(routeId).trafficVolume; }

    /**
     * Returns the sequence of links of a given route in its primary path.
     * 
     * @param routeId Route identifier
     * @return Sequence of links
     * @since 0.2.3
     */
    public int[] getRoutePrimaryPathSequenceOfLinks(long routeId) { return IntUtils.copy(getRoute(routeId).seqLinks); }

    /**
     * Returns the attributes for a given route.
     * 
     * @param routeId Route identifier
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String, String> getRouteSpecificAttributes(long routeId) { return getRoute(routeId).getAttributes(); }
    
    /**
     * Returns the routes traversing elements in a given SRG in their primary path.
     * 
     * @param srgId SRG identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getSRGRoutesPrimaryPath(int srgId)
    {
        Set<Long> aux = new TreeSet<Long>();
        int[] linkIds = E_f[srgId];
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
            if (IntUtils.containsAny(seqLinks, linkIds))
                aux.add(routeId);
        }
        
        return LongUtils.toArray(aux);
    }
    
    /**
     * Returns the routes traversing elements in a given SRG in their current path.
     * 
     * @param srgId SRG identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getSRGRoutesCurrentPath(int srgId)
    {
        Set<Long> aux = new TreeSet<Long>();
        int[] linkIds = E_f[srgId];
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
            int[] seqLinks = convertBackupRoute2SequenceOfLinks(seqLinksAndSegments);
            if (IntUtils.containsAny(seqLinks, linkIds))
                aux.add(routeId);
        }
        
        return LongUtils.toArray(aux);
    }
    
    /**
     * Converts a sequence of links and segments to a sequence of links.
     *
     * @param seqLinksAndSegments Sequence of links and segments
     * @return Sequence of links
     * @since 0.2.3
     */
    public int[] convertBackupRoute2SequenceOfLinks(long[] seqLinksAndSegments)
    {
	List<Integer> sequenceOfLinks = new LinkedList<Integer>();

	for (long itemId : seqLinksAndSegments)
	{
	    if (itemId >= 0)
	    {
		int linkId = (int) itemId;
		sequenceOfLinks.add(linkId);
	    }
	    else
	    {
		long segmentId = -1 - itemId;
		sequenceOfLinks.addAll(IntUtils.toList(getProtectionSegmentSequenceOfLinks(segmentId)));
	    }
	}

	return IntUtils.toArray(sequenceOfLinks);
    }

    /**
     * Returns the set of SRGs which are down.
     * 
     * @return SRGs down
     * @since 0.2.0
     */
    public int[] getSRGsDown() { return IntUtils.toArray(srgsDown); }
    
    /**
     * Indicates whether or not there are active protection segments.
     * 
     * @return <code>true</code> if there are active protection segments. Otherwise, <code>false</code>
     * @since 0.2.3
     */
    public boolean hasProtectionSegments() { return !activeSegments.isEmpty(); }

    /**
     * Indicates whether or not there are active routes.
     * 
     * @return <code>true</code> if there are active routes. Otherwise, <code>false</code>
     * @since 0.2.3
     */
    public boolean hasRoutes() { return !activeRoutes.isEmpty(); }
    
    /**
     * Indicates the priority of each of the given routes (default, 0).
     * 
     * @param routeIds Route identifiers
     * @return Priority vector
     * @since 0.2.3
     */
    public int[] getRoutePriorityVector(long[] routeIds)
    {
        IntArrayList routeAttributeList = new IntArrayList();
        for(long routeId : routeIds)
        {
            String aux_priority = getRouteAttribute(routeId, "routePriority");
            int priority = 0;
            try { priority = Integer.parseInt(aux_priority); } catch(Throwable e ) { }
            
            routeAttributeList.add(priority);
        }
        
        routeAttributeList.trimToSize();
        return routeAttributeList.elements();
    }
    
    /**
     * Indicates whether or not a given route allows partial recovery (default, <code>true</code>).
     * 
     * @param routeId Route identifier
     * @return <code>true</code> if route allows partial recovery. Otherwise, <code>false</code>
     * @since 0.2.3
     */
    public boolean isRoutePartialRecoveryAllowed(long routeId)
    {
        boolean isPartialRecoveryAllowed = true;
        String aux_isPartialRecoveryAllowed = getRouteAttribute(routeId, "partialRecoveryAllowed");
        if (aux_isPartialRecoveryAllowed != null)
        {
            try { isPartialRecoveryAllowed = Boolean.parseBoolean(aux_isPartialRecoveryAllowed); }
            catch(Throwable e ) { }
        }
        
        return isPartialRecoveryAllowed;
    }

    /**
     * Indicates whether or not a given route is revertible (default, <code>true</code>).
     * 
     * @param routeId Route identifier
     * @return <code>true</code> if route is revertible. Otherwise, <code>false</code>
     * @since 0.2.3
     */
    public boolean isRouteRevertible(long routeId)
    {
        String aux_isRevertive = getRouteAttribute(routeId, "revertiveMode");
        
        boolean isRevertible = aux_isRevertive == null ? true : Boolean.parseBoolean(aux_isRevertive);
        return isRevertible;
    }
    
    /**
     * Resets the state of the network.
     * 
     * @since 0.2.0
     */
    @Override
    public void reset()
    {
	checkIsModifiable();

        nextRouteId = 0;
        nextSegmentId = 0;
        
        activeRoutes.clear();
        activeSegments.clear();
        
	int R = netPlan.getNumberOfRoutes();
	int S = netPlan.getNumberOfProtectionSegments();
        
        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            int[] seqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidth = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            Map<String, String> attributes = netPlan.getProtectionSegmentSpecificAttributes(segmentId);
            
            long auxSegmentId = addProtectionSegment(seqLinks, reservedBandwidth, attributes);
            if (auxSegmentId != segmentId) throw new RuntimeException("Bad");
        }
        
        for(int routeId = 0; routeId < R; routeId++)
        {
            int demandId = netPlan.getRouteDemand(routeId);
            double trafficVolume = netPlan.getRouteCarriedTrafficInErlangs(routeId);
            int[] seqLinks = netPlan.getRouteSequenceOfLinks(routeId);
            long[] segmentIds = IntUtils.toLongArray(netPlan.getRouteBackupSegmentList(routeId));
            Map<String, String> attributes = netPlan.getRouteSpecificAttributes(routeId);
            
            long auxRouteId = addRoute(demandId, trafficVolume, seqLinks, segmentIds, attributes);
            if (auxRouteId != routeId) throw new RuntimeException("Bad");
        }

        srgsDown.clear();
	aux_linksDown.clear();
	aux_nodesDown.clear();
	aux_linksUp.clear();
	aux_nodesUp.clear();
    }

    /**
     * Update the network state.
     *
     * @param event Current simulation event
     * @param actions List of actions to perform
     * @return A <code>null</code> object
     * @since 0.2.0
     */
    @Override
    public Object update(SimEvent event, List actions)
    {
        checkIsModifiable();
        
        if (!(event instanceof ResilienceEvent)) throw new RuntimeException("Bad");
        ResilienceEvent resilienceEvent = (ResilienceEvent) event;

	switch(resilienceEvent.getEventType())
	{
	    case SRG_FAILURE:
		int srgIdGoingDown = resilienceEvent.getId();
		srgsDown.add(srgIdGoingDown);

		aux_linksDown.clear();
		aux_nodesDown.clear();
		aux_linksUp.clear();
		aux_nodesUp.clear();

		return null;

	    case SRG_REPARATION:
		int srgIdGoingUp = resilienceEvent.getId();
		srgsDown.remove(srgIdGoingUp);

		aux_linksDown.clear();
		aux_nodesDown.clear();
		aux_linksUp.clear();
		aux_nodesUp.clear();

		return null;

	    case NODE_FAILURE:
		aux_nodesDown.add(resilienceEvent.getId());
		break;

	    case NODE_REPARATION:
		aux_nodesUp.add(resilienceEvent.getId());
		break;

	    case LINK_FAILURE:
		aux_linksDown.add(resilienceEvent.getId());
		break;

	    case LINK_REPARATION:
		aux_linksUp.add(resilienceEvent.getId());
		break;

	    default:
		throw new RuntimeException("Bad - Unknown resilience event type");
	}

	int[] nodesDown = getNodesDown();
	int[] linksDown = getLinksDown();
        long[] long_linksDown = IntUtils.toLongArray(linksDown);
	long[] segmentsDown = LongUtils.toArray(getProtectionSegmentsDown(nodesDown, linksDown));
        
        long[] routeIds = getRouteIds();
        
        for(long routeId : routeIds)
        {
	    long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
	    int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(convertBackupRoute2SequenceOfLinks(seqLinksAndSegments));
            
            ActiveRoute route = getRoute(routeId);
            
	    if (IntUtils.containsAny(seqNodes, nodesDown) || LongUtils.containsAny(seqLinksAndSegments, long_linksDown))
	    {
                route.currentSeqLinksAndSegments = IntUtils.toLongArray(IntUtils.copy(route.seqLinks));
                route.currentTrafficVolume = 0;
		continue;
	    }

	    LongArrayList segmentIds = new LongArrayList();

	    for(long linkId : seqLinksAndSegments)
		if (linkId < 0)
		    segmentIds.add(-1 - linkId);

	    segmentIds.trimToSize();

	    if (LongUtils.containsAny(segmentIds.elements(), segmentsDown))
	    {
                route.currentSeqLinksAndSegments = IntUtils.toLongArray(IntUtils.copy(route.seqLinks));
                route.currentTrafficVolume = 0;
	    }
        }

        for(Object action : actions)
	{
            if (!(action instanceof ProvisioningAction)) throw new RuntimeException("Bad");

            ProvisioningAction provisioningAction = (ProvisioningAction) action;
            ProvisioningAction.ActionType actionType = provisioningAction.getActionType();
            
            if (actionType == ProvisioningAction.ActionType.ADD_PROTECTION_SEGMENT)
            {
                int[] seqLinks = provisioningAction.addSegmentSeqLinks;
                double reservedBandwidthInErlangs = provisioningAction.addSegmentReservedBandwidthInErlangs;
                Map<String, String> attributes = provisioningAction.addSegmentAttributes;
                
                addProtectionSegment(seqLinks, reservedBandwidthInErlangs, attributes);
            }
            else if (actionType == ActionType.ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST)
            {
                long routeId = provisioningAction.addSegmentToRouteRouteId;
                long segmentId = provisioningAction.addSegmentToRouteSegmentId;

                boolean accept = checkProtectionSegmentMergeabilityToRoute(routeId, segmentId);
                if (!accept) throw new Net2PlanException("Protection segment " + segmentId + " is not applicable to route " + routeId);
                
                ActiveRoute route = getRoute(routeId);
                route.backupSegments.add(segmentId);
            }
            else if (actionType == ActionType.ADD_ROUTE)
            {
                int demandId = provisioningAction.addRouteDemandId;
                double trafficVolume = provisioningAction.addRouteTrafficVolumeInErlangs;
                int[] seqLinks = provisioningAction.addRouteSeqLinks;
                
                long[] backupSegments = provisioningAction.addRouteBackupSegments;
                Map<String, String> attributes = provisioningAction.addRouteAttributes;
                
                addRoute(demandId, trafficVolume, seqLinks, backupSegments, attributes);
            }
            else if (actionType == ActionType.MODIFY_ROUTE)
            {
                long routeId = provisioningAction.modifyRouteId;
                double trafficVolume = provisioningAction.modifyRouteTrafficVolumeInErlangs;
                long[] seqLinksAndSegments = provisioningAction.modifyRouteSeqLinksAndSegments;
                Map<String, String> attributes = provisioningAction.modifyRouteAttributes;
                
                int demandId = getRouteDemand(routeId);
                if (seqLinksAndSegments != null) checkRouteValidityForDemand(seqLinksAndSegments, demandId);
                if (trafficVolume < 0 && trafficVolume != -1) throw new Net2PlanException("Bad current carried traffic by route " + routeId);
                
                int[] seqLinks = convertBackupRoute2SequenceOfLinks(seqLinksAndSegments);
                int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);
                
                if (IntUtils.containsAny(nodesDown, seqNodes)) throw new Net2PlanException("New route contains some node which is down");
                if (IntUtils.containsAny(linksDown, seqLinks)) throw new Net2PlanException("New route contains some link which is down");
                
                ActiveRoute route = getRoute(routeId);
                if (trafficVolume != -1) route.currentTrafficVolume = trafficVolume;
                if (seqLinksAndSegments != null) route.currentSeqLinksAndSegments = seqLinksAndSegments;
                if (attributes != null) route.setAttributes(attributes);
            }
            else if (actionType == ActionType.REMOVE_ALL_PROTECTION_SEGMENTS)
            {
                for(long routeId : routeIds)
                {
                    long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
                    long[] seqLinks = IntUtils.toLongArray(getRoutePrimaryPathSequenceOfLinks(routeId));
                    if (!Arrays.equals(seqLinksAndSegments, seqLinks)) throw new Net2PlanException("Route " + routeId + " is now using some protection segment(s)");
                }
                
                for(long routeId : routeIds) getRoute(routeId).backupSegments.clear();
            }
            else if (actionType == ActionType.REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST)
            {
                long routeId = provisioningAction.removeAllSegmentsFromRouteId;
                long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
                long[] seqLinks = IntUtils.toLongArray(getRoutePrimaryPathSequenceOfLinks(routeId));
                if (!Arrays.equals(seqLinksAndSegments, seqLinks)) throw new Net2PlanException("Route " + routeId + " is now using some protection segment(s)");
                
                getRoute(routeId).backupSegments.clear();
            }
            else if (actionType == ActionType.REMOVE_ALL_ROUTES)
            {
                activeRoutes.clear();
            }
            else if (actionType == ActionType.REMOVE_PROTECTION_SEGMENT)
            {
                long segmentId = provisioningAction.removeSegmentId;
                
                Set<Long> routes = new HashSet<Long>();
                for(long routeId : routeIds)
                {
                    long[] backupSegmentIds = getRouteBackupSegmentList(routeId);
                    if (!LongUtils.contains(backupSegmentIds, segmentId)) continue;

                    long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
                    long actualSegmentId = -1 - segmentId;
                    if (LongUtils.contains(seqLinksAndSegments, actualSegmentId)) throw new Net2PlanException("Segment " + segmentId + " is being used by route " + routeId);
                    
                    routes.add(routeId);
                }
                
                for(long routeId : routes)
                {
                    ActiveRoute route = getRoute(routeId);
                    route.backupSegments.remove(segmentId);
                }                
            }
            else if (actionType == ActionType.REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST)
            {
                long segmentId = provisioningAction.removeSegmentFromRouteSegmentId;
                long routeId = provisioningAction.removeSegmentFromRouteRouteId;
                
                long[] backupSegmentIds = getRouteBackupSegmentList(routeId);
                if (!LongUtils.contains(backupSegmentIds, segmentId)) throw new Net2PlanException("Segment " + segmentId + " is not associated to route " + routeId);
                
                long[] seqLinksAndSegments = getRouteCurrentSequenceOfLinksAndSegments(routeId);
                long actualSegmentId = -1 - segmentId;
                if (LongUtils.contains(seqLinksAndSegments, actualSegmentId)) throw new Net2PlanException("Segment " + segmentId + " is being used by route " + routeId);
                
                ActiveRoute route = getRoute(routeId);
                route.backupSegments.remove(segmentId);
            }
            else if (actionType == ActionType.REMOVE_ROUTE)
            {
                long routeId = provisioningAction.removeRouteId;
                if (!activeRoutes.containsKey(routeId)) throw new Net2PlanException("Route " + routeId + " cannot be removed since it is not active");
                
                activeRoutes.remove(routeId);
            }
            else
            {
                throw new RuntimeException("Bad - Unknown action type");
            }
	}
        
        return null;
    }
    
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
	ActiveRoute route = getRoute(routeId);
	ActiveProtectionSegment segment = getProtectionSegment(segmentId);
        
	int[] sequenceOfNodes_route = netPlan.convertSequenceOfLinks2SequenceOfNodes(route.seqLinks);
	int[] sequenceOfNodes_segment = netPlan.convertSequenceOfLinks2SequenceOfNodes(segment.seqLinks);
        
        int[] headPositions = IntUtils.find(sequenceOfNodes_route, sequenceOfNodes_segment[0], Constants.SearchType.FIRST);
        if (headPositions.length == 0) return false;

        int[] tailPositions = IntUtils.find(sequenceOfNodes_route, sequenceOfNodes_segment[sequenceOfNodes_segment.length - 1], Constants.SearchType.LAST);
        if (tailPositions.length == 0) return false;
        
        if (tailPositions[0] <= headPositions[0]) return false;
        
        return true;
    }

    /**
     * Returns a deep copy of the current state.
     * 
     * @return Deep copy of the current state
     * @since 0.2.0
     */
    @Override
    public SimState copy()
    {
        ResilienceNetState netState = new ResilienceNetState(netPlan);
        
        for(Entry<Long, ActiveRoute> entry : activeRoutes.entrySet())
        {
            long routeId = entry.getKey();
            ActiveRoute route = entry.getValue().copy();
            
            activeRoutes.put(routeId, route);
        }
        
        for(Entry<Long, ActiveProtectionSegment> entry : activeSegments.entrySet())
        {
            long segmentId = entry.getKey();
            ActiveProtectionSegment segment = entry.getValue().copy();
            
            activeSegments.put(segmentId, segment);
        }

        netState.nextRouteId = nextRouteId;
        netState.nextSegmentId = nextSegmentId;

        isModifiable = true;

        netState.srgsDown.addAll(srgsDown);
        netState.aux_linksDown.addAll(aux_linksDown);
        netState.aux_nodesDown.addAll(aux_nodesDown);
        netState.aux_linksUp.addAll(aux_linksUp);
        netState.aux_nodesUp.addAll(aux_nodesUp);
        
        return netState;
    }

    /**
     * Returns an unmodifiable view of the network.
     *
     * @return An unmodifiable view of the network
     * @since 0.2.0
     */
    @Override
    public SimState unmodifiableView()
    {
        ResilienceNetState netState = new ResilienceNetState();
        netState.netPlan = netPlan;
        netState.N = N;
        netState.E = E;
        netState.D = D;
        netState.N_f = N_f;
        netState.E_f = E_f;
        netState.u_e = u_e;
        netState.h_d = h_d;
        netState.linkTable = linkTable;
        
        netState.activeRoutes = activeRoutes;
        netState.activeSegments = activeSegments;
        netState.nextRouteId = nextRouteId;
        netState.nextSegmentId = nextSegmentId;
        netState.srgsDown = srgsDown;
        netState.aux_linksDown = aux_linksDown;
        netState.aux_nodesDown = aux_nodesDown;
        netState.aux_linksUp = aux_linksUp;
        netState.aux_nodesUp = aux_nodesUp;

        netState.isModifiable = false;
        
        return netState;
    }

    private static class ActiveRoute extends NetworkElement
    {
        private final int demandId;
        private final int[] seqLinks;
        private long[] currentSeqLinksAndSegments;
        private final double trafficVolume;
        private double currentTrafficVolume;
        private final Set<Long> backupSegments;
        
        public ActiveRoute(int demandId, double trafficVolume, int[] seqLinks, long[] backupSegments, Map<String, String> attributes)
        {
            super(attributes);
            
            if (seqLinks.length == 0) throw new Net2PlanException("Bad - Sequence of links cannot be empty");
            
            this.demandId = demandId;
            this.seqLinks = IntUtils.copy(seqLinks);
            this.trafficVolume = trafficVolume;
            this.backupSegments = new LinkedHashSet<Long>(LongUtils.toList(backupSegments));
            this.currentSeqLinksAndSegments = IntUtils.toLongArray(seqLinks);
            this.currentTrafficVolume = trafficVolume;
        }
        
        public ActiveRoute copy()
        {
            int aux_demandId = demandId;
            double aux_trafficVolume = trafficVolume;
            int[] aux_seqLinks = IntUtils.copy(seqLinks);
            long[] aux_backupSegments = LongUtils.toArray(backupSegments);
            Map<String, String> aux_attributes = getAttributes();

            ActiveRoute route = new ActiveRoute(aux_demandId, aux_trafficVolume, aux_seqLinks, aux_backupSegments, aux_attributes);
            route.currentSeqLinksAndSegments = LongUtils.copy(currentSeqLinksAndSegments);
            route.currentTrafficVolume = currentTrafficVolume;
            
            return route;
        }
    }
    
    private static class ActiveProtectionSegment extends NetworkElement
    {
        private final double reservedBandwidth;
        private final int[] seqLinks;

        public ActiveProtectionSegment(double reservedBandwidth, int[] seqLinks, Map<String, String> attributes)
        {
            super(attributes);
            
            this.reservedBandwidth = reservedBandwidth;
            this.seqLinks = IntUtils.copy(seqLinks);
        }

        public ActiveProtectionSegment copy()
        {
            double aux_reservedBandwidth = reservedBandwidth;
            int[] aux_seqLinks = seqLinks;
            Map<String, String> aux_attributes = getAttributes();

            ActiveProtectionSegment segment = new ActiveProtectionSegment(aux_reservedBandwidth, aux_seqLinks, aux_attributes);
            
            return segment;
        }
    }

    /**
     * Returns the list of protection segments which are down.
     *
     * @param nodesDown Set of nodes which are currently down
     * @param linksDown Set of links which are currently down
     * @return List of protection segments in state down
     * @since 0.2.3
     */
    private Set<Long> getProtectionSegmentsDown(int[] nodesDown, int[] linksDown)
    {
	Set<Long> segmentsDown = new HashSet<Long>();
        
        long[] segmentIds = getProtectionSegmentIds();
        for(long segmentId : segmentIds)
        {
	    int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
	    int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);

	    if (IntUtils.containsAny(nodesDown, seqNodes) || IntUtils.containsAny(linksDown, seqLinks))
		segmentsDown.add(segmentId);
        }
        
	return Collections.unmodifiableSet(segmentsDown);
    }

    /**
     * Returns the planned ingress/egress/traversing traffic to each node.
     *
     * @return Planned ingress/egress/traversing per node. Each row represents a node, whereas each column represents ingress, egress and traversing traffic, respectively
     * @since 0.2.3
     */
    public double[][] getNodePrimaryTrafficInErlangsVector()
    {
        long[] routeIds = getRouteIds();
        
	double[][] nodeTraffic = new double[N][3];
        
        for(long routeId : routeIds)
        {
            int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
            int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);
            
            double trafficVolume = getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);
            try
            {
                nodeTraffic[seqNodes[0]][0] += trafficVolume;
            }
            catch(Throwable e)
            {
                System.out.println(N + " - " + seqNodes[0]);
                throw(e);
            }
            nodeTraffic[seqNodes[seqNodes.length - 1]][1] += trafficVolume;

	    for (int seqId = 1; seqId < seqNodes.length - 1; seqId++)
		nodeTraffic[seqNodes[seqId]][2] += trafficVolume;
        }

	return nodeTraffic;
    }
    
    /**
     * Returns the current ingress/egress/traversing traffic to each node.
     *
     * @return Current ingress/egress/traversing per node. Each row represents a node, whereas each column represents ingress, egress and traversing traffic, respectively
     * @since 0.2.3
     */
    public double[][] getNodeCurrentTrafficInErlangsVector()
    {
        long[] routeIds = getRouteIds();
        
	double[][] nodeTraffic = new double[N][3];
        
        for(long routeId : routeIds)
        {
            int[] seqLinks = convertBackupRoute2SequenceOfLinks(getRouteCurrentSequenceOfLinksAndSegments(routeId));
            int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);
            
            double trafficVolume = getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            nodeTraffic[seqNodes[0]][0] += trafficVolume;
            nodeTraffic[seqNodes[seqNodes.length - 1]][1] += trafficVolume;

	    for (int seqId = 1; seqId < seqNodes.length - 1; seqId++)
		nodeTraffic[seqNodes[seqId]][2] += trafficVolume;
        }

	return nodeTraffic;
    }
    
    /**
     * Returns the routes traversing a given link in their current path.
     * 
     * @param linkId Link identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getLinkTraversingCurrentRoutes(int linkId)
    {
        Set<Long> aux = new TreeSet<Long>();
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int[] seqLinks = convertBackupRoute2SequenceOfLinks(getRouteCurrentSequenceOfLinksAndSegments(routeId));
            if (IntUtils.contains(seqLinks, linkId))
                aux.add(routeId);
        }
        
        return LongUtils.toArray(aux);
    }

    /**
     * Returns the routes traversing a given link in their primary path.
     * 
     * @param linkId Link identifier
     * @return Route identifiers
     * @since 0.2.3
     */
    public long[] getLinkTraversingPrimaryRoutes(int linkId)
    {
        Set<Long> aux = new TreeSet<Long>();
        
        long[] routeIds = getRouteIds();
        for(long routeId : routeIds)
        {
            int[] seqLinks = getRoutePrimaryPathSequenceOfLinks(routeId);
            if (IntUtils.contains(seqLinks, linkId))
                aux.add(routeId);
        }
        
        return LongUtils.toArray(aux);
    }
}