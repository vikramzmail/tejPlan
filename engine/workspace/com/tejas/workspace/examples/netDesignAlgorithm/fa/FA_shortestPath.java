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

package com.tejas.workspace.examples.netDesignAlgorithm.fa;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm for flow assignment problems which routes all traffic of each demand, through the shortest path, measured in number of hops or in km,
 * being this a user-defined parameter
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_shortestPath implements IAlgorithm
{

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		String shortestPathType = algorithmParameters.get("shortestPathType");
		if (!shortestPathType.equalsIgnoreCase("hops") && !shortestPathType.equalsIgnoreCase("km")) throw new Net2PlanException("'shortestPathType' must be 'hops' or 'km'");

		/* Check */
		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");

		/* the candidate path list, with one path per each demand, the shortest path */
		CandidatePathList cpl;
		if (shortestPathType.equalsIgnoreCase("hops"))
			cpl = new CandidatePathList(netPlan, "K", "1");
		else
			cpl = new CandidatePathList(netPlan, netPlan.getLinkLengthInKmVector(), "K", "1");

		/* Remove all routes in the current netPlan */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Route each demand in the shortest path  */
		double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		for (int demandId = 0; demandId < D; demandId++)
		{
			int[] pathIds = cpl.getPathsPerDemand(demandId);
			if (pathIds.length == 0) continue;
			int[] sequenceOfLinks = cpl.getSequenceOfLinks(pathIds[0]);
			netPlan.addRoute(demandId, h_d[demandId], sequenceOfLinks, new int[0], new HashMap<String, String>());
		}

		return "Ok!";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> parameters = new ArrayList<Triple<String, String, String>>();
		parameters.add(Triple.of("shortestPathType", "hops", "Each demand is routed according to the shortest path according to this type. Can be 'km' or 'hops'"));
		return parameters;
	}

	@Override
	public String getDescription()
	{
		return "Algorithm for flow assignment problems which routes all traffic of each demand, through the shortest path, measured " +
				"in number of hops or in km, being this a user-defined parameter";
	}
}
