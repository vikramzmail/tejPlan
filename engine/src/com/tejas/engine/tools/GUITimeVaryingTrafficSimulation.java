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

package com.tejas.engine.tools;

import com.tejas.engine.tools.GUINetworkDesign;
import com.tejas.engine.tools.GUISimulationTemplate;

import static com.tejas.engine.tools.GUINetworkDesign.netPlanTablesHeader;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.internal.sim.impl.TimeVaryingTrafficSimulation;
import com.tejas.engine.libraries.GraphTheoryMetrics;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.NetworkPerformanceMetrics;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.CellRenderers;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.FixedColumnDecorator;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.ParamValueTable;
import com.tejas.engine.utils.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Graphical-user interface for the time-varying traffic simulation.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class GUITimeVaryingTrafficSimulation extends GUISimulationTemplate
{
    private static final long serialVersionUID = 1L;

    private JTable[] netPlanTables;
    private ParamValueTable[] networkSummaryTables;
    
    private List<FixedColumnDecorator> decorators;

    private JTextField networkName;
    private JTextArea networkDescription;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public GUITimeVaryingTrafficSimulation()
    {
        super("TIME-VARYING TRAFFIC SIMULATION", new TimeVaryingTrafficSimulation(), SimulatorType.SYNCHRONOUS);
    }

    @Override
    public void updateNetPlanView()
    {
        for (int tableId = 0; tableId < netPlanTables.length; tableId++)
        {
            netPlanTables[tableId].setEnabled(false);
            ((DefaultTableModel) netPlanTables[tableId].getModel()).setDataVector(new Object[1][netPlanTablesHeader[tableId].length], netPlanTablesHeader[tableId]);
        }

        NetPlan netPlan = simKernel.getNetPlan();
        TimeVaryingNetState netState = (TimeVaryingNetState) simKernel.getNetState();
        NetPlan newNetPlan = netState.convertToNetPlan();
        Map<String, String> net2planParameters = Configuration.getOptions();
        
        int N = netPlan.getNumberOfNodes();
        int E = netState.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        int R = netState.getNumberOfRoutes();
        int S = netState.getNumberOfProtectionSegments();
        int numSRGs = netState.getNumberOfSRGs();
        Map<String, String> networkAttributes = netPlan.getNetworkAttributes();

        Object[][] nodeData = new Object[N][netPlanTablesHeader[0].length];
        Object[][] linkData = new Object[E][netPlanTablesHeader[1].length];
        Object[][] demandData = new Object[D][netPlanTablesHeader[2].length];
        Object[][] routeData = new Object[R][netPlanTablesHeader[3].length];
        Object[][] segmentData = new Object[S][netPlanTablesHeader[4].length];
        Object[][] srgData = new Object[numSRGs][netPlanTablesHeader[5].length];
        Object[][] networkData = new Object[networkAttributes.size()][netPlanTablesHeader[6].length];

        double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

        int[][] demandTable = netPlan.getDemandTable();
        double[] h_d = netState.getDemandOfferedTrafficInErlangsVector();
        double[] r_d = netState.getDemandCarriedTrafficInErlangsVector();
        double[] lostTraffic_d = new double[D];
        
        // Auxiliary variables
        double[] ingressTrafficPerNode = netState.getNodeIngressTrafficInErlangsVector();
        double[] egressTrafficPerNode = netState.getNodeEgressTrafficInErlangsVector();
        double[] traversingTrafficPerNode = netState.getNodeTraversingTrafficInErlangsVector();

        NetworkPerformanceMetrics metrics = new NetworkPerformanceMetrics(newNetPlan, net2planParameters);
        double max_rho_e = metrics.getLinkMaximumUtilization();

        for (int demandId = 0; demandId < D; demandId++)
        {
            h_d[demandId] = DoubleUtils.isEqualWithinAbsoluteTolerance(h_d[demandId], 0, PRECISIONFACTOR) ? 0 : h_d[demandId];
            r_d[demandId] = DoubleUtils.isEqualWithinAbsoluteTolerance(r_d[demandId], 0, PRECISIONFACTOR) ? 0 : r_d[demandId];
            lostTraffic_d[demandId] = h_d[demandId] == 0 ? 0 : 100 * (1 - r_d[demandId] / h_d[demandId]);
            if (lostTraffic_d[demandId] < PRECISIONFACTOR) lostTraffic_d[demandId] = 0;
        }
        
        long[] routeIds = netState.getRouteIds();
        long[] segmentIds = netState.getProtectionSegmentIds();
        
        // Get node data and put in the corresponding table
        if (N > 0)
        {
            for (int nodeId = 0; nodeId < N; nodeId++)
            {
                long[] srgIds = netState.getNodeSRGs(nodeId);
                
                nodeData[nodeId][0] = nodeId;
                nodeData[nodeId][1] = netPlan.getNodeName(nodeId);
                nodeData[nodeId][2] = netPlan.getNodeXYPosition(nodeId)[0];
                nodeData[nodeId][3] = netPlan.getNodeXYPosition(nodeId)[1];
                nodeData[nodeId][4] = ingressTrafficPerNode[nodeId];
                nodeData[nodeId][5] = egressTrafficPerNode[nodeId];
                nodeData[nodeId][6] = traversingTrafficPerNode[nodeId];
                nodeData[nodeId][7] = ingressTrafficPerNode[nodeId] + egressTrafficPerNode[nodeId] + traversingTrafficPerNode[nodeId];
                nodeData[nodeId][8] = srgIds.length > 0 ? LongUtils.join(srgIds, ", ") : "none";
                nodeData[nodeId][9] = StringUtils.mapToString(netPlan.getNodeSpecificAttributes(nodeId), "=", ", ");
            }

            netPlanTables[0].setEnabled(true);
            ((DefaultTableModel) netPlanTables[0].getModel()).setDataVector(nodeData, netPlanTablesHeader[0]);
        }

        // Get link data and put in the corresponding table
        if (E > 0)
        {
            long[] linkIds = netState.getLinkIds();
            int i = 0;
            
            for (long linkId : linkIds)
            {
                long[] srgIds = netState.getLinkSRGs(linkId);
                long[] traversingRoutes = netState.getLinkTraversingRoutes(linkId);

                double u_e = netState.getLinkCapacityInErlangs(linkId);
                double y_e = netState.getLinkCarriedTrafficInErlangs(linkId);
                double r_e = netState.getLinkCapacityReservedForProtectionInErlangs(linkId);
                double rho_e = (y_e + r_e) / u_e;
                if (Double.isNaN(rho_e)) rho_e = 0;
                
                linkData[i][0] = linkId;
                linkData[i][1] = netState.getLinkOriginNode(linkId);
                linkData[i][2] = netState.getLinkDestinationNode(linkId);
                linkData[i][3] = u_e;
                linkData[i][4] = y_e;
                linkData[i][5] = r_e;
                linkData[i][6] = rho_e;
                linkData[i][7] = netState.getLinkLengthInKm(linkId);
                linkData[i][8] = traversingRoutes.length + (traversingRoutes.length > 0 ? " (" + LongUtils.join(traversingRoutes, ",")  + ")" : "");
                linkData[i][9] = DoubleUtils.isEqualWithinRelativeTolerance(max_rho_e, rho_e, PRECISIONFACTOR);
                linkData[i][10] = srgIds.length > 0 ? LongUtils.join(srgIds, ", ") : "none";
                linkData[i][11] = StringUtils.mapToString(netState.getLinkSpecificAttributes(linkId), "=", ", ");
                i++;
            }

            netPlanTables[1].setEnabled(true);
            ((DefaultTableModel) netPlanTables[1].getModel()).setDataVector(linkData, netPlanTablesHeader[1]);
            netPlanTables[1].getColumnModel().getColumn(netPlanTables[1].convertColumnIndexToView(6)).setCellRenderer(new CellRenderers.LinkUtilizationCellRenderer(true));
        }

        if (D > 0)
        {
            int[] bifurcationDegree = metrics.getDemandBifurcationDegreeVector();
            for (int demandId = 0; demandId < D; demandId++)
            {
                long[] routeIds_thisDemand = netState.getDemandRoutes(demandId);

                demandData[demandId][0] = demandId;
                demandData[demandId][1] = demandTable[demandId][0];
                demandData[demandId][2] = demandTable[demandId][1];
                demandData[demandId][3] = h_d[demandId];
                demandData[demandId][4] = r_d[demandId];
                demandData[demandId][5] = lostTraffic_d[demandId];
                demandData[demandId][6] = bifurcationDegree[demandId] > 1 ? String.format("Yes (%d)", bifurcationDegree[demandId]) : "No";
                demandData[demandId][7] = routeIds_thisDemand.length + (routeIds_thisDemand.length > 0 ? " (" + LongUtils.join(routeIds_thisDemand, ",")  + ")" : "");
                demandData[demandId][8] = StringUtils.mapToString(netPlan.getDemandSpecificAttributes(demandId), "=", ", ");
            }

            netPlanTables[2].setEnabled(true);
            ((DefaultTableModel) netPlanTables[2].getModel()).setDataVector(demandData, netPlanTablesHeader[2]);
            netPlanTables[2].getColumnModel().getColumn(netPlanTables[2].convertColumnIndexToView(5)).setCellRenderer(new CellRenderers.LostTrafficCellRenderer(3, true));
        }

        if (R > 0)
        {
            int i = 0;
            
            for (long routeId : routeIds)
            {
                int demandId = netState.getRouteDemand(routeId);

                long[] plannedSequenceOfLinks = netState.getRouteSequenceOfLinks(routeId);
                int[] plannedSequenceOfNodes = netState.getRouteSequenceOfNodes(routeId);

                double maxUtilization = 0;
                for(long linkId : plannedSequenceOfLinks)
                {
                    double u_e = netState.getLinkCapacityInErlangs(linkId);
                    double y_e = netState.getLinkCarriedTrafficInErlangs(linkId);
                    double r_e = netState.getLinkCapacityReservedForProtectionInErlangs(linkId);
                    double rho_e = (y_e + r_e) / u_e;
                    
                    maxUtilization = Math.max(maxUtilization, rho_e);
                }

                routeData[i][0] = routeId;
                routeData[i][1] = demandId;
                routeData[i][2] = demandTable[demandId][0];
                routeData[i][3] = demandTable[demandId][1];
                routeData[i][4] = h_d[demandId];
                routeData[i][5] = netState.getRouteCarriedTrafficInErlangs(routeId);
                routeData[i][6] = LongUtils.join(plannedSequenceOfLinks, " => ");
                routeData[i][7] = IntUtils.join(plannedSequenceOfNodes, " => ");
                routeData[i][8] = netState.getRouteLengthInKm(routeId);
                routeData[i][9] = maxUtilization;
                routeData[i][10] = LongUtils.join(netState.getRouteBackupSegmentList(routeId), ", ");
                routeData[i][11] = StringUtils.mapToString(netState.getRouteSpecificAttributes(routeId), "=", ", ");
                i++;
            }

            netPlanTables[3].setEnabled(true);
            ((DefaultTableModel) netPlanTables[3].getModel()).setDataVector(routeData, netPlanTablesHeader[3]);
            netPlanTables[3].getColumnModel().getColumn(netPlanTables[3].convertColumnIndexToView(9)).setCellRenderer(new CellRenderers.LinkUtilizationCellRenderer(true));
        }

        if (S > 0)
        {
            int i = 0;
            
            for (long segmentId : segmentIds)
            {
                long[] sequenceOfLinks = netState.getProtectionSegmentSequenceOfLinks(segmentId);
                int[] sequenceOfNodes = netState.getProtectionSegmentSequenceOfNodes(segmentId);
                long[] routeIds_thisSegment = netState.getProtectionSegmentRoutes(segmentId);
                int numRoutes = routeIds_thisSegment.length;

                segmentData[i][0] = segmentId;
                segmentData[i][1] = netState.getProtectionSegmentOriginNode(segmentId);
                segmentData[i][2] = netState.getProtectionSegmentDestinationNode(segmentId);
                segmentData[i][3] = netState.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
                segmentData[i][4] = LongUtils.join(sequenceOfLinks, " => ");
                segmentData[i][5] = IntUtils.join(sequenceOfNodes, " => ");
                segmentData[i][6] = netState.getProtectionSegmentLengthInKm(segmentId);
                segmentData[i][7] = numRoutes > 1 ? "Shared" : (numRoutes == 0 ? "Not used" : "Dedicated");
                segmentData[i][8] = numRoutes + (routeIds_thisSegment.length > 0 ? " (" + LongUtils.join(routeIds_thisSegment, ",")  + ")" : "");
                segmentData[i][9] = StringUtils.mapToString(netState.getProtectionSegmentSpecificAttributes(segmentId), "=", ", ");
                i++;
            }

            netPlanTables[4].setEnabled(true);
            ((DefaultTableModel) netPlanTables[4].getModel()).setDataVector(segmentData, netPlanTablesHeader[4]);
        }

        if (numSRGs > 0)
        {
            long[] srgIds = netState.getSRGIds();
            int i = 0;
            
            for (long srgId : srgIds)
            {
                long[] routeIds_thisSRG = netState.getSRGRoutes(srgId);
                int numRoutes = routeIds_thisSRG.length;
                
                srgData[i][0] = srgId;
                srgData[i][1] = netState.getSRGMeanTimeToFailInHours(srgId);
                srgData[i][2] = netState.getSRGMeanTimeToRepairInHours(srgId);
                srgData[i][3] = IntUtils.join(netState.getSRGNodes(srgId), ", ");
                srgData[i][4] = LongUtils.join(netState.getSRGLinks(srgId), ", ");
                srgData[i][5] = numRoutes + (numRoutes > 0 ? " (" + LongUtils.join(routeIds_thisSRG, ",")  + ")" : "");
                srgData[i][6] = StringUtils.mapToString(netState.getSRGSpecificAttributes(srgId), "=", ", ");
                i++;
            }

            netPlanTables[5].setEnabled(true);
            ((DefaultTableModel) netPlanTables[5].getModel()).setDataVector(srgData, netPlanTablesHeader[5]);
        }

        int attribId = 0;
        for(Map.Entry<String, String> entry : networkAttributes.entrySet())
        {
            networkData[attribId][0] = entry.getKey();
            networkData[attribId][1] = entry.getValue();
        }
        
        networkName.setText(netPlan.getNetworkName());
        networkDescription.setText(netPlan.getNetworkDescription());
	networkDescription.setCaretPosition(0);

        netPlanTables[6].setEnabled(true);
        ((DefaultTableModel) netPlanTables[6].getModel()).setDataVector(networkData, netPlanTablesHeader[6]);

        for (FixedColumnDecorator decorator : decorators)
        {
            List<JTable> list = new LinkedList<JTable>();
            list.add(decorator.getFixedTable());
            list.add(decorator.getMainTable());
            
            for(JTable table : list)
            {
                table.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer(true));
                table.setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer(true));
                table.setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer(true));
            }
        }

        double binaryRate = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));
        double erlangsToBpsFactor = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));

        int[][] linkTable = newNetPlan.getLinkTable();
        double[] u_e = newNetPlan.getLinkCapacityInErlangsVector();
        double[] y_e = newNetPlan.getLinkCarriedTrafficInErlangsVector();
        double U_e = DoubleUtils.sum(u_e);
        double max_rho_e_noProtection = U_e == 0 ? 0 : DoubleUtils.maxValue(DoubleUtils.divideNonSingular(y_e, newNetPlan.getLinkCapacityNotReservedForProtectionInErlangsVector()));
        double u_e_avg = DoubleUtils.average(u_e);
        double[] l_e = newNetPlan.getLinkLengthInKmVector();
        double H_d = DoubleUtils.sum(h_d);
        double R_d = DoubleUtils.sum(r_d);
        GraphTheoryMetrics metrics_hops = new GraphTheoryMetrics(linkTable, N, null);
        GraphTheoryMetrics metrics_km = new GraphTheoryMetrics(linkTable, N, l_e);
        GraphTheoryMetrics metrics_sec = new GraphTheoryMetrics(linkTable, N, newNetPlan.getLinkPropagationDelayInSecondsVector());
        int[] nodeOutDegree = metrics_hops.getOutNodeDegree();
        int[] maxMinOutDegree = nodeOutDegree.length == 0 ? new int[] {0, 0} : IntUtils.maxMinValues(nodeOutDegree);
        double avgOutDegree = metrics_hops.getAverageOutNodeDegree();
        int networkDiameter_hops = (int) metrics_hops.getDiameter();
        double networkDiameter_km = metrics_km.getDiameter();
        double networkDiameter_ms = 1000 * metrics_sec.getDiameter();
        double offeredTrafficInErlangsPerNodePair = metrics.getNodePairAverageOfferedTrafficInErlangs();

        boolean isRoutingBifurcated = metrics.isRoutingBifurcated();

        double averageRouteLength_hops = 0;
        double averageRouteLength_km = 0;
        double averageRouteLength_ms = 0;

        if (R > 0 && R_d > 0)
        {
            averageRouteLength_hops = metrics.getRouteAverageLength(null);
            averageRouteLength_km = metrics.getRouteAverageLength(l_e);
            averageRouteLength_ms = 1000 * metrics.getRouteAverageLength(newNetPlan.getLinkPropagationDelayInSecondsVector());
        }

        double[] u_e_reservedForProtection = newNetPlan.getLinkCapacityReservedForProtectionInErlangsVector();
        double U_e_reservedForProtection = DoubleUtils.sum(u_e_reservedForProtection);
        double u_e_reservedForProtection_avg = E == 0 ? 0 : U_e_reservedForProtection / E;
        double percentageReserved = U_e == 0 ? 0 : 100 * U_e_reservedForProtection / U_e;
        
        double blockedTraffic = metrics.getBlockedTrafficPercentage();
        double[] protectionDegree = metrics.getTrafficProtectionDegree();
        double percentageUnprotected = protectionDegree[0];
        double percentageDedicated = protectionDegree[1];
        double percentageShared = protectionDegree[2];
        
        String srgModel = metrics.getSRGModel();
        
        double[] aux_srgDisjointness = metrics.getSRGDisjointnessPercentage();
        double percentageRouteSRGDisjointness_withEndNodes = aux_srgDisjointness[0];
        double percentageRouteSRGDisjointness_withoutEndNodes = aux_srgDisjointness[1];

        Map<String, Object> topologyData = new LinkedHashMap<String, Object>();
        topologyData.put("Number of nodes", N);
        topologyData.put("Number of links", E);
        topologyData.put("Node out-degree (max, min, avg)", String.format("%d, %d, %.3f", maxMinOutDegree[0], maxMinOutDegree[1], avgOutDegree));
        topologyData.put("All links are bidirectional (yes/no)", GraphUtils.isBidirectional(newNetPlan) ? "Yes" : "No");
        topologyData.put("Network diameter (hops, km, ms)", String.format("%d, %.3f, %.3g", networkDiameter_hops, networkDiameter_km, networkDiameter_ms));
        topologyData.put("Capacity installed: total (Erlangs, bps)", String.format("%.3f, %.3g", U_e, U_e * erlangsToBpsFactor));
        topologyData.put("Capacity installed: average per link (Erlangs, bps)", String.format("%.3f, %.3g", u_e_avg, u_e_avg * erlangsToBpsFactor));
        networkSummaryTables[0].setData(topologyData);
        
        networkSummaryTables[0].setToolTipText(0, 0, "Indicates the number of defined nodes");
        networkSummaryTables[0].setToolTipText(1, 0, "Indicates the number of defined links");
        networkSummaryTables[0].setToolTipText(2, 0, "Indicates the maximum/minimum/average value for the out-degree, that is, the number of outgoing links per node");
        networkSummaryTables[0].setToolTipText(3, 0, "Indicates whether all links are bidirectional, that is, if there are the same number of links between each node pair in both directions (irrespective of the respective capacities)");
        networkSummaryTables[0].setToolTipText(4, 0, "Indicates the network diameter, that is, the length of the largest shortest-path");
        networkSummaryTables[0].setToolTipText(5, 0, "Indicates the total capacity installed in the network");
        networkSummaryTables[0].setToolTipText(6, 0, "Indicates the average capacity installed per link");

        boolean isTrafficSymmetric = GraphUtils.isWeightedBidirectional(demandTable, h_d, N);

        Map<String, Object> trafficData = new LinkedHashMap<String, Object>();
        trafficData.put("Number of demands", D);
        trafficData.put("Offered traffic: total (Erlangs, bps)", String.format("%.3f, %.3g", H_d, H_d * erlangsToBpsFactor));
        trafficData.put("Offered traffic: average per node pair (Erlangs, bps)", String.format("%.3f, %.3g", offeredTrafficInErlangsPerNodePair, offeredTrafficInErlangsPerNodePair * erlangsToBpsFactor));
        trafficData.put("Blocked traffic (%)", String.format("%.3f", blockedTraffic));
        trafficData.put("Symmetric offered traffic?", isTrafficSymmetric ? "Yes" : "No");
        networkSummaryTables[1].setData(trafficData);

        networkSummaryTables[1].setToolTipText(0, 0, "Indicates the number of defined demands");
        networkSummaryTables[1].setToolTipText(1, 0, "Indicates the total offered traffic to the network");
        networkSummaryTables[1].setToolTipText(2, 0, "Indicates the total offered traffic to the network per each node pair");
        networkSummaryTables[1].setToolTipText(3, 0, "Indicates the percentage of blocked traffic from the total offered to the network");
        networkSummaryTables[1].setToolTipText(4, 0, "Indicates whether the offered traffic is symmetric, that is, if there are the same number of demands with the same offered traffic between each node pair in both directions");
        
        boolean hasRoutingLoops = GraphUtils.hasRoutingLoops(newNetPlan, GraphUtils.CheckRoutingCycleType.NO_REPEAT_NODE);

        Map<String, Object> routingData = new LinkedHashMap<String, Object>();
        routingData.put("Number of routes", R);
        routingData.put("Bifurcated routing (yes/no)", isRoutingBifurcated ? "Yes" : "No");
        routingData.put("Network congestion - bottleneck utilization % (w. reserved bw, w.o. reserved bw)", String.format("%.3f, %.3f", max_rho_e, max_rho_e_noProtection));
        routingData.put("Average route length (hops, km, ms)", String.format("%.3f, %.3f, %.3g", averageRouteLength_hops, averageRouteLength_km, averageRouteLength_ms));
        routingData.put("Routing has loops?", hasRoutingLoops ? "Yes" : "No");
        
        networkSummaryTables[2].setData(routingData);

        networkSummaryTables[2].setToolTipText(0, 0, "Indicates the number of defined routes");
        networkSummaryTables[2].setToolTipText(1, 0, "Indicates whether the routing is bifurcated, that is, if at least there are more than one route carrying traffic from any demand");
        networkSummaryTables[2].setToolTipText(2, 0, "Indicates the network congestion, that is, the utilization of the busiest link");
        networkSummaryTables[2].setToolTipText(3, 0, "Indicates the average route length");
        networkSummaryTables[2].setToolTipText(4, 0, "Indicates whether the routing has loops, that is, if at least a route visits a node more than once");

        Map<String, Object> protectionData = new LinkedHashMap<String, Object>();
        protectionData.put("Number of protection segments", S);
        protectionData.put("Average link capacity reserved for protection (%, Erlangs, bps)", String.format("%.3f, %.3f, %.3g", percentageReserved, u_e_reservedForProtection_avg, u_e_reservedForProtection_avg * binaryRate));
        protectionData.put("% of carried traffic unprotected", String.format("%.3f", percentageUnprotected));
        protectionData.put("% of carried traffic complete and dedicated protection", String.format("%.3f", percentageDedicated));
        protectionData.put("% of carried traffic partial and/or shared protection", String.format("%.3f", percentageShared));
        protectionData.put("Number of SRGs", numSRGs);
        protectionData.put("SRG definition characteristic", srgModel);
        protectionData.put("% routes protected with SRG disjoint segments (w. end nodes, w.o. end nodes)", String.format("%.3f, %.3f", percentageRouteSRGDisjointness_withEndNodes, percentageRouteSRGDisjointness_withoutEndNodes));
        networkSummaryTables[3].setData(protectionData);
        
        networkSummaryTables[3].setToolTipText(0, 0, "Indicates the number of defined protection segments");
        networkSummaryTables[3].setToolTipText(1, 0, "Indicates the average reserved bandwidth per protection segment");
        networkSummaryTables[3].setToolTipText(2, 0, "Indicates the percentage of traffic (from the total) which is not protected by any segment");
        networkSummaryTables[3].setToolTipText(3, 0, "Indicates the percentage of traffic (from the total) which is protected by dedicated segments reservating the total traffic of their corresponding routes");
        networkSummaryTables[3].setToolTipText(4, 0, "Indicates the percentage of traffic (from the total) which is neither unprotected or dedicated");
        networkSummaryTables[3].setToolTipText(5, 0, "Indicates the number of defined SRGs");
        networkSummaryTables[3].setToolTipText(6, 0, "Indicates whether SRG definition follows one of the predefined models (per node, per link...), or 'Mixed' otherwise (or 'None' if no SRGs are defined)");
        networkSummaryTables[3].setToolTipText(7, 0, "Indicates the percentage of routes from the total which have all protection segments SRG-disjoint, without taking into account the carried traffic per route");
        
        for(JTable table : networkSummaryTables)
        {
            AdvancedJTable.setVisibleRowCount(table, table.getRowCount());
            AdvancedJTable.setWidthAsPercentages(table, 0.7, 0.3);
        }
        
        pan_topology.getCanvas().updateTopology(newNetPlan);
    }

    @Override
    public Pair<JComponent, String> getNetPlanView()
    {
	JTabbedPane pane = new JTabbedPane();

	netPlanTables = new JTable[GUINetworkDesign.netPlanTablesHeader.length];
	DefaultTableModel[] model = new DefaultTableModel[netPlanTables.length];

	for (int modelId = 0; modelId < model.length; modelId++)
	{
	    Object[][] data = new Object[1][GUINetworkDesign.netPlanTablesHeader[modelId].length];
	    String[] header = GUINetworkDesign.netPlanTablesHeader[modelId];
	    model[modelId] = new ClassAwareTableModel(data, header);
	    netPlanTables[modelId] = new AdvancedJTable(model[modelId]);
	}
        
        decorators = new LinkedList<FixedColumnDecorator>();
                
        networkSummaryTables = new ParamValueTable[4];
        for(int i = 0; i < networkSummaryTables.length; i++)
            networkSummaryTables[i] = new ParamValueTable(new String[] {"Metric", "Value"});
        
        networkName = new JTextField();
        networkDescription = new JTextArea();
        
        GUINetworkDesign.initializeNetworkState(this, pan_topology, pane, null, netPlanTables, model, decorators, GUINetworkDesign.netPlanTablesHeader, networkName, networkDescription, networkSummaryTables, false);

	return Pair.of((JComponent) pane, "View current state");
    }

    @Override
    public void addManuallyEvent() { throw new Net2PlanException("Option not available yet in this simulator"); }

    @Override
    public void showRoute(int routeId)
    {
        long[] activeLinkIds = ((TimeVaryingNetState) simKernel.getNetState()).getLinkIds();
        long[] activeRouteIds = ((TimeVaryingNetState) simKernel.getNetState()).getRouteIds();
        
        long activeRouteId = activeRouteIds[routeId];
        long[] seqLinks = ((TimeVaryingNetState) simKernel.getNetState()).getRouteSequenceOfLinks(activeRouteId);
        int[] zeroBasedSeqLinks = getZeroBasedLinkIds(seqLinks, activeLinkIds);
        showRoute(zeroBasedSeqLinks);
    }
    
    private static int[] getZeroBasedLinkIds(long[] linkIds, long[] activeLinkIds)
    {
        int[] zeroBasedLinkIds = new int[linkIds.length];
        for(int i = 0; i < linkIds.length; i++)
            zeroBasedLinkIds[i] = LongUtils.find(activeLinkIds, linkIds[i], Constants.SearchType.FIRST)[0];            
        
        return zeroBasedLinkIds;
    }

    @Override
    public void showSRG(int srgId)
    {
        long[] activeSRGIds = ((TimeVaryingNetState) simKernel.getNetState()).getSRGIds();
        long activeSRGId = activeSRGIds[srgId];
        
        int[] nodeIds = ((TimeVaryingNetState) simKernel.getNetState()).getSRGNodes(activeSRGId);
        
        long[] activeLinkIds = ((TimeVaryingNetState) simKernel.getNetState()).getLinkIds();
        long[] linkIds = ((TimeVaryingNetState) simKernel.getNetState()).getSRGLinks(activeSRGId);
        int[] zeroBasedSeqLinks = getZeroBasedLinkIds(linkIds, activeLinkIds);
        
	pan_topology.getCanvas().showNodesAndLinks(nodeIds, zeroBasedSeqLinks);
	pan_topology.getCanvas().refresh();
    }

    @Override
    public void showSegment(int segmentId)
    {
        long[] activeLinkIds = ((TimeVaryingNetState) simKernel.getNetState()).getLinkIds();
        long[] activeSegmentIds = ((TimeVaryingNetState) simKernel.getNetState()).getProtectionSegmentIds();
        
        long activeSegmentId = activeSegmentIds[segmentId];
        long[] seqLinks = ((TimeVaryingNetState) simKernel.getNetState()).getProtectionSegmentSequenceOfLinks(activeSegmentId);
        int[] zeroBasedSeqLinks = getZeroBasedLinkIds(seqLinks, activeLinkIds);
        showRoute(zeroBasedSeqLinks);
    }

    @Override
    protected void showRoute(int[] linkIds)
    {
        pan_topology.getCanvas().showRoute(linkIds);
        pan_topology.getCanvas().refresh();
    }
}
