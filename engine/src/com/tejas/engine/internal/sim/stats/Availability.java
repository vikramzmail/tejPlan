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

package com.tejas.engine.internal.sim.stats;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;

import com.tejas.engine.internal.sim.stats.SimStats;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class to collect performance metrics from resilience simulation.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class Availability extends SimStats
{
    private final int D, E;
    private final double[] r_d_0;
    private final double r_D_0;

    double lastEventTime;

    double[] r_d_s;
    double r_D_s;

    double[] A_d_1;
    double[] A_d_2;
    double A_D_1;
    double A_D_2;
    
    double previousState_oversubscribed_d;
    double previousState_oversubscribed_e;
    double accum_oversubscribed_d;
    double accum_oversubscribed_e;

    double timeNoFailure;
    double totalTime;

    double timeAtTransitory;
    
    private final boolean assumeOverSubscribedLinksAsFailedLinks;
    private final double precisionFactor;
    
    private final double[] h_d;
    private final double H_d;
    private final double[] u_e;
    private final double[] spareCapacity_e;
    
    /**
     *
     * @param netPlan
     * @param netState
     */
    public Availability(NetPlan netPlan, ResilienceNetState netState, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
    {
	super(netPlan, netState, simulationParameters, net2planParameters);
        
        assumeOverSubscribedLinksAsFailedLinks = Boolean.parseBoolean(simulationParameters.get("assumeOverSubscribedLinksAsFailedLinks"));
        precisionFactor = Double.parseDouble(net2planParameters.get("precisionFactor"));

        E = netPlan.getNumberOfLinks();
	D = netPlan.getNumberOfDemands();
        
        u_e = netPlan.getLinkCapacityInErlangsVector();
        spareCapacity_e = netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector();
        h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        H_d = DoubleUtils.sum(h_d);

        r_d_0 = netPlan.getDemandCarriedTrafficInErlangsVector();
	r_D_0 = DoubleUtils.sum(r_d_0);

	r_d_s = new double[D];
	r_D_s = 0;

	computeCarriedTrafficInThisState();

	A_d_1 = new double[D];
	A_d_2 = new double[D];

	Arrays.fill(A_d_1, 0);
	Arrays.fill(A_d_2, 0);
	A_D_1 = 0;
	A_D_2 = 0;

	lastEventTime = 0;

	timeNoFailure = 0;
	totalTime = 0;
	timeAtTransitory = 0;
        
        previousState_oversubscribed_d = 0;
        previousState_oversubscribed_e = 0;
        accum_oversubscribed_d = 0;
        accum_oversubscribed_e = 0;
    }

    private void computeAvailabilityInPreviousState(double timeInThisState)
    {
	totalTime += timeInThisState;

	for(int demandId = 0; demandId < D; demandId++)
	{
	    A_d_1[demandId] += (r_d_s[demandId] - r_d_0[demandId]) >= -precisionFactor ? timeInThisState : 0;
	    A_d_2[demandId] += Math.min(1, r_d_s[demandId] / r_d_0[demandId]) * timeInThisState;
	}

	A_D_1 += (r_D_s - r_D_0) >= -precisionFactor ? timeInThisState : 0;
	A_D_2 += Math.min(1, r_D_s / r_D_0) * timeInThisState;

	if ((r_D_s - r_D_0) >= -precisionFactor) timeNoFailure += timeInThisState;
    }

    private void computeCarriedTrafficInThisState()
    {
        if (assumeOverSubscribedLinksAsFailedLinks)
        {
            int[] oversubscribedLinks = ((ResilienceNetState) netState).getLinksOversubscribed();
            if (oversubscribedLinks.length == 0)
            {
                r_d_s = ((ResilienceNetState) netState).getDemandCurrentCarriedTrafficInErlangsVector();
            }
            else
            {
                Arrays.fill(r_d_s, 0);

                long[] routeIds = ((ResilienceNetState) netState).getRouteIds();

                for(long routeId : routeIds)
                {
                    int demandId = ((ResilienceNetState) netState).getRouteDemand(routeId);

                    double trafficVolume = ((ResilienceNetState) netState).getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
                    if (trafficVolume == 0) continue;

                    long[] seqLinksAndSegments = ((ResilienceNetState) netState).getRouteCurrentSequenceOfLinksAndSegments(routeId);

                    boolean assumeFailed = false;

                    for(long itemId : seqLinksAndSegments)
                    {
                        if (itemId >= 0)
                        {
                            int linkId = (int) itemId;
                            if (IntUtils.contains(oversubscribedLinks, linkId))
                            {
                                assumeFailed = true;
                                break;
                            }
                        }
                        else
                        {
                            long segmentId = -1 - itemId;
                            if (((ResilienceNetState) netState).getProtectionSegmentReservedBandwidthInErlangs(segmentId) == 0)
                            {
                                int[] seqLinks = ((ResilienceNetState) netState).getProtectionSegmentSequenceOfLinks(segmentId);
                                if (IntUtils.containsAny(oversubscribedLinks, seqLinks))
                                {
                                    assumeFailed = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!assumeFailed) r_d_s[demandId] += trafficVolume;
                }
            }
        }
        else
        {
            r_d_s = ((ResilienceNetState) netState).getDemandCurrentCarriedTrafficInErlangsVector();
        }
        
        double[] y_e = ((ResilienceNetState) netState).getLinkCurrentCarriedTrafficInErlangsVector();
        long[] segmentIds = ((ResilienceNetState) netState).getProtectionSegmentIds();
        for(long segmentId : segmentIds)
        {
            int[] seqLinks = ((ResilienceNetState) netState).getProtectionSegmentSequenceOfLinks(segmentId);
            double reservedBandwidthInErlangs = ((ResilienceNetState) netState).getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            if (reservedBandwidthInErlangs == 0)
            {
                double carriedTraffic = ((ResilienceNetState) netState).getProtectionSegmentCurrentCarriedTrafficInErlangs(segmentId);
                for(int linkId : seqLinks) y_e[linkId] += carriedTraffic;
            }
            else
            {
                for(int linkId : seqLinks) y_e[linkId] += reservedBandwidthInErlangs;
            }
        }
        
        IntArrayList linkIds = new IntArrayList();
        for(int linkId = 0; linkId < E; linkId++)
            if (y_e[linkId] > u_e[linkId] + precisionFactor)
                linkIds.add(linkId);
        
        double Y_e = DoubleUtils.sum(y_e);
        r_D_s = DoubleUtils.sum(r_d_s);
        
        for(int linkId = 0; linkId < E; linkId++)
        {
            previousState_oversubscribed_e += Y_e == 0 ? 0 : Math.max(0, y_e[linkId] - spareCapacity_e[linkId]) / Y_e;
        }
        
        for(int demandId = 0; demandId < D; demandId++)
        {
            previousState_oversubscribed_d += r_D_s == 0 ? 0 : Math.max(0, r_d_s[demandId] - h_d[demandId]) / r_D_s;
        }
    }

    @Override
    public void computeNextState(SimEvent event, List actions)
    {
        double currentSimTime = event.getEventTime();

	double previousStateDuration = currentSimTime - lastEventTime;
	computeAvailabilityInPreviousState(previousStateDuration);

	lastEventTime = currentSimTime;
	computeCarriedTrafficInThisState();
    }

    @Override
    public void reset(double currentSimTime)
    {
	timeAtTransitory = currentSimTime;

	totalTime = 0;
	timeNoFailure = 0;
	Arrays.fill(A_d_1, 0);
	Arrays.fill(A_d_2, 0);
	A_D_1 = 0;
	A_D_2 = 0;
        
        previousState_oversubscribed_d = 0;
        previousState_oversubscribed_e = 0;
        accum_oversubscribed_d = 0;
        accum_oversubscribed_e = 0;

	double previousStateDuration = currentSimTime - lastEventTime;
	computeCarriedTrafficInThisState();
	computeAvailabilityInPreviousState(previousStateDuration);
	lastEventTime = currentSimTime;
    }

    @Override
    public String getResults(double currentSimTime)
    {
	if (currentSimTime == 0) return "<p>No information available</p>";
        
        double tObservation = currentSimTime - timeAtTransitory;
	if (tObservation == 0) return "<p>No information available</p>";

        double tObservation_lastPeriod = currentSimTime - lastEventTime;
	StringBuilder html = new StringBuilder();

	html.append("<h2>Blocking</h2>");
        
        html.append("<p>In this report, some availability network-wide and per-demand information is shown. Definitions of availability metrics can be found in the availability report</p>");
            
        html.append("<p><b>Important</b>: All the metrics in this report are time-averaged, which means that the value of the metric is weighted by the period of time in that the network can be found in that state, divided by the total simulation time</p>");

	html.append("<ul>");
	html.append("<li>Total offered traffic (in Erlangs): ").append(DoubleUtils.sum(netPlan.getDemandOfferedTrafficInErlangsVector())).append("</li>");
	html.append("<li>Total planned carried traffic (in Erlangs): ").append(DoubleUtils.sum(netPlan.getRouteCarriedTrafficInErlangsVector())).append("</li>");
	html.append("<li>Total carried traffic in this state (in Erlangs): ").append(DoubleUtils.sum(((ResilienceNetState) netState).getDemandCurrentCarriedTrafficInErlangsVector())).append("</li>");

        double avg_oversubscribed_e = 100.0 * (accum_oversubscribed_e + tObservation_lastPeriod * previousState_oversubscribed_e) / tObservation;
        double avg_oversubscribed_d = 100.0 * (accum_oversubscribed_d + tObservation_lastPeriod * previousState_oversubscribed_d) / tObservation;
        html.append("<li>Avg. over-subscribed link capacity (%): ").append(avg_oversubscribed_e).append("</li>");
        html.append("<li>Avg. excess carried traffic (%): ").append(avg_oversubscribed_d).append("</li>");
        
	html.append("</ul>");

	html.append("<h2>Availability</h2>");

        Pair<double[][], double[]> availability = getCurrentAvailability(currentSimTime);

        double[][] A_d = availability.getFirst();
        double[] A_D = availability.getSecond();

        html.append("<h3>Per-demand information</h3>");

        html.append("<table border='1'>");
        html.append("<tr><th><b>Demand id</b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Offered traffic (Erlangs)</b></th><th><b>Availability (classic)</b></th><th><b>Availability (weighted)</b></th></tr>");
        for (int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);

            html.append(String.format("<tr><td>%d</td><td>n%d (%s)</td><td>n%d (%s)</td><td>%.3g</td><td>%.6f</td><td>%.6f</td></tr>", demandId, ingressNodeId, netPlan.getNodeName(ingressNodeId), egressNodeId, netPlan.getNodeName(egressNodeId), netPlan.getDemandOfferedTrafficInErlangs(demandId), A_d[0][demandId], A_d[1][demandId]));
        }
        html.append("</table>");

        html.append("<h3>Network-wide information</h3>");
        html.append("<ul>");
        html.append("<li>Total up time: ").append(SimEvent.secondsToYearsDaysHoursMinutesSeconds(timeNoFailure)).append("</li>");
        html.append("<li>Total sim. time: ").append(SimEvent.secondsToYearsDaysHoursMinutesSeconds(totalTime)).append("</li>");
        html.append("<li>Total up time: ").append(SimEvent.secondsToYearsDaysHoursMinutesSeconds(timeNoFailure)).append("</li>");
        html.append("<li>Total sim. time: ").append(SimEvent.secondsToYearsDaysHoursMinutesSeconds(totalTime)).append("</li>");
        html.append("</ul>");

        html.append("<table border='1'>");
        html.append("<tr><th><b>Total offered traffic (Erlangs)</b></th><th><b>Availability (classic)</b></th><th><b>Availability (weighted)</b></th></tr>");
        double H = DoubleFactory1D.dense.make(netPlan.getDemandOfferedTrafficInErlangsVector()).zSum();
        html.append(String.format("<tr><td>%.3g</td><td>%.6f</td><td>%.6f</td></tr>", H, A_D[0], A_D[1]));
        html.append("</table>");

	return html.toString();
    }

    private Pair<double[][], double[]> getCurrentAvailability(double currentSimTime)
    {
	double tObservation = currentSimTime - timeAtTransitory;

	double[] A_d_1_out = new double[D];
	double[] A_d_2_out = new double[D];
	for(int demandId = 0; demandId < D; demandId++)
	{
	    A_d_1_out[demandId] = A_d_1[demandId] / tObservation;
	    A_d_2_out[demandId] = A_d_2[demandId] / tObservation;
	}

	double A_D_1_out = A_D_1 / tObservation;
	double A_D_2_out = A_D_2 / tObservation;

	return Pair.of(new double[][] { A_d_1_out, A_d_2_out }, new double[] { A_D_1_out, A_D_2_out } );
    }
}
