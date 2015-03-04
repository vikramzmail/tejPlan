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

package com.tejas.workspace.examples.reports;

import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.Constants.OrderingType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>This report shows line engineering information for WDM links in the network.</p>
 * 
 * <p>The report assumes that the network links are WDM optical fibres with the following scheme:
 * <ul>
 * <li>A transmitter per WDM channel with the specifications given by "tp__XXX". The number of 
 * channels can vary from one to up to channels_maxNumChannels and, the design should be correct 
 * in all the cases. Transmitter specifications are set in "tp__XXX" input parameters</li>
 * <li>A multiplexer that receives the input power from the transmitters and with specifications 
 * given by "mux__XXX" parameters</li>
 * <li>A fiber link of a distance given by the link length, and with specifications given by 
 * "fiber__XXX" parameters. The fiber can be split into spans if optical amplifers (EDFAs) 
 * and/or dispersion compensating modules (DCMs) are placed along the fiber.</li>
 * <li>A set of optical amplifiers (EDFAs) located in none, one or more positions in the 
 * fiber link, separating them in different spans. EDFAs are supposed to operate in the 
 * automatic gain control mode. Thus, the gain is the same, whatever the number of input 
 * WDM channels. EDFA positions (as distance in km from the link start to the EDFA location) 
 * and EDFA gains (assumed in dB) are read from the "edfaPositions_km" and "edfaGains_dB" 
 * attributes of the links. The format of both attributes are the same: a string of numbers 
 * separated by spaces. The <i>i</i>-th number corresponding to the position/gain of the 
 * <i>i</i>-th EDFA. If the attributes do not exist, it is assumed that no EDFAs are placed 
 * in this link. EDFA specifications given by "edfa__XXX" parameters</li>
 * <li>A set of dispersion compensating modules (DCMs) located in none, one or more positions 
 * in the fiber link, separating them in different spans. If a DCM and a EDFA have the same 
 * location, it is assumed that the DCM is placed first, to reduce the non-linear effects. DCM 
 * positions (as distance in km from the link start to the DCM location) are read from the 
 * "dcmPositions_km" attribute of the link, and the same format as with "edfaPositions_km" 
 * attribute is expected. If the attribute does not exist, it is assumed that no DCMs are 
 * placed in this link. DCM specifications are given by "dcm__XXX" parameters</li>
 * <li>At the receiver end, WDM channels in the links are separated using a demultiplexer, 
 * with specifications given by "mux__XXX" parameters</li>
 * <li>Each channel ends in a receiver, with specifications given by "tp__XXX" parameters</li>
 * </ul></p>
 * 
 * <p>The basic checks performed are:</p>
 * <ul>
 * <li>Signal power levels are within operating ranges at the mux/demux/edfas/dcms and 
 * receivers, both when the link has one single active channel, or when all the 
 * "channels__maxNumChannels" are active</li>
 * <li>Chromatic dispersion is within the operating ranges in every point of the fiber, 
 * and at the receiver</li>
 * <li>Optical Signal to Noise Ration (OSNR) is within the operating range at the receiver</li>
 * <li>Polarization mode dispersion (PMD) is within the operating range at the receiver</li>
 * </ul></p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, February 2014
 */
public class Report_WDM_pointToPointLineEngineering implements IReport
{
    private final double constant_c = 299792458; // speed of light in m/s
    private final double constant_h = 6.626E-34; // Plank constant m^2 kg/sec
    private final double precisionMargenForChecks_dB = 0.00001; 

    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
        final boolean report__checkCDOnlyAtTheReceiver = Boolean.parseBoolean(reportParameters.get("report__checkCDOnlyAtTheReceiver"));

        /* Usable wavelengths */
        final double channels__minChannelLambda_nm = Double.parseDouble(reportParameters.get("channels__minChannelLambda_nm"));
        final double channels__channelSpacing_GHz = Double.parseDouble(reportParameters.get("channels__channelSpacing_GHz"));
        final int channels__maxNumChannels = Integer.parseInt(reportParameters.get("channels__maxNumChannels"));
        final double channels__maxChannelLambda_nm = this.constant_c / ((this.constant_c / channels__minChannelLambda_nm) - (channels__maxNumChannels - 1) * channels__channelSpacing_GHz); 

        /* Fiber specifications  */
        final String fiber__ituType = reportParameters.get("fiber__ituType");
        final double fiber__attenuation_dB_per_km = Double.parseDouble(reportParameters.get("fiber__attenuation_dB_per_km"));
        final double fiber__worseChromaticDispersion_ps_per_nm_per_km = Double.parseDouble(reportParameters.get("fiber__worseChromaticDispersion_ps_per_nm_per_km"));
        final double fiber__PMD_ps_per_sqroot_km = Double.parseDouble(reportParameters.get("fiber__PMD_ps_per_sqroot_km"));

        /* Transponder specifications */
        final double tp__outputPower_dBm = Double.parseDouble(reportParameters.get("tp__outputPower_dBm"));
        final double tp__maxChromaticDispersionTolerance_ps_per_nm = Double.parseDouble(reportParameters.get("tp__maxChromaticDispersionTolerance_ps_per_nm"));
        final double tp__minOSNR_dB = Double.parseDouble(reportParameters.get("tp__minOSNR_dB"));
        final double tp__inputPowerSensitivityMin_dBm = Double.parseDouble(reportParameters.get("tp__inputPowerSensitivityMin_dBm"));
        final double tp__inputPowerSensitivityMax_dBm = Double.parseDouble(reportParameters.get("tp__inputPowerSensitivityMax_dBm"));
        final double tp__minWavelength_nm = Double.parseDouble(reportParameters.get("tp__minWavelength_nm"));
        final double tp__maxWavelength_nm = Double.parseDouble(reportParameters.get("tp__maxWavelength_nm"));
        final double tp__pmdTolerance_ps = Double.parseDouble(reportParameters.get("tp__pmdTolerance_ps"));

