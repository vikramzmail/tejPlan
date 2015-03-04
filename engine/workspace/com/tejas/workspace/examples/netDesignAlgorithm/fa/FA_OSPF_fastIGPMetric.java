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

import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.IPUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>Routes traffic with ECMP getting link weights using two different heuristics from [Umit2009].</p>
 * 
 * <p><b>Important</b>: Integrality of link metrics (required by real-world IGP protocols) 
 * cannot be ensured by using these methods.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, February 2014
 * @see [Umit2009] H. Ümit, "Techniques and Tools for Intra-domain Traffic Engineering," Ph.D. Thesis, Université catholique de Louvain (Belgium), December 2009
 */
public class FA_OSPF_fastIGPMetric implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        
        // Basic checks
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a demand set are required");

        // Remove all routes in current netPlan object
        netPlan.removeAllRoutes();
        netPlan.removeAllProtectionSegments();

        String method = algorithmParameters.get("method");
        double precisionFactor = Double.parseDouble(net2planParameters.get("precisionFactor"));
        String solverName = algorithmParameters.get("solverName");
        String solverLibraryName = algorithmParameters.get("solverLibraryName");

        Pair<double[], Double> output;
        
        switch(method)
        {
            case "colGen":
                output = procedureColGen(netPlan, solverName, solverLibraryName, precisionFactor);
                break;
                
            case "dv":
                output = procedureDV(netPlan, solverName, solverLibraryName, precisionFactor);
                break;
                
            default:
                throw new Net2PlanException("Unknown method " + method + ". Valid names are 'colGen' and 'dv'");
        }
        
        double[] w_e = output.getFirst();
        double igpCost = output.getSecond();
        
	int[][] linkTable = netPlan.getLinkTable();
	double[][] f_te = IPUtils.computeECMPRoutingTableMatrix(linkTable, w_e, N);

	IPUtils.setLinkWeightAttributes(netPlan, w_e);
        IPUtils.setRoutesFromRoutingTableMatrix(netPlan, f_te);
        
        return "Ok! IGP cost = " + igpCost;
    }

    @Override
    public String getDescription()
    {
        StringBuilder description = new StringBuilder();
        String NEW_LINE = StringUtils.getLineSeparator();
        
        description.append("This algorithm provides two heuristic methods, presented in [Umit2009], to deal with the IGP weight setting problem with the ECMP rule (NP-hard).");
        description.append(NEW_LINE);
        description.append("These heuristics are based on dual properties of multi-commodity flow (MCF) problems to obtain link metrics in a fast and efficient manner.");
        description.append(NEW_LINE);
        description.append("The first method (colGen) is a column generation heuristic, initialized with non-bifurcated min-hop routing, which adds new paths until no more profitable path can be added. A path-based form of the MCF problem is used into the Restricted Master Problem.");
        description.append(NEW_LINE);
        description.append("The second method (dv) gets link metrics as the dual variables of the link constraints of a link-based MCF problem.");
        description.append(NEW_LINE);
        description.append("Important: Integrality of link metrics (required by real-world IGP protocols) cannot be ensured by using these methods.");
        description.append(NEW_LINE);        
        description.append("[Umit2009] H. Ümit, \"Techniques and Tools for Intra-domain Traffic Engineering,\" Ph.D. Thesis, Université catholique de Louvain (Belgium), December 2009");
        
        return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
        parameters.add(Triple.of("method", "colGen", "Heuristic method to obtain IGP weights. Valid values are 'colGen' (column generation) and 'dv' (dual variables)"));
	parameters.add(Triple.of("solverName", "cplex", "The solver name to be used by JOM"));
	parameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default"));

	return parameters;
    }

    /**
     * Performs the pricing stage of the column generation algorithm.
     * 
     * @param linkTable Link table (each row has two items, the first one is the origin node, and the second one is the destination node)
     * @param demandId Demand identifier
     * @param ingressNodeId Ingress node for the demand
     * @param egressNodeId Egress node for the demand
     * @param pi_e Dual variables for the link capacity constraints
     * @param lambda_d Dual variables for the carried traffic constraints
     * @return A new path (represented as a sequence of links) to be added, or an empty array if no path would improve the objective function
     * @since 0.2.3
     */
    private static int[] pricing(int[][] linkTable, int demandId, int ingressNodeId, int egressNodeId, double[] pi_e, double[] lambda_d)
    {
        int[] sp = GraphUtils.getShortestPath(linkTable, ingressNodeId, egressNodeId, pi_e);
        if (sp.length > 0)
        {
            double pathLength = GraphUtils.convertPath2PathCost(sp, pi_e);
            if (pathLength < lambda_d[demandId]) return sp;
        }
        
        return new int[0];
    }
    
    /**
     * Obtains the IGP weights using the column-generation heuristic.
     * 
     * @param netPlan Network design
     * @param solverName Solver name
     * @param solverLibraryName Solver library path (optional, empty means default path)
     * @param precisionFactor Precision factor
     * @return IGP weight for each link, and the IGP cost penalty
     * @since 0.2.3
     */
    private static Pair<double[], Double> procedureColGen(NetPlan netPlan, String solverName, String solverLibraryName, double precisionFactor)
    {
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        
        int[][] linkTable = netPlan.getLinkTable();
        double[] u_e = netPlan.getLinkCapacityInErlangsVector();
        double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        
        // Initial weight value (min-hop routing)
        double[] w_e = DoubleUtils.ones(E);
        double c_best = Double.MAX_VALUE;
        
        // Create the candidate path list (one path per demand)
        CandidatePathList cpl = new CandidatePathList(netPlan, w_e, "K", "1");
        for(int demandId = 0; demandId < D; demandId++)
        {
            int[] pathIds = cpl.getPathsPerDemand(demandId);
            if (pathIds.length == 0) throw new Net2PlanException("No candidate paths found for demand " + demandId);
        }
        
        do
        {
            OptimizationProblem op = solveLP_xp(u_e, h_d, cpl, solverName, solverLibraryName);
            Pair<double[], double[]> dualVariables = retrieveDual(op, precisionFactor);
            
            double[] pi_e = dualVariables.getFirst();
            double[] lambda_d = dualVariables.getSecond();
            double c = op.getOptimalCost();
            
            if (c < c_best)
            {
                w_e = DoubleUtils.copy(pi_e);
                c_best = c;
            }
            
            boolean pathAdded = false;
            
            for(int demandId = 0; demandId < D; demandId++)
            {
                int ingressNodeId = netPlan.getDemandIngressNode(demandId);
                int egressNodeId = netPlan.getDemandEgressNode(demandId);
                
                int[] sp = pricing(linkTable, demandId, ingressNodeId, egressNodeId, pi_e, lambda_d);
                if (sp.length == 0) continue;
                
                pathAdded = true;
                cpl.addPath(demandId, sp);
            }
            
            if (!pathAdded) break;
            
        } while(true);
        
        return Pair.of(w_e, c_best);
    }
    
    /**
     * Obtains the IGP weights using the dual-variables heuristic.
     * 
     * @param netPlan Network design
     * @param solverName Solver name
     * @param solverLibraryName Solver library path (optional, empty means default path)
     * @param precisionFactor Precision factor
     * @return IGP weight for each link, and the IGP cost penalty
     * @since 0.2.3
     */
    private static Pair<double[], Double> procedureDV(NetPlan netPlan, String solverName, String solverLibraryName, double precisionFactor)
    {
        double[] u_e = netPlan.getLinkCapacityInErlangsVector();
        double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        DoubleMatrixND A_ne = GraphUtils.getNodeLinkIncidenceMatrix(netPlan);
        DoubleMatrixND w_nd = GraphUtils.getNodeDemandIncidenceMatrix(netPlan);
        
        OptimizationProblem op = solveLP_xde(u_e, h_d, A_ne, w_nd, solverName, solverLibraryName);
        Pair<double[], double[]> dualVariables = retrieveDual(op, precisionFactor);
        
        return Pair.of(dualVariables.getFirst(), op.getOptimalCost());
    }
    
    /**
     * Returns the dual variables of the optimization problem.
     * 
     * @param op <code>OptimizationProblem</code>
     * @param precisionFactor Precision factor
     * @return Dual variables of the optimization problem. First array corresponds to the dual variables of the link capacity constraints, and the second one to the carried traffic constraints
     * @since 0.2.3
     */
    private static Pair<double[], double[]> retrieveDual(OptimizationProblem op, double precisionFactor)
    {
        double[] pi_e = op.getMultipliersOfConstraint("linkCapacity").to1DArray();
        double[] lambda_d = op.getMultipliersOfConstraint("carriedTraffic").to1DArray();
        
        for(int i = 0; i < pi_e.length; i++) { pi_e[i] = Math.abs(pi_e[i]); if (pi_e[i] <= precisionFactor) pi_e[i] = 0; }
        for(int i = 0; i < lambda_d.length; i++) { lambda_d[i] = Math.abs(lambda_d[i]); if (lambda_d[i] <= precisionFactor) lambda_d[i] = 0; }
        
        return Pair.of(pi_e, lambda_d);
    }
    
    /**
     * Solves a link-based LP formulation of the multi-commodity flow (MCF) problem 
     * using as objective function the minimization of the total IGP-cost penalty.
     * 
     * @param u_e Link capacity vector
     * @param h_d Offered traffic vector
     * @param A_ne Node-link incidence matrix (where each position <i>a<sub>ij</sub></i> is equal to 1 if node <i>i</i> is the origin node of link <i>j</i>, -1 if it is the destination node, and 0 otherwise)
     * @param w_nd Node-demand incidence matrix (where each position <i>a<sub>ij</sub></i> is equal to 1 if node <i>i</i> is the ingress node of demand <i>j</i>, -1 if it is the egress node, and 0 otherwise)
     * @param solverName Solver name
     * @param solverLibraryName Solver library path (optional, empty means default path)
     * @return An <code>OptimizationProblem</code> already solved. An exception is thrown if a solution could not be found
     * @since 0.2.3
     */
    private static OptimizationProblem solveLP_xde(double[] u_e, double[] h_d, DoubleMatrixND A_ne, DoubleMatrixND w_nd, String solverName, String solverLibraryName)
    {
        int E = u_e.length;
        int D = h_d.length;
        
	OptimizationProblem op = new OptimizationProblem();

	op.setInputParameter("A_ne", A_ne);
	op.setInputParameter("w_nd", w_nd);
	op.setInputParameter("h_d", h_d, "row");
	op.setInputParameter("u_e", u_e, "row");

	op.addDecisionVariable("c_e", false, new int[] {1, E}, 0, Double.MAX_VALUE);  // Penalty per link
	op.addDecisionVariable("x_de", false, new int[] {D, E}, 0, Double.MAX_VALUE); // Carried traffic per demand and link
	op.addDecisionVariable("y_e", false, new int[] {1, E}, 0, Double.MAX_VALUE);  // Carried traffic per link

	op.addConstraint("A_ne * x_de' == w_nd * diag(h_d)", "carriedTraffic"); // for each demand, all traffic is carried
	op.addConstraint("sum(x_de, 1) == y_e", "linkCapacity");                // for each link, its carried traffic is equal to sum of the carried traffic for each traversing demand
        
        // Piece-wise linear approximation of the penalty function
	op.addConstraint("c_e >= y_e");
	op.addConstraint("c_e >= 3.0 * y_e - (2.0/3.0) * u_e");
	op.addConstraint("c_e >= 10.0 * y_e - (16.0/3.0) * u_e");
	op.addConstraint("c_e >= 70.0 * y_e - (178.0/3.0) * u_e");
	op.addConstraint("c_e >= 500.0 * y_e - (1468.0/3.0) * u_e");
	op.addConstraint("c_e >= 5000.0 * y_e - (16318.0/3.0) * u_e");

	op.setObjectiveFunction("minimize", "sum(c_e)");

	op.solve(solverName, "solverLibraryName", solverLibraryName);
	if(!op.solutionIsOptimal()) throw new Net2PlanException("Solution is not optimal");
        
        return op;
    }
    
    /**
     * Solves a path-based LP formulation of the multi-commodity flow (MCF) problem 
     * using as objective function the minimization of the total IGP-cost penalty.
     * 
     * @param u_e Link capacity vector
     * @param h_d Offered traffic vector
     * @param cpl Candidate path list
     * @param solverName Solver name
     * @param solverLibraryName Solver library path (optional, empty means default path)
     * @return An <code>OptimizationProblem</code> already solved. An exception is thrown if a solution could not be found
     * @since 0.2.3
     */
    private static OptimizationProblem solveLP_xp(double[] u_e, double[] h_d, CandidatePathList cpl, String solverName, String solverLibraryName)
    {
        int E = u_e.length;
        int P = cpl.getNumberOfPaths();
        
        OptimizationProblem op = new OptimizationProblem();
        
	op.addDecisionVariable("c_e", false, new int[] {1, E}, 0, Double.MAX_VALUE); // Penalty per link
	op.addDecisionVariable("y_e", false, new int[] {1, E}, 0, Double.MAX_VALUE); // Carried traffic per link
        op.addDecisionVariable("x_p", false, new int[] {1, P}, 0, Double.MAX_VALUE); // Carried traffic per path
        
        op.setInputParameter("u_e", u_e, "row");
        op.setInputParameter("h_d", h_d, "row");
        op.setInputParameter("A_dp", cpl.computeDemand2PathAssignmentMatrix());
        op.setInputParameter("A_ep", cpl.computeLink2PathAssignmentMatrix());
        
        op.addConstraint("A_dp * x_p' == h_d'", "carriedTraffic"); // for each demand, all traffic is carried
        op.addConstraint("A_ep * x_p' == y_e'", "linkCapacity");   // for each link, its carried traffic is equal to sum of the carried traffic by all traversing paths
        
        // Piece-wise linear approximation of the penalty function
	op.addConstraint("c_e >= y_e");
	op.addConstraint("c_e >= 3.0 * y_e - (2.0/3.0) * u_e");
	op.addConstraint("c_e >= 10.0 * y_e - (16.0/3.0) * u_e");
	op.addConstraint("c_e >= 70.0 * y_e - (178.0/3.0) * u_e");
	op.addConstraint("c_e >= 500.0 * y_e - (1468.0/3.0) * u_e");
	op.addConstraint("c_e >= 5000.0 * y_e - (16318.0/3.0) * u_e");
        
	op.setObjectiveFunction("minimize", "sum(c_e)");

	op.solve(solverName, "solverLibraryName", solverLibraryName);
	if(!op.solutionIsOptimal()) throw new Net2PlanException("Solution is not optimal");
        
        return op;
    }
}
