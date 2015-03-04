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

package com.tejas.workspace.examples.reports.delay;

import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.NetworkPerformanceMetrics;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This report shows delay information considering a packet-switched network.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class Report_delay implements IReport
{

    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
	if (!netPlan.hasRoutes()) return "<html><body><p>No delay information available. Routing is not defined.</p></body></html>";

	int E = netPlan.getNumberOfLinks();
	int D = netPlan.getNumberOfDemands();
	int R = netPlan.getNumberOfRoutes();

	String html;

	String hurstParameter = reportParameters.get("hurstParameter");
	NetworkPerformanceMetrics metrics = new NetworkPerformanceMetrics(netPlan, net2planParameters, "hurstParameter", hurstParameter);

	String[] nodeName = netPlan.getNodeNameVector();
	int[][] linkTable = netPlan.getLinkTable();
	int[][] demandTable = netPlan.getDemandTable();

	// Link metrics
	double[] T_e_prop = metrics.getLinkPropagationDelayInSecondsVector();
	double[] T_e_tx = metrics.getLinkTransmissionDelayInSecondsVector();
	double[] T_e_buf = metrics.getLinkBufferingDelayInSecondsVector();
	double[] T_e = metrics.getLinkTotalDelayInSecondsVector();

	// Route metrics
	double[] T_r_prop = metrics.getRoutePropagationDelayInSecondsVector();
	double[] T_r_tx = metrics.getRouteTransmissionDelayInSecondsVector();
	double[] T_r_buf = metrics.getRouteBufferingDelayInSecondsVector();
	double[] T_r = metrics.getRouteTotalDelayInSecondsVector();

	// Demand metrics
	double[] T_d_prop = metrics.getDemandAveragePropagationDelayInSeconds();
	double[] T_d_tx = metrics.getDemandAverageTransmissionDelayInSeconds();
	double[] T_d_buf = metrics.getDemandAverageBufferingDelayInSeconds();
	double[] T_d = metrics.getDemandAverageTotalDelayInSeconds();

	// Network metrics
	double T = metrics.getNetworkAverageDelay();
	double T_onlyPropagation = metrics.getNetworkAveragePropagationDelay();

	// Link information table
	StringBuilder linkDelayTable = new StringBuilder();
	linkDelayTable.append("<table border='1'>");
	linkDelayTable.append("<tr><th><b>Link id</a></b></th><th><b>Origin node</b></th><th><b>Destination node</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th>");
	for (int linkId = 0; linkId < E; linkId++)
	    linkDelayTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", linkId, linkTable[linkId][0], nodeName[linkTable[linkId][0]], linkTable[linkId][1], nodeName[linkTable[linkId][1]], T_e_prop[linkId] * 1000, T_e_tx[linkId] * 1000, T_e_buf[linkId] * 1000, T_e[linkId] * 1000, netPlan.getLinkSpecificAttributes(linkId).toString()));
	linkDelayTable.append("</table>");

	// Route information table
	StringBuilder pathDelayTable = new StringBuilder();
	pathDelayTable.append("<table border='1'>");
	pathDelayTable.append("<tr><th><b>Route id</a></b></th><th><b>Demand id</a></b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Sequence of links</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th></tr>");
	for (int routeId = 0; routeId < R; routeId++)
	{
	    int demandId = netPlan.getRouteDemand(routeId);
	    int[] sequenceOfLinks = netPlan.getRouteSequenceOfLinks(routeId);
	    String auxSequenceOfLinks = IntUtils.join(sequenceOfLinks, " => ");

	    pathDelayTable.append(String.format("<tr><td>%d</td><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%s</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", routeId, demandId, demandTable[demandId][0], nodeName[demandTable[demandId][0]], demandTable[demandId][1], nodeName[demandTable[demandId][1]], auxSequenceOfLinks, T_r_prop[routeId] * 1000, T_r_tx[routeId] * 1000, T_r_buf[routeId] * 1000, T_r[routeId] * 1000, netPlan.getRouteSpecificAttributes(routeId)));
	}
	pathDelayTable.append("</table>");

	// Demand information table
	StringBuilder demandDelayTable = new StringBuilder();
	demandDelayTable.append("<table border='1'>");
	demandDelayTable.append("<tr><th><b>Demand id</a></b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th></tr>");
	for (int demandId = 0; demandId < D; demandId++)
	    demandDelayTable.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", demandId, demandTable[demandId][0], nodeName[demandTable[demandId][0]], demandTable[demandId][1], nodeName[demandTable[demandId][1]], T_d_prop[demandId] * 1000, T_d_tx[demandId] * 1000, T_d_buf[demandId] * 1000, T_d[demandId] * 1000, netPlan.getDemandSpecificAttributes(demandId)));
	demandDelayTable.append("</table>");

	// Network information table
	StringBuilder networkDelayTable = new StringBuilder();
	networkDelayTable.append("<table border='1'>");
	networkDelayTable.append("<tr><td>Average network delay (ms)</td><td>").append(String.format("%.3g", T * 1000)).append("</td></tr>");
	networkDelayTable.append("<tr><td>Average network delay only considering propagation (ms)</td><td>").append(String.format("%.3g", T_onlyPropagation * 1000)).append("</td></tr>");
	networkDelayTable.append("</table>");

	try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/examples/reports/delay/main.html").toURI().toURL()); }
	catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }

	html = html.replaceFirst("#linkDelayTable#", linkDelayTable.toString());
	html = html.replaceFirst("#pathDelayTable#", pathDelayTable.toString());
	html = html.replaceFirst("#demandDelayTable#", demandDelayTable.toString());
	html = html.replaceFirst("#networkDelayTable#", networkDelayTable.toString());

	return html;
    }

    @Override
    public String getDescription()
    {
	return "This report shows delay information considering a packet-switched network";
    }

    @Override
    public String getTitle()
    {
	return "Delay metrics";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> aux = new ArrayList<Triple<String, String, String>>();

	aux.add(Triple.of("hurstParameter", "0.5", "Hurst parameter (H) of self-similar sources [0.5, 1) (M/M/1 model when H = 0.5)"));

	return aux;
    }
}