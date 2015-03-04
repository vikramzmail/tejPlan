package com.tejas.workspace.examples.netDesignAlgorithm.fa;

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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.Constants.SearchType;

/**
 * Given a set of nodes and links, with their capacities, an offered traffic, and a set of admissible paths for each demand (given by the k-shortest paths
 * in km for each demand, being k a user-defined parameter), this algorithm uses a tabu search metaheuristic to find for each demand a link-disjoint
 * 1+1 routes, so that the network congestion is minimized. For each demand, the candidate 1+1 pairs are computed as all the link disjoint pairs p1,p2, such that
 * p1 and p2 are admissible paths. Two solutions are considered neighbors if they differ in the routing of one demand (either primary path, backup path or both).
 * The number of iterations is a user-defined parameter. The tabu list size is a user-defined parameters, defined as a fraction of the number of demands.
 * If the current solution is not improved during a number of consecutive iterations (a user-defined parameter), the algorithm produces a randomization of
 * the current solution based to jump to a different solution of the search space. This randomization makes use of a long-term memory structure. This
 * structure counts for each 1+1 pair p, the number of times that introducing this path in a solution, meant a solution improvement.
 * With this, we intend to capture the paths that more often have produced improving results, and use this information to tune the randomization jumps.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, May 2013
 */
