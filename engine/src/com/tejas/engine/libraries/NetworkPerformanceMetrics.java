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

package com.tejas.engine.libraries;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.impl.DenseIntMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.BooleanUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Class implementing different network metrics (i.e. blocking, delay...)</p>
 *
 * <p>On the one hand, it provides static methods to compute several performance
 * metrics using classical formulae (i.e. Erlang-B or Kaufman-Roberts recursion
 * for call blocking probability...).</p>
 *
 * <p>On the other hand, given a complete network design, it allows to obtain several
 * performance metrics such as average delay or blocking information, using the
 * referred methods above.</p>
 *
 * <p><b>Traffic and queuing delay model</b></p>
 * 
 * <p>We assume that each demand <i>d</i> is a traffic source of average load 
 * <i>h<sub>d</sub></i>. The traffic in each link is the aggregation of the
 * traffic from the demands routed through each link. We assume that the packet
 * arrivals in each link are independent from each other, and follow a self-similar
 * pattern with Hurst parameter <i>H &isin; [0.5, 1)</i>. Roughly speaking,
 * self-similarity in the traffic means that the traffic is bursty at different
 * time scales. The higher <i>H</i> (<i>H&asymp;1</i>), the more self-similar
 * the traffic is. A Hurst parameter <i>H=0.5</i> characterizes non self-similar
 * traffic (i.e. Poisson traffic has a parameter <i>H=0.5</i>). There are many
 * models to estimate queuing delays for queues fed with self-similar traffic.
 * Estimations of queuing delays for queues fed with self-similar traffic are
 * usually very complex, so we use the simple estimation in [<a href='#Stallings2002'>1</a>].</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see <a name='Stallings2002' />[1] W. Stallings, <i>High-Speed Networks and Internets: Performance and Quality of Service</i>, Prentice Hall, 2002.
 */
public final class NetworkPerformanceMetrics
{
    // Class parameters
    private double hurstParameter;

    // Cache
    private int N, E, D, R, S, numSRGs;
    private DoubleMatrix1D u_e, u_e_noProtection, y_e, rho_e, d_e;
    private DoubleMatrix1D h_d, r_d;
    private DoubleMatrix1D x_p;
    private DoubleMatrix1D u_s_reservedForProtection;
    private double U_e, U_e_noProtection;
    private double H_d, R_d;
    private int[][] linkTable;
    private List<int[]> routes;
    private List<int[]> routeBackupSegments;
    private List<int[]> segments;
    private DoubleMatrix1D t_e_prop, t_e_tx, t_e_buf, t_e;
    private DoubleMatrix1D t_p_prop, t_p_tx, t_p_buf, t_p;
    private DoubleMatrix1D t_d_prop, t_d_tx, t_d_buf, t_d;
    private int[][] demand2RouteIds;
    private int[] bifurcationDegree;
    private double T;
    private double T_onlyProp;
    private int[] bottleneckLinks;
    private int[] dedicatedSegments;
    private int[][] N_f;
    private int[][] E_f;

    private int[][] F_n;
    private int[][] F_e;
    private int[][] F_r;
    private int[][] F_s;

    // Net2Plan parameters
    private double PRECISIONFACTOR;
    private double packetLengthInBytes;
    private double R_b;
    private double v_prop;
    private double secondsPerErlang;
    