        /* Optical amplifier specifications */
        final double edfa__minWavelength_nm = Double.parseDouble(reportParameters.get("edfa__minWavelength_nm"));
        final double edfa__maxWavelength_nm = Double.parseDouble(reportParameters.get("edfa__maxWavelength_nm"));
        final double edfa__minInputPower_dBm = Double.parseDouble(reportParameters.get("edfa__minInputPower_dBm"));
        final double edfa__maxInputPower_dBm = Double.parseDouble(reportParameters.get("edfa__maxInputPower_dBm"));
        final double edfa__minOutputPower_dBm = Double.parseDouble(reportParameters.get("edfa__minOutputPower_dBm"));
        final double edfa__maxOutputPower_dBm = Double.parseDouble(reportParameters.get("edfa__maxOutputPower_dBm"));
        final double edfa__minGain_dB = Double.parseDouble(reportParameters.get("edfa__minGain_dB"));
        final double edfa__maxGain_dB = Double.parseDouble(reportParameters.get("edfa__maxGain_dB"));
        final double edfa__PMD_ps = Double.parseDouble(reportParameters.get("edfa__PMD_ps"));
        final double edfa__noiseFactorMaximumGain_dB = Double.parseDouble(reportParameters.get("edfa__noiseFactorMaximumGain_dB"));
        final double edfa__noiseFactorMinimumGain_dB = Double.parseDouble(reportParameters.get("edfa__noiseFactorMinimumGain_dB"));
        final double edfa__noiseFactorReferenceBandwidth_nm = Double.parseDouble(reportParameters.get("edfa__noiseFactorReferenceBandwidth_nm"));

        /* Dispersion compensation modules specifications */
        final double dcm__worseCaseChannelDispersion_ps_per_nm = Double.parseDouble(reportParameters.get("dcm__worseCaseChannelDispersion_ps_per_nm"));
        final double dcm__PMD_ps = Double.parseDouble(reportParameters.get("dcm__PMD_ps"));
        final double dcm__insertionLoss_dB = Double.parseDouble(reportParameters.get("dcm__insertionLoss_dB"));

        /* Mux/demux modules specifications */
        final double mux__insertionLoss_dB = Double.parseDouble(reportParameters.get("mux__insertionLoss_dB"));
        final double mux__PMD_ps = Double.parseDouble(reportParameters.get("mux__PMD_ps"));
        final double mux__maxInputPower_dBm = Double.parseDouble(reportParameters.get("mux__maxInputPower_dBm"));

        /* OSNR penalties */
        final double osnrPenalty__CD_dB = Double.parseDouble(reportParameters.get("osnrPenalty__CD_dB"));
        final double osnrPenalty__nonLinear_dB = Double.parseDouble(reportParameters.get("osnrPenalty__nonLinear_dB"));
        final double osnrPenalty__PMD_dB = Double.parseDouble(reportParameters.get("osnrPenalty__PMD_dB"));
        final double osnrPenalty__PDL_dB = Double.parseDouble(reportParameters.get("osnrPenalty__PDL_dB"));
        final double osnrPenalty__transmitterChirp_dB = Double.parseDouble(reportParameters.get("osnrPenalty__transmitterChirp_dB"));
        final double osnrPenalty__muxDemuxCrosstalk_dB = Double.parseDouble(reportParameters.get("osnrPenalty__muxDemuxCrosstalk_dB"));
        final double osnrPenalty__unassignedMargin_dB = Double.parseDouble(reportParameters.get("osnrPenalty__unassignedMargin_dB"));
        final double osnrPenalty__SUM_dB = osnrPenalty__CD_dB + osnrPenalty__nonLinear_dB + osnrPenalty__PMD_dB + osnrPenalty__PDL_dB + osnrPenalty__transmitterChirp_dB + osnrPenalty__muxDemuxCrosstalk_dB + osnrPenalty__unassignedMargin_dB;  

        final int E = netPlan.getNumberOfLinks();

        ArrayList<ArrayList<Triple<Double,String,double []>>> signalInfo = new ArrayList<ArrayList<Triple<Double,String,double []>>> (); 
        ArrayList<ArrayList<Triple<Double,String,String>>> warnings = new ArrayList<ArrayList<Triple<Double,String,String>>> (); 

