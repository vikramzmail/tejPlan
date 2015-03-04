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

package com.tejas.workspace.examples.cacSimulation.cacAlgorithm;

import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.cacSimulation.ICACAlgorithm;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>This algorithm receives lightpath connection requests and releases. For each 
 * lightpath release, wavelength resources are liberated. For each lightpath request, 
 * it determines how many lightpaths are actually required. Then, it tries to allocate 
 * them over a set of precomputed <tt>k</tt>-shortest paths (maximum path length in km 
 * equal to <tt>maxLightpathLengthInKm</tt>) using first-fit wavelength assignment 
 * (wavelength continuity is enforced).</p>
 * 
 * <p>Let \( trafficVolume \) be the traffic volume request for the connection 
 * request, and \( binaryRatePerChannel_Gbps \) the binary rate per lightpath. 
 * Then, the number of lightpaths, denoted as \( numLps \), to be established 
 * is computed as follows:</p>
 * 
 * <p><i>numLps = ceil(trafficVolume/binaryRatePerChannel_Gbps</i></p>
 * 
 * <p>Then, all the lightpaths are tried to be allocated as aforementioned. If 
 * there are no resources for all lightpaths, connection may be accepted partially.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2014
 */
public class CACSim_AA_WDM_basicAlgorithm implements ICACAlgorithm
{
    private CandidatePathList cpl;
    private int[] w_f;
    private double binaryRatePerChannel_Gbps;
    private int[][] fiberTable;
    private List<Set<Integer>> wavelengthOccupancy;
    
    @Override
    public void initialize(NetPlan netPlan, ConnectionNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        WDMUtils.checkConsistency(netPlan, net2planParameters);
        
        int k = Integer.parseInt(algorithmParameters.get("k"));
        double maxLightpathLengthInKm = Double.parseDouble(algorithmParameters.get("maxLightpathLengthInKm"));
        
        binaryRatePerChannel_Gbps = Double.parseDouble(algorithmParameters.get("binaryRatePerChannel_Gbps"));
        cpl = new CandidatePathList(netPlan, netPlan.getLinkLengthInKmVector(), "maxLengthInKm", Double.toString(maxLightpathLengthInKm) , "K" , Integer.toString(k));
        fiberTable = netPlan.getLinkTable();
        w_f = WDMUtils.getFiberNumWavelengthsAttributes(netPlan);

        int E = netPlan.getNumberOfLinks();
        wavelengthOccupancy = new ArrayList<Set<Integer>>();
        for(int fiberId = 0; fiberId < E ; fiberId++) wavelengthOccupancy.add(new HashSet<Integer>());
    }

    @Override
    public List<CACAction> processEvent(NetPlan netPlan, ConnectionNetState connectionNetState, CACEvent event)
    {
	final List<CACAction> actions = new LinkedList<CACAction>();
        
        switch(event.getEventType())
        {
            case CONNECTION_REQUEST:
                
                int demandId = event.getRequestDemandId();
                double trafficVolume = event.getRequestTrafficVolumeInErlangs();
                int numLps = (int) Math.ceil(trafficVolume / binaryRatePerChannel_Gbps);
                
                List<int[]> seqFibers = new LinkedList<int[]>();
                List<Map<String, String>> lpAttributes = new LinkedList<Map<String, String>>();
                
                boolean failToAllocate;
                for(int lpId = 0; lpId < numLps; lpId++)
                {
                    failToAllocate = true;
                    
                    for (int pathId : cpl.getPathsPerDemand(demandId))
                    {
                        int[] seqFibers_thisLp = cpl.getSequenceOfLinks(pathId);
                        int[] seqWavelengths_thisLp = WDMUtils.WA_firstFit(fiberTable, seqFibers_thisLp, w_f, wavelengthOccupancy);
                        
                        if (seqWavelengths_thisLp.length == 0) continue;

                        failToAllocate = false;
                        
                        /* add this route, updating wavelength occupation */
                        for(int hopId = 0; hopId < seqFibers_thisLp.length; hopId++)
                        {
                            int fiberId = seqFibers_thisLp[hopId];
                            int wavelengthId = seqWavelengths_thisLp[hopId];
                            wavelengthOccupancy.get(fiberId).add(wavelengthId);
                        }
                        
                        seqFibers.add(seqFibers_thisLp);
                        Map<String, String> attributes_thisLp = new HashMap<String, String>();
                        attributes_thisLp.put("seqWavelengths", IntUtils.join(seqWavelengths_thisLp, " "));
                        lpAttributes.add(attributes_thisLp);
                        break;
                    }
                    
                    if (failToAllocate == true) break;
                }
                
                if (seqFibers.isEmpty())
                {
                    actions.add(CACAction.blockRequest("No lightpath could be allocated"));
                    
                }
                else
                {
                    double[] binaryRate = DoubleUtils.constantArray(seqFibers.size(), binaryRatePerChannel_Gbps);
                    actions.add(CACAction.acceptRequest(seqFibers, binaryRate, lpAttributes));
                }
                
                break;
                
            case CONNECTION_RELEASE:
                
                /* update wavelength occupation */
                long connId = event.getReleaseConnectionId();
                long[] lpIds = connectionNetState.getConnectionRoutes(connId);
                for(long lpId : lpIds)
                {
                    int[] seqFibers_thisLp = connectionNetState.getConnectionRouteSequenceOfLinks(connId);
                    int[] seqWavelengths_thisLp = StringUtils.toIntArray(StringUtils.split(connectionNetState.getConnectionRouteAttribute(lpId, "seqWavelengths"), " "));
                    
                    for(int hopId = 0; hopId < seqFibers_thisLp.length; hopId++)
                    {
                        int fiberId = seqFibers_thisLp[hopId];
                        int wavelengthId = seqWavelengths_thisLp[hopId];
                        wavelengthOccupancy.get(fiberId).remove(wavelengthId);
                    }
                }
                
                break;
        }
        
        return actions;
    }

    @Override
    public String getDescription()
    {
        String newLine = StringUtils.getLineSeparator();
        
        StringBuilder description = new StringBuilder();
        description.append("This algorithm receives lightpath connection requests and releases. For each " +
            "lightpath release, wavelength resources are liberated. For each lightpath request, " +
            "it determines how many lightpaths are actually required. Then, it tries to allocate " +
            "them over a set of precomputed k-shortest paths (maximum path length in km " +
            "equal to maxLightpathLengthInKm) using first-fit wavelength assignment (wavelength continuity is enforced)");
        description.append(newLine).append(newLine);
        description.append("If there are no resources for all lightpaths, connection may be accepted partially");
        
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
    public String finish(StringBuilder output, double simTime) { return null; }

    @Override
    public void finishTransitory(double simTime) { }
}
