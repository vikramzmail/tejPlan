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

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.IPUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This algorithm reacts to any resilience event (failure/restoration) trying to 
 * routing all the traffic according to the ECMP rule, using link IGP weights. These 
 * weights are not modified by the algorithm.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, February 2014
 */
public class NRSim_AA_OSPF_pathRerouting implements IProvisioningAlgorithm
{
    private int[][] demandTable, linkTable;
    private double[] h_d;
    private int N;
    private double[] ospfWeights;
    
    @Override
    public String getDescription()
    {
        return "This algorithm reacts to any resilience "
            + "event (failure/restoration) trying to  routing all the traffic "
            + "according to the ECMP rule, using link IGP weights. These weights "
            + "are not modified by the algorithm";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
        
        return parameters;
    }

    @Override
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
        List<ProvisioningAction> actions = new LinkedList<ProvisioningAction>();
        
        // Remove all existing routes
        actions.add(ProvisioningAction.removeAllRoutes());
        
        // Get current network state and update an auxiliary OSPF weights vector accordingly
        Set<Integer> _nodesDown = new HashSet<Integer>();
        Set<Integer> _linksDown = new HashSet<Integer>();
        
	if (event.getEventType() == ResilienceEvent.EventType.NODE_FAILURE || event.getEventType() == ResilienceEvent.EventType.LINK_FAILURE)
	{
	    Set<Long> affectedRoutes = new HashSet<Long>();
	    Set<Long> unrecoverableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> segmentAvailability = new HashMap<Long, Double>();

	    netState.getFailureEffects(event, _nodesDown, _linksDown, affectedRoutes, unrecoverableRoutes, _currentLinkAvailableCapacity, segmentAvailability);
        }
        else
        {
	    Set<Long> reparableRoutes = new HashSet<Long>();
	    Set<Long> _unreparableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> segmentAvailability = new HashMap<Long, Double>();

	    netState.getReparationEffects(event, _nodesDown, _linksDown, reparableRoutes, _unreparableRoutes, _currentLinkAvailableCapacity, segmentAvailability);
        }
        
        int[] nodesDown = IntUtils.toArray(_nodesDown);
        int[] linksDown = IntUtils.toArray(_linksDown);
        
        double[] current_ospfWeights = DoubleUtils.copy(ospfWeights);

        for(int linkId : linksDown) current_ospfWeights[linkId] = Double.MAX_VALUE;

        for(int nodeId : nodesDown)
        {
            int[] linkIds = netPlan.getNodeTraversingLinks(nodeId);
            for(int linkId : linkIds) current_ospfWeights[linkId] = Double.MAX_VALUE;
        }

        // Compute new routing according to current state
        double[][] f_te = IPUtils.computeECMPRoutingTableMatrix(linkTable, current_ospfWeights, N);

        List<Integer> demands_p = new ArrayList<Integer>();
        List<int[]> seqLinks_p = new ArrayList<int[]>();
        List<Double> x_p = new ArrayList<Double>();
        GraphUtils.convert_fte2xp(linkTable, demandTable, h_d, f_te, demands_p, seqLinks_p, x_p);

        // Add all routes to the network state
        int P = demands_p.size();
        
        for(int routeId = 0; routeId < P; routeId++)
        {
            int demandId = demands_p.get(routeId);
            double trafficVolumeInErlangs = x_p.get(routeId);
            int[] seqLinks = seqLinks_p.get(routeId);
            actions.add(ProvisioningAction.addRoute(demandId, trafficVolumeInErlangs, seqLinks, null, null));
            
            if (IntUtils.containsAny(seqLinks, linksDown)) throw new Net2PlanException("Bad - Forbidden link");
        }
        
        return actions;
    }

    @Override
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        demandTable = netPlan.getDemandTable();
        h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        linkTable = netPlan.getLinkTable();
        N = netPlan.getNumberOfNodes();
        ospfWeights = IPUtils.getLinkWeightAttributes(netPlan);
    }

    @Override
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public void finishTransitory(double simTime) { }
}