        final double[] d_e = netPlan.getLinkLengthInKmVector();
        for (int e = 0 ; e < E ; e ++)
        {
            ArrayList<Triple<Double,String,double []>> signalInfoThisLink = new ArrayList<Triple<Double,String,double []>> ();
            ArrayList<Triple<Double,String,String>> warningsThisLink = new ArrayList <Triple<Double,String,String>> ();

            final String st_edfaPositions_km = (netPlan.getLinkAttribute(e, "edfaPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "edfaPositions_km");
            final String st_edfaGains_dB = (netPlan.getLinkAttribute(e, "edfaGains_dB") == null)? "" : netPlan.getLinkAttribute(e, "edfaGains_dB");
            final String st_dcmPositions_km = (netPlan.getLinkAttribute(e, "dcmPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "dcmPositions_km");

            final double [] edfaPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_edfaPositions_km));
            final double [] edfaGains_dB = StringUtils.toDoubleArray(StringUtils.split(st_edfaGains_dB));
            final double [] dcmPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_dcmPositions_km));

            /* Put in order the elements in the WDM line */
            ArrayList<Triple<Double,String,Double>> elementPositions = getElementPositionsArray (e , d_e [e] , edfaPositions_km , edfaGains_dB , dcmPositions_km); 
            int numDCMs = 0;
            int numEDFAs = 0;

            /* In the transmitter */
            final double numChannels_dB = linear2dB(channels__maxNumChannels);
            double current_powerPerChannel_dBm = tp__outputPower_dBm;
            double current_CD_ps_per_nm = 0;
            double current_PMDSquared_ps2 = 0;
            double current_OSNR_linear = Double.MAX_VALUE; //Double.POSITIVE_INFINITY; Double.MAX_VALUE; // no noise
            double current_kmFromTransmitter = 0;

            /* Check the range of wavelengths of the transponder */
            if (channels__minChannelLambda_nm < tp__minWavelength_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Transmitter", "Wavelength " + channels__minChannelLambda_nm + " nm is outside the transponder range [" + tp__minWavelength_nm  + " nm , " + tp__maxWavelength_nm + " nm]"));
            if (channels__maxChannelLambda_nm > tp__maxWavelength_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Transmitter", "Wavelength " + channels__maxChannelLambda_nm + " nm is outside the transponder range [" + tp__minWavelength_nm  + " nm , " + tp__maxWavelength_nm + " nm]"));

            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "After Transmitter" , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			

            /* At the output of the multiplexer */
            /* Check if the input power at the multiplexer does not exceed the limit */
            if (tp__outputPower_dBm + numChannels_dB > mux__maxInputPower_dBm + precisionMargenForChecks_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Mux", "Input power if all the WDM channels are active (" + (tp__outputPower_dBm + numChannels_dB) + " dBm) exceeds the maximum input power to the multiplexer (" + mux__maxInputPower_dBm + " dBm"));
            current_powerPerChannel_dBm -= mux__insertionLoss_dB;
            current_PMDSquared_ps2 += Math.pow(mux__PMD_ps,2);    

            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "After MUX" , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			

            /* Iterate along consecutive spans (ended by OA, DCM or DEMUX at the receiver) */
            for (Triple<Double,String,Double> span : elementPositions)
            {
                    final double elementPosition_km = span.getFirst();
                    final String elementType = span.getSecond();
                    final double gainIfOA_dB = span.getThird();
                    if (elementPosition_km < current_kmFromTransmitter) throw new RuntimeException ("Unexpected error");

                    /* The span of fiber */
                    final double fiberLength_km = elementPosition_km - current_kmFromTransmitter;
                    current_powerPerChannel_dBm -= fiber__attenuation_dB_per_km * fiberLength_km;
                    current_CD_ps_per_nm += fiber__worseChromaticDispersion_ps_per_nm_per_km * fiberLength_km;
                    current_PMDSquared_ps2 += fiberLength_km * Math.pow(fiber__PMD_ps_per_sqroot_km, 2);
                    current_kmFromTransmitter = elementPosition_km;

                    /* Check chromatic dispersion at the end of the fiber span */
                    if (!report__checkCDOnlyAtTheReceiver)
                    {
                            if (current_CD_ps_per_nm > tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "End fiber span", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is above the upper limit (" + tp__maxChromaticDispersionTolerance_ps_per_nm + " ps/nm)"));
                            if (current_CD_ps_per_nm < -tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "End fiber span", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is below the lower limit (" + (-tp__maxChromaticDispersionTolerance_ps_per_nm) + " ps/nm)"));
                    }

                    /* The element */
                    if (elementType.equals ("DCM"))
                    {
                            current_powerPerChannel_dBm -= dcm__insertionLoss_dB;
                            current_CD_ps_per_nm += dcm__worseCaseChannelDispersion_ps_per_nm;
                            current_PMDSquared_ps2 += Math.pow(dcm__PMD_ps,2); 
                            /* Check chromatic dispersion at the end of the fiber */
                            if (!report__checkCDOnlyAtTheReceiver)
                            {
                                    if (current_CD_ps_per_nm > tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Output of DCM", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is above the upper limit (" + tp__maxChromaticDispersionTolerance_ps_per_nm + " ps/nm)"));
                                    if (current_CD_ps_per_nm < -tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Output of DCM", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is below the lower limit (" + (-tp__maxChromaticDispersionTolerance_ps_per_nm) + " ps/nm)"));
                            }
                            numDCMs ++;
                            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "After DCM #" + numDCMs , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			
                    }
                    else if (elementType.equals ("OA"))
                    {
                            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "Before EDFA #" + (numEDFAs + 1) , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			

                            if (channels__minChannelLambda_nm < edfa__minWavelength_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Wavelength " + channels__minChannelLambda_nm + " is outside EDFA range [" + edfa__minWavelength_nm + " nm , " + edfa__maxWavelength_nm + " nm]"));
                            if (channels__maxChannelLambda_nm > edfa__maxWavelength_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Wavelength " + channels__minChannelLambda_nm + " is outside EDFA range [" + edfa__minWavelength_nm + " nm , " + edfa__maxWavelength_nm + " nm]"));
                            if (current_powerPerChannel_dBm + precisionMargenForChecks_dB < edfa__minInputPower_dBm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Input power at the amplifier if only one WDM channel is active (" + current_powerPerChannel_dBm + " dBm) is outside the EDFA input power range [" + edfa__minInputPower_dBm + " dBm , " + edfa__maxInputPower_dBm + " dBm]"));
                            if (current_powerPerChannel_dBm + numChannels_dB > edfa__maxInputPower_dBm + precisionMargenForChecks_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Input power at the amplifier if all WDM channels are active (" + (current_powerPerChannel_dBm + numChannels_dB) + " dBm) is outside the EDFA input power range [" + edfa__minInputPower_dBm + " dBm , " + edfa__maxInputPower_dBm + " dBm]")); 
                            if ( (gainIfOA_dB < edfa__minGain_dB - precisionMargenForChecks_dB) || (gainIfOA_dB > edfa__maxGain_dB + precisionMargenForChecks_dB)) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "EDFA gain (" + gainIfOA_dB  + " dB) is outside the EDFA gain range [" + edfa__minGain_dB + " dB , " + edfa__maxGain_dB + " dB]")); 

                            /* update the OSNR */
                            final double edfa_noiseFactorThisGain_dB = edfa__noiseFactorMinimumGain_dB + (gainIfOA_dB - edfa__minGain_dB) * (edfa__noiseFactorMaximumGain_dB - edfa__noiseFactorMinimumGain_dB) / (edfa__maxGain_dB - edfa__minGain_dB) ;
                            if ((edfa_noiseFactorThisGain_dB < Math.min(edfa__noiseFactorMinimumGain_dB, edfa__noiseFactorMaximumGain_dB)) || (edfa_noiseFactorThisGain_dB > Math.max(edfa__noiseFactorMinimumGain_dB, edfa__noiseFactorMaximumGain_dB))) throw new RuntimeException ("Bad");
                            final double edfa_NF_linear = dB2Linear(edfa_noiseFactorThisGain_dB); 
                            final double highestFrequencyChannel_Hz = this.constant_c / (channels__minChannelLambda_nm * 1e-9);
                            final double referenceBandwidthAtHighestFrequency_Hz = -highestFrequencyChannel_Hz + this.constant_c / ((channels__minChannelLambda_nm - edfa__noiseFactorReferenceBandwidth_nm) * 1e-9);
                            final double inputPower_linear = dB2Linear(current_powerPerChannel_dBm) * 1E-3;
                            final double thisEDFAAddedNoise_linear = edfa_NF_linear * constant_h * highestFrequencyChannel_Hz * referenceBandwidthAtHighestFrequency_Hz;
                            final double addedOSNRThisOA_linear = inputPower_linear / thisEDFAAddedNoise_linear;
                            current_OSNR_linear = (current_OSNR_linear == Double.MAX_VALUE)? addedOSNRThisOA_linear : 1 / (1/current_OSNR_linear + 1/addedOSNRThisOA_linear);

                            /* update the signal power */
                            current_powerPerChannel_dBm += gainIfOA_dB;
                            if (current_powerPerChannel_dBm < edfa__minOutputPower_dBm - precisionMargenForChecks_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Output power at the amplifier if only one WDM channel is active (" + current_powerPerChannel_dBm + " dBm) is outside the EDFA output power range [" + edfa__minOutputPower_dBm + " dBm , " + edfa__maxOutputPower_dBm + " dBm]")); 
                            if (current_powerPerChannel_dBm + numChannels_dB > edfa__maxOutputPower_dBm + precisionMargenForChecks_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "EDFA", "Output power at the amplifier if all WDM channels are active (" + (current_powerPerChannel_dBm + numChannels_dB) + " dBm) is outside the EDFA output power range [" + edfa__minOutputPower_dBm + " dBm , " + edfa__maxOutputPower_dBm + " dBm]")); 

                            /* update the PMD */
                            current_PMDSquared_ps2 += Math.pow(edfa__PMD_ps,2); 
                            numEDFAs ++;
                            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "After EDFA #" + numEDFAs , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			

                    }
                    else 
                            throw new RuntimeException ("Unexpected error");
            }

            /* Fiber span after the last element, to the receiver */
            final double spanLength_km = d_e [e] - current_kmFromTransmitter;
            current_powerPerChannel_dBm -= fiber__attenuation_dB_per_km * spanLength_km;
            current_CD_ps_per_nm += fiber__worseChromaticDispersion_ps_per_nm_per_km * spanLength_km;
            current_PMDSquared_ps2 +=  spanLength_km * Math.pow(fiber__PMD_ps_per_sqroot_km,2);
            current_kmFromTransmitter = d_e [e];

            /* Check chromatic dispersion at the end of the fiber (input of the demux) */
            if (!report__checkCDOnlyAtTheReceiver)
            {
                    if (current_CD_ps_per_nm > tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Input of demux", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is above the upper limit (" + tp__maxChromaticDispersionTolerance_ps_per_nm + " ps/nm)"));
                    if (current_CD_ps_per_nm < -tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Input of demux", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is below the lower limit (" + (-tp__maxChromaticDispersionTolerance_ps_per_nm) + " ps/nm)"));
            }
            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "Before DEMUX" , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			

            /* The demultiplexer at the end of the line */
            /* Check if the input power at the multiplexer does not exceed the limit */
            if (current_powerPerChannel_dBm + numChannels_dB > mux__maxInputPower_dBm + precisionMargenForChecks_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "DEMUX", "Input power at the demux if all WDM channels are active (" + (current_powerPerChannel_dBm + numChannels_dB) + " dBm) is ebove the DEMUX maximum input power (" + mux__maxInputPower_dBm + " dBm)")); 
            current_powerPerChannel_dBm -= mux__insertionLoss_dB;
            current_PMDSquared_ps2 += Math.pow(mux__PMD_ps,2);

            /* In the receiver */
            /* CHECK 1: CHROMATIC DISPERSION WITHIN THE LIMITS */
            if (current_CD_ps_per_nm > tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Receiver", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is above the upper limit (" + tp__maxChromaticDispersionTolerance_ps_per_nm + " ps/nm)"));
            if (current_CD_ps_per_nm < -tp__maxChromaticDispersionTolerance_ps_per_nm) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Receiver", "Chromatic dispersion at this point (" + current_CD_ps_per_nm + " ps/nm) is below the lower limit (" + (-tp__maxChromaticDispersionTolerance_ps_per_nm) + " ps/nm)"));
            /* CHECK 2: OSNR WITHIN THE LIMITS */
            if (linear2dB(current_OSNR_linear) < tp__minOSNR_dB + osnrPenalty__SUM_dB) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Receiver", "OSNR at the receiver (" + linear2dB(current_OSNR_linear) + " dB) is below the minimum OSNR tolerance plus margin (" + tp__minOSNR_dB + " dB + " + osnrPenalty__SUM_dB + " dB = " + (tp__minOSNR_dB + osnrPenalty__SUM_dB)  + " dB)")); 
            /* CHECK 3: POWER BUDGET WITHIN LIMITS */
            if ((current_powerPerChannel_dBm > tp__inputPowerSensitivityMax_dBm + precisionMargenForChecks_dB) || (current_powerPerChannel_dBm < tp__inputPowerSensitivityMin_dBm - precisionMargenForChecks_dB)) 
                    warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Receiver", "Input power (" + current_powerPerChannel_dBm + " dBm) is outside the sensitivity range [" + tp__inputPowerSensitivityMin_dBm + " dBm , " + tp__inputPowerSensitivityMax_dBm + " dBm]"));
            /* CHECK 3: PMD TOLERANCE OF RECEIVER */
            // current_PMDSquared_ps2 = Math.sqrt(numEDFAs * Math.pow(edfa__PMD_ps,2) + numDCMs * Math.pow(dcm__PMD_ps,2) + 2 * Math.pow(mux__PMD_ps,2) + Math.sqrt(d_e [e]) * fiber__PMD_ps_per_sqroot_km);
            // final double pmdCalculatedAtTheEnd_ps = Math.sqrt(numEDFAs * Math.pow(edfa__PMD_ps,2) + numDCMs * Math.pow(dcm__PMD_ps,2) + 2 * Math.pow(mux__PMD_ps,2) + d_e [e] * Math.pow(fiber__PMD_ps_per_sqroot_km,2));
            // if (Math.abs(pmdCalculatedAtTheEnd_ps - Math.sqrt(current_PMDSquared_ps2)) > 0.001) System.out.println("ERROR calculating PMD: pmdCalculatedAtTheEnd_ps = " + pmdCalculatedAtTheEnd_ps + ",current_PMDSquared_ps2=" + current_PMDSquared_ps2 ); 

            if (Math.sqrt(current_PMDSquared_ps2) > tp__pmdTolerance_ps) warningsThisLink.add(Triple.of(current_kmFromTransmitter, "Receiver", "PMD at the receiver (" + Math.sqrt(current_PMDSquared_ps2) + " ps) is above the maximum PMD tolerance (" + tp__pmdTolerance_ps + " ps)"));

            signalInfoThisLink.add(new Triple<Double,String,double []> (current_kmFromTransmitter , "Receiver" , new double [] { current_CD_ps_per_nm , linear2dB(current_OSNR_linear) , current_powerPerChannel_dBm , Math.sqrt(current_PMDSquared_ps2)} ));			
            signalInfo.add(signalInfoThisLink);
            warnings.add(warningsThisLink);
        }

        return printReport(netPlan, reportParameters, signalInfo, warnings);
    }

    @Override
    public String getDescription() { return "This report shows line engineering information for WDM links in the network"; }

    @Override
    public String getTitle() { return "WDM line engineering"; }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> aux = new ArrayList<Triple<String, String, String>>();

        aux.add(Triple.of("report__checkCDOnlyAtTheReceiver", "true", "If true, the chromatic dispersion only is checked against the CD tolerance at the receiver. If false, it is checked against the same value, in all the fiber spans"));

        /* Usable wavelengths */
        aux.add(Triple.of("channels__minChannelLambda_nm", "1530.33", "Channel minimum wavelength in nm"));
        aux.add(Triple.of("channels__channelSpacing_GHz", "100", "Channel spacing in GHz"));
        aux.add(Triple.of("channels__maxNumChannels", "16", "Maximum number of WDM channels that will be used"));

        /* Fiber specifications  */
        aux.add(Triple.of("fiber__ituType", "G.655", "ITU-T fiber type"));
        aux.add(Triple.of("fiber__attenuation_dB_per_km", "0.25", "Fiber attenuation in dB/km"));
        aux.add(Triple.of("fiber__worseChromaticDispersion_ps_per_nm_per_km", "6", "Chromatic dispersion of the fiber in ps/nm/km"));
        aux.add(Triple.of("fiber__PMD_ps_per_sqroot_km", "0.5", "Polarization mode dispersion per km^0.5 of fiber (PMD_Q link factor)"));

        /* Transponder specifications */
        aux.add(Triple.of("tp__outputPower_dBm", "6", "Output power of the transmitter in dBm"));
        aux.add(Triple.of("tp__maxChromaticDispersionTolerance_ps_per_nm", "800", "Maximum chromatic dispersion tolerance in ps/nm at the receiver"));
        aux.add(Triple.of("tp__minOSNR_dB", "10", "Minimum OSNR needed at the receiver"));
        aux.add(Triple.of("tp__inputPowerSensitivityMin_dBm", "-18", "Minimum input power at the receiver in dBm"));
        aux.add(Triple.of("tp__inputPowerSensitivityMax_dBm", "-8", "Maximum input power at the receiver in dBm"));
        aux.add(Triple.of("tp__minWavelength_nm", "1529.55", "Minimum wavelength usable by the transponder"));
        aux.add(Triple.of("tp__maxWavelength_nm", "1561.84", "Maximum wavelength usable by the transponder"));
        aux.add(Triple.of("tp__pmdTolerance_ps", "10", "Maximum tolarance of polarizarion mode dispersion (mean of differential group delay) in ps at the receiver"));

        /* Optical amplifier specifications */
        aux.add(Triple.of("edfa__minWavelength_nm", "1530", "Minimum wavelength usable by the EDFA"));
        aux.add(Triple.of("edfa__maxWavelength_nm", "1563", "Maximum wavelength usable by the EDFA"));
        aux.add(Triple.of("edfa__minInputPower_dBm", "-29", "Minimum input power at the EDFA"));
        aux.add(Triple.of("edfa__maxInputPower_dBm", "2", "Maximum input power at the EDFA"));
        aux.add(Triple.of("edfa__minOutputPower_dBm", "-6", "Minimum output power at the EDFA"));
        aux.add(Triple.of("edfa__maxOutputPower_dBm", "19", "Maximum output power at the EDFA"));
        aux.add(Triple.of("edfa__minGain_dB", "17", "Minimum gain at the EDFA"));
        aux.add(Triple.of("edfa__maxGain_dB", "23", "Maximum gain at the EDFA"));
        aux.add(Triple.of("edfa__PMD_ps", "0.5", "Polarization mode dispersion in ps added by the EDFA"));
        aux.add(Triple.of("edfa__noiseFactorMaximumGain_dB", "5.4", "Noise factor at the EDFA when the gain is in its upper limit (linear interpolation is used to estimate the noise figura at other gains)"));
        aux.add(Triple.of("edfa__noiseFactorMinimumGain_dB", "6.4", "Noise factor at the EDFA when the gain is in its lower limit (linear interpolation is used to estimate the noise figura at other gains)"));
        aux.add(Triple.of("edfa__noiseFactorReferenceBandwidth_nm", "0.5", "Reference bandwidth that measures the noise factor at the EDFA"));

        /* Dispersion compensation modules specifications */
        aux.add(Triple.of("dcm__worseCaseChannelDispersion_ps_per_nm", "-551", "Dispersion compensation (ps/nm) in the WDM channel with lower dispersion in absolute number"));
        aux.add(Triple.of("dcm__PMD_ps", "0.7", "Polarization mode dispersion in ps added by the DCM"));
        aux.add(Triple.of("dcm__insertionLoss_dB", "3.5", "Maximum insertion loss added by the DCM"));

        /* Mux/demux modules specifications */
        aux.add(Triple.of("mux__insertionLoss_dB", "5.1", "Maximum insertion loss in dB added by the mux/demux"));
        aux.add(Triple.of("mux__PMD_ps", "0.5", "Polarization mode dispersion in ps added by the mux/demux"));
        aux.add(Triple.of("mux__maxInputPower_dBm", "24", "Maximum input power in dBm at the mux/demux"));

        /* OSNR penalties */
        aux.add(Triple.of("osnrPenalty__CD_dB", "1", "OSNR penalty caused by residual chromatic dispersion (assumed within limits)"));
        aux.add(Triple.of("osnrPenalty__nonLinear_dB", "2", "OSNR penalty caused by the non-linear effects SPM, XPM, FWM and Brillouin / Raman scattering"));
        aux.add(Triple.of("osnrPenalty__PMD_dB", "0.5", "OSNR penalty caused by the polarization mode dispersion (assumed within limits)"));
        aux.add(Triple.of("osnrPenalty__PDL_dB", "0.3", "OSNR penalty caused by polarization dispersion losses"));
        aux.add(Triple.of("osnrPenalty__transmitterChirp_dB", "0.5", "OSNR penalty caused by transmitter chirp "));
        aux.add(Triple.of("osnrPenalty__muxDemuxCrosstalk_dB", "0.2", "OSNR penalty caused by the crosstalk at the mux and the demux"));
        aux.add(Triple.of("osnrPenalty__unassignedMargin_dB", "3", "OSNR penalty caused by not assigned margins (e.g. random effects, aging, ...)"));

        return aux;
    }

