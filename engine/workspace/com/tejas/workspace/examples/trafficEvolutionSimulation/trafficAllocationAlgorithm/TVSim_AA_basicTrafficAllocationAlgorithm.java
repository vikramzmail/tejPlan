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

package com.tejas.workspace.examples.trafficEvolutionSimulation.trafficAllocationAlgorithm;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficAllocationAlgorithm;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction;
import com.tejas.engine.utils.Triple;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This algorithm allocates the traffic over the existing routes. Link
 * over-subscription may happen.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.1, February 2014
 */
public class TVSim_AA_basicTrafficAllocationAlgorithm implements ITrafficAllocationAlgorithm
{
    @Override
    public List<TrafficAllocationAction> processEvent(NetPlan netPlan, TimeVaryingNetState netState, double[] h_d, Calendar currentTime)
    {
	List<TrafficAllocationAction> actions = new LinkedList<TrafficAllocationAction>();

        double[] f_p = netPlan.getRouteCarriedTrafficFractionVector();
        int R = f_p.length;
        
        for(int routeId = 0; routeId < R; routeId++)
        {
            int demandId = netPlan.getRouteDemand(routeId);
            actions.add(TrafficAllocationAction.modifyRoute(routeId, h_d[demandId] * f_p[routeId], null));
        }
        
	return actions;
    }

    @Override
    public String getDescription() { return "This algorithm allocates the traffic over the existing routes. Link over-subscription may happen"; }

    @Override
    public List<Triple<String, String, String>> getParameters() { return new LinkedList<Triple<String, String, String>>(); }

    @Override
    public void initialize(NetPlan netPlan, TimeVaryingNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) { }

    @Override
    public String finish(StringBuilder output, Calendar finishTime) { return null; }

    @Override
    public void finishTransitory(Calendar currentTime) { }
}