public class FA_TS_minCongestion11 implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	/* Initialize some variables */
	final int N = netPlan.getNumberOfNodes();
	final int D = netPlan.getNumberOfDemands();
	final int E = netPlan.getNumberOfLinks();

	/* Basic checks */
	if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");

	final double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
	final double [] u_e = netPlan.getLinkCapacityInErlangsVector();

	final int k = Integer.parseInt(algorithmParameters.get("k")); // number of loopless candidate paths per demand
	final int ts_maxNumIt = Integer.parseInt(algorithmParameters.get("ts_maxNumIt"));
	final int ts_tabuListTenure = (int) Math.floor(D * Double.parseDouble(algorithmParameters.get("ts_tabuListTenureFraction")));
	final int ts_numItNonImprovingToRandomize = Integer.parseInt(algorithmParameters.get("ts_numItNonImprovingToRandomize"));

	/* Construct the candidate path list */
	CandidatePathList cpl = new CandidatePathList (netPlan , netPlan.getLinkLengthInKmVector() , "K" , Integer.toString(k));

	/* For each path, compute those of the same demand which are link disjoint */
	ArrayList<ArrayList<int []>> candidate11PairsList_d = computeCandidate11PairList (netPlan , cpl);

	/* Solution coding: an int[D] with the index of the 1+1 pair associated to this demand.   */
	int [] current_solution = new int [D]; // initialize it so all demands use the first 1+1 pair in its 1+1 pair list
	double current_cost = computeNetCongestion (u_e , current_solution , h_d, candidate11PairsList_d , cpl);

	/* Initialize tabu list: list of last demands changed */
	LinkedList<Integer> tabuList = new LinkedList<Integer> ();

	/* Initialize long-time memory structure: for each path pair, the number of times appeared in improving solutions */
	int [][] longTermMemory = new int [D][];
	for (int d = 0 ; d < D ; d ++) longTermMemory [d] = new int [candidate11PairsList_d.get(d).size()];

	/* Main loop */
	Random rnd = new Random ();
	double best_cost = current_cost;
	int [] best_solution = Arrays.copyOf(current_solution,D);
	int numberOfIterationsWithoutImproving = 0;
	for (int numIt = 0 ; numIt < ts_maxNumIt ; numIt ++)
	{
		/* For debug purposes: print information of the iteration */
		//System.out.println("Starting iteration : " + numIt + ". Best cost: " + best_cost + ". Current cost: " + current_cost ); // . Current solution: " + Arrays.toString(current_solution));

		/* check if the best solution was not improved in last iterations. If so, move to a new solution.
		 * This new solution is a random variation of a solution created from the long term memory */
		if (numberOfIterationsWithoutImproving > ts_numItNonImprovingToRandomize)
		{
			current_solution = new int [D];
			for (int d = 0 ; d < D ; d ++)
			{
				boolean takeBestLongTermPath = (rnd.nextDouble() <= 0.8); // 0.8 probability takes the best pair according to long term memory
				if (takeBestLongTermPath)
				{
					int [] bestPairIds = IntUtils.maxIndexes(longTermMemory [d], SearchType.ALL);
					current_solution [d] =  bestPairIds [rnd.nextInt(bestPairIds.length)]; // the best pair according to long term memory. If more than one with the same => choose randomly among them
				}
				else
					current_solution [d] = rnd.nextInt(longTermMemory [d].length); // a path randomly chosen
				current_cost = computeNetCongestion (u_e , current_solution , h_d, candidate11PairsList_d , cpl);
				numberOfIterationsWithoutImproving = 0;
			}
			//System.out.println("JUMP!!!");
		}



		/* compute the neighborhood: change one demand */
		int [] bestAcceptableNeighbor_solution = null; // best neighbor solution found in this iteration
		double bestAcceptableNeighbor_cost = Double.MAX_VALUE; // cost of best neighbor solution found in this iteration
		int bestAcceptableNeighbor_changingDemand = -1; // the demand that changes in the neighbor respect to current solution: needed for log-term memory update

		/* Iterate for each neighbor: a neighbor solution differs from current in the 1+1 pair used in ONE demand  */
		for (int neighborChangingDemand = 0 ; neighborChangingDemand < D ; neighborChangingDemand ++)
		{
			for (int neighborNewPathPairId = 0 ; neighborNewPathPairId < candidate11PairsList_d.get(neighborChangingDemand).size () ; neighborNewPathPairId ++)
			{
				/* If the neighbor is the same as current solution, skip this iteration */
				if (neighborNewPathPairId == current_solution [neighborChangingDemand]) continue;

				/* create the neighbor solution, copying it in a new array */
				int [] neighbor_solution = Arrays.copyOf(current_solution, D);
				neighbor_solution [neighborChangingDemand] = neighborNewPathPairId;

				/* compute the cost of the neighbor solution */
				double neighbor_cost = computeNetCongestion (u_e , neighbor_solution , h_d, candidate11PairsList_d , cpl);

				/* Update the long-term memory structure: if the solution is better than current solution, store it */
				if (neighbor_cost < current_cost)
					longTermMemory [neighborChangingDemand][neighborNewPathPairId] ++;

				/* Ambition criteria: if the cost is better than best solution so far: accept it as next potential solution
				 * (best solution, and best neighbor are updated)  */
				if (neighbor_cost < best_cost)
				{
					best_solution = Arrays.copyOf(neighbor_solution,D);
					best_cost = neighbor_cost;
					bestAcceptableNeighbor_solution = Arrays.copyOf(neighbor_solution,D);
					bestAcceptableNeighbor_cost = neighbor_cost;
					numberOfIterationsWithoutImproving = 0;
					continue;
				}

				/* check if the neighbor solution is tabu: if so, skip it */
				if (tabuList.contains(neighborChangingDemand)) continue;

				/* the neighbor solution is not tabu: check if it is the best neighbor found so far */
				if (neighbor_cost < bestAcceptableNeighbor_cost)
				{
					bestAcceptableNeighbor_cost = neighbor_cost;
					bestAcceptableNeighbor_solution = Arrays.copyOf(neighbor_solution, D);
					bestAcceptableNeighbor_changingDemand = neighborChangingDemand;
				}
			}
		}

		/* Check if the solution we make is an improving solution (update numberOfIterationsWithoutImproving) */
		if (bestAcceptableNeighbor_cost < current_cost)
				numberOfIterationsWithoutImproving = 0;
		else
			numberOfIterationsWithoutImproving ++;

		/* Move to the best neighbor found: current solution is the best neighbor */
		
		/* If there are no neighbors => rare thing => force random variation of the solution */
		if (bestAcceptableNeighbor_solution == null) { numberOfIterationsWithoutImproving = ts_numItNonImprovingToRandomize + 1; continue; }
		
		current_solution = bestAcceptableNeighbor_solution;
		current_cost = bestAcceptableNeighbor_cost;
		

		/* Update the tabu list: add the new move to the list */
		tabuList.addLast(bestAcceptableNeighbor_changingDemand);
		/* Update the tabu list: if the tabu list is oversized, remove the first (oldest) element  */
		if (tabuList.size() > ts_tabuListTenure) tabuList.removeFirst();
	}

	/* Save the best solution found into the netPlan object */
	netPlan.removeAllRoutes();
	netPlan.removeAllProtectionSegments();
	for (int d = 0 ; d < D ; d ++)
	{
		final int bestPair = best_solution [d];
		final int [] primaryAndBackupPath = candidate11PairsList_d.get(d).get(bestPair);
		final int newProtectionSegment = netPlan.addProtectionSegment(cpl.getSequenceOfLinks(primaryAndBackupPath [1]), h_d [d] , null);
		netPlan.addRoute(d, h_d [d], cpl.getSequenceOfLinks(primaryAndBackupPath [0]), new int [] { newProtectionSegment } , null);
	}

	return "Ok! Cost : " + best_cost;
}

    @Override
    public String getDescription()
    {
	return "Given a set of nodes and links, with their capacities, an offered traffic, and a set of admissible paths for each demand (given " +
	    "by the k-shortest paths in km for each demand, being k a user-defined parameter), this algorithm uses a tabu search metaheuristic " +
	    "to find for each demand a link-disjoint 1+1 routes, so that the network congestion is minimized. For each demand, the candidate 1+1 pairs are " +
	    "computed as all the link disjoint pairs p1,p2, such that p1 and p2 are admissible paths. Two solutions are considered neighbors if they " +
	    "differ in the routing of one demand (either primary path, backup path or both). The number of iterations is a user-defined parameter. " +
	    "The tabu list size is a user-defined parameters, defined as a fraction of the number of demands. If the current solution is not improved " +
	    "during a number of consecutive iterations (a user-defined parameter), the algorithm produces a randomization of the current solution based " +
	    "to jump to a different solution of the search space. This randomization makes use of a long-term memory structure. This structure counts for " +
	    "each 1+1 pair p, the number of times that introducing this path in a solution, meant a solution improvement. With this, we intend to " +
	    "capture the paths that more often have produced improving results, and use this information to tune the randomization jumps.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("k", "10", "Number of candidate paths per demand"));
	algorithmParameters.add(Triple.of("ts_maxNumIt", "1000", "Number of iterations in the outer loop"));
	algorithmParameters.add(Triple.of("ts_tabuListTenureFraction", "0.1", "Size of the tabu list as a fraction of the number of demands"));
	algorithmParameters.add(Triple.of("ts_numItNonImprovingToRandomize", "10", "Number iterations non improving the best solution, which imply a large randomization"));
	return algorithmParameters;
    }

    /* computes the congestion in the network (the objective function of the problem) */
    private double computeNetCongestion (double [] u_e , int [] solution_d , double [] h_d , ArrayList<ArrayList<int []>> potential11Pairs_d , CandidatePathList cpl)
    {
	final int E = u_e.length;
	final int D = solution_d.length;
	double [] y_e = new double [E];
	for (int d = 0 ; d < D ; d ++)
	{
		final int [] pathPair = potential11Pairs_d.get(d).get(solution_d [d]);
		for (int e : cpl.getSequenceOfLinks(pathPair [0]))
			y_e [e] += h_d [d];
		for (int e : cpl.getSequenceOfLinks(pathPair [1]))
			y_e [e] += h_d [d];
	}
	return DoubleUtils.maxValue(DoubleUtils.divide(y_e, u_e));
    }

    /* From a candidate path list, extract for each demand, the list of link-disjoint path pairs. There pairs are candidates for becoming primary-backup
     * path pairs for the demand.  */
    private ArrayList<ArrayList<int []>> computeCandidate11PairList (NetPlan netPlan , CandidatePathList cpl)
    {
	final int D = netPlan.getNumberOfDemands();
	ArrayList<ArrayList<int []>> cpl11 = new ArrayList<ArrayList<int []>> (D);
	for (int d = 0 ; d < D ; d ++)
	{
	    cpl11.add(new ArrayList<int []> ());
	    int [] pIds = cpl.getPathsPerDemand(d);
	    for (int contP_1 = 0 ; contP_1 < pIds.length - 1 ; contP_1 ++)
	    {
		    final int p1 = pIds [contP_1];
		    final int [] seqLinks_1 = cpl.getSequenceOfLinks(p1);
		    for (int contP_2 = contP_1 + 1 ; contP_2 < pIds.length ; contP_2 ++)
		    {
			    final int p2 = pIds [contP_2];
			    final int [] seqLinks_2 = cpl.getSequenceOfLinks(p2);
			    if (IntUtils.intersect(seqLinks_1 , seqLinks_2).length == 0) cpl11.get(d).add (new int [] { p1 , p2 } );
		    }
	    }

	    if (cpl11.get(d).isEmpty()) throw new Net2PlanException ("Demand " + d + " has no two link-disjoint paths");
	}

	return cpl11;
    }
}