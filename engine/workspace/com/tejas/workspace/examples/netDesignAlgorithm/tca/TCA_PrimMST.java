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

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Triple;

import java.util.*;

/** This algorithms computes the bidirectional minimum spanning tree (MST) for the given set of nodes, using the node distance as the cost measure for each link.
 * For a network of N nodes, the returned topology is a tree of N-1 bidirectional links, so that no other topology is able to connect the N nodes
 * with bidirectional links, at a lower cost (being the cost the sum of the lengths of the links). To compute the MST, the Prim algorithm is implemented.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCA_PrimMST implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        int N = netPlan.getNumberOfNodes();

        if (N == 0) throw new Net2PlanException("This algorithm requires a set of nodes");

        int initialNode = Integer.parseInt(algorithmParameters.get("initialNode"));
        if (initialNode < 0 || initialNode >= N) throw new Net2PlanException("Initial node must be in range [0, " + (N-1) + "N]");

        netPlan.removeAllLinks();
        double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));

        Set<Integer> nodesNotInTree = new HashSet<Integer>();
        for(int nodeId = 0; nodeId < N; nodeId++)
            nodesNotInTree.add(nodeId);
        nodesNotInTree.remove(initialNode);

        Set<Integer> nodesInTree = new HashSet<Integer>();
        nodesInTree.add(initialNode);

        int E = 0;
        int[][] linkTable = new int[N-1][2];
        double[] costVector = new double[N-1];

        while(E < N-1)
        {
            double minValue = Double.MAX_VALUE;
            int src = -1;
            int dst = -1;

            for(int originNodeId : nodesInTree)
            {
                for(int destinationNodeId : nodesNotInTree)
                {
                    double costThisPair = netPlan.getNodePairPhysicalDistance(originNodeId, destinationNodeId);

                    if (costThisPair < minValue)
                    {
                        src = originNodeId;
                        dst = destinationNodeId;
                        minValue = costThisPair;
                    }
                }
            }

            nodesNotInTree.remove(dst);
            nodesInTree.add(dst);

            linkTable[E][0] = src;
            linkTable[E][1] = dst;
            costVector[E] = minValue;

            E++;
        }

        for(int linkId = 0; linkId < E; linkId++)
        {
            netPlan.addLink(linkTable[linkId][0], linkTable[linkId][1], linkCapacities, costVector[linkId], net2planParameters);
            netPlan.addLink(linkTable[linkId][1], linkTable[linkId][0], linkCapacities, costVector[linkId], net2planParameters);
        }

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
        return "This algorithms computes the bidirectional minimum spanning tree (MST) for the given set of nodes, using the " +
            "node distance as the cost measure for each link. For a network of N nodes, the returned topology is a tree of N-1 " +
            "bidirectional links, so that no other topology is able to connect the N nodes with bidirectional links, at a lower" +
            " cost (being the cost the sum of the lengths of the links). To compute the MST, the Prim algorithm is implemented.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
        algorithmParameters.add(Triple.of("initialNode", "0", "Initial node of the spanning tree"));

        return algorithmParameters;
    }
}
