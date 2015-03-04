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

package com.tejas.workspace.examples.reports.blocking;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tint.IntFactory1D;

import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.NetworkPerformanceMetrics;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This report analyzes a network design in terms of blocking probability.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class Report_blocking implements IReport
{

    @Override
    public String getDescription()
    {
	return "This report analyzes a network design in terms of blocking probability";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	return new ArrayList<Triple<String, String, String>>();
    }

    @Override
    public String getTitle()
    {
	return "Blocking report";
    }

    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
	String html;

	if (netPlan.hasRoutes())
	{
	    netPlan.checkValidity(net2planParameters, true, true);

	    int E = netPlan.getNumberOfLinks();
	    int D = netPlan.getNumberOfDemands();
	    int R = netPlan.getNumberOfRoutes();

	    double[] r_d = netPlan.getDemandCarriedTrafficInErlangsVector();
	    int[] u_e = DoubleUtils.toIntArray(netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector());
	    double[] y_e = netPlan.getLinkCarriedTrafficInErlangsVector();
	    double[] h_p = netPlan.getRouteCarriedTrafficInErlangsVector();

	    int[] s_d = new int[D];
	    Arrays.fill(s_d, 1);

	    // Link blocking probabilities
	    double[][] B_ep = new double[E][R];
	    double[] B_e_erlangB = new double[E];

	    // Path blocking probabilities
	    double[] B_p = new double[R];
	    double[] B_p_erlangB = new double[R];

	    // Average network blocking probability
	    double B = 0;
	    double B_erlangB = 0;

	    int[] s_p = new int[R];

	    int[][] P_d = new int[D][];

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		P_d[demandId] = netPlan.getDemandRoutes(demandId);

		try { s_d[demandId] = Integer.parseInt(netPlan.getDemandAttribute(demandId, "connectionSize")); }
		catch(Exception ex) { s_d[demandId] = 1; }

		for (int routeId : P_d[demandId])
		{
		    s_p[routeId] = s_d[demandId];
		}
	    }

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int[] routeIds = netPlan.getLinkTraversingRoutes(linkId);
		if (routeIds.length == 0) continue;

		double[] h_p_thisLink = DoubleUtils.select(h_p, routeIds);
		int[] s_p_thisLink = IntUtils.select(s_p, routeIds);

		double[] B_ep_thisLink = NetworkPerformanceMetrics.kaufmanRobertsRecursion(u_e[linkId], h_p_thisLink, s_p_thisLink);
		B_e_erlangB[linkId] = NetworkPerformanceMetrics.erlangBLossProbability(u_e[linkId], DoubleUtils.sum(h_p_thisLink));
		for (int i = 0; i < routeIds.length; i++)
		{
		    B_ep[linkId][routeIds[i]] = B_ep_thisLink[i];
		}

	    }

	    for (int routeId = 0; routeId < R; routeId++)
	    {
		B_p[routeId] = 1;
		B_p_erlangB[routeId] = 1;
		int[] sequenceOfLinks = netPlan.getRouteSequenceOfLinks(routeId);
		for (int linkId : sequenceOfLinks)
		{
		    B_p[routeId] *= (1 - B_ep[linkId][routeId]);
		    B_p_erlangB[routeId] *= (1 - B_e_erlangB[linkId]);
		}

		B_p[routeId] = 1 - B_p[routeId];
		B_p_erlangB[routeId] = 1 - B_p_erlangB[routeId];
	    }

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		for (int routeId : P_d[demandId])
		{
		    B += h_p[demandId] * B_p[routeId];
		    B_erlangB += h_p[demandId] * B_p_erlangB[routeId];
		}
	    }

	    B /= DoubleFactory1D.dense.make(r_d).zSum();
	    B_erlangB /= DoubleFactory1D.dense.make(r_d).zSum();

	    int min_s_d = IntFactory1D.dense.make(s_d).getMinLocation()[0];
	    int max_s_d = IntFactory1D.dense.make(s_d).getMaxLocation()[0];

	    // Generate link blocking probabilities table
	    final StringBuilder linkBlockingProbabilitiesTable = new StringBuilder();

	    double[][] blockingProbabilityPerLinkPerConnectionSize = new double[E][max_s_d];

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int[] routes = netPlan.getLinkTraversingRoutes(linkId);

		for (int routeId : routes)
		{
		    blockingProbabilityPerLinkPerConnectionSize[linkId][s_p[routeId] - 1] = B_ep[linkId][routeId];
		}
	    }

	    linkBlockingProbabilitiesTable.append("<table border='1'>");
	    linkBlockingProbabilitiesTable.append(String.format("<tr><th rowspan='2'>Link id</th><th rowspan='2'>Origin node</th><th rowspan='2'>Destination node</th><th rowspan='2'><i>y<sub>e</sub></i> (Erlangs)</th><th rowspan='2'><i>u<sub>e</sub></i> (Erlangs)</th><th colspan='%d'><i>B<sub>e</sub></i></th><th rowspan='2'><i>B<sub>e</sub></i> (Erlang-B)</th></tr>", max_s_d));
	    linkBlockingProbabilitiesTable.append("<tr>");
	    for (int connectionSize = 1; connectionSize <= max_s_d; connectionSize++)
	    {
		linkBlockingProbabilitiesTable.append(String.format("<td><i>s<sub>d</sub>=%d</i></td>", connectionSize));
	    }
	    linkBlockingProbabilitiesTable.append("</tr>");

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int originNodeId = netPlan.getLinkOriginNode(linkId);
		int destinationNodeId = netPlan.getLinkDestinationNode(linkId);

		String originNodeName = netPlan.getNodeName(originNodeId);
		String destinationNodeName = netPlan.getNodeName(destinationNodeId);

		linkBlockingProbabilitiesTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3f</td><td>%d</td>", linkId, originNodeId, originNodeName, destinationNodeId, destinationNodeName, y_e[linkId], u_e[linkId]));
		for (int connectionSizeId = 0; connectionSizeId < max_s_d; connectionSizeId++)
		{
		    if (blockingProbabilityPerLinkPerConnectionSize[linkId][connectionSizeId] > 0)
		    {
			linkBlockingProbabilitiesTable.append(String.format("<td>%.3f</td>", blockingProbabilityPerLinkPerConnectionSize[linkId][connectionSizeId]));
		    }
		    else
		    {
			linkBlockingProbabilitiesTable.append("<td>-</td>");
		    }
		}

		linkBlockingProbabilitiesTable.append(String.format("<td>%.3f</td></tr>", B_e_erlangB[linkId]));
	    }

	    linkBlockingProbabilitiesTable.append("</table>");

	    // Generate path blocking probabilities table
	    final StringBuilder pathBlockingProbabilitiesTable = new StringBuilder();
	    pathBlockingProbabilitiesTable.append("<table border='1'>");
	    pathBlockingProbabilitiesTable.append("<tr><th>Route id</th><th>Demand id</th><th><i>s<sub>d</sub></i></th><th><i>h<sub>d</sub></i> (Erlangs)</th><th>Node sequence</th><th>Link sequence</th><th>Blocking probability</th><th>Effective carried traffic (Erlangs)</th><th>Blocking probability (Erlang-B)</th></tr>");

	    for (int routeId = 0; routeId < R; routeId++)
	    {
		int demandId = netPlan.getRouteDemand(routeId);
		String nodeSequence = IntUtils.join(netPlan.getRouteSequenceOfNodes(routeId), " => ");
		String linkSequence = IntUtils.join(netPlan.getRouteSequenceOfLinks(routeId), " => ");

		pathBlockingProbabilitiesTable.append(String.format("<tr><td>%d</td><td>%d</td><td>%d</td><td>%.3f</td><td>%s</td><td>%s</td><td>%.3f</td><td>%.3f</td><td>%.3f</td></tr>", routeId, demandId, s_p[routeId], h_p[routeId], nodeSequence, linkSequence, B_p[routeId], h_p[routeId] * (1 - B_p[routeId]), B_p_erlangB[routeId]));
	    }
	    pathBlockingProbabilitiesTable.append("</table>");

	    // Generate network blocking information table
	    final StringBuilder networkBlockingProbabilityTable = new StringBuilder();

	    double totalOfferedTraffic = DoubleFactory1D.dense.make(h_p).zSum();
	    double effectiveCarriedTraffic = totalOfferedTraffic * (1 - B);

	    String monoclassTraffic = (min_s_d == 1 && max_s_d == min_s_d) ? "Yes" : "No";

	    double[] offeredTrafficByConnectionSize = new double[max_s_d];
	    double[] carriedTrafficByConnectionSize = new double[max_s_d];

	    for (int routeId = 0; routeId < R; routeId++)
	    {
		offeredTrafficByConnectionSize[s_p[routeId] - 1] = h_p[routeId];
		carriedTrafficByConnectionSize[s_p[routeId] - 1] = h_p[routeId] * (1 - B_p[routeId]);
	    }

	    StringBuilder connectionSizeDistribution = new StringBuilder();

	    for (int connectionSizeId = 0; connectionSizeId < max_s_d; connectionSizeId++)
	    {
		if (connectionSizeDistribution.length() != 0)
		    connectionSizeDistribution.append(", ");

		double percentage = offeredTrafficByConnectionSize[connectionSizeId] == 0 ? 0 : 100 * carriedTrafficByConnectionSize[connectionSizeId] / offeredTrafficByConnectionSize[connectionSizeId];
		connectionSizeDistribution.append(String.format("%d (%.3f%%)", connectionSizeId + 1, percentage));
	    }

	    networkBlockingProbabilityTable.append("<table border='1'>");
	    networkBlockingProbabilityTable.append("<tr><th>Parameter</th><th>Value</th></tr>");
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Total offered traffic (Erlangs)</td><td>%.3f</td></tr>", totalOfferedTraffic));
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Average blocking probability</td><td>%.3f</td></tr>", B));
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Average blocking probability (Erlang-B)</td><td>%.3f</td></tr>", B_erlangB));
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Carried traffic</td><td>%.3f</td></tr>", effectiveCarriedTraffic));
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Monoclass traffic?</td><td>%s</td></tr>", monoclassTraffic));
	    networkBlockingProbabilityTable.append(String.format("<tr><td>Connection sizes</td><td>%s</td></tr>", connectionSizeDistribution.toString()));
	    networkBlockingProbabilityTable.append("</table>");

            try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/examples/reports/blocking/main.html").toURI().toURL()); }
            catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }

	    html = html.replaceFirst("#linkBlockingProbabilitiesTable#", linkBlockingProbabilitiesTable.toString());
	    html = html.replaceFirst("#pathBlockingProbabilitiesTable#", pathBlockingProbabilitiesTable.toString());
	    html = html.replaceFirst("#networkBlockingProbabilityTable#", networkBlockingProbabilityTable.toString());
	}
	else
	{
	    html = "<html><body><p>No information available</p></body></html>";
	}

	return html;
    }
}
