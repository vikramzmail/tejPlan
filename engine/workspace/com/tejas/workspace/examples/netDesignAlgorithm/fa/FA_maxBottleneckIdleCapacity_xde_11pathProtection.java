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

import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains a link-disjoint primary and backup path for the
 * traffic of each demand, that minimizes the network congestion, with or without the constraint of non-bifurcated routing, solving an ILP
 * flow-link formulation. In this algorithm, congestion is minimized by maximizing the idle link capacity
 * in the bottleneck (the link with less idle capacity).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_maxBottleneckIdleCapacity_xde_11pathProtection implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		double [] u_e = netPlan.getLinkCapacityInErlangsVector();
		double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();

		/* Basic checks */
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("x_de", true , new int[] { D , E }, 0, 1); // 1 if the primary path of demand d, traverses link e
		op.addDecisionVariable("xx_de", true , new int[] { D , E }, 0, 1);  // 1 if the backup path of demand d, traverses link e
		op.addDecisionVariable("u", false, new int[] { 1 , 1 }, 0, Double.MAX_VALUE); // idle capacity in the bottleneck link

		/* Set some input parameters */
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("h_d", h_d , "row");

		/* Sets the objective function */
		op.setObjectiveFunction("maximize", "u");

		/* VECTORIAL FORM OF THE CONSTRAINTS [EQUIVALENT TO THE FOR-LOOP FORM, COMMENT ONE OF BOTH]  */
		op.setInputParameter("A_ne", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
		op.setInputParameter("A_nd", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
		op.addConstraint("A_ne * (x_de') == A_nd"); // the flow-conservation constraints for the primary path (NxD constraints)
		op.addConstraint("A_ne * (xx_de') == A_nd"); // the flow-conservation constraints for the backup path (NxD constraints)
		op.addConstraint("x_de + xx_de <= 1"); // the primary and backup path are link disjoint (DxE constraints)
		op.addConstraint("h_d * (x_de + xx_de) <= u_e - u"); // the capacity constraints summing primary and backup paths (E constraints)

		/* Call the solver to solve the problem */
		String solverName = algorithmParameters.get("solverName");
		String solverLibraryName = algorithmParameters.get("solverLibraryName");
		op.solve(solverName, "solverLibraryName" , solverLibraryName);

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

		/* Retrieve the optimum solutions */
		double [][] x_de = (double [][]) op.getPrimalSolution("x_de").toArray();
		double [][] xx_de = (double [][]) op.getPrimalSolution("xx_de").toArray();

		/* Convert the x_de variables into a set of routes for each demand  */
		ArrayList<Integer> primary_demands = new ArrayList<Integer> ();
		ArrayList<int []> primary_seqLinks = new ArrayList<int []> ();
		ArrayList<Double> primary_x_p = new ArrayList<Double> ();
		GraphUtils.convert_xde2xp(netPlan.getLinkTable() , netPlan.getDemandTable() , x_de , primary_demands , primary_seqLinks , primary_x_p);

		ArrayList<Integer> backup_demands = new ArrayList<Integer> ();
		ArrayList<int []> backup_seqLinks = new ArrayList<int []> ();
		ArrayList<Double> backup_x_p = new ArrayList<Double> ();
		GraphUtils.convert_xde2xp(netPlan.getLinkTable() , netPlan.getDemandTable() , xx_de , backup_demands , backup_seqLinks , backup_x_p);

		/* Remove all routes in current netPlan object */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Update netPlan object adding the calculated routes */
		if (primary_demands.size() != D) throw new Net2PlanException ("Unexpected error");
		if (backup_demands.size() != D) throw new Net2PlanException ("Unexpected error");
		for (int d = 0 ; d < D ; d ++)
		{
			/* for each demand, there is one primary and one backup path*/
			int primary_p = primary_demands.indexOf(d); if (primary_p == -1) throw new RuntimeException ("Unexpected error");
			int backup_p = backup_demands.indexOf(d); if (backup_p == -1) throw new RuntimeException ("Unexpected error");
			int protectionSegmentId = netPlan.addProtectionSegment(backup_seqLinks.get(backup_p), h_d [d] , null); // add the protection segment (backup path)
			netPlan.addRoute(d , h_d [d] , primary_seqLinks.get(primary_p), new int [] {protectionSegmentId} , null); // add the primary route (protected)
		}

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		StringBuilder aux = new StringBuilder();
		aux.append("Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains a " +
				"link-disjoint primary and backup path for the traffic of each demand, that minimizes the network congestion, with or without the " +
				"constraint of non-bifurcated routing, solving an ILP flow-link formulation. In this algorithm, congestion is minimized by maximizing the " +
				"idle link capacity in the bottleneck (the link with less idle capacity). ");
		return aux.toString();
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
		algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
		return algorithmParameters;
	}




}
