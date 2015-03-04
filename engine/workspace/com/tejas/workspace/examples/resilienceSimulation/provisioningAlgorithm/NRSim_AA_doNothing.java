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
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>Algorithm which implements a 'do nothing' resilience mechanism. Under failure,
 * takes no action; while for reparations tries to restore to the primary path, 
 * provided that resources are available.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, December 2013
 */
public class NRSim_AA_doNothing implements IProvisioningAlgorithm
{
    @Override
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
	List<ProvisioningAction> actions = new LinkedList<ProvisioningAction>();

	if (event.getEventType() == ResilienceEvent.EventType.NODE_REPARATION || event.getEventType() == ResilienceEvent.EventType.LINK_REPARATION)
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
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
    }

    @Override
    public String getDescription()
    {
        return "Algorithm which implements a 'do nothing' resilience mechanism. "
            + "Under failure, takes no action; while for reparations tries "
            + "to restore to the primary path.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() { return new LinkedList<Triple<String, String, String>>(); }

    @Override
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public void finishTransitory(double simTime) { }
}