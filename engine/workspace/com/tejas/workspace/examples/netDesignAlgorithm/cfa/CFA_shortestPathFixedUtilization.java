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

package com.tejas.workspace.examples.netDesignAlgorithm.cfa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a network topology, and the offered traffic, this algorithm first routes the traffic according to the shortest path (in number of traversed
 * links or in number of traversed km), and then fixes the capacities so that the utilization in all the links is equal to a user-defined given value.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class CFA_shortestPathFixedUtilization implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        final int N = netPlan.getNumberOfNodes();
        final int E = netPlan.getNumberOfLinks();
        final int D = netPlan.getNumberOfDemands();

        /* Basic checks */
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

        /* Take some user-defined parameters */
        final double cg = Double.parseDouble(algorithmParameters.get("cg"));
        final String shortestPathType = algorithmParameters.get("shortestPathType");
        if ((cg <= 0) || (cg > 1)) throw new Net2PlanException("Cg parameter must be positive, and below or equal to 1");
        if (!shortestPathType.equalsIgnoreCase("km") && !shortestPathType.equalsIgnoreCase("hops")) throw new Net2PlanException("Wrong shortestPathType parameter");

        /* The cost of each link for computing the shortest path depends on the user defined parameter "shortestPathType" */
        double linkCostVector[];
        if (shortestPathType.equalsIgnoreCase("hops"))
            linkCostVector = DoubleUtils.ones(E);
        else
            linkCostVector = netPlan.getLinkLengthInKmVector();

        /* Update the netPlan object with the new routes */
        netPlan.removeAllRoutes();
        final int[][] linkTable = netPlan.getLinkTable();
        for (int d = 0; d < D; d++)
        {
            /* compute the shortest path for this demand */
            final int originNodeId = netPlan.getDemandIngressNode(d);
            final int destinationNodeId = netPlan.getDemandEgressNode(d);
            final int[] sequenceOfLinks = GraphUtils.getShortestPath(linkTable, originNodeId, destinationNodeId, linkCostVector);
            if (sequenceOfLinks.length == 0) continue;

            /* Add the route, no protection segments assigned to the route */
            double offeredTraffic = netPlan.getDemandOfferedTrafficInErlangs(d);
            netPlan.addRoute(d, offeredTraffic, sequenceOfLinks, null, null);
        }

        /* For each link, set the capacity as the one which fixes the utilization to the given value */
        final double[] y_e = netPlan.getLinkCarriedTrafficInErlangsVector();
        for (int e = 0; e < E; e++)
            netPlan.setLinkCapacityInErlangs(e, y_e[e] / cg);

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
	return "Routes the traffic according to the shortest path, and then fixes the link capacities to match a given congestion figure";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("cg", "0.6", "Fixed link utilization"));
	algorithmParameters.add(Triple.of("shortestPathType", "hops", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'"));
	return algorithmParameters;
    }
}