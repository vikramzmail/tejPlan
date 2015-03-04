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

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.impl.DenseIntMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.DoubleMatrixND;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import java.util.*;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.jgrapht.DirectedGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.AsWeightedGraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.Subgraph;

/**
 * <p>Auxiliary static methods to work with graphs.</p>
 *
 * <p>These methods make intensive use of several Java libraries (i.e. <a href='#jom'>JOM</a>, <a href='#jgrapht'>JGraphT</a> or <a href='#jung'>JUNG</a>) hiding low-level details to users.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see <a name='jom' /><a href='http://ait.upct.es/~ppavon/jom'>Java Optimization Modeler (JOM) website</a>
 * @see <a name='jgrapht' /><a href='http://jgrapht.org/'>JGraphT website</a>
 * @see <a name='jung' /><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 */
public class GraphUtils
{
    /**
     * Indicates whether (and how) or not to check routing cycles.
     * 
     * @since 0.2.3
     */
    public enum CheckRoutingCycleType
    {
        /**
         * Routing cycles are not checked.
         * 
         * @since 0.2.3
         */
        NO_CHECK,
        
        /**
         * No node is traversed more than one.
         * 
         * @since 0.2.3
         */
        NO_REPEAT_NODE,
        
        /**
         * No link is traversed more than one.
         * 
         * @since 0.2.3
         */
        NO_REPEAT_LINK
    };
    /**
     * <p>Checks for validity of a given path (continuity and, optionally, no loops).</p>
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param sequenceOfLinks Sequence of traversed links
     * @param checkCycles Indicates whether (and how) or not to check if there are cycles
     * @since 0.2.3
     */
    public static void checkRouteContinuity(int[][] linkTable, int[] sequenceOfLinks, CheckRoutingCycleType checkCycles)
    {
	if (sequenceOfLinks.length == 0) throw new Net2PlanException("No path");
        
        if (checkCycles == CheckRoutingCycleType.NO_REPEAT_LINK && IntUtils.unique(sequenceOfLinks).length != sequenceOfLinks.length)
            throw new Net2PlanException("There is a loop, seq. links = " + Arrays.toString(sequenceOfLinks));

	Set<Integer> visitedNodes = new LinkedHashSet<Integer>();
	int originNode = linkTable[sequenceOfLinks[0]][0];
	visitedNodes.add(originNode);
	for(int hopId = 0; hopId < sequenceOfLinks.length - 1; hopId++)
	{
	    int destinationNodeId = linkTable[sequenceOfLinks[hopId]][1];
	    	if (destinationNodeId != linkTable[sequenceOfLinks[hopId + 1]][0])
                throw new RuntimeException("Physical route is not feasible");
            
            switch(checkCycles)
            {
                case NO_REPEAT_NODE:
                    if (visitedNodes.contains(destinationNodeId))
                    {
                        String seqNodes = IntUtils.join(IntUtils.toArray(visitedNodes), " ") + " " + destinationNodeId + "...";
                        throw new Net2PlanException("There is a loop, seq. links = " + Arrays.toString(sequenceOfLinks) + ", seq. nodes = " + seqNodes);
                    }
                    break;
                    
                default:
                    break;
            }

	    visitedNodes.add(destinationNodeId);
	}
    }

    /**
     * Returns the total cost for a given path.
     * 
     * @param seqLinks Sequence of traversed links per each route
     * @param costVector Link weights
     * @return Cost of the path
     * @since 0.2.0
     */
    public static double convertPath2PathCost(int[] seqLinks, double[] costVector)
    {
	double pathCost = DoubleUtils.sum(DoubleUtils.select(costVector, seqLinks));

	return pathCost;
    }

    /**
     * Returns the total cost for a given a list of paths.
     * 
     * @param pathList List of paths
     * @param costVector Link weights
     * @return Cost per path
     * @since 0.2.0
     */
    public static double[] convertPathList2PathCost(List<int[]> pathList, double[] costVector)
    {
	double[] pathCost = new double[pathList.size()];

	ListIterator<int[]> it = pathList.listIterator();
	while(it.hasNext())
	{
	    int pathId = it.nextIndex();
	    int[] path = it.next();
	    pathCost[pathId] = convertPath2PathCost(path, costVector);
	}

	return pathCost;
    }

    /**
     * Returns the carried traffic per link.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param demandTable The table of demands in the network: one row per demand, first column input node, second column output node
     * @param x_de Array containing the amount of traffic from demand d which traverses link e
     * @return Carried traffic per link
     * @since 0.2.0
     */
    public static double[] convert_xde2ye(int[][] linkTable, int[][] demandTable, double[][] x_de)
    {
	if (x_de.length == 0) return new double[0];

	int E = x_de[0].length;

	List<Integer> d_p = new ArrayList<Integer>();
	List<int[]> seqLinks = new ArrayList<int[]>();
	List<Double> x_p = new ArrayList<Double>();

	convert_xde2xp(linkTable, demandTable, x_de, d_p, seqLinks, x_p);

	double[] y_e = convert_xp2ye(DoubleUtils.toArray(x_p), seqLinks, E);

	return y_e;
    }
    
    /**
     * Given a set of traffic routes and their carried traffic returns a destination-based
     * routing in the form x_te (amount of traffic targeted to node t, transmitted through node e).
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param demandTable The table of demands in the network: one row per demand, first column input node, second column output node
     * @param d_p This is the form in which the demands for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param allSeqLinks Sequence of traversed links per each route
     * @param N Number of nodes
     * @return Destination-based routing in the form x_te (amount of traffic targeted to node t, transmitted through node e)
     * @since 0.2.0
     */
    public static double[][] convert_xp2xte(int[][] linkTable, int[][] demandTable, int[] d_p, double[] x_p, List<int[]> allSeqLinks, int N)
    {
	int E = linkTable.length;
	int D = demandTable.length;

	double[][] x_de = convert_xp2xde(linkTable, x_p, d_p, allSeqLinks);
	double[][] x_te = new double[N][E];

	for(int demandId = 0; demandId < D; demandId++)
	    for(int linkId = 0; linkId < E; linkId++)
		x_te[demandTable[demandId][1]][linkId] += x_de[demandId][linkId];

	return x_te;
    }

    /**
     * Given a set of traffic routes and their carried traffic returns a destination-based
     * routing in the form f_te (fractions of traffic in a node, that is forwarded
     * through each of its output links).
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param demandTable The table of demands in the network: one row per demand, first column input node, second column output node
     * @param d_p This is the form in which the demands for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param allSeqLinks Sequence of traversed links per each route
     * @param N Number of nodes
     * @return Destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its output links)
     * @since 0.2.0
     */
    public static double[][] convert_xp2fte(int[][] linkTable, int[][] demandTable, int[] d_p, double[] x_p, List<int[]> allSeqLinks, int N)
    {
	double[][] x_te = convert_xp2xte(linkTable, demandTable, d_p, x_p, allSeqLinks, N);
	return convert_xte2fte(linkTable, x_te);
    }

    /**
     * Returns the carried traffic per link.
     * 
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param seqLinks Sequence of traversed links per each route
     * @param E Number of links
     * @return Carried traffic per link
     * @since 0.2.0
     */
    public static double[] convert_xp2ye(double[] x_p, List<int[]> seqLinks, int E)
    {
	DoubleMatrix1D xx_p = DoubleFactory1D.dense.make(x_p);
	DoubleMatrixND delta_ep = computeLink2PathAssignmentMatrix(seqLinks, E);

	return delta_ep.view2D().zMult(xx_p, null).toArray();
    }

    /**
     * Converts a given sequence of links to the corresponding sequence of nodes.
     * 
     * @param seqLinks Sequence of links
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @return Sequence of nodes
     * @since 0.2.2
     */
    public static int[] convertSeqLinks2seqNodes(int[] seqLinks, int[][] linkTable)
    {
        int[] seqNodes = new int[seqLinks.length + 1];

        seqNodes[0] = linkTable[seqLinks[0]][0];

        for(int i = 0; i < seqLinks.length; i++)
            seqNodes[i+1] = linkTable[seqLinks[i]][1];

        return seqNodes;
    }

