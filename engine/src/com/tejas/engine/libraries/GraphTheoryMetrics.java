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

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleEigenvalueDecomposition;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.DoubleMatrixND;
import com.net2plan.libraries.GraphUtils;
import com.tejas.engine.libraries.GraphUtils.JGraphTUtils;
import com.tejas.engine.libraries.GraphUtils.JUNGUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;

import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import java.util.*;
import org.apache.commons.collections15.Transformer;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.EdmondsKarpMaximumFlow;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DirectedWeightedMultigraph;

/**
 * <p>Class to deal with graph-theory metrics computation.</p>
 *
 * <p><b>Important</b>: Internal computations (like shortest-paths) are cached in order to improve efficiency.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class GraphTheoryMetrics
{
    private final double[] costVector;
    private final int N;
    private final int E;
    private final int[][] linkTable;

    private DoubleMatrixND adjacencyMatrix;
    private double[] adjacencyMatrixEigenvalues;
    private List<int[]> allPairsShortestPaths;
    private double[] allPairsShortestPathLengths;
    private DirectedGraph<Integer, Integer> graph_jgrapht;
    private Graph<Integer, Integer> graph_jung;
    private DoubleMatrixND incidenceMatrix;
    private DoubleMatrixND laplacianMatrix;
    private double[] laplacianMatrixEigenvalues;
    private Transformer<Integer, Double> nev;
    private int[] outNodeDegree;

    /**
     * Default constructor.
     *
     * @param linkTable Set of installed links (first column: origin node, second column: destination node)
     * @param N Number of nodes in the network
     * @param costVector Link weights
     * @since 0.2.0
     */
    public GraphTheoryMetrics(int[][] linkTable, int N, double[] costVector)
    {
	this.N = N;
	this.E = linkTable.length;

	if (costVector == null) costVector = DoubleUtils.ones(E);

	this.costVector = DoubleUtils.copy(costVector);

	this.linkTable = new int[E][];
	for(int linkId = 0; linkId < E; linkId++)
	    this.linkTable[linkId] = IntUtils.copy(linkTable[linkId]);

	adjacencyMatrix = null;
	adjacencyMatrixEigenvalues = null;
	allPairsShortestPaths = null;
	allPairsShortestPathLengths = null;
	graph_jgrapht = null;
	graph_jung = null;
	incidenceMatrix = null;
	laplacianMatrix = null;
	laplacianMatrixEigenvalues = null;
	nev = null;
	outNodeDegree = null;
    }

    /**
     * Returns the adjacency matrix of the network. The adjacency matrix is a
     * <i>NxN</i> matrix (where <i>N</i> is the number of nodes in the network),
     * where each position (<i>i</i>,<i>j</i>) represents the number of directed
     * links from <i>i</i> to <i>j</i>.
     *
     * @return Adjacency matrix
     * @since 0.2.0
     */
    private DoubleMatrixND getAdjacencyMatrix()
    {
	if (adjacencyMatrix == null)
	{
	    adjacencyMatrix = new DoubleMatrixND(new int[] {N, N}, "sparse");
	    for(int linkId = 0; linkId < E; linkId++)
		adjacencyMatrix.setQuick(linkTable[linkId], adjacencyMatrix.get(linkTable[linkId]) + 1);
	}

	return adjacencyMatrix;
    }

    /**
     * Returns the eigenvalues of the adjacency matrix.
     *
     * @return Eigenvalues of the adjacency matrix
     * @since 0.2.0
     */
    private double[] getAdjacencyMatrixEigenvalues()
    {
	if (adjacencyMatrixEigenvalues == null)
	{
	    DoubleMatrix2D A_nn = getAdjacencyMatrix().view2D();
	    A_nn.copy().assign(A_nn.viewDice(), DoubleFunctions.max);

	    DenseDoubleAlgebra alg = new DenseDoubleAlgebra();
	    DenseDoubleEigenvalueDecomposition eig = alg.eig(A_nn);

	    adjacencyMatrixEigenvalues = eig.getRealEigenvalues().toArray();
	    DoubleUtils.sort(adjacencyMatrixEigenvalues, Constants.OrderingType.ASCENDING);
	}

	return adjacencyMatrixEigenvalues;
    }

    /**
     * <p>Returns the algebraic connectivity of the network. The algebraic connectivity
     * is equal to the second smallest eigenvalue of the laplacian matrix.</p>
     *
     * <p>For symmetric (or undirected) networks, if the algebraic connectivity
     * is different from zero, it is ensured that the network is connected, that is,
     * it is possible to find a path between each node pair.
     *
     * @return Algebraic connectivity
     * @since 0.2.0
     */
    public double getAlgebraicConnectivity()
    {
	double[] eig = getLaplacianMatrixEigenvalues();
	return eig[1];
    }

    private List<int[]> getAllPairsShortestPaths()
    {
	if (allPairsShortestPaths == null)
	{
	    allPairsShortestPaths = new ArrayList<int[]>();
	    Graph<Integer, Integer> aux_graph = getGraph_JUNG();

	    Transformer<Integer, Double> aux_nev = getCostTransformer();
	    DijkstraShortestPath<Integer, Integer> dsp = new DijkstraShortestPath<Integer, Integer>(aux_graph, aux_nev);

	    for(int originNodeId = 0; originNodeId < N; originNodeId++)
	    {
		for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
		{
		    if (originNodeId == destinationNodeId) continue;

		    Number dist = dsp.getDistance(originNodeId, destinationNodeId);
		    if (dist == null) continue;

		    allPairsShortestPaths.add(IntUtils.toArray(dsp.getPath(originNodeId, destinationNodeId)));
		}
	    }

	    allPairsShortestPathLengths = GraphUtils.convertPathList2PathCost(allPairsShortestPaths, costVector);
	}

	return allPairsShortestPaths;
    }

    private double[] getAllPairsShortestPathLengths()
    {
	if (allPairsShortestPathLengths	== null)
	    getAllPairsShortestPaths();

	return allPairsShortestPathLengths;
    }

    /**
     * Returns the assortativity of the network.
     *
     * @return Assortativity
     * @since 0.2.0
     */
    public double getAssortativity()
    {
	if (E == 0) return 0;

	int[] aux_outNodeDegree = getOutNodeDegree();

	double a = 0;
	double b = 0;
	double y = 0;

	for(int linkId = 0; linkId < E; linkId++)
	{
	    int j_e = aux_outNodeDegree[linkTable[linkId][0]];
	    int k_e = aux_outNodeDegree[linkTable[linkId][1]];

	    y += j_e + k_e;
	    a += j_e * k_e;
	    b += j_e * j_e + k_e * k_e;
	}

	y /= 2.0D * E;
	y *= y;
	a /= E;
	b /= 2.0D * E;

	return (a - y) / (b - y);
    }

    /**
     * Returns the average neighbor connectivity.
     *
     * @return Average neighbor connectivity
     * @since 0.2.0
     */
    public double getAverageNeighborConnectivity()
    {
	if (E == 0) return 0;

	int[] aux_outNodeDegree = getOutNodeDegree();

	int maxNodeDegree = IntUtils.maxValue(aux_outNodeDegree);

	double[] knn = new double[maxNodeDegree+1];
	DoubleMatrix2D m = DoubleFactory2D.sparse.make(maxNodeDegree + 1, maxNodeDegree + 1);

	for(int linkId = 0; linkId < E; linkId++)
	{
	    int degree_k1 = aux_outNodeDegree[linkTable[linkId][0]];
	    int degree_k2 = aux_outNodeDegree[linkTable[linkId][1]];

	    m.set(degree_k1, degree_k2, m.get(degree_k1, degree_k2) + 1);
	}

	for(int k_1 = 1; k_1 <= maxNodeDegree; k_1++)
	    knn[k_1] = k_1 * m.viewRow(k_1).zSum();

	return DoubleUtils.averageNonZeros(knn) / (E-1);
    }

    /**
     * Returns the average number of outgoing links per node.
     *
     * @return Average number of outgoing links per node
     * @since 0.2.0
     */
    public double getAverageOutNodeDegree()
    {
	return IntUtils.average(getOutNodeDegree());
    }

    /**
     * Returns the average shortest path distance among all node-pair shortest paths.
     *
     * @return Average shortest path distance
     * @since 0.2.0
     */
    public double getAverageShortestPathDistance()
    {
	List<int[]> allPairsSPs = getAllPairsShortestPaths();
	int numPaths = allPairsSPs.size();

	if (numPaths == 0) return 0;

	double[] pathCost = getAllPairsShortestPathLengths();
	return DoubleUtils.average(pathCost);
    }

    /**
     * Returns the average two-term reliability (A2TR) of the network. A2TR is computed
     * as the ratio between the number of node-pair for which a path can be found
     * and the same number when the network is connected (<i>Nx(N-1)</i>, where
     * <i>N</i> is the number of nodes in the network). The value is in range [0, 1].
     *
     * @return Average two-term reliability
     * @since 0.2.0
     */
    public double getAverageTwoTermReliability()
    {
	if (E == 0) return 0;

	DirectedGraph<Integer, Integer> graph = getGraph_JGraphT();
	StrongConnectivityInspector<Integer, Integer> ci = new StrongConnectivityInspector<Integer, Integer>(graph);
	List<Set<Integer>> connectedComponents = ci.stronglyConnectedSets();

	double sum = 0;
	Iterator<Set<Integer>> it = connectedComponents.iterator();
	while(it.hasNext())
	{
	    int componentSize = it.next().size();
	    sum += componentSize * (componentSize - 1);
	}

	return sum / (N * (N - 1));
    }

    /**
     * <p>Returns the betweeness centrality for nodes and links. The betweeness
     * centrality is equal to the number of node-pair shortest paths which pass
     * through a network element (node or link).</p>
     *
     * <p>Internally it makes use of the Brandes' algorithm.</p>
     *
     * @return Betweeness centrality (<code>output[0]</code> is equal to the betweeness
     * centrality per node, <code>output[1]</code> is that for links)
     * @since 0.2.0
     */
    public double[][] getBetweenessCentrality()
    {
	if (E == 0) return new double[2][0];

	BetweennessCentrality bc = new BetweennessCentrality(getGraph_JUNG(), getCostTransformer());

	double[][] aux = new double[2][];
	aux[0] = new double[N];
	aux[1] = new double[E];

	for(int nodeId = 0; nodeId < N; nodeId++)
	    aux[0][nodeId] = bc.getVertexScore(nodeId);

	for(int linkId = 0; linkId < E; linkId++)
	    aux[1][linkId] = bc.getEdgeScore(linkId);

	return aux;
    }

    /**
     * Returns the clustering coefficient of the network.
     *
     * @return Clustering coefficient
     * @since 0.2.0
     */
    public double getClusteringCoefficient()
    {
	if (E == 0) return 0;

	int[] aux_outNodeDegree = getOutNodeDegree();

	double[] clusteringCoefficient = new double[N];
	for(int nodeId = 0; nodeId < N; nodeId++)
	{
	    switch(aux_outNodeDegree[nodeId])
	    {
		case 0:
		    break;

		case 1:
		    clusteringCoefficient[nodeId] = 1;
		    break;

		default:
		    int[] neighbors = getNeighbors(nodeId);

		    int aux = 0;
		    for(int i : neighbors)
		    {
			int[] aux_neighbors = getNeighbors(i);

			for(int j : neighbors)
			{
			    if (i == j) continue;

			    if (IntUtils.contains(aux_neighbors, j))
				aux++;
			}
		    }

		    clusteringCoefficient[nodeId] = (double) aux / neighbors.length;
		    break;
	    }
	}

	return DoubleUtils.average(clusteringCoefficient);
    }

    private Transformer<Integer, Double> getCostTransformer()
    {
	if (nev == null)
	    nev = JUNGUtils.getEdgeWeightTransformer(costVector);

	return nev;
    }

    /**
     * Returns the density of the network. The density represents the ratio
     * between the number of links in the network and the number of links needed
     * to build a full-mesh network (<i>Nx(N-1)</i>, where <i>N</i> is the number of
     * nodes in the network).
     *
     * @return Density
     * @since 0.2.0
     */
    public double getDensity()
    {
	if (N == 0) return 0;

	return (double) E / (N*(N-1));
    }

    /**
     * Returns the diameter of the network. The diameter is the longest path distance
     * among all node-pair shortest paths.
     *
     * @return Network diameter
     * @since 0.2.0
     */
    public double getDiameter()
    {
	List<int[]> allPairsSPs = getAllPairsShortestPaths();
	int numPaths = allPairsSPs.size();

	if (numPaths == 0) return 0;

	double[] pathCost = getAllPairsShortestPathLengths();
	return DoubleUtils.maxValue(pathCost);
    }

    private DirectedGraph<Integer, Integer> getGraph_JGraphT()
    {
	if (graph_jgrapht == null)
	    graph_jgrapht = (DirectedGraph<Integer, Integer>) JGraphTUtils.getGraphFromLinkTable(linkTable, N);

	return graph_jgrapht;
    }

    private Graph<Integer, Integer> getGraph_JUNG()
    {
	if (graph_jung == null)
	    graph_jung = JUNGUtils.getGraphFromLinkTable(linkTable, N);

	return graph_jung;
    }

    /**
     * Returns the heterogeneity of the network. The heterogeneity is equal to the
     * standard deviation of all node-pair shortest paths divided by the average
     * shortest path distance.
     *
     * @return Heterogeneity
     * @since 0.2.0
     */
    public double getHeterogeneity()
    {
	double[] pathCost = getAllPairsShortestPathLengths();
	if (pathCost.length == 0) return 0;

	return DoubleUtils.std(pathCost) / getAverageShortestPathDistance();
    }

    /**
     * Returns the incidence matrix of the network. The incidence matrix is a
     * <i>NxE</i> matrix (where <i>N</i> and <i>E</i> are the number of nodes
     * and links in the network, respectively), where each position (<i>i</i>,
     * <i>j</i>) is equal to: '1', if node <i>i</i> is the origin node of the link
     * <i>j</i>; '-1', if node <i>i</i> is the destination node of the link
     * <i>j</i>; and '0', otherwise.
     *
     * @return Incidence matrix
     * @since 0.2.0
     */
    private DoubleMatrixND getIncidenceMatrix()
    {
	if (incidenceMatrix == null)
	{
	    incidenceMatrix = new DoubleMatrixND(new int[] {N, E}, "sparse");
	    for(int linkId = 0; linkId < E; linkId++)
	    {
		incidenceMatrix.setQuick(new int[] {linkTable[linkId][0], linkId}, 1);
		incidenceMatrix.setQuick(new int[] {linkTable[linkId][1], linkId}, -1);
	    }
	}

	return incidenceMatrix;
    }

    /**
     * Returns the laplacian matrix of the network. The laplacian matrix is equal
     * to the product of the incidence matrix by its transpose matrix.
     *
     * @return Laplacian matrix
     * @since 0.2.0
     */
    private DoubleMatrixND getLaplacianMatrix()
    {
	if (laplacianMatrix == null)
	{
	    DoubleMatrix2D A_ne = getIncidenceMatrix().view2D();
	    laplacianMatrix = new DoubleMatrixND(A_ne.copy().zMult(A_ne.viewDice(), null));
	}

	return laplacianMatrix;
    }

    /**
     * Returns the eigenvalues of the laplacian matrix of the network.
     *
     * @return Eigenvalues of the laplacian matrix
     * @since 0.2.0
     */
    private double[] getLaplacianMatrixEigenvalues()
    {
	if (laplacianMatrixEigenvalues == null)
	{
	    DenseDoubleAlgebra alg = new DenseDoubleAlgebra();
	    DenseDoubleEigenvalueDecomposition eig = alg.eig(getLaplacianMatrix().view2D());

	    laplacianMatrixEigenvalues = eig.getRealEigenvalues().toArray();
	    DoubleUtils.sort(laplacianMatrixEigenvalues, Constants.OrderingType.ASCENDING);
	}

	return laplacianMatrixEigenvalues;
    }

    /**
     * <p>Returns the link connectivity. The link connectivity is equal to the smallest
     * number of link-disjoint paths between each node pair.</p>
     *
     * <p>Internally it makes use of the Edmonds-Karp algorithm to compute the maximum
     * flow between each node pair, assuming a link capacity equal to one for every link.</p>
     *
     * @return Link connectivity
     * @since 0.2.0
     */
    public int getLinkConnectivity()
    {
	if (E == 0) return 0;

	int k = Integer.MAX_VALUE;

	DirectedGraph<Integer, Integer> graph = getGraph_JGraphT();
	EdmondsKarpMaximumFlow<Integer, Integer> ek = new EdmondsKarpMaximumFlow(graph);

	for(int originNodeId = 0; originNodeId < N; originNodeId++)
	{
	    for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
	    {
		if (originNodeId == destinationNodeId) continue;

		ek.calculateMaximumFlow(originNodeId, destinationNodeId);
                k = Math.min(k, ek.getMaximumFlowValue().intValue());

                if (k == 0) break;
	    }
	}

	return k;
    }

    /**
     * Returns the set of nodes reachable from a given node.
     *
     * @param nodeId Node identifier
     * @return Set of reachable nodes
     * @since 0.2.0
     */
    public int[] getNeighbors(int nodeId)
    {
	return IntUtils.toArray(getGraph_JUNG().getSuccessors(nodeId));
    }

    /**
     * Returns the node connectivity. The node connectivity is equal to the smallest
     * number of node-disjoint paths between each node pair.
     *
     * <p>Internally it makes use of the (modified) Edmonds-Karp algorithm to compute the maximum
     * flow between each node pair, assuming a link capacity equal to one for every link.</p>
     *
     * @return Node connectivity
     * @since 0.2.0
     */
    public int getNodeConnectivity()
    {
	if (E == 0) return 0;

	int k = Integer.MAX_VALUE;

	DirectedGraph<Integer, Integer> graph = new DirectedWeightedMultigraph<Integer, Integer>(Integer.class);

	int newE = 0;

	for(int nodeId = 0; nodeId < N; nodeId++)
	{
	    graph.addVertex(nodeId);
	    graph.addVertex(-nodeId);
	    graph.addEdge(nodeId, -nodeId, newE++);
	}

	for(int linkId = 0; linkId < E; linkId++)
	    graph.addEdge(-linkTable[linkId][0], linkTable[linkId][1], newE++);

	EdmondsKarpMaximumFlow<Integer, Integer> ek = new EdmondsKarpMaximumFlow(graph);

	for(int originNodeId = 0; originNodeId < N; originNodeId++)
	{
	    for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
	    {
		if (originNodeId == destinationNodeId) continue;

		ek.calculateMaximumFlow(-originNodeId, destinationNodeId);

		int maxFlow = (int) ek.getMaximumFlowValue().doubleValue();

		if (maxFlow < k)
		    k = maxFlow;

		if (k == 0)
		    break;
	    }
	}

	return k;
    }

    /**
     * Returns the number of outgoing links for each node.
     *
     * @return Number of outgoing links per node
     * @since 0.2.0
     */
    public int[] getOutNodeDegree()
    {
	if (outNodeDegree == null)
	{
	    Graph<Integer, Integer> aux_graph = getGraph_JUNG();
	    outNodeDegree = new int[N];
	    for(int nodeId = 0; nodeId < N; nodeId++)
		outNodeDegree[nodeId] = aux_graph.outDegree(nodeId);
	}

	return IntUtils.copy(outNodeDegree);
    }

    /**
     * Returns the spectral radius of the network. The spectral radius is equal
     * to the largest eigenvalue of the adjacency matrix.
     *
     * @return Spectral radius
     * @since 0.2.0
     */
    public double getSpectralRadius()
    {
	if (E == 0) return 0;

	double[] eig = getAdjacencyMatrixEigenvalues();

	return eig[eig.length - 1];
    }

    /**
     * Returns the symmetry ratio. The symmetry ratio is equal to the number
     * of distinct eigenvalues of the adjacency matrix divided by the network
     * density plus one.
     *
     * @return Symmetry ratio
     * @since 0.2.0
     */
    public double getSymmetryRatio()
    {
	if (E == 0) return 0;

	double[] eig = getAdjacencyMatrixEigenvalues();

	return (double) DoubleUtils.unique(eig).length / (getDensity() + 1);
    }
}
