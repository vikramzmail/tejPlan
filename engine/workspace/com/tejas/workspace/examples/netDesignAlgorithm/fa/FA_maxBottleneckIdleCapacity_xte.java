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
 * Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the destination-based traffic routing that
 * minimizes the network congestion. Destination-based routing means that the routing could be implemented i.e. in an IP network.
 * In this algorithm, congestion is minimized by maximizing the idle link capacity in the bottleneck (the link with less idle capacity).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class FA_maxBottleneckIdleCapacity_xte implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        double[] u_e = netPlan.getLinkCapacityInErlangsVector();
        double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        double[][] trafficMatrix = netPlan.getTrafficMatrix();
        /* The diagonal in the traffic matrix contains minus the amount of traffic destined to that node */
        for (int n1 = 0 ; n1 < N ; n1 ++) for (int n2 = 0 ; n2 < N ; n2 ++) if (n1 == n2) continue; else trafficMatrix [n2][n2] -= trafficMatrix [n1][n2];

        /* Basic checks */
        if (N == 0 || D == 0 || E == 0) throw new Net2PlanException("This algorithm requires a topology with links, and a demand set");

        /* Remove all routes in current netPlan object */
        netPlan.removeAllRoutes();
        netPlan.removeAllProtectionSegments();

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Add the decision variables to the problem */
        op.addDecisionVariable("x_te", false, new int[] { N, E }, 0, Double.MAX_VALUE);
        op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE);

        /* Set some input parameters */
        op.setInputParameter("u_e", u_e, "row");
        op.setInputParameter("h_d", h_d, "row");

        /* Sets the objective function */
        op.setObjectiveFunction("maximize", "u");

        /* VECTORIAL FORM OF THE CONSTRAINTS [EQUIVALENT TO THE FOR-LOOP FORM, COMMENT ONE OF BOTH] */
        op.setInputParameter("A_ne", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
        op.setInputParameter("A_nd", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
        op.setInputParameter("TM", trafficMatrix);
        op.addConstraint("A_ne * (x_te') == TM"); // the flow-conservation constraints (NxN constraints)
        op.addConstraint("sum(x_te,1) <= u_e - u"); // the capacity constraints (E constraints)

        /* FOR-LOOP BASED FORM OF THE CONSTRAINTS [EQUIVALENT TO THE VECTORIAL FORM, COMMENT ONE OF BOTH] */
//		/* Flow conservation constraints */
//		for (int n = 0 ; n < N ; n ++)
//		{
//			int [] outLinks = netPlan.getNodeOutgoingLinks(n);
//			int [] inLinks = netPlan.getNodeIncomingLinks(n);
//			op.setInputParameter("delta_out", outLinks , "row");
//			op.setInputParameter("delta_in", inLinks , "row");
//			for (int t = 0 ; t < N ; t ++)
//			{
//				op.setInputParameter("t", t);
//				op.addConstraint ("sum(x_te(t,delta_out)) - sum(x_te(t,delta_in)) == " + trafficMatrix[n][t]);
//			}
//		}
//		/* Capacity - min congestion constraints */
//		for (int e = 0 ; e < E ; e ++)
//			op.addConstraint("sum(x_te(all," + e + ")) <= " + u_e[e] + " - u");

        /* Call the solver to solve the problem */
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");
        op.solve(solverName, "solverLibraryName" , solverLibraryName);

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions */
        double[][] x_te = (double[][]) op.getPrimalSolution("x_te").toArray();

        /* Convert the x_te variables into a set of routes for each demand */
        ArrayList<Integer> demands_p = new ArrayList<Integer>();
        ArrayList<int[]> seqLinks_p = new ArrayList<int[]>();
        ArrayList<Double> x_p = new ArrayList<Double>();
        double[][] f_te = GraphUtils.convert_xte2fte(netPlan.getLinkTable(), x_te);
        if (GraphUtils.checkRouting_fte(netPlan.getLinkTable(), f_te) != 0) throw new Net2PlanException("The routing has cycles");
        GraphUtils.convert_fte2xp(netPlan.getLinkTable(), netPlan.getDemandTable(), netPlan.getDemandOfferedTrafficInErlangsVector(), f_te, demands_p, seqLinks_p, x_p);

        /* Update netPlan object adding the calculated routes */
        for (int p = 0; p < x_p.size(); p++)
                netPlan.addRoute(demands_p.get(p), x_p.get(p), seqLinks_p.get(p), null, null);

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
            StringBuilder aux = new StringBuilder();
            aux.append("Given a network topology, the capacities in the links and the offered traffic, this algorithm obtains the destination-based traffic " +
                            "routing that minimizes the network congestion. Destination-based routing means that the routing could be implemented i.e. in an IP network. In " +
                            "this algorithm, congestion is minimized by maximizing the idle link capacity in the bottleneck (the link with less idle capacity). ");
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
