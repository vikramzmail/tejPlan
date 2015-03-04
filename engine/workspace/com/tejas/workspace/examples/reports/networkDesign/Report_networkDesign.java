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

package com.tejas.workspace.examples.reports.networkDesign;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphTheoryMetrics;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class Report_networkDesign implements IReport
{
    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int D = netPlan.getNumberOfDemands();
	int R = netPlan.getNumberOfRoutes();
	int S = netPlan.getNumberOfProtectionSegments();
        int numSRGs = netPlan.getNumberOfSRGs();

	double averageNodeDegree = N == 0 ? 0 : (double) E / (double) N;
	int[][] linkTable = netPlan.getLinkTable();
	DoubleMatrix1D d_e = DoubleFactory1D.dense.make(netPlan.getLinkLengthInKmVector());
	DoubleMatrix1D u_e = DoubleFactory1D.dense.make(netPlan.getLinkCapacityInErlangsVector());
	double averageLinkLength = 0;
	double maxCapacity = 0;
	double minCapacity = 0;
	double averageCapacity = 0;
	double totalCapacity = 0;
	int networkDiameter_hops = 0;
	double networkDiameter_km = 0;
	double averageShortestPath_hops = 0;
	double averageShortestPath_km = 0;
	double capacityModule = 0;

	boolean isConnected = false;
	boolean isBidirectional = false;
	boolean isWeightedBidirectional = false;
	boolean isSimple = false;

	double Y_e = DoubleUtils.sum(netPlan.getLinkCarriedTrafficInErlangsVector());

	if (E > 0)
	{
	    isConnected = GraphUtils.isConnected(netPlan);
	    isBidirectional = GraphUtils.isBidirectional(netPlan);
	    isWeightedBidirectional = GraphUtils.isWeightedBidirectional(netPlan, u_e.toArray());
	    isSimple = GraphUtils.isSimple(netPlan);

	    averageLinkLength = d_e.zSum() / d_e.size();
	    maxCapacity = u_e.getMaxLocation()[0];
	    minCapacity = u_e.getMinLocation()[0];
	    totalCapacity = u_e.zSum();
	    averageCapacity = totalCapacity / u_e.size();

	    GraphTheoryMetrics metrics_hops = new GraphTheoryMetrics(linkTable, N, null);
	    GraphTheoryMetrics metrics_km = new GraphTheoryMetrics(linkTable, N, d_e.toArray());

	    networkDiameter_hops = (int) metrics_hops.getDiameter();
	    networkDiameter_km = metrics_km.getDiameter();

	    averageShortestPath_hops = metrics_hops.getAverageShortestPathDistance();
	    averageShortestPath_km = metrics_km.getAverageShortestPathDistance();

	    capacityModule = DoubleUtils.gcd(u_e.toArray());

	    if (capacityModule < 1E-3) capacityModule = 0;
	}

	double binaryRate = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));

	String html;
        try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/examples/reports/networkDesign/main.html").toURI().toURL()); }
        catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }

	// Put topology metrics
	html = html.replaceFirst("#topologyMetrics_N#", String.format("%d", N));
	html = html.replaceFirst("#topologyMetrics_E#", String.format("%d", E));
	html = html.replaceFirst("#topologyMetrics_averageNodeDegree#", String.format("%.3f", averageNodeDegree));
	html = html.replaceFirst("#topologyMetrics_averageLinkLength#", String.format("%.3f", averageLinkLength));
	html = html.replaceFirst("#topologyMetrics_networkDiameter_hops#", String.format("%d", networkDiameter_hops));
	html = html.replaceFirst("#topologyMetrics_networkDiameter_km#", String.format("%.3f", networkDiameter_km));
	html = html.replaceFirst("#topologyMetrics_averageShortestPath_hops#", String.format("%.3f", averageShortestPath_hops));
	html = html.replaceFirst("#topologyMetrics_averageShortestPath_km#", String.format("%.3f", averageShortestPath_km));
	html = html.replaceFirst("#topologyMetrics_isConnected#", String.format("%s", isConnected ? "Yes" : "No"));
	html = html.replaceFirst("#topologyMetrics_isBidirectional#", String.format("%s", isBidirectional ? "Yes" : "No"));
	html = html.replaceFirst("#topologyMetrics_isWeightedBidirectional#", String.format("%s", isWeightedBidirectional ? "Yes" : "No"));
	html = html.replaceFirst("#topologyMetrics_isSimple#", String.format("%s", isSimple ? "Yes" : "No"));

	// Put capacity metrics
	html = html.replaceFirst("#capacityMetrics_maxCapacity#", String.format("%.3f (%.3f)", maxCapacity, maxCapacity * binaryRate));
	html = html.replaceFirst("#capacityMetrics_minCapacity#", String.format("%.3f (%.3f)", minCapacity, minCapacity * binaryRate));
	html = html.replaceFirst("#capacityMetrics_averageCapacity#", String.format("%.3f (%.3f)", averageCapacity, averageCapacity * binaryRate));
	html = html.replaceFirst("#capacityMetrics_totalCapacity#", String.format("%.3f (%.3f)", totalCapacity, totalCapacity * binaryRate));
	html = html.replaceFirst("#capacityMetrics_capacityModule#", String.format("%.3f (%.3f)", capacityModule, capacityModule * binaryRate));

	DoubleMatrix1D h_d = DoubleFactory1D.dense.make(netPlan.getDemandOfferedTrafficInErlangsVector());
	DoubleMatrix1D r_d = DoubleFactory1D.dense.make(netPlan.getDemandCarriedTrafficInErlangsVector());
	double H_d = h_d.zSum();
	double R_d = r_d.zSum();
	html = html.replaceFirst("#trafficMetrics_totalOfferedTraffic#", String.format("%.3f (%.3f)", H_d, H_d * binaryRate));
	html = html.replaceFirst("#trafficMetrics_totalCarriedTraffic#", String.format("%.3f (%.3f)", R_d, R_d * binaryRate));
	html = html.replaceFirst("#trafficMetrics_averageOfferedTraffic#", String.format("%.3f (%.3f)", D == 0 ? 0 : H_d / D, D == 0 ? 0 : H_d / D * binaryRate));
	html = html.replaceFirst("#trafficMetrics_averageCarriedTraffic#", String.format("%.3f (%.3f)", D == 0 ? 0 : R_d / D, D == 0 ? 0 : R_d / D * binaryRate));
	html = html.replaceFirst("#trafficMetrics_blockedTraffic#", String.format("%.3f", H_d == 0 ? 0 : Math.max(100 * (1 - R_d / H_d), 0)));
	html = html.replaceFirst("#trafficMetrics_numHops#", String.format("%.3f", R_d == 0 ? 0 : Y_e / R_d));

	double[] u_e_planned = netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector();
	double[] y_e_planned = netPlan.getLinkCarriedTrafficInErlangsVector();
	double[] rho_e_planned = new double[E];

	for (int linkId = 0; linkId < E; linkId++)
	{
	    rho_e_planned[linkId] = u_e_planned[linkId] == 0 ? 0 : y_e_planned[linkId] / u_e_planned[linkId];
	}
	double[][] nodeXYPositionTable = netPlan.getNodeXYPositionTable();
	String[] nodeName = netPlan.getNodeNameVector();

	// Per-node information
	StringBuilder nodeInformationTable = new StringBuilder();

	if (N > 0)
	{
	    nodeInformationTable.append("<table border='1'>");
	    nodeInformationTable.append("<tr><th><b><a href='#detailedPerNodeDescription_id'>Node id</a></b></th><th><b><a href='#detailedPerNodeDescription_name'>Node name</a></b></th><th><b><a href='#detailedPerNodeDescription_xyCoord'>X-Coord, Y-Coord</a></b></th><th><b><a href='#detailedPerNodeDescription_attributes'>Attributes</a></b></th></tr>");
	    for (int nodeId = 0; nodeId < N; nodeId++)
	    {
		String nodeAttributeString = StringUtils.mapToString(netPlan.getNodeSpecificAttributes(nodeId), "=", ", ");
		nodeInformationTable.append(String.format("<tr><td>%d</td><td>%s</td><td>%.3f, %.3f</td><td>%s</td></tr>", nodeId, nodeName[nodeId], nodeXYPositionTable[nodeId][0], nodeXYPositionTable[nodeId][1], nodeAttributeString));
	    }
	    nodeInformationTable.append("</table>");
	}
	else
	{
	    nodeInformationTable.append("No node information available");
	}

	html = html.replaceFirst("#nodeInformationTable#", nodeInformationTable.toString());

	// Per-link information
	StringBuilder linkInformationTable = new StringBuilder();

	if (E > 0)
	{
	    linkInformationTable.append("<table border='1'>" + "<tr><th><b><a href='#detailedPerLinkDescription_id'>Link id</a></b></th><th><b><a href='#detailedPerLinkDescription_start'>Start</a></b></th><th><b><a href='#detailedPerLinkDescription_end'>End</a></b></th><th><b><a href='#detailedPerLinkDescription_linkLength'>Link length (km)</a></b></th><th><b><a href='#detailedPerLinkDescription_attributes'>Attributes</a></b></th></tr>");
	    for (int linkId = 0; linkId < E; linkId++)
	    {
		String linkAttributeString = StringUtils.mapToString(netPlan.getLinkSpecificAttributes(linkId), "=", ", ");
		linkInformationTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%s</td></tr>", linkId, linkTable[linkId][0], nodeName[linkTable[linkId][0]], linkTable[linkId][1], nodeName[linkTable[linkId][1]], d_e.getQuick(linkId), linkAttributeString));
	    }
	    linkInformationTable.append("</table>");
	}
	else
	{
	    linkInformationTable.append("No link information available");
	}

	html = html.replaceFirst("#linkInformationTable#", linkInformationTable.toString());

	StringBuilder demandInformationTable = new StringBuilder();
	if (D > 0)
	{
	    double[] r_d_planned = netPlan.getDemandCarriedTrafficInErlangsVector();
	    demandInformationTable.append("<table border='1'><tr><th><b>Id</b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Offered traffic (E)</b></th><th><b>Carried traffic (E)</b></th><th><b>% Blocked traffic</b></th><th><b>Bifurcated</b></th><th><b>Attributes</b></th></tr>");

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		int[] traversingRoutes = netPlan.getDemandRoutes(demandId);

		int numberOfRoutesCarryingTraffic_planned = 0;
		boolean isBifurcated_planned = false;
		for (int routeId : traversingRoutes)
		{
		    if (netPlan.getRouteCarriedTrafficInErlangs(routeId) > 0)
		    {
			numberOfRoutesCarryingTraffic_planned++;
		    }

		    if (numberOfRoutesCarryingTraffic_planned > 1)
		    {
			isBifurcated_planned = true;
			break;
		    }
		}

		int ingressNodeId = netPlan.getDemandIngressNode(demandId);
		int egressNodeId = netPlan.getDemandEgressNode(demandId);
		double carriedTraffic = r_d_planned[demandId];

		String demandAttributeString = StringUtils.mapToString(netPlan.getDemandSpecificAttributes(demandId), "=", ", ");
		demandInformationTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%.3f</td><td>%.3f</td><td>%s</td><td>%s</td></tr>", demandId, ingressNodeId, nodeName[ingressNodeId], egressNodeId, nodeName[egressNodeId], h_d.get(demandId), carriedTraffic, h_d.get(demandId) == 0 ? 0 : 100 * (1 - r_d_planned[demandId] / h_d.get(demandId)), isBifurcated_planned ? "Yes" : "No", demandAttributeString));
	    }
	    demandInformationTable.append("</table>");
	}
	else
	{
	    demandInformationTable.append("No traffic demands information available");
	}

	html = html.replaceFirst("#demandInformationTable#", demandInformationTable.toString());

	StringBuilder routeInformationTable = new StringBuilder();
	if (R > 0)
	{
	    routeInformationTable.append("<table border='1'><tr><th><b>Id</b></th><th><b>Demand</b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Carried traffic (E)</b></th><th><b>Sequence of links</b></th><th><b>Sequence of nodes</b></th><th><b>Bottleneck utilization</b></th><th><b>Backup segments</b></th><th><b>Attributes</b></th></tr>");
	    for (int routeId = 0; routeId < R; routeId++)
	    {
		int demandId = netPlan.getRouteDemand(routeId);

		int ingressNodeId = netPlan.getDemandIngressNode(demandId);
		int egressNodeId = netPlan.getDemandEgressNode(demandId);
		double carriedTraffic = netPlan.getRouteCarriedTrafficInErlangs(routeId);

		int[] sequenceOfLinks = netPlan.getRouteSequenceOfLinks(routeId);
		int[] sequenceOfNodes = netPlan.getRouteSequenceOfNodes(routeId);
		int[] backupSegments = netPlan.getRouteBackupSegmentList(routeId);

		double bottleneckUtilization = Double.NEGATIVE_INFINITY;
		for(int linkId : sequenceOfLinks)
		{
		    bottleneckUtilization = Math.max(bottleneckUtilization, rho_e_planned[linkId]);
		}

		String routeAttributeString = StringUtils.mapToString(netPlan.getRouteSpecificAttributes(routeId), "=", ", ");
		routeInformationTable.append(String.format("<tr><td>%d</td><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%s</td><td>%s</td><td>%.3f</td><td>%s</td><td>%s</td></tr>", routeId, demandId, ingressNodeId, nodeName[ingressNodeId], egressNodeId, nodeName[egressNodeId], carriedTraffic, IntUtils.join(sequenceOfLinks, " => "), IntUtils.join(sequenceOfNodes, " => "), bottleneckUtilization, IntUtils.join(backupSegments, ", "), routeAttributeString));
	    }
	    routeInformationTable.append("</table>");
	}
	else
	{
	    routeInformationTable.append("No routing information available");
	}

	html = html.replaceFirst("#routeInformationTable#", routeInformationTable.toString());

	// Per-protection segment information
	StringBuilder segmentInformationTable = new StringBuilder();

	if (S > 0)
	{
	    segmentInformationTable.append("<table border='1'><tr><th><b>Id</b></th><th><b>Origin node</b></th><th><b>Destination node</b></th><th><b>Reserved bandwidth (E)</b></th><th><b>Sequence of links</b></th><th><b>Sequence of nodes</b></th><th><b>Length (km)</b></th><th><b>Dedicated/Shared</b></th><th><b>Attributes</b></th></tr>");
	    for (int segmentId = 0; segmentId < S; segmentId++)
	    {
		double segmentLength = 0;
		int[] sequenceOfLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
		int[] sequenceOfNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
		for (int linkId : sequenceOfLinks)
		{
		    segmentLength += netPlan.getLinkLengthInKm(linkId);
		}

		int numberOfRoutes = 0;
		for (int routeId = 0; routeId < R; routeId++)
		{
		    int[] backupSegments = netPlan.getRouteBackupSegmentList(routeId);
		    Arrays.sort(backupSegments);
		    if (Arrays.binarySearch(backupSegments, segmentId) >= 0)
		    {
			numberOfRoutes++;
		    }
		}

		int originNodeId = netPlan.getProtectionSegmentOriginNode(segmentId);
		int destinationNodeId = netPlan.getProtectionSegmentDestinationNode(segmentId);

		String segmentAttributeString = StringUtils.mapToString(netPlan.getProtectionSegmentSpecificAttributes(segmentId), "=", ", ");
		segmentInformationTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%s</td><td>%s</td><td>%.3f</td><td>%s</td><td>%s</td></tr>", segmentId, originNodeId, nodeName[originNodeId], destinationNodeId, nodeName[destinationNodeId], netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId), IntUtils.join(sequenceOfLinks, " => "), IntUtils.join(sequenceOfNodes, " => "), segmentLength, numberOfRoutes > 1 ? String.format("Shared (%d routes)", numberOfRoutes) : (numberOfRoutes == 0 ? "Not used" : "Dedicated"), segmentAttributeString));
	    }
	    segmentInformationTable.append("</table>");
	}
	else
	{
	    segmentInformationTable.append("No protection segments information available");
	}

	html = html.replaceFirst("#segmentInformationTable#", segmentInformationTable.toString());

	// Per-SRG information
	StringBuilder srgInformationTable = new StringBuilder();

	if (numSRGs > 0)
	{
	    srgInformationTable.append("<table border='1'><tr><th><b>Id</b></th><th><b>Mean time to fail (hours)</b></th><th><b>Mean time to repair (hours)</b></th><th><b>Nodes</b></th><th><b>Links</b></th><th><b># Affected routes</b></th><th><b>Attributes</b></th></tr>");
            
	    for (int srgId = 0; srgId < numSRGs; srgId++)
	    {
                double mttf = netPlan.getSRGMeanTimeToFailInHours(srgId);
                double mttr = netPlan.getSRGMeanTimeToRepairInHours(srgId);
                int[] nodeIds = netPlan.getSRGNodes(srgId);
                int[] linkIds = netPlan.getSRGLinks(srgId);
                int[] routeIds = netPlan.getSRGRoutes(srgId);
                int numRoutes = routeIds.length;
                String routesString = numRoutes + (numRoutes > 0 ? " (" + IntUtils.join(routeIds, ",")  + ")" : "");
                Map<String, String> srgAttributes = netPlan.getSRGSpecificAttributes(srgId);
		String srgAttributeString = srgAttributes.isEmpty() ? "none" : StringUtils.mapToString(srgAttributes, "=", ", ");
                
		srgInformationTable.append(String.format("<tr><td>%d</td><td>%f</td><td>%f</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", srgId, mttf, mttr, IntUtils.join(nodeIds, ", "), IntUtils.join(linkIds, ", "), routesString, srgAttributeString));
	    }
	    srgInformationTable.append("</table>");
	}
	else
	{
	    srgInformationTable.append("No SRGs information available");
	}

	html = html.replaceFirst("#srgInformationTable#", srgInformationTable.toString());

        return html;
    }

    @Override
    public String getDescription() { return "This report shows basic information about the network design"; }

    @Override
    public String getTitle() { return "Network design report"; }

    @Override
    public List<Triple<String, String, String>> getParameters() { return new ArrayList<Triple<String, String, String>>(); }
}