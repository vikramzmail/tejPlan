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

package com.tejas.workspace.examples.reports.availability;

import cern.colt.matrix.tdouble.DoubleFactory1D;

import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType;
import com.tejas.engine.libraries.SRGUtils;
import com.tejas.engine.utils.*;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * This report analyzes a network design in terms of average availability under a set of failures.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.1, December 2013
 */
public class Report_availability implements IReport
{
    private double[] y_d_0;
    private double y_D_0;

    private double[] A_d_1;
    private double[] A_d_2;

    private double A_D_1;
    private double A_D_2;
    
    private double pi_excess;

    @Override
    public String getDescription()
    {
	return "This report analyzes a network design in terms of average availability under a set of failures";
    }

    @Override
    public String getTitle()
    {
	return "Availability report";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> reportParameters = new ArrayList<Triple<String, String, String>>();
	reportParameters.add(Triple.of("alg_provisioning", "", "Algorithm to process failure events"));
	reportParameters.add(Triple.of("analyzeDoubleFailures", "true", "Indicates whether double failures are studied"));
        reportParameters.add(Triple.of("assumeOverSubscribedLinksAsFailedLinks", "false", "Indicates whether over-subscribed links are assumed as failed links in metrics. Each route traversing a over-subscribed link will be assumed to carry no traffic"));
	reportParameters.add(Triple.of("assumeUnprotectedRoutes", "false", "Do not apply the recovery algorithm"));
	reportParameters.add(Triple.of("defaultMTTFInHours", "8748", "Default value for Mean Time To Fail (hours)"));
	reportParameters.add(Triple.of("defaultMTTRInHours", "12", "Default value for Mean Time To Repair (hours)"));
	reportParameters.add(Triple.of("failureModel", "perBidirectionalLinkBundle", "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle"));
	reportParameters.add(Triple.of("omitProtectionSegments", "false", "Remove protection segments from the network plan to free their reserved bandwidth"));
        reportParameters.add(Triple.of("allowLinkOversubscription","true","Indicates whether or not links may be over-subscripted"));
        reportParameters.add(Triple.of("allowExcessCarriedTraffic","true","Indicates whether or not is enforced that carried traffic cannot be greater than the offered one for any demand"));

	return reportParameters;
    }

