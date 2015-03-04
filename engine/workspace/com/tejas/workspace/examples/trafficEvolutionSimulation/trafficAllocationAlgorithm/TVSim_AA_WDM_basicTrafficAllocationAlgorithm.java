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

package com.tejas.workspace.examples.trafficEvolutionSimulation.trafficAllocationAlgorithm;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficAllocationAlgorithm;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>At each time interval, this algorithm adjusts dynamically the number of lightpaths 
 * for each demand with respect to the ones in the previous period.</p>
 * 
 * <p>In case that new lightpaths are required, it performs an RWA using first-fit 
 * (wavelength continuity is enforced) over a set of precomputed <tt>k</tt>-shortest paths 
 * (maximum path length in km equal to <tt>maxLightpathLengthInKm</tt>). These paths are 
 * evaluated in order. Once a feasible RWA is found, then the lightpath 
 * is allocated, and no other candidate solution is checked.</p>
 * 
 * <p>Let <i>currentNumLps<sub>d</sub></i> be the number of lightpaths which are active for 
 * demand <i>d &isin; D</i> in the current time, <i>h<sub>d</sub></i> the new traffic volume in 
 * Gbps for demand <i>d &isin; D</i>, and <tt>binaryRatePerChannel_Gbps</tt> the binary 
 * rate per lightpath. Then, the number of lightpaths for demand <i>d &isin; D</i>, 
 * denoted as <i>numLps<sub>d</sub></i>, for the new period is computed as follows:</p>
 * 
 * <p><i>numLps<sub>d</sub> = ceil(h<sub>d</sub>/binaryRatePerChannel_Gbps)</i></p>
 * 
 * <p>Then, we adjust the number of lightpaths as follows:
 * <ul>
 *  <li>If <i>numLps<sub>d</sub> - currentNumLps<sub>d</sub> > 0</i>, add <i>numLps<sub>d</sub> - currentNumLps<sub>d</sub></i>
 *  lightpaths using the pre-computed k-shortest paths and first-fit wavelength assignment</li>
 *  <li>If <i>numLps<sub>d</sub> - currentNumLps<sub>d</sub> < 0</i>, remove the latest <i>numLps<sub>d</sub> - currentNumLps<sub>d</sub></i>
 *  lightpaths from demand</li>
 *  <li>Otherwise, do nothing</li>
 * </ul></p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2014
 */
public class TVSim_AA_WDM_basicTrafficAllocationAlgorithm implements ITrafficAllocationAlgorithm
{
    private CandidatePathList cpl;
    private int D;
    private int[] w_f;
    private double binaryRatePerChannel_Gbps;
    private int[][] fiberTable;
    private List<Set<Integer>> wavelengthOccupancy;
    
