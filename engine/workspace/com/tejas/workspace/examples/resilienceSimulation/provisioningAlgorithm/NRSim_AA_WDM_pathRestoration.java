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

package com.tejas.workspace.examples.resilienceSimulation.provisioningAlgorithm;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>This algorithm implements a local lightpath restoration mechanism. Since it 
 * assumes that the WDM network requires regenerators/wavelength converters, if 
 * the input design does not contain them, the algorithm computes the requirements 
 * of regeneration/conversion for each lightpath.</p>
 * 
 * <p>Upon failure, it tries to reroute each affected lightpath over the shortest 
 * path (installing as many regenerators/wavelength converters as needed).</p>
 * 
 * <p>Upon restoration, it tries to restore lightpaths to their original route, 
 * (maybe using a different sequence of wavelengths).</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class NRSim_AA_WDM_pathRestoration implements IProvisioningAlgorithm
{
    private enum RESTORATION_POINT { FIRST_NODE, FIRST_AVAILABLE_UPSTREAM_NODE };
    private RESTORATION_POINT restorationPoint;
    private double maxRegeneratorDistanceInKm;

    private List<Set<Integer>> wavelengthOccupancy;
    private int[] regeneratorOccupancy;
    private int[] w_f;
    private double[] l_f;
    private double[] numAvailableWavelengths;
    private int[][] fiberTable;

    private int[][] current_seqWavelengths;
    private int[][] current_seqRegenerators;

    private int[] stats_max_regeneratorOccupancy;

    private int[][] current_seqFibers;

    private void checkInternalState(ResilienceNetState netState)
    {
	int N = regeneratorOccupancy.length;
	int F = fiberTable.length;
	int LP = current_seqFibers.length;

	List<Set<Integer>> aux_wavelengthOccupancy = new ArrayList<Set<Integer>>();
	int[] aux_regeneratorOccupancy = new int[N];

	for(int fiberId = 0; fiberId < F; fiberId++)
	    aux_wavelengthOccupancy.add(new HashSet<Integer>());

	for(int lightpathId = 0; lightpathId < LP; lightpathId++)
	{
	    int[] seqFibers = LongUtils.toIntArray(netState.getRouteCurrentSequenceOfLinksAndSegments(lightpathId));
	    if (!Arrays.equals(seqFibers, current_seqFibers[lightpathId])) throw new RuntimeException("Bad 1");

	    if (current_seqWavelengths[lightpathId].length == 0)
	    {
		if (current_seqRegenerators[lightpathId].length > 0) throw new RuntimeException("Bad 2");
		continue;
	    }
            
            int[] seqRegenerators = WDMUtils.computeRegeneratorPositions(fiberTable, seqFibers, l_f, maxRegeneratorDistanceInKm);
	    int minNumRegenerators = IntUtils.find(seqRegenerators, 1, Constants.SearchType.ALL).length;
	    if (IntUtils.find(current_seqRegenerators[lightpathId], 1, Constants.SearchType.ALL).length < minNumRegenerators) throw new RuntimeException("Bad 3");

	    for(int hopId = 0; hopId < seqFibers.length; hopId++)
	    {
		int fiberId = seqFibers[hopId];
		int wavelengthId = current_seqWavelengths[lightpathId][hopId];

		if (wavelengthId < 0 || wavelengthId >= w_f[fiberId]) throw new RuntimeException("Bad 4");
		if (aux_wavelengthOccupancy.get(fiberId).contains(wavelengthId))
		{
		    System.out.println("lightpathId " + lightpathId);
		    System.out.println("fiberId " + fiberId);
		    System.out.println("wavelengthId " + wavelengthId);

		    throw new RuntimeException("Bad 5");
		}
                
		aux_wavelengthOccupancy.get(fiberId).add(wavelengthId);
                
                if (current_seqRegenerators[lightpathId][hopId] == 1)
                {
                    int nodeId = fiberTable[fiberId][0];
                    aux_regeneratorOccupancy[nodeId]++;
                }
	    }
	}

	for(int fiberId = 0; fiberId < F; fiberId++)
	{
	    if (!aux_wavelengthOccupancy.get(fiberId).containsAll(wavelengthOccupancy.get(fiberId)) || !wavelengthOccupancy.get(fiberId).containsAll(aux_wavelengthOccupancy.get(fiberId)))
	    {
		System.out.println("fiberId " + fiberId);
		System.out.println("aux_wavelengthOccupancy " + new TreeSet<Integer>(aux_wavelengthOccupancy.get(fiberId)));
		System.out.println("wavelengthOccupancy " + new TreeSet<Integer>(wavelengthOccupancy.get(fiberId)));
		throw new RuntimeException("Bad 6");
	    }

	    if (numAvailableWavelengths[fiberId] != w_f[fiberId] - aux_wavelengthOccupancy.get(fiberId).size())
		throw new RuntimeException("Bad 7, available = " + numAvailableWavelengths[fiberId] + ", w_f = " + w_f[fiberId] + ", occupancy = " + wavelengthOccupancy.get(fiberId).size());
	}

	if (!Arrays.equals(regeneratorOccupancy, aux_regeneratorOccupancy)) throw new RuntimeException("Bad 8");
    }

    @Override
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
	checkInternalState(netState);
        
        List<ProvisioningAction> actions = new LinkedList<ProvisioningAction>();

        int N = netPlan.getNumberOfNodes();
        int F = netPlan.getNumberOfLinks();

        if (event.getEventType() == ResilienceEvent.EventType.NODE_FAILURE || event.getEventType() == ResilienceEvent.EventType.LINK_FAILURE)
        {
	    Set<Integer> _nodesDown = new HashSet<Integer>();
	    Set<Integer> _fibersDown = new HashSet<Integer>();
	    Set<Long> affectedLightpaths = new HashSet<Long>();
	    Set<Long> _unrecoverableLightpaths = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> _segmentAvailability = new HashMap<Long, Double>();
	    netState.getFailureEffects(event, _nodesDown, _fibersDown, affectedLightpaths, _unrecoverableLightpaths, _currentLinkAvailableCapacity, _segmentAvailability);

	    int[] nodesDown = IntUtils.toArray(_nodesDown);
	    int[] fibersDown = IntUtils.toArray(_fibersDown);
            
            for(long aux_lightpathId : affectedLightpaths)
            {
                int lightpathId = (int) aux_lightpathId;
		if (current_seqWavelengths[lightpathId].length > 0)
		{
		    for(int hopId = 0; hopId < current_seqFibers[lightpathId].length; hopId++)
		    {
			int fiberId = current_seqFibers[lightpathId][hopId];
			int wavelengthId = current_seqWavelengths[lightpathId][hopId];
			wavelengthOccupancy.get(fiberId).remove(wavelengthId);
			numAvailableWavelengths[fiberId]++;

                        if (current_seqRegenerators[lightpathId][hopId] == 1)
                        {
                            int nodeId = fiberTable[fiberId][0];
                            regeneratorOccupancy[nodeId]--;
                        }
                    }
		}
                
		current_seqFibers[lightpathId] = netPlan.getRouteSequenceOfLinks(lightpathId);
                current_seqWavelengths[lightpathId] = new int[0];
                current_seqRegenerators[lightpathId] = new int[0];
            }

	    affectedLightpaths.removeAll(_unrecoverableLightpaths);
	    _nodesDown.clear();
	    _fibersDown.clear();
	    _unrecoverableLightpaths.clear();
	    _currentLinkAvailableCapacity.clear();
            _segmentAvailability.clear();

	    double[] costPerFiber = DoubleUtils.copy(l_f);

            for(int fiberId : fibersDown)
            {
                costPerFiber[fiberId] = Double.MAX_VALUE;
                wavelengthOccupancy.get(fiberId).clear();
                numAvailableWavelengths[fiberId] = w_f[fiberId];
            }

            for(int nodeId : nodesDown)
		regeneratorOccupancy[nodeId] = 0;

            for(long aux_lightpathId : affectedLightpaths)
            {
                int lightpathId = (int) aux_lightpathId;
                int demandId = netState.getRouteDemand(lightpathId);
                int firstNode = restorationPoint == RESTORATION_POINT.FIRST_NODE ? netPlan.getDemandIngressNode(demandId) : netState.getFirstAvailableNodeUpstream(lightpathId, nodesDown, fibersDown);
                int egressNode = netPlan.getDemandEgressNode(demandId);

		int[] csp = GraphUtils.getCapacitatedShortestPath(fiberTable, firstNode, egressNode, costPerFiber, numAvailableWavelengths, 1);
		if (csp.length == 0) continue;

		if (IntUtils.containsAny(fibersDown, csp))
		{
		    System.out.println("fibersDown " + Arrays.toString(fibersDown));
		    System.out.println("fibersThisSubPath " + Arrays.toString(csp));
		    throw new RuntimeException("Bad - Using fibers down in the subpath");
		}

		if (IntUtils.containsAny(nodesDown, netPlan.convertSequenceOfLinks2SequenceOfNodes(csp)))
		{
		    System.out.println("nodesDown " + Arrays.toString(nodesDown));
		    System.out.println("nodesThisSubPath " + Arrays.toString(netPlan.convertSequenceOfLinks2SequenceOfNodes(csp)));
		    throw new RuntimeException("Bad - Using nodes down in the subpath");
		}

                if (restorationPoint == RESTORATION_POINT.FIRST_AVAILABLE_UPSTREAM_NODE)
                {
                    try
                    {
                        csp = netState.getMergedRoute(current_seqFibers[lightpathId], csp);
                    }
                    catch(Throwable e)
                    {
                        System.out.println("demand id " + demandId);
                        System.out.println("route id " + lightpathId);
                        System.out.println("current_seqFibers: " + Arrays.toString(current_seqFibers[lightpathId]) + ", seqNodes = " + Arrays.toString(netPlan.convertSequenceOfLinks2SequenceOfNodes(current_seqFibers[lightpathId])));
                        System.out.println("csp: " + Arrays.toString(csp) + ", seqNodes = " + Arrays.toString(netPlan.convertSequenceOfLinks2SequenceOfNodes(csp)));
                    }
                }

		if (IntUtils.containsAny(fibersDown, csp))
		{
		    System.out.println("fibersDown " + Arrays.toString(fibersDown));
		    System.out.println("fibersThisPath " + Arrays.toString(csp));
		    throw new RuntimeException("Bad - Using fibers down");
		}

		if (IntUtils.containsAny(nodesDown, netPlan.convertSequenceOfLinks2SequenceOfNodes(csp)))
		{
		    System.out.println("nodesDown " + Arrays.toString(nodesDown));
		    System.out.println("nodesThisPath " + Arrays.toString(netPlan.convertSequenceOfLinks2SequenceOfNodes(csp)));
		    throw new RuntimeException("Bad - Using nodes down");
		}

		GraphUtils.checkRouteContinuity(fiberTable, csp, GraphUtils.CheckRoutingCycleType.NO_CHECK);

                List<int[]> newLightpath = new LinkedList<int[]>();
                newLightpath.add(csp);

                List<int[]> aux_seqWavelengths = new LinkedList<int[]>();
                List<int[]> aux_seqRegenerators = new LinkedList<int[]>();

                WDMUtils.WA_RPP_firstFit(fiberTable, newLightpath, w_f, wavelengthOccupancy, aux_seqWavelengths, l_f, regeneratorOccupancy, aux_seqRegenerators, maxRegeneratorDistanceInKm);

		int[] seqWavelengths = aux_seqWavelengths.get(0);
                int[] seqRegenerators = aux_seqRegenerators.get(0);

		if(seqWavelengths.length == 0) continue;

		for(int fiberId : csp)
		{
		    numAvailableWavelengths[fiberId]--;
		    if (numAvailableWavelengths[fiberId] < 0)
			System.out.println("Wavelength exhaustion in fiber " + fiberId);
		}

		current_seqFibers[lightpathId] = csp;
                current_seqWavelengths[lightpathId] = seqWavelengths;
                current_seqRegenerators[lightpathId] = seqRegenerators;

                double binaryRate = netPlan.getRouteCarriedTrafficInErlangs(lightpathId);
                actions.add(ProvisioningAction.modifyRoute(lightpathId, binaryRate, IntUtils.toLongArray(csp), null));
            }
        }
        else
        {
	    Set<Integer> _nodesDown = new HashSet<Integer>();
	    Set<Integer> _linksDown = new HashSet<Integer>();
	    Set<Long> reparableRoutes = new HashSet<Long>();
	    Set<Long> _unreparableRoutes = new HashSet<Long>();
	    List<Double> _currentLinkAvailableCapacity = new ArrayList<Double>();
            Map<Long, Double> _segmentAvailability = new HashMap<Long, Double>();

	    netState.getReparationEffects(event, _nodesDown, _linksDown, reparableRoutes, _unreparableRoutes, _currentLinkAvailableCapacity, _segmentAvailability);

	    _nodesDown.clear();
	    _linksDown.clear();
	    _unreparableRoutes.clear();
	    _currentLinkAvailableCapacity.clear();
            _segmentAvailability.clear();

            for(long aux_lightpathId : reparableRoutes)
            {
                int lightpathId = (int) aux_lightpathId;
                int[] seqFibers = netPlan.getRouteSequenceOfLinks(lightpathId);

                List<int[]> newLightpath = new LinkedList<int[]>();
                newLightpath.add(seqFibers);

                List<int[]> aux_seqWavelengths = new LinkedList<int[]>();
                List<int[]> aux_seqRegenerators = new LinkedList<int[]>();
		double[] aux_numAvailableWavelengths = DoubleUtils.copy(numAvailableWavelengths);

		List<Set<Integer>> aux_wavelengthOccupancy = new ArrayList<Set<Integer>>();
                for(int fiberId = 0; fiberId < F; fiberId++)
                    aux_wavelengthOccupancy.add(new HashSet<Integer>(wavelengthOccupancy.get(fiberId)));

                int[] aux_regeneratorOccupancy = IntUtils.copy(regeneratorOccupancy);

		if (current_seqWavelengths[lightpathId].length > 0)
		{
		    for(int hopId = 0; hopId < current_seqFibers[lightpathId].length; hopId++)
		    {
			int fiberId = current_seqFibers[lightpathId][hopId];
			int wavelengthId = current_seqWavelengths[lightpathId][hopId];

			aux_wavelengthOccupancy.get(fiberId).remove(wavelengthId);
			aux_numAvailableWavelengths[fiberId]++;
                        
                        if (current_seqRegenerators[lightpathId][hopId] == 1)
                        {
                            int nodeId = fiberTable[fiberId][0];
                            aux_regeneratorOccupancy[nodeId]--;
                        }
		    }
		}

                WDMUtils.WA_RPP_firstFit(fiberTable, newLightpath, w_f, aux_wavelengthOccupancy, aux_seqWavelengths, l_f, aux_regeneratorOccupancy, aux_seqRegenerators, maxRegeneratorDistanceInKm);

                int[] seqWavelengths = aux_seqWavelengths.get(0);
                int[] seqRegenerators = aux_seqRegenerators.get(0);

                if(seqWavelengths.length == 0) continue;

		for(int fiberId : seqFibers)
		    aux_numAvailableWavelengths[fiberId]--;

		regeneratorOccupancy = aux_regeneratorOccupancy;
                wavelengthOccupancy = aux_wavelengthOccupancy;
		numAvailableWavelengths = aux_numAvailableWavelengths;

		current_seqFibers[lightpathId] = seqFibers;
                current_seqWavelengths[lightpathId] = seqWavelengths;
                current_seqRegenerators[lightpathId] = seqRegenerators;

                double binaryRate = netPlan.getRouteCarriedTrafficInErlangs(lightpathId);
                actions.add(ProvisioningAction.modifyRoute(lightpathId, binaryRate, IntUtils.toLongArray(seqFibers), null));
            }
        }

        for(int nodeId = 0; nodeId < N; nodeId++)
            if (regeneratorOccupancy[nodeId] > stats_max_regeneratorOccupancy[nodeId])
                stats_max_regeneratorOccupancy[nodeId] = regeneratorOccupancy[nodeId];

        return actions;
    }

    @Override
    public String finish(StringBuilder output, double simTime)
    {
	output.append(String.format("<p>Total number of regenerators to be installed: %d</p>", (int) IntUtils.sum(stats_max_regeneratorOccupancy)));

	return "Regenerator statistics from lightpath fast restoration algorithm";
    }

    @Override
    public String getDescription()
    {
        StringBuilder description = new StringBuilder();
        
        String newLine = StringUtils.getLineSeparator();
        description.append("This algorithm implements a local lightpath restoration mechanism. Since it assumes that the WDM network requires regenerators/wavelength converters, if the input design does not contain them, the algorithm computes the requirements of regeneration/conversion for each lightpath.");
        description.append(newLine).append(newLine);
        description.append("Upon failure, it tries to reroute each affected lightpath over the shortest path (installing as many regenerators/wavelength converters as needed).");
        description.append(newLine).append(newLine);
        description.append("Upon restoration, it tries to restore lightpaths to their original route (maybe using a different sequence of wavelengths).");
        
        return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
        parameters.add(Triple.of("restorationPoint", "firstAvailableUpstreamNode", "From which node restoration starts? (firstNode, firstAvailableUpstreamNode)"));
        parameters.add(Triple.of("maxRegeneratorDistanceInKm", "2800", "Maximum regeneration distance"));

        return parameters;
    }

    @Override
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	int S = netPlan.getNumberOfProtectionSegments();
	if (S != 0) throw new Net2PlanException("Protection segments must be removed (they are occupying resources: wavelengths, regenerators...)");

        String _fromFirstAvailableUpstreamNode = algorithmParameters.get("restorationPoint");
        switch(_fromFirstAvailableUpstreamNode)
        {
            case "firstNode":
                restorationPoint = RESTORATION_POINT.FIRST_NODE;
                break;
            case "firstAvailableUpstreamNode":
                restorationPoint = RESTORATION_POINT.FIRST_AVAILABLE_UPSTREAM_NODE;
                break;
            default:
                throw new Net2PlanException("'restorationPoint' must be 'firstNode' or 'firstAvailableUpstreamNode'");
        }

        maxRegeneratorDistanceInKm = Double.parseDouble(algorithmParameters.get("maxRegeneratorDistanceInKm"));
        if (maxRegeneratorDistanceInKm <= 0) throw new Net2PlanException("'maxRegeneratorDistanceInKm' must be greater than zero");
        
        // Diferenciar entre diseño WDM y diseño no-WDM
        WDMUtils.checkConsistency(netPlan, net2planParameters);

        wavelengthOccupancy = WDMUtils.getWavelengthOccupancy(netPlan);
        
        l_f = netPlan.getLinkLengthInKmVector();
        w_f = WDMUtils.getFiberNumWavelengthsAttributes(netPlan);
        fiberTable = netPlan.getLinkTable();

        int F = w_f.length;
        numAvailableWavelengths = new double[F];
        for(int fiberId = 0; fiberId < F; fiberId++)
            numAvailableWavelengths[fiberId] = w_f[fiberId] - wavelengthOccupancy.get(fiberId).size();

        int N = netPlan.getNumberOfNodes();
	int LP = netPlan.getNumberOfRoutes();
	current_seqFibers = new int[LP][];
	current_seqWavelengths = new int[LP][];
	current_seqRegenerators = new int[LP][];
        
        regeneratorOccupancy = new int[N];

	for(int lpId = 0; lpId < LP; lpId++)
	{
	    current_seqFibers[lpId] = netPlan.getRouteSequenceOfLinks(lpId);
	    current_seqWavelengths[lpId] = WDMUtils.getLightpathSeqWavelengthsAttribute(netPlan, lpId);
            
	    current_seqRegenerators[lpId] = WDMUtils.getLightpathSeqRegeneratorsAttribute(netPlan, lpId);
            if (current_seqRegenerators[lpId].length == 0) current_seqRegenerators[lpId] = WDMUtils.computeRegeneratorPositions(fiberTable, current_seqFibers[lpId], current_seqWavelengths[lpId], l_f, maxRegeneratorDistanceInKm);
            
            int numHops = current_seqFibers[lpId].length;
            for(int hopId = 0; hopId < numHops; hopId++)
            {
                int fiberId = current_seqFibers[lpId][hopId];
                if (current_seqRegenerators[lpId][hopId] == 1)
                {
                    int nodeId = fiberTable[fiberId][0];
                    regeneratorOccupancy[nodeId]++;
                }
            }
	}

        stats_max_regeneratorOccupancy = IntUtils.copy(regeneratorOccupancy);
    }

    @Override
    public void finishTransitory(double simTime) { }
}