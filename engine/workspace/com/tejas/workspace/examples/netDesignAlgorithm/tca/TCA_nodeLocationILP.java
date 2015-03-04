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

package com.tejas.workspace.examples.netDesignAlgorithm.tca;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cern.colt.Arrays;
import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

/**
 * Given a set of access nodes, this algorithm computes the subset of access nodes which have a core node located next to it (in the same place),
 * and the links access-to-core nodes, so that the network cost is minimized. This cost is given by a cost per core node (always 1) plus a cost per link,
 * given by the product of link distance and the user-defined parameter linkCostPerKm. Access-core link capacities are fixed to the user-defined parameter
 * linkCapacities. A core node cannot be connected to more than K_max access nodes, a user-defined parameter. This problem is modeled as a ILP and optimally
 * solved using a solver.
 *
 * @author Pablo Pavon-Marino, Jose Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCA_nodeLocationILP implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        final double linkCostPerKm = Double.parseDouble(algorithmParameters.get("linkCostPerKm"));
        final double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));
        final double K_max = Double.parseDouble(algorithmParameters.get("K_max"));
        final int N = netPlan.getNumberOfNodes();
        if (N == 0) throw new Net2PlanException ("The number of nodes must be positive");

        /* Compute the cost of one link */
        double [][] c_ij = new double [N][N];
        for (int i = 0 ; i < N ; i ++)
            for (int j = 0 ; j < N ; j ++)
                c_ij [i][j] = netPlan.getNodePairPhysicalDistance(i, j) * linkCostPerKm;

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Set some input parameters */
        op.setInputParameter("c_ij" , c_ij);
        op.setInputParameter("K_max", K_max);

        /* Add the decision variables to the problem */
        op.addDecisionVariable("z_j", true, new int[] { 1 , N }, 0, 1); // 1 if there is a node in this site
        op.addDecisionVariable("e_ij", true , new int[] { N , N }, 0, 1); // 1 if site i is connected to site j

        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(z_j) + sum (e_ij .* c_ij)");

        /* VECTORIAL FORM OF THE CONSTRAINTS  */
        op.addConstraint("sum(e_ij , 2) == 1"); // each site is connected to a core site
        op.addConstraint("sum(e_ij , 1) <= K_max * z_j"); // a site is connected to other, if the second is a core site

        /* Call the solver to solve the problem */
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");
        op.solve(solverName, "solverLibraryName" , solverLibraryName);

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions */
        double[] z_j = op.getPrimalSolution("z_j").to1DArray();
        double[][] e_ij = (double [][]) op.getPrimalSolution("e_ij").toArray();

        System.out.println("z_j: " + Arrays.toString(z_j));

        /* Remove all previous demands, links, protection segments, routes */
        netPlan.removeAllLinks();
        netPlan.removeAllDemands();
        netPlan.removeAllProtectionSegments();
        netPlan.removeAllRoutes();
        
        /* Save the new network links */
        for (int i = 0 ; i < N ; i ++)
        {
            for (int j = 0 ; j < N ; j ++)
            {
                if (e_ij [i][j] == 1)
                {
                    if (z_j [j] == 0) throw new RuntimeException ("Unexpected error");
                    if (i != j) { netPlan.addLink (i , j , linkCapacities , netPlan.getNodePairPhysicalDistance(i, j) , null); }
                }
            }
        }

        return "Ok! Num nodos troncales : " + DoubleUtils.sum(z_j);
    }

    @Override
    public String getDescription()
    {
        return "Given a set of access nodes, this algorithm computes the subset of access nodes which have a core node located next to it (in the same place), " +
            " and the links access-to-core nodes, so that the network cost is minimized. This cost is given by a cost per core node (always 1) plus a cost per link, " +
            " given by the product of link distance and the user-defined parameter linkCostPerKm. Access-core link capacities are fixed to the user-defined parameter " +
            " linkCapacities. A core node cannot be connected to more than K_max access nodes, a user-defined parameter. This problem is modeled as a ILP and optimally " +
            " sollved using a solver.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
        algorithmParameters.add(Triple.of("linkCostPerKm", "0.001", "The cost of 1 km of access to core link (core node cost is always 1)"));
        algorithmParameters.add(Triple.of("K_max", "5", "Maximum number of access nodes that can be connected to a core node"));
        algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));

        return algorithmParameters;
    }
}