    @Override
    public List<TrafficAllocationAction> processEvent(NetPlan netPlan, TimeVaryingNetState netState, double[] h_d, Calendar currentTime)
    {
        double H_d = DoubleUtils.sum(netState.getDemandOfferedTrafficInErlangsVector());
        double R_d = DoubleUtils.sum(netState.getDemandCarriedTrafficInErlangsVector());
        
        System.out.println("H_d " + H_d);
        System.out.println("R_d " + R_d);
        System.out.println("R_d/H_d " + R_d/H_d);
        
        List<TrafficAllocationAction> actions = new LinkedList<TrafficAllocationAction>();
        
        for(int demandId = 0; demandId < D; demandId++)
        {
            int numLps = (int) Math.ceil(h_d[demandId] / binaryRatePerChannel_Gbps);
            long[] currentLpIds = netState.getDemandRoutes(demandId);
            
            if (numLps > currentLpIds.length)
            {
                /* add new lightpaths */
                int numLpsToBeSetUp = numLps - currentLpIds.length;
                
                for (int r = 0; r < numLpsToBeSetUp; r++)
                {
                    for (int pathId : cpl.getPathsPerDemand(demandId))
                    {
                        int[] seqFibers = cpl.getSequenceOfLinks(pathId);
                        int[] seqWavelengths = WDMUtils.WA_firstFit(fiberTable, seqFibers, w_f, wavelengthOccupancy);
                        if (seqWavelengths.length == 0) continue;
                        
                        /* add this route, updating wavelength occupation */
                        for(int hopId = 0; hopId < seqFibers.length; hopId++)
                        {
                            int fiberId = seqFibers[hopId];
                            int wavelengthId = seqWavelengths[hopId];
                            wavelengthOccupancy.get(fiberId).add(wavelengthId);
                        }

                        Map<String, String> lpAttributes = new HashMap<String, String>();
                        lpAttributes.put("seqWavelengths", IntUtils.join(seqWavelengths, " "));
                        actions.add(TrafficAllocationAction.addRoute(demandId, binaryRatePerChannel_Gbps, IntUtils.toLongArray(seqFibers), null, lpAttributes));
                        break;
                    }
                }
            }
            else if (numLps < currentLpIds.length)
            {
                /* eliminate last lightpaths, if I have to reduce the amount of lightpaths */
                int numLpsToBeTornDown = currentLpIds.length - numLps;
                
                for (int r = 0; r < numLpsToBeTornDown; r++)
                {
                    long lpId = currentLpIds[currentLpIds.length - 1 - r];

                    final int[] seqFibers = LongUtils.toIntArray(netState.getRouteSequenceOfLinks(lpId));
                    final int[] seqWavelengths = StringUtils.toIntArray(StringUtils.split(netState.getRouteAttribute(lpId, "seqWavelengths"), " "));

                    /* remove this route, updating wavelength occupation */
                    actions.add(TrafficAllocationAction.removeRoute(lpId));
                    
                    for(int hopId = 0; hopId < seqFibers.length; hopId++)
                    {
                        int fiberId = seqFibers[hopId];
                        int wavelengthId = seqWavelengths[hopId];
                        wavelengthOccupancy.get(fiberId).remove(wavelengthId);
                    }
                }
            }
        }

        return actions;
    }

    @Override
    public String getDescription()
    {
        String newLine = StringUtils.getLineSeparator();
        
        StringBuilder description = new StringBuilder();
        description.append("This algorithm adjusts dynamically the number of lightpaths for each demand with respect to the ones in the previous period.");
        description.append(newLine).append(newLine);
        description.append("At each time interval, this algorithm adjusts dynamically the number of lightpaths for each demand with respect to the ones in the previous period.");
        description.append(newLine).append(newLine);
        description.append("In case that new lightpaths are required, it performs an RWA using first-fit (wavelength continuity is enforced) over a set of precomputed k-shortest paths (maximum path length in km equal to maxLightpathLengthInKm). These paths are evaluated in order. Once a feasible RWA is found, then the lightpath is allocated, and no other candidate solution is checked.");
        
        return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
        algorithmParameters.add(Triple.of("binaryRatePerChannel_Gbps", "10", "Binary rate of all the lightpaths"));
        algorithmParameters.add(Triple.of("maxLightpathLengthInKm", "5000", "Maximum allowed lightpath length in km"));
        algorithmParameters.add(Triple.of("k", "3", "Maximum number of candidate paths per demand"));
        
        return algorithmParameters;
    }

    @Override
    public void initialize(NetPlan netPlan, TimeVaryingNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        WDMUtils.checkConsistency(netPlan, net2planParameters);
        
        int k = Integer.parseInt(algorithmParameters.get("k"));
        double maxLightpathLengthInKm = Double.parseDouble(algorithmParameters.get("maxLightpathLengthInKm"));
        
        binaryRatePerChannel_Gbps = Double.parseDouble(algorithmParameters.get("binaryRatePerChannel_Gbps"));
        cpl = new CandidatePathList(netPlan, netPlan.getLinkLengthInKmVector(), "maxLengthInKm", Double.toString(maxLightpathLengthInKm) , "K" , Integer.toString(k));
        D = netPlan.getNumberOfDemands();
        fiberTable = netPlan.getLinkTable();
        w_f = WDMUtils.getFiberNumWavelengthsAttributes(netPlan);
        wavelengthOccupancy = WDMUtils.getWavelengthOccupancy(netPlan);
    }

    @Override
    public String finish(StringBuilder output, Calendar finishTime) { return null; }

    @Override
    public void finishTransitory(Calendar currentTime) { }
}
