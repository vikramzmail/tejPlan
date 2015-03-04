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

package com.tejas.workspace.examples.netDesignAlgorithm.fa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a set of nodes and links, with their capacities, an offered traffic vector, and a set of admissible paths for each demand (given by the k-shortest paths
 * in km for each demand, being k a user-defined parameter), this algorithm uses a simulated annealing metaheuristic to find the non-bifurcated routing solution
 * that minimizes the network congestion. Two solutions are considered neighbors if they differ the routing ofone demand. The number of iterations
 * in the inner (all with the same temperature) and outer loop (decreasing the temperature) are user-defined parameters.
 * The initial temperature is computed such that we accept a solution with a 5% of more congestion with 99% of probability. The last
 * temperature is such that with accept a solution with a 5% of more congestion with a 0.01% of probability. Temperature decreases geometrically, the alpha
 * factor of this progression is computed to match previous parameters.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_SAN_minCongestion implements IAlgorithm
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

	final int k = Integer.parseInt(algorithmParameters.get("k")); // number of loopless candidate paths per demand
	final int san_numOuterIterations = Integer.parseInt(algorithmParameters.get("san_numOuterIterations"));
	final int san_numInnerIterations = Integer.parseInt(algorithmParameters.get("san_numInnerIterations"));

	/* Initial temperature so that we accept bad solution with extra congestion 0.05 with probability 0.99 */
	final double san_initialTemperature = -0.05 / Math.log (0.99);

	/* Final temperature so that we accept bad solution with extra congestion 0.05 with probability 0.001 */
	final double san_finalTemperature = -0.05 / Math.log (0.001);

	/* Alpha factor so that starting in the initial temp, after exactly san_numOuterIterations we have a temperature equal to the final temp  */
	/* In each outer iteration, the new temperature t(i+1) = alpha*t(i) */
//	final double san_alpha = Math.pow (san_finalTemperature / san_initialTemperature , 1/(san_numOuterIterations-1));
	final double san_alpha = Math.pow (san_finalTemperature / san_initialTemperature , 1.0/(san_numOuterIterations-1)); // JL

	System.out.println("Initial temp: " + san_initialTemperature + ", end temp: " + san_finalTemperature + ", alpha: " + san_alpha);

	/* Construct the candidate path list */
	CandidatePathList cpl = new CandidatePathList (netPlan , netPlan.getLinkLengthInKmVector() , "K" , Integer.toString(k));
	final double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
	final double [] u_e = netPlan.getLinkCapacityInErlangsVector();
	int [] numberOfDifferentPaths_d = new int [D];
	for (int d = 0 ; d < D ; d ++)
		numberOfDifferentPaths_d [d] = cpl.getPathsPerDemand(d).length;

	/* Construct an initial solution: each demand is carried through the shortest path in km  */
	int [] current_usedPath_d = new int [D];
	for (int d = 0 ; d < D ; d ++)
		current_usedPath_d [d] = cpl.getPathsPerDemand(d) [0];
	double current_congestion = computeNetCongestion (u_e , current_usedPath_d , h_d , cpl);
	/* Update the best solution found so far to the initial solution */
	int [] best_usedPath_d = current_usedPath_d;
	double best_congestion = current_congestion;

	/* Main algorithm loop */
	Random rnd = new Random (); // random number generator
	double temp = san_initialTemperature;
	for (int outerItCount = 0 ; outerItCount < san_numOuterIterations ; outerItCount ++)
	{
		for (int innerItCount = 0 ; innerItCount < san_numInnerIterations ; innerItCount ++)
		{
			/* choose a demand randomly */
			final int d = rnd.nextInt(D);
			final int numberPaths = numberOfDifferentPaths_d [d];
			final int currentPath = current_usedPath_d [d];

			/* choose a new (different) path for that demand randomly */
			int newUsedPath = cpl.getPathsPerDemand(d) [rnd.nextInt(numberPaths)];
			while (newUsedPath == currentPath) {
				newUsedPath = cpl.getPathsPerDemand(d) [rnd.nextInt(numberPaths)];
				}
			int [] potential_usedPath_d = Arrays.copyOf(current_usedPath_d, D);
			potential_usedPath_d [d] = newUsedPath;

			/* compute the new solution cost */
			double newCongestion = computeNetCongestion (u_e , potential_usedPath_d , h_d , cpl);

			/* Check if we accept the new solution */
			if (acceptSolution (current_congestion , newCongestion , temp , rnd))
			{
				current_usedPath_d [d] = newUsedPath; // current_usedPath_d = Arrays.copyof (potential_usedPath_d , D);
				current_congestion = newCongestion;

				/* update the best solution if needed */
				if (current_congestion < best_congestion)
				{
					best_congestion = current_congestion;
					best_usedPath_d = Arrays.copyOf(current_usedPath_d , D);
				}
			}
		}
		temp *= san_alpha;
	}

	/* Save the best solution found */
	netPlan.removeAllRoutes();
	netPlan.removeAllProtectionSegments();
	for (int d = 0 ; d < D ; d ++)
		netPlan.addRoute(d, h_d [d], cpl.getSequenceOfLinks(best_usedPath_d [d]), null, null);

	return "Ok! Congestion : " + best_congestion;
    }

    @Override
    public String getDescription()
    {
	return "Given a set of nodes and links, with their capacities, an offered traffic vector, and a set of admissible paths for each demand (given by " +
	    "the k-shortest paths in km for each demand, being k a user-defined parameter), this algorithm uses a simulated annealing metaheuristic" +
	    " to find the non-bifurcated routing solution that minimizes the network congestion." +
	    "Two solutions are considered neighbors if they differ the routing ofone demand. The number of iterations in the inner (all with the" +
	    " same temperature) and outer loop (decreasing the temperature) are user-defined parameters. The initial temperature is computed such " +
	    "that we accept a solution with a 5% of more congestion with 99% of probability. The last temperature is such that with accept a " +
	    "solution with a 5% of more congestion with a 0.01% of probability. Temperature decreases geometrically, the alpha factor of this" +
	    " progression is computed to match previous parameters.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("k", "100", "Number of candidate paths per demand"));
	algorithmParameters.add(Triple.of("san_numOuterIterations", "50", "Number of iterations in the outer loop (changing the temperature)"));
	algorithmParameters.add(Triple.of("san_numInnerIterations", "1000", "Number of iterations in the inner loop (all with the same temperature)"));
	return algorithmParameters;
    }

    /* computes the congestion in the network (the objective function of the problem) */
    private double computeNetCongestion(double[] u_e, int[] pathId_d, double[] h_d, CandidatePathList cpl)
    {
	final int E = u_e.length;
	final int D = pathId_d.length;
	double [] y_e = new double [E];
	for (int d = 0 ; d < D ; d ++)
	    for (int e : cpl.getSequenceOfLinks(pathId_d[d]))
		    y_e[e] += h_d [d];

	return DoubleUtils.maxValue(DoubleUtils.divide(y_e, u_e));
    }

    /* SAN criteria to accept a solution according to its cost (to minimize) */
    private boolean acceptSolution (double current_cost , double new_cost , double temperature , Random rnd)
    {
	if (new_cost < current_cost) return true;
	final double probThreshold = Math.exp (-(new_cost - current_cost) / temperature);
	if (rnd.nextDouble() < probThreshold) return true;
	return false;
    }
}