    /**
     * <p>Default constructor.</p>
     * 
     * <p>Parameters:</p>
     * <ul>
     * <li>hurstParameter: Hurst parameter characterizing self-similar traffic, in range [0.5, 1) (H = 0.5 yields to M/M/1 queuing model). Default value = 0.5</li>
     * </ul>
     *
     * @param netPlan A network design
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @param paramValuePairs Parameters to be passed to the class to tune its operation. An even number of <code>String</code> is to be passed. For each <code>String</code> pair, first <code>String</code> must be the name of the parameter, second a <code>String</code> with its value. If no name-value pairs are set, default values are used
     * @since 0.2.2
     */
    public NetworkPerformanceMetrics(NetPlan netPlan, Map<String, String> net2planParameters, String... paramValuePairs)
    {
	Configuration.check(net2planParameters);

	hurstParameter = 0.5;

	int numParameters = (int) (paramValuePairs.length / 2);
	if ( (((double) paramValuePairs.length) / 2) != (double) numParameters) throw new RuntimeException("A parameter has not assigned its value");

	for (int contParam = 0 ; contParam < numParameters ; contParam ++)
	{
	    String parameter = paramValuePairs[contParam * 2];
	    String value = paramValuePairs[contParam * 2 + 1];

	    if (parameter.equalsIgnoreCase("hurstParameter"))
	    {
		hurstParameter = Double.parseDouble(value);
		if (hurstParameter < 0.5 || hurstParameter >= 1) throw new RuntimeException("'hurstParameter' must be a number in range [0.5, 1)");
	    }
	    else
	    {
		throw new RuntimeException("Unknown parameter " + parameter);
	    }
	}

	PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
	R_b = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));
	packetLengthInBytes = Double.parseDouble(net2planParameters.get("averagePacketLengthInBytes"));
	v_prop = Double.parseDouble(net2planParameters.get("propagationSpeedInKmPerSecond"));
	if (v_prop <= 0) v_prop = Double.MAX_VALUE;

	secondsPerErlang = 8 * packetLengthInBytes / R_b;
        N = netPlan.getNumberOfNodes();
	E = netPlan.getNumberOfLinks();
	D = netPlan.getNumberOfDemands();
	R = netPlan.getNumberOfRoutes();
        S = netPlan.getNumberOfProtectionSegments();
        numSRGs = netPlan.getNumberOfSRGs();

        u_e = DoubleFactory1D.dense.make(netPlan.getLinkCapacityInErlangsVector());
	u_e_noProtection = DoubleFactory1D.dense.make(netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector());
	y_e = DoubleFactory1D.dense.make(netPlan.getLinkCarriedTrafficInErlangsVector());
	rho_e = DoubleFactory1D.dense.make(netPlan.getLinkUtilizationVector());
	d_e = DoubleFactory1D.dense.make(netPlan.getLinkLengthInKmVector());
        h_d = DoubleFactory1D.dense.make(netPlan.getDemandOfferedTrafficInErlangsVector());
        r_d = DoubleFactory1D.dense.make(netPlan.getDemandCarriedTrafficInErlangsVector());
	x_p = DoubleFactory1D.dense.make(netPlan.getRouteCarriedTrafficInErlangsVector());
        u_s_reservedForProtection = DoubleFactory1D.dense.make(netPlan.getProtectionSegmentReservedBandwithInErlangsVector());
        
        bottleneckLinks = DoubleUtils.maxIndexes(rho_e.toArray(), Constants.SearchType.ALL, PRECISIONFACTOR);
        dedicatedSegments = BooleanUtils.find(netPlan.getProtectionSegmentIsDedicatedVector(), true, Constants.SearchType.ALL);
        
        N_f = netPlan.getSRGNodesVector();
        E_f = netPlan.getSRGLinksVector();
        F_n = netPlan.getNodeSRGsVector();
        F_e = netPlan.getLinkSRGsVector();
        
        U_e = u_e.zSum();
        U_e_noProtection = u_e_noProtection.zSum();
	H_d = h_d.zSum();
	R_d = r_d.zSum();
        
        linkTable = netPlan.getLinkTable();

	demand2RouteIds = new int[D][];
	for(int demandId = 0; demandId < D; demandId++)
	    demand2RouteIds[demandId] = netPlan.getDemandRoutes(demandId);

	routes = netPlan.getRouteAllSequenceOfLinks();
        segments = netPlan.getProtectionSegmentAllSequenceOfLinks();
        
        F_r = new int[R][];
        F_s = new int[S][];
        
        for(int routeId = 0; routeId < R; routeId++)
        {
            int[] linkIds = routes.get(routeId);
            int[] nodeIds = convertSeqLinks2SeqNodes(linkIds);
            
            Set<Integer> aux_srgIds = new HashSet<Integer>();
            for(int linkId : linkIds) aux_srgIds.addAll(IntUtils.toList(F_e[linkId]));
            for(int nodeId : nodeIds) aux_srgIds.addAll(IntUtils.toList(F_n[nodeId]));
            F_r[routeId] = IntUtils.toArray(aux_srgIds);
        }
            
        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            int[] linkIds = segments.get(segmentId);
            int[] nodeIds = convertSeqLinks2SeqNodes(linkIds);
            
            Set<Integer> aux_srgIds = new HashSet<Integer>();
            for(int linkId : linkIds) aux_srgIds.addAll(IntUtils.toList(F_e[linkId]));
            for(int nodeId : nodeIds) aux_srgIds.addAll(IntUtils.toList(F_n[nodeId]));
            F_s[segmentId] = IntUtils.toArray(aux_srgIds);
        }

        routeBackupSegments = new LinkedList<int[]>();
        for(int routeId = 0; routeId < R; routeId++)
            routeBackupSegments.add(netPlan.getRouteBackupSegmentList(routeId));
            
	t_e_prop = null;
	t_e_tx = null;
	t_e_buf = null;
	t_e = null;

	t_p_prop = null;
	t_p_tx = null;
	t_p_buf = null;
	t_p = null;

	t_d_prop = null;
	t_d_tx = null;
	t_d_buf = null;
	t_d = null;

	bifurcationDegree = null;

	T = -1;
	T_onlyProp = -1;
    }
    
    /**
     * Returns the maximum link utilization.
     * 
     * @return Maximum link utilization
     * @since 0.2.3
     */
    public double getLinkMaximumUtilization() { return rho_e.size() == 0 ? 0 : rho_e.getMaxLocation()[0]; }

    /**
     * Returns the average buffering delay for each demand. Since routing may
     * be bifurcated, the average delay is equal to the weighted mean of transmission
     * among all routes for a demand, where weight is equal to the carried traffic.
     *
     * @return Buffering delay for each demand
     * @since 0.2.2
     */
    public double[] getDemandAverageBufferingDelayInSeconds()
    {
	if (t_d_buf == null)
	{
	    t_d_buf = DoubleFactory1D.dense.make(D);
	    DoubleMatrix1D aux_t_r_buf = DoubleFactory1D.dense.make(getRouteBufferingDelayInSecondsVector());

	    for(int demandId = 0; demandId < D; demandId++)
	    {
		DoubleMatrix1D x_p_thisRoute = x_p.viewSelection(demand2RouteIds[demandId]);
		double totalTraffic = x_p_thisRoute.zSum();
		if (totalTraffic < PRECISIONFACTOR) continue;

		t_d_buf.setQuick(demandId, x_p_thisRoute.zDotProduct(aux_t_r_buf.viewSelection(demand2RouteIds[demandId])) / totalTraffic);
	    }
	}

	return t_d_buf.toArray();
    }

    /**
     * Returns the average propagation delay for each demand. Since routing may
     * be bifurcated, the average delay is equal to the weighted mean of transmission
     * among all routes for a demand, where weight is equal to the carried traffic.
     *
     * @return Propagation delay for each demand
     * @since 0.2.2
     */
    public double[] getDemandAveragePropagationDelayInSeconds()
    {
	if (t_d_prop == null)
	{
	    t_d_prop = DoubleFactory1D.dense.make(D);
	    DoubleMatrix1D aux_t_r_prop = DoubleFactory1D.dense.make(getRoutePropagationDelayInSecondsVector());

	    for(int demandId = 0; demandId < D; demandId++)
	    {
		DoubleMatrix1D x_p_thisRoute = x_p.viewSelection(demand2RouteIds[demandId]);
		double totalTraffic = x_p_thisRoute.zSum();
		if (totalTraffic < PRECISIONFACTOR) continue;

		t_d_prop.setQuick(demandId, x_p_thisRoute.zDotProduct(aux_t_r_prop.viewSelection(demand2RouteIds[demandId])) / totalTraffic);
	    }
	}

	return t_d_prop.toArray();
    }

    /**
     * Returns the average end-to-end delay (propagation + transmission + buffering) for each demand.
     *
     * @return Total delay for each demand
     * @since 0.2.2
     */
    public double[] getDemandAverageTotalDelayInSeconds()
    {
	if (t_d == null)
        {
	    DoubleMatrix1D aux_t_d_prop = DoubleFactory1D.dense.make(getDemandAveragePropagationDelayInSeconds());
	    DoubleMatrix1D aux_t_d_tx = DoubleFactory1D.dense.make(getDemandAverageTransmissionDelayInSeconds());
	    DoubleMatrix1D aux_t_d_buf = DoubleFactory1D.dense.make(getDemandAverageBufferingDelayInSeconds());
            
	    t_d = aux_t_d_prop.assign(aux_t_d_tx, DoubleFunctions.plus).assign(aux_t_d_buf, DoubleFunctions.plus);
        }

	return t_d.toArray();
    }

    /**
     * Returns the average transmission delay for each demand. Since routing may
     * be bifurcated, the average delay is equal to the weighted mean of transmission
     * among all routes for a demand, where weight is equal to the carried traffic.
     *
     * @return Transmission delay for each demand
     * @since 0.2.2
     */
    public double[] getDemandAverageTransmissionDelayInSeconds()
    {
	if (t_d_tx == null)
	{
	    t_d_tx = DoubleFactory1D.dense.make(D);
	    DoubleMatrix1D aux_t_r_tx = DoubleFactory1D.dense.make(getRouteTransmissionDelayInSecondsVector());

	    for(int demandId = 0; demandId < D; demandId++)
	    {
                if (demand2RouteIds[demandId].length == 0) continue;

                DoubleMatrix1D x_p_thisRoute = x_p.viewSelection(demand2RouteIds[demandId]);
		double totalTraffic = x_p_thisRoute.zSum();
		if (totalTraffic < PRECISIONFACTOR) continue;

		t_d_tx.setQuick(demandId, x_p_thisRoute.zDotProduct(aux_t_r_tx.viewSelection(demand2RouteIds[demandId])) / totalTraffic);
	    }
	}

	return t_d_tx.toArray();
    }

    /**
     * Returns the bifurcation degree for each demand. Bifurcation degree is equal
     * to the number of routes associated to a given demand which actually carry traffic.
     *
     * @return Bifurcation degree for each demand
     * @since 0.2.2
     */
    public int[] getDemandBifurcationDegreeVector()
    {
	if (bifurcationDegree == null)
	{
	    bifurcationDegree = new int[D];
	    for(int demandId = 0; demandId < D; demandId++)
	    {
		int[] candidateRoutes = demand2RouteIds[demandId];
		int routesCarryingTraffic = 0;
		for(int routeId : candidateRoutes)
		    if (x_p.getQuick(routeId) >= PRECISIONFACTOR)
			routesCarryingTraffic++;

		bifurcationDegree[demandId] = routesCarryingTraffic;
	    }
	}

	return IntUtils.copy(bifurcationDegree);
    }

    /**
     * Returns the buffering (or queuing) delay for each link.
     *
     * @return Buffering delay for each link
     * @since 0.2.2
     */
    public double[] getLinkBufferingDelayInSecondsVector()
    {
	if (t_e_buf == null)
	{
	    double[] aux_t_e_tx = getLinkTransmissionDelayInSecondsVector();
	    t_e_buf = DoubleFactory1D.dense.make(E);

	    for(int linkId = 0; linkId < E; linkId++)
		t_e_buf.setQuick(linkId, rho_e.getQuick(linkId) >= 1 ? Double.POSITIVE_INFINITY : aux_t_e_tx[linkId] * Math.pow(rho_e.getQuick(linkId), (1 / (2 * (1 - hurstParameter)))) / Math.pow(1 - rho_e.getQuick(linkId), hurstParameter / (1 - hurstParameter)));
	}

	return t_e_buf.toArray();
    }

    /**
     * Returns the propagation delay for each link.
     *
     * @return Propagation delay for each link
     * @since 0.2.2
     */
    public double[] getLinkPropagationDelayInSecondsVector()
    {
	if (t_e_prop == null)
	    t_e_prop = DoubleFactory1D.dense.make(DoubleUtils.divide(d_e.toArray(), v_prop));

	return t_e_prop.toArray();
    }

    /**
     * Returns the total delay (propagation + transmission + buffering) for each link.
     *
     * @return Total delay for each link
     * @since 0.2.2
     */
    public double[] getLinkTotalDelayInSecondsVector()
    {
	if (t_e == null)
        {
	    DoubleMatrix1D aux_t_e_prop = DoubleFactory1D.dense.make(getLinkPropagationDelayInSecondsVector());
	    DoubleMatrix1D aux_t_e_tx = DoubleFactory1D.dense.make(getLinkTransmissionDelayInSecondsVector());
	    DoubleMatrix1D aux_t_e_buf = DoubleFactory1D.dense.make(getLinkBufferingDelayInSecondsVector());
            
	    t_e = aux_t_e_prop.assign(aux_t_e_tx, DoubleFunctions.plus).assign(aux_t_e_buf, DoubleFunctions.plus);
        }

	return t_e.toArray();
    }

    /**
     * Returns the transmission delay for each link.
     *
     * @return Transmission delay for each link
     * @since 0.2.2
     */
    public double[] getLinkTransmissionDelayInSecondsVector()
    {
	if (t_e_tx == null)
	    t_e_tx = u_e_noProtection.copy().assign(DoubleFunctions.chain(DoubleFunctions.mult(secondsPerErlang), DoubleFunctions.inv));

	return t_e_tx.toArray();
    }

    /**
     * Returns the average network delay.
     *
     * @return Average network delay
     * @since 0.2.2
     */
    public double getNetworkAverageDelay()
    {
	if (T == -1)
	{
	    if (R_d < PRECISIONFACTOR)
	    {
		T = 0;
	    }
	    else
	    {
		DoubleMatrix1D aux_t_e = DoubleFactory1D.dense.make(getLinkTotalDelayInSecondsVector());
		T = y_e.zDotProduct(aux_t_e) / R_d;
	    }
	}

	return T;
    }
    
    /**
     * Returns the average network delay, considering only the propagation time.
     *
     * @return Average network delay (only considering propagation times)
     * @since 0.2.2
     */
    public double getNetworkAveragePropagationDelay()
    {
	if (T_onlyProp == -1)
	{
	    if (R_d < PRECISIONFACTOR)
	    {
		T_onlyProp = 0;
	    }
	    else
	    {
		DoubleMatrix1D aux_t_e_prop = DoubleFactory1D.dense.make(getLinkPropagationDelayInSecondsVector());
		T_onlyProp = y_e.zDotProduct(aux_t_e_prop) / R_d;
	    }
	}

	return T_onlyProp;
    }

    /**
     * Obtains the average route length among the current routes according to
     * certain link cost metric.
     * 
     * @param linkCostMetric Link cost metric (one per link)
     * @return Average route length
     * @since 0.2.2
     */
    public double getRouteAverageLength(double[] linkCostMetric)
    {
	if (R_d < PRECISIONFACTOR) return 0;

        if (linkCostMetric == null) linkCostMetric = DoubleUtils.ones(E);
        
        if (linkCostMetric.length != E) throw new Net2PlanException("Number of link-cost values doesn't match the number of links");

        double routeAverageLength = 0;
        for(int routeId = 0; routeId < R; routeId++)
            routeAverageLength += x_p.getQuick(routeId) * DoubleUtils.sum(DoubleUtils.select(linkCostMetric, routes.get(routeId)));

        routeAverageLength /= R_d;

	return routeAverageLength;
    }

    /**
     * Returns the buffering delay across the sequence of links for each route.
     * It is equal to the sum of the individual buffering delays per link.
     *
     * @return Buffering delay for each route
     * @since 0.2.2
     */
    public double[] getRouteBufferingDelayInSecondsVector()
    {
	if (t_p_buf == null)
	{
	    DoubleMatrix1D aux_t_e_buf = DoubleFactory1D.dense.make(getLinkBufferingDelayInSecondsVector());
	    t_p_buf = DoubleFactory1D.dense.make(R);

	    for(int routeId = 0; routeId < R; routeId++)
		t_p_buf.setQuick(routeId, aux_t_e_buf.viewSelection(routes.get(routeId)).zSum());
	}

	return t_p_buf.toArray();
    }

    /**
     * Returns the propagation delay across the sequence of links for each route.
     * It is equal to the sum of the individual propagation delays per link.
     *
     * @return Propagation delay for each route
     * @since 0.2.2
     */
    public double[] getRoutePropagationDelayInSecondsVector()
    {
	if (t_p_prop == null)
	{
	    DoubleMatrix1D aux_t_e_prop = DoubleFactory1D.dense.make(getLinkPropagationDelayInSecondsVector());
	    t_p_prop = DoubleFactory1D.dense.make(R);

	    for(int routeId = 0; routeId < R; routeId++)
		t_p_prop.setQuick(routeId, aux_t_e_prop.viewSelection(routes.get(routeId)).zSum());
	}

	return t_p_prop.toArray();
    }

    /**
     * Returns the total delay across the sequence of links for each route.
     * It is equal to the sum of the individual total delays per link.
     *
     * @return Total delay for each route
     * @since 0.2.2
     */
    public double[] getRouteTotalDelayInSecondsVector()
    {
	if (t_p == null)
        {
	    DoubleMatrix1D aux_t_p_prop = DoubleFactory1D.dense.make(getRoutePropagationDelayInSecondsVector());
	    DoubleMatrix1D aux_t_p_tx = DoubleFactory1D.dense.make(getRouteTransmissionDelayInSecondsVector());
	    DoubleMatrix1D aux_t_p_buf = DoubleFactory1D.dense.make(getRouteBufferingDelayInSecondsVector());
            
	    t_p = aux_t_p_prop.assign(aux_t_p_tx, DoubleFunctions.plus).assign(aux_t_p_buf, DoubleFunctions.plus);
        }

	return t_p.toArray();
    }

    /**
     * Returns the transmission delay across the sequence of links for each route.
     * It is equal to the sum of the individual transmission delays per link.
     *
     * @return Transmission delay for each route
     * @since 0.2.2
     */
    public double[] getRouteTransmissionDelayInSecondsVector()
    {
	if (t_p_tx == null)
	{
	    DoubleMatrix1D aux_t_e_tx = DoubleFactory1D.dense.make(getLinkTransmissionDelayInSecondsVector());
	    t_p_tx = DoubleFactory1D.dense.make(R);

	    for(int routeId = 0; routeId < R; routeId++)
		t_p_tx.setQuick(routeId, aux_t_e_tx.viewSelection(routes.get(routeId)).zSum());
	}

	return t_p_tx.toArray();
    }
    
    /**
     * Returns <code>true</code> if the link is a bottleneck (i.e. it has the maximum
     * utilization among all links), otherwise <code>false</code>.
     *
     * @param linkId Link identifier
     * @return <code>true</code> if the link is a bottleneck, otherwise <code>false</code>
     * @since 0.2.2
     */
    public boolean isBottleneckLink(int linkId)
    {
        return IntUtils.contains(bottleneckLinks, linkId);
    }

    /**
     * Returns <code>true</code> if the routing is bifurcated, otherwise <code>false</code>.
     *
     * @return <code>true</code> if at least one demand is bifurcated, otherwise <code>false</code>
     * @since 0.2.2
     */
    public boolean isRoutingBifurcated()
    {
        int[] aux_bifurcationDegree = getDemandBifurcationDegreeVector();
        
        if (aux_bifurcationDegree.length == 0 || IntUtils.maxValue(aux_bifurcationDegree) <= 1)
            return false;
        else
            return true;
    }

    /**
     * Computes the Kaufman-Roberts recursion for a multi-rate loss model system.
     *
     * @param u_e Link capacity (in integer units). It must be greater or equal than zero
     * @param h_p Traffic volume vector. Each element is referred to a connection of type <code>p</code> and must be greater than zero
     * @param s_p Capacity units occupied in the link by each accepted connection of type <code>p</code>. Each element must be greater or equal than one
     * @return Vector of connection blocking probability. Each element is referred to a connection of type <code>p</code>
     * @since 0.2.0
     */
    public static double[] kaufmanRobertsRecursion(int u_e, double[] h_p, int[] s_p)
    {
	// Sanity checks
	if (u_e <= 0) throw new Net2PlanException("Link capacity must be greater or equal than zero");
	if (h_p.length != s_p.length) throw new Net2PlanException("Length of offered traffic vector and connection size vector don't match");

	DoubleMatrix1D A_k = new DenseDoubleMatrix1D(h_p);
	IntMatrix1D b_k = new DenseIntMatrix1D(s_p);

	if (A_k.getMinLocation()[0] < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");
	if (b_k.getMinLocation()[0] < 1) throw new Net2PlanException("Connection size must be greater or equal than one");

	int K = h_p.length;
	int offset = b_k.getMaxLocation()[0];
	DoubleMatrix1D g = new DenseDoubleMatrix1D(1 + u_e + offset);
	g.assign(0);
	g.set(offset - 1, 1);

	for (int c = 1; c <= u_e; c++)
	{
	    for (int k = 1; k <= K; k++)
		g.set(offset + c - 1, g.get(offset + c - 1) + A_k.get(k - 1) * g.get(offset + c - b_k.get(k-1) - 1));

	    g.set(offset + c - 1, g.get(offset + c - 1) / c);
	}

	double sumG = g.zSum();

	DoubleMatrix1D pb_k = new DenseDoubleMatrix1D(K);
	pb_k.assign(0);
	for (int k = 1; k <= K; k++)
	    for (int c = u_e - b_k.get(k-1) + 1; c <= u_e; c++)
		pb_k.set(k - 1, pb_k.get(k - 1) + (1/sumG) * g.get(offset + c - 1));

	return pb_k.toArray();
    }

    /**
     * Returns the probability of call blocking in a <code>M/M/n/n</code> queue system using the efficient implementation presented in [1].
     *
     * @param numberOfServers Number of servers (i.e. link capacity in integer units). It must be greater or equal than zero
     * @param load Traffic load (i.e. carried traffic by the link). It must be greater or equal than zero
     * @return Call blocking probability
     * @since 0.2.0
     * @see <code>[1] S. Qiao, L. Qiao, "A Robust and Efficient Algorithm for Evaluating Erlang B Formula", Technical Report CAS98-03, McMaster University (Canada), October 1998</code>
     */
    public static double erlangBLossProbability(int numberOfServers, double load)
    {
	// Sanity checks
	if (numberOfServers < 0) throw new Net2PlanException("Number of server must be greater or equal than 0");
	if (load < 0) throw new Net2PlanException("System load must be greater or equal than 0");

	// Initialize output variable
	double gradeOfService = 0;

	// Compute the grade of service (or Erlang loss probability)
	if (load > 1E-10)
	{
	    double s = 0;
	    for (int i = 1; i <= numberOfServers; i++)
		s = (1 + s) * (i / load);

	    gradeOfService = 1 / (1 + s);
	}

	return gradeOfService;
    }

    /**
     * Returns the number of servers (i.e. link capacity) to achieve a given grade of service (i.e. call blocking probability) under a given load in a <code>M/M/n/n</code> queue system using the efficient implementation presented in [1].
     *
     * @param gradeOfService Grade of service (i.e. call blocking probability). It must be greater or equal than zero
     * @param load Traffic load (i.e. carried traffic by the link). It must be greater or equal than zero
     * @return Number of servers
     * @since 0.2.0
     * @see <code>[1] S. Qiao, L. Qiao, "A Robust and Efficient Algorithm for Evaluating Erlang B Formula", Technical Report CAS98-03, McMaster University (Canada), October 1998</code>
     */
    public static int inverseErlangB(double gradeOfService, double load)
    {
	// Sanity checks
	if (gradeOfService < 0) throw new Net2PlanException("Grade of service must be greater or equal than 0");
	if (load < 0) throw new Net2PlanException("System load must be greater or equal than 0");

	// Initialize output variable
	int numberOfServers = 0;

	// Compute the number of servers
	if (load > 1E-10)
	{
	    int l = 0;
	    int r = (int) Math.ceil(load);
	    double fR = NetworkPerformanceMetrics.erlangBLossProbability(r, load);

	    while (fR > gradeOfService)
	    {
		l = r;
		r += 32;
		fR = NetworkPerformanceMetrics.erlangBLossProbability(r, load);
	    }

	    while (r - l > 1)
	    {
		int m = (int) Math.ceil((double) (l + r) / 2);
		double fMid = NetworkPerformanceMetrics.erlangBLossProbability(m, load);
		if (fMid > gradeOfService) l = m;
		else r = m;
	    }

	    numberOfServers = r;
	}

	return numberOfServers;
    }
    
    private int[] convertSeqLinks2SeqNodes(int[] seqLinks)
    {
        IntArrayList seqNodes = new IntArrayList();
        seqNodes.add(linkTable[seqLinks[0]][0]);
        
        for(int hopId = 0; hopId < seqLinks.length; hopId++)
            seqNodes.add(linkTable[seqLinks[hopId]][1]);
        
        seqNodes.trimToSize();
            
        return seqNodes.elements();
    }
    
    /**
     * Returns the percentage of blocked traffic.
     * 
     * @return Percentage of blocked traffic
     * @since 0.2.2
     */
    public double getBlockedTrafficPercentage()
    {
        double accum_lostTraffic = 0;
        for(int demandId = 0; demandId < D; demandId++)
            accum_lostTraffic += Math.max(h_d.getQuick(demandId) - r_d.getQuick(demandId), 0);
        
        if (H_d == 0)
        {
            if (accum_lostTraffic == 0) return 0;
            else return 100;
        }
        else
        {
            return 100.0 * accum_lostTraffic / H_d;
        }
    }
    
    /**
     * <p>Returns the statistics for protection degree carried traffic. Returned
     * values are the following:</p>
     * 
     * <ul>
     * <li>Unprotected: Percentage of carried traffic which is not backed up by protection segments</li>
     * <li>Complete and dedicated protection: Percentage of carried traffic which
     * has one or more dedicated protection segments covering the whole route with enough bandwidth</li>
     * <li>Partial and/or shared protection: For cases not covered by the above definitions</li>
     * </ul>
     * 
     * @return Traffic protection degree
     * @since 0.2.2
     */
    public double[] getTrafficProtectionDegree()
    {
        double percentageUnprotected = 0;
        double percentageDedicated = 0;
        double percentageShared = 0;

        for(int routeId = 0; routeId < R; routeId++)
        {
            int[] segmentIds_thisRoute = routeBackupSegments.get(routeId);
            if (segmentIds_thisRoute.length == 0)
            {
                percentageUnprotected += x_p.getQuick(routeId);
                continue;
            }
            
            int[] sequenceOfLinks_thisRoute = routes.get(routeId);
            int[] sequenceOfNodes_thisRoute = convertSeqLinks2SeqNodes(sequenceOfLinks_thisRoute);

            double u_s_reservedForProtection_thisRoute_max = u_s_reservedForProtection.viewSelection(segmentIds_thisRoute).getMaxLocation()[0];
            double u_s_reservedForProtection_thisRoute_min = u_s_reservedForProtection.viewSelection(segmentIds_thisRoute).getMinLocation()[0];

            if (u_s_reservedForProtection_thisRoute_min < PRECISIONFACTOR)
            {
                percentageUnprotected += x_p.getQuick(routeId);
            }
            else
            {
                int[] dedicatedSegmentIds_thisRoute = IntUtils.intersect(segmentIds_thisRoute, dedicatedSegments);
                boolean isCompleteAndDedicated = false;

                if (dedicatedSegmentIds_thisRoute.length > 0 && u_s_reservedForProtection_thisRoute_max > x_p.getQuick(routeId) - PRECISIONFACTOR)
                {
                    Set<Integer> totallyProtectedLinks = new HashSet<Integer>();
                    for(int segmentId : dedicatedSegmentIds_thisRoute)
                    {
                        if (u_s_reservedForProtection.getQuick(segmentId) > x_p.getQuick(routeId) - PRECISIONFACTOR)
                        {
                            int[] seqLinks = segments.get(segmentId);
                            int[] seqNodes = convertSeqLinks2SeqNodes(seqLinks);
                            int ingressNodeId = seqNodes[0];
                            int egressNodeId = seqNodes[seqNodes.length - 1];

                            int head_pos = IntUtils.find(sequenceOfNodes_thisRoute, ingressNodeId, Constants.SearchType.FIRST)[0];
                            int tail_pos = IntUtils.find(sequenceOfNodes_thisRoute, egressNodeId, Constants.SearchType.LAST)[0];

                            for(int pos = head_pos; pos < tail_pos; pos++)
                                totallyProtectedLinks.add(sequenceOfLinks_thisRoute[pos]);
                        }
                    }

                    int[] nonTotallyProtectedLinks = IntUtils.setdiff(sequenceOfLinks_thisRoute, IntUtils.toArray(totallyProtectedLinks));

                    if (nonTotallyProtectedLinks.length == 0) isCompleteAndDedicated = true;
                }

                if (isCompleteAndDedicated) percentageDedicated += x_p.getQuick(routeId);
                else percentageShared += x_p.getQuick(routeId);
            }
        }

        percentageUnprotected = R_d == 0 ? 0 : 100 * percentageUnprotected / R_d;
        percentageDedicated = R_d == 0 ? 0 : 100 * percentageDedicated / R_d;
        percentageShared = R_d == 0 ? 0 : 100 * percentageShared / R_d;

        return new double[] { percentageUnprotected, percentageDedicated, percentageShared };
    }
    
    /**
     * Returns the average offered traffic between every node pair. It is equal
     * to the total ingress traffic to the network divided by the total number
     * of node pairs (<i>Nx(N-1)</i>, where <i>N</i> is the number of nodes.
     * 
     * @return Average offered traffic per node pair
     * @since 0.2.2
     */
    public double getNodePairAverageOfferedTrafficInErlangs()
    {
        return N == 0 ? 0 : H_d / (N*(N-1));
    }

    /**
     * Indicates whether SRG definition follows one of the predefined models (per 
     * node, per link...), or 'Mixed' otherwise (or 'None' if no SRGs are defined).
     * 
     * @return Description of the current SRG model
     * @since 0.2.3
     */
    public String getSRGModel()
    {
        boolean isAnOneSRGPerNodeModel = true;
        
        Set<Integer> nodes = new HashSet<Integer>();
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = N_f[srgId];
            int[] linkIds = E_f[srgId];
            
            if (linkIds.length > 0)
            {
                isAnOneSRGPerNodeModel = false;
                break;
            }
            
            switch(nodeIds.length)
            {
                case 0:
                    continue;
                    
                case 1:
                    if (nodes.contains(nodeIds[0])) isAnOneSRGPerNodeModel = false;
                    else nodes.add(nodeIds[0]);
                    break;
                    
                default:
                    isAnOneSRGPerNodeModel = false;
                    break;
            }
            
            if (!isAnOneSRGPerNodeModel) break;
        }
        

        if (nodes.size() != N) isAnOneSRGPerNodeModel = false;
        
        nodes.clear();
        
        boolean isAnOneSRGPerLinkModel = true;

        Set<Integer> links = new HashSet<Integer>();
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = N_f[srgId];
            int[] linkIds = E_f[srgId];
            
            if (nodeIds.length > 0)
            {
                isAnOneSRGPerLinkModel = false;
                break;
            }

            switch(linkIds.length)
            {
                case 0:
                    continue;
                    
                case 1:
                    if (links.contains(linkIds[0])) isAnOneSRGPerLinkModel = false;
                    else links.add(linkIds[0]);
                    break;
                    
                default:
                    isAnOneSRGPerLinkModel = false;
                    break;
            }
            
            if (!isAnOneSRGPerLinkModel) break;
        }

        if (links.size() != E) isAnOneSRGPerLinkModel = false;
        
        links.clear();

        boolean isAnOneSRGPerLinkBundleModel = true;
        
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = N_f[srgId];
            int[] linkIds = E_f[srgId];
            
            if (nodeIds.length > 0)
            {
                isAnOneSRGPerLinkBundleModel = false;
                break;
            }

            switch(linkIds.length)
            {
                case 0:
                    continue;
                    
                default:
                    int originNodeId = linkTable[linkIds[0]][0];
                    int destinationNodeId = linkTable[linkIds[0]][1];
                    
                    List<Integer> actualLinkIds = new LinkedList<Integer>();
                    
                    for(int linkId = 0; linkId < E; linkId++)
                    {
                        int originNodeIdThisLink = linkTable[linkId][0];
                        int destinationNodeIdThisLink = linkTable[linkId][1];
                        
                        if (originNodeIdThisLink == originNodeId && destinationNodeIdThisLink == destinationNodeId)
                            actualLinkIds.add(linkId);
                    }
                    
                    for(int linkId : actualLinkIds)
                    {
                        if (links.contains(linkId)) isAnOneSRGPerLinkBundleModel = false;
                        if (!IntUtils.contains(linkIds, linkId)) isAnOneSRGPerLinkBundleModel = false;
                        
                        if (!isAnOneSRGPerLinkBundleModel) break;
                        
                        links.add(linkId);
                    }
                    
                    break;
            }

            if (!isAnOneSRGPerLinkBundleModel) break;
        }
        
        if (links.size() != E) isAnOneSRGPerLinkBundleModel = false;
        
        links.clear();

        boolean isAnOneSRGPerBidiLinkBundleModel = true;
        
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = N_f[srgId];
            int[] linkIds = E_f[srgId];
            
            if (nodeIds.length > 0)
            {
                isAnOneSRGPerBidiLinkBundleModel = false;
                break;
            }

            switch(linkIds.length)
            {
                case 0:
                    continue;
                    
                default:
                    int originNodeId = linkTable[linkIds[0]][0];
                    int destinationNodeId = linkTable[linkIds[0]][1];
                    
                    Set<Integer> actualLinkIds = new HashSet<Integer>();
                    
                    for(int linkId = 0; linkId < E; linkId++)
                    {
                        int originNodeIdThisLink = linkTable[linkId][0];
                        int destinationNodeIdThisLink = linkTable[linkId][1];
                        
                        if ( (originNodeIdThisLink == originNodeId && destinationNodeIdThisLink == destinationNodeId) ||
                            (originNodeIdThisLink == destinationNodeId && destinationNodeIdThisLink == originNodeId) )
                            actualLinkIds.add(linkId);
                    }
                    
                    for(int linkId : actualLinkIds)
                    {
                        if (links.contains(linkId)) isAnOneSRGPerBidiLinkBundleModel = false;
                        if (!IntUtils.contains(linkIds, linkId)) isAnOneSRGPerBidiLinkBundleModel = false;
                        
                        if (!isAnOneSRGPerBidiLinkBundleModel) break;
                        
                        links.add(linkId);
                    }
                    
                    break;
            }

            if (!isAnOneSRGPerBidiLinkBundleModel) break;
        }
        
        if (links.size() != E) isAnOneSRGPerBidiLinkBundleModel = false;
        
        links.clear();
        
        String srgModel;
        if (isAnOneSRGPerNodeModel && (!isAnOneSRGPerLinkModel && !isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerBidiLinkBundleModel)) srgModel = "One SRG per node";
        else if (isAnOneSRGPerBidiLinkBundleModel && !isAnOneSRGPerNodeModel) srgModel = "One SRG per bidirectional link bundle";
        else if (isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerNodeModel && !isAnOneSRGPerBidiLinkBundleModel) srgModel = "One SRG per unidirectional link bundle";
        else if (isAnOneSRGPerLinkModel && !isAnOneSRGPerNodeModel && !isAnOneSRGPerLinkBundleModel && !isAnOneSRGPerBidiLinkBundleModel) srgModel = "One SRG per unidirectional link";
        else srgModel = numSRGs > 0 ? "Mixed" : "None";
        
        return srgModel;
    }
    
    /**
     * Returns the percentage of SRG disjointness of traffic routes and
     * protection segments.
     * 
     * @return Two SRG disjointness values (either considering or not end nodes)
     * @since 0.2.3
     */
    public double[] getSRGDisjointnessPercentage()
    {
        int accum_routeSRGDisjoint_withEndNodes = 0;
        int accum_routeSRGDisjoint_withoutEndNodes = 0;
        
        for(int routeId = 0; routeId < R; routeId++)
        {
            int[] segmentIds = routeBackupSegments.get(routeId);
            if (segmentIds.length == 0) continue;
            
            boolean srgDisjoint_withEndNodes = true;
            boolean srgDisjoint_withoutEndNodes = true;
            
            int[] linkIds = routes.get(routeId);
            int[] nodeIds = convertSeqLinks2SeqNodes(linkIds);
            
            Set<Integer> aux_segmentSRGIds = new HashSet<Integer>();
            for(int segmentId : segmentIds)
            {
                int[] srgIds = F_s[segmentId];
                aux_segmentSRGIds.addAll(IntUtils.toList(srgIds));
            }
            
            int[] segmentSRGIds = IntUtils.toArray(aux_segmentSRGIds);
            int[] routeSRGIds = F_r[routeId];
            
            int[] commonSRGs_withEndNodes = IntUtils.intersect(routeSRGIds, segmentSRGIds);
            if (commonSRGs_withEndNodes.length > 0) srgDisjoint_withEndNodes = false;
            
            Set<Integer> aux_routeSRGIds_withoutEndNodes = new HashSet<Integer>();
            for(int hopId = 0; hopId < linkIds.length; hopId++)
            {
                int linkId = linkIds[hopId];
                aux_routeSRGIds_withoutEndNodes.addAll(IntUtils.toList(F_e[linkId]));
                
                if (hopId == 0) continue;

                int nodeId = nodeIds[hopId];
                aux_routeSRGIds_withoutEndNodes.addAll(IntUtils.toList(F_n[nodeId]));
            }
            
            int[] routeSRGIds_withoutEndNodes = IntUtils.toArray(aux_routeSRGIds_withoutEndNodes);
            
            int[] commonSRGs_withoutEndNodes = IntUtils.intersect(routeSRGIds_withoutEndNodes, segmentSRGIds);
            if (commonSRGs_withoutEndNodes.length > 0) srgDisjoint_withoutEndNodes = false;

            if (srgDisjoint_withEndNodes) accum_routeSRGDisjoint_withEndNodes++;
            if (srgDisjoint_withoutEndNodes) accum_routeSRGDisjoint_withoutEndNodes++;
        }
        
        double percentageRouteSRGDisjointness_withoutEndNodes = R > 0 ? 100 * accum_routeSRGDisjoint_withoutEndNodes / (double) R : 0;
        double percentageRouteSRGDisjointness_withEndNodes = R > 0 ? 100 * accum_routeSRGDisjoint_withEndNodes / (double) R : 0;
        
        return new double[] { percentageRouteSRGDisjointness_withEndNodes, percentageRouteSRGDisjointness_withoutEndNodes };
    }
}