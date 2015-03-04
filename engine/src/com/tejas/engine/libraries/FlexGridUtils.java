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

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.libraries.ModulationFormatUtils.ModulationFormat;
import com.tejas.engine.utils.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Class implementing some static methods to assist in creating algorithms for 
 * flex-grid networks.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class FlexGridUtils
{
    /**
     * Main method to test this class.
     * 
     * @param args Command-line arguments (unused)
     * @since 0.2.3
     */
    public static void main(String[] args)
    {
        boolean[] slotAvailabilityVector_1 = new boolean[] {true, false, true, true, false};
        boolean[] slotAvailabilityVector_2 = new boolean[] {false, false, true, true, false};
        boolean[] slotAvailabilityVector_3 = new boolean[] {true, false, true, true, true};
        boolean[] slotAvailabilityVector_4 = new boolean[] {false, false, true, false, false};
        boolean[] slotAvailabilityVector_5 = new boolean[] {false, false, false, false, false};
        
        System.out.println(computeAvailableSpectrumVoids(slotAvailabilityVector_1));
        System.out.println(computeAvailableSpectrumVoids(slotAvailabilityVector_2));
        System.out.println(computeAvailableSpectrumVoids(slotAvailabilityVector_3));
        System.out.println(computeAvailableSpectrumVoids(slotAvailabilityVector_4));
        System.out.println(computeAvailableSpectrumVoids(slotAvailabilityVector_5));

        boolean[] slotAvailabilityVector_6 = new boolean[] {true, false, true, true, true, true};

        System.out.println(computeMaximumRequests(computeAvailableSpectrumVoids(slotAvailabilityVector_6), 1));
        System.out.println(computeMaximumRequests(computeAvailableSpectrumVoids(slotAvailabilityVector_6), 2));
        System.out.println(computeMaximumRequests(computeAvailableSpectrumVoids(slotAvailabilityVector_6), 3));
        System.out.println(computeMaximumRequests(computeAvailableSpectrumVoids(slotAvailabilityVector_6), 4));
        System.out.println(computeMaximumRequests(computeAvailableSpectrumVoids(slotAvailabilityVector_6), 5));
    }
    
    /**
     * <p>Computes the list of spectral voids (list of available contiguous slots) 
     * from a slot availability vector of a path.</p>
     * 
     * @param slotAvailabilityVector Slot availability vector, where each position indicates whether or not its corresponding frequency slot is available along the path
     * @return List of spectrum voids, each one with a pair indicating both the initial slot id and the number of consecutive slots within the void. If no spectrum void is found, it returns an empty list
     * @since 0.2.3
     */
    public static List<Pair<Integer, Integer>> computeAvailableSpectrumVoids(boolean[] slotAvailabilityVector)
    {
        List<Pair<Integer, Integer>> result = new LinkedList<Pair<Integer, Integer>>();
        List<Integer> curr = new LinkedList<Integer>();
        
        int totalFiberSlots = slotAvailabilityVector.length;
        for(int slotId = 0; slotId < totalFiberSlots - 1; slotId++)
        {
            if (!slotAvailabilityVector[slotId]) continue;
            
            curr.add(slotId);

            if (!slotAvailabilityVector[slotId + 1])
            {
                result.add(Pair.of(curr.get(0), curr.size()));
                curr = new LinkedList<Integer>();
            }
        }
        
        if (slotAvailabilityVector[totalFiberSlots - 1]) curr.add(totalFiberSlots - 1);
        if (!curr.isEmpty()) result.add(Pair.of(curr.get(0), curr.size()));
    
        return result;
    }

    /**
     * Computes the maximum number of requests (each one measured in number of slots) which 
     * can be allocated in a set of spectrum voids.
     * 
     * @param availableSpectrumVoids List of available spectrum voids (first item of each pair is the initial slot identifier, whereas the second one is the number of consecutive slots)
     * @param numSlots Number of required slots for a reference connection
     * @return Maximum number of requests which can be allocated in a set of spectrum voids
     * @since 0.2.3
     */
    public static int computeMaximumRequests(List<Pair<Integer, Integer>> availableSpectrumVoids, int numSlots)
    {
        int numRequests = 0;
        
        for(Pair<Integer, Integer> spectrumVoid : availableSpectrumVoids)
        {
            int numSlotsThisVoid = spectrumVoid.getSecond();
            if (numSlotsThisVoid < numSlots) continue;
            
            numRequests += (int) Math.floor((double) numSlotsThisVoid / numSlots);
        }
        
        return numRequests;
    }
    
    /**
     * Computes the number of frequency slots required for a certain amount of 
     * bandwidth (measured in Gbps), including guard-bands.
     * 
     * @param bandwidthInGbps Requested bandwidth (in Gbps)
     * @param slotGranularityInGHz Slot granularity (in GHz) 
     * @param guardBandInGHz Guard-band size (in GHz)
     * @param modulationFormat Modulation format
     * @return Number of slots required to allocate the bandwidth demand
     * @since 0.2.3
     */
    public static int computeNumberOfSlots(double bandwidthInGbps, double slotGranularityInGHz, double guardBandInGHz, ModulationFormat modulationFormat)
    {
        if (bandwidthInGbps < 0) throw new Net2PlanException("'bandwidthInGbps' must be greater or equal than zero");
        if (slotGranularityInGHz <= 0) throw new Net2PlanException("'slotGranularityInGHz' must be greater than zero");
        if (guardBandInGHz <= 0) throw new Net2PlanException("'guardBandInGHz' must be greater than zero");
        
        double requestedBandwidthInGHz = bandwidthInGbps / modulationFormat.spectralEfficiencyInBpsPerHz;
        double requiredBandwidthInGHz = requestedBandwidthInGHz + guardBandInGHz;
        int numSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);
        
        return numSlots;
    }

    /**
     * <p>Computes the slot availability vector of a path, represented by a sequence 
     * of fibers, where each position indicates whether or not its corresponding 
     * frequency slot is available along the path.</p>
     * 
     * <p><b>Important</b>: Loop-free paths should be employed, but it is not 
     * checked by the method.</p>
     * 
     * @param seqFibers (Loop-free) Sequence of traversed fibers
     * @param slotAvailabilityMatrix An <i>E</i>x<i>S</i> matrix, where each element <i>a_<sub>es</sub></i> is equal to <code>true</code> if the frequency slot <i>s</i> is available on fiber <i>e</i>; otherwise, false
     * @return Slot availability vector
     * @since 0.2.3
     */
    public static boolean[] computePathSlotAvailabilityVector(int[] seqFibers, boolean[][] slotAvailabilityMatrix)
    {
        int numSlots = slotAvailabilityMatrix[0].length;
        
        boolean[] pathSlotAvailabilityVector = new boolean[numSlots];
        for(int slotId = 0; slotId < numSlots; slotId++)
        {
            boolean isAvailable = true;
            for(int fiberId : seqFibers)
            {
                if (!slotAvailabilityMatrix[fiberId][slotId])
                {
                    isAvailable = false;
                    break;
                }
            }
            
            pathSlotAvailabilityVector[slotId] = isAvailable;
        }
        
        return pathSlotAvailabilityVector;
    }
}
