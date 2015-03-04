/******************************************************************************
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
import java.util.Map;
import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;


/**

 * Given a network topology of OADM optical nodes connected by WDM fiber links, and a traffic demand of lightpath requests, this algorithm
 * optimally computes the RWA (Routing and Wavelength Assignment) that carries all the lightpaths minimizing the average propagation delay, solving an Integer Linear Program (ILP).
 * Wavelength conversion is not allowed. Only those routes with a length below a user-defined threshold maxLightpathLengthInKm are accepted. All the channels are of the same binaryRatePerChannel_Gbps capacity.
 * The offered traffic demand is supposed to be measured in Gbps, and is rounded up to a multiple of binaryRatePerChannel_Gbps.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2014
 */
public class CFA_WDM_basicRWA implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        final int N = netPlan.getNumberOfNodes();
        final int E = netPlan.getNumberOfLinks();
        final int D = netPlan.getNumberOfDemands();

        /* Receive the input parameters */
        final int k = Integer.parseInt(algorithmParameters.get("k"));
        final int W = Integer.parseInt(algorithmParameters.get("numWavelengthsPerFiber"));
        final double maxLightpathLengthInKm = Double.parseDouble(algorithmParameters.get("maxLightpathLengthInKm"));
        final double binaryRatePerChannel_Gbps = Double.parseDouble(algorithmParameters.get("binaryRatePerChannel_Gbps"));

        /* Basic checks */
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");
        if (Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang")) != 1E9) throw new Net2PlanException("To avoid confusions, the traffic should be measured in Gbps in binaryRateInBitsPerSecondPerErlang net2plan option");

        /* Round up the demand offered traffic to upper multiple of lightpath binary rate */
        for (int d = 0; d < D; d++) { final double h_d = netPlan.getDemandOfferedTrafficInErlangs(d); netPlan.setDemandOfferedTrafficInErlangs(d, Math.ceil(h_d / binaryRatePerChannel_Gbps) * binaryRatePerChannel_Gbps); }
        
        /* Set the link capacity as the binary rate of a lightpath multiplied by the number of wavelengths */
        for (int e = 0; e < E; e++)
                netPlan.setLinkCapacityInErlangs(e, W * binaryRatePerChannel_Gbps);

        /* Remove all routes in current netPlan object. Initialize link capacities and attributes, and demand offered traffic */
        netPlan.removeAllRoutes();
        netPlan.removeAllProtectionSegments();

        /* Create the candidate path list */
        CandidatePathList cpl = new CandidatePathList(netPlan, netPlan.getLinkLengthInKmVector(), "K", Integer.toString(k), "maxLengthInKm", Double.toString(maxLightpathLengthInKm));
        final int P = cpl.getNumberOfPaths();

        /* Create the optimization problem object (JOM library) */
        OptimizationProblem op = new OptimizationProblem();

        /* Add the decision variables to the problem */
        op.addDecisionVariable("x_pw", true, new int[] { P, W }, 0, 1); // 1 if lightpath d(p) is routed through path p in wavelength w

        /* Set some input parameters */
        op.setInputParameter("W", W);
        op.setInputParameter("l_p", cpl.computeWeightPerPath(netPlan.getLinkLengthInKmVector()) , "row");
        op.setInputParameter("h_d", DoubleUtils.toIntArray(DoubleUtils.divide(netPlan.getDemandOfferedTrafficInErlangsVector() , binaryRatePerChannel_Gbps))  , "row");

        /* Sets the objective function */
        op.setObjectiveFunction("minimize", "sum(l_p * x_pw)"); // sum_pw (l_p · x_pw) 

        /* VECTORIAL FORM OF THE CONSTRAINTS */
        op.setInputParameter("A_dp", cpl.computeDemand2PathAssignmentMatrix());
        op.setInputParameter("A_ep", cpl.computeLink2PathAssignmentMatrix());
        op.addConstraint("A_dp * x_pw * ones([W ; 1]) == h_d'"); // each lightpath d: is carried in exactly one p-w --> sum_{p in P_d, w} x_dp <= 1, for all d
        op.addConstraint("A_ep * x_pw <= 1"); // wavelength-clashing constraints --> sum_{p in P_e, w} x_pw <= 1, for all e,w
        /* FOR-LOOP FORM OF THE CONSTRAINTS (slower to assemble the OptimizationProblem object) */
//				/* Routing constraints */
//				for (int d = 0 ; d < D ; d ++)
//				{
//					final int [] P_d = cpl.getPathsPerDemand(d);
//					op.setInputParameter ("d" , d);
//					op.setInputParameter ("P_d" , P_d , "row");
//					for (int w = 0 ; w < W ; w ++)
//						op.addConstraint("sum(x_pw(P_d,all)) == h_d(d)");
//				}
//				/* Wavelength clashing constraints --> for loop version */
//				for (int e = 0 ; e < E ; e ++)
//				{
//					final int [] P_e = cpl.getPathsPerLink(e);
//					op.setInputParameter ("P_e" , P_e , "row");
//					for (int w = 0 ; w < W ; w ++)
//						op.addConstraint("sum(x_pw(P_e, " + w + ")) <= 1");
//				}

        /* Call the solver to solve the problem */
//       op.solve(algorithmParameters.get("solverName"), "solverLibraryName", algorithmParameters.get("solverLibraryName"));
       op.solve(algorithmParameters.get("solverName"), "solverLibraryName", algorithmParameters.get("solverLibraryName"));

        /* If an optimal solution was not found, quit */
        if (!op.solutionIsOptimal()) throw new Net2PlanException("An optimal solution was not found");

        /* Retrieve the optimum solutions. Convert the bps into Erlangs */
        double[][] x_pw = (double[][]) op.getPrimalSolution("x_pw").toArray();

        /* Update netPlan object adding the calculated routes */
        for (int p = 0; p < P; p++)
        {
            for (int w = 0; w < W; w++)
            {
                if (x_pw[p][w] == 1)
                {
                    final int demandId = cpl.getDemandId(p);
                    final int[] seqFibers = cpl.getSequenceOfLinks(p);
                    final int routeId = netPlan.addRoute(demandId, binaryRatePerChannel_Gbps, seqFibers, null, null);
                    
                    WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan, routeId, w);
                }
            }
        }

        WDMUtils.setFiberNumWavelengthsAttributes(netPlan, W);
        WDMUtils.checkConsistency(netPlan, net2planParameters);

        return "Ok!";
    }

    @Override
    public String getDescription()
    {
        return "Given a network topology of OADM optical nodes connected by WDM fiber links, and a traffic demand of lightpath requests, this algorithm " +
            "optimally computes the RWA (Routing and Wavelength Assignment) that carries all the lightpaths minimizing the average propagation delay, solving an Integer Linear Program (ILP). " +
            "Wavelength conversion is not allowed. Only those routes with a length below a user-defined threshold maxLightpathLengthInKm are accepted. All the channels are of the same binaryRatePerChannel_Gbps capacity. " +
            "The offered traffic demand is supposed to be measured in Gbps, and is rounded up to a multiple of binaryRatePerChannel_Gbps.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("k", "3", "Maximum number of admissible paths per demand"));
        algorithmParameters.add(Triple.of("numWavelengthsPerFiber", "80", "Number of wavelengths per link"));
        algorithmParameters.add(Triple.of("binaryRatePerChannel_Gbps", "10", "Binary rate of all the lightpaths"));
        algorithmParameters.add(Triple.of("maxLightpathLengthInKm", "5000", "Maximum allowed lightpath length in km"));
//        algorithmParameters.add(Triple.of("solverName", "cplex", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "c:\\windows\\system32\\glpk_4_54.dll", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
        
        return algorithmParameters;
    }
}
