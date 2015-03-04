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

package com.tejas.engine.libraries;

import cern.colt.list.tint.IntArrayList;

import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;

import java.util.*;

/**
 * Class to deal with optical topologies including wavelength assignment and regenerator placement.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public class WDMUtils
{
    /**
     * Returns the total number of wavelengths on each fiber.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @return Number of wavelengths per fiber
     * @since 0.2.3
     */
    public static int[] getFiberNumWavelengthsAttributes(NetPlan physicalLayer)
    {
        int E = physicalLayer.getNumberOfLinks();
        
        int[] w_f = new int[E];
        for(int fiberId = 0; fiberId < E; fiberId++)
        {
            String aux_numWavelengths = physicalLayer.getLinkAttribute(fiberId, "numWavelengths");
            if (aux_numWavelengths == null) throw new WDMException("Fiber " + fiberId + " doesn't have a 'numWavelengths' attribute");
            
            try { w_f[fiberId] = Integer.parseInt(aux_numWavelengths); }
            catch(Throwable e) { throw new WDMException("Attribute 'numWavelengths' for fiber " + fiberId + " is not a valid integer"); }
            
            if (w_f[fiberId] < 0) throw new WDMException("Attribute 'numWavelengths' for fiber " + fiberId + " must be a non-negative integer");
        }
        
        return w_f;
    }
    
    /**
     * Returns the sequence of regenerators/wavelength converters for the given lightpath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @return Sequence of regenerators/wavelength converters
     * @since 0.2.3
     */
    public static int[] getLightpathSeqRegeneratorsAttribute(NetPlan physicalLayer, int lpId)
    {
        int[] seqRegenerators = StringUtils.toIntArray(StringUtils.split(physicalLayer.getRouteAttribute(lpId, "seqRegenerators"), " "));
        return seqRegenerators;
    }
    
    /**
     * Returns the sequence of wavelengths for the given lightpath.
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @return
     * @since 0.2.3
     */
    public static int[] getLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId)
    {
        int[] seqWavelengths = StringUtils.toIntArray(StringUtils.split(physicalLayer.getRouteAttribute(lpId, "seqWavelengths"), " "));
        return seqWavelengths;
    }
    
    /**
     * Returns the sequence of wavelengths for each lighptath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @return Sequence of wavelengths for each lightpath
     * @since 0.2.3
     */
    public static List<int[]> getLightpathSeqWavelengthsAttributes(NetPlan physicalLayer)
    {
        List<int[]> out = new LinkedList<int[]>();
        
        int R = physicalLayer.getNumberOfRoutes();
        for(int routeId = 0; routeId < R; routeId++)
            out.add(getLightpathSeqWavelengthsAttribute(physicalLayer, routeId));
        
        return out;
    }
    
    /**
     * Returns the sequence of regenerators/wavelength converters for the given lightpath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @return
     * @since 0.2.3
     */
    public static int[] getProtectionLightpathSeqRegeneratorsAttribute(NetPlan physicalLayer, int lpId)
    {
        int[] seqRegenerators = StringUtils.toIntArray(StringUtils.split(physicalLayer.getProtectionSegmentAttribute(lpId, "seqRegenerators"), " "));
        return seqRegenerators;
    }

    /**
     * Returns the sequence of wavelengths for a given protection lightpath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @return Sequence of wavelengths
     * @since 0.2.3
     */
    public static int[] getProtectionLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId)
    {
        int[] seqWavelengths = StringUtils.toIntArray(StringUtils.split(physicalLayer.getProtectionSegmentAttribute(lpId, "seqWavelengths"), " "));
        return seqWavelengths;
    }

    /**
     * Sets the number of wavelengths available on each fiber to the same value.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param numWavelengths Number of wavelengths for all fibers
     * @since 0.2.3
     */
    public static void setFiberNumWavelengthsAttributes(NetPlan physicalLayer, int numWavelengths)
    {
        if (numWavelengths < 0) throw new Net2PlanException("'numWavelengths' must be a non-negative integer");

        int E = physicalLayer.getNumberOfLinks();
        for(int fiberId = 0; fiberId < E; fiberId++) physicalLayer.setLinkAttribute(fiberId, "numWavelengths", Integer.toString(numWavelengths));
    }

    /**
     * Sets the number of wavelengths available on each fiber.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param w_f Number of wavelengths per fiber
     * @since 0.2.3
     */
    public static void setFiberNumWavelengthsAttributes(NetPlan physicalLayer, int[] w_f)
    {
	if (IntUtils.minValue(w_f) <= 0) throw new Net2PlanException("Number of wavelengths per fiber must be greater than zero");

        int E = physicalLayer.getNumberOfLinks();
        for(int fiberId = 0; fiberId < E; fiberId++) physicalLayer.setLinkAttribute(fiberId, "numWavelengths", Integer.toString(w_f[fiberId]));
    }
    
    /**
     * Sets the sequence of regenerators/wavelength converters for a given lightpath.
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param seqRegenerators Sequence of regenerators/wavelength converters (as many as the number of links in the lightpath)
     * @since 0.2.3
     */
    public static void setLightpathSeqRegeneratorsAttribute(NetPlan physicalLayer, int lpId, int[] seqRegenerators)
    {
        physicalLayer.setRouteAttribute(lpId, "seqRegenerators", IntUtils.join(seqRegenerators, " "));
    }

    /**
     * Sets the sequence of wavelengths for the given lightpath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     * @since 0.2.3
     */
    public static void setLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId, int[] seqWavelengths)
    {
        physicalLayer.setRouteAttribute(lpId, "seqWavelengths", IntUtils.join(seqWavelengths, " "));
    }

    /**
     * Sets the current wavelength for the given lightpath, assuming no wavelength conversion.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param wavelengthId Wavelength identifier for all traversed fibers
     * @since 0.2.3
     */
    public static void setLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId, int wavelengthId)
    {
        physicalLayer.setRouteAttribute(lpId, "seqWavelengths", IntUtils.join(IntUtils.mult(IntUtils.ones(physicalLayer.getRouteNumberOfHops(lpId)), wavelengthId), " "));
    }

    /**
     * Sets the sequence of regenerators/wavelength converters for a given protection lightpath.
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param seqRegenerators Sequence of regenerators/wavelength converters (as many as the number of links in the lightpath)
     * @since 0.2.3
     */
    public static void setProtectionLightpathSeqRegeneratorsAttribute(NetPlan physicalLayer, int lpId, int[] seqRegenerators)
    {
        physicalLayer.setProtectionSegmentAttribute(lpId, "seqRegenerators", IntUtils.join(seqRegenerators, " "));
    }

    /**
     * Sets the sequence of wavelengths for a protection lightpath.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     * @since 0.2.3
     */
    public static void setProtectionLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId, int[] seqWavelengths)
    {
        physicalLayer.setProtectionSegmentAttribute(lpId, "seqWavelengths", IntUtils.join(seqWavelengths, " "));
    }
    
    /**
     * Sets the sequence of wavelengths for a protection lightpath, assuming no wavelength conversion.
     * 
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param lpId Lightpath identifier
     * @param wavelengthId Wavelength identifier for all traversed fibers
     * @since 0.2.3
     */
    public static void setProtectionLightpathSeqWavelengthsAttribute(NetPlan physicalLayer, int lpId, int wavelengthId)
    {
        physicalLayer.setProtectionSegmentAttribute(lpId, "seqWavelengths", IntUtils.join(IntUtils.mult(IntUtils.ones(physicalLayer.getProtectionSegmentNumberOfHops(lpId)), wavelengthId), " "));
    }

    /**
     * <p>Performs extra checks to those applicable to every network design, especially
     * focused on WDM networks.</p>
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.3
     */
    public static void checkConsistency(NetPlan physicalLayer, Map<String, String> net2planParameters)
    {
        int[] w_f = getFiberNumWavelengthsAttributes(physicalLayer);
        
        int D = physicalLayer.getNumberOfDemands();
        for(int lpDemandId = 0; lpDemandId < D; lpDemandId++)
        {
//            double h_d = physicalLayer.getDemandOfferedTrafficInErlangs(lpDemandId);
//            if (h_d < PRECISIONFACTOR) throw new WDMException("Lightpath demand " + lpDemandId + " must request have a non-zero binary rate");
//            
//            int numLps = (int) Math.ceil(h_d / binaryRatePerWDMChannel);
//            if (!DoubleUtils.isEqualWithinRelativeTolerance(numLps * binaryRatePerWDMChannel, h_d, PRECISIONFACTOR))
//                throw new WDMException("Lightpath demand " + lpDemandId + " must request a binary rate equal to an integer multiple of 'binaryRatePerWDMChannel'");
            
            int[] lpIds = physicalLayer.getDemandRoutes(lpDemandId);
//            if (lpIds.length > numLps) throw new WDMException("Number of carried lightpaths for demand " + lpDemandId + " is greater than the offered one");
            
            for(int lpId : lpIds)
            {
//                double x_p = physicalLayer.getRouteCarriedTrafficInErlangs(lpId);
//                if (x_p < PRECISIONFACTOR) throw new WDMException("Lightpath " + lpId + " must carry a non-zero binary rate");
//                
//                if (!DoubleUtils.isEqualWithinRelativeTolerance(binaryRatePerWDMChannel, x_p, PRECISIONFACTOR))
//                    throw new WDMException("Carried traffic by lightpath " + lpId + " must be equal to 'binaryRatePerWDMChannel'");
                
                int numHops_thisLp = physicalLayer.getRouteNumberOfHops(lpId);
                int[] seqWavelengths_thisLp = getLightpathSeqWavelengthsAttribute(physicalLayer, lpId);
                int[] seqRegenerators_thisLp = getLightpathSeqRegeneratorsAttribute(physicalLayer, lpId);
                if (seqWavelengths_thisLp.length != numHops_thisLp) throw new WDMException("Length of sequence of wavelengths for lightpath " + lpId + " does not match the number of traversed fibers");
                if (seqRegenerators_thisLp.length > 0 && seqRegenerators_thisLp.length != numHops_thisLp) throw new WDMException("Length of sequence of regenerators/wavelength converters for lightpath " + lpId + " does not match the number of traversed fibers");
            }
        }
        
        int S = physicalLayer.getNumberOfProtectionSegments();
        for(int protectionLpId = 0; protectionLpId < S; protectionLpId++)
        {
//            double u_s = physicalLayer.getProtectionSegmentReservedBandwidthInErlangs(protectionLpId);
//            if (u_s < PRECISIONFACTOR) throw new WDMException("Protection lightpath " + protectionLpId + " must carry a non-zero binary rate");
//
//            if (!DoubleUtils.isEqualWithinRelativeTolerance(binaryRatePerWDMChannel, u_s, PRECISIONFACTOR))
//                throw new WDMException("Protected traffic by protection lightpath " + protectionLpId + " must be equal to 'binaryRatePerWDMChannel'");
//                
//            int originNodeId_thisProtectionLp = physicalLayer.getProtectionSegmentOriginNode(protectionLpId);
//            int destinationNodeId_thisProtectionLp = physicalLayer.getProtectionSegmentDestinationNode(protectionLpId);
            
            int numHops_thisProtectionLp = physicalLayer.getProtectionSegmentNumberOfHops(protectionLpId);
            int[] seqWavelengths_thisProtectionLp = getProtectionLightpathSeqWavelengthsAttribute(physicalLayer, protectionLpId);
            int[] seqRegenerators_thisProtectionLp = getProtectionLightpathSeqRegeneratorsAttribute(physicalLayer, protectionLpId);
            if (seqWavelengths_thisProtectionLp.length != numHops_thisProtectionLp) throw new WDMException("Length of sequence of wavelengths for protection lightpath " + protectionLpId + " does not match the number of traversed fibers");
            if (seqRegenerators_thisProtectionLp.length > 0 && seqRegenerators_thisProtectionLp.length != numHops_thisProtectionLp) throw new WDMException("Length of sequence of regenerators/wavelength converters for protection lightpath " + protectionLpId + " does not match the number of traversed fibers");
            
//            int[] lpIds = physicalLayer.getProtectionSegmentRoutes(protectionLpId);
//            for(int lpId : lpIds)
//            {
//                int originNodeId_thisLp = physicalLayer.getRouteIngressNode(lpId);
//                int destinationNodeId_thisLp = physicalLayer.getRouteEgressNode(lpId);
//                
//                if (originNodeId_thisLp != originNodeId_thisProtectionLp) throw new WDMException("Protection lightpath " + protectionLpId + " cannot be used for lightpath " + lpId + " since does not protect the whole physical route");
//                if (destinationNodeId_thisLp != destinationNodeId_thisProtectionLp) throw new WDMException("Protection lightpath " + protectionLpId + " cannot be used for lightpath " + lpId + " since does not protect the whole physical route");
//            }
        }
        
	getRegeneratorOccupancy(physicalLayer);
	getWavelengthOccupancy(physicalLayer);
    }

    /**
     * Returns the list of nodes within the lightpath route containing a regenerator,
     * only following a distance criterium, assuming no wavelength conversion is required.
     *
     * @param fiberTable Set of installed fibers (first column: origin node, second column: destination node)
     * @param seqFibers Sequence of fibers traversed by the lightpath
     * @param l_f Physical length in km per fiber
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
     * @since 0.2.3
     */
    public static int[] computeRegeneratorPositions(int[][] fiberTable, int[] seqFibers, double[] l_f, double maxRegeneratorDistanceInKm)
    {
        int numHops = seqFibers.length;
        
        double accumDistance = 0;
        int[] seqRegenerators = new int[numHops];
        
        for(int hopId = 0; hopId < numHops; hopId++)
        {
            int fiberId = seqFibers[hopId];
            double fiberLengthInKm = l_f[fiberId];
            
	    if(fiberLengthInKm > maxRegeneratorDistanceInKm)
		throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiberId, fiberLengthInKm, maxRegeneratorDistanceInKm));

	    accumDistance += fiberLengthInKm;

	    if (accumDistance > maxRegeneratorDistanceInKm)
	    {
                seqRegenerators[hopId] = 1;
		accumDistance = l_f[fiberId];
	    }
            else
            {
                seqRegenerators[hopId] = 0;
            }
        }
        
        return seqRegenerators;
    }

    /**
     * Returns the list of nodes within the lightpath route containing a regenerator,
     * only following a distance criterium, assuming no wavelength conversion is required.
     *
     * @param fiberTable Set of installed fibers (first column: origin node, second column: destination node)
     * @param seqFibers Sequence of fibers traversed by the lightpath
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     * @param l_f Physical length in km per fiber
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
     * @since 0.2.3
     */
    public static int[] computeRegeneratorPositions(int[][] fiberTable, int[] seqFibers, int[] seqWavelengths, double[] l_f, double maxRegeneratorDistanceInKm)
    {
        int numHops = seqFibers.length;
        
        double accumDistance = 0;
        int[] seqRegenerators = new int[numHops];
        
        for(int hopId = 0; hopId < numHops; hopId++)
        {
            int fiberId = seqFibers[hopId];
            double fiberLengthInKm = l_f[fiberId];
            
	    if(fiberLengthInKm > maxRegeneratorDistanceInKm)
		throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiberId, fiberLengthInKm, maxRegeneratorDistanceInKm));

	    accumDistance += fiberLengthInKm;

	    if (accumDistance > maxRegeneratorDistanceInKm || (hopId > 0 && seqWavelengths[hopId - 1] != seqWavelengths[hopId]))
	    {
                seqRegenerators[hopId] = 1;
		accumDistance = l_f[fiberId];
	    }
            else
            {
                seqRegenerators[hopId] = 0;
            }
        }
        
        return seqRegenerators;
    }

    /**
     * <p>Returns the number of regenerators installed per node.</p>
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a physical topology
     * @return Number of regenerators installed per node
     * @since 0.2.1
     */
    public static int[] getRegeneratorOccupancy(NetPlan physicalLayer)
    {
        int N = physicalLayer.getNumberOfNodes();
        int LP = physicalLayer.getNumberOfRoutes();
        int S = physicalLayer.getNumberOfProtectionSegments();

        int[] regeneratorOccupancy = new int[N];

        for(int lightpathId = 0; lightpathId < LP; lightpathId++)
        {
            int[] seqFibers = physicalLayer.getRouteSequenceOfLinks(lightpathId);
            int[] seqRegenerators = getLightpathSeqRegeneratorsAttribute(physicalLayer, lightpathId);
            if (seqRegenerators.length == 0) continue;

            for(int hopId = 0; hopId < seqFibers.length; hopId++)
            {
                if (seqRegenerators[hopId] == 0) continue;
                
                int fiberId = seqFibers[hopId];
                int nodeId = physicalLayer.getLinkOriginNode(fiberId);
                regeneratorOccupancy[nodeId]++;
            }
        }

        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            int[] seqFibers = physicalLayer.getProtectionSegmentSequenceOfLinks(segmentId);
            int[] seqRegenerators = getProtectionLightpathSeqRegeneratorsAttribute(physicalLayer, segmentId);
            if (seqRegenerators.length == 0) continue;

            for(int hopId = 0; hopId < seqFibers.length; hopId++)
            {
                if (seqRegenerators[hopId] == 0) continue;
                
                int fiberId = seqFibers[hopId];
                int nodeId = physicalLayer.getLinkOriginNode(fiberId);
                regeneratorOccupancy[nodeId]++;
            }
        }

	return regeneratorOccupancy;
    }

    /**
     * <p>Returns the set of used wavelengths per fiber.</p>
     *
     * @param physicalLayer A {@link com.tejas.engine.interfaces.networkDesign.NetPlan} representing a physical topology
     * @return Set of used wavelengths per fiber
     * @since 0.2.1
     */
    public static List<Set<Integer>> getWavelengthOccupancy(NetPlan physicalLayer)
    {
        int F = physicalLayer.getNumberOfLinks();
        int LP = physicalLayer.getNumberOfRoutes();
	int S = physicalLayer.getNumberOfProtectionSegments();

        int[] w_f = getFiberNumWavelengthsAttributes(physicalLayer);

        List<Set<Integer>> wavelengthOccupancy = new ArrayList<Set<Integer>>();

        for(int fiberId = 0; fiberId < F; fiberId++)
            wavelengthOccupancy.add(new HashSet<Integer>());

        for(int lightpathId = 0; lightpathId < LP; lightpathId++)
        {
            int[] seqFibers = physicalLayer.getRouteSequenceOfLinks(lightpathId);
            int[] seqWavelengths = getLightpathSeqWavelengthsAttribute(physicalLayer, lightpathId);

            for(int hopId = 0; hopId < seqFibers.length; hopId++)
            {
                int fiberId = seqFibers[hopId];
                int wavelengthId = seqWavelengths[hopId];

                if (w_f[fiberId] <= wavelengthId)
                    throw new WDMException(String.format("Fiber %d only has %d wavelengths (lightpath %d, wavelength %d)", fiberId, w_f[fiberId], lightpathId, wavelengthId));

                if (wavelengthOccupancy.get(fiberId).contains(wavelengthId))
                    throw new WDMException(String.format("Two lightpaths/segments cannot share a wavelength (fiber %d, wavelength %d)", fiberId, wavelengthId));

                wavelengthOccupancy.get(fiberId).add(wavelengthId);
            }
        }

        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            int[] seqFibers = physicalLayer.getProtectionSegmentSequenceOfLinks(segmentId);
            int[] seqWavelengths = getProtectionLightpathSeqWavelengthsAttribute(physicalLayer, segmentId);

            for(int hopId = 0; hopId < seqFibers.length; hopId++)
            {
                int fiberId = seqFibers[hopId];
                int wavelengthId = seqWavelengths[hopId];

                if (w_f[fiberId] <= wavelengthId)
                    throw new WDMException(String.format("Fiber %d only has %d wavelengths (segment %d, wavelength %d)", fiberId, w_f[fiberId], segmentId, wavelengthId));

                if (wavelengthOccupancy.get(fiberId).contains(wavelengthId))
                    throw new WDMException(String.format("Two lightpaths/segments cannot share a wavelength (fiber %d, wavelength %d)", fiberId, wavelengthId));

                wavelengthOccupancy.get(fiberId).add(wavelengthId);
            }
        }

	return wavelengthOccupancy;
    }
    
    
    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
     * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
     * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
     * assigned to each lightpath (along the whole physical route) is the minimum
     * index among the common free-wavelength set for all traversed fibers.</p>
     *
     * <p>In case a lightpath cannot be allocated, the corresponding sequence of
     * wavelengths (<code>seqWavelengths</code> parameter) will be an empty array.</p>
     *
     * @param fiberTable Set of installed fibers (first column: origin node, second column: destination node)
     * @param seqFibers Sequence of traversed fibers
     * @param w_f Number of wavelengths per fiber
     * @param wavelengthOccupancy Set of used wavelengths per fiber
     * @return Sequence of wavelengths traversed by each lightpath
     * @since 0.2.1
     */
    public static int[] WA_firstFit(int[][] fiberTable, int[] seqFibers, int[] w_f, List<Set<Integer>> wavelengthOccupancy)
    {
        GraphUtils.checkRouteContinuity(fiberTable, seqFibers, GraphUtils.CheckRoutingCycleType.NO_REPEAT_LINK);
        
        int num_w = IntUtils.minValue(IntUtils.select(w_f, seqFibers));
        for(int wId = 0; wId < num_w; wId++)
        {
            boolean isOccupied = false;

            for(int fiberId : seqFibers)
            {
                if (wavelengthOccupancy.get(fiberId).contains(wId))
                {
                    isOccupied = true;
                    break;
                }
            }

            if(isOccupied) continue;

            int[] seqWavelengths = new int[seqFibers.length];
            Arrays.fill(seqWavelengths, wId);

            for(int fiberId : seqFibers)
                wavelengthOccupancy.get(fiberId).add(wId);

            return seqWavelengths;
        }
        
        return new int[0];
    }

    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
     * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
     * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
     * assigned to each lightpath (along the whole physical route) is the minimum
     * index among the common free-wavelength set for all traversed fibers.</p>
     *
     * <p>In case a lightpath cannot be allocated, the corresponding sequence of
     * wavelengths (<code>seqWavelengths</code> parameter) will be an empty array.</p>
     *
     * @param fiberTable Set of installed fibers (first column: origin node, second column: destination node)
     * @param lightpaths Sequence of fibers traversed by each lightpath
     * @param w_f Number of wavelengths per fiber
     * @param wavelengthOccupancy Set of used wavelengths per fiber
     * @return Sequence of wavelengths traversed by each lightpath
     * @since 0.2.3
     */
    public static List<int[]> WA_firstFit(int[][] fiberTable, List<int[]> lightpaths, int[] w_f, List<Set<Integer>> wavelengthOccupancy)
    {
        List<int[]> seqWavelengths = new LinkedList<int[]>();
        Iterator<int[]> it = lightpaths.iterator();
        while(it.hasNext())
        {
            int[] seqFibers = it.next();
            GraphUtils.checkRouteContinuity(fiberTable, seqFibers, GraphUtils.CheckRoutingCycleType.NO_REPEAT_LINK);
        
            int num_w = IntUtils.minValue(IntUtils.select(w_f, seqFibers));
            int granted_w = -1;

            for(int wId = 0; wId < num_w; wId++)
            {
                boolean isOccupied = false;

                for(int fiberId : seqFibers)
                {
                    if (wavelengthOccupancy.get(fiberId).contains(wId))
                    {
                        isOccupied = true;
                        break;
                    }
                }

                if(isOccupied) continue;

                granted_w = wId;

                int[] lambdas = new int[seqFibers.length];
                Arrays.fill(lambdas, granted_w);
                seqWavelengths.add(lambdas);

                for(int fiberId : seqFibers)
                    wavelengthOccupancy.get(fiberId).add(granted_w);

                break;
            }

            if (granted_w == -1) seqWavelengths.add(new int[0]);
        }
        
        return seqWavelengths;
    }

    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion assuming
     * full wavelength conversion and regeneration. Each node selects the first
     * free wavelength for its output fiber, and next nodes in the lightpath try
     * to maintain it. If not possible, or regeneration is needed, then include
     * a regenerator (can act also as a full wavelength converter) and search
     * for the first free wavelength, and so on.</p>
     *
     * <p>In case a lightpath cannot be allocated, the corresponding sequence of
     * wavelengths (<code>seqWavelengths</code> parameter) will be an empty array.</p>
     *
     * @param fiberTable Set of installed fibers (first column: origin node, second column: destination node)
     * @param lightpaths Sequence of fibers traversed by each lightpath
     * @param w_f Number of wavelengths per fiber
     * @param wavelengthOccupancy Set of used wavelengths per fiber
     * @param seqWavelengths Sequence of wavelengths traversed by each lightpath
     * @param l_f Physical length in km per fiber
     * @param regeneratorOccupancy Number of regenerators installed per node
     * @param seqRegenerators A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     */
    public static void WA_RPP_firstFit(int[][] fiberTable, List<int[]> lightpaths, int[] w_f, List<Set<Integer>> wavelengthOccupancy, List<int[]> seqWavelengths, double[] l_f, int[] regeneratorOccupancy, List<int[]> seqRegenerators, double maxRegeneratorDistanceInKm)
    {
	int W = IntUtils.maxValue(w_f);

        ListIterator<int[]> it = lightpaths.listIterator();
        while(it.hasNext())
        {
            int[] seqFibers = it.next();

            IntArrayList aux_seqWavelengths = new IntArrayList();
            IntArrayList aux_seqRegenerators = new IntArrayList();

	    double control_accumDistance = 0;
	    Set<Integer> control_occupied_w = new HashSet<Integer>();
	    int control_firstFitValidWavelengthForSubpath = -1;
	    List<Integer> control_currentSubpathSeqLinks = new LinkedList<Integer> ();

	    boolean lpAllocated = true;

	    Map<Integer, Set<Integer>> avoidLoopWavelengthClash = new HashMap<Integer, Set<Integer>>();

	    for (int fiberId : seqFibers)
	    {
                if(l_f[fiberId] > maxRegeneratorDistanceInKm)
                    throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiberId, l_f[fiberId], maxRegeneratorDistanceInKm));

		/* update the info as if this link was included in the subpath */
		final double plusLink_accumDistance = control_accumDistance + l_f[fiberId];
		int plusLink_firstFitValidWavelengthForSubpath = -1;
		Set<Integer> plusLink_occupied_w = new HashSet<Integer>(control_occupied_w);
		for(int wavelengthId = w_f[fiberId]; wavelengthId < W; wavelengthId++)
		    plusLink_occupied_w.add(wavelengthId);

		if (avoidLoopWavelengthClash.containsKey(fiberId))
		    plusLink_occupied_w.addAll(avoidLoopWavelengthClash.get(fiberId));

		for(int wavelengthId = w_f[fiberId] - 1; wavelengthId >= 0; wavelengthId--)
		{
		    if(!plusLink_occupied_w.contains(wavelengthId) && !wavelengthOccupancy.get(fiberId).contains(wavelengthId))
			plusLink_firstFitValidWavelengthForSubpath = wavelengthId;
		    else
			plusLink_occupied_w.add(wavelengthId);
		}

		if (!control_currentSubpathSeqLinks.contains(fiberId) && plusLink_accumDistance <= maxRegeneratorDistanceInKm && plusLink_firstFitValidWavelengthForSubpath != -1)
		{
		    /* we do not have to put a regenerator in the origin node of e: the subpath is valid up to now */
		    control_accumDistance = plusLink_accumDistance;
		    control_occupied_w = plusLink_occupied_w;
		    control_firstFitValidWavelengthForSubpath = plusLink_firstFitValidWavelengthForSubpath;
		    control_currentSubpathSeqLinks.add(fiberId);
                    aux_seqRegenerators.add(0);
		    continue;
		}

		/* Here if we have to put a regenerator in initial node of this link, add a subpath */
		if (control_firstFitValidWavelengthForSubpath == -1)
		{
		    lpAllocated = false;
		    break;
		}
                aux_seqRegenerators.add(1);
		int numFibersSubPath = control_currentSubpathSeqLinks.size();
		for (int cont = 0 ; cont < numFibersSubPath ; cont ++)
		{
		    aux_seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

		    int aux_fiberId = control_currentSubpathSeqLinks.get(cont);
		    if (!avoidLoopWavelengthClash.containsKey(aux_fiberId))
			avoidLoopWavelengthClash.put(aux_fiberId, new HashSet<Integer>());

		    avoidLoopWavelengthClash.get(aux_fiberId).add(control_firstFitValidWavelengthForSubpath);
		}

		/* new span includes just this link */
		control_accumDistance = l_f[fiberId];
		control_currentSubpathSeqLinks = new LinkedList<Integer>();
		control_currentSubpathSeqLinks.add(fiberId);
		control_occupied_w = new HashSet<Integer>(wavelengthOccupancy.get(fiberId));
		if (avoidLoopWavelengthClash.containsKey(fiberId))
		    control_occupied_w.addAll(avoidLoopWavelengthClash.get(fiberId));

		control_firstFitValidWavelengthForSubpath = -1;
		for(int wavelengthId = 0; wavelengthId < w_f[fiberId]; wavelengthId++)
		{
		    if(!control_occupied_w.contains(wavelengthId))
		    {
			control_firstFitValidWavelengthForSubpath = wavelengthId;
			break;
		    }
		}

		if (control_firstFitValidWavelengthForSubpath == -1)
		{
		    lpAllocated = false;
		    break;
		}
	    }

	    /* Add the last subpath */
	    /* Here if we have to put a regenerator in initial node of this link, add a subpath */
	    if (control_firstFitValidWavelengthForSubpath == -1)
		lpAllocated = false;

	    if (!lpAllocated)
	    {
                seqWavelengths.add(new int[0]);
                seqRegenerators.add(new int[0]);

		continue;
	    }

	    int numFibersSubPath = control_currentSubpathSeqLinks.size();
	    for (int cont = 0 ; cont < numFibersSubPath ; cont ++)
		aux_seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

	    aux_seqWavelengths.trimToSize();
	    seqWavelengths.add(aux_seqWavelengths.elements());

	    aux_seqRegenerators.trimToSize();
	    seqRegenerators.add(aux_seqRegenerators.elements());

	    for(int hopId = 0; hopId < seqFibers.length; hopId++)
	    {
		int fiberId = seqFibers[hopId];
		int wavelengthId = aux_seqWavelengths.getQuick(hopId);
		wavelengthOccupancy.get(fiberId).add(wavelengthId);
                
                if (aux_seqRegenerators.get(hopId) == 1)
                {
                    int nodeId = fiberTable[fiberId][0];
                    regeneratorOccupancy[nodeId]++;
                }
	    }
	}
    }
    
    private static class WDMException extends Net2PlanException
    {
        public WDMException(String message) { super("WDM: " + message); }
    }
}
