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

package com.tejas.workspace.examples.netDesignAlgorithm.ca;

import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Given a network topology, a known amount of traffic carried in each link, and an available budget, this algorithm computes the
 * capacities in the links that minimize the average network delay (considering only queuing and transmission delays in the links, using the M/M/1 model),
 * with the constraint that the cost does not exceeds the available budget. The total network cost is given by the sum of the costs of the links, where the cost in each link
 * is given by \( u_e ^ \alpha \), being \( \alpha \) a positive parameter. If \( \alpha \) is between 0 and 1, the link cost function is concave respect to the capacities.
 * We solve the problem using a convex formulation, where the decision variables are the utilizations \( \rho_e \) in the links. Note that thanks to this
 * change of variable, the problem is convex (in the variables \( \rho_e \) ) even if it was not originally convex (in the variables \( u_e \) ).</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class CA_minDelayConcaveCost implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        int R = netPlan.getNumberOfRoutes();
        double[] y_e = netPlan.getLinkCarriedTrafficInErlangsVector();

        /* Basic checks */
        if (N == 0 || E == 0 || D == 0 || R == 0) throw new Net2PlanException("This algorithm requires a topology with links, a demand set and the traffic routing");

        /* Take some user-defined parameters */
        double alpha = Double.parseDouble(algorithmParameters.get("alpha"));
        double availableBudget = Double.parseDouble(algorithmParameters.get("C"));
        if (alpha <= 0) throw new Net2PlanException("Alpha parameter must be positive");

        /* Check if the problem is infeasible: the budget is too short */
        double minimumNetworkCost = 0; for (int e = 0 ; e < E ; e ++) minimumNetworkCost += Math.pow(y_e[e] , alpha);
        if (minimumNetworkCost > availableBudget) throw new Net2PlanException("The available budget is too small to have a feasible solution. Minimum budget needed: " + minimumNetworkCost);

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Add the decision variables to the problem */
        op.addDecisionVariable("rho_e", false , new int[] { 1 , E }, 0, 1);

        /* Set some input parameters */
        op.setInputParameter("y_e", y_e , "row");
        op.setInputParameter("alpha", alpha);
        op.setInputParameter("C", availableBudget);

        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum ( rho_e ./ (1-rho_e)  )");

        /* VECTORIAL FORM OF THE CONSTRAINTS */
        op.addConstraint("sum ((y_e ./ rho_e) ^ alpha ) <= C"); // the capacity constraints (E constraints)

        /* Call the solver to solve the problem */
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");
        op.solve(solverName, "solverLibraryName" , solverLibraryName);

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal ()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions */
        double [] rho_e = op.getPrimalSolution("rho_e").to1DArray();

        /* Update netPlan object  */
        for (int e = 0 ; e < E ; e ++)
            netPlan.setLinkCapacityInErlangs(e, y_e [e] / rho_e [e]);

        /* Check that the budget is used */
        double thisSolutionCost = 0; for (int e = 0 ; e < E ; e ++) thisSolutionCost += Math.pow((y_e [e]/rho_e [e]) , alpha);
        if (Math.abs(thisSolutionCost - availableBudget) > 1E-2)
                return "Ok! Warning: numerical error: the resulting network cost does not exhaust the available budget (current cost: " + thisSolutionCost + " , avaiable budget: " + availableBudget + ")";
        else
                return "Ok!";
    }

    @Override
    public String getDescription()
    {
        return "Given a network topology, a known amount of traffic carried in each link, and an available budget, this algorithm computes " +
            "the capacities in the link that minimizes the average network delay (considering only queuing and transmission delays in the links, using " +
            "the M/M/1 model), subject to the cost does not exceeds the available budget. The total network cost is given by the sum of the costs of the " +
            "links, where the cost in each link is given by  u_e ^ alpha , being  alpha a positive parameter. If alpha is between " +
            "0 and 1, the link cost is concave respect to the capacities. We solve the problem using a convex formulation, where the decision variables " +
            "are the utilizations rho_e  in the links. Note that thanks to this change of variable, the problem is convex (in the " +
            "variables rho_e ) even if it was not originally (in the variables  u_e )";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("alpha", "1", "Alpha factor for the cost function"));
        algorithmParameters.add(Triple.of("C", "100", "Available budget"));
        algorithmParameters.add(Triple.of("solverName", "ipopt", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default"));

        return algorithmParameters;
    }
}