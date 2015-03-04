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

package com.tejas.workspace.examples.resilienceSimulation.provisioningAlgorithm;

import cern.colt.list.tint.IntArrayList;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>Algorithm which implements a generic protection mechanism using pre-defined protection segments in the network plan.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class NRSim_AA_genericProtectionSegmentAlgorithm implements IProvisioningAlgorithm
{
    @Override
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
	List<ProvisioningAction> actions = new LinkedList<ProvisioningAction>();

	if (event.getEventType() == ResilienceEvent.EventType.NODE_FAILURE || event.getEventType() == ResilienceEvent.EventType.LINK_FAILURE)
	{
            // Get failure effects
	    Set<Integer> _nodesDown = new HashSet<Integer>();
	    Set<Integer> _linksDown = new HashSet<Integer>();
	    Set<Long> affectedRoutes = new HashSet<Long>();
	    Set<Long> _unrecoverableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> segmentAvailability = new HashMap<Long, Double>();

	    netState.getFailureEffects(event, _nodesDown, _linksDown, affectedRoutes, _unrecoverableRoutes, _currentLinkAvailableCapacity, segmentAvailability);

	    affectedRoutes.removeAll(_unrecoverableRoutes);

	    _nodesDown.clear();
	    _linksDown.clear();
            _unrecoverableRoutes.clear();
	    _currentLinkAvailableCapacity.clear();
            
            // Sort routes by priority
            long[] affectedRouteIds = LongUtils.toArray(affectedRoutes);
            int[] routePriority = netState.getRoutePriorityVector(affectedRouteIds);
            int[] sortedIndexes = IntUtils.sortIndexes(routePriority, Constants.OrderingType.DESCENDING);
            affectedRouteIds = LongUtils.select(affectedRouteIds, sortedIndexes);

            // For each affected route
	    for(long routeId : affectedRouteIds)
	    {
		double trafficVolume = netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);
		long[] planned_seqLinks = IntUtils.toLongArray(netState.getRoutePrimaryPathSequenceOfLinks(routeId));

		long[] segmentIds = netState.getRouteBackupSegmentList(routeId);

                // For each protection segment
		for(long segmentId : segmentIds)
		{
                    if (!segmentAvailability.containsKey(segmentId)) continue;
                    
                    // Try to route traffic on this protection segment
                    double segmentAvailableCapacity = segmentAvailability.get(segmentId);
                    if (segmentAvailableCapacity <= 0) continue;
                    
		    if (segmentAvailableCapacity < trafficVolume && !netState.isRoutePartialRecoveryAllowed(routeId)) continue;

		    long[] protectionSequenceOfLinksAndSegments = netState.getMergedBackupRoute(planned_seqLinks, segmentId);
		    double recoveredTraffic = Math.min(segmentAvailableCapacity, trafficVolume);
                    segmentAvailability.put(segmentId, segmentAvailableCapacity - recoveredTraffic);

		    actions.add(ProvisioningAction.modifyRoute(routeId, recoveredTraffic, protectionSequenceOfLinksAndSegments, null));
		    break;
		}
	    }
	}
	else
	{
            // Get reparation effects
	    Set<Integer> _nodesDown = new HashSet<Integer>();
	    Set<Integer> _linksDown = new HashSet<Integer>();
	    Set<Long> reparableRoutes = new HashSet<Long>();
	    Set<Long> _unreparableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> segmentAvailability = new HashMap<Long, Double>();

	    netState.getReparationEffects(event, _nodesDown, _linksDown, reparableRoutes, _unreparableRoutes, _currentLinkAvailableCapacity, segmentAvailability);

	    double[] currentLinkAvailableCapacity = DoubleUtils.toArray(_currentLinkAvailableCapacity);

	    _nodesDown.clear();
	    _linksDown.clear();
	    _unreparableRoutes.clear();
	    _currentLinkAvailableCapacity.clear();

            // Sort routes by priority
            long[] reparableRoutesIds = LongUtils.toArray(reparableRoutes);
            int[] routePriority = netState.getRoutePriorityVector(reparableRoutesIds);
            int[] sortedIndexes = IntUtils.sortIndexes(routePriority, Constants.OrderingType.DESCENDING);
            reparableRoutesIds = LongUtils.select(reparableRoutesIds, sortedIndexes);

            // For each route
	    for(long routeId : reparableRoutesIds)
	    {
                // If not revertible, go to next route
                if (!netState.isRouteRevertible(routeId)) continue;
                
                // Check if there is now enough capacity in the links to recover the route to the primary path
                // To do so, we remove traffic from current route and test for primary path: if there is enough 
                // capacity in the primary path, we confirm the restore action; otherwise, we undo temporal changes
		double current_trafficVolume = netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
                long[] current_seqLinksAndSegments = netState.getRouteCurrentSequenceOfLinksAndSegments(routeId);

                IntArrayList current_seqLinks = new IntArrayList();
                for(long itemId : current_seqLinksAndSegments)
                    if (itemId >= 0) { int linkId = (int) itemId; current_seqLinks.add(linkId); }
                current_seqLinks.trimToSize();
                
                for(int linkId : current_seqLinks.elements())
                    currentLinkAvailableCapacity[linkId] += current_trafficVolume;
                
                double trafficVolume = netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);
		int[] seqLinks = netState.getRoutePrimaryPathSequenceOfLinks(routeId);

                for(int linkId : seqLinks)
                    currentLinkAvailableCapacity[linkId] -= trafficVolume;
                
                boolean thereIsEnoughCapacity = true;
                
                for(int linkId : seqLinks)
                {
                    if ( currentLinkAvailableCapacity[linkId] < 0)
                    {
                        thereIsEnoughCapacity = false;
                        break;
                    }
                }
                
                if (thereIsEnoughCapacity)
                {
                    actions.add(ProvisioningAction.modifyRoute(routeId, trafficVolume, IntUtils.toLongArray(seqLinks), null));
                }
                else
                {
                    for(int linkId : seqLinks)
                        currentLinkAvailableCapacity[linkId] += trafficVolume;

                    for(int linkId : current_seqLinks.elements())
                        currentLinkAvailableCapacity[linkId] -= current_trafficVolume;
                }
	    }
	}

	return actions;
    }

    @Override
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
    }

    @Override
    public String getDescription()
    {
	StringBuilder description = new StringBuilder();

	String NEWLINE = String.format("%n");
	String TAB = String.format("\t");

	description.append("Algorithm to perform per-route using pre-defined protection segments.").append(NEWLINE).append(NEWLINE);
	description.append("Steps").append(NEWLINE);
	description.append("1. Sort routes by descending priority order (use route-parameter priority, assume 1 by default)").append(NEWLINE);
	description.append("2. For each route").append(NEWLINE);
	description.append(TAB).append("2.1 Is route affected?").append(NEWLINE);
	description.append(TAB).append(TAB).append("No: Go to next route").append(NEWLINE);
	description.append(TAB).append(TAB).append("Yes: Go to 2.2").append(NEWLINE);
	description.append(TAB).append("2.2 What event affected to route?").append(NEWLINE);
	description.append(TAB).append(TAB).append("Restoration event: Go to 2.3").append(NEWLINE);
	description.append(TAB).append(TAB).append("Failure event: Go to 2.4").append(NEWLINE);
	description.append(TAB).append("2.3 Is route in revertive mode?").append(NEWLINE);
	description.append(TAB).append(TAB).append("No: Go to next route").append(NEWLINE);
	description.append(TAB).append(TAB).append("Yes: There is enough capacity to restore the whole planned traffic?").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append("No: Go to next route").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append("Yes: Recover").append(NEWLINE);
	description.append(TAB).append("2.4 For each pre-defined protection segment").append(NEWLINE);
	description.append(TAB).append(TAB).append("Has node, link and bandwidth availability?").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append("No: Go to next segment").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append("Yes: Recover the whole route?").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append("No: Go to next segment").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append("Yes: Partial recover mode allowed?").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append("Yes: Recover as much traffic as possible and go to next route").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append("No: Would recover all?").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append("Yes: Use this segment and go to next route").append(NEWLINE);
	description.append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append(TAB).append("No: Go to next segment").append(NEWLINE);

	return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters() { return new ArrayList<Triple<String, String, String>>(); }

    @Override
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public void finishTransitory(double simTime) { }
}