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

package com.tejas.workspace.examples.netDesignAlgorithm.fba;

import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Given a network topology and the capacities in the links, this algorithm creates one demand of traffic for each input-output node pair,
 * routes the traffic of each demand through the shortest path in number of hops, and obtains the offered traffic for each demand (that is, assigns bandwidth)
 * to each demand, so that global assignment is alpha-fair, with the constraint that no network link is over-congested.</p>
 *
 * <p>According to [<a href='#ref'>1</a>], the alpha-fairness assignment is obtained by solving the NUM model (Network Utility Maximization)
 * where the utility of a demand d which is assigned a bandwidth h_d is given by: (r^(1-alpha) / (1-alpha)), and the target is to maximize the sum
 * of the utilities in all the demands in the network.</p>
 *
 * <a name='ref' />[1] J. Mo, J. Walrand, "Fair End-to-End Window-Based Congestion Control," <i>IEEE/ACM transactions on Networking</i>, vol. 8, no. 5, pp. 556–567, October 2000
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FBA_maxAlphaFairness implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	/* Initialize some variables */
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	double[] u_e = netPlan.getLinkCapacityInErlangsVector();

	/* Basic checks */
	if (N == 0 || E == 0) throw new Net2PlanException("This algorithm requires a complete topology (nodes and links)");

	/* Take some user-defined parameters */
	double alpha = Double.parseDouble(algorithmParameters.get("alpha"));
	if (alpha < 0) throw new Net2PlanException("Alpha parameter must be non negative");
	String shortestPathType = algorithmParameters.get("shortestPathType");
	if (!shortestPathType.equalsIgnoreCase("hops") && !shortestPathType.equalsIgnoreCase("km")) throw new Net2PlanException("'shortestPathType' must be 'hops' or 'km'");

	/* Remove all routes in current netPlan object */
	netPlan.removeAllDemands();
	netPlan.removeAllRoutes();
	netPlan.removeAllProtectionSegments();

	/* Add one demand per ingress-egress node pair */
	final double [] costVector = shortestPathType.equals("km")? netPlan.getLinkLengthInKmVector() : DoubleUtils.ones(E);

	for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
	{
	    for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
	    {
		if (ingressNodeId == egressNodeId) continue;

		netPlan.addDemand(ingressNodeId, egressNodeId, 1, null);
	    }
	}

	/* For each demand, set the offered traffic solving the BA formulation */
	final int D = netPlan.getNumberOfDemands();

	/* For each demand, compute the shortest path route */
	CandidatePathList cpl = new CandidatePathList(netPlan, costVector, "K", "1");

	/* Create the optimization problem object (JOM library) */
	OptimizationProblem op = new OptimizationProblem();

	/* Add the decision variables to the problem */
	op.addDecisionVariable("h_d", false, new int[] {1, D}, 0, Double.MAX_VALUE);

	/* Set some input parameters */
	op.setInputParameter("u_e", u_e , "row");
	op.setInputParameter("alpha", alpha);
	op.setInputParameter("R_de", cpl.computeDemand2LinkAssignmentMatrix());

	/* Sets the objective function */
	if (alpha == 1)
	    op.setObjectiveFunction("maximize", "sum(ln(h_d))");
	else if (alpha == 0)
	    op.setObjectiveFunction("maximize", "sum(h_d)");
	else
	    op.setObjectiveFunction("maximize", "(1-alpha) * sum(h_d ^ (1-alpha))");

	op.addConstraint("h_d * R_de <= u_e"); // the capacity constraints (E constraints)

	/* Call the solver to solve the problem */
	String solverName = algorithmParameters.get("solverName");
	String solverLibraryName = algorithmParameters.get("solverLibraryName");
	op.solve(solverName, "solverLibraryName", solverLibraryName);

	/* If an optimal solution was not found, quit */
	if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

	/* Retrieve the optimum solutions */
	double[] h_d = op.getPrimalSolution("h_d").to1DArray();

	for (int d = 0 ; d < D ; d ++)
	    netPlan.setDemandOfferedTrafficInErlangs(d, h_d[d]);

	netPlan.addRoutes(cpl, h_d, false);

	/* A simple check to detect some possible numerical errors: the worse link utilization must be 100% */
	if (netPlan.getLinkMaximumUtilization() < 0.98)
	    return "Ok! Warning: numerical error: the resulting network congestion is not 100%";
	else
	    return "Ok!";
    }

    @Override
    public String getDescription()
    {
	return "Given a network topology and the capacities in the links, this algorithm creates one demand of traffic for each input-output node pair," +
	    " routes the traffic of each demand through the shortest path in number of hops, and obtains the offered traffic for each" +
	    " demand (that is, assigns bandwidth) to each demand, so that global assignment is alpha-fair, with the constraint that no " +
	    "network link is over-congested. According to the Mo & Walrand paper (see ref below), the alpha-fairness assignment is " +
	    "obtained by solving the NUM model (Network Utility Maximization) where the utility of a demand d which is assigned a" +
	    " bandwidth h_d is given by: (r^(1-alpha) / (1-alpha)), and the target is to maximize the sum of the utilities in all " +
	    "the demands in the network " +
	    "\n \nReference: Mo, J.; Walrand, J. (2000). \"Fair End-to-End Window-Based Congestion Control\". IEEE/ACM transactions on Networking 8: 556–567";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("alpha", "2", "Alpha factor for the utility function."));
	algorithmParameters.add(Triple.of("shortestPathType", "km" , "Each demand is routed according to the shortest path according to this type. Can be 'km' or 'hops'"));
	algorithmParameters.add(Triple.of("solverName", "ipopt", "The solver name to be used by JOM"));
	algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));

	return algorithmParameters;
    }
}