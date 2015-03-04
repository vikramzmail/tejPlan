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

import com.tejas.engine.interfaces.networkDesign.NetPlan;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class to deal with multilayer networks. In multilayer networks, links in the
 * layer <i>i</i> are equivalent to one demand and one route on the layer <i>i+1</i>
 * (the below one)
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public class MultiLayerUtils
{
    /**
     * <p>Check for consistency between a list of hierarchical layers. Here,
     * it assumes that links in a given layer are demands of the layer below.</p>
     *
     * @param netPlans List of network plans (an ascending order will be assumed, that is, the first layer will be the upper one)
     * @since 0.2.1
     */
    public static void checkLayerConsistency(List<NetPlan> netPlans)
    {
	if (netPlans.size() < 2) return;

	Iterator<NetPlan> it = netPlans.iterator();
	NetPlan upperLayer = it.next();

	int upperLayerId = 0;
	while(it.hasNext())
	{
	    NetPlan lowerLayer = it.next();
	    int lowerLayerId = upperLayerId + 1;

	    // Check nodes
	    int upperLayer_N = upperLayer.getNumberOfNodes();
	    int lowerLayer_N = lowerLayer.getNumberOfNodes();

	    if (upperLayer_N != lowerLayer_N)
		throw new RuntimeException(String.format("%d nodes at layer %d are mapped to %d nodes at layer %d", upperLayer_N, upperLayerId, lowerLayer_N, lowerLayerId));

	    for(int upperLayer_nodeId = 0; upperLayer_nodeId < upperLayer_N; upperLayer_nodeId++)
	    {
		double[] upperLayer_nodeXYPosition = upperLayer.getNodeXYPosition(upperLayer_nodeId);

		int lowerLayer_nodeId = upperLayer_nodeId;
		double[] lowerLayer_nodeXYPosition = lowerLayer.getNodeXYPosition(lowerLayer_nodeId);

		if (!Arrays.equals(upperLayer_nodeXYPosition, lowerLayer_nodeXYPosition))
		    throw new RuntimeException(String.format("Position for node %d at layer %d (%f, %f) does not match position for node %d at layer %d (%f, %f)", upperLayer_nodeId, upperLayerId, upperLayer_nodeXYPosition[0], upperLayer_nodeXYPosition[1], lowerLayer_nodeId, lowerLayerId, lowerLayer_nodeXYPosition[0], lowerLayer_nodeXYPosition[1]));
	    }

	    // Check links (upper layer) are equal to demands (lower layer)
	    int upperLayer_E = upperLayer.getNumberOfLinks();
	    int lowerLayer_D = lowerLayer.getNumberOfDemands();

	    if (upperLayer_E != lowerLayer_D)
		throw new RuntimeException(String.format("%d links at layer %d are mapped to %d demands at layer %d", upperLayer_E, upperLayerId, lowerLayer_D, lowerLayerId));

	    for(int upperLayer_linkId = 0; upperLayer_linkId < upperLayer_E; upperLayer_linkId++)
	    {
		int upperLayer_originNodeId = upperLayer.getLinkOriginNode(upperLayer_linkId);
		int upperLayer_destinationNodeId = upperLayer.getLinkDestinationNode(upperLayer_linkId);

		int lowerLayer_demandId = upperLayer_linkId;
		int lowerLayer_ingressNodeId = lowerLayer.getDemandIngressNode(lowerLayer_demandId);
		int lowerLayer_egressNodeId = lowerLayer.getDemandEgressNode(lowerLayer_demandId);

		if (upperLayer_originNodeId != lowerLayer_ingressNodeId)
		    throw new RuntimeException(String.format("Origin node %d for link %d at layer %d is mapped to ingress node %d for demand %d at layer %d", upperLayer_originNodeId, upperLayer_linkId, upperLayerId, lowerLayer_ingressNodeId, lowerLayer_demandId, lowerLayerId));

		if (upperLayer_destinationNodeId != lowerLayer_egressNodeId)
		    throw new RuntimeException(String.format("Destination node %d for link %d at layer %d is mapped to egress node %d for demand %d at layer %d", upperLayer_originNodeId, upperLayer_linkId, upperLayerId, lowerLayer_ingressNodeId, lowerLayer_demandId, lowerLayerId));

		double upperLayer_linkCapacityInErlangs = upperLayer.getLinkCapacityInErlangs(upperLayer_linkId);
		double lowerLayer_demandOfferedTrafficInErlangs = lowerLayer.getDemandOfferedTrafficInErlangs(lowerLayer_demandId);

		if (upperLayer_linkCapacityInErlangs != lowerLayer_demandOfferedTrafficInErlangs)
		    throw new RuntimeException(String.format("Link capacity for link %d at layer %d (%f Erlangs) does not match offered traffic for demand %d at layer %d (%f Erlangs)", upperLayer_linkId, upperLayerId, upperLayer_linkCapacityInErlangs, lowerLayer_demandId, lowerLayerId, lowerLayer_demandOfferedTrafficInErlangs));
	    }

	    upperLayer = lowerLayer;
	    upperLayerId = lowerLayerId;
	}
    }
}
