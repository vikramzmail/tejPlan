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
 * "k" shortest paths in number of hops, between the demand end nodes. "k" is a user-defined parameter. The fraction of the demand volume that
 * is carried by a path that carries traffic, is constrained to be higher or equal than a user-defined parameter "tMin". That is, a path p can
 * carry no traffic, or an amount of traffic equal or greater than tMin x h_d(p), where d(p) is the demand associated to path p. Finally, the user-defined
 * parameter BIFMAX sets the maximum number of paths of a demand that can carry traffic.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, May 2013
 */
public class FA_minBottleneckUtilizationBifurcationConstraints_xp implements IAlgorithm
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
		double tMin = Double.parseDouble(algorithmParameters.get("tMin"));
		int BIFMAX = Integer.parseInt(algorithmParameters.get("BIFMAX"));
		if ((tMin < 0) || (tMin > 1)) throw new Net2PlanException("Parameter tMin must be between 0 and 1");
		if (BIFMAX < 1) throw new Net2PlanException ("Parameter BIFMAX must be greater or equal than 1");

		/* Basic checks */
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all routes in current netPlan object */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Create the candidate path list */
		CandidatePathList cpl = new CandidatePathList (netPlan , DoubleUtils.ones(E) , "K" , Integer.toString(k));
		int P = cpl.getNumberOfPaths();
		double [] h_p = DoubleUtils.select(h_d, cpl.getDemandIdsPerPath()); // for each path p, the offered traffic of its associated demand d(p)

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("x_p", false , new int[] { 1 , P }, 0, 1); // the amount of traffic of demand d(p) carried by path p
		op.addDecisionVariable("xx_p", true , new int[] { 1 , P }, 0, 1); // 1 if the path p carries traffic, 0 otherwise
		op.addDecisionVariable("rho", false , new int[] { 1 , 1 }, 0, 1); // amount of traffic in link e

		/* Set some input parameters */
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("h_p" , h_p , "row");
		op.setInputParameter("h_d" , h_d , "row");
		op.setInputParameter("tMin" , tMin);
		op.setInputParameter("BIFMAX" , BIFMAX);


		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "rho");

		/* VECTORIAL FORM OF THE CONSTRAINTS  */
		op.setInputParameter("A_dp", cpl.computeDemand2PathAssignmentMatrix ());
		op.setInputParameter("A_ep", cpl.computeLink2PathAssignmentMatrix ());
		op.addConstraint("A_dp * x_p' == 1"); // for each demand, the 100% of the traffic is carried (summing the associated paths)
		op.addConstraint("A_ep * (h_p .* x_p)' <= u_e' * rho"); // for each link, its utilization is below or equal to rho
		op.addConstraint("xx_p >= x_p"); // if a path carries traffic => xx_p = 1
		op.addConstraint("x_p >= tMin * xx_p"); // if a path does not carry traffic, then xx_p = 0. If it carriers, then x_p >= tMin
		op.addConstraint("A_dp * xx_p' <= BIFMAX"); // the number of paths carrying traffic of a demand is limited by BIFMAX

		/* Call the solver to solve the problem */
		String solverName = algorithmParameters.get("solverName");
		String solverLibraryName = algorithmParameters.get("solverLibraryName");
		op.solve(solverName, "solverLibraryName" , solverLibraryName);

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal ()) throw new Net2PlanException("An optimal solution was not found");

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		double [] x_p = op.getPrimalSolution("x_p").to1DArray();

		/* Update netPlan object adding the calculated routes */
		for (int p = 0 ; p < x_p.length ; p ++)
			if (x_p [p]  * h_p [p] > net2planPrecision)
				netPlan.addRoute(cpl.getDemandId(p), x_p [p] * h_p [p] , cpl.getSequenceOfLinks(p), null, null);

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		return "Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the traffic routing that minimizes " +
				" the worst link utilization using a flow-path formulation. For each demand, the set of admissible paths is composed of the ranking of (at most)  " +
				" \"k\" shortest paths in number of hops, between the demand end nodes. \"k\" is a user-defined parameter. The fraction of the demand volume that " +
				" is carried by a path that carries traffic, is constrained to be higher or equal than a user-defined parameter \"tMin\". That is, a path p can " +
				" carry no traffic, or an amount of traffic equal or greater than tMin x h_d(p), where d(p) is the demand associated to path p. Finally, the user-defined " +
				" parameter \"BIFMAX\" sets the maximum number of paths of a demand that can carry traffic.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("k", "5", "Maximum number of admissible paths per demand"));
		algorithmParameters.add(Triple.of("tMin", "0.5", "Minimum amount of traffic a path can carry"));
		algorithmParameters.add(Triple.of("BIFMAX", "2", "Maximum number of paths in which the traffic of a demand can be bifurcated"));
		algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
		algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
		return algorithmParameters;
	}




}
