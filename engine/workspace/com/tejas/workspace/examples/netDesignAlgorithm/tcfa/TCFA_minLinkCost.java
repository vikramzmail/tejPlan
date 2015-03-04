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

package com.tejas.workspace.examples.netDesignAlgorithm.tcfa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a set of nodes N, and a traffic demand, this algorithm computes the set of links, their capacities and the routing of traffic that optimally minimizes
 * the network cost given by a fixed cost per link, plus a variable cost with link capacities. Between two nodes, at most one link can exist. The maximum link
 * capacity is Umax, a user-defined parameter. Link fixed cost is given by link distance by fixedCostFactorPerKm, a user-defined parameter.
 * Link variable cost is given by link distance, by link capacity, by variableCostFactorPerKmAndTrafficUnit, a user-defined parameter. The problem is modeled
 * with JOM as a MILP, and optimally solved by a external solver.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCFA_minLinkCost implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        final double net2planPrecision = Double.parseDouble(net2planParameters.get("precisionFactor"));
        final double U_max = Double.parseDouble(algorithmParameters.get("U_max"));
        final double fixedCostFactorPerKm = Double.parseDouble(algorithmParameters.get("fixedCostFactorPerKm"));
        final double variableCostFactorPerKmAndTrafficUnit = Double.parseDouble(algorithmParameters.get("variableCostFactorPerKmAndTrafficUnit"));
        final int N = netPlan.getNumberOfNodes();
        final int D = netPlan.getNumberOfDemands();
        if (N == 0) throw new Net2PlanException ("The number of nodes must be positive");
        if (D == 0) throw new Net2PlanException ("The number of demands must be positive");
        final double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();

        /* Create a full mesh of links in the netPlan object */
        netPlan.removeAllProtectionSegments();
        netPlan.removeAllRoutes();
        netPlan.removeAllLinks();
        for (int i = 0 ; i < N ; i ++)
            for (int j = 0 ; j < N ; j ++)
                if (i != j)
                    netPlan.addLink (i , j , U_max , netPlan.getNodePairPhysicalDistance(i, j) , null);

        /* Compute the distances of the potential links */
        final int E_fm = netPlan.getNumberOfLinks();
        final double [] d_e = netPlan.getLinkLengthInKmVector();

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Set some input parameters */
        op.setInputParameter("U_max", U_max);
        op.setInputParameter("fc", fixedCostFactorPerKm);
        op.setInputParameter("vc", variableCostFactorPerKmAndTrafficUnit);
        op.setInputParameter("h_d", h_d , "row");
        op.setInputParameter("d_e", d_e , "row");

        /* Add the decision variables to the problem */
        op.addDecisionVariable("p_e", true , new int[] { 1 , E_fm }, 0, 1); // 1 if there is a link from node i to node j, 0 otherwise
        op.addDecisionVariable("u_e", false , new int[] { 1 , E_fm }, 0, U_max); // 1 if there is a link from node i to node j, 0 otherwise
        op.addDecisionVariable("x_de", false , new int[] { D , E_fm }, 0, U_max); // 1 if there is a link from node i to node j, 0 otherwise

        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(fc * d_e .* p_e) + sum (vc * d_e .* u_e)");

        /* VECTORIAL FORM OF THE CONSTRAINTS */
        /* Compute some matrices required for writing the constraints */
        op.setInputParameter("A_e", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
        op.setInputParameter("A_d", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
        op.addConstraint("A_e * (x_de') == A_d"); // the flow-conservation constraints (NxD constraints)
        op.addConstraint("h_d * x_de <= u_e"); // the capacity constraints (E constraints)
        op.addConstraint("u_e <= U_max * p_e"); // the capacity constraints (E constraints)

        /* Call the solver to solve the problem */
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");
        op.solve(solverName, "solverLibraryName" , solverLibraryName);

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions */
        final double [] p_e = op.getPrimalSolution("p_e").to1DArray();
        final double [] u_e = op.getPrimalSolution("u_e").to1DArray();
        final double [][] x_de = (double [][]) op.getPrimalSolution("x_de").toArray();

        /* Capacities: Fix the capacities in all the links */
        for (int e = 0 ; e < E_fm ; e ++)
            netPlan.setLinkCapacityInErlangs(e, u_e [e]);

        /* Routes: Convert the x_de variables into a set of routes for each demand and add the routes */
        ArrayList<Integer> demands = new ArrayList<Integer> ();
        ArrayList<int []> seqLinks_fm = new ArrayList<int []> (); // the sequence of links, where the linkId numbering is with respect to the full-mesh topology
        ArrayList<Double> x_p = new ArrayList<Double> ();
        GraphUtils.convert_xde2xp(netPlan.getLinkTable() , netPlan.getDemandTable() , x_de , demands , seqLinks_fm , x_p);
        for (int p = 0 ; p < x_p.size() ; p ++)
                if (x_p.get(p) * h_d [demands.get(p)] > net2planPrecision)
                        netPlan.addRoute(demands.get(p), x_p.get(p) * h_d [demands.get(p)] , seqLinks_fm.get(p), null, null);

        /* Links: Remove those links which are not used in the design */
        /* The design made is such that the links removed are not used in any route, and have capacity 0 */
        /* Important: Removing the links must be done AFTER setting the link capacities and the routes in netPlan object. This is
         * because the indexes of the links in u_e, x_de are renumbered in netPlan after calling to removeLinks. If you
         * call removeLinks before setting the capacities and routes, you have to make to "manually" make the renumbering of the links ids
         * when setting the capacities and routes... it is easier if you let Net2Plan handle that */
        final int [] linkIdsToRemove = DoubleUtils.find (p_e , 0 , Constants.SearchType.ALL);
        netPlan.removeLinks(linkIdsToRemove);

        return "Ok! Num links: " + netPlan.getNumberOfLinks() + ", total capacity: " + DoubleUtils.sum(netPlan.getLinkCapacityInErlangsVector());
    }

    @Override
    public String getDescription()
    {
        return "Given a set of nodes N, and a traffic demand, this algorithm computes the set of links, their capacities and the routing of traffic that " +
            "optimally minimizes the network cost given by a fixed cost per link, plus a variable cost with link capacities. Between two nodes, " +
            "at most one link can exist. The maximum link capacity is Umax, a user-defined parameter. Link fixed cost is given by link distance " +
            "by fixedCostFactorPerKm, a user-defined parameter. Link variable cost is given by link distance, by link capacity, by " +
            "variableCostFactorPerKmAndTrafficUnit, a user-defined parameter. The problem is modeled with JOM as a MILP, and optimally solved by a external solver.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("fixedCostFactorPerKm", "1", "Fixed cost factor per km of a link (the cost of a link of 1 km and 0 capacity)"));
        algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
        algorithmParameters.add(Triple.of("U_max", "10", "The capacities to set in the links"));
        algorithmParameters.add(Triple.of("variableCostFactorPerKmAndTrafficUnit", "1", "Variable cost factor per km and traffic unit of a link (the cost of a link of 1 km and 1 unit of capacity, and 0 of fixed cost)"));

        return algorithmParameters;
    }
}