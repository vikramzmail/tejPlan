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
import cern.colt.list.tlong.LongArrayList;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>Algorithm which implements a local restoration mechanism.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class NRSim_AA_MPLS_fastReroute implements IProvisioningAlgorithm
{
    @Override
    public String getDescription()
    {
	return "This algorithm implements a local restoration mechanism. In local "
                + "restoration each route is tried to restore upon a failure at "
                + "the node immediately upstream to that failure. This node which "
                + "redirects the traffic onto the backup path is called the Point "
                + "of Local Repair (PLR), and the node downstream where the backup "
                + "path rejoins to the the primary path is called Merge Point (MP). "
                + "This mechanism provides faster recovery because the decision of "
                + "recovery is strictly local.";
    }

    @Override
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
	List<ProvisioningAction> actions = new LinkedList<ProvisioningAction>();

	if (event.getEventType() == ResilienceEvent.EventType.NODE_FAILURE || event.getEventType() == ResilienceEvent.EventType.LINK_FAILURE)
	{
	    Set<Integer> _nodesDown = new HashSet<Integer>();
	    Set<Integer> _linksDown = new HashSet<Integer>();
	    Set<Long> affectedRoutes = new HashSet<Long>();
	    Set<Long> unrecoverableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> segmentAvailability = new HashMap<Long, Double>();

	    netState.getFailureEffects(event, _nodesDown, _linksDown, affectedRoutes, unrecoverableRoutes, _currentLinkAvailableCapacity, segmentAvailability);

	    int[] nodesDown = IntUtils.toArray(_nodesDown);
	    int[] linksDown = IntUtils.toArray(_linksDown);
	    double[] currentLinkAvailableCapacity = DoubleUtils.toArray(_currentLinkAvailableCapacity);
	    affectedRoutes.removeAll(unrecoverableRoutes);

	    _nodesDown.clear();
	    _linksDown.clear();
	    unrecoverableRoutes.clear();
	    _currentLinkAvailableCapacity.clear();
	    segmentAvailability.clear();

	    int E = netPlan.getNumberOfLinks();
	    double[] costPerLink = DoubleUtils.ones(E);

	    for(int linkId : linksDown)
		costPerLink[linkId] = Double.MAX_VALUE;

	    int[][] linkTable = netPlan.getLinkTable();

	    for(long routeId : affectedRoutes)
	    {
		int[] plannedSeqLinks = netState.getRoutePrimaryPathSequenceOfLinks(routeId);
		double trafficVolume = netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);

                int firstAvailableNodeUpstream = netState.getFirstAvailableNodeUpstream(routeId, nodesDown, linksDown);
		int firstAvailableNodeDownstream = netState.getFirstAvailableNodeDownstream(routeId, nodesDown, linksDown);
		if (firstAvailableNodeUpstream == -1 || firstAvailableNodeDownstream == -1) continue;

		int[] csp = GraphUtils.getCapacitatedShortestPath(linkTable, firstAvailableNodeUpstream, firstAvailableNodeDownstream, costPerLink, currentLinkAvailableCapacity, trafficVolume);
		if (csp.length == 0 && !netState.isRoutePartialRecoveryAllowed(routeId)) continue;

                csp = GraphUtils.getShortestPath(linkTable, firstAvailableNodeUpstream, firstAvailableNodeDownstream, costPerLink);
                if (csp.length == 0) continue;

		int[] path = netState.getMergedRoute(plannedSeqLinks, csp);
		int[] uniqueLinks = IntUtils.unique(path);
		if (path.length == uniqueLinks.length)
		{
		    trafficVolume = Math.min(trafficVolume, DoubleUtils.minValue(DoubleUtils.select(currentLinkAvailableCapacity, path)));
		}
		else
		{
		    double[] aux_linkAvailableCapacity = DoubleUtils.copy(currentLinkAvailableCapacity);
		    for(int linkId : uniqueLinks)
		    {
			int numTimes = IntUtils.find(path, linkId, Constants.SearchType.ALL).length;
			aux_linkAvailableCapacity[linkId] /= numTimes;
		    }

		    trafficVolume = Math.min(trafficVolume, DoubleUtils.minValue(DoubleUtils.select(aux_linkAvailableCapacity, uniqueLinks)));
		}

		if (trafficVolume == 0) continue;

		for(int linkId : path) currentLinkAvailableCapacity[linkId] -= trafficVolume;

		actions.add(ProvisioningAction.modifyRoute(routeId, trafficVolume, IntUtils.toLongArray(path), null));
	    }
	}
	else
	{
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

	    for(long routeId : reparableRoutes)
	    {
                if (!netState.isRouteRevertible(routeId)) continue;
                
		double current_trafficVolume = netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
                long[] current_seqLinksAndSegments = netState.getRouteCurrentSequenceOfLinksAndSegments(routeId);
                
                IntArrayList current_seqLinks = new IntArrayList();
                LongArrayList current_segments = new LongArrayList();
                
                for(long itemId : current_seqLinksAndSegments)
                    if (itemId >= 0)
                        current_seqLinks.add((int) itemId);
                
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
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public List<Triple<String, String, String>> getParameters() { return new LinkedList<Triple<String, String, String>>(); }

    @Override
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) { }

    @Override
    public void finishTransitory(double simTime) { }
}
