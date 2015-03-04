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

package com.tejas.workspace.examples.cacSimulation.cacAlgorithm;

import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.cacSimulation.ICACAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Basic CAC algorithm which tries to route each connection request in the shortest path.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class CACSim_AA_basicAlgorithm implements ICACAlgorithm
{
    private double[] costVector;
    private int[][] linkTable;
    
    @Override
    public void initialize(NetPlan netPlan, ConnectionNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        final String shortestPathType = algorithmParameters.get("shortestPathType");
        
        if (shortestPathType.equalsIgnoreCase("km"))
        {
            costVector = netPlan.getLinkLengthInKmVector();
        }
        else if (shortestPathType.equalsIgnoreCase("hops"))
        {
            int E = netPlan.getNumberOfLinks();
            costVector = DoubleUtils.ones(E);
        }
        else
        {
            throw new Net2PlanException("Wrong 'shortestPathType' parameter");
        }
        
        linkTable = netPlan.getLinkTable();
    }

    @Override
    public List<CACAction> processEvent(NetPlan netPlan, ConnectionNetState connectionNetState, CACEvent event)
    {
	final List<CACAction> actions = new LinkedList<CACAction>();
        
        switch(event.getEventType())
        {
            case CONNECTION_REQUEST:
                final int demandId = event.getRequestDemandId();
                final double trafficVolume = event.getRequestTrafficVolumeInErlangs();

                final int ingressNodeId = netPlan.getDemandIngressNode(demandId);
                final int egressNodeId = netPlan.getDemandEgressNode(demandId);

                final int[] spLinks = GraphUtils.getShortestPath(linkTable, ingressNodeId, egressNodeId, costVector);
                if (spLinks.length == 0) actions.add(CACAction.blockRequest("No path from ingress node to egress node"));
                else actions.add(CACAction.acceptRequest(spLinks, trafficVolume, null));
                
                break;
                
            case CONNECTION_RELEASE:
                break;
        }
        
        return actions;
    }

    @Override
    public String getDescription() { return "Basic CAC algorithm which tries to "
            + "route each connection request in the shortest path"; }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	final List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("shortestPathType", "hops", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'"));
        
	return algorithmParameters;
    }

    @Override
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public void finishTransitory(double simTime) { }
}
