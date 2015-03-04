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
import com.tejas.engine.utils.Constants.ShortestPathType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Returns the maximum scaled version of the offered traffic vector so that the 
 * network capacity (summation of capacity of all links) is exhausted.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, January 2014
 */
public class BA_maxTrafficDemandsNetworkCapacity implements IAlgorithm
{
    @Override
    public String getDescription()
    {
        return "Returns the maximum scaled version of the offered traffic vector "
                + "so that the network capacity (summation of capacity of all "
                + "links) is exhausted";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	final List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
	algorithmParameters.add(Triple.of("shortestPathType", "hops", "Criteria to compute the shortest path. Valid values: 'hops' or 'km'"));
        
	return algorithmParameters;
    }

    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Initialize some variables */
        final int N = netPlan.getNumberOfNodes();
        final int E = netPlan.getNumberOfLinks();
        final int D = netPlan.getNumberOfDemands();

        /* Check */
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology and a demand set");

        final String aux_shortestPathType = algorithmParameters.get("shortestPathType");
        if (!aux_shortestPathType.equalsIgnoreCase("hops") && !aux_shortestPathType.equalsIgnoreCase("km")) throw new Net2PlanException("'shortestPathType' must be 'hops' or 'km'");
        final ShortestPathType shortestPathType = aux_shortestPathType.equalsIgnoreCase("hops") ? ShortestPathType.HOPS : ShortestPathType.KM;
        
        /* Remove all routes in the current netPlan */
        netPlan.removeAllRoutes();
        netPlan.removeAllProtectionSegments();
        
        /* Compute and update new offered traffic vector */
        final double[] h_d = TrafficMatrixGenerationModels.normalizeTraffic_networkCapacity(netPlan, shortestPathType);
        netPlan.setDemandOfferedTrafficInErlangsVector(h_d);

        return "Ok";
    }
    
}
