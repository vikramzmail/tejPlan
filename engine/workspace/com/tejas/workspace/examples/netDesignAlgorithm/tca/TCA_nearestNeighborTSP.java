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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Triple;

/**
 * Given a set of nodes, this heuristic tries to find a (possibly sub-optimal) minimum cost bidirectional ring (where the cost of a link is given by its
 * length in km) using the nearest-neighbor greedy heuristic. The algorithm starts in a user-defined initial node, and in each iteration sets
 * the next node to visit as the closest one to current node, that has not been visited yet. Link capacities are fixed to a user-defined constant value.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCA_nearestNeighborTSP implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	final double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));
	final int initialNode = Integer.parseInt(algorithmParameters.get("initialNode"));
	final int N = netPlan.getNumberOfNodes();
	if (N == 0) throw new Net2PlanException ("The number of nodes must be positive");
	if ((initialNode >= N) || (initialNode < 0)) throw new Net2PlanException ("The number of nodes must be positive");

	/* Remove all the links in the current design */
	netPlan.removeAllLinks();

	double totalRingLength = 0;
	int currentNode = initialNode;
	HashSet <Integer> notVisitedNode = new HashSet <Integer> (); for (int i = 0 ; i < N ; i ++) if (i != initialNode) notVisitedNode.add (i);
	while (notVisitedNode.size() > 0)
	{
		double bestDistance = Double.MAX_VALUE;
		int bestNeighbor = -1;
		for (int n : notVisitedNode)
			if (netPlan.getNodePairPhysicalDistance(currentNode, n) < bestDistance)
			{
				bestDistance = netPlan.getNodePairPhysicalDistance(currentNode, n);
				bestNeighbor = n;
			}
		/* Add the link to the solution */
		netPlan.addLink(currentNode, bestNeighbor, linkCapacities, bestDistance , null);
		netPlan.addLink(bestNeighbor, currentNode , linkCapacities, bestDistance , null);
		totalRingLength += bestDistance;

		/* Update the current node */
		currentNode = bestNeighbor;
		notVisitedNode.remove(currentNode);
	}

	/* Add the link to the solution */
	final double lastLinkDistance = netPlan.getNodePairPhysicalDistance(initialNode, currentNode);
	netPlan.addLink(currentNode, initialNode, linkCapacities, lastLinkDistance , null);
	netPlan.addLink(initialNode, currentNode , linkCapacities, lastLinkDistance , null);
	totalRingLength += lastLinkDistance;

	return "Ok! Ring total length: " + totalRingLength;
    }

    @Override
    public String getDescription()
    {
	return "Given a set of nodes, this heuristic tries to find a (possibly sub-optimal) minimum cost bidirectional ring (where the cost of a link " +
	    "is given by its length in km) using the nearest-neighbor greedy heuristic. The algorithm starts in a user-defined initial node, and in " +
	    "each iteration sets the next node to visit as the closest one to current node, that has not been visited yet. Link capacities " +
	    "are fixed to a user-defined constant value.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
	algorithmParameters.add(Triple.of("initialNode", "0", "The id of the initial node in the algorithm"));

	return algorithmParameters;
    }
}
