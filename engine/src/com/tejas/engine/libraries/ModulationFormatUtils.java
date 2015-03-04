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
import com.tejas.engine.utils.DoubleUtils;

/**
 * Class implementing some static methods to deal with algorithms which adapt 
 * modulation formats according to certain criteria (i.e. optical reach).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class ModulationFormatUtils
{
    /**
     * Main method to test this class.
     * 
     * @param args Command-line arguments (unused)
     * @since 0.2.3
     */
    public static void main(String[] args)
    {
       int[] seqFibers = new int[] {0, 1};
       double[] l_e = new double[] {1000, 3000};
       
       System.out.println(computeModulationFormat(seqFibers, l_e, ModulationFormat.DefaultSet));
    }
    
    /**
     * Returns the modulation format with the maximum spectral efficiency, whereas 
     * the optical reach constraint is fulfilled, for the given path.
     * 
     * @param seqFibers Sequence of traversed fibers
     * @param l_e Link length vector (in kilometers)
     * @param availableModulationFormats Set of candidate modulation formats
     * @return Best modulation format for the given path
     * @since 0.2.3
     */
    public static ModulationFormat computeModulationFormat(int[] seqFibers, double[] l_e, ModulationFormat[] availableModulationFormats)
    {
        if (availableModulationFormats == null) throw new Net2PlanException("Available modulation formats cannot be null");
        
        double pathLengthInKm = DoubleUtils.sum(DoubleUtils.select(l_e, seqFibers));
        ModulationFormat candidateModulationFormat = null;
        for(ModulationFormat modulationFormat : availableModulationFormats)
        {
            if (pathLengthInKm > modulationFormat.opticalReachInKm) continue;
            
            if (candidateModulationFormat == null || modulationFormat.spectralEfficiencyInBpsPerHz > candidateModulationFormat.spectralEfficiencyInBpsPerHz)
                candidateModulationFormat = modulationFormat;
        }
        
        if (candidateModulationFormat == null) throw new Net2PlanException("No modulation format is applicable");
        
        return candidateModulationFormat;
    }
    
    /**
     * Returns the modulation format with the maximum spectral efficiency, while 
     * the optical reach constraint is fulfilled, for each path in a {@link com.tejas.engine.libraries.CandidatePathList CandidatePathList} 
     * object.
     * 
     * @param cpl Candidate path list
     * @param l_e Link length vector (in kilometers)
     * @param availableModulationFormats Set of candidate modulation formats
     * @return Modulation format per path
     * @since 0.2.3
     */
    public static ModulationFormat[] computeModulationFormatPerPath(CandidatePathList cpl, double[] l_e, ModulationFormat[] availableModulationFormats)
    {
        int P = cpl.getNumberOfPaths();
        
        ModulationFormat[] modulationFormatPerPath = new ModulationFormat[P];
        for(int pathId = 0; pathId < P; pathId++)
        {
            int[] seqFibers = cpl.getSequenceOfLinks(pathId);
            modulationFormatPerPath[pathId] = computeModulationFormat(seqFibers, l_e, availableModulationFormats);
        }
        
        return modulationFormatPerPath;
    }
    
    /**
     * Class to define modulation formats. Data for default formats were obtained 
     * from [1].
     * 
     * @since 0.2.3
     * @see [1] Z. Zhu, W. Lu, L. Zhang, and N. Ansari, "Dynamic Service Provisioning in Elastic Optical Networks with Hybrid Single-/Multi-Path Routing," in Journal of Lightwave Technology, vol. 31, no. 1, January 2013
     */
    public static class ModulationFormat
    {
        /**
         * BPSK format (optical reach = 9600 km, spectral efficiency = 1 bps/Hz).
         * 
         * @since 0.2.3
         */
        public final static ModulationFormat BPSK = ModulationFormat.of("BPSK", 9600.0, 1.0);
        
        /**
         * QPSK format (optical reach = 4800 km, spectral efficiency = 2 bps/Hz).
         * 
         * @since 0.2.3
         */
        public final static ModulationFormat QPSK = ModulationFormat.of("QPSK", 4800.0, 2.0);
        
        /**
         * 8-QAM format (optical reach = 2400 km, spectral efficiency = 3 bps/Hz).
         * 
         * @since 0.2.3
         */
        public final static ModulationFormat QAM_8 = ModulationFormat.of("8-QAM", 2400.0, 3.0);
        
        /**
         * 16-QAM format (optical reach = 1200 km, spectral efficiency = 4 bps/Hz).
         * 
         * @since 0.2.3
         */
        public final static ModulationFormat QAM_16 = ModulationFormat.of("16-QAM", 1200.0, 4.0);
        
        /**
         * Default set of available modulations (BPSK, QPSK, 8-QAM, 16-QAM).
         * 
         * @since 0.2.3
         */
        public final static ModulationFormat[] DefaultSet = new ModulationFormat[] { BPSK, QPSK, QAM_8, QAM_16 };
        
        /**
         * Modulation name.
         * 
         * @since 0.2.3
         */
        public final String name;
        
        /**
         * Optical reach (in kilometers).
         * 
         * @since 0.2.3
         */
        public final double opticalReachInKm;
        
        /**
         * Spectral efficiency (in bps per Hz).
         * 
         * @since 0.2.3
         */
        public final double spectralEfficiencyInBpsPerHz;

        /**
         * Default constructor.
         * 
         * @param name Modulation name
         * @param opticalReachInKm Optical reach (in kilometers)
         * @param spectralEfficiencyInBpsPerHz Spectral efficiency (in bps per Hz)
         * @since 0.2.3
         */
        public ModulationFormat(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz)
        {
            if (name == null || name.isEmpty()) throw new Net2PlanException("Modulation name cannot be null");
            if (opticalReachInKm <= 0) throw new Net2PlanException("Optical reach must be greater than zero");
            if (spectralEfficiencyInBpsPerHz <= 0) throw new Net2PlanException("Spectral efficiency must be greater than zero");

            this.name = name;
            this.opticalReachInKm = opticalReachInKm;
            this.spectralEfficiencyInBpsPerHz = spectralEfficiencyInBpsPerHz;
        }

        /**
         * Factory method.
         * 
         * @param name Modulation name
         * @param opticalReachInKm Optical reach (in kilometers)
         * @param spectralEfficiencyInBpsPerHz Spectral efficiency (in bps per Hz)
         * @return New modulation format with the given parameters
         * @since 0.2.3
         */
        public static ModulationFormat of(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz)
        {
            return new ModulationFormat(name, opticalReachInKm, spectralEfficiencyInBpsPerHz);
        }
        
        @Override
        public String toString()
        {
            return name + ": optical reach = " + opticalReachInKm + " km, spectral efficiency = " + spectralEfficiencyInBpsPerHz + " bps/Hz";
        }
    }
}
