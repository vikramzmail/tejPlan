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

package com.tejas.workspace.examples.reports.robustness;

import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphTheoryMetrics;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Triple;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class Report_robustness implements IReport
{
    @Override
    public String getDescription()
    {
	return "This report analyses a network design in terms of robustness under different multiple-failures scenarios";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	return new ArrayList<Triple<String, String, String>>();
    }

    @Override
    public String getTitle()
    {
	return "Robustness report";
    }

    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();

	if (N == 0 || E == 0) throw new Net2PlanException("This report requires a physical topology (nodes and links)");

	int[][] linkTable = netPlan.getLinkTable();
	double[] costVector = DoubleUtils.ones(E);

	GraphTheoryMetrics metrics = new GraphTheoryMetrics(linkTable, N, costVector);

	int[] outNodeDegreeVector = metrics.getOutNodeDegree();
	double[][] betweenessCentrality = metrics.getBetweenessCentrality();

	double a2tr = metrics.getAverageTwoTermReliability();
	double algebraicConnectivity = metrics.getAlgebraicConnectivity();
	double averageNeighborConnectivity = metrics.getAverageNeighborConnectivity();
	double assortativity = metrics.getAssortativity();
	double averageLinkBC = DoubleUtils.average(betweenessCentrality[1]);
	double averageNodeBC = DoubleUtils.average(betweenessCentrality[0]);
	double avgPathLength = metrics.getAverageShortestPathDistance();
	double clusteringCoeff = metrics.getClusteringCoefficient();
	double density = metrics.getDensity();
	int diameter = (int) metrics.getDiameter();
	double heterogeneity = metrics.getHeterogeneity();
	int linkConnectivity = metrics.getLinkConnectivity();
	int maxNodeDegree = IntUtils.maxValue(outNodeDegreeVector);
	int nodeConnectivity = metrics.getNodeConnectivity();
	int numberOfLinks = E;
	int numberOfNodes = N;
	double outNodeDegree = IntUtils.average(outNodeDegreeVector);
	double spectralRadius = metrics.getSpectralRadius();
	double symmetryRatio = metrics.getSymmetryRatio();

	String html;
        try { html = HTMLUtils.getHTMLFromURL(getClass().getResource("/com/net2plan/examples/reports/robustness/main.html").toURI().toURL()); }
        catch(URISyntaxException | MalformedURLException e) { throw new RuntimeException(e); }

	html = html.replaceFirst("#algebraicConnectivity#", String.format("%.3f", algebraicConnectivity));
	html = html.replaceFirst("#comments_algebraicConnectivity#", "-");

	html = html.replaceFirst("#assortativity#", String.format("%.3f", assortativity));
	html = html.replaceFirst("#comments_assortativity#", assortativity < 0 ? "Your network is vulnerable to static attacks" : "Your network is robust to static attacks");

	html = html.replaceFirst("#averageLinkBC#", String.format("%.3f", averageLinkBC));
	html = html.replaceFirst("#comments_averageLinkBC#", "-");

	html = html.replaceFirst("#averageNeighborConnectivity#", String.format("%.3f", averageNeighborConnectivity));
	html = html.replaceFirst("#comments_averageNeighborConnectivity#", "-");

	html = html.replaceFirst("#outNodeDegree#", String.format("%.3f", outNodeDegree));
	html = html.replaceFirst("#comments_outNodeDegree#", "-");

	html = html.replaceFirst("#averageNodeBC#", String.format("%.3f", averageNodeBC));
	html = html.replaceFirst("#comments_averageNodeBC#", "-");

	html = html.replaceFirst("#avgPathLength#", String.format("%.3f", avgPathLength));
	html = html.replaceFirst("#comments_avgPathLength#", "-");

	html = html.replaceFirst("#a2tr#", String.format("%.3f", a2tr));
	html = html.replaceFirst("#comments_a2tr#", "-");

	html = html.replaceFirst("#clusteringCoeff#", String.format("%.3f", clusteringCoeff));
	html = html.replaceFirst("#comments_clusteringCoeff#", "-");

	html = html.replaceFirst("#density#", String.format("%.3f", density));
	html = html.replaceFirst("#comments_density#", "-");

	html = html.replaceFirst("#diameter#", String.format("%d", diameter));
	html = html.replaceFirst("#comments_diameter#", "-");

	html = html.replaceFirst("#heterogeneity#", String.format("%.3f", heterogeneity));
	html = html.replaceFirst("#comments_heterogeneity#", "-");

	html = html.replaceFirst("#linkConnectivity#", String.format("%d", linkConnectivity));
	html = html.replaceFirst("#comments_linkConnectivity#", "-");

	html = html.replaceFirst("#numberOfLinks#", String.format("%d", numberOfLinks));
	html = html.replaceFirst("#comments_numberOfLinks#", "-");

	html = html.replaceFirst("#maxNodeDegree#", String.format("%d", maxNodeDegree));
	html = html.replaceFirst("#comments_maxNodeDegree#", "-");

	html = html.replaceFirst("#nodeConnectivity#", String.format("%d", nodeConnectivity));
	html = html.replaceFirst("#comments_nodeConnectivity#", "-");

	html = html.replaceFirst("#numberOfNodes#", String.format("%d", numberOfNodes));
	html = html.replaceFirst("#comments_numberOfNodes#", "-");

	html = html.replaceFirst("#spectralRadius#", String.format("%.3f", spectralRadius));
	html = html.replaceFirst("#comments_spectralRadius#", "-");

	html = html.replaceFirst("#symmetryRatio#", String.format("%.3f", symmetryRatio));
	html = html.replaceFirst("#comments_symmetryRatio#", symmetryRatio < 1 ? "-" : "Your network performs equally in response to a random (SR) or a target attack (ST)");

	return html;
    }
}
