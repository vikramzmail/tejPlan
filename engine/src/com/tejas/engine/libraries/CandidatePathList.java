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
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.impl.DenseIntMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.DoubleMatrixND;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils.JUNGUtils;
import com.tejas.engine.libraries.GraphUtils.JUNGUtils.GraphPath;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import java.io.*;
import java.util.Map.Entry;
import java.util.*;
import org.apache.commons.collections15.ListUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

/**
 * <p>A candidate path list is an object containing a set of paths computed for
 * each demand in the network. This object is commonly used for solving
 * flow-path formulations. Each path is characterized by a demand and a sequence
 * of traversed links (route). If more than one demand exists between two nodes,
 * the same route appears in different paths, each for one demand. There are
 * several forms of initializing a candidate path list, based on the k-shortest
 * path idea. In general, for every demand k paths are computed with the
 * shortest weight according to some weights assigned to the links.</p>
 *
 * <p>The computation of paths can be configured via <code>"parameter=value"</code> options in the constructor. There are several options to
 * configure, which can be combined:</p>
 *
 * <ul>
 * <li><code>K</code>: Number of desired loopless shortest paths (default: 3). If <i>K'</i><<code>K</code> different paths are found between the demand node pairs, then only <i>K'</i> paths are included in the candidate path list</li>
 * <li><code>maxLengthInKm</code>: Maximum path length measured in kilometers allowed (default: Double.MAX_VALUE)</li>
 * <li><code>maxNumHops</code>: Maximum number of hops allowed (default: Integer.MAX_VALUE)</li>
 * <li><code>maxWeight</code>: Maximum path weight allowed (default: Double.MAX_VALUE)</li>
 * <li><code>maxWeightFactorRespectToShortestPath</code>: Maximum path weight factor with respect to the shortest path weight (default: Double.MAX_VALUE)</li>
 * <li><code>maxWeightRespectToShortestPath</code>: Maximum path weight with respect to the shortest path weight (default: Double.MAX_VALUE). While the previous one is a multiplicative factor, this one is an additive factor</li>
 * <li><code>weights</code>: Link weight vector (default: vector of 1s, which corresponds to a shortest path algorithm using number of hops as metric)</li>
 * </ul>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CandidatePathList
{
	private Map<Integer, IntArrayList> demand2PathMap;
	private List<List<Integer>> ksp;
	private Map<Integer, IntArrayList> link2PathMap;
	private NetPlan netPlan;
	private IntArrayList path2demand;
	private int N;
	private int E;
	private int D;
	private int K;
	private double maxLengthInKm;
	private int maxNumHops;
	private double maxWeight;
	private double maxWeightFactorRespectToShortestPath;
	private double maxWeightRespectToShortestPath;
	private double[] weights;

	/**
	 * Initializes the candidate path list, previously stored in a system file.</p>
	 *
	 * @param netPlan The network plan, containing at least the nodes, links and
	 * demands in the network. Routes already defined will be ommitted
	 * @param f File containing a previously computed <code>CandidatePathList</code>
	 * @since 0.2.0
	 */
	public CandidatePathList(NetPlan netPlan, File f)
	{
		N = netPlan.getNumberOfNodes();
		E = netPlan.getNumberOfLinks();
		D = netPlan.getNumberOfDemands();

		int[][] linkTable = netPlan.getLinkTable();

		this.netPlan = netPlan;

		Properties cpl = new Properties();

		try
		{
			try (InputStream in = new FileInputStream(f))
			{
				cpl.load(in);
			}
		}
		catch(Throwable e)
		{
			throw new RuntimeException(e);
		}

		int[] d_p = StringUtils.toIntArray(StringUtils.split(cpl.getProperty("d_p"), " "));
		weights = StringUtils.toDoubleArray(StringUtils.split(cpl.getProperty("weights"), " "));

		int P = d_p.length;

		path2demand = new IntArrayList(d_p);
		ksp = new ArrayList<List<Integer>>();
		demand2PathMap = new HashMap<Integer, IntArrayList>();
		link2PathMap = new HashMap<Integer, IntArrayList>();

		for(int pathId = 0; pathId < P; pathId++)
		{
			int demandId = d_p[pathId];

			if (!demand2PathMap.containsKey(demandId))
				demand2PathMap.put(demandId, new IntArrayList());

			demand2PathMap.get(demandId).add(pathId);

			int[] seqLinks = StringUtils.toIntArray(StringUtils.split(cpl.getProperty("path_" + pathId), " "));
			GraphUtils.checkRouteContinuity(linkTable, seqLinks, GraphUtils.CheckRoutingCycleType.NO_REPEAT_NODE);

			ksp.add(IntUtils.toList(seqLinks));

			for(int linkId : seqLinks)
			{
				if (!link2PathMap.containsKey(linkId))
					link2PathMap.put(linkId, new IntArrayList());

				link2PathMap.get(linkId).add(pathId);
			}
		}
	}

	/**
	 * Saves the current candidate path list to a given file for further usage.
	 *
	 * @param f Output file
	 */
	public void save(File f)
	{
		Properties cpl = new Properties();

		int[] d_p = getDemandIdsPerPath();
		int P = d_p.length;

		cpl.setProperty("d_p", IntUtils.join(d_p, " "));
		cpl.setProperty("weights", DoubleUtils.join(weights, " "));

		for(int pathId = 0; pathId < P; pathId++)
		{
			cpl.setProperty("path_" + pathId, IntUtils.join(getSequenceOfLinks(pathId), " "));
		}

		try
		{
			try (OutputStream out = new FileOutputStream(f))
			{
				cpl.store(out, "Candidate path list");
			}
		}
		catch(Throwable e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initializes the candidate path list, computing all the paths for each
	 * demand. <p>A candidate path list is an object containing a set of paths
	 * computed for each demand in the network. This object is commonly used for
	 * solving flow-path formulations. Each path is characterized by a demand
	 * and a sequence of traversed links (route). If more than one demand exists
	 * between two nodes, one path per demand is created, even though they follow the same route.
	 * There are several forms of initializing a candidate path
	 * list, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 *
	 * <p>Here, all link weights are initialized to 1, meaning that shortest path will be computed according to the number of hops.</p>
	 *
	 * @param netPlan The network plan, containing at least the nodes, links and
	 * demands in the network. Routes already defined will be ommitted
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of <code>String</code> is to be passed. For each <code>String</code> pair, first <code>String</code> must be the name of the parameter, second a <code>String</code> with its value. If no name-value pairs are set, default values are used
	 * @since 0.2.0
	 */
	public CandidatePathList(NetPlan netPlan, String... paramValuePairs)
	{
		this(netPlan, DoubleUtils.ones(netPlan.getNumberOfLinks()), paramValuePairs);
	}

	/**
	 * Initializes the candidate path list, computing all the paths for each
	 * demand. <p>A candidate path list is an object containing a set of paths
	 * computed for each demand in the network. This object is commonly used for
	 * solving flow-path formulations. Each path is characterized by a demand
	 * and a sequence of traversed links (route). If more than one demand exists
	 * between two nodes, one path per demand is created, even though they follow the same route.
	 * There are several forms of initializing a candidate path
	 * list, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 *
	 * @param netPlan The network plan, containing at least the nodes, links and
	 * demands in the network. Routes already defined will be ommitted
	 * @param weights Link weight vector for the shortest path algorithm
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of <code>String</code> is to be passed. For each <code>String</code> pair, first <code>String</code> must be the name of the parameter, second a <code>String</code> with its value. If no name-value pairs are set, default values are used
	 * @since 0.2.0
	 */
	public CandidatePathList(NetPlan netPlan, double[] weights, String... paramValuePairs)
	{
		this(netPlan, false, weights, paramValuePairs);
	}

	/**
	 * Initializes the candidate path list, computing all the paths for each
	 * demand. <p>A candidate path list is an object containing a set of paths
	 * computed for each demand in the network. This object is commonly used for
	 * solving flow-path formulations. Each path is characterized by a demand
	 * and a sequence of traversed links (route). If more than one demand exists
	 * between two nodes, one path per demand is created, even though they follow the same route.
	 * There are several forms of initializing a candidate path
	 * list, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 *
	 * <p>Here, all link weights are initialized to 1, meaning that shortest path will be computed according to the number of hops.</p>
	 *
	 * @param netPlan The network plan, containing at least the nodes, links and
	 * demands in the network.
	 * @param useRoutesWithinNetPlan A flag to indicate whether current routes
	 * in netPlan (if available) should be added as candidate paths
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of <code>String</code> is to be passed. For each <code>String</code> pair, first <code>String</code> must be the name of the parameter, second a <code>String</code> with its value. If no name-value pairs are set, default values are used
	 * @since 0.2.0
	 */
	public CandidatePathList(NetPlan netPlan, boolean useRoutesWithinNetPlan, String... paramValuePairs)
	{
		this(netPlan, useRoutesWithinNetPlan, DoubleUtils.ones(netPlan.getNumberOfLinks()), paramValuePairs);
	}

	/**
	 * Initializes the candidate path list, computing all the paths for each
	 * demand. <p>A candidate path list is an object containing a set of paths
	 * computed for each demand in the network. This object is commonly used for
	 * solving flow-path formulations. Each path is characterized by a demand
	 * and a sequence of traversed links (route). If more than one demand exists
	 * between two nodes, one path per demand is created, even though they follow the same route.
	 * There are several forms of initializing a candidate path
	 * list, based on the k-shortest path idea. In general, for every demand k
	 * paths are computed with the shortest weight according to some weights
	 * assigned to the links.</p>
	 *
	 * @param netPlan The network plan, containing at least the nodes, links and
	 * demands in the network.
	 * @param useRoutesWithinNetPlan A flag to indicate whether current routes
	 * in netPlan (if available) should be added as candidate paths
	 * @param weights Link weight vector for the shortest path algorithm
	 * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of <code>String</code> is to be passed. For each <code>String</code> pair, first <code>String</code> must be the name of the parameter, second a <code>String</code> with its value. If no name-value pairs are set, default values are used
	 * @since 0.2.0
	 */
	public CandidatePathList(final NetPlan netPlan, boolean useRoutesWithinNetPlan, double[] weights, String... paramValuePairs)
	{
		N = netPlan.getNumberOfNodes();
		E = netPlan.getNumberOfLinks();
		D = netPlan.getNumberOfDemands();

		demand2PathMap = new HashMap<Integer, IntArrayList>();
		link2PathMap = new HashMap<Integer, IntArrayList>();
		ksp = new ArrayList<List<Integer>>();
		path2demand = new IntArrayList();
		this.netPlan = netPlan;

		K = 3;
		maxLengthInKm = Double.MAX_VALUE;
		maxNumHops = Integer.MAX_VALUE;
		maxWeight = Double.MAX_VALUE;
		maxWeightFactorRespectToShortestPath = Double.MAX_VALUE;
		maxWeightRespectToShortestPath = Double.MAX_VALUE;
		this.weights = DoubleUtils.copy(weights);

		if (useRoutesWithinNetPlan)
		{
			int R = netPlan.getNumberOfRoutes();

			for (int routeId = 0; routeId < R; routeId++)
			{
				addPath(netPlan.getRouteDemand(routeId), IntUtils.toList(netPlan.getRouteSequenceOfLinks(routeId)));
			}
		}

		int numParameters = (int) (paramValuePairs.length / 2);
		if ( (((double) paramValuePairs.length) / 2) != (double) numParameters) throw new RuntimeException ("A parameter has not assigned its value");
		for (int contParam = 0 ; contParam < numParameters ; contParam ++)
		{
			String parameter = paramValuePairs[contParam * 2];
			String value = paramValuePairs[contParam * 2 + 1];

			if (parameter.equalsIgnoreCase("K"))
			{
				K = Integer.parseInt(value);
				if (K <= 0) throw new RuntimeException("'K' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxLengthInKm"))
			{
				maxLengthInKm = Double.parseDouble(value);
				if (maxLengthInKm <= 0) throw new RuntimeException("'maxLengthInKm' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxNumHops"))
			{
				maxNumHops = Integer.parseInt(value);
				if (maxNumHops <= 0) throw new RuntimeException("'maxNumHops' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxWeight"))
			{
				maxWeight = Double.parseDouble(value);
				if (maxWeight <= 0) throw new RuntimeException("'maxWeight' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxWeightFactorRespectToShortestPath"))
			{
				maxWeightFactorRespectToShortestPath = Double.parseDouble(value);
				if (maxWeightFactorRespectToShortestPath <= 0) throw new RuntimeException("'maxWeightFactorRespectToShortestPath' parameter must be greater than zero");
			}
			else if (parameter.equalsIgnoreCase("maxWeightRespectToShortestPath"))
			{
				maxWeightRespectToShortestPath = Double.parseDouble(value);
				if (maxWeightRespectToShortestPath < 0) throw new RuntimeException("'maxWeightRespectToShortestPath' parameter must be greater or equal than zero");
			}
			else
			{
				throw new RuntimeException("Unknown parameter " + parameter);
			}
		}

		Graph<Integer, Integer> g = JUNGUtils.getPhysicalLayerGraph(netPlan);

		YenLoopLessKShortestPathsAlgorithm<Integer, Integer> paths = new YenLoopLessKShortestPathsAlgorithm<Integer, Integer>(g, JUNGUtils.getEdgeWeightTransformer(weights))
				{
			@Override
			public boolean acceptPath(GraphPath<Integer> candidate)
			{
				double pathLengthInKm = 0;
				for (int linkId : candidate.getPath()) pathLengthInKm += netPlan.getLinkLengthInKm(linkId);

				if (pathLengthInKm > maxLengthInKm) return false;
				if (candidate.getPathLength() > maxNumHops) return false;

				return true;
			}

			@Override
			public boolean compareCandidateToShortestPath(GraphPath<Integer> candidate, GraphPath<Integer> shortestPath)
			{
				if (candidate.getPathWeight() > maxWeight) return false;
				if (candidate.getPathWeight() > shortestPath.getPathWeight() * maxWeightFactorRespectToShortestPath) return false;
				if (candidate.getPathWeight() > shortestPath.getPathWeight() + maxWeightRespectToShortestPath) return false;

				return true;
			}
				};

				for (int a_p = 0; a_p < N; a_p++)
				{
					for (int b_p = 0; b_p < N; b_p++)
					{
						if (a_p == b_p) continue;

						int[] demandIdsThisNodePair = netPlan.getNodePairDemands(a_p, b_p);

						List<List<Integer>> pathsThisNodePair = paths.getPaths(a_p, b_p, K);

						for (List<Integer> seqLinks : pathsThisNodePair)
						{
							for (int cont = 0; cont < demandIdsThisNodePair.length; cont++)
							{
								int dId = demandIdsThisNodePair[cont];
								addPath(dId, seqLinks);
							}
						}
					}
				}
	}

	private int addPath(int demandId, List<Integer> sequenceOfLinks)
	{
		ksp.add(sequenceOfLinks);
		int pathId = ksp.size() - 1;
		path2demand.add(demandId);
		if (!demand2PathMap.containsKey(demandId))
		{
			demand2PathMap.put(demandId, new IntArrayList());
		}
		demand2PathMap.get(demandId).add(pathId);
		for (int e : sequenceOfLinks)
		{
			if (!link2PathMap.containsKey(e))
			{
				link2PathMap.put(e, new IntArrayList());
			}
			link2PathMap.get(e).add(pathId);
		}

		return pathId;
	}


	/**
	 * Checks whether a link belongs to a path.
	 *
	 * @param e Link identifier
	 * @param p Path identifier
	 * @return <code>true</code> if link <code>e</code> belongs to path <code>p</code>. Otherwise, <code>false</code>.
	 * @since 0.2.0
	 */
	public boolean linkBelongsToPath(int e, int p)
	{
		for (int link : this.getSequenceOfLinks(p))
		{
			if (link == e)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Obtains the demand associated to the path
	 *
	 * @param p Path identifier
	 * @return Demand identifier
	 */
	public int getDemandId(int p)
	{
		return this.path2demand.get(p);
	}

	/**
	 * Removes a path from the candidate path list.
	 *
	 * @param p Path identifier
	 * @since 0.2.0
	 */
	public void removePath(int p)
	{
		ksp.remove(p);

		for(Entry<Integer, IntArrayList> entry : demand2PathMap.entrySet())
		{
			IntArrayList paths = entry.getValue();
			int pos = paths.indexOf(p);
			if (pos != -1) paths.remove(pos);

			for(int i = 0; i < paths.size(); i++)
				if (paths.get(i) > p) paths.set(i, paths.get(i)-1);
		}

		for(Entry<Integer, IntArrayList> entry : link2PathMap.entrySet())
		{
			IntArrayList paths = entry.getValue();
			int pos = paths.indexOf(p);
			if (pos != -1) paths.remove(pos);

			for(int i = 0; i < paths.size(); i++)
				if (paths.get(i) > p) paths.set(i, paths.get(i)-1);
		}

		path2demand.remove(p);
	}

	/**
	 * Obtains a vector with the demands associated to each path (the path id is the index)
	 *
	 * @return A vector with the demand identifiers
	 */
	public int[] getDemandIdsPerPath()
	{
		path2demand.trimToSize();
		return path2demand.elements();
	}

	/**
	 * Returns the number of paths in the list
	 *
	 * @return The number of paths
	 */
	public int getNumberOfPaths()
	{
		return ksp.size();
	}

	/**
	 * Returns the number of demands in the network.
	 *
	 * @return Number of demands
	 * @since 0.2.3
	 */
	public int getNumberOfDemands() { return D; }

	/**
	 * Obtains the path object associated to identifier p
	 *
	 * @param p Path identifier
	 * @return The path object
	 */
	public List<Integer> getPath(int p)
	{
		return this.ksp.get(p);
	}

	/**
	 * Returns the array of identifiers of the paths that are associated to this
	 * demand
	 *
	 * @param d The demand
	 * @return An array with the path identifiers
	 */
	public int[] getPathsPerDemand(int d)
	{
		demand2PathMap.get(d).trimToSize();
		return demand2PathMap.get(d).elements();
	}

	/**
	 * Adds a new path to the path list.
	 *
	 * @param demandId Demand identifier
	 * @param seqLinks An array with the sequence of link identifiers
	 * @return Path identifier
	 */
	public int addPath(int demandId, int[] seqLinks)
	{
		netPlan.checkRouteValidityForDemand(seqLinks, demandId);
		return addPath(demandId, IntUtils.toList(seqLinks));
	}

	/**
	 * Returns the array of identifiers of the paths that traverse this link
	 *
	 * @param e The link identifier
	 * @return An array with path identifiers
	 */
	public int[] getPathsPerLink(int e)
	{
		IntArrayList paths = link2PathMap.get(e);
		if (paths == null)
		{
			return new int[0];
		}
		link2PathMap.get(e).trimToSize();
		return link2PathMap.get(e).elements();
	}

	/**
	 * Returns the array of identifiers of the paths of demand d that traverse
	 * link e
	 *
	 * @param d The demand identifier
	 * @param e The link identifier
	 * @return An array with path identifiers
	 */
	public int[] getPathsPerDemandAndLink(int d, int e)
	{
		IntArrayList p_d = demand2PathMap.get(d).copy();
		IntArrayList p_e = link2PathMap.get(e);

		if ((p_d == null) || (p_e == null))
		{
			throw new RuntimeException("pd:" + p_d + ",pe=" + p_e); //return new int [0];
		}
		p_d.trimToSize();
		p_e.trimToSize();
		if ((p_d.size() == 0) || (p_e.size() == 0))
		{
			return new int[0];
		}

		/*
		 * make the intersection. p_d is now "p_de"
		 */
		p_d.retainAll(p_e);
		p_d.trimToSize();

		return p_d.elements();
	}

	/**
	 * Computes the amount of traffic in each link, if each path carries the
	 * traffic given in x_p vector
	 *
	 * @param x_p The traffic carried by each path in the list
	 * @return The traffic carried in each link
	 */
	public double[] computeTrafficInEachLink(double[] x_p)
	{
		int E = this.netPlan.getNumberOfLinks();
		int P = ksp.size();
		double[] y_e = new double[E];
		if (x_p.length != P)
		{
			throw new RuntimeException("Wrong array size");
		}
		for (int p = 0; p < P; p++)
		{
			int seqLinks[] = this.getSequenceOfLinks(p);
			for (int e : seqLinks)
			{
				y_e[e] += x_p[p];
			}
		}
		return y_e;
	}

	/**
	 * Computes the amount of traffic carried for each demand, if each path
	 * carries the traffic given in x_p vector
	 *
	 * @param x_p The traffic carried by each path in the list
	 * @return The traffic carried by each demand
	 */
	public double[] computeTrafficCarriedPerDemand(double[] x_p)
	{
		int D = this.netPlan.getNumberOfDemands();
		int P = ksp.size();
		if (x_p.length != P)
		{
			throw new RuntimeException("Wrongs array size");
		}
		double[] r_d = new double[D];
		for (int p = 0; p < P; p++)
		{
			r_d[this.getDemandId(p)] += x_p[p];
		}
		return r_d;
	}

	/**
	 * Returns the identifiers of the shortest paths for each demand, according
	 * to the given weights. If outputSpCosts is an array of as many positions
	 * as demands, then the costs of the shortest paths returned are stored
	 * there.
	 *
	 * @param weightPerLink The weight for each link
	 * @param outputSpCosts If an array of a length equal to the number of
	 * demands is passed, the costs of the shortest paths are stored there
	 * @return an array with the identifiers for each demand, of the shortest
	 * paths
	 * @since 0.2.0
	 */
	public int[] getShortestPathsPerDemand(double[] weightPerLink, double[] outputSpCosts)
	{
		int P = ksp.size();
		if (weightPerLink.length != E) throw new RuntimeException("Wrong array size");

		int[] spIds_d = new int[D];
		double[] bestCostSp_d = new double[D];
		Arrays.fill(bestCostSp_d, Double.MAX_VALUE);
		for (int p = 0; p < P; p++)
		{
			int d = this.getDemandId(p);
			double cost = 0;
			for (int e : this.getSequenceOfLinks(p))
			{
				cost += weightPerLink[e];
			}
			if (cost < bestCostSp_d[d])
			{
				bestCostSp_d[d] = cost;
				spIds_d[d] = p;
			}
		}
		if (outputSpCosts != null)
		{
			if (outputSpCosts.length == D)
			{
				System.arraycopy(bestCostSp_d, 0, outputSpCosts, 0, D);
			}
		}
		return spIds_d;
	}

	/**
	 * Returns the demand-link incidence matrix (a <i>D</i>x<i>E</i> matrix in which an element <i>&delta;<sub>de</sub></i> is equal to the number of times which traffic routes carrying traffic from demand <i>d</i> traverse link <i>e</i>).
	 *
	 * @return The demand-link incidence matrix
	 * @since 0.2.0
	 */
	public DoubleMatrixND computeDemand2LinkAssignmentMatrix()
	{
		DoubleMatrix2D delta_dp = computeDemand2PathAssignmentMatrix().view2D();
		DoubleMatrix2D delta_ep = computeLink2PathAssignmentMatrix().view2D();

		return new DoubleMatrixND(delta_dp.zMult(delta_ep.viewDice(), null));
	}

	/**
	 * Returns the demand-path incidence matrix (a <i>D</i>x<i>P</i> matrix in which an element <i>&delta;<sub>dp</sub></i> is equal to 1 if traffic route <i>p</i> is able to carry traffic from demand <i>d</i>).
	 *
	 * @return The demand-path incidence matrix
	 * @since 0.2.0
	 */
	public DoubleMatrixND computeDemand2PathAssignmentMatrix()
	{
		int P = getNumberOfPaths();

		DoubleMatrixND delta_dp = new DoubleMatrixND(new int[] {D, P}, "sparse");
		int[] d_p = getDemandIdsPerPath();

		for(int pathId = 0; pathId < P; pathId++)
		{
			IntMatrix1D[] indexes = new IntMatrix1D[2];
			indexes[0] = new DenseIntMatrix1D(new int[] {d_p[pathId]});
			indexes[1] = new DenseIntMatrix1D(new int[] {pathId});

			delta_dp.viewSelection(indexes).assign(1);
		}

		return delta_dp;
	}

	/**
	 * Returns the link-path incidence matrix (an <i>E</i>x<i>P</i> matrix in which an element <i>&delta;<sub>ep</sub></i> is equal to the number of times which traffic route <i>p</i> traverses link <i>e</i>).
	 *
	 * @return The link-path incidence matrix
	 * @since 0.2.0
	 */
	public DoubleMatrixND computeLink2PathAssignmentMatrix()
	{
		int P = getNumberOfPaths();

		DoubleMatrixND delta_ep = new DoubleMatrixND(new int[] {E, P}, "sparse");

		for(int pathId = 0; pathId < P; pathId++)
		{
			int[] sequenceOfLinks = getSequenceOfLinks(pathId);

			IntMatrix1D[] indexes = new IntMatrix1D[2];
			indexes[0] = new DenseIntMatrix1D(sequenceOfLinks);
			indexes[1] = new DenseIntMatrix1D(new int[] {pathId});

			delta_ep.viewSelection(indexes).assign(DoubleFunctions.plus(1));
		}

		return delta_ep;
	}

	/**
	 * For the set of paths provided, computes the map which provides for each
	 * link, the paths traversing the link.
	 *
	 * @param pathIds The paths of interest
	 * @return Each element of the List for each link, containing an
	 * ArrayList with one element per path in pathIds that traverse the link
	 * @since 0.2.0
	 */
	public List<int[]> computePathsPerLinkSublist(int[] pathIds)
	{
		int E = netPlan.getNumberOfLinks();
		List<List<Integer>> aux = new ArrayList<List<Integer>>();
		for (int e = 0; e < E; e++)
		{
			aux.add(new ArrayList<Integer>());
		}
		for (int p : pathIds)
		{
			for (int e : this.getSequenceOfLinks(p))
			{
				aux.get(e).add(p);
			}
		}

		List<int[]> res = new ArrayList<int[]>();
		for(int linkId = 0; linkId < E; linkId++)
		{
			res.add(IntUtils.toArray(aux.get(linkId)));
		}

		return res;
	}

	/**
	 * Computes the weight of each path in the list, for the given weight of the
	 * links. The weight of a path is the sum of the weights of the traversing
	 * links
	 *
	 * @param weightPerLink The set of weight per link
	 * @return An array with the cost of each path
	 */
	public double[] computeWeightPerPath(double[] weightPerLink)
	{
		int E = this.netPlan.getNumberOfLinks();
		int P = ksp.size();
		if (weightPerLink.length != E)
		{
			throw new RuntimeException("Wrong array size");
		}
		double[] weightPerPath = new double[P];
		for (int p = 0; p < P; p++)
		{
			int seqLinks[] = this.getSequenceOfLinks(p);
			for (int e : seqLinks)
			{
				weightPerPath[p] += weightPerLink[e];
			}
		}
		return weightPerPath;
	}

	/**
	 * Returns the shortest path cost for every demand.
	 *
	 * @param weightPerLink Link weights
	 * @return Shortest path cost for every demand
	 * @since 0.2.0
	 */
	public double[] computeSpWeightPerDemand(double[] weightPerLink)
	{
		double[] spCostPerDemand = new double[D];
		double[] weightPerPath = this.computeWeightPerPath(weightPerLink);
		for (int d = 0; d < D; d++)
		{
			int[] pIds = this.getPathsPerDemand(d);
			spCostPerDemand[d] = Double.MAX_VALUE;
			for (int p : pIds)
			{
				if (weightPerPath[p] < spCostPerDemand[d])
				{
					spCostPerDemand[d] = weightPerPath[p];
				}
			}
		}
		return spCostPerDemand;
	}

	/**
	 * Returns the cost of a path, given a set of link weights
	 *
	 * @param p Path identifier
	 * @param weightPerLink Link weights
	 * @return Path cost
	 * @since 0.2.0
	 */
	public double getPathCost(final int p, final double[] weightPerLink)
	{
		if (weightPerLink.length != this.netPlan.getNumberOfLinks())
		{
			throw new RuntimeException("Wrong size of array");
		}
		double pathCost = 0;
		for (int e : this.getSequenceOfLinks(p))
		{
			pathCost += weightPerLink[e];
		}
		return pathCost;
	}

	/**
	 * Returns the path identifiers of the shortest path for a given demand.
	 *
	 * @param d Demand identifier
	 * @param weightPerLink Link weights
	 * @return Vector of path identifiers
	 * @since 0.2.0
	 */
	public int[] computeShortestPathsForDemand(final int d, final double[] weightPerLink)
	{
		if (weightPerLink.length != this.netPlan.getNumberOfLinks())
		{
			throw new RuntimeException("Wrong size of array");
		}

		List<Integer> spIds = new ArrayList<Integer>();
		double spCost = Double.MAX_VALUE;
		for (int p : this.getPathsPerDemand(d))
		{
			final double pathCost = getPathCost(p, weightPerLink);
			if (spCost > pathCost)
			{
				spCost = pathCost;
				spIds.clear();
				spIds.add(p);
			}
			else if (spCost == pathCost) // JL
			{
				spIds.add(p);
			}
		}

		return IntUtils.toArray(spIds);
	}

	/**
	 * Obtains the sequence of links of path p
	 *
	 * @param p Path identifier
	 * @return An array with the sequence of link identifiers
	 * @since 0.2.0
	 */
	public int[] getSequenceOfLinks(int p)
	{
		List<Integer> seq = this.ksp.get(p);
		int[] seqLinks = new int[seq.size()];
		int cont = 0;
		for (int e : seq)
		{
			seqLinks[cont] = e;
			cont++;
		}
		return seqLinks;
	}

	/**
	 * Obtains the sequence of links of all paths
	 *
	 * @return A list of with the sequence of link identifiers per path
	 * @since 0.2.3
	 */
	public List<int[]> getSequenceOfLinksAllPaths()
	{
		List<int[]> seqLinks = new LinkedList<int[]>();
		Iterator<List<Integer>> it = ksp.iterator();
		while(it.hasNext())
			seqLinks.add(IntUtils.toArray(it.next()));

		return seqLinks;
	}

	/**
	 * Obtains the sequence of links of path p
	 *
	 * @param p Path identifier
	 * @return An array with the sequence of link identifiers
	 * @since 0.2.2
	 */
	public int[] getSequenceOfNodes(int p)
	{
		return netPlan.convertSequenceOfLinks2SequenceOfNodes(getSequenceOfLinks(p));
	}

	/**
	 * A formatted <code>String</code> describing the paths in the list, their associated demands and sequence of links
	 *
	 * @return String
	 * @since 0.2.0
	 */
	@Override
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		for (int cont = 0; cont < ksp.size(); cont++)
		{
			int[] seqLinks = getSequenceOfLinks(cont);
			s.append("path: ").append(cont).append(": demand: ").append(getDemandId(cont)).append(", seq links: ").append(netPlan.getLinkOriginNode(seqLinks[0]));
			for (int cont_e = 0; cont_e < seqLinks.length; cont_e++)
				s.append(" - ").append(netPlan.getLinkDestinationNode(seqLinks[cont_e]));

			s.append("\n");
		}

		return s.toString();
	}

	/**
	 * Class to calculate the (loopless) <i>k</i>-shortest paths between a node pair using Yen's algorithm [1].
	 *
	 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo Zaragoza
	 * @since 0.2.0
	 * @see <code>[1] J.Y. Yen, "Finding the K Shortest Loopless Paths in a Network", <i>Management Science</i>, vol. 17, no. 11, pp. 712-716, Jul. 1971</code>
	 */
	static class YenLoopLessKShortestPathsAlgorithm<V, E>
	{
		private Graph<V, E> graph;
		private Transformer<E, Double> nev;
		private DijkstraShortestPath<V, E> dijkstra;

		/**
		 * Creates an object to calculate the (loopless) <i>k</i>-shortest paths between the start vertex and others vertices.
		 *
		 * @param graph Graph on which shortest paths are searched
		 * @param nev The class responsible for returning weights for edges
		 * @since 0.2.0
		 */
		public YenLoopLessKShortestPathsAlgorithm(Graph<V, E> graph, Transformer<E, Double> nev)
		{
			this.graph = graph;
			this.nev = nev;

			dijkstra = new DijkstraShortestPath<V, E>(graph, nev);
		}

		/**
		 * <p>Returns the (loopless) <i>k</i>-shortest simple paths in increasing order of weight.</p>
		 * <p><b>Important</b>: If only <i>n</i> < <i>k</i> paths can be found, only that <i>n</i> paths will be returned.</p>
		 *
		 * @param startVertex Start vertex of the calculated paths
		 * @param endVertex Target vertex of the calculated paths
		 * @param k Number of paths to be computed
		 * @return List of paths in increasing order of weight
		 * @since 0.2.0
		 */
		public List<List<E>> getPaths(V startVertex, V endVertex, int k)
		{
			if (!graph.containsVertex(startVertex)) throw new Net2PlanException("'startVertex' is not within the graph");
			if (!graph.containsVertex(endVertex)) throw new Net2PlanException("'endVertex' is not within the graph");

			LinkedList<List<E>> paths = new LinkedList<List<E>>();

			if (startVertex.equals(endVertex)) return paths;

			PriorityQueue<GraphPath> priorityQueue = new PriorityQueue<GraphPath>();

			if (dijkstra.getDistance(startVertex, endVertex) == null) return paths;
			List<E> aux = dijkstra.getPath(startVertex, endVertex);
			double cost = JUNGUtils.getPathWeight(aux, nev);
			GraphPath<E> shortestPath = new GraphPath<E>(aux, cost);

			if(!acceptPath(shortestPath))
			{
				return paths;
			}

			paths.add(aux);

			DijkstraShortestPath<V, E> blockedDijkstra;

			while (paths.size() < k)
			{
				List<E> curShortestPath = paths.getLast();

				int currentPathLength = curShortestPath.size();

				// Split path into Head and NextEdge
				for (int deviationId = 0; deviationId < currentPathLength; deviationId++)
				{
					List<E> head = curShortestPath.subList(0, deviationId);
					V deviationVertex = head.isEmpty() ? startVertex : graph.getDest(head.get(deviationId - 1));

					// 1. Block edges.
					Graph<V, E> blocked = blockFilter(head, deviationVertex, paths);

					// 2. Get shortest path in graph with blocked edges.
					blockedDijkstra = new DijkstraShortestPath<V, E>(blocked, nev);

					Number dist = blockedDijkstra.getDistance(deviationVertex, endVertex);
					if (dist == null) continue;

					List<E> tail = blockedDijkstra.getPath(deviationVertex, endVertex);

					// 3. Combine head and tail into new path.
					List<E> candidatePath = new ArrayList<E>(deviationId + tail.size());
					candidatePath.addAll(head);
					candidatePath.addAll(tail);

					GraphPath<E> candidate = new GraphPath<E>(candidatePath, JUNGUtils.getPathWeight(candidatePath, nev));

					// Check if we already found this solution
					if (priorityQueue.contains(candidate)) continue;

					if (!acceptPath(candidate) || !compareCandidateToShortestPath(candidate, shortestPath)) continue;

					priorityQueue.add(candidate);
				}

				if (priorityQueue.isEmpty()) break; // No more candidate paths

				paths.add(priorityQueue.poll().getPath());
			}

			return paths;
		}

		/**
		 * Blocks all incident edges of the vertices in head as well as the edge connecting head to the next node by creating a new filtered graph.
		 *
		 * @param head The current head, from source to deviation node
		 * @param deviation The edge to the next node
		 * @param foundPaths The solutions already found and to check against
		 * @return The filtered graph without the blocked edges
		 * @since 0.2.0
		 */
		private Graph<V, E> blockFilter(List<E> head, V deviation, List<List<E>> foundPaths)
		{
			final Set<E> blocked = new HashSet<E>();

			// Block incident edges to make all vertices in head unreachable.
			for (E e : head)
			{
				for (E e2 : graph.getIncidentEdges(graph.getSource(e)))
				{
					blocked.add(e2);
				}
			}

			// Block all outgoing edges that have been used at deviation vertex
			for (List<E> path : foundPaths)
			{
				if (path.size() > head.size() && ListUtils.isEqualList(path.subList(0, head.size()), head))
				{
					for (E e : path)
					{
						if (graph.isSource(deviation, e))
						{
							blocked.add(e);
							break; // Continue with next path.
						}
					}
				}
			}

			EdgePredicateFilter<V, E> filter = new EdgePredicateFilter<V, E>(new Predicate<E>()
					{
				@Override
				public boolean evaluate(E e)
				{
					return !blocked.contains(e);
				}
					});

			return filter.transform(graph);
		}

		public boolean acceptPath(GraphPath<E> candidate)
		{
			return true;
		}

		public boolean compareCandidateToShortestPath(GraphPath<E> candidate, GraphPath<E> shortestPath)
		{
			return true;
		}
	}
}