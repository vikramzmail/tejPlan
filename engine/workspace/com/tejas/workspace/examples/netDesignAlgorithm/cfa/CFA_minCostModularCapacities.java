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

package com.tejas.workspace.examples.netDesignAlgorithm.cfa;

import java.util.ArrayList;
import java.util.List;
import com.jom.OptimizationProblem;
import java.util.Map;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a network topology, and the offered traffic, this algorithm obtains the traffic routing and the (modular) capacities in the links that minimizes the
 * link costs. The capacity of a link is constrained to be the aggregation of integer multiples of modules of capacities {0.15 , 0.6 , 2.4 , 9.6} Gbps, and prices
 * {1, 2, 4, 8} monetary units. Link utilization is limited by the user-defined parameter rhoMax.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class CFA_minCostModularCapacities implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	/* Initialize some variables */
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int D = netPlan.getNumberOfDemands();
	double R = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));
	double net2planPrecision = Double.parseDouble(net2planParameters.get("precisionFactor"));
	double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
	double [] h_d_Gbps = DoubleUtils.mult(h_d , R / 1e9);
	double [] p_i = new double [] { 1 , 2 , 4 , 8};
	double [] u_i = new double [] { 0.15 , 0.6 , 2.4 , 9.6 };

	/* Basic checks */
	if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

	/* Take some user-defined parameters */
	double rhoMax = Double.parseDouble(algorithmParameters.get("rhoMax"));
	if ((rhoMax <= 0) || (rhoMax > 1)) throw new Net2PlanException("rhoMax parameter must be positive, and equal or lower than one");

	/* Create the optimization problem object (JOM library) */
	OptimizationProblem op = new OptimizationProblem();

	/* Add the decision variables to the problem */
	op.addDecisionVariable("x_de", false , new int[] { D , E }, 0, 1);
	op.addDecisionVariable("y_e", false , new int[] { 1 , E }, 0, Double.MAX_VALUE);
	op.addDecisionVariable("n_ei", true , new int[] { E , 4 }, 0, Double.MAX_VALUE);

	/* Set some input parameters */
	op.setInputParameter("p_i", p_i , "row");
	op.setInputParameter("u_i", u_i , "row");
	op.setInputParameter("h_d_Gbps", h_d_Gbps , "row");
	op.setInputParameter("rhoMax", rhoMax);

	/* Sets the objective function */
	op.setObjectiveFunction("minimize", "sum (n_ei * p_i')");

	/* VECTORIAL FORM OF THE CONSTRAIN TS  */
	op.setInputParameter("A_e", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
	op.setInputParameter("A_d", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
	op.addConstraint("A_e * (x_de') == A_d"); // the flow-conservation constraints (NxD constraints)
	op.addConstraint("y_e == h_d_Gbps * x_de"); // computing the traffic in each link (E constraints)
	op.addConstraint("y_e <= rhoMax * u_i * n_ei'"); // the utilization in the link is below rhoMax

	/* Call the solver to solve the problem */
	String solverName = algorithmParameters.get("solverName");
	String solverLibraryName = algorithmParameters.get("solverLibraryName");
	op.solve(solverName, "solverLibraryName" , solverLibraryName);

	/* If an optimal solution was not found, quit */
	if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

	/* Retrieve the optimum solutions */
	double [][] n_ei = (double [][]) op.getPrimalSolution("n_ei").toArray();
	double [][] x_de = (double [][]) op.getPrimalSolution("x_de").toArray();

	/* Update netPlan object: routing */
	/* Remove previous routes */
	netPlan.removeAllRoutes();
	netPlan.removeAllProtectionSegments();

	/* Convert the x_de variables into a set of routes for each demand  */
	ArrayList<Integer> demands = new ArrayList<Integer> ();
	ArrayList<int []> seqLinks = new ArrayList<int []> ();
	ArrayList<Double> x_p = new ArrayList<Double> ();
	GraphUtils.convert_xde2xp(netPlan.getLinkTable() , netPlan.getDemandTable() , x_de , demands , seqLinks , x_p);

	/* Update netPlan object adding the calculated routes */
	for (int p = 0 ; p < x_p.size() ; p ++)
		if (x_p.get(p) * h_d [demands.get(p)] > net2planPrecision)
			netPlan.addRoute(demands.get(p), x_p.get(p) * h_d [demands.get(p)] , seqLinks.get(p), null, null);

	/* Update netPlan object: link capacities  */
	for (int e = 0 ; e < E ; e ++)
	{
	    double thisLinkCapacity_Gbps = 0;
	    for (int i = 0 ; i < u_i.length ; i ++) { thisLinkCapacity_Gbps += u_i [i] * n_ei [e][i]; }
	    netPlan.setLinkCapacityInErlangs(e, thisLinkCapacity_Gbps * 1e9 / R);
	}

	/* Check that the budget is used */
	return "Ok! Cost: " + op.getOptimalCost();
    }

    @Override
    public String getDescription()
    {
	return "Given a network topology, and the offered traffic, this algorithm obtains the traffic routing and the (modular) capacities in the links that minimizes the " +
	    "link costs. The capacity of a link is constrained to be the aggregation of integer multiples of modules of capacities {0.15 , 0.6 , 2.4 , 9.6} Gbps, and prices " +
	    "{1, 2, 4, 8} monetary units. Link utilization is limited by the user-defined parameter rhoMax.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("rhoMax", "0.5", "Maximum utilization allowed in the links."));
	algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
	algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
	return algorithmParameters;
    }
}
