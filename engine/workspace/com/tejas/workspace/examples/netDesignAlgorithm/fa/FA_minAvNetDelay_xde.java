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
 * Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the traffic routing that minimizes
 * the average network delay T, estimated according to the formula: \( T = \frac{1}{\sum_d h_d} \sum_e y_e T_e \), where \( h_d \) is
 * the offered traffic by demand \( d \) [ in bps ], and \( T_e \) is the average link delay estimated for link \( e \), given
 * by \( d_e + \frac{L}{u_e - y_e} \). For each link \( e \), \( d_e \) is the propagation delay, \( y_e \) is the average
 * traffic in the link and \( u_e \) is the link capacity (both in bps). \( L \) is the average packet length in bits.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_minAvNetDelay_xde implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		double binaryRatePerErlang = Double.parseDouble(net2planParameters.get ("binaryRateInBitsPerSecondPerErlang"));
		double averagePacketLengthInBits = 8 * Double.parseDouble(net2planParameters.get ("averagePacketLengthInBytes"));
		double [] u_e = netPlan.getLinkCapacityInErlangsVector();
		double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		double [] d_e_secs = netPlan.getLinkPropagationDelayInSecondsVector();

		/* Basic checks */
		if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

		/* Remove all routes in current netPlan object */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();

		/* Add the decision variables to the problem */
		op.addDecisionVariable("x_de", false , new int[] { D , E }, 0, 1);
		op.addDecisionVariable("y_e", false , new int[] { 1 , E }, 0, Double.MAX_VALUE);

		/* Set some input parameters */
		op.setInputParameter("u_e", u_e , "row");
		op.setInputParameter("h_d", h_d , "row");
		op.setInputParameter("d_e_secs",  d_e_secs , "row");
		op.setInputParameter("L",  averagePacketLengthInBits);
		op.setInputParameter("R",  binaryRatePerErlang);


		/* Sets the objective function */
		op.setObjectiveFunction("minimize", "sum( y_e .* (d_e_secs + (L./R) * (1 ./ (u_e - y_e)))  )");

		/* VECTORIAL FORM OF THE CONSTRAINTS [EQUIVALENT TO THE FOR-LOOP FORM, COMMENT ONE OF BOTH]  */
		op.setInputParameter("A_ne", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
		op.setInputParameter("A_nd", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
		op.addConstraint("A_ne * (x_de') == A_nd"); // the flow-conservation constraints (NxD constraints)
		op.addConstraint("y_e == h_d * x_de"); // computing the traffic in each link (E constraints)
		op.addConstraint("y_e <= u_e"); // the capacity constraints (E constraints)

		/* FOR-LOOP BASED FORM OF THE CONSTRAINTS [EQUIVALENT TO THE VECTORIAL FORM, COMMENT ONE OF BOTH]  */
//		/* Flow conservation constraints */
//		for (int n = 0 ; n < N ; n ++)
//		{
//			int [] outLinks = netPlan.getNodeOutgoingLinks(n);
//			int [] inLinks = netPlan.getNodeIncomingLinks(n);
//			op.setInputParameter("delta_out", outLinks , "row");
//			op.setInputParameter("delta_in", inLinks , "row");
//			for (int d = 0 ; d < D ; d ++)
//			{
//				op.setInputParameter("d", d);
//				int a_d = netPlan.getDemandIngressNode(d);
//				int b_d = netPlan.getDemandEgressNode(d);
//				double divergence = (a_d == n)? 1 : (b_d == n)? -1 : 0;
//				op.addConstraint ("sum(x_de(d,delta_out)) - sum(x_de(d,delta_in)) == " + divergence);
//			}
//		}
//		/* the constraints that compute the traffic in each link (E constraints) */
//		for (int e = 0 ; e < E ; e ++)
//			op.addConstraint("y_e(" + e + ") == h_d * x_de(all," + e + ")");
//		for (int e = 0 ; e < E ; e ++)
//			op.addConstraint("y_e (" + e + ") <= u_e (" + e + ")");


		/* Call the solver to solve the problem */
		String solverName = algorithmParameters.get("solverName");
		String solverLibraryName = algorithmParameters.get("solverLibraryName");
		op.solve(solverName, "solverLibraryName" , solverLibraryName);

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal ()) throw new Net2PlanException ("An optimal solution was not found");

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		double [][] x_de = (double [][]) op.getPrimalSolution("x_de").toArray();

		/* Convert the x_de variables into a set of routes for each demand  */
		ArrayList<Integer> demands = new ArrayList<Integer> ();
		ArrayList<int []> seqLinks = new ArrayList<int []> ();
		ArrayList<Double> x_p = new ArrayList<Double> ();
		GraphUtils.convert_xde2xp(netPlan.getLinkTable() , netPlan.getDemandTable() , x_de , demands , seqLinks , x_p);

		/* Update netPlan object adding the calculated routes */
		for (int p = 0 ; p < x_p.size() ; p ++)
			netPlan.addRoute(demands.get(p), x_p.get(p) * h_d [demands.get(p)] , seqLinks.get(p), null, null);

		return "Ok!";
	}

	@Override
	public String getDescription()
	{
		StringBuilder aux = new StringBuilder();
		aux.append(" Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the traffic routing that" +
				" minimizes the average network delay T, estimated according to the formula: \\( T = \frac{1}{\\sum_d h_d} \\sum_e y_e T_e \\), where \\( h_d \\) " +
				"is the offered traffic by demand \\( d \\) [ in bps ], and \\( T_e \\) is the average link delay estimated for link \\( e \\), given" +
				" by \\( d_e + \\frac{L}{u_e - y_e} \\). For each link \\( e \\), \\( d_e \\) is the propagation delay, \\( y_e \\) is the average traffic in " +
				"the link and \\( u_e \\) is the link capacity (both in bps). \\( L \\) is the average packet length in bits.");
		return aux.toString();
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("solverName", "ipopt", "The solver name to be used by JOM"));
		algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
		return algorithmParameters;
	}




}
