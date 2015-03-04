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
import java.util.List;
import java.util.Map;
import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the traffic routing that minimizes
 * the worst link utilization using a flow-path formulation. For each demand, the set of admissible paths is composed of the ranking of (at most)
 * "k" shortest paths in km, between the demand end nodes. "k" is a user-defined parameter.  Paths for which its propagation time sums more than
 * "maxPropDelayMs", a user-defined parameter, are considered not admissible. The routing may be constrained to be non-bifurcated
 * setting the user-defined parameter "nonBifurcated" to "true".
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_minBottleneckUtilization_xp implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		double net2planPrecision = Double.parseDouble(net2planParameters.get("precisionFactor"));
		double [] u_e = netPlan.getLinkCapacityInErlangsVector();
		double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		int k = Integer.parseInt(algorithmParameters.get("k"));
		boolean nonBifurcatedRouting = Boolean.parseBoolean(algorithmParameters.get("nonBifurcated"));
		double maxPropDelayMs = Double.parseDouble(algorithmParameters.get("maxPropDelayMs"));
		double maxPathLengthKm = (maxPropDelayMs / 1000) * Double.parseDouble(net2planParameters.get("propagationSpeedInKmPerSecond"));

		/* Basic checks */
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all routes in current netPlan object */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Create the candidate path list */
		CandidatePathList cpl = new CandidatePathList (netPlan , netPlan.getLinkLengthInKmVector() , "K" , Integer.toString(k) , "maxLengthInKm" , Double.toString(maxPathLengthKm));
		int P = cpl.getNumberOfPaths();
		double [] h_p = DoubleUtils.select(h_d, cpl.getDemandIdsPerPath()); // for each path p, the offered traffic of its associated demand d(p)

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("x_p", nonBifurcatedRouting , new int[] { 1 , P }, 0, 1); // the fraction of traffic of demand d(p) that is carried by p
		op.addDecisionVariable("rho", false , new int[] { 1 , 1 }, 0, 1); // the worse utilization in the links

		/* Set some input parameters */
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("h_p" , h_p , "row");

		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "rho");

		/* VECTORIAL FORM OF THE CONSTRAINTS  */
		op.setInputParameter("A_dp", cpl.computeDemand2PathAssignmentMatrix ());
		op.setInputParameter("A_ep", cpl.computeLink2PathAssignmentMatrix ());
		op.addConstraint("A_dp * x_p' == 1"); // for each demand, the 100% of the traffic is carried (summing the associated paths)
		op.addConstraint("A_ep * ((h_p .* x_p)') <= u_e' * rho" , "capConstraints"); // for each link, its utilization is below or equal to rho


		/* Call the solver to solve the problem */
		String solverName = algorithmParameters.get("solverName");
		String solverLibraryName = algorithmParameters.get("solverLibraryName");
		op.solve(solverName, "solverLibraryName" , solverLibraryName);

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal ()) throw new RuntimeException ("An optimal solution was not found");

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		double [] x_p = op.getPrimalSolution("x_p").to1DArray();

		/* Update netPlan object adding the calculated routes */
		for (int p = 0 ; p < x_p.length ; p ++)
			if (x_p [p] * h_d [cpl.getDemandId(p)] > net2planPrecision)
				netPlan.addRoute(cpl.getDemandId(p), x_p [p] * h_d [cpl.getDemandId(p)] , cpl.getSequenceOfLinks(p), null, null);

		/* Print the multipliers of the link utilization constraints */
		for (int e  = 0 ; e < E ; e ++)
		{
			int a_e = netPlan.getLinkOriginNode(e);
			int b_e = netPlan.getLinkDestinationNode(e);
			System.out.println("Link id " +  e + "(" + a_e + " -> " + b_e + "): pi = " + op.getMultipliersOfConstraint("capConstraints").get(e) + " , util " + netPlan.getLinkUtilization(e));
		}

		return "Ok!: |P| = " + cpl.getNumberOfPaths();
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the traffic routing that minimizes" +
				" the worst link utilization using a flow-path formulation. For each demand, the set of admissible paths is composed of the ranking of (at most) " +
				"\"k\" shortest paths in km, between the demand end nodes. \"k\" is a user-defined parameter.  Paths for which its propagation time sums more than " +
				" \"maxPropDelayMs\", a user-defined parameter, are considered not admissible. The routing may be constrained to be non-bifurcated " +
				" setting the user-defined parameter \"nonBifurcated\" to \"true\"";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("k", "5", "Maximum number of admissible paths per demand"));
		algorithmParameters.add(Triple.of("nonBifurcated", "false", "True if the routing is constrained to be non-bifurcated"));
		algorithmParameters.add(Triple.of("maxPropDelayMs", "30", "Maximum propagation delay in miliseconds allowed in a path"));
		algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
		algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
		return algorithmParameters;
	}




}