    /**
     * Returns the shortest pair of link-disjoint paths. Each row represents a path.
     * In case a path cannot be found, its corresponding row will be an empty array.
     * Internally it uses the Suurballe-Tarjan algorithm.
     * 
     * @param src Origin node
     * @param dst Destination node
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param costVector Link weights
     * @param N Number of nodes
     * @return Shortest pair of link-disjoint paths
     * @since 0.2.1
     */
    public static int[][] getTwoLinkDisjointPaths(int src, int dst, int[][] linkTable, double[] costVector, int N)
    {
        int[][] linkDisjointSPs = new int[2][0];

        if (N < 2) return linkDisjointSPs;

        int E = linkTable.length;

        Graph<Integer, Integer> graph = JUNGUtils.getGraphFromLinkTable(linkTable, N);
        final Transformer<Integer, Double> nev = JUNGUtils.getEdgeWeightTransformer(costVector);
	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer linkId)
		{
		    return (nev.transform(linkId) == Double.MAX_VALUE) ? false : true;
		}
	    });
	graph = linkFilter.transform(graph);

        DijkstraShortestPath<Integer, Integer> dsp = new DijkstraShortestPath<Integer, Integer>(graph, nev);

        final List<Integer> sp1 = dsp.getPath(src, dst);

        if (sp1.isEmpty()) return linkDisjointSPs;

        for(int linkId : sp1)
        {
            int originNodeId = linkTable[linkId][0];
            int destinationNodeId = linkTable[linkId][1];

            for(int linkIdToRemove : graph.findEdgeSet(destinationNodeId, originNodeId))
                graph.removeEdge(linkIdToRemove);

            graph.removeEdge(linkId);
            graph.addEdge(linkId, destinationNodeId, originNodeId);
        }

        dsp.reset();

        List<Integer> sp2 = dsp.getPath(src, dst);

        if (sp2.isEmpty())
        {
            linkDisjointSPs[0] = IntUtils.toArray(sp1);
            return linkDisjointSPs;
        }

        for(int linkId : sp2)
        {
            if (sp1.contains(linkId))
                sp1.remove(Integer.valueOf(linkId));
            else
                sp1.add(linkId);
        }

        for(int linkId = 0; linkId < E; linkId++)
            graph.removeEdge(linkId);

        for(int linkId : sp1)
        {
            int originNodeId = linkTable[linkId][0];
            int destinationNodeId = linkTable[linkId][1];

            graph.addEdge(linkId, originNodeId, destinationNodeId);
        }

        dsp.reset();

        List<Integer> sp1_final = dsp.getPath(src, dst);

        for(int linkId : sp1_final)
            graph.removeEdge(linkId);

        dsp.reset();

        List<Integer> sp2_final = dsp.getPath(src, dst);

        linkDisjointSPs[0] = IntUtils.toArray(sp1_final);
        linkDisjointSPs[1] = IntUtils.toArray(sp2_final);

        return linkDisjointSPs;
    }

    /**
     * Returns the shortest pair of node-disjoint paths. Each row represents a path.
     * In case a path cannot be found, its corresponding row will be an empty array.
     * Internally it uses a modified version of the Suurballe-Tarjan algorithm.
     * 
     * @param src Origin node
     * @param dst Destination node
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param costVector Link weights
     * @param N Number of nodes
     * @return Shortest pair of node-disjoint paths
     * @since 0.2.1
     */
    public static int[][] getTwoNodeDisjointPaths(final int src, int dst, int[][] linkTable, double[] costVector, int N)
    {
        int[][] nodeDisjointSPs = new int[2][0];

        if (N < 2) return nodeDisjointSPs;

        int E = linkTable.length;

        int aux_N = 2*N;
        int aux_E = E;

        int[][] aux_linkTable = new int[E+N][2];
        double[] aux_costVector = new double[E+N];

        for(int linkId = 0; linkId < E; linkId++)
        {
            aux_linkTable[linkId][0] = E + linkTable[linkId][0];
            aux_linkTable[linkId][1] = linkTable[linkId][1];
            aux_costVector[linkId] = costVector[linkId];
        }

        for(int nodeId = 0; nodeId < N; nodeId++)
        {
            aux_linkTable[aux_E][0] = nodeId;
            aux_linkTable[aux_E][1] = E + nodeId;
            aux_costVector[aux_E] = 1;

            aux_E++;
        }

        nodeDisjointSPs = getTwoLinkDisjointPaths(E + src, dst, aux_linkTable, aux_costVector, aux_N);

        List<Integer> sp1 = IntUtils.toList(nodeDisjointSPs[0]);
        List<Integer> sp2 = IntUtils.toList(nodeDisjointSPs[1]);

        Iterator<Integer> it = sp1.iterator();
        while(it.hasNext())
            if (it.next() >= E)
                it.remove();

        it = sp2.iterator();
        while(it.hasNext())
            if (it.next() >= E)
                it.remove();

        nodeDisjointSPs[0] = IntUtils.toArray(sp1);
        nodeDisjointSPs[1] = IntUtils.toArray(sp2);

        return nodeDisjointSPs;
    }

    /**
     * Returns all the loopless shortest paths between two nodes. All these paths
     * have the same total length.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param originNodeId      Origin node
     * @param destinationNodeId Destination node
     * @param N Number of nodes
     * @param weights Link weights
     * @return All loopless shortest paths
     * @since 0.2.2
     */
    public static List<int[]> getAllLooplessShortestPaths(int[][] linkTable, int originNodeId, int destinationNodeId, int N, double[] weights)
    {
	Graph<Integer, Integer> g = JUNGUtils.getGraphFromLinkTable(linkTable, N);
	final Transformer<Integer, Double> nev = JUNGUtils.getEdgeWeightTransformer(weights);
	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer linkId)
		{
		    return (nev.transform(linkId) == Double.MAX_VALUE) ? false : true;
		}
	    });
	g = linkFilter.transform(g);

	CandidatePathList.YenLoopLessKShortestPathsAlgorithm<Integer, Integer> paths = new CandidatePathList.YenLoopLessKShortestPathsAlgorithm<Integer, Integer>(g, nev)
	{
	    @Override
	    public boolean compareCandidateToShortestPath(JUNGUtils.GraphPath<Integer> candidate, JUNGUtils.GraphPath<Integer> shortestPath)
	    {
		return candidate.getPathWeight() == shortestPath.getPathWeight();
	    }
	};

	List<List<Integer>> aux = paths.getPaths(originNodeId, destinationNodeId, Integer.MAX_VALUE);
	List<int[]> pathList = new LinkedList<int[]>();
	for(List<Integer> path : aux)
	    pathList.add(IntUtils.toArray(path));

	return pathList;
    }

    /**
     * Returns the K-loopless shortest paths between two nodes. If only <i>n<K</i>
     * could be found, only these ones will be returned.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param originNodeId      Origin node
     * @param destinationNodeId Destination node
     * @param N Number of nodes
     * @param K Number of different paths
     * @param weights Link weights
     * @return K-shortest paths
     * @since 0.2.0
     */
    public static List<int[]> getKLooplessShortestPaths(int[][] linkTable, int originNodeId, int destinationNodeId, int N, int K, double[] weights)
    {
	Graph<Integer, Integer> g = JUNGUtils.getGraphFromLinkTable(linkTable, N);
	final Transformer<Integer, Double> nev = JUNGUtils.getEdgeWeightTransformer(weights);
	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer linkId)
		{
		    return (nev.transform(linkId) == Double.MAX_VALUE) ? false : true;
		}
	    });
	g = linkFilter.transform(g);

	CandidatePathList.YenLoopLessKShortestPathsAlgorithm<Integer, Integer> paths = new CandidatePathList.YenLoopLessKShortestPathsAlgorithm<Integer, Integer>(g, nev);
	List<int[]> pathList = new ArrayList<int[]>();
	for(List<Integer> path : paths.getPaths(originNodeId, destinationNodeId, K))
	    pathList.add(IntUtils.toArray(path));

	return pathList;
    }

    /**
     * Given the amount of traffic for each demand d traversing link e, it computes
     * the equivalent path-based routing.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param demandTable The table of demands in the network: one row per demand, first column input node, second column output node
     * @param x_de Array containing the amount of traffic from demand d which traverses link e
     * @param d_p This is the form in which the demands for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param sequenceOfLinks Sequence of traversed links per each route
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @return The number of new paths
     * @since 0.2.0
     */
    public static int convert_xde2xp(int[][] linkTable, int[][] demandTable, double[][] x_de, List<Integer> d_p, List<int[]> sequenceOfLinks, List<Double> x_p)
    {
	double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

	int E = linkTable.length;
	int D = demandTable.length;
        
        int numPaths = 0;

	for(int demandId = 0; demandId < D; demandId++)
	{
	    int ingressNodeId = demandTable[demandId][0];
	    int egressNodeId = demandTable[demandId][1];

	    double[] x_e = DoubleUtils.copy(x_de[demandId]);

	    for(int linkId = 0; linkId < E; linkId++)
		if (x_e[linkId] < 0)
		    x_e[linkId] = 0;

	    int[] incomingLinksToIngressNode = GraphUtils.getIncomingLinks(linkTable, ingressNodeId);
	    int[] outgoingLinksFromIngressNode = GraphUtils.getOutgoingLinks(linkTable, ingressNodeId);

	    double divAtIngressNode = DoubleUtils.sum(DoubleUtils.select(x_e, outgoingLinksFromIngressNode)) - DoubleUtils.sum(DoubleUtils.select(x_e, incomingLinksToIngressNode));

	    while(divAtIngressNode > PRECISIONFACTOR)
	    {
		List<Integer> candidates = new LinkedList<Integer>();
		for(int linkId = 0; linkId < E; linkId++)
		    if (x_e[linkId] > 0)
			candidates.add(linkId);

		if (candidates.isEmpty()) break;

		int[] candidateLinks = IntUtils.toArray(candidates);
		int[][] newLinkTable = IntFactory2D.dense.make(linkTable).viewSelection(candidateLinks, null).toArray();
		int[] path = GraphUtils.getShortestPath(newLinkTable, ingressNodeId, egressNodeId, DoubleUtils.ones(candidates.size()));
		if (path.length == 0) break;

		path = IntUtils.select(candidateLinks, path);
		double trafficInPath = Math.min(DoubleUtils.minValue(DoubleUtils.select(x_e, path)), divAtIngressNode);

		divAtIngressNode -= trafficInPath;

		x_p.add(trafficInPath);
		d_p.add(demandId);
		sequenceOfLinks.add(path);
		for(int linkId : path)
		    x_e[linkId] -= trafficInPath;
                numPaths++;

		if (divAtIngressNode <= PRECISIONFACTOR) break;
	    }
	}
        
        return numPaths;
    }

    /**
     * Given a destination-based routing in the form of an array x_te (amount of traffic targeted to node t, transmitted through node e), it
     * returns the associated destination-based routing in the form of fractions f_te (fraction of the traffic targeted to node t that arrives
     * (or is generated in) node a(e) (the initial node of link e), that is forwarded through link e). If a node n does not forward any traffic to a destination t,
     * it is not possible to determine the f_te fractions for the output links of the node. Then, the function arbitrarily assumes that the 100% of the traffic
     * to node t, would be forwarded through the shortest path (in number of hops) from n to t. Finally note that for every destination t, f_te = 0
     * for all the links e outgoing of node t.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param x_te Array containing the amount of traffic targeted to node t, transmitted through node e
     * @return The f_te array created
     * @since 0.2.0
     */
    public static double[][] convert_xte2fte(int[][] linkTable, double[][] x_te)
    {
	    int N = x_te.length;
	    int E = linkTable.length;

	    double[][] f_te = new double[N][E];
	    /* For each node n, where we are filling the routing table */
	    for (int n = 0; n < N; n++)
	    {
		    /* Compute the outgoing links from node n */
		    int[] outLinks = getOutgoingLinks(linkTable, n);
		    /* For each destination t, fill the f_te fractions */
		    for (int t = 0; t < N; t++)
		    {
			    if (n == t) continue; // f_te = 0 for all the outgoing links of node t
			    double outTraffic = DoubleUtils.sum(DoubleUtils.select(x_te[t], outLinks));
			    if (outTraffic != 0) // there is traffic leaving the node
				    for (int e : outLinks)
					    f_te[t][e] = x_te[t][e] / outTraffic;
			    else 	// no traffic leaving the node: all the routing in the shortest path from n to t
			    {
				    int[] seqLinks = GraphUtils.getShortestPath(linkTable, n, t, DoubleUtils.ones(E));
				    if (seqLinks.length == 0) continue;
				    f_te[t][seqLinks[0]] = 1;
			    }
		    }
	    }
	    return f_te;
    }

    /**
     * This function checks the validity of a destination-based routing (i.e IP routing). The destination-based routing is represented by the
     * f_te variables. In particular, the function checks (i) if the routing has closed routing cycles
     * (so that, the 100% of the traffic that enter these cycles, never reaches the destination), (ii) if the routing has open cycles (so that the traffic
     * entering the cycle has in every loop, a positive probability of leaving the cycle). Note that if a routing has cycles of type (i) or (ii), it cannot be
     * represented by a finite amount of routes in Net2Plan.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param f_te For each destination node t, and each link e, f_te[t][e] sets the fraction of the traffic targeted to node t that arrives
     * (or is generated in) node a(e) (the initial node of link e), that is forwarded through link e.
     * It must hold that for every node n different of t, the sum of the fractions f_te along its outgoing links
     * must be 1. For every destination t, f_te = 0 for all the links e that are outgoing links of t
     * @return -2 if the routing has closed loops, -1 if the routing does not have closed routing cycles, but has open routing cycles,
     * 0 if the routing has no cycles, and thus can be represented with a finite amount of routes.
     * @since 0.2.0
     */
    public static int checkRouting_fte(int[][] linkTable, double[][] f_te)
    {
	    int N = f_te.length;
	    int E = linkTable.length;

	    DenseDoubleAlgebra algebra = new DenseDoubleAlgebra();

	    /* For each target node t, detect if the routing is cycleless and well defined */
	    for (int t = 0; t < N; t++)
	    {
		    /* Compute the Q matrix */
		    DoubleMatrix2D IminusQ = DoubleFactory2D.dense.identity(N - 1); // the node t is eliminated
		    for (int e = 0; e < E; e++)
		    {
			    int a_e = linkTable[e][0];
			    int b_e = linkTable[e][1];
			    if ((a_e == t) || (b_e == t)) continue;
			    int aux_a_e = (a_e > t) ? a_e - 1 : a_e;
			    int aux_b_e = (b_e > t) ? b_e - 1 : b_e;
			    IminusQ.set(aux_a_e, aux_b_e, IminusQ.get(aux_a_e, aux_b_e) - f_te[t][e]);
		    }

		    /* Compute the chain fundamental matrix M */
		    if (algebra.det(IminusQ) == 0) return -2; //throw new RuntimeException ("The x_te vector is associated to a routing that is not well defined: some destinations may not be reached");
		    DoubleMatrix2D M = algebra.inverse(IminusQ);
		    for (int contN = 0; contN < N - 1; contN++)
			    if (M.get(contN, contN) != 1)
			    {
				return -1;
			    } //throw new RuntimeException ("The x_te vector is associated to a routing that is not well defined in the strict sense: there may be cycles in the routing");
	    }

	    return 0;
    }

    /**
     * Given a table of links, demands or paths, where first column is the link/demand/path inital node, and second column the target node,
     * it computes the incidence matrix. This is a matrix with as many rows as nodes, and as many columns as links/demands/paths. Position (n,e)
     * has a 1 if the link/demand/path e is initiated in node n, -1 if it ends in node n, and 0 otherwise.
     * 
     * @param anyTable The table of links, deamnds, paths etc. where we extract the incidence matrix
     * @param N The number of nodes in the network
     * @return The incidence matrix
     * @since 0.2.0
     */
    public static double[][] getIncidenceMatrix(int[][] anyTable, int N)
    {
	    final int E = anyTable.length;
	    double[][] res = new double[N][E];
	    for (int e = 0; e < E; e++)
	    {
		    res[anyTable[e][0]][e] = 1;
		    res[anyTable[e][1]][e] = -1;
	    }
	    return res;
    }

    /**
     * Given a destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its output links), and
     * an offered traffic to the network, it generates the resulting set of paths that are produced.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param demandTable The table of demands in the network: one row per demand, first column input node, second column output node
     * @param h_d The amount of traffic offered for each demand
     * @param f_te For each destination node t, and each link e, f_te[t][e] sets the fraction of the traffic targeted to node t that arrives
     * (or is generated in) node a(e) (the initial node of link e), that is forwarded through link e.
     * It must hold that for every node n different of t, the sum of the fractions f_te along its outgoing links
     * must be 1. For every destination t, f_te = 0 for all the links e that are outgoing links of t
     * @param demands_p This is the form in which the demands for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param seqLinks_p This is the form in which the sequence of links for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @since 0.2.0
     */
    public static void convert_fte2xp(int[][] linkTable, int[][] demandTable, double[] h_d, double[][] f_te, List<Integer> demands_p, List<int[]> seqLinks_p, List<Double> x_p)
    {
	int check_fte = checkRouting_fte(linkTable, f_te);

	if (check_fte == -1) throw new RuntimeException("Routing has cycles");
	if (check_fte == -2) throw new RuntimeException("Some nodes are not reachable");

	int D = demandTable.length;

	/* For each demand */
	List<IntArrayList> aux_seqLinks_p = new LinkedList<IntArrayList>();
	for (int d = 0; d < D; d++)
	{
	    int a_d = demandTable[d][0];
	    int b_d = demandTable[d][1];
	    LinkedList<Integer> activePaths = new LinkedList<Integer>();
	    int[] outLinks_a_d = getOutgoingLinks(linkTable, a_d);

	    for (int e : outLinks_a_d)
	    {
		if (f_te[b_d][e] > 0)
		{
		    aux_seqLinks_p.add(new IntArrayList(new int[] { e }));
		    x_p.add(h_d[d] * f_te[b_d][e]);
		    demands_p.add(d);
		    if (linkTable[e][1] != b_d) activePaths.add(x_p.size() - 1);
		}
	    }

	    while (activePaths.size() > 0)
	    {
		final int p = activePaths.getFirst();
		IntArrayList seqLinks = aux_seqLinks_p.get(p);
		final double current_xp = x_p.get(p);
		final int currentLastNode = linkTable[seqLinks.get(seqLinks.size() - 1)][1];
		int[] outLinks = getOutgoingLinks(linkTable, currentLastNode);
		boolean alreadyOneOutputLinkWithTraffic = false;
		for (int e : outLinks)
		{
		    if (f_te[b_d][e] > 0)
		    {
			if (!alreadyOneOutputLinkWithTraffic)
			{
			    alreadyOneOutputLinkWithTraffic = true;
			    seqLinks.add(e);
			    x_p.set(p, current_xp * f_te[b_d][e]);
			    if (linkTable[e][1] == b_d) activePaths.removeFirst();
			}
			else
			{
			    IntArrayList extendedSeqLinks = seqLinks.copy();
			    extendedSeqLinks.set(extendedSeqLinks.size() - 1, e);
			    aux_seqLinks_p.add(extendedSeqLinks);
			    x_p.add(current_xp * f_te[b_d][e]);
			    demands_p.add(d);
			    if (linkTable[e][1] != b_d) activePaths.add(x_p.size() - 1);
			}
		    }
		}

		if (alreadyOneOutputLinkWithTraffic == false) throw new RuntimeException("Unexpected error");
	    }
	}

	int P = aux_seqLinks_p.size();
	for (int pathId = 0; pathId < P; pathId++)
	{
	    aux_seqLinks_p.get(pathId).trimToSize();
	    seqLinks_p.add(aux_seqLinks_p.get(pathId).elements());
	}
    }

    /**
     * Given a link table, with a row per link and two columns (first the origin node, second the destination node), and given a node n, it
     * returns the links that leave the node. Note that we can use this function passing demand tables or path tables, to get the outgoing demands/paths of a node.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param node Node identifier
     * @return The outgoing links of the node
     * @since 0.2.0
     */
    public static int[] getOutgoingLinks(int[][] linkTable, int node)
    {
        final int E = linkTable.length;
        IntArrayList links = new IntArrayList();
        for (int e = 0; e < E; e++)
                if (linkTable[e][0] == node) links.add(e);
        links.trimToSize();
        
        return links.elements();
    }

    /**
     * Given a link table, with a row per link and two columns (first the origin node, second the destination node), and given a node n, it
     * returns the links that enter the node. Note that we can use this function passing demand tables or path tables, to get the incoming demands/paths of a node.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param node Node identifier
     * @return The incoming links of the node
     * @since 0.2.0
     */
    public static int[] getIncomingLinks(int[][] linkTable, int node)
    {
        final int E = linkTable.length;
        IntArrayList links = new IntArrayList();
        for (int e = 0; e < E; e++)
                if (linkTable[e][1] == node) links.add(e);
        links.trimToSize();
        return links.elements();
    }

    /**
     * Given a path-based routing, returns the amount of traffic for each demand d
     * traversing each link e.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param x_p This is the form in which the amounts of traffic in each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param d_p This is the form in which the demands for each path are returned. The user should pass an empty array to the function, and this array is internally filled with the correct data.
     * @param sequenceOfLinks Sequence of traversed links per each route
     * @return Demand-link routing
     * @since 0.2.0
     */
    public static double[][] convert_xp2xde(int[][] linkTable, double[] x_p, int[] d_p, List<int[]> sequenceOfLinks)
    {
	int E = linkTable.length;
	int D = d_p.length;
	int P = x_p.length;

	DoubleMatrix2D x_de = DoubleFactory2D.sparse.make(D, E, 0);

	for(int p = 0; p < P; p++)
	{
	    x_de.viewRow(d_p[p]).viewSelection(sequenceOfLinks.get(p)).assign(DoubleFunctions.plus(x_p[p]));
	}

	return x_de.toArray();
    }

    /**
     * Returns the shortest path that fulfills a given minimum capacity requirement
     * along its traversed links. In case no path can be found, an empty array will
     * be returned.
     * 
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param originNodeId      Origin node
     * @param destinationNodeId Destination node
     * @param costVector Link weights
     * @param capacityVector Link capacity vector
     * @param capacityGoal Minimum capacity required
     * @return Shortest path fulfilling a minimum capacity requirement
     * @since 0.2.0
     */
    public static int[] getCapacitatedShortestPath(int[][] linkTable, int originNodeId, int destinationNodeId, final double[] costVector, final double[] capacityVector, final double capacityGoal)
    {
	int N = IntUtils.maxValue(linkTable);
	N = Math.max(N, Math.max(originNodeId, destinationNodeId));

	Graph<Integer, Integer> graph = JUNGUtils.getGraphFromLinkTable(linkTable, N);
	Transformer<Integer, Double> nev = JUNGUtils.getEdgeWeightTransformer(costVector);

	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer object)
		{
		    return (costVector[object] == Double.MAX_VALUE || capacityVector[object] < capacityGoal) ? false : true;
		}
	    });

	graph = linkFilter.transform(graph);

	if (!graph.containsVertex(originNodeId) || !graph.containsVertex(destinationNodeId))
	    return new int[0];

	DijkstraShortestPath<Integer, Integer> dsp = new DijkstraShortestPath<Integer, Integer>(graph, nev);
	List<Integer> path = dsp.getPath(originNodeId, destinationNodeId);
	return IntUtils.toArray(path);
    }

    /**
     * Returns the demand-path incidence matrix (a <i>D</i>x<i>P</i> matrix in which an element <i>&delta;<sub>dp</sub></i> is equal to 1 if traffic route <i>p</i> is able to carry traffic from demand <i>d</i>).
     *
     * @param netPlan Network plan
     * @return The demand-path incidence matrix
     * @since 0.2.0
     */
    public static DoubleMatrixND computeDemand2PathAssignmentMatrix(NetPlan netPlan)
    {
	int D = netPlan.getNumberOfDemands();
	int R = netPlan.getNumberOfRoutes();

	DoubleMatrixND delta_dp = new DoubleMatrixND(new int[] {D, R}, "sparse");
	for(int routeId = 0; routeId < R; routeId++)
	{
	    delta_dp.setQuick(new int[] {netPlan.getRouteDemand(routeId), routeId}, 1);
	}

	return delta_dp;
    }

    /**
     * <p>Returns the demand-path vector (a <i>1</i>x<i>P</i> vector in which an element <i>d(p)</i> is equal to the demand identifier for path <i>p</i>).</p>
     *
     * @param netPlan Network plan
     * @return The demand-path vector
     * @since 0.2.0
     */
    public static int[] getDemandPathVector(NetPlan netPlan)
    {
	int R = netPlan.getNumberOfRoutes();

	int[] d_p = new int[R];

	for(int routeId = 0; routeId < R; routeId++)
	{
	    d_p[routeId] = netPlan.getRouteDemand(routeId);
	}

	return d_p;
    }

    /**
     * <p>Returns the node adjacency matrix (a <i>N</i>x<i>N</i> matrix in which an element <i>a<sub>ij</sub></i> is equal to the number of links from node <i>i</i> to node <i>j</i>).</p>
     *
     * <p>The output is in the sparse <code>DoubleMatrixND</code> format, so that could be directly used along with the <a href='#jom'>JOM</a> library in order to solve optimization problems.</p>
     *
     * <p>For users not interested in this format, a classical dense <code>double[][]</code> matrix could be obtained via the command:</p>
     *
     * <p><code>double[][] matrix = (double[][]) getNodeAdjacencyMatrix(netPlan).toArray();</code></p>
     *
     * @param netPlan Network plan
     * @return The node adjacency matrix
     * @since 0.2.0
     * @see <a href='http://ait.upct.es/~ppavon/jom'>Java Optimization Modeler (JOM) website</a>
     */
    public static DoubleMatrixND getNodeAdjacencyMatrix(NetPlan netPlan)
    {
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int[][] linkTable = netPlan.getLinkTable();

	DoubleMatrixND A_nn = new DoubleMatrixND(new int[] {N, N}, "sparse");
	for (int linkId = 0; linkId < E; linkId++)
	{
	    int originNodeId = linkTable[linkId][0];
	    int destinationNodeId = linkTable[linkId][1];
	    A_nn.set(new int[] {originNodeId, destinationNodeId}, A_nn.get(new int[] {originNodeId, destinationNodeId}) + 1);
	}

	return A_nn;
    }

    /**
     * <p>Returns the node-demand incidence matrix (a <i>N</i>x<i>D</i> matrix in which an element <i>w<sub>nd</sub></i> is equal to 1 if node <i>n</i> is the ingress node of demand <i>d</i>, -1 if node <i>n</i> is the egress node of demand <i>d</i>, and zero otherwise).</p>
     *
     * <p>The output is in the sparse <code>DoubleMatrixND</code> format, so that could be directly used along with the <a href='#jom'>JOM</a> library in order to solve optimization problems.</p>
     *
     * <p>For users not interested in this format, a classical dense <code>double[][]</code> matrix could be obtained via the command:</p>
     *
     * <p><code>double[][] matrix = (double[][]) getNodeDemandIncidenceMatrix(netPlan).toArray();</code></p>
     *
     * @param netPlan Network plan
     * @return The node-demand incidence matrix
     * @since 0.2.0
     * @see <a href='http://ait.upct.es/~ppavon/jom'>Java Optimization Modeler (JOM) website</a>
     */
    public static DoubleMatrixND getNodeDemandIncidenceMatrix(NetPlan netPlan)
    {
	int N = netPlan.getNumberOfNodes();
	int D = netPlan.getNumberOfDemands();
	int[][] demandTable = netPlan.getDemandTable();

	DoubleMatrixND w_nd = new DoubleMatrixND(new int[] {N, D}, "sparse");
	for (int demandId = 0; demandId < D; demandId++)
	{
	    int ingressNodeId = demandTable[demandId][0];
	    int egressNodeId = demandTable[demandId][1];
	    w_nd.set(new int[] {ingressNodeId, demandId}, 1);
	    w_nd.set(new int[] {egressNodeId, demandId}, -1);
	}

	return w_nd;
    }

    /**
     * Returns the node-link incidence matrix (a <i>N</i>x<i>E</i> matrix in which an element <i>a<sub>ne</sub></i> is equal to 1 if node <i>n</i> is the origin node of link <i>e</i>, -1 if node <i>n</i> is the destination node of link <i>e</i>, and zero otherwise).
     *
     * <p>The output is in the sparse <code>DoubleMatrixND</code> format, so that could be directly used along with the <a href='#jom'>JOM</a> library in order to solve optimization problems.</p>
     *
     * <p>For users not interested in this format, a classical dense <code>double[][]</code> matrix could be obtained via the command:</p>
     *
     * <p><code>double[][] matrix = (double[][]) getNodeLinkIncidenceMatrix(netPlan).toArray();</code></p>
     *
     * @param netPlan Network plan
     * @return The node-link incidence matrix
     * @since 0.2.0
     * @see <a href='http://ait.upct.es/~ppavon/jom'>Java Optimization Modeler (JOM) website</a>
     */
    public static DoubleMatrixND getNodeLinkIncidenceMatrix(NetPlan netPlan)
    {
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int[][] linkTable = netPlan.getLinkTable();

	DoubleMatrixND A_ne = new DoubleMatrixND(new int[] {N, E}, "sparse");

	for (int linkId = 0; linkId < E; linkId++)
	{
	    int originNodeId = linkTable[linkId][0];
	    int destinationNodeId = linkTable[linkId][1];
	    A_ne.set(new int[] {originNodeId, linkId}, 1);
	    A_ne.set(new int[] {destinationNodeId, linkId}, -1);
	}

	return A_ne;
    }

    /**
     * Returns the link-path incidence matrix (an <i>E</i>x<i>P</i> matrix in which an element <i>&delta;<sub>ep</sub></i> is equal to the number of times which traffic route <i>p</i> traverses link <i>e</i>).
     *
     * @param seqLinks Sequence of traversed links per each route
     * @param E Number of links within the network
     * @return The link-path incidence matrix
     * @since 0.2.0
     */
    public static DoubleMatrixND computeLink2PathAssignmentMatrix(List<int[]> seqLinks, int E)
    {
	int R = seqLinks.size();

	DoubleMatrixND delta_ep = new DoubleMatrixND(new int[] {E, R}, "sparse");

	ListIterator<int[]> it = seqLinks.listIterator();
	while(it.hasNext())
	{
	    int routeId = it.nextIndex();

	    IntMatrix1D[] indexes = new IntMatrix1D[2];
	    indexes[0] = new DenseIntMatrix1D(it.next());
	    indexes[1] = new DenseIntMatrix1D(new int[] {routeId});

	    delta_ep.viewSelection(indexes).assign(DoubleFunctions.plus(1));
	}

	return delta_ep;
    }

    /**
     * Returns the link-path incidence matrix (an <i>E</i>x<i>P</i> matrix in which an element <i>&delta;<sub>ep</sub></i> is equal to the number of times which traffic route <i>p</i> traverses link <i>e</i>).
     *
     * @param netPlan Network plan
     * @return The link-path incidence matrix
     * @since 0.2.0
     */
    public static DoubleMatrixND computeLink2PathAssignmentMatrix(NetPlan netPlan)
    {
	return computeLink2PathAssignmentMatrix(netPlan.getRouteAllSequenceOfLinks(), netPlan.getNumberOfLinks());
    }

    /**
     * Checks whether the physical topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
     *
     * @param netPlan A network plan
     * @return <code>true</code> if the physical topology is bidirectional, and false otherwise
     * @since 0.2.0
     */
    public static boolean isBidirectional(NetPlan netPlan)
    {
        return isBidirectional(netPlan.getLinkTable(), netPlan.getNumberOfNodes());
//	if (!netPlan.hasNodes()) return false;
//
//	DoubleMatrixND A_nn = getNodeAdjacencyMatrix(netPlan);
//	DoubleProperty properties = DoubleProperty.ZERO;
//	return properties.isSymmetric(A_nn.view2D());
    }
    
    /**
     * Checks whether the physical topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param N Number of nodes
     * @return <code>true</code> if the physical topology is bidirectional, and false otherwise
     * @since 0.2.3
     */
    public static boolean isBidirectional(int[][] linkTable, int N)
    {
        org.jgrapht.Graph<Integer, Integer> graph = JGraphTUtils.getGraphFromLinkTable(linkTable, N);
	return JGraphTUtils.isWeightedBidirectional(graph);
    }

    /**
     * Checks whether the physical topology has the same number of links between each node pair in both directions (assuming multi-digraphs) and same weights per direction.
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param linkWeight Link weight vector
     * @param N Number of nodes
     * @return <code>true</code> if the physical topology is bidirectional and symmetric, and false otherwise
     * @since 0.2.3
     */
    public static boolean isWeightedBidirectional(int[][] linkTable, double[] linkWeight, int N)
    {
        org.jgrapht.Graph<Integer, Integer> auxGraph = JGraphTUtils.getGraphFromLinkTable(linkTable, N);
	org.jgrapht.Graph<Integer, Integer> graph = JGraphTUtils.getAsWeightedGraph(auxGraph, linkWeight);
	return JGraphTUtils.isWeightedBidirectional(graph);
    }
    
    /**
     * Indicates whether routing has loops (a node is visited more than once).
     * 
     * @param netPlan Network design
     * @param checkCycles Indicates whether (and how) or not to check if there are cycles
     * @return <code>true</code> if there are routing loops, and <code>false</code> otherwise. By convention, it returns <code>false</code> for designs without routing.
     * @since 0.2.3
     */
    public static boolean hasRoutingLoops(NetPlan netPlan, CheckRoutingCycleType checkCycles)
    {
        if (checkCycles == CheckRoutingCycleType.NO_CHECK) throw new Net2PlanException("An option to check cycles must be indicated");
        
        int R = netPlan.getNumberOfRoutes();
        for(int routeId = 0; routeId < R; routeId++)
        {
            switch(checkCycles)
            {
                case NO_REPEAT_LINK:
                    int[] seqLinks = netPlan.getRouteSequenceOfLinks(routeId);
                    if (IntUtils.unique(seqLinks).length != seqLinks.length) return true;
                    break;
                    
                case NO_REPEAT_NODE:
                    int[] seqNodes = netPlan.getRouteSequenceOfNodes(routeId);
                    if (IntUtils.unique(seqNodes).length != seqNodes.length) return true;
                    break;
                    
                default:
                    throw new RuntimeException("Bad");
            }
            
        }
        
        return false;
    }
    
    /**
     * Checks whether the physical topology has the same number of links between each node pair in both directions (assuming multi-digraphs) and same weights per direction.
     *
     * @param netPlan A network plan
     * @param linkWeight Link weight vector
     * @return <code>true</code> if the physical topology is weighted-bidirectional, and false otherwise
     * @since 0.2.0
     */
    public static boolean isWeightedBidirectional(NetPlan netPlan, double[] linkWeight)
    {
        return isWeightedBidirectional(netPlan.getLinkTable(), linkWeight, netPlan.getNumberOfNodes());
    }

    /**
    * Check whether the physical topology is connected, that is, if it is possible to connect every node to each other.
    *
    * @param netPlan A network plan
    * @return <code>true</code> if the physical topology is connected, and false otherwise
    * @since 0.2.0
    */
    public static boolean isConnected(NetPlan netPlan)
    {
	org.jgrapht.Graph<Integer, Integer> graph = JGraphTUtils.getPhysicalLayerGraph(netPlan);
	return JGraphTUtils.isConnected(graph);
    }

    /**
    * Check whether the physical topology is connected, that is, if it is possible to connect every node to each other, but only in a subset of nodes (subgraph).
    *
    * @param netPlan A network plan
    * @param nodes Vector of nodes
    * @return <code>true</code> if the subgraph is connected, and false otherwise
    * @since 0.2.0
    */
    public static boolean isConnected(NetPlan netPlan, int[] nodes)
    {
	org.jgrapht.Graph<Integer, Integer> graph = JGraphTUtils.getPhysicalLayerGraph(netPlan);
	return JGraphTUtils.isConnected(graph, new HashSet<Integer>(IntUtils.toList(nodes)));
    }

    /**
    * Check whether the physical topology is simple, that is, if it has at most one unidirectional link from a node to each other.
    *
    * @param netPlan A network plan
    * @return <code>true</code> if the physical topology is simple, and false otherwise
    * @since 0.2.0
    */
    public static boolean isSimple(NetPlan netPlan)
    {
	DoubleMatrixND A_nn = getNodeAdjacencyMatrix(netPlan);
	return A_nn.getMaxLocation()[0] <= 1 ? true : false;
    }

    /**
     * Obtains the sequence of links representing the (unidirectional) shortest path between two nodes.
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param originNodeId      Origin node
     * @param destinationNodeId Destination node
     * @param costVector Link weights
     * @return Sequence of links in the shortest path (empty, if destination not reachable from origin)
     * @since 0.2.0
     */
    public static int[] getShortestPath(int[][] linkTable, int originNodeId, int destinationNodeId, final double[] costVector)
    {
	int N = IntUtils.maxValue(linkTable);
	N = Math.max(N, Math.max(originNodeId, destinationNodeId));

	Graph<Integer, Integer> graph = JUNGUtils.getGraphFromLinkTable(linkTable, N);
	final Transformer<Integer, Double> nev = JUNGUtils.getEdgeWeightTransformer(costVector);
	EdgePredicateFilter<Integer, Integer> linkFilter = new EdgePredicateFilter<Integer, Integer>(new Predicate<Integer>()
	    {
		@Override
		public boolean evaluate(Integer linkId)
		{
		    return (nev.transform(linkId) == Double.MAX_VALUE) ? false : true;
		}
	    });
	graph = linkFilter.transform(graph);

	if (!graph.containsVertex(originNodeId) || !graph.containsVertex(destinationNodeId))
	    return new int[0];


	DijkstraShortestPath<Integer, Integer> dsp = new DijkstraShortestPath<Integer, Integer>(graph, nev);
	List<Integer> path = dsp.getPath(originNodeId, destinationNodeId);
	return IntUtils.toArray(path);
    }

    /**
    * <p>Auxiliary class to work with the graph library <a href='GraphUtils.html#jgrapht'>JGraphT</a>.</p>
    *
    * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
    * @since 0.2.0
    */
    public static class JGraphTUtils
    {
	/**
	 * Obtains a <code>JGraphT</code> graph from a given link table.
	 *
         * @param linkTable Set of installed links (first column: origin node, second column: destination node)
	 * @param N         Number of nodes
	 * @return <code>JGraphT</code> graph
	 * @since 0.2.0
	 */
	public static org.jgrapht.Graph<Integer, Integer> getGraphFromLinkTable(int[][] linkTable, int N)
	{
	    int E = linkTable.length;

	    org.jgrapht.Graph<Integer, Integer> graph = new DirectedWeightedMultigraph<Integer, Integer>(Integer.class);

	    for(int nodeId = 0; nodeId < N; nodeId++)
		graph.addVertex(nodeId);

	    for(int linkId = 0; linkId < E; linkId++)
		graph.addEdge(linkTable[linkId][0], linkTable[linkId][1], linkId);

	    return graph;
	}

	/**
	* <p>It generates a weighted view of the backing graph specified in the constructor. This graph allows modules to apply algorithms designed for weighted graphs to an unweighted graph by providing an explicit edge weight mapping.</p>
	*
	* <p>Query operations on this graph "read through" to the backing graph. Vertex addition/removal and edge addition/removal are supported.</p>
	*
	* @param graph The backing graph over which a weighted view is to be created
	* @param edgeWeightVector A mapping of edges to weights
	* @return Returns a weighted view of the backing graph specified in the constructor
	* @since 0.2.0
	*/
	public static org.jgrapht.Graph<Integer, Integer> getAsWeightedGraph(org.jgrapht.Graph<Integer, Integer> graph, double[] edgeWeightVector)
	{
	    if (graph.edgeSet().size() != edgeWeightVector.length)
	    {
		throw new Net2PlanException("Number of edges in graph and edge weight vector length don't match");
	    }

	    Map<Integer, Double> edgeWeightMap = new HashMap<Integer, Double>();
	    for (int edgeId = 0; edgeId < edgeWeightVector.length; edgeId++)
	    {
		edgeWeightMap.put(edgeId, edgeWeightVector[edgeId]);
	    }

	    return new AsWeightedGraph<Integer, Integer>(graph, edgeWeightMap);
	}

	/**
	* Returns a graph representing the traffic demands (nodes and links) of the network plan.
	*
	* @param netPlan A network plan
	* @return A graph representing the traffic demands of the network plan
	* @since 0.2.0
	*/
	public static org.jgrapht.Graph<Integer, Integer> getDemandLayerGraph(NetPlan netPlan)
	{
	    org.jgrapht.Graph<Integer, Integer> graph = new DirectedWeightedMultigraph<Integer, Integer>(Integer.class);

	    int N = netPlan.getNumberOfNodes();
	    int D = netPlan.getNumberOfDemands();

	    int[][] demandTable = netPlan.getDemandTable();

	    for (int nodeId = 0; nodeId < N; nodeId++) graph.addVertex(nodeId);

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		int ingressNodeId = demandTable[demandId][0];
		int egressNodeId = demandTable[demandId][1];
		graph.addEdge(ingressNodeId, egressNodeId, demandId);
	    }

	    return graph;
	}

	/**
	* Returns a graph representing the physical topology (nodes and links) of the network plan.
	*
	* @param netPlan A network plan
	* @return A graph representing the physical topology of the network plan
	* @since 0.2.0
	*/
	public static org.jgrapht.Graph<Integer, Integer> getPhysicalLayerGraph(NetPlan netPlan)
	{
	    org.jgrapht.Graph<Integer, Integer> graph = new DirectedWeightedMultigraph<Integer, Integer>(Integer.class);

	    int N = netPlan.getNumberOfNodes();
	    int E = netPlan.getNumberOfLinks();

	    int[][] linkTable = netPlan.getLinkTable();

	    for (int nodeId = 0; nodeId < N; nodeId++) graph.addVertex(nodeId);

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int originNodeId = linkTable[linkId][0];
		int destinationNodeId = linkTable[linkId][1];
		graph.addEdge(originNodeId, destinationNodeId, linkId);
	    }

	    return graph;
	}

	/**
	* Check whether the topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
	*
	* @param graph The graph to analyze
	* @return <code>true</code> if the graph is bidirectional, and false otherwise
	* @since 0.2.0
	*/
	public static boolean isBidirectional(org.jgrapht.Graph graph)
	{
	    Object[] vertices = graph.vertexSet().toArray();

	    for(int vertexId_1 = 0; vertexId_1 < vertices.length; vertexId_1++)
		for(int vertexId_2 = vertexId_1 + 1; vertexId_2 < vertices.length; vertexId_2++)
		    if (graph.getAllEdges(vertices[vertexId_1], vertices[vertexId_2]).size() != graph.getAllEdges(vertices[vertexId_2], vertices[vertexId_1]).size()) return false;

	    return true;
	}

	/**
	* Checks whether the graph has the same number of links between each node pair in both directions (assuming multi-digraphs) and same individual weights per direction.
	*
	* @param graph The graph to analyze
	* @return <code>true</code> if the graph is weighted-bidirectional, and false otherwise. By convention returns <code>false</code> if network is empty 
	* @since 0.2.0
	*/
	public static boolean isWeightedBidirectional(org.jgrapht.Graph graph)
	{
	    Object[] vertices = graph.vertexSet().toArray();
            if (vertices.length == 0) return false;

	    for(int vertexId_1 = 0; vertexId_1 < vertices.length; vertexId_1++)
	    {
		for(int vertexId_2 = vertexId_1 + 1; vertexId_2 < vertices.length; vertexId_2++)
		{
		    Set links_12 = graph.getAllEdges(vertices[vertexId_1], vertices[vertexId_2]);
		    Set links_21 = graph.getAllEdges(vertices[vertexId_2], vertices[vertexId_1]);

		    if (links_12.size() != links_21.size()) return false;

		    Iterator it_12 = links_12.iterator();
		    while(it_12.hasNext())
		    {
			Object aux_12 = it_12.next();

			Iterator it_21 = links_21.iterator();
			while(it_21.hasNext())
			{
			    Object aux_21 = it_21.next();

			    if (graph.getEdgeWeight(aux_12) == graph.getEdgeWeight(aux_21))
			    {
				it_12.remove();
				it_21.remove();
				break;
			    }
			}
		    }

		    if (!links_12.isEmpty() || !links_12.isEmpty()) return false;
		}
	    }

	    return true;
	}

	/**
	* Check whether the graph is connected, that is, if it is possible to connect every node to each other.
	*
	* @param graph The graph to analyze
	* @return <code>true</code> if the graph is connected, and false otherwise
	* @since 0.2.0
	*/
	public static boolean isConnected(org.jgrapht.Graph graph)
	{
	    if (graph instanceof DirectedGraph)
	    {
		StrongConnectivityInspector ci = new StrongConnectivityInspector((DirectedGraph) graph);
		return ci.isStronglyConnected();
	    }
	    else if (graph instanceof UndirectedGraph)
	    {
		ConnectivityInspector ci = new ConnectivityInspector((UndirectedGraph) graph);
		return ci.isGraphConnected();
	    }

	    throw new RuntimeException("Bad");
	}

	/**
	* Check whether the graph is connected, that is, if it is possible to connect every node to each other, but only in a subset of vertices (subgraph).
	*
	* @param graph The graph to analyze
	* @param vertices Subset of vertices
	* @return <code>true</code> if the subgraph is connected, and false otherwise
	* @since 0.2.0
	*/
	public static boolean isConnected(org.jgrapht.Graph graph, Set vertices)
	{
	    Subgraph subgraph = new Subgraph(graph, vertices, null);
	    return isConnected(subgraph);
	}

	/**
	* Check whether the graph is simple, that is, if it has at most one link between each node pair (one per direction under directed graphs, one under undirected graphs).
	*
	* @param graph The graph to analyze
	* @return <code>true</code> if the graph is simple, and false otherwise
	* @since 0.2.0
	*/
	public static boolean isSimple(org.jgrapht.Graph graph)
	{
	    Set vertexSet = graph.vertexSet();
	    Iterator it1 = vertexSet.iterator();
	    while(it1.hasNext())
	    {
		Object aux1 = it1.next();
		Iterator it2 = vertexSet.iterator();
		while(it2.hasNext())
		{
		    Object aux2 = it2.next();
		    if (aux2.equals(aux1)) continue;

		    if (graph.getAllEdges(aux1, aux2).size() > 1) return false;
		}
	    }

	    return true;
	}
    }

    /**
    * <p>Auxiliary class to work with the graph library <a href='GraphUtils.html#jung'>JUNG</a>.</p>
    *
    * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
    * @since 0.2.0
    */
    public static class JUNGUtils
    {
	/**
	 * Obtains a <code>JUNG</code> graph from a given link table.
	 *
         * @param linkTable Set of installed links (first column: origin node, second column: destination node)
	 * @param N         Number of nodes
	 * @return <code>JUNG</code> graph
	 * @since 0.2.0
	 */
	public static Graph<Integer, Integer> getGraphFromLinkTable(int[][] linkTable, int N)
	{
	    int E = linkTable.length;

	    Graph<Integer, Integer> graph = new DirectedOrderedSparseMultigraph<Integer, Integer>();

	    for(int nodeId = 0; nodeId < N; nodeId++)
		graph.addVertex(nodeId);

	    for(int linkId = 0; linkId < E; linkId++)
		graph.addEdge(linkId, linkTable[linkId][0], linkTable[linkId][1]);

	    return graph;
	}

	/**
	 * Returns the weight of a path given the sequence of edges.
	 *
         * @param <E> Class type for edges
         * @param path Sequence of edge
	 * @param edgeWeightTransformer The class responsible for returning weights for edges
	 * @return Path weight
	 * @since 0.2.0
	 */
	public static<E> double getPathWeight(List<E> path, Transformer<E, Double> edgeWeightTransformer)
	{
	    double pathWeight = 0;
	    for (E edge : path) pathWeight += edgeWeightTransformer.transform(edge).doubleValue();
	    return pathWeight;
	}

	/**
	 * Obtains a transformer for returning link weight from link identifier
	 *
	 * @param edgeWeightVector Link weights
	 * @return A transformer for returning weights for edges
	 * @since 0.2.0
	 */
	public static Transformer<Integer, Double> getEdgeWeightTransformer(final double[] edgeWeightVector)
	{
	    return new Transformer<Integer, Double>()
	    {
		@Override
		public Double transform(Integer i)
		{
		    return edgeWeightVector[i];
		}
	    };
	}

	/**
	 * Obtains a transformer for returning link weight from link identifier
	 *
	 * @param edgeWeightVector Link weights
	 * @return A transformer for returning weights for edges
	 * @since 0.2.0
	 */
	public static Transformer<Integer, Integer> getEdgeWeightTransformer(final int[] edgeWeightVector)
	{
	    return new Transformer<Integer, Integer>()
	    {
		@Override
		public Integer transform(Integer i)
		{
		    return edgeWeightVector[i];
		}
	    };
	}

	/**
	 * Returns a graph representing the traffic demands (nodes and links) of the network plan.
	 *
	 * @param netPlan A network plan
	 * @return A graph representing the traffic demands of the network plan
	 * @since 0.2.0
	 */
	public static Graph<Integer, Integer> getDemandLayerGraph(NetPlan netPlan)
	{
	    Graph<Integer, Integer> graph = new DirectedOrderedSparseMultigraph<Integer, Integer>();

	    int N = netPlan.getNumberOfNodes();
	    int D = netPlan.getNumberOfDemands();

	    int[][] demandTable = netPlan.getDemandTable();

	    for (int nodeId = 0; nodeId < N; nodeId++)
	    {
		graph.addVertex(nodeId);
	    }

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		int ingressNodeId = demandTable[demandId][0];
		int egressNodeId = demandTable[demandId][1];
		graph.addEdge(demandId, ingressNodeId, egressNodeId);
	    }

	    return graph;
	}

	/**
	 * Returns a graph representing the physical topology (nodes and links) of the network plan.
	 *
	 * @param netPlan A network plan
	 * @return A graph representing the physical topology of the network plan
	 * @since 0.2.0
	 */
	public static Graph<Integer, Integer> getPhysicalLayerGraph(NetPlan netPlan)
	{
	    Graph<Integer, Integer> graph = new DirectedOrderedSparseMultigraph<Integer, Integer>();

    	    int N = netPlan.getNumberOfNodes();
	    int E = netPlan.getNumberOfLinks();

	    int[][] linkTable = netPlan.getLinkTable();

	    for (int nodeId = 0; nodeId < N; nodeId++)
	    {
		graph.addVertex(nodeId);
	    }

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int originNodeId = linkTable[linkId][0];
		int destinationNodeId = linkTable[linkId][1];
		graph.addEdge(linkId, originNodeId, destinationNodeId);
	    }

	    return graph;
	}

	/**
         * <p>Class to represent a path in a Graph. Note that a path is defined in terms of edges (rather than vertices) so that multiple edges between the same pair of vertices can be discriminated.</p>
         *
         * <p>It implements the <code>{@link java.lang.Comparable Comparable}</code> interface to impose order between different paths. First, try to order using the path weight, and if equals, using the number of hops.</p>
         *
         * @param <E> Class type for edges
         * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
         * @since 0.2.0
         */
	public static class GraphPath<E> implements Comparable<GraphPath>
	{
	    private final List<E> path;
	    private final double pathWeight;

	    /**
             * Default constructor.
             * 
             * @param path Sequence of links
             * @param pathWeight Path weight
             * @since 0.2.0
             */
	    public GraphPath(List<E> path, double pathWeight)
	    {
		this.path = path;
		this.pathWeight = pathWeight;
	    }

	    /**
             * Returns the edges making up the path.
             *
             * @return An unmodifiable list with the sequence of edges followed by the path
             * @since 0.2.0
             */
	    public List<E> getPath()
	    {
		return Collections.unmodifiableList(path);
	    }

	    /**
             * Returns the path length measured in number of hops or edges. It is equivalent to <code>{@link #getPath getPath()}.size()</code>.
             *
             * @return The path length
             * @since 0.2.0
             */
	    public int getPathLength()
	    {
		return getPath().size();
	    }

	    /**
             * Returns the weight assigned to the path.
             *
             * @return The weight assigned to the path
             * @since 0.2.0
             */
	    public double getPathWeight()
	    {
		return pathWeight;
	    }

            /**
             * Indicates whether some other object is "equal to" this one.
             *
             * @param o Reference object with which to compare
             * @return <code>true</code> if this object is the same as the <code>o</code> argument; <code>false</code> otherwise
             * @since 0.2.0
             */
	    @Override
	    public boolean equals(Object o)
	    {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof GraphPath)) return false;

		GraphPath p = (GraphPath) o;
		if (getPath().equals(p.getPath()) && getPathWeight() == p.getPathWeight()) return true;

		return false;
	    }

            /**
             * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by <code>HashMap</code>.
             *
             * @return Hash code value for this object
             * @since 0.2.0
             */
	    @Override
	    public int hashCode()
	    {
		int hash = 7;
		hash = 37 * hash + (this.path != null ? this.path.hashCode() : 0);
		hash = 37 * hash + (int) (Double.doubleToLongBits(this.pathWeight) ^ (Double.doubleToLongBits(this.pathWeight) >>> 32));
		return hash;
	    }
            
            /**
             * Compares this object with the specified object for order.
             *
             * @param o The object to be compared
             * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
             * @since 0.2.0
             */
	    @Override
	    public int compareTo(GraphPath o)
	    {
		if (pathWeight < o.pathWeight) return -1;
		if (pathWeight > o.pathWeight) return 1;

		return path.size() == o.path.size() ? 0 : (path.size() > o.path.size() ? -1 : 1);
	    }
	}
    }
}