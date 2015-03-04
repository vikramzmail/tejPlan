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
import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to collect performance metrics from CAC simulation.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class ConnectionPerformance extends SimStats
{
    private final int E, D;
    private final double[] u_e;
    private final double[] spareCapacity_e;
    private final int[][] linkTable;
    private final String[] nodeNameVector;
    private final int[][] demandTable;
    private final double[] h_d;
    private final double[] r_d;
    
    private double lastEventTime;
    private double timeAtTransitory;
    
    private double[] accum_connBP_offeredTraffic_d;
    private double[] accum_connBP_blockedTraffic_d;
    private double[] accum_connBP_offeredNumConnections_d;
    private double[] accum_connBP_blockedNumConnections_d;
    
    private double[] accum_numActiveConnections_d;
    private double[] previousState_numActiveConnections_d;
    private int[] max_numActiveConnections_d;
    private int[] min_numActiveConnections_d;
    
    private double[] accum_numActiveConnections_e;
    private double[] previousState_numActiveConnections_e;
    private int[] max_numActiveConnections_e;
    private int[] min_numActiveConnections_e;
    
    private double[] accum_r_d;
    private double[] previousState_r_d;
    private double[] max_r_d;
    private double[] min_r_d;
    
    private double[] accum_h_d;
    private double[] accum_lostTraffic_d;
    
    private double[] max_y_e;
    private double[] min_y_e;
    private double[] previousState_y_e;
    private double[] accum_y_e;
    
    private double[] accum_overSubscribed_e;
    private double[] accum_overSubscribed_d;
    private double[] previousState_overSubscribed_e;
    private double[] previousState_overSubscribed_d;
    
    private Map<Long, Triple<Integer, Double, Double>> previousState_r_c;
    
    private List<Triple<Double, Integer, Double>> blockedConnections;
    
    public ConnectionPerformance(NetPlan netPlan, SimState netState, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
    {
        super(netPlan, netState, simulationParameters, net2planParameters);
        
        E = netPlan.getNumberOfLinks();
        D = netPlan.getNumberOfDemands();
        
        u_e = netPlan.getLinkCapacityInErlangsVector();
        double[] r_e = netPlan.getLinkCapacityReservedForProtectionInErlangsVector();
        spareCapacity_e = new double[E]; for(int linkId = 0; linkId < E; linkId++) spareCapacity_e[linkId] = u_e[linkId] - r_e[linkId];
        linkTable = netPlan.getLinkTable();
        nodeNameVector = netPlan.getNodeNameVector();
        demandTable = netPlan.getDemandTable();
        h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        r_d = netPlan.getDemandCarriedTrafficInErlangsVector();
        
        accum_connBP_blockedNumConnections_d = new double[D];
        accum_connBP_blockedTraffic_d = new double[D];
        accum_connBP_offeredNumConnections_d = new double[D];
        accum_connBP_offeredTraffic_d = new double[D];
        
        accum_numActiveConnections_d = new double[D];
        previousState_numActiveConnections_d = new double[D];
        max_numActiveConnections_d = new int[D];
        min_numActiveConnections_d = new int[D];
        
        accum_numActiveConnections_e = new double[E];
        previousState_numActiveConnections_e = new double[E];
        max_numActiveConnections_e = new int[E];
        min_numActiveConnections_e = new int[E];
        
        accum_r_d = new double[D];
        previousState_r_d = new double[D];
        max_r_d = new double[D];
        min_r_d = new double[D];
    
        accum_h_d = new double[D];
        accum_lostTraffic_d = new double[D];
        
        max_y_e = new double[E];
        min_y_e = new double[E];
        previousState_y_e = new double[E];
        accum_y_e = new double[E];
        
        blockedConnections = new LinkedList<Triple<Double, Integer, Double>>();
        
        accum_overSubscribed_e = new double[E];
        accum_overSubscribed_d = new double[D];
        previousState_overSubscribed_e = new double[E];
        previousState_overSubscribed_d = new double[D];
        
        previousState_r_c = new HashMap<Long, Triple<Integer, Double, Double>>();
    
        reset(0);
    }
    
    @Override
    public void computeNextState(SimEvent event, List actions)
    {
        ConnectionNetState connNetState = (ConnectionNetState) netState;
        
        if (!(event instanceof CACEvent)) throw new RuntimeException("Bad");
        CACEvent cacEvent = (CACEvent) event;
        double currentSimTime = event.getEventTime();
        double tObservation = currentSimTime - lastEventTime;
        
        Iterator<Triple<Double, Integer, Double>> it = blockedConnections.iterator();
        while(it.hasNext())
        {
            Triple<Double, Integer, Double> aux = it.next();
            double finishTimeThisConnection = aux.getFirst();
            int demandId = aux.getSecond();
            double offeredTraffic = aux.getThird();
            
            if (finishTimeThisConnection < lastEventTime) { it.remove(); continue; }
            
            if (finishTimeThisConnection <= currentSimTime) it.remove();
            
            if (finishTimeThisConnection == Double.MAX_VALUE || finishTimeThisConnection >= currentSimTime)
            {
                accum_h_d[demandId] += tObservation * offeredTraffic;
                accum_lostTraffic_d[demandId] += tObservation * offeredTraffic;
            }
            else
            {
                accum_h_d[demandId] += (finishTimeThisConnection - lastEventTime) * offeredTraffic;
                accum_lostTraffic_d[demandId] += (finishTimeThisConnection - lastEventTime) * offeredTraffic;
            }
        }
        
        if (cacEvent.getEventType() == CACEvent.EventType.CONNECTION_REQUEST)
        {
            int demandId = cacEvent.getRequestDemandId();
            double h_c = cacEvent.getRequestTrafficVolumeInErlangs();
            
            double finishTime = cacEvent.getRequestDurationInSeconds() == Double.MAX_VALUE ? Double.MAX_VALUE : currentSimTime + cacEvent.getRequestDurationInSeconds();
            
            accum_connBP_offeredNumConnections_d[demandId]++;
            accum_connBP_offeredTraffic_d[demandId] += h_c;
            
            boolean isDecisionMade = false;
            for(Object action : actions)
            {
                if (!(action instanceof CACAction)) throw new RuntimeException("Bad");
                
                switch(((CACAction) action).getActionType())
                {
                    case ACCEPT_REQUEST:
                        long connId = connNetState.getConnectionRouteNextId() - 1;
                        double r_c = connNetState.getConnectionCurrentCarriedTrafficInErlangs(connId);
                        previousState_r_c.put(connId, Triple.of(demandId, h_c, r_c));
                        
                        isDecisionMade = true;
                        break;
                        
                    case BLOCK_REQUEST:
                        blockedConnections.add(Triple.of(finishTime, demandId, h_c));
                        accum_connBP_blockedTraffic_d[demandId] += h_c;
                        accum_connBP_blockedNumConnections_d[demandId]++;

                        isDecisionMade = true;
                        break;
                        
                    default:
                        break;
                }
                
                if (isDecisionMade) break;
            }
        }
        
        Set<Long> connIds = previousState_r_c.keySet();
        for(long connId : connIds)
        {
            int demandId = previousState_r_c.get(connId).getFirst();
            double h_c_thisConnection = previousState_r_c.get(connId).getSecond();
            double r_c_thisConnection = previousState_r_c.get(connId).getThird();
            accum_lostTraffic_d[demandId] += Math.max(0, h_c_thisConnection - r_c_thisConnection) * tObservation;
            accum_h_d[demandId] += h_c_thisConnection * tObservation;
        }
        
        for(int demandId = 0; demandId < D; demandId++)
        {
            accum_numActiveConnections_d[demandId] += previousState_numActiveConnections_d[demandId] * tObservation;
            accum_r_d[demandId] += previousState_r_d[demandId] * tObservation;
            
            accum_overSubscribed_d[demandId] += previousState_overSubscribed_d[demandId] * tObservation;
        }
        
        for(int linkId = 0; linkId < E; linkId++)
        {
            accum_numActiveConnections_e[linkId] += previousState_numActiveConnections_e[linkId] * tObservation;
            accum_y_e[linkId] += previousState_y_e[linkId] * tObservation;

            accum_overSubscribed_e[linkId] += previousState_overSubscribed_e[linkId] * tObservation;
        }
        
        updatePreviousState();
        
        lastEventTime = currentSimTime;
    }
    
    private void updatePreviousState()
    {
        ConnectionNetState connNetState = (ConnectionNetState) netState;
        long[] connIds = connNetState.getConnectionIds();
        long[] connRouteIds = connNetState.getConnectionRouteIds();

        Arrays.fill(previousState_numActiveConnections_d, 0);
        Arrays.fill(previousState_numActiveConnections_e, 0);
        Arrays.fill(previousState_y_e, 0);
        Arrays.fill(previousState_r_d, 0);
        Arrays.fill(previousState_overSubscribed_d, 0);
        Arrays.fill(previousState_overSubscribed_e, 0);
        
        for(long connRouteId : connRouteIds)
        {
            long connId = connNetState.getConnectionRouteConnection(connRouteId);
            int demandId = connNetState.getConnectionDemand(connId);
            double carriedTraffic = connNetState.getConnectionRouteCarriedTrafficInErlangs(connRouteId);
            previousState_numActiveConnections_d[demandId]++;
            
            previousState_r_d[demandId] += carriedTraffic;
            
            int[] seqLinks = connNetState.getConnectionRouteSequenceOfLinks(connRouteId);
            for(int linkId : seqLinks) previousState_y_e[linkId] += carriedTraffic;
            
            int[] linkIds = IntUtils.unique(seqLinks);
            for(int linkId : linkIds) previousState_numActiveConnections_e[linkId]++;
        }
        
        double Y_e = DoubleUtils.sum(previousState_y_e);
        double R_d = DoubleUtils.sum(previousState_r_d);
        
        previousState_r_c.clear();
        for(long connId : connIds)
        {
            double h_d_thisConnection = connNetState.getConnectionRequestedTrafficInErlangs(connId);
            double r_d_thisConnection = connNetState.getConnectionCurrentCarriedTrafficInErlangs(connId);
            int demandId = connNetState.getConnectionDemand(connId);
            
            previousState_overSubscribed_d[demandId] += R_d == 0 ? 0 : Math.max(0, r_d_thisConnection - h_d_thisConnection) / R_d;
            
            previousState_r_c.put(connId, Triple.of(demandId, h_d_thisConnection, r_d_thisConnection));
        }
        
        for(int demandId = 0; demandId < D; demandId++)
        {
            max_numActiveConnections_d[demandId] = Math.max(max_numActiveConnections_d[demandId], (int) previousState_numActiveConnections_d[demandId]);
            min_numActiveConnections_d[demandId] = Math.min(min_numActiveConnections_d[demandId], (int) previousState_numActiveConnections_d[demandId]);
            
            max_r_d[demandId] = Math.max(max_r_d[demandId], previousState_r_d[demandId]);
            min_r_d[demandId] = Math.min(min_r_d[demandId], previousState_r_d[demandId]);
        }
        
        for(int linkId = 0; linkId < E; linkId++)
        {
            max_numActiveConnections_e[linkId] = Math.max(max_numActiveConnections_e[linkId], (int) previousState_numActiveConnections_e[linkId]);
            min_numActiveConnections_e[linkId] = Math.min(min_numActiveConnections_e[linkId], (int) previousState_numActiveConnections_e[linkId]);

            max_y_e[linkId] = Math.max(max_y_e[linkId], (int) previousState_y_e[linkId]);
            min_y_e[linkId] = Math.min(min_y_e[linkId], (int) previousState_y_e[linkId]);
            
            previousState_overSubscribed_e[linkId] += Y_e == 0 ? 0 : Math.max(0, previousState_y_e[linkId] - spareCapacity_e[linkId]) / Y_e;
        }
    }

    @Override
    public void reset(double currentSimTime)
    {
        lastEventTime = currentSimTime;
        timeAtTransitory = currentSimTime;
        
        Arrays.fill(accum_connBP_blockedNumConnections_d, 0);
        Arrays.fill(accum_connBP_blockedTraffic_d, 0);
        Arrays.fill(accum_connBP_offeredNumConnections_d, 0);
        Arrays.fill(accum_connBP_offeredTraffic_d, 0);
        
        Arrays.fill(accum_numActiveConnections_d, 0);
        Arrays.fill(previousState_numActiveConnections_d, 0);
        Arrays.fill(max_numActiveConnections_d, 0);
        Arrays.fill(min_numActiveConnections_d, 0);
        
        Arrays.fill(accum_numActiveConnections_e, 0);
        Arrays.fill(previousState_numActiveConnections_e, 0);
        Arrays.fill(max_numActiveConnections_e, 0);
        Arrays.fill(min_numActiveConnections_e, 0);
        
        Arrays.fill(accum_r_d, 0);
        Arrays.fill(previousState_r_d, 0);
        Arrays.fill(max_r_d, 0);
        Arrays.fill(min_r_d, 0);
        
        Arrays.fill(accum_h_d, 0);
        Arrays.fill(accum_lostTraffic_d, 0);
        
        Arrays.fill(max_y_e, 0);
        Arrays.fill(min_y_e, 0);
        Arrays.fill(previousState_y_e, 0);
        Arrays.fill(accum_y_e, 0);
        
        Arrays.fill(accum_overSubscribed_e, 0);
        Arrays.fill(accum_overSubscribed_d, 0);
        
        Iterator<Triple<Double, Integer, Double>> it = blockedConnections.iterator();
        while(it.hasNext())
        {
            Triple<Double, Integer, Double> aux = it.next();
            if (aux.getFirst() <= currentSimTime) it.remove();
        }
        
        updatePreviousState();
    }

    @Override
    public String getResults(double currentSimTime)
    {
	if (currentSimTime == 0) return "<p>No information available</p>";
        
        double tObservation = currentSimTime - timeAtTransitory;
	if (tObservation == 0) return "<p>No information available</p>";

        double tObservation_lastPeriod = currentSimTime - lastEventTime;
        
	String html;
        
        try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/internal/sim/stats/ConnectionPerformance.html").toURI().toURL()); }
        catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }
        
        double total_blockedNumConnections_d = DoubleUtils.sum(accum_connBP_blockedNumConnections_d);
        double total_blockedTraffic_d = DoubleUtils.sum(accum_connBP_blockedTraffic_d);
        double total_offeredNumConnections_d = DoubleUtils.sum(accum_connBP_offeredNumConnections_d);
        double total_offeredTraffic_d = DoubleUtils.sum(accum_connBP_offeredTraffic_d);
        
        double connectionBlockingProbability = total_offeredNumConnections_d == 0 ? 0 : 100.0 * total_blockedNumConnections_d / total_offeredNumConnections_d;
        double connectionWeightedBlockingProbability = total_offeredTraffic_d == 0 ? 0 : 100.0 * total_blockedTraffic_d / total_offeredTraffic_d;
        int max_numActiveConnections = (int) IntUtils.sum(max_numActiveConnections_d);
        int min_numActiveConnections = (int) IntUtils.sum(min_numActiveConnections_d);
        double avg_numActiveConnections = DoubleUtils.sum(accum_numActiveConnections_d) / tObservation; // (DoubleUtils.sum(accum_numActiveConnections_d) + tObservation_lastPeriod * DoubleUtils.sum(previousState_numActiveConnections_d)) / tObservation;
        
        double max_totalCarriedTraffic = DoubleUtils.sum(max_r_d);
        double min_totalCarriedTraffic = DoubleUtils.sum(min_r_d);
        double previousState_totalCarriedTraffic = DoubleUtils.sum(previousState_r_d);
        double accum_totalCarriedTraffic = DoubleUtils.sum(accum_r_d);
        double avg_totalCarriedTraffic = accum_totalCarriedTraffic / tObservation; // (accum_totalCarriedTraffic + tObservation_lastPeriod * previousState_totalCarriedTraffic) / tObservation;
        
//        double[] aux_accum_h_d = DoubleUtils.copy(accum_h_d);
//        double[] aux_accum_lostTraffic_d = DoubleUtils.copy(accum_lostTraffic_d);
//        Iterator<Triple<Double, Integer, Double>> it = blockedConnections.iterator();
//        while(it.hasNext())
//        {
//            Triple<Double, Integer, Double> aux = it.next();
//            double finishTimeThisConnection = aux.getFirst();
//            int demandId = aux.getSecond();
//            double offeredTraffic = aux.getThird();
//            
//            if (finishTimeThisConnection < lastEventTime) { it.remove(); continue; }
//            
//            if (finishTimeThisConnection <= currentSimTime) it.remove();
//            
//            if (finishTimeThisConnection == Double.MAX_VALUE || finishTimeThisConnection >= currentSimTime)
//            {
//                aux_accum_h_d[demandId] += tObservation_lastPeriod * offeredTraffic;
//                aux_accum_lostTraffic_d[demandId] += tObservation_lastPeriod * offeredTraffic;
//            }
//            else
//            {
//                aux_accum_h_d[demandId] += (finishTimeThisConnection - lastEventTime) * offeredTraffic;
//                aux_accum_lostTraffic_d[demandId] += (finishTimeThisConnection - lastEventTime) * offeredTraffic;
//            }
//        }
//        
//        Set<Long> connIds = previousState_r_c.keySet();
//        for(long connId : connIds)
//        {
//            int demandId = previousState_r_c.get(connId).getFirst();
//            double h_c_thisConnection = previousState_r_c.get(connId).getSecond();
//            double r_c_thisConnection = previousState_r_c.get(connId).getThird();
//            aux_accum_lostTraffic_d[demandId] += Math.max(0, h_c_thisConnection - r_c_thisConnection) * tObservation_lastPeriod;
//            aux_accum_h_d[demandId] += h_c_thisConnection * tObservation_lastPeriod;
//        }

        double avg_lostTraffic = DoubleUtils.sum(accum_lostTraffic_d) / tObservation;
        double avg_offeredTraffic = DoubleUtils.sum(accum_h_d) / tObservation;
        double trafficBlockingProbability = avg_offeredTraffic == 0 ? 0 : 100.0 * avg_lostTraffic / avg_offeredTraffic;
        double avg_overSubscribing_e = 100.0 * DoubleUtils.sum(accum_overSubscribed_e) / tObservation; // 100.0 * DoubleUtils.sum(DoubleUtils.sum(accum_overSubscribed_e, DoubleUtils.mult(previousState_overSubscribed_e, tObservation_lastPeriod))) / tObservation;
        double avg_overSubscribing_d = 100.0 * DoubleUtils.sum(accum_overSubscribed_d) / tObservation; // 100.0 * DoubleUtils.sum(DoubleUtils.sum(accum_overSubscribed_d, DoubleUtils.mult(previousState_overSubscribed_d, tObservation_lastPeriod))) / tObservation;

        html = html.replaceFirst("#network_numConnections#", String.format("%d, %d, %.3f", max_numActiveConnections, min_numActiveConnections, avg_numActiveConnections));
        html = html.replaceFirst("#network_carriedTraffic#", String.format("%.3f, %.3f, %.3f", max_totalCarriedTraffic, min_totalCarriedTraffic, avg_totalCarriedTraffic));
        html = html.replaceFirst("#network_connBlockingProbability#", String.format("%.3f", connectionBlockingProbability));
        html = html.replaceFirst("#network_connWeightedBlockingProbability#", String.format("%.3f", connectionWeightedBlockingProbability));
        html = html.replaceFirst("#network_trafficBlockingProbability#", String.format("%.3f", trafficBlockingProbability));
        html = html.replaceFirst("#network_avgOverSubscribedLinkCapacity#", String.format("%.3f", avg_overSubscribing_e));
        html = html.replaceFirst("#network_avgExcessCarriedTraffic#", String.format("%.3f", avg_overSubscribing_d));
        
        StringBuilder perDemandInformation = new StringBuilder();
        for(int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = demandTable[demandId][0];
            String ingressNodeName = nodeNameVector[ingressNodeId];
            int egressNodeId = demandTable[demandId][1];
            String egressNodeName = nodeNameVector[egressNodeId];
            Map<String, String> demandAttributes = netPlan.getDemandSpecificAttributes(demandId);
            String demandAttributeString = demandAttributes.isEmpty() ? "none" : StringUtils.mapToString(demandAttributes, "=", ", ");
            
            double connBlockingProbability = accum_connBP_offeredNumConnections_d[demandId] == 0 ? 0 : 100.0 * accum_connBP_blockedNumConnections_d[demandId] / accum_connBP_offeredNumConnections_d[demandId];
            double connWeightedBlockingProbability = accum_connBP_offeredTraffic_d[demandId] == 0 ? 0 : 100.0 * accum_connBP_blockedTraffic_d[demandId] / accum_connBP_offeredTraffic_d[demandId];
            double avg_r_d = accum_r_d[demandId] / tObservation; // (accum_r_d[demandId] + tObservation_lastPeriod * previousState_r_d[demandId]) / tObservation;
            double avg_numConnections = accum_numActiveConnections_d[demandId] / tObservation; //(accum_numActiveConnections_d[demandId] + tObservation_lastPeriod * previousState_numActiveConnections_d[demandId]) / tObservation;
            double avg_h_d = accum_h_d[demandId] / tObservation;
            
            double avg_lostTraffic_d = accum_lostTraffic_d[demandId] / tObservation;
            double trafficBlockingProbability_d = avg_h_d == 0 ? 0 : 100.0 * avg_lostTraffic_d / avg_h_d;
           
            perDemandInformation.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%.3f</td><td>%.3f</td><td>%.3f, %.3f, %.3f</td><td>%d, %d, %.3f</td><td>%.3f</td><td>%.3f</td><td>%.3f</td><td>%s</td></tr>", demandId, ingressNodeId, ingressNodeName, egressNodeId, egressNodeName, h_d[demandId], avg_h_d, r_d[demandId], max_r_d[demandId], min_r_d[demandId], avg_r_d, max_numActiveConnections_d[demandId], min_numActiveConnections_d[demandId], avg_numConnections, connBlockingProbability, connWeightedBlockingProbability, trafficBlockingProbability_d, demandAttributeString));
        }
        
        html = html.replaceFirst("#perDemandInformation#", perDemandInformation.toString());
        
        StringBuilder perLinkInformation = new StringBuilder();
        for(int linkId = 0; linkId < E; linkId++)
        {
            int originNodeId = linkTable[linkId][0];
            String originNodeName = nodeNameVector[originNodeId];
            int destinationNodeId = linkTable[linkId][1];
            String destinationNodeName = nodeNameVector[destinationNodeId];
            Map<String, String> linkAttributes = netPlan.getLinkSpecificAttributes(linkId);
            String linkAttributeString = linkAttributes.isEmpty() ? "none" : StringUtils.mapToString(linkAttributes, "=", ", ");
            
            double avg_y_e = accum_y_e[linkId] / tObservation; //(accum_y_e[linkId] + tObservation_lastPeriod * previousState_y_e[linkId]) / tObservation;
            double avg_numConnections = accum_numActiveConnections_e[linkId] / tObservation; //(accum_numActiveConnections_e[linkId] + tObservation_lastPeriod * previousState_numActiveConnections_e[linkId]) / tObservation;
            
            perLinkInformation.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%.3f</td><td>%.3f, %.3f, %.3f</td><td>%d, %d, %.3f</td><td>%s</td></tr>", linkId, originNodeId, originNodeName, destinationNodeId, destinationNodeName, u_e[linkId], spareCapacity_e[linkId], max_y_e[linkId], min_y_e[linkId], avg_y_e, max_numActiveConnections_e[linkId], min_numActiveConnections_e[linkId], avg_numConnections, linkAttributeString));
        }
        
        html = html.replaceFirst("#perLinkInformation#", perLinkInformation.toString());
        
        return html;
    }
    
}
