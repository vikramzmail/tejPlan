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

import com.tejas.engine.internal.sim.stats.SimStats;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.libraries.GraphTheoryMetrics;
import com.tejas.engine.libraries.NetworkPerformanceMetrics;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Class to collect performance metrics from time-varying traffic simulation.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public final class NetworkEvolution extends SimStats
{
    private double lastEventTime;
    private double timeAtTransitory;

    private double accum_E, previousState_E;
    private double accum_nodeDegree;
    private double previousState_nodeDegree;
    private double accum_networkDiameter_hops, accum_networkDiameter_km, accum_networkDiameter_ms;
    private int previousState_networkDiameter_hops;
    private double previousState_networkDiameter_km, previousState_networkDiameter_ms;
    private double accum_U_e, previousState_U_e;
    private double accum_H_d, previousState_H_d;
    private double accum_averageTrafficPerNodePair, previousState_averageTrafficPerNodePair;
    private double accum_blockedTraffic, previousState_blockedTraffic;
    private double accum_R, previousState_R;
    private double accum_max_rho_e, accum_max_rho_e_without_protection, previousState_max_rho_e, previousState_max_rho_e_without_protection;
    private double accum_averageRouteLength_hops, accum_averageRouteLength_km, accum_averageRouteLength_ms;
    private double previousState_averageRouteLength_hops, previousState_averageRouteLength_km, previousState_averageRouteLength_ms;
    private double accum_S, previousState_S;
    private double accum_u_e_reservedForProtection_avg, accum_percentageReserved, previousState_u_e_reservedForProtection_avg, previousState_percentageReserved;
    private double accum_percentageUnprotected, accum_percentageDedicated, accum_percentageShared;
    private double previousState_percentageUnprotected, previousState_percentageDedicated, previousState_percentageShared;
    private double accum_oversubscribedTraffic;
    private double previousState_oversubscribedTraffic;

    /**
     * Default constructor.
     * 
     * @param netPlan  Reference to the planned network design
     * @param netState Reference to the current network state
     * @since 0.2.3
     */
    public NetworkEvolution(NetPlan netPlan, TimeVaryingNetState netState, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
    {
	super(netPlan, netState, simulationParameters, net2planParameters);
        
        previousState_E = 0;
        previousState_nodeDegree = 0;
        previousState_networkDiameter_hops = 0;
        previousState_networkDiameter_km = 0;
        previousState_networkDiameter_ms = 0;
        previousState_U_e = 0;
        previousState_H_d = 0;
        previousState_averageTrafficPerNodePair = 0;
        previousState_blockedTraffic = 0;
        previousState_R = 0;
        previousState_max_rho_e = 0;
        previousState_max_rho_e_without_protection = 0;
        previousState_averageRouteLength_hops = 0;
        previousState_averageRouteLength_km = 0;
        previousState_averageRouteLength_ms = 0;
        previousState_S = 0;
        previousState_u_e_reservedForProtection_avg = 0;
        previousState_percentageReserved = 0;
        previousState_percentageUnprotected = 0;
        previousState_percentageDedicated = 0;
        previousState_percentageShared = 0;
        previousState_oversubscribedTraffic = 0;
        
        reset(0);
    }

    @Override
    public void computeNextState(SimEvent event, List actions)
    {
        double currentSimTime = event.getEventTime();
        double tObservation = currentSimTime - lastEventTime;
        
        accum_E += tObservation * previousState_E;
        accum_nodeDegree += tObservation * previousState_nodeDegree;
        accum_networkDiameter_hops += tObservation * previousState_networkDiameter_hops;
        accum_networkDiameter_km += tObservation * previousState_networkDiameter_km;
        accum_networkDiameter_ms += tObservation * previousState_networkDiameter_ms;
        accum_U_e += tObservation * previousState_U_e;
        accum_H_d += tObservation * previousState_H_d;
        accum_averageTrafficPerNodePair += tObservation * previousState_averageTrafficPerNodePair;
        accum_blockedTraffic += tObservation * previousState_blockedTraffic;
        accum_R += tObservation * previousState_R;
        accum_max_rho_e += tObservation * previousState_max_rho_e;
        accum_max_rho_e_without_protection += tObservation * previousState_max_rho_e_without_protection;
        accum_averageRouteLength_hops += tObservation * previousState_averageRouteLength_hops;
        accum_averageRouteLength_km += tObservation * previousState_averageRouteLength_km;
        accum_averageRouteLength_ms += tObservation * previousState_averageRouteLength_ms;
        accum_S += tObservation * previousState_S;
        accum_u_e_reservedForProtection_avg += tObservation * previousState_u_e_reservedForProtection_avg;
        accum_percentageReserved += tObservation * previousState_percentageReserved;
        accum_percentageUnprotected += tObservation * previousState_percentageUnprotected;
        accum_percentageDedicated += tObservation * previousState_percentageDedicated;
        accum_percentageShared += tObservation * previousState_percentageShared;
        accum_oversubscribedTraffic += tObservation * previousState_oversubscribedTraffic;

        updatePreviousState();

        lastEventTime = currentSimTime;
    }
    
    private void updatePreviousState()
    {
        NetPlan currentNetPlan = netState.convertToNetPlan();
        NetworkPerformanceMetrics metrics = new NetworkPerformanceMetrics(currentNetPlan, net2planParameters);

        int N = currentNetPlan.getNumberOfNodes();
        int[][] linkTable = currentNetPlan.getLinkTable();
        double[] l_e = currentNetPlan.getLinkLengthInKmVector();
        double[] u_e = currentNetPlan.getLinkCapacityInErlangsVector();

        double[] u_e_reservedForProtection = currentNetPlan.getLinkCapacityReservedForProtectionInErlangsVector();
        double U_e_reservedForProtection = DoubleUtils.sum(u_e_reservedForProtection);
        double[] protectionDegree = metrics.getTrafficProtectionDegree();
        
        GraphTheoryMetrics metrics_hops = new GraphTheoryMetrics(linkTable, N, null);
        GraphTheoryMetrics metrics_km = new GraphTheoryMetrics(linkTable, N, l_e);
        GraphTheoryMetrics metrics_sec = new GraphTheoryMetrics(linkTable, N, currentNetPlan.getLinkPropagationDelayInSecondsVector());
        
        previousState_E = currentNetPlan.getNumberOfLinks();
        previousState_nodeDegree = IntUtils.average(metrics_hops.getOutNodeDegree());
        previousState_networkDiameter_hops = (int) metrics_hops.getDiameter();
        previousState_networkDiameter_km = metrics_km.getDiameter();
        previousState_networkDiameter_ms = 1000 * metrics_sec.getDiameter();
        previousState_U_e = DoubleUtils.sum(u_e);
        
        previousState_H_d = DoubleUtils.sum(currentNetPlan.getDemandOfferedTrafficInErlangsVector());
        previousState_averageTrafficPerNodePair = metrics.getNodePairAverageOfferedTrafficInErlangs();
        previousState_blockedTraffic = metrics.getBlockedTrafficPercentage();
        
        previousState_R = currentNetPlan.getNumberOfRoutes();
        previousState_max_rho_e = previousState_U_e == 0 ? 0 : currentNetPlan.getLinkMaximumUtilization();
        previousState_max_rho_e_without_protection = previousState_U_e == 0 ? 0 : currentNetPlan.getLinkMaximumUtilizationWithoutConsiderReservedBandwidthForProtection();
        previousState_averageRouteLength_hops = metrics.getRouteAverageLength(null);
        previousState_averageRouteLength_km = metrics.getRouteAverageLength(currentNetPlan.getLinkLengthInKmVector());
        previousState_averageRouteLength_ms = 1000 * metrics.getRouteAverageLength(currentNetPlan.getLinkPropagationDelayInSecondsVector());
        
        previousState_S = currentNetPlan.getNumberOfProtectionSegments();
        previousState_u_e_reservedForProtection_avg = previousState_E == 0 ? 0 : U_e_reservedForProtection / previousState_E;
        previousState_percentageReserved = previousState_U_e == 0 ? 0 : 100 * U_e_reservedForProtection / previousState_U_e;

        previousState_percentageUnprotected = protectionDegree[0];
        previousState_percentageDedicated = protectionDegree[1];
        previousState_percentageShared = protectionDegree[2];
        
        previousState_oversubscribedTraffic = 0;
        double totalCarriedTraffic = 0;
        
        double[] y_e = currentNetPlan.getLinkCarriedTrafficInErlangsVector();
        for(int linkId = 0; linkId < previousState_E; linkId++)
        {
            double carriedTraffic = y_e[linkId] + u_e_reservedForProtection[linkId];
            previousState_oversubscribedTraffic += Math.max(0, carriedTraffic - u_e[linkId]);
            totalCarriedTraffic += carriedTraffic;
        }
        
        previousState_oversubscribedTraffic = totalCarriedTraffic == 0 ? 0 : 100 * previousState_oversubscribedTraffic / totalCarriedTraffic;
    }

    @Override
    public void reset(double currentSimTime)
    {
	timeAtTransitory = currentSimTime;
	lastEventTime = currentSimTime;
        
        accum_E = 0;
        accum_nodeDegree = 0;
        accum_networkDiameter_hops = 0;
        accum_networkDiameter_km = 0;
        accum_networkDiameter_ms = 0;
        accum_U_e = 0;
        accum_H_d = 0;
        accum_averageTrafficPerNodePair = 0;
        accum_blockedTraffic = 0;
        accum_R = 0;
        accum_max_rho_e = 0;
        accum_max_rho_e_without_protection = 0;
        accum_averageRouteLength_hops = 0;
        accum_averageRouteLength_km = 0;
        accum_averageRouteLength_ms = 0;
        accum_S = 0;
        accum_u_e_reservedForProtection_avg = 0;
        accum_percentageReserved = 0;
        accum_percentageUnprotected = 0;
        accum_percentageDedicated = 0;
        accum_percentageShared = 0;
        accum_oversubscribedTraffic = 0;
        
        updatePreviousState();
    }

    @Override
    public String getResults(double currentSimTime)
    {
	String html;

	if (currentSimTime == 0)
	{
            html = "<h2>Network evolution</h2><p>No information available</p>";
	}
	else
	{
	    double tObservation = currentSimTime - timeAtTransitory;
            double tObservation_lastPeriod = currentSimTime - lastEventTime;

            double aux_E = (accum_E + tObservation_lastPeriod * previousState_E) / tObservation;
            double aux_nodeDegree = (accum_nodeDegree + tObservation_lastPeriod * previousState_nodeDegree) / tObservation;
            double aux_networkDiameter_hops = (accum_networkDiameter_hops + tObservation_lastPeriod * previousState_networkDiameter_hops) / tObservation;
            double aux_networkDiameter_km = (accum_networkDiameter_km + tObservation_lastPeriod * previousState_networkDiameter_km) / tObservation;
            double aux_networkDiameter_ms = (accum_networkDiameter_ms + tObservation_lastPeriod * previousState_networkDiameter_ms) / tObservation;
            double aux_U_e = (accum_U_e + tObservation_lastPeriod * previousState_U_e) / tObservation;
            double aux_H_d = (accum_H_d + tObservation_lastPeriod * previousState_H_d) / tObservation;
            double aux_averageTrafficPerNodePair = (accum_averageTrafficPerNodePair + tObservation_lastPeriod * previousState_averageTrafficPerNodePair) / tObservation;
            double aux_blockedTraffic = (accum_blockedTraffic + tObservation_lastPeriod * previousState_blockedTraffic) / tObservation;
            double aux_R = (accum_R + tObservation_lastPeriod * previousState_R) / tObservation;
            double aux_max_rho_e = (accum_max_rho_e + tObservation_lastPeriod * previousState_max_rho_e) / tObservation;
            double aux_max_rho_e_without_protection = (accum_max_rho_e_without_protection + tObservation_lastPeriod * previousState_max_rho_e_without_protection) / tObservation;
            double aux_averageRouteLength_hops = (accum_averageRouteLength_hops + tObservation_lastPeriod * previousState_averageRouteLength_hops) / tObservation;
            double aux_averageRouteLength_km = (accum_averageRouteLength_km + tObservation_lastPeriod * previousState_averageRouteLength_km) / tObservation;
            double aux_averageRouteLength_ms = (accum_averageRouteLength_ms + tObservation_lastPeriod * previousState_averageRouteLength_ms) / tObservation;
            double aux_S = (accum_S + tObservation_lastPeriod * previousState_S) / tObservation;
            double aux_u_e_reservedForProtection_avg = (accum_u_e_reservedForProtection_avg + tObservation_lastPeriod * previousState_u_e_reservedForProtection_avg) / tObservation;
            double aux_percentageReserved = (accum_percentageReserved + tObservation_lastPeriod * previousState_percentageReserved) / tObservation;
            double aux_percentageUnprotected = (accum_percentageUnprotected + tObservation_lastPeriod * previousState_percentageUnprotected) / tObservation;
            double aux_percentageDedicated = (accum_percentageDedicated + tObservation_lastPeriod * previousState_percentageDedicated) / tObservation;
            double aux_percentageShared = (accum_percentageShared + tObservation_lastPeriod * previousState_percentageShared) / tObservation;
            double aux_oversubscribedTraffic = (accum_oversubscribedTraffic + tObservation_lastPeriod * previousState_oversubscribedTraffic) / tObservation;
            
            int N = netPlan.getNumberOfNodes();
            int D = netPlan.getNumberOfDemands();
            double erlangsToBpsFactor = Double.parseDouble(Configuration.getOption("binaryRateInBitsPerSecondPerErlang"));
            
            try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/internal/sim/stats/NetworkEvolution.html").toURI().toURL()); }
            catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }
            
            html = html.replaceFirst("#topologyInfo_numberOfNodes#", String.format("%d", N));
            html = html.replaceFirst("#topologyInfo_numberOfLinks#", String.format("%.3f", aux_E));
            html = html.replaceFirst("#topologyInfo_nodeDegree#", String.format("%.3f", aux_nodeDegree));
            html = html.replaceFirst("#topologyInfo_diameter#", String.format("%.3f, %.3f, %.3g", aux_networkDiameter_hops, aux_networkDiameter_km, aux_networkDiameter_ms));
            html = html.replaceFirst("#topologyInfo_totalCapacity#", String.format("%.3f, %.3g", aux_U_e, aux_U_e * erlangsToBpsFactor));
            
            html = html.replaceFirst("#trafficInfo_numberOfDemands#", String.format("%d", D));
            html = html.replaceFirst("#trafficInfo_totalTraffic#", String.format("%.3f, %.3g", aux_H_d, aux_H_d * erlangsToBpsFactor));
            html = html.replaceFirst("#trafficInfo_trafficPerNodePair#", String.format("%.3f, %.3g", aux_averageTrafficPerNodePair, aux_averageTrafficPerNodePair * erlangsToBpsFactor));
            html = html.replaceFirst("#trafficInfo_blockedTraffic#", String.format("%.3f", aux_blockedTraffic));
            html = html.replaceFirst("#trafficInfo_oversubscribedTraffic#", String.format("%.3f", aux_oversubscribedTraffic));
            
            html = html.replaceFirst("#routingInfo_numberOfRoutes#", String.format("%.3f", aux_R));
            html = html.replaceFirst("#routingInfo_bottleneckUtilization#", String.format("%.3f, %.3f", aux_max_rho_e, aux_max_rho_e_without_protection));
            html = html.replaceFirst("#routingInfo_routeLength#", String.format("%.3f, %.3f, %.3g", aux_averageRouteLength_hops, aux_averageRouteLength_km, aux_averageRouteLength_ms));
            
            html = html.replaceFirst("#segmentInfo_numberOfSegments#", String.format("%.3f", aux_S));
            html = html.replaceFirst("#segmentInfo_capacityReserved#", String.format("%.3f, %.3f, %.3g", aux_percentageReserved, aux_u_e_reservedForProtection_avg, aux_u_e_reservedForProtection_avg * erlangsToBpsFactor));
            html = html.replaceFirst("#segmentInfo_unprotectedTraffic#", String.format("%.3f", aux_percentageUnprotected));
            html = html.replaceFirst("#segmentInfo_dedicatedProtectionTraffic#", String.format("%.3f", aux_percentageDedicated));
            html = html.replaceFirst("#segmentInfo_sharedProtectionTraffic#", String.format("%.3f", aux_percentageShared));
	}

	return html;
    }
}