    private ArrayList<Triple<Double,String,Double>> getElementPositionsArray (int e , double linkLength , double [] edfaPositions_km , double [] edfaGains_dB , double [] dcmPositions_km) throws Net2PlanException
    {
        if (edfaPositions_km.length != edfaGains_dB.length) throw new Net2PlanException ("Link: " + e + ". Number of elements in edfaPositions_km is not equal to the number of elements in edfaGains_dB.");
        for (double edfaPosition : edfaPositions_km) if ((edfaPosition < 0) || (edfaPosition > linkLength)) throw new Net2PlanException ("Link: " + e + ". Wrong OA position: " + edfaPosition + ", link length = " + linkLength);
        for (double dcmPosition : dcmPositions_km) if ((dcmPosition < 0) || (dcmPosition > linkLength)) throw new Net2PlanException ("Link: " + e + ". Wrong DCM position: " + ", link length = " + linkLength);

        ArrayList<Triple<Double,String,Double>> res = new ArrayList<Triple<Double,String,Double>> ();

        int [] sortedOAPositionsIndexes = (edfaPositions_km.length == 0)? new int [0]: DoubleUtils.sortIndexes(edfaPositions_km, OrderingType.ASCENDING);
        int [] sortedDCMPositionsIndexes = (dcmPositions_km.length == 0)? new int [0] : DoubleUtils.sortIndexes(dcmPositions_km, OrderingType.ASCENDING);

        int numOAInserted = 0;
        int numDCMInserted = 0;
        while (true)
        {
            double positionNextOA = (numOAInserted < edfaPositions_km.length)? edfaPositions_km [sortedOAPositionsIndexes [numOAInserted]] : -1;
            double positionNextDCM = (numDCMInserted < dcmPositions_km.length)? dcmPositions_km [sortedDCMPositionsIndexes [numDCMInserted]] : -1;

            /* IS no more elements, it is done */
            if ((positionNextOA == -1) && (positionNextDCM == -1)) break;

            /* If at the same position, we assume DCM goes first, to reduce non-linear impairments in the DCM */
            boolean nextElementIsOA = (positionNextDCM == -1) || ((positionNextOA != -1) && (positionNextOA < positionNextDCM) );
            if (nextElementIsOA)
                    { res.add(Triple.of(positionNextOA, "OA", edfaGains_dB [sortedOAPositionsIndexes [numOAInserted]])); numOAInserted ++; }
            else
                    { res.add(Triple.of(positionNextDCM, "DCM", -1.0)); numDCMInserted ++; }
        }

        return res;
    }

