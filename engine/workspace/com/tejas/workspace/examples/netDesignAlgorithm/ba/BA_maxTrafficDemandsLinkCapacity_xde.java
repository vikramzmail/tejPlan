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

package com.tejas.workspace.examples.netDesignAlgorithm.ba;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.TrafficMatrixGenerationModels;
import com.tejas.engine.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>Returns the maximum scaled version of the offered traffic vector that can be 
 * carried by the network, provided that no link is oversubscribed. It is assumed 
 * no protection capacity is reserved.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, January 2014
 */
public class BA_maxTrafficDemandsLinkCapacity_xde implements IAlgorithm
{
    @Override
    public String getDescription() { return "Returns the maximum scaled version of the offered traffic vector that can be carried by the network, provided that no link is oversubscribed"; }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
        algorithmParameters.add(Triple.of("solverLibraryName", "", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));

	return algorithmParameters;
    }

    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        
        /* Check */
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");
        
        String solverName = algorithmParameters.get("solverName");
	String solverLibraryName = algorithmParameters.get("solverLibraryName");
        
        /* Remove all routes in the current netPlan */
        netPlan.removeAllRoutes();
        netPlan.removeAllProtectionSegments();

        /* Compute and update new offered traffic vector */
        double[] h_d = TrafficMatrixGenerationModels.normalizeTraffic_linkCapacity_xde(netPlan, solverName, solverLibraryName);
        netPlan.setDemandOfferedTrafficInErlangsVector(h_d);

        return "Ok";
    }
}
