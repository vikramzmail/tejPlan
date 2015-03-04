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

package com.tejas.engine.libraries;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;

import java.util.*;

/**
 * Provides a set of static methods which can be useful when dealing with network resilience.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class SRGUtils
{
    /**
     * Type of shared-risk model.
     *
     * @since 0.2.3
     */
    public enum SharedRiskModel
    {
	/**
	 * Defines a SRG per each node. Links are not associated to any SRG.
	 */
	PER_NODE,

	/**
	 * Defines a SRG per each unidirectional link. Nodes are not associated to any SRG.
	 */
	PER_LINK,

	/**
	 * Defines a SRG (one per direction) containing all links between a node pair. Nodes are not associated to any SRG.
	 */
	PER_DIRECTIONAL_LINK_BUNDLE,

	/**
	 * Defines a SRG containing all links between a node pair. Nodes are not associated to any SRG.
	 */
	PER_BIDIRECTIONAL_LINK_BUNDLE,
    };
    
    /**
     * Configures the SRGs into the network design. Existing SRGs will be removed.
     *
     * @param netPlan A network design containing a physical topology
     * @param defaultMTTF Default value for mean time to fail (in hours). Zero or negative value means <code>Double.MAX_VALUE</code>
     * @param defaultMTTR Default value for mean time to repair (in hours). Zero or negative value are not allowed
     * @param sharedRiskModel Model defining SRGs
     * @param removeExistingSRGs Indicates whether or not existing SRGs should be removed
     * @since 0.2.3
     */
    public static void configureSRGs(NetPlan netPlan, double defaultMTTF, double defaultMTTR, SharedRiskModel sharedRiskModel, boolean removeExistingSRGs)
    {
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
        
        if (defaultMTTF <= 0) defaultMTTF = Double.MAX_VALUE;
        if (defaultMTTR <= 0) throw new Net2PlanException("'defaultMTTR' must be greater than zero");
        
        if (removeExistingSRGs) netPlan.removeAllSRGs();
        
	switch(sharedRiskModel)
	{
	    case PER_BIDIRECTIONAL_LINK_BUNDLE:

		for(int originNodeId = 0; originNodeId < N; originNodeId++)
		{
		    for(int destinationNodeId = originNodeId + 1; destinationNodeId < N; destinationNodeId++)
		    {
			int[] nodePairLinks = netPlan.getNodePairBidirectionalLinks(originNodeId, destinationNodeId);
			if (nodePairLinks.length == 0) continue;
                        
                        netPlan.addSRG(null, nodePairLinks, defaultMTTF, defaultMTTR, null);
		    }
		}

		break;

	    case PER_DIRECTIONAL_LINK_BUNDLE:

		for(int originNodeId = 0; originNodeId < N; originNodeId++)
		{
		    for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
		    {
			if (originNodeId == destinationNodeId) continue;

			int[] nodePairLinks = netPlan.getNodePairLinks(originNodeId, destinationNodeId);
			if (nodePairLinks.length == 0) continue;

                        netPlan.addSRG(null, nodePairLinks, defaultMTTF, defaultMTTR, null);
		    }
		}

		break;

	    case PER_LINK:

		for(int linkId = 0; linkId < E; linkId++)
                    netPlan.addSRG(null, new int[] { linkId }, defaultMTTF, defaultMTTR, null);

		break;

	    case PER_NODE:

		for(int nodeId = 0; nodeId < N; nodeId++)
                    netPlan.addSRG(new int[] { nodeId }, null, defaultMTTF, defaultMTTR, null);

		break;

	    default:
		throw new RuntimeException("Bad - Invalid failure model type");
	}
    }

    /**
     * Returns the set of SRGs going down on each failure state.
     * 
     * @param numSRGs Number of defined SRGs
     * @param considerNoFailureState Flag to indicate whether or not no failure state is included
     * @param considerDoubleFailureStates Flag to indicate whether or not double failure states are included
     * @return Set of SRGs going down on each failure state
     * @since 0.2.3
     */
    public static List<Set<Integer>> enumerateFailureStates(int numSRGs, boolean considerNoFailureState, boolean considerDoubleFailureStates)
    {
	if (numSRGs < 0) throw new RuntimeException("Number of SRGs must be greater or equal than zero");
        
        List<Set<Integer>> F_s = new LinkedList<Set<Integer>>();
        
        // No failure
        if (considerNoFailureState) F_s.add(new TreeSet<Integer>());

        // Single failure
        for (int srgId = 0; srgId < numSRGs; srgId++)
        {
            Set<Integer> aux = new TreeSet<Integer>();
            aux.add(srgId);
            F_s.add(aux);
        }

        // Double failures
        if (considerDoubleFailureStates)
        {
            for (int srgId_1 = 0; srgId_1 < numSRGs; srgId_1++)
            {
                for (int srgId_2 = srgId_1 + 1; srgId_2 < numSRGs; srgId_2++)
                {
                    Set<Integer> aux = new TreeSet<Integer>();
                    aux.add(srgId_1);
                    aux.add(srgId_2);
                    F_s.add(aux);
                }
            }
        }

        return F_s;
    }

    /**
     * Computes the probability to find the network on each failure state.
     * 
     * @param F_s Set of SRGs failing per each failure state
     * @param A_f Availability value per SRG
     * @return Probability to find the network in each failure state
     * @since 0.2.3
     */
    public static double[] computeStateProbabilities(List<Set<Integer>> F_s, double[] A_f)
    {
	Set<Integer> noFaultyItems = new HashSet<Integer>();

	int F = A_f.length;
	for(int failureGroupId = 0; failureGroupId < F; failureGroupId++)
	{
	    if (A_f[failureGroupId] <= 0 || A_f[failureGroupId] > 1) throw new RuntimeException("Availability for SRG " + failureGroupId + " must be in range (0, 1] (current " + A_f[failureGroupId] + ")");
	    if (A_f[failureGroupId] == 1) noFaultyItems.add(failureGroupId);
	}

	int S = F_s.size();
	double[] pi_s = new double[S];
        
        ListIterator<Set<Integer>> it = F_s.listIterator();
        while(it.hasNext())
        {
            int stateId = it.nextIndex();
            Set<Integer> aux = it.next();
            
            for(int failureGroupId : noFaultyItems)
                if (aux.contains(failureGroupId))
		    throw new RuntimeException("SRG " + failureGroupId + " is always up, but it is in failure state " + stateId);
            
	    pi_s[stateId] = 1;

	    for (int failureGroupId = 0; failureGroupId < A_f.length; failureGroupId++)
		pi_s[stateId] *= aux.contains(failureGroupId) ? (1 - A_f[failureGroupId]) : A_f[failureGroupId];
        }
        
	double aux = DoubleUtils.sum(pi_s);
	if (aux > 1) throw new RuntimeException("Bad - Summation of state probabilities is greater than one (repeated states?)");

	return pi_s;
    }
}
