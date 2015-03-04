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
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm for flow assignment problems which routes all traffic of each demand, through the shortest path, and then creates a backup path , measured in number of hops or in km, being this a user-defined parameter.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_shortestPath_11pathProtection implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();

		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");

		String shortestPathType = algorithmParameters.get("shortestPathType");
		if (!shortestPathType.equalsIgnoreCase("hops") && !shortestPathType.equalsIgnoreCase("km")) throw new Net2PlanException("'shortestPathType' must be 'hops' or 'km'");
		double [] linkCosts = (shortestPathType.equalsIgnoreCase("hops"))? DoubleUtils.ones(E) : netPlan.getLinkLengthInKmVector();
		double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		int [][] linkTable = netPlan.getLinkTable();

		/* First compute all the primary and backup paths */
		/* If for two nodes, we could not find primary & backup paths => return */
		ArrayList <int []> primaryPaths = new ArrayList<int []> ();
		ArrayList <int []> backupPaths = new ArrayList<int []> ();
		for (int d = 0; d < D; d ++)
		{
			int a_d = netPlan.getDemandIngressNode(d);
			int b_d = netPlan.getDemandEgressNode(d);
			int[] primaryPath = GraphUtils.getShortestPath(linkTable, a_d , b_d , linkCosts);
			primaryPaths.add(primaryPath);
			if (primaryPath.length == 0) throw new Net2PlanException ("The network is not connected");
			double [] onesInNonUsedLinks = DoubleUtils.ones(E); for (int e : primaryPath) onesInNonUsedLinks [e] = 0;
			int [] backupPath = GraphUtils.getCapacitatedShortestPath(linkTable , a_d, b_d, linkCosts, onesInNonUsedLinks , 1);
			if (backupPath.length == 0) throw new Net2PlanException ("Could not find a link disjoint backup path");
			backupPaths.add(backupPath);
		}

		/* Remove all the routes and links */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Update the netPlan object with the routes and the protection segments */
		for (int d = 0 ; d < D ; d ++)
		{
			int backupSegment = netPlan.addProtectionSegment(backupPaths.get(d), h_d[d], new HashMap<String, String>());
			netPlan.addRoute(d , h_d[d], primaryPaths.get(d) , new int [] { backupSegment } , new HashMap<String, String>());
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
		return "Algorithm for flow assignment problems which routes all traffic of each demand, through the shortest path, and" +
				"then creates a backup path , measured in number of hops or in km, being this a user-defined parameter.";
	}

}
