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

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.IPUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.Constants.SearchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This algorithm computes the number of IP links between two nodes, and the OSPF 
 * routing (with ECMP), so that all the traffic is carried, while all the links 
 * have an utilization not exceeding a given threshold (<code>maximumUtilization</code>). All 
 * IP links have the same given <code>fixedOSPFWeight</code> weight, and the 
 * same given <code>fixedIPLinkCapacity</code> capacity. Initially, only one IP 
 * link is established between those nodes which have a link between them in 
 * the input design. Then, we compute the worse utilization among all the 
 * links, in no failure condition, or including also any single bidirectional 
 * failure condition, if <code>singleBidirectionalFailureProvision=true</code>. 
 * If the worse utilization in any IP link does not exceed <code>maximumUtilization</code>, 
 * we are done. If not, we add two more links of <code>fixedIPLinkCapacity</code> 
 * (one in each direction), between the nodes of the highest utilized IP link, 
 * and iterate again. The added links belong to the same SRGs and have the same 
 * attributes as the other links between same nodes (they are affected by the 
 * same risk of failure). The final design returned can take two forms. If 
 * <code>condenseAllIPLinksInOne=false</code>, the design returned is as the 
 * one described: all the links are IP links of the same <code>fixedIPLinkCapacity</code>. 
 * If not, we condense all the IP links between two nodes into one single link of 
 * aggregated capacity. Then, the output design will have as many links as the 
 * input design, being the link capacity a multiple of <code>fixedIPLinkCapacity</code>. 
 * In this latter case, SRGs at the input and output design have the same links 
 * associated (they are unchanged).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, February 2014
 */
public class CFA_OSPF_fixedWeight implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan_fibers, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        final int N = netPlan_fibers.getNumberOfNodes();
        final int D = netPlan_fibers.getNumberOfDemands();
        final int E_fibers = netPlan_fibers.getNumberOfLinks();

        /* Basic checks */
        if ((D == 0) || (E_fibers == 0)) throw new Net2PlanException("This algorithm requires a topology with links ande demands");
        if (!GraphUtils.isSimple(netPlan_fibers)) throw new Net2PlanException ("Simple input topologies are expected: at most one link between two nodes");

        final boolean singleBidirectionalFailureProvision = Boolean.parseBoolean(algorithmParameters.get("singleBidirectionalFailureProvision"));
        final double maximumUtilization = Double.parseDouble(algorithmParameters.get("maximumUtilization"));
        final double fixedIPLinkCapacity = Double.parseDouble(algorithmParameters.get("fixedIPLinkCapacity"));
        final double fixedOSPFWeight = Double.parseDouble(algorithmParameters.get("fixedOSPFWeight"));
        final boolean condenseAllIPLinksInOne = Boolean.parseBoolean(algorithmParameters.get("condenseAllIPLinksInOne"));

        /* Take the IP view, one link per WDM channel. Start with one single WDM channel per link */
        NetPlan ipView = netPlan_fibers.copy();
        for (int e = 0 ; e < ipView.getNumberOfLinks() ; e ++) 
            ipView.setLinkCapacityInErlangs(e, fixedIPLinkCapacity); 

        while (true)
        {
            final int E_channels = ipView.getNumberOfLinks();
            final int [][] linkTable_channels = ipView.getLinkTable();
            final double [] ospfWeights_channels = DoubleUtils.mult(DoubleUtils.ones(E_channels), fixedOSPFWeight);

            /* Compute the routing and save in ipView, if no failures  */
            final double [][] f_te_noFailure = IPUtils.computeECMPRoutingTableMatrix(linkTable_channels , ospfWeights_channels , N);
            IPUtils.setRoutesFromRoutingTableMatrix(ipView, f_te_noFailure);
            double [] y_e_worseCase = ipView.getLinkCarriedTrafficInErlangsVector();

            /* If I need to set the capacity enough to carry all the traffic in any single failure bidirectional failure, compute the worse case traffic in each IP link */
            if (singleBidirectionalFailureProvision)
            {
                final double [] y_e_worseCase_singleBidFailure = computeWorseTrafficPerLinkIfSingleBidirectionalFailure (ipView.unmodifiableView() , fixedOSPFWeight);
                for (int e = 0 ; e < E_channels ; e ++) y_e_worseCase [e] = Math.max (y_e_worseCase_singleBidFailure [e] , y_e_worseCase [e]); 
            }

            /* check that all the links between two same nodes carry the same amount of traffic */
            for (int n1 = 0 ; n1 < N ; n1 ++)
            {
                for (int n2 = 0 ; n2 < N ; n2 ++)
                {
                    if (n1 == n2) continue;
                    
                    final int [] e_sameNodePair = ipView.getNodePairLinks(n1, n2); 
                    for (int e : e_sameNodePair) if (y_e_worseCase [e] != y_e_worseCase [e_sameNodePair [0]]) throw new RuntimeException("Bad");
                }
            }

            /* Take the link with highest amount of traffic */
            final int e_channel_maxTraffic = DoubleUtils.maxIndexes(y_e_worseCase, SearchType.FIRST) [0];
            final int a_e = ipView.getLinkOriginNode(e_channel_maxTraffic);
            final int b_e = ipView.getLinkDestinationNode(e_channel_maxTraffic);
            final int [] fibresSameNodePair_sameDirection = netPlan_fibers.getNodePairLinks(a_e, b_e); if (fibresSameNodePair_sameDirection.length != 1) throw new RuntimeException ("Bad");
            final int [] fibresSameNodePair_oppositeDirection = netPlan_fibers.getNodePairLinks(b_e, a_e); if (fibresSameNodePair_oppositeDirection.length != 1) throw new RuntimeException ("Bad");
            final double worseLinkUtilization = y_e_worseCase [e_channel_maxTraffic] / fixedIPLinkCapacity;

            /* If the worse utilization is within the limit => stop */
            if (worseLinkUtilization <= maximumUtilization) break;

            /* If not, add TWO more channels (one in each direction) in the fiber where the channels have the higuest utilization. They have the same attributes as the original fibers */
            final int newIpLink_sameDirection = ipView.addLink(ipView.getLinkOriginNode(e_channel_maxTraffic), ipView.getLinkDestinationNode(e_channel_maxTraffic), fixedIPLinkCapacity, netPlan_fibers.getLinkLengthInKm(fibresSameNodePair_sameDirection [0]) ,  netPlan_fibers.getLinkSpecificAttributes(fibresSameNodePair_sameDirection [0]));
            final int newIpLink_oppositeDirection = ipView.addLink(ipView.getLinkDestinationNode(e_channel_maxTraffic), ipView.getLinkOriginNode(e_channel_maxTraffic), fixedIPLinkCapacity, netPlan_fibers.getLinkLengthInKm(fibresSameNodePair_oppositeDirection [0]) ,  netPlan_fibers.getLinkSpecificAttributes(fibresSameNodePair_oppositeDirection [0]));

            /* Update the SRGs: add the new IP links to the SRGs which were associated with the same original fiber link */
            for (int srg : netPlan_fibers.getLinkSRGs(fibresSameNodePair_sameDirection [0]))
                ipView.addLinkToSRG(newIpLink_sameDirection, srg);
            
            for (int srg : netPlan_fibers.getLinkSRGs(fibresSameNodePair_oppositeDirection [0]))
                ipView.addLinkToSRG(newIpLink_oppositeDirection, srg);
        }

        /* If I have to return the per-fiber view */
        if (!condenseAllIPLinksInOne)
        {
            netPlan_fibers.copyFrom (ipView); 
            IPUtils.setLinkWeightAttributes(netPlan_fibers, fixedOSPFWeight);
            
            return "Ok. IP View returned";
        }

        /* Return "per-fiber" original view. If have to re-create the routes */
        netPlan_fibers.removeAllRoutes();
        netPlan_fibers.removeAllProtectionSegments();
        final int P = ipView.getNumberOfRoutes();
        for (int p = 0 ; p < P ; p ++)
        {
                final int demandId = ipView.getRouteDemand(p);
                final double carriedTraffic = ipView.getRouteCarriedTrafficInErlangs(p);
                final int [] seqChannels = ipView.getRouteSequenceOfLinks(p);
                int [] seqFibers = new int [seqChannels.length];
                for (int cont = 0 ; cont < seqChannels.length ; cont ++)
                        seqFibers [cont] = netPlan_fibers.getNodePairLinks(ipView.getLinkOriginNode(seqChannels [cont]), ipView.getLinkDestinationNode(seqChannels [cont])) [0];
                netPlan_fibers.addRoute(demandId, carriedTraffic , seqFibers , null , null);
        }

        /* Set the link weight attributes */
        IPUtils.setLinkWeightAttributes(netPlan_fibers, fixedOSPFWeight);

        /* Set the capacity according to the number of channels in the associated fiber, multiplied by the capacity module */
        for (int e = 0 ; e < E_fibers ; e ++)
        {
            final double u_e = fixedIPLinkCapacity * ipView.getNodePairLinks(netPlan_fibers.getLinkOriginNode(e), netPlan_fibers.getLinkDestinationNode(e)).length;
            netPlan_fibers.setLinkCapacityInErlangs(e, u_e);
        }

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
        return "This algorithm computes the number of IP links between two nodes, and the OSPF routing (with ECMP), so that all the traffic is carried, " +
            "while all the links have an utilization not exceeding a given threshold (maximumUtilization). All IP links have the same given fixedOSPFWeight weight, " +
            "and the same given fixedIPLinkCapacity capacity. Initially, only one IP link is established between those nodes " +
            "which have a link between them in the input design. Then, we compute the worse utilization among all the links, in no failure condition, or including also" +
            " any single bidirectional failure condition, if singleBidirectionalFailureProvision=true." +
            "If the worse utilization in any IP link does not exceed maximumUtilization, we are done. If not, we add two more links of" +
            " fixedIPLinkCapacity (one in each direction), between the nodes of the highest utilized IP link, and iterate again." +
            " The added links belong to the same SRGs and have the same attributes as the other links between same nodes (they are affected by the same risk of failure)." +
            "The final design returned can take two forms. If condenseAllIPLinksInOne=false, the design returned is as the one described: all the " +
            "links are IP links of the same fixedIPLinkCapacity. If not, we condense all the IP links between two nodes into one single link of aggregated " +
            "capacity. Then, the output design will have as many links as the input design, being the link capacity a multiple of fixedIPLinkCapacity. " +
            "In this latter case, SRGs at the input and output design have the same links associated (they are unchanged).";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();	
        algorithmParameters.add(Triple.of("singleBidirectionalFailureProvision", "true", "If true, the number of links between the nodes is computed so that if all the links between two nodes (both directions) fail simultaneously, all the traffic is carried (same OSPF weights in the surviving links) without exceeding the maximum utilization."));
        algorithmParameters.add(Triple.of("maximumUtilization", "0.8", "Maximum IP link utilization permitted in the network"));
        algorithmParameters.add(Triple.of("fixedIPLinkCapacity", "10", "All the IP links have this capacity"));
        algorithmParameters.add(Triple.of("fixedOSPFWeight", "10", "Fixed OSPF weight to assign to all the IP links"));
        algorithmParameters.add(Triple.of("condenseAllIPLinksInOne", "true", "If true, the design is modified so that each link is a WDM channel (IP view). SRGs are the same, but replacing fibers affected by a failure, by all their associated WDM channels. If not, the links are left as one per fiber (physical layer view), the capacity summing the one in its channels"));
        
        return algorithmParameters;
    }

    private double [] computeWorseTrafficPerLinkIfSingleBidirectionalFailure (NetPlan ipView , double fixedOSPFWeight)
    {
        /* Compute the worse traffic per link */ 
        final int N = ipView.getNumberOfNodes();
        final int [][] demandTable = ipView.getDemandTable();
        final double [] h_d = ipView.getDemandOfferedTrafficInErlangsVector();
        final int E_channels = ipView.getNumberOfLinks();
        final int [][] linkTable_channels = ipView.getLinkTable();
        final double [] ospfWeights_channel = DoubleUtils.mult(DoubleUtils.ones(E_channels), fixedOSPFWeight);
        double [] y_e_worseCase = new double [E_channels];

        /* Loop: all the links between n1 and n2 fail */
        for (int n1 = 0 ; n1 < N ; n1 ++)
        {
            for (int n2 = n1+1 ; n2 < N ; n2 ++)
            {
                final int [] failingLinks = ipView.getNodePairBidirectionalLinks(n1,n2);
                if (failingLinks.length == 0) continue;

                /* compute the link weights, now setting as MAX_VALUE the failing links */
                double [] current_ospfWeights = DoubleUtils.copy(ospfWeights_channel);
                for(int linkId : failingLinks) current_ospfWeights[linkId] = Double.MAX_VALUE;

                /* Compute the new routing */
                final double [][] f_te_withoutFailingLinks = IPUtils.computeECMPRoutingTableMatrix(linkTable_channels, current_ospfWeights , N);
                List<Integer> demands_p = new ArrayList<Integer>();
                List<int[]> seqLinks_p = new ArrayList<int[]>();
                List<Double> x_p = new ArrayList<Double>();
                GraphUtils.convert_fte2xp(linkTable_channels, demandTable, h_d, f_te_withoutFailingLinks , demands_p, seqLinks_p, x_p);

                /* Take the traffic per link in this routing */
                double[] y_e_withoutFailingLinks = GraphUtils.convert_xp2ye(DoubleUtils.toArray(x_p) , seqLinks_p , E_channels);

                for (int e : failingLinks) if (y_e_withoutFailingLinks [e] != 0) throw new RuntimeException("Bad");

                /* Update the worse traffic per link in this routing */
                for (int e = 0 ; e < E_channels ; e ++) y_e_worseCase [e] = Math.max(y_e_worseCase [e] , y_e_withoutFailingLinks [e]);
            }
        }
        
        return y_e_worseCase;
    }
}