    private static double dB2Linear(double dB) { return Math.pow(10, dB/10); }
    private static double linear2dB(double num) { return 10 * Math.log10(num); }
    
    private String printReport(NetPlan netPlan , Map<String, String> reportParameters , ArrayList<ArrayList<Triple<Double,String,double []>>> signalInfo , ArrayList<ArrayList<Triple<Double,String,String>>> warnings)
    {
        final int E = netPlan.getNumberOfLinks();
        final double[] d_e = netPlan.getLinkLengthInKmVector();
        StringBuilder out = new StringBuilder ();
        DecimalFormat df_2 = new DecimalFormat ("###.##");

        out.append ("<html>");
        out.append ("<head><title>Point-to-point WDM line engineering report</title></head>");
        out.append ("<html><body>");
        out.append ("<h1>Point-to-point WDM line engineering report</h1>");
        out.append ("<p>This report checks the correctness of the line engineering design of the point-to-point WDM links in an optical network, following the guidelines described in ITU-T Manual 2009 \"Optical fibres, cables and systems\" (chapter 7, section 2 <em>Worst case design for system with optical line amplifiers</em>). The report assumes that the network links are WDM optical fibres with the following scheme:</p>");
        out.append ("<ul>");
        out.append ("<li>A transmitter per WDM channel with the specifications given by \"tp__XXX\". The number of channels can vary from one to up to channels_maxNumChannels and, the design should be correct in all the cases. Transmitter specifications are set in \"tp__XXX\" input parameters</li>");
        out.append ("<li>A multiplexer that receives the input power from the transmitters and  with specifications given by \"mux__XXX\" parameters</li>");
        out.append ("<li>A fiber link of a distance given by the link length, and with specifications given by \"fiber__XXX\" parameters. The fiber can be split into spans if optical amplifers (EDFAs) and/or dispersion compensating modules (DCMs) are placed along the fibre.</li>");
        out.append ("<li>A set of optical amplifiers (EDFAs) located in none, one or more positions in the fiber link, separating them in different spans. EDFAs are supposed to operate in the automatic gain control mode. Thus, the gain is the same, whatever the number of input WDM channels. " +
                        "EDFA positions (as distance in km from the link start to the EDFA location) and EDFA gains (assumed in dB) are read from the \"edfaPositions_km\" and \"edfaGains_dB\" attributes of the links. The format of both attributes are the same: a string of numbers separated by spaces. " +
                        "The i-th number corresponding to the position/gain of the i-th EDFA. If the attributes do not exist, it is assumed that no EDFAs are placed in this link. EDFA specifications given by \"edfa__XXX\" parameters</li>");
        out.append ("<li>A set of dispersion compensating modules (DCMs) located in none, one or more positions in the fiber link, separating them in different spans. If a DCM and a EDFA have the same location, it is assumed that the DCM is placed first, to reduce the non-linear effects. " +
                        "DCM positions (as distance in km from the link start to the DCM location) are read from the \"dcmPositions_km\" attribute of the link, and the same format as with \"edfaPositions_km\" attribute is expected. " +
                        "If the attribute does not exist, it is assumed that no DCMs are placed in this link. DCM specifications are given by \"dcm__XXX\" parameters</li>");
        out.append ("<li>At the receiver end, WDM channels in the links are separated using a demultiplexer, with specifications given by \"mux__XXX\" parameters</li>");
        out.append ("<li>Each channel ends in a receiver, with specifications given by \"tp__XXX\" parameters</li>");
        out.append ("</ul>");
        out.append ("<p>The basic checks performed are:</p>");
        out.append ("<ul>");
        out.append ("<li>Signal power levels are within operating ranges at the mux/demux/edfas/dcms and receivers, both when the link has one single active channel, or when all the \"channels__maxNumChannels\" are active</li>");
        out.append ("<li>Chromatic dispersion is within the operating ranges in every point of the fiber, and at the receiver.</li>");
        out.append ("<li>Optical Signal to Noise Ration (OSNR) is within the operating range at the receiver.</li>");
        out.append ("<li>Polarization mode dispersion (PMD) is within the operating range at the receiver.</li>");
        out.append ("</ul>");

        out.append ("<h2>Input Parameters</h2>");
        out.append ("<table border='1'>");
        out.append ("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");

        for (Triple<String, String, String> paramDef : this.getParameters())
        {
                String name = paramDef.getFirst();
                String description = paramDef.getThird();
                String value = reportParameters.get(name);
                out.append("<tr><td>" + name + "</td><td>" + value + "</td><td>" + description + "</td></tr>");
        }
        out.append("</table>");

        out.append("<h2>INFORMATION SUMMARY - Signal metrics at the receiver end</h2>");
        out.append ("<table border='1'>");
        out.append ("<tr><th><b>Link #</b></th><th><b>Length (km)</b></th><th><b># EDFAs</b></th><th><b># DCMs</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th></tr>");
        for (int e = 0 ; e < E ; e ++)
        {
                final String st_edfaPositions_km = (netPlan.getLinkAttribute(e, "edfaPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "edfaPositions_km");
                final String st_dcmPositions_km = (netPlan.getLinkAttribute(e, "dcmPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "dcmPositions_km");
                final double [] edfaPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_edfaPositions_km));
                final double [] dcmPositions_km = StringUtils.toDoubleArray(StringUtils.split(st_dcmPositions_km));

                Triple<Double,String,double []> t = signalInfo.get (e).get(signalInfo.get (e).size() - 1); 
                out.append("<tr><td>" + e + "</td><td>" + df_2.format(d_e [e]) + "</td><td>" + edfaPositions_km.length + "</td><td>" + dcmPositions_km.length + "</td><td>" + df_2.format(t.getThird() [0]) + "</td><td>" + df_2.format(t.getThird() [1]) + "</td><td>" + df_2.format(t.getThird() [2]) + "</td><td>" + ((t.getThird() [3] == -1)? "-" : df_2.format(t.getThird() [3])) + "</td>" + "</tr>");
        }

        out.append("</table>");

        out.append("<h2>DESIGN WARNINGS</h2>");
        out.append ("<table border='1'>");
        out.append ("<tr><th><b>Link #</b></th><th><b>Length (km)</b></th><th><b>Warnings</b></th></tr>");
        for (int e = 0 ; e < E ; e ++)
        {
                out.append("<tr><td>" + e + "</td><td>" + d_e [e] + "</td><td>");
                ArrayList<Triple<Double,String,String>> warningsThisLink = warnings.get (e);
                if (warningsThisLink.isEmpty()) { out.append("<p>None</p>"); continue; }
                for (Triple<Double,String,String> w : warningsThisLink)
                {
                        out.append("<p>[" + w.getSecond() + " (km " + df_2.format(w.getFirst()) + ") - " + w.getThird() + "</p>");
                }
                out.append("</td></tr>");
        }
        out.append("</table>");

        out.append("<h2>PER-LINK DETAILED INFORMATION </h2>");
        out.append("<p>Number of links: " + E + "</p>");

        for (int e = 0 ; e < E ; e ++)
        {
                ArrayList<Triple<Double,String,double []>> signalInfoThisLinks = signalInfo.get (e); 

                final String st_edfaPositions_km = (netPlan.getLinkAttribute(e, "edfaPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "edfaPositions_km");
                final String st_edfaGains_dB = (netPlan.getLinkAttribute(e, "edfaGains_dB") == null)? "" : netPlan.getLinkAttribute(e, "edfaGains_dB");
                final String st_dcmPositions_km = (netPlan.getLinkAttribute(e, "dcmPositions_km") == null)? "" : netPlan.getLinkAttribute(e, "dcmPositions_km");

                out.append("<h3>LINK # " + e + "</h3>");
                out.append("<table border=\"1\">");
                out.append("<caption>Link input information</caption>");
                out.append("<tr><td>Link length (km)</td><td>" + d_e [e]+  "</td></tr>");
                out.append("<tr><td>EDFA positions (km)</td><td>" + st_edfaPositions_km +  "</td></tr>");
                out.append("<tr><td>EDFA gains (dB)</td><td>" + st_edfaGains_dB +  "</td></tr>");
                out.append("<tr><td>DCM positions (km)</td><td>" + st_dcmPositions_km +  "</td></tr>");
                out.append("</table>");

                out.append("<table border=\"1\">");
                out.append("<caption>Signal metrics evolution</caption>");
                out.append ("<tr><th><b>Position (km)</b></th><th><b>Position (description)</b></th><th><b>Chromatic Dispersion (ps/nm)</b></th><th><b>OSNR (dB)</b></th><th><b>Power per WDM channel (dBm)</b></th><th><b>Polarization Mode Dispersion (ps)</b></th></tr>");
                for (Triple<Double,String,double []> t : signalInfoThisLinks)
                        out.append("<tr><td>" + df_2.format(t.getFirst()) + "</td><td>" + t.getSecond() + "</td><td>" + df_2.format(t.getThird() [0]) + "</td><td>" + df_2.format(t.getThird() [1]) + "</td><td>" + df_2.format(t.getThird() [2]) + "</td><td>" + ((t.getThird() [3] == -1)? "-" : df_2.format(t.getThird() [3])) + "</td>" + "</tr>");
                out.append("</table>");
        }
        out.append("</body></html>");
        return out.toString();
    }
}