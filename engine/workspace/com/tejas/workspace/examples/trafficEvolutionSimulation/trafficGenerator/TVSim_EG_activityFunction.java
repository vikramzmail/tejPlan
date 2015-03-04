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

package com.tejas.workspace.examples.trafficEvolutionSimulation.trafficGenerator;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficGenerator;
import com.tejas.engine.libraries.TrafficMatrixGenerationModels;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.RandomUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>This class modifies the offered traffic volume per demand according to an activity function (see [<a href='#Milbrandt2005'>1</a>]) with respect to the timezone.</p>
 *
 * <p>The timezone of each node is represented as the time offset respecting to Coordinated Universal Time (UTC) and it is between -12 and 12.</p>
 *
 * @see <a name='Milbrandt2005' />[1] J. Milbrandt, M. Menth, S. Kopf, "Adaptive Bandwidth Allocation: Impact of Traffic Demand Models for Wide Area Networks," University of Würzburg, Germany, Report No. 363, June 2005
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class TVSim_EG_activityFunction implements ITrafficGenerator
{
    private double sigma;
    private double threshold;
    private double[] h_d;
    private Random r;
    private int[] timezones;

    @Override
    public double[] execute(NetPlan netPlan, Calendar calendar)
    {
	int D = h_d.length;

	int hours = calendar.get(Calendar.HOUR_OF_DAY);
	int minutes = calendar.get(Calendar.MINUTE);
	int seconds = calendar.get(Calendar.SECOND);
	int weekday = calendar.get(Calendar.DAY_OF_WEEK);

	double UTC = hours + (double) minutes / 60 + (double) seconds / 3600;

	int N = netPlan.getNumberOfNodes();
	double[] activityFactor = new double[N];
	if (weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY)
	{
	    for(int nodeId = 0; nodeId < N; nodeId++)
		activityFactor[nodeId] = TrafficMatrixGenerationModels.activityFactor(UTC, timezones[nodeId], 0.3, 0.5);
	}
	else
	{
	    for(int nodeId = 0; nodeId < N; nodeId++)
		activityFactor[nodeId] = TrafficMatrixGenerationModels.activityFactor(UTC, timezones[nodeId], 0.3, 1);
	}

	double[] aux_h_d = DoubleUtils.copy(h_d);

	for(int demandId = 0; demandId < D; demandId++)
	{
	    int ingressNodeId = netPlan.getDemandIngressNode(demandId);
	    int egressNodeId = netPlan.getDemandEgressNode(demandId);

	    double activityFactorNodePair = (activityFactor[ingressNodeId] + activityFactor[egressNodeId]) / 2;

	    double variation = r.nextGaussian() * sigma;
	    if (variation > threshold) variation = threshold;
	    else if (variation < -threshold) variation = -threshold;

	    activityFactorNodePair += variation;
	    if (activityFactorNodePair < 0) activityFactorNodePair = 0;

	    aux_h_d[demandId] *= activityFactorNodePair;
	}

	return aux_h_d;
    }

    @Override
    public String getDescription()
    {
	StringBuilder description = new StringBuilder();
	description.append("This class modifies the offered traffic volume per demand according to an activity function.");

	return description.toString();
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
	parameters.add(Triple.of("defaultTimezone", "0", "Default timezone with respect to UTC (in range [-12, 12])"));
	parameters.add(Triple.of("sigma", "0.05", "Standard deviation from normalized peak traffic"));
	parameters.add(Triple.of("threshold", "0.25", "Maximum variation of the random noise"));
	parameters.add(Triple.of("randomSeed", "-1", "Seed for the random generator (-1 means random)"));

	return parameters;
    }

    @Override
    public void initialize(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
	if (seed == -1) seed = RandomUtils.random(0, Long.MAX_VALUE - 1);
	r = new Random(seed);
        
	int defaultTimezone = Integer.parseInt(algorithmParameters.get("defaultTimezone"));
	if (defaultTimezone < -12 || defaultTimezone > 12) throw new Net2PlanException("'defaultTimezone' must be in range [-12, 12]");
        
        sigma = Double.parseDouble(algorithmParameters.get("sigma"));
	if (sigma < 0) throw new Net2PlanException("'sigma' must be a non-negative number");
        threshold = Double.parseDouble(algorithmParameters.get("threshold"));
	if (threshold < 0) throw new Net2PlanException("'threshold' must be a non-negative number");

	timezones = StringUtils.toIntArray(netPlan.getNodeAttributeVector("timezone"), defaultTimezone);
	int N = netPlan.getNumberOfNodes();

	for(int nodeId = 0; nodeId < N; nodeId++)
	    if (timezones[nodeId] < -12 || timezones[nodeId] > 12)
		throw new Net2PlanException(String.format("Timezone for node %d must be in range [-12, 12]", nodeId));

	h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
    }
}
