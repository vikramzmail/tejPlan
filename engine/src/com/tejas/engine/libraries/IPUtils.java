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

import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.StringUtils;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import java.util.Map.Entry;
import java.util.*;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

/**
 * Class for destination-based routing (IP-like).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class IPUtils
{
    /**
     * Main method to test methods from this class. The example 7.1 as appeared in [<a href='#Pioro2004'>1</a>] is used throughout the test.
     *
     * @param args Unused
     * @see <a name='Pioro2004' />[1] M. Pioro, D. Medhi, <i>Routing, Flow, and Capacity Design in Communication and Computer Networks</i>, Morgan-Kaufmann, 2004
     * @since 0.2.0
     */
    public static void main(String args[])
    {
	int N = 7;

	NetPlan netPlan = new NetPlan();
	for(int nodeId = 0; nodeId < N; nodeId++) netPlan.addNode(0, 0, null, null);

	netPlan.addLink(5, 0, 0, 0, null);
	netPlan.addLink(5, 1, 0, 0, null);
	netPlan.addLink(0, 2, 0, 0, null);
	netPlan.addLink(0, 3, 0, 0, null);
	netPlan.addLink(1, 4, 0, 0, null);
	netPlan.addLink(2, 6, 0, 0, null);
	netPlan.addLink(3, 6, 0, 0, null);
	netPlan.addLink(4, 6, 0, 0, null);

	int[][] linkTable = netPlan.getLinkTable();
	int E = linkTable.length;

	netPlan.addDemand(5, 6, 1, null);

	double[] linkWeights = new double[E];
	Arrays.fill(linkWeights, 1);

	double[][] f_te_ECMP = computeECMPRoutingTableMatrix(linkTable, linkWeights, N);
	double[][] f_te_OMP = computeOMPRoutingTableMatrix(linkTable, linkWeights, N);

	System.out.println("ECMP");
	setRoutesFromRoutingTableMatrix(netPlan, f_te_ECMP);
	double[][] ff_te_ECMP = getRoutingTableMatrix(netPlan);
	if (!Arrays.deepEquals(f_te_ECMP, ff_te_ECMP)) throw new RuntimeException("Bad");
	System.out.println(routingTableMatrixToString(linkTable, ff_te_ECMP));

	System.out.println(String.format("%n%n") + "OMP");
	setRoutesFromRoutingTableMatrix(netPlan, f_te_OMP);
	double[][] ff_te_OMP = getRoutingTableMatrix(netPlan);
	if (!Arrays.deepEquals(f_te_OMP, ff_te_OMP)) throw new RuntimeException("Bad");
	System.out.println(routingTableMatrixToString(linkTable, ff_te_OMP));
    }

    /**
     * <p>Computes the routing tables for ECMP (Equal-Cost Multi-Path) using an OSPF-like (or IS-IS) mechanism, given a set of link weights (links with weight equal to <code>Double.MAX_VALUE</code> are forbidden).</p>
     *
     * <p>The point with ECMP-based routing is that traffic is equally split among the candidate output links, not between paths. For example, if three shortest paths go from a node <i>n</i> to the egress node <i>t</i>, and there are only two candidate output links, each link will forward 50% of traffic to node <i>t</i>, instead of a 33.33%/66.66% ratio. To achieve a behavior like the latter, use the {@link #computeOMPRoutingTableMatrix computeOMPRoutingTableMatrix()} method.</p>
     *
     * <p><b>Important</b>: Although non-integer values are allowed, usage of positive integer values is encouraged to follow the OSPF standard</p>
     *
     * @param linkTable   Set of links defining a physical topology
     * @param linkWeights Set of link weights (must be greater or equal than one)
     * @param N           Number of nodes in the network
     * @return A destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
     * @since 0.2.0
     */
    public static double[][] computeECMPRoutingTableMatrix(int[][] linkTable, double[] linkWeights, int N)
    {
	if (DoubleUtils.minValue(linkWeights) < 1) throw new Net2PlanException("Link weights must be greater or equal than 1");

	int E = linkTable.length;
	double[][] f_te = new double[N][E];

	Graph<Integer, Integer> g = GraphUtils.JUNGUtils.getGraphFromLinkTable(linkTable, N);
	final Transformer<Integer, Double> nev = GraphUtils.JUNGUtils.getEdgeWeightTransformer(linkWeights);
	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer linkId)
		{
		    return (nev.transform(linkId) == Double.MAX_VALUE) ? false : true;
		}
	    });
	g = linkFilter.transform(g);

	DijkstraShortestPath<Integer, Integer> dsp = new DijkstraShortestPath<Integer, Integer>(g, nev);

	for(int b_d = 0; b_d < N; b_d++)
	{
	    for(int a_d = 0; a_d < N; a_d++)
	    {
		if (a_d == b_d) continue;

		Number dist = dsp.getDistance(a_d, b_d);
		if (dist == null) continue;

		double distIngressToEgress = dist.doubleValue();

		Set<Integer> A_t = new HashSet<Integer>();

		for(int c_d = 0; c_d < N; c_d++)
		{
		    if (a_d == c_d) continue;

		    if (dsp.getDistance(a_d, c_d) == null) continue;

		    Number dist2 = dsp.getDistance(c_d, b_d);
		    if (dist2 == null) continue;

		    double distIntermediateToEgress = dist2.doubleValue();

		    Collection<Integer> linksFromIngressToIntermediate = g.findEdgeSet(a_d, c_d);
		    for(int linkId : linksFromIngressToIntermediate)
			if (linkWeights[linkId] == distIngressToEgress - distIntermediateToEgress)
			    A_t.add(linkId);
		}

		int outdegree = A_t.size();

		if (outdegree > 0)
		    for(int linkId : A_t)
			f_te[b_d][linkId] = 1.0 / outdegree;
	    }
	}

	return f_te;
    }

    /**
     * <p>Computes the optimal routing tables using an OSPF-OMP-like mechanism, given a set of link weights.</p>
     *
     * <p>Contrary to ECMP, this routing scheme equally splits traffic among multiple shortest paths. For example, if three shortest paths go from a node <i>n</i> to the egress node <i>t</i>, and there are only two candidate output links, traffic will be divided following a 33.33%/66.66% ratio.</p>
     *
     * @param linkTable   Set of links defining a physical topology
     * @param linkWeights Set of link weights (must be greater or equal than one)
     * @param N           Number of nodes in the network
     * @return A destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
     * @since 0.2.0
     */
    public static double[][] computeOMPRoutingTableMatrix(int[][] linkTable, double[] linkWeights, int N)
    {
	if (DoubleUtils.minValue(linkWeights) < 1) throw new Net2PlanException("Link weights must be greater or equal than 1");

	int E = linkTable.length;
	double[][] f_te = new double[N][E];

	for(int originNodeId = 0; originNodeId < N; originNodeId++)
	{
	    for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
	    {
		if (originNodeId == destinationNodeId) continue;

		List<int[]> allSPs = GraphUtils.getAllLooplessShortestPaths(linkTable, originNodeId, destinationNodeId, N, linkWeights);
		if(allSPs.isEmpty()) continue;

		Map<Integer, Integer> aux = new HashMap<Integer, Integer>();

		Iterator<int[]> it = allSPs.iterator();
		while(it.hasNext())
		{
		    int linkId = it.next()[0];

		    if (aux.containsKey(linkId)) aux.put(linkId, aux.get(linkId) + 1);
		    else aux.put(linkId, 1);
		}

		for(Entry<Integer, Integer> entry : aux.entrySet())
		    f_te[destinationNodeId][entry.getKey()] = (double) entry.getValue() / allSPs.size();
	    }
	}

	return f_te;
    }

    /**
     * Obtains the set of link weights (link attribute 'linkWeight', default: 1) from a given a network design.
     *
     * @param netPlan Network design
     * @return Set of link weights (must be greater than zero, default: 1)
     * @since 0.2.0
     */
    public static double[] getLinkWeightAttributes(NetPlan netPlan)
    {
	double[] linkWeights = StringUtils.toDoubleArray(netPlan.getLinkAttributeVector("linkWeight"), 1);
	if (DoubleUtils.minValue(linkWeights) <= 0) throw new Net2PlanException("Link weights must be greater than zero");

	return linkWeights;
    }

    /**
     * Obtains a destination-based routing from a given network design.
     *
     * @param netPlan Network design
     * @return A destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
     * @since 0.2.0
     */
    public static double[][] getRoutingTableMatrix(NetPlan netPlan)
    {
	int[][] linkTable = netPlan.getLinkTable();
	int[][] demandTable = netPlan.getDemandTable();
	int[] d_p = netPlan.getRouteDemandVector();
	double[] x_p = netPlan.getRouteCarriedTrafficInErlangsVector();
	List<int[]> seqLinks = netPlan.getRouteAllSequenceOfLinks();
	int N = netPlan.getNumberOfNodes();

	return GraphUtils.convert_xp2fte(linkTable, demandTable, d_p, x_p, seqLinks, N);
    }

    /**
     * Outputs a given set of routing tables to a <code>String</code>. For debugging purposes.
     *
     * @param linkTable   Set of links defining a physical topology
     * @param f_te        Destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
     * @return A <code>String</code> from the given routing tables
     * @since 0.2.0
     */
    public static String routingTableMatrixToString(int[][] linkTable, double[][] f_te)
    {
	int N = f_te.length;

	StringBuilder routingTable = new StringBuilder();
	double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

	for(int nodeId = 0; nodeId < N; nodeId++)
	{
	    routingTable.append(String.format("Routing table for n%d%n", nodeId));

	    int[] outgoingLinks = GraphUtils.getOutgoingLinks(linkTable, nodeId);

	    boolean thereAreRoutes = false;
            
            if (outgoingLinks.length > 0)
            {
                for(int destinationId = 0; destinationId < N; destinationId++)
                {
                    double[] f_t = DoubleUtils.select(f_te[destinationId], outgoingLinks);
                    if (DoubleUtils.maxValue(f_t) < PRECISIONFACTOR) continue;

                    thereAreRoutes = true;

                    routingTable.append(String.format("dst: n%d, gw:", destinationId));
                    for(int i = 0; i < outgoingLinks.length; i++)
                    {
                        if (f_t[i] < PRECISIONFACTOR) continue;
                        routingTable.append(String.format(" [e%d (n%d): %f]", outgoingLinks[i], linkTable[outgoingLinks[i]][1], f_t[i]));
                    }

                    routingTable.append(String.format("%n"));
                }
            }

	    if(!thereAreRoutes) routingTable.append("Empty");

	    if (nodeId != N-1) routingTable.append(String.format("%n"));
	}

	return routingTable.toString();
    }

    /**
     * Adds a 'linkWeight' attribute to each link with an associated link weight.
     *
     * @param netPlan    Network design
     * @param linkWeight Link weight value (same for every link)
     * @since 0.2.0
     */
    public static void setLinkWeightAttributes(NetPlan netPlan, double linkWeight)
    {
	if (linkWeight <= 0) throw new Net2PlanException("Link weight must be greater than zero");

	int E = netPlan.getNumberOfLinks();

	for(int linkId = 0; linkId < E; linkId++)
	    netPlan.setLinkAttribute(linkId, "linkWeight", Double.toString(linkWeight));
    }

    /**
     * Adds a 'linkWeight' attribute to each link with an associated link weight.
     *
     * @param netPlan     Network design
     * @param linkWeights Set of link weights (must be greater than zero)
     * @since 0.2.0
     */
    public static void setLinkWeightAttributes(NetPlan netPlan, double[] linkWeights)
    {
	if (DoubleUtils.minValue(linkWeights) <= 0) throw new Net2PlanException("Link weights must be greater than zero");

	int E = netPlan.getNumberOfLinks();

	for(int linkId = 0; linkId < E; linkId++)
	    netPlan.setLinkAttribute(linkId, "linkWeight", Double.toString(linkWeights[linkId]));
    }

    /**
     * Generates routes from the demand set of a given a network design using a given set of routing tables.
     *
     * @param netPlan Network design
     * @param f_te    Destination-based routing in the form of fractions <i>f<sub>te</sub></i> (fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a</i>(<i>e</i>) (the initial node of link <i>e</i>), that is forwarded through link <i>e</i>)
     * @since 0.2.0
     */
    public static void setRoutesFromRoutingTableMatrix(NetPlan netPlan, double[][] f_te)
    {
	// Convert from f_te to x_p
	int[][] linkTable = netPlan.getLinkTable();
	int[][] demandTable = netPlan.getDemandTable();
	double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
	List<Integer> d_p = new ArrayList<Integer>();
	List<int[]> seqLinks = new ArrayList<int[]>();
	List<Double> x_p = new ArrayList<Double>();
	GraphUtils.convert_fte2xp(linkTable, demandTable, h_d, f_te, d_p, seqLinks, x_p);

	// Remove all routes
	netPlan.removeAllRoutes();

	// Add new routes
	int R = x_p.size();
	for(int routeId = 0; routeId < R; routeId++)
	    netPlan.addRoute(d_p.get(routeId), x_p.get(routeId), seqLinks.get(routeId), null, null);
    }

    /**
     * <p>Returns the cost of a routing scheme according to the carried traffic per 
     * link and link capacities, assuming the IGP-WO cost model [1].</p>
     * 
     * <p>Let <i>E</i> be a set of links, where <i>u<sub>e</sub></i> is the capacity 
     * for link <i>e &isin; E</i>, and <i>y<sub>e</sub></i> is the carried traffic 
     * by link <i>e &isin; E</i>. Then, the total cost of the associated routing, 
     * denoted as <i>C</i>, is the sum for every link <i>e &isin; E</i> of its 
     * associated cost <i>c<sub>e</sub></i>, which is computed as follows:</p>
     * 
     * <ul>
     * <li><i>c<sub>e</sub></i> =        <i>y<sub>e</sub></i>                                     , if <i>&rho;<sub>e</sub> &isin; [0, 1/3)</i></li>
     * <li><i>c<sub>e</sub></i> =    3 * <i>y<sub>e</sub></i> -     (2 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [1/3, 2/3)</i></li>
     * <li><i>c<sub>e</sub></i> =   10 * <i>y<sub>e</sub></i> -    (16 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [2/3, 9/10)</i></li>
     * <li><i>c<sub>e</sub></i> =   70 * <i>y<sub>e</sub></i> -   (178 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [9/10, 1)</i></li>
     * <li><i>c<sub>e</sub></i> =  500 * <i>y<sub>e</sub></i> -  (1468 / 3) * <i>u<sub>e</sub></i>, if <i>&rho;<sub>e</sub> &isin; [1, 11/10)</i></li>
     * <li><i>c<sub>e</sub></i> = 5000 * <i>y<sub>e</sub></i> - (16318 / 3) * <i>u<sub>e</sub></i>, otherwise</li>
     * </ul>
     * 
     * <p>where <i>&rho;<sub>e</sub></i> is the ratio <i>y<sub>e</sub></i> / <i>u<sub>e</sub></i>.</p>
     * 
     * @param y_e Carried traffic per link vector
     * @param u_e Link capacity vector
     * @return IGP cost
     * @since 0.2.3
     * @see [1] H. Ümit, "Techniques and Tools for Intra-domain Traffic Engineering," Ph.D. Thesis, Université catholique de Louvain (Belgium), December 2009
     */
    public static double calculateIGPCost(double[] y_e, double[] u_e)
    {
        int E = u_e.length;
        double cost = 0;
        
        for(int linkId = 0; linkId < E; linkId++)
        {
            double phi = 0;

            double aux = y_e[linkId];
            if (aux >= phi) phi = aux;
            
            aux =    3.0 * y_e[linkId] -     (2.0 / 3.0) * u_e[linkId]; if (aux >= phi) phi = aux;
            aux =   10.0 * y_e[linkId] -    (16.0 / 3.0) * u_e[linkId]; if (aux >= phi) phi = aux;
            aux =   70.0 * y_e[linkId] -   (178.0 / 3.0) * u_e[linkId]; if (aux >= phi) phi = aux;
            aux =  500.0 * y_e[linkId] -  (1468.0 / 3.0) * u_e[linkId]; if (aux >= phi) phi = aux;
            aux = 5000.0 * y_e[linkId] - (16318.0 / 3.0) * u_e[linkId]; if (aux >= phi) phi = aux;
            
            cost += phi;
        }
        
        return cost;
    }
}