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

import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Given a set of nodes N, this algorithm computes the bidirectional ring with the minimum length (in km) in all its nodes, solving with an ILP (Integer Linear Program)
 * the associated Traveling Salesman Problem (TSP) instance. All the links are set to have the capacity passed as input parameter.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TCA_minLengthBidirectionalRingILP implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        final int N = netPlan.getNumberOfNodes();
        final int auxD = N-1;
        final int auxE = N*(N-1);
        double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));

        /* Basic checks */
        if (N == 0) throw new Net2PlanException("This algorithm requires a topology with nodes");

        /* Create an auxiliar demand table. N-1 demands, from node 0 to nodes 1..N-1 */
        int [][] aux_demandTable = new int [auxD][2]; // auxiliar demand table
        for (int d = 0 ; d < auxD ; d ++) { aux_demandTable [d][0] = 0; aux_demandTable [d][1] = d+1; }
        /* Create an auxiliar link table (unidirectional links). All nodes interconnected between them  */
        int [][] aux_linkTable = new int [auxE][2]; // auxiliar link table
        double [] d_e = new double [auxE]; // distance between two nodes
        int linkId = 0;
        for (int n1 = 0 ; n1 < N ; n1 ++)
        {
            for (int n2 = 0 ; n2 < N ; n2 ++)
            {
                if (n1 == n2) continue;
                aux_linkTable [linkId][0] = n1; aux_linkTable [linkId][1] = n2;
                d_e [linkId++] = netPlan.getNodePairPhysicalDistance(n1, n2);
            }
        }

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Set some input parameters */
        op.setInputParameter("d_e", d_e , "row");

        /* Add the decision variables to the problem */
        op.addDecisionVariable("x_e", true, new int[] { 1 , auxE }, 0, 1); // 1 if the link is up
        op.addDecisionVariable("x_de", false , new int[] { auxD , auxE }, 0, 1); // 1 if traffic of aux demand d is in link e

        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(d_e .* x_e)");

        /* VECTORIAL FORM OF THE CONSTRAINTS [EQUIVALENT TO THE FOR-LOOP FORM, COMMENT ONE OF BOTH] */
        /* Compute some matrices required for writing the constraints */
        double [][] A_e = new double [N][auxE]; // 1 if node n is the initial node of link e, -1 if it is the end node
        double [][] A_e_outLinks = new double [N][auxE]; // 1 if node n is the initial node of link e
        double [][] A_e_inLinks = new double [N][auxE]; // 1 if node n is the end node of link e
        for (int e = 0 ; e < auxE ; e ++)
                { int a_e = aux_linkTable [e][0]; int b_e = aux_linkTable [e][1]; A_e [a_e][e] = 1; A_e [b_e][e] = -1; A_e_outLinks [a_e][e] = 1; A_e_inLinks [b_e][e] = 1; }
        op.setInputParameter("A_d", GraphUtils.getIncidenceMatrix(aux_demandTable, N)); // 1 if node n is the initial node of demand d, -1 if it is the end node
        op.setInputParameter("A_e", A_e);
        op.setInputParameter("A_e_outLinks", A_e_outLinks);
        op.setInputParameter("A_e_inLinks", A_e_inLinks);
        op.setInputParameter("ones_D", DoubleUtils.ones(auxD) , "column"); // a column of ones, a coordinate for each demand
        /* Problem constraints */
        op.addConstraint("A_e * (x_de') == A_d"); // the flow-conservation constraints (NxN constraints). This is to assure that it is possible to reach any node from node 0. Thus the resulting topology is connected
        op.addConstraint("A_e_inLinks * x_e' == 1"); // each node has one input link
        op.addConstraint("A_e_outLinks * x_e' == 1"); // each node has one output link
        op.addConstraint("x_de <= ones_D * x_e"); // if a link does not exist, it cannot carry traffic

        /* Call the solver to solve the problem */
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");
        op.solve(solverName, "solverLibraryName" , solverLibraryName);

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions */
        double[] x_e = op.getPrimalSolution("x_e").to1DArray();

        /* Remove all previous demands, links, protection segments, routes */
        netPlan.removeAllLinks();
        netPlan.removeAllDemands();
        netPlan.removeAllProtectionSegments();
        netPlan.removeAllRoutes();
        /* Save the new network links */
        for (int e = 0; e < auxE ; e ++)
        {
            if (x_e [e] == 0) continue;
            int a_e = aux_linkTable [e][0]; int b_e = aux_linkTable [e][1];
            /* to create a bidirectional topology, I add two links, one for each direction  */
            netPlan.addLink (a_e , b_e , linkCapacities , netPlan.getNodePairPhysicalDistance(a_e , b_e ) , null);
            netPlan.addLink (b_e , a_e , linkCapacities , netPlan.getNodePairPhysicalDistance(b_e , a_e ) , null);
        }

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
        return "Given a set of nodes N, this algorithm computes the bidirectional ring with the minimum length (in km) in all its nodes, solving with an ILP (Integer " +
            "Linear Program) the associated Traveling Salesman Problem (TSP) instance. All the links are set to have the capacity passed as input parameter.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
        algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));

        return algorithmParameters;
    }

}
