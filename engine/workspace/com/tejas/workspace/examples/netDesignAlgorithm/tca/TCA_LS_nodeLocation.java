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

package com.tejas.workspace.examples.netDesignAlgorithm.tca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Triple;

/**
 * Given a set of access nodes, this algorithm computes the subset of access nodes which have a core node located next to it (in the same place),
 * and the links access-to-core nodes, so that the network cost is minimized. This cost is given by a cost per core node (always 1) plus a cost per link,
 * given by the product of link distance and the user-defined parameter linkCostPerKm. Access-core link capacities are fixed to the user-defined parameter
 * linkCapacities. This problem is solved using a local-search heuristic. Initially, every access node has a core node located next to it. In each iteration,
 * for every core node, I check the cost if we eliminated it and reconnected the access nodes. The best solution among these that also reduces the cost of
 * the current solution becomes the next solution. If no node can be eliminated without worsening the solution, a local optimal has been reached and the
 * algorithm stops.

 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCA_LS_nodeLocation implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		final double linkCostPerKm = Double.parseDouble(algorithmParameters.get("linkCostPerKm"));
		final double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));
		final int initialNode = Integer.parseInt(algorithmParameters.get("initialNode"));
		final int N = netPlan.getNumberOfNodes();
		if (N == 0) throw new Net2PlanException ("The number of nodes must be positive");
		if ((initialNode >= N) || (initialNode < 0)) throw new Net2PlanException ("The number of nodes must be positive");

		/* Compute the cost of one link */
		double [][] c_ij = new double [N][N];
		for (int i = 0 ; i < N ; i ++)
			for (int j = 0 ; j < N ; j ++)
					{ c_ij [i][j] = linkCostPerKm * netPlan.getNodePairPhysicalDistance(i, j); }

		/* The cost of a link ij is given by c_ij. The cost of a core node in location j is 1 */
		int [] current_coreNodeConnectedToAccessNode = new int [N]; Arrays.fill(current_coreNodeConnectedToAccessNode, initialNode);
		HashSet <Integer> current_inactiveCoreNodes = new HashSet <Integer> (); for (int i = 0 ; i < N ; i ++) if (i != initialNode) current_inactiveCoreNodes.add (i);
		double current_cost = 1; for (int i = 0 ; i < N ; i ++) current_cost += c_ij [i][initialNode];

		while (true)
		{
			/* Add one node to the list */
			int bestNeighbor_newCoreNode = -1;
			double bestNeighbor_newCost = Double.MAX_VALUE;

			for (int newCoreNode : current_inactiveCoreNodes)
			{
				final double newCost = newCostAfterAddingCoreNode (newCoreNode , N-current_inactiveCoreNodes.size() , current_coreNodeConnectedToAccessNode , c_ij);
				if (newCost < bestNeighbor_newCost) { bestNeighbor_newCost = newCost; bestNeighbor_newCoreNode = newCoreNode; }
			}

			/* No improvement: end */
			if (bestNeighbor_newCost >= current_cost) break;

			/* Take the best improving solution */
			current_inactiveCoreNodes.remove(bestNeighbor_newCoreNode);
			current_cost = bestNeighbor_newCost;
			for (int i = 0 ; i < N ; i ++)
			{
				final double oldLinkCost = c_ij [i][current_coreNodeConnectedToAccessNode [i]];
				final double newLinkCost =  c_ij [i][bestNeighbor_newCoreNode];
				if (newLinkCost < oldLinkCost) current_coreNodeConnectedToAccessNode [i] = bestNeighbor_newCoreNode;
			}
		}

		/* Remove all previous demands, links, protection segments, routes */
		netPlan.removeAllLinks();
		/* Save the new network links */
		for (int i = 0 ; i < N ; i ++)
		{
			final int j = current_coreNodeConnectedToAccessNode [i];
			if (current_inactiveCoreNodes.contains(j)) throw new Net2PlanException ("Core node " + j + " does not exist!!");
			if (i != j) { netPlan.addLink (i , j , linkCapacities , c_ij [i][j] / linkCostPerKm , null); }
		}

		return "Ok! Number of backbone nodes: " + (N - current_inactiveCoreNodes.size()) + ", cost: " + current_cost;
	}

	@Override
	public String getDescription()
	{
		return "Given a set of access nodes, this algorithm computes the subset of access nodes which have a core node located next to it (in the same place), " +
				" and the links access-to-core nodes, so that the network cost is minimized. This cost is given by a cost per core node (always 1) plus a cost " +
				"per link, given by the product of link distance and the user-defined parameter linkCostPerKm. Access-core link capacities are fixed " +
				"to the user-defined parameter linkCapacities. This problem is solved using a local-search heuristic. Initially, every access node has a core " +
				"node located next to it. In each iteration, for every core node, I check the cost if we eliminated it and reconnected the access nodes. " +
				"The best solution among these that also reduces the cost of the current solution becomes the next solution. If no node can be eliminated " +
				"without worsening the solution, a local optimal has been reached and the algorithm stops.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
		algorithmParameters.add(Triple.of("linkCostPerKm", "1", "The cost per km of the links between access and core nodes"));
		algorithmParameters.add(Triple.of("initialNode", "0", "The id of the initial node in the algorithm"));
		return algorithmParameters;
	}

	private double newCostAfterAddingCoreNode (int newCoreNode , int currentNumberOfCoreNodes , int [] currentConnectivity , double [][] c_ij)
	{
		final double N = currentConnectivity.length;
		double newCost = currentNumberOfCoreNodes + 1; // cost of adding one core node
		for (int i = 0 ; i < N ; i ++)
		{
			final double costIfOldLink = c_ij [i][currentConnectivity [i]];
			final double costIfNewLink =  c_ij [i][newCoreNode];
			newCost += Math.min (costIfNewLink , costIfOldLink);
		}
		return newCost;
	}

}