    @Override
    public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
    {
	if (!netPlan.hasRoutes())
            return "<html><body>No information available</body></html>";
        
        double precisionFactor = Double.parseDouble(net2planParameters.get("precisionFactor"));
        
	boolean allowExcessCarriedTraffic, allowLinkOversubscription, analyzeDoubleFailures, assumeOverSubscribedLinksAsFailedLinks, assumeUnprotectedRoutes, omitProtectionSegments;
	double defaultMTTFInHours;
	double defaultMTTRInHours;
	String failureModel;

	IProvisioningAlgorithm algorithm = null;

	// Check report parameters
        try { allowExcessCarriedTraffic = Boolean.parseBoolean(reportParameters.get("allowExcessCarriedTraffic")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'allowExcessCarriedTraffic' value must be true or false"); }

        try { allowLinkOversubscription = Boolean.parseBoolean(reportParameters.get("allowLinkOversubscription")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'allowLinkOversubscription' value must be true or false"); }

        try { analyzeDoubleFailures = Boolean.parseBoolean(reportParameters.get("analyzeDoubleFailures")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'analyzeDoubleFailures' value must be true or false"); }
        
	try { assumeOverSubscribedLinksAsFailedLinks = Boolean.parseBoolean(reportParameters.get("assumeOverSubscribedLinksAsFailedLinks")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'assumeOverSubscribedLinksAsFailedLinks' value must be true or false"); }

	try { assumeUnprotectedRoutes = Boolean.parseBoolean(reportParameters.get("assumeUnprotectedRoutes")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'assumeUnprotectedRoutes' value must be true or false"); }
        
	try { omitProtectionSegments = Boolean.parseBoolean(reportParameters.get("omitProtectionSegments")); }
	catch (Exception ex) { throw new Net2PlanException("Parameter 'omitProtectionSegments' value must be true or false"); }

	try
        {
	    defaultMTTFInHours = Double.parseDouble(reportParameters.get("defaultMTTFInHours"));
	    if (defaultMTTFInHours <= 0) defaultMTTFInHours = Double.MAX_VALUE;
	}
	catch (Exception ex)
	{
	    throw new Net2PlanException("Parameter 'defaultMTTFInHours' value must be a valid number");
	}

	try
	{
	    defaultMTTRInHours = Double.parseDouble(reportParameters.get("defaultMTTRInHours"));
	    if (defaultMTTRInHours <= 0) throw new Exception("Bad");
	}
	catch (Exception ex)
	{
	    throw new Net2PlanException("Parameter 'defaultMTTRInHours' value must be greater than zero");
	}
        

	failureModel = reportParameters.get("failureModel");

	if (!reportParameters.containsKey("alg_provisioning_File") || reportParameters.get("alg_provisioning_File").isEmpty())
	    throw new Net2PlanException("A provisioning algorithm must be defined");

	File algorithmFile = new File(reportParameters.get("alg_provisioning_File"));
	String algorithmName = reportParameters.get("alg_provisioning_Algorithm");
	String algorithmParam = reportParameters.get("alg_provisioning_Parameters");

	Map<String, String> algorithmParameters = new HashMap<String, String>();
	String[] parameters = StringUtils.split(algorithmParam, ", ");
	for (String parameter : parameters)
	{
	    String[] paramValue = StringUtils.split(parameter, "=");
	    algorithmParameters.put(paramValue[0], paramValue[1]);
	}

        int D = netPlan.getNumberOfDemands();
        int R = netPlan.getNumberOfRoutes();
        
        if (omitProtectionSegments) netPlan.removeAllProtectionSegments();

        switch (failureModel)
        {
            case "SRGfromNetPlan":
                break;

            case "perNode":
                SRGUtils.configureSRGs(netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_NODE, true);
                break;

            case "perLink":
                SRGUtils.configureSRGs(netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_LINK, true);
                break;

            case "perDirectionalLinkBundle":
                SRGUtils.configureSRGs(netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true);
                break;

            case "perBidirectionalLinkBundle":
                SRGUtils.configureSRGs(netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
                break;

            default:
                throw new Net2PlanException("Failure model not valid. Please, check algorithm parameters description");
        }
        
        int numSRGs = netPlan.getNumberOfSRGs();
        int[][] N_f = netPlan.getSRGNodesVector();
        int[][] E_f = netPlan.getSRGLinksVector();
        double[] A_f = netPlan.getSRGAvailabilityVector();
        
        List<Set<Integer>> F_s = defineFailureStates(numSRGs, analyzeDoubleFailures);
        
        // Compute state probabilities (pi_s)
        double[] pi_s = defineStateProbabilities(F_s, A_f);
        double sum_pi_s = DoubleUtils.sum(pi_s);
        pi_excess = 1 - sum_pi_s;

//        System.out.println("pi_s " + Arrays.toString(pi_s));
//        System.out.println("sum(pi_s) " + sum_pi_s);
//        System.out.println("pi_excess " + pi_excess);

        A_d_1 = new double[D];
        A_d_2 = new double[D];

        A_D_1 = 0;
        A_D_2 = 0;
        
        y_d_0 = netPlan.getDemandCarriedTrafficInErlangsVector();
        y_D_0 = DoubleUtils.sum(y_d_0);

        double[] y_d_s = new double[D];
        double y_D_s;

        ListIterator<Set<Integer>> stateIterator = F_s.listIterator();

        ResilienceNetState template = new ResilienceNetState(netPlan);

        while (stateIterator.hasNext())
        {
            int stateId = stateIterator.nextIndex();

            Set<Integer> aux_affectedSRGs = stateIterator.next();
            int[] affectedSRGs = IntUtils.toArray(aux_affectedSRGs);

            Arrays.fill(y_d_s, 0);

            if (affectedSRGs.length == 0)
            {
                y_d_s = DoubleUtils.copy(y_d_0);
            }
            else
            {
                if (assumeUnprotectedRoutes)
                {
                    Set<Integer> nodesDown = new HashSet<Integer>();
                    Set<Integer> linksDown = new HashSet<Integer>();
                    Set<Integer> routesDown = new HashSet<Integer>();

                    for(int srgId : affectedSRGs)
                    {
                        for(int nodeId : N_f[srgId]) nodesDown.add(nodeId);
                        for(int linkId : E_f[srgId]) linksDown.add(linkId);
                    }

                    for(int nodeId : nodesDown)
                    {
                        int[] routeIds = netPlan.getNodeTraversingRoutes(nodeId);
                        for(int routeId : routeIds) routesDown.add(routeId);
                    }

                    for(int linkId : linksDown)
                    {
                        int[] routeIds = netPlan.getLinkTraversingRoutes(linkId);
                        for(int routeId : routeIds) routesDown.add(routeId);
                    }

                    for (int routeId = 0; routeId < R; routeId++)
                    {
                        if (routesDown.contains(routeId)) continue;

                        int demandId = netPlan.getRouteDemand(routeId);
                        double carriedTraffic = netPlan.getRouteCarriedTrafficInErlangs(routeId);

                        y_d_s[demandId] += carriedTraffic;
                    }
                }
                else
                {
                    ResilienceNetState netState = (ResilienceNetState) template.copy();

                    Set<Integer> nodesUp2Down = new HashSet<Integer>();
                    Set<Integer> linksUp2Down = new HashSet<Integer>();
                    netState.getNodeLinkStateChanges(affectedSRGs, new int[0], null, nodesUp2Down, null, linksUp2Down);
                    
                    try
                    {
                        algorithm = ClassLoaderUtils.getInstance(algorithmFile, algorithmName, IProvisioningAlgorithm.class);
                        algorithm.initialize(netPlan, (ResilienceNetState) netState.unmodifiableView(), algorithmParameters, net2planParameters);

                        // Nodes up -> down
                        for(int nodeId : nodesUp2Down)
                        {
                            ResilienceEvent singleEvent = new ResilienceEvent(pi_s[stateId], nodeId, EventType.NODE_FAILURE);

                            List<ProvisioningAction> actions = algorithm.processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);

                            netState.update(singleEvent, actions);
                            netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                        }

                        // Links up -> down
                        for(int linkId : linksUp2Down)
                        {
                            ResilienceEvent singleEvent = new ResilienceEvent(pi_s[stateId], linkId, EventType.LINK_FAILURE);

                            List<ProvisioningAction> actions = algorithm.processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);

                            netState.update(singleEvent, actions);
                            netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                        }

                        for(int srgId : affectedSRGs)
                        {
                            ResilienceEvent singleEvent = new ResilienceEvent(pi_s[stateId], srgId, EventType.SRG_FAILURE);
                            netState.update(singleEvent, null);
                            netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                        }
                        
                        if (assumeOverSubscribedLinksAsFailedLinks)
                        {
                            long[] routeIds = netState.getRouteIds();
                            int[] oversubscribedLinks = netState.getLinksOversubscribed();

                            for(long routeId : routeIds)
                            {
                                int demandId = netState.getRouteDemand(routeId);

                                double trafficVolume = netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
                                if (trafficVolume == 0) continue;

                                long[] seqLinksAndSegments = netState.getRouteCurrentSequenceOfLinksAndSegments(routeId);

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
                                        if (netState.getProtectionSegmentReservedBandwidthInErlangs(segmentId) == 0)
                                        {
                                            int[] seqLinks = netState.getProtectionSegmentSequenceOfLinks(segmentId);
                                            if (IntUtils.containsAny(oversubscribedLinks, seqLinks))
                                            {
                                                assumeFailed = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (!assumeFailed) y_d_s[demandId] += trafficVolume;
                            }
                        }
                        else
                        {
                            y_d_s = netState.getDemandCurrentCarriedTrafficInErlangsVector();
                        }
                    }
                    catch(Throwable e)
                    {
                        try { if (algorithm != null) ((Closeable) algorithm.getClass().getClassLoader()).close(); }
                        catch(Throwable e1) { }

                        throw(e);
                    }
                }
            }

            for (int demandId = 0; demandId < D; demandId++)
            {
                if (y_d_0[demandId] == 0) continue;

                A_d_1[demandId] += (y_d_s[demandId] - y_d_0[demandId]) >= -precisionFactor ? pi_s[stateId] : 0;
                A_d_2[demandId] += Math.min(1, y_d_s[demandId] / y_d_0[demandId]) * pi_s[stateId];
            }

            if (y_D_0 == 0) continue;
            
            y_D_s = DoubleUtils.sum(y_d_s);
            
            A_D_1 += (y_D_s - y_D_0) >= -precisionFactor ? pi_s[stateId] : 0;
            A_D_2 += Math.min(1, y_D_s / y_D_0) * pi_s[stateId];
        }
        
        String html = generateReport(netPlan);

	return html;
    }
    
    /**
     * Returns the set of SRGs going down on each failure state.
     * 
     * @param numSRGs Number of defined SRGs
     * @param analyzeDoubleFailures Flag to indicate whether or not double failure states are included
     * @return Set of SRGs going down on each failure state
     * @since 1.0
     */
    protected List<Set<Integer>> defineFailureStates(int numSRGs, boolean analyzeDoubleFailures)
    {
        return SRGUtils.enumerateFailureStates(numSRGs, true, analyzeDoubleFailures);
    }

    /**
     * Computes the probability to find the network on each failure state.
     * 
     * @param F_s Set of SRGs failing per each failure state
     * @param A_f Availability value per SRG
     * @return Probability to find the network in each failure state
     * @since 1.0
     */
    protected double[] defineStateProbabilities(List<Set<Integer>> F_s, double[] A_f)
    {
        return SRGUtils.computeStateProbabilities(F_s, A_f);
    }

    /**
     * Returns an HTML <code>String</code> containing the report.
     * 
     * @param netPlan A network plan
     * @return Report in HTML format
     * @since 1.0
     */
    protected String generateReport(NetPlan netPlan)
    {
        int D = netPlan.getNumberOfDemands();

        // Per-demand table
        StringBuilder perDemandInfo = new StringBuilder();
        perDemandInfo.append("<table border='1'>");
        perDemandInfo.append("<tr><th><b>Demand id</b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Offered traffic (Erlangs)</b></th><th><b>Availability (classic)</b></th><th><b>Availability (weighted)</b></th></tr>");
        for (int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);

            double A_d_0 = y_d_0[demandId] == 0 ? 0 : 1;

            perDemandInfo.append(String.format("<tr><td>%d</td><td>n%d (%s)</td><td>n%d (%s)</td><td>%.3g</td><td>%.6f - %.6f</td><td>%.6f - %.6f</td></tr>", demandId, ingressNodeId, netPlan.getNodeName(ingressNodeId), egressNodeId, netPlan.getNodeName(egressNodeId), netPlan.getDemandOfferedTrafficInErlangs(demandId), A_d_1[demandId], A_d_1[demandId] + pi_excess * A_d_0, A_d_2[demandId], A_d_2[demandId] + pi_excess * A_d_0));
        }
        perDemandInfo.append("</table>");

        // Network table
        StringBuilder networkInfo = new StringBuilder();
        
        double H = DoubleFactory1D.dense.make(netPlan.getDemandOfferedTrafficInErlangsVector()).zSum();
        double A_D_0 = y_D_0 == 0 ? 0 : 1;

        networkInfo.append("<ul>");
        networkInfo.append(String.format("<p>Total offered traffic: %.3g Erlangs</p>", H));
        networkInfo.append(String.format("<p>Total carried traffic under no failures: %.3g Erlangs</p>", y_D_0));
        networkInfo.append("</ul>");

        networkInfo.append("<center>");
        networkInfo.append("<table border='1'><thead><tr><th><b>Metric</b></th><th><b>Availability (classic)</b></th><th><b>Availability (weighted)</b></th></thead><tbody>");
        
        networkInfo.append(String.format("<tr><td>Network Availability</td><td>%.6f - %.6f</td><td>%.6f - %.6f</td></tr>", A_D_1, A_D_1 + pi_excess * A_D_0, A_D_2, A_D_2 + pi_excess * A_D_0));
        
        int worstDemandId_1 = DoubleUtils.minIndexes(A_d_1, Constants.SearchType.FIRST)[0];
        int worstDemandId_2 = DoubleUtils.minIndexes(A_d_2, Constants.SearchType.FIRST)[0];
        double worst_A_d_0_1 = y_d_0[worstDemandId_1] == 0 ? 0 : 1;
        double worst_A_d_0_2 = y_d_0[worstDemandId_2] == 0 ? 0 : 1;

        networkInfo.append(String.format("<tr><td>Worst Demand Availability</td><td>%.6f - %.6f</td><td>%.6f - %.6f</td></tr>", A_d_1[worstDemandId_1], A_d_1[worstDemandId_1] + pi_excess * worst_A_d_0_1, A_d_2[worstDemandId_2], A_d_2[worstDemandId_2] + pi_excess * worst_A_d_0_2));
        networkInfo.append(String.format("<tr><td>Average Demand Availability</td><td>%.6f - %.6f</td><td>%.6f - %.6f</td></tr>", DoubleUtils.average(A_d_1), DoubleUtils.average(A_d_1) + pi_excess * A_D_0, DoubleUtils.average(A_d_2), DoubleUtils.average(A_d_2) + pi_excess * A_D_0));
        
        networkInfo.append("</tbody></table>");
        networkInfo.append("</center>");

        String html;

        html = HTMLUtils.getHTMLFromURL(getReportTemplateFile());
        html = html.replaceFirst("#perDemandInfo#", perDemandInfo.toString());
        html = html.replaceFirst("#networkInfo#", networkInfo.toString());
        
        return html;
    }
    
    /**
     * Returns the location of the template file for the report.
     * 
     * @return Location of the template file
     * @since 1.0
     */
    protected URL getReportTemplateFile()
    {
        try { return getClass().getResource("/com/net2plan/examples/reports/availability/main_availability.html").toURI().toURL(); }
        catch(Throwable ex) { throw new RuntimeException(ex); }
    }

}