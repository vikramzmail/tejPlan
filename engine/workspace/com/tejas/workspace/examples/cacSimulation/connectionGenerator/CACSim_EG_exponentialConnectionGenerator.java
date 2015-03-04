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

package com.tejas.workspace.examples.cacSimulation.connectionGenerator;

import cern.jet.random.tdouble.AbstractDoubleDistribution;
import cern.jet.random.tdouble.Exponential;
import cern.jet.random.tdouble.engine.MersenneTwister64;

import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.cacSimulation.IConnectionEventGenerator;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.RandomUtils;
import com.tejas.engine.utils.Triple;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Connection generator in which connection requests from traffic demands arrive
 * according to a Poisson process and are independent of each other.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class CACSim_EG_exponentialConnectionGenerator implements IConnectionEventGenerator
{
    private AbstractDoubleDistribution[] iat_d;
    private AbstractDoubleDistribution[] ht_d;
    private double[] s_d;
    
    @Override
    public String getDescription()
    {
        return "Connection generator in which connection requests from traffic "
                + "demands arrive according to a Poisson process and are "
                + "independent of each other";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
        parameters.add(Triple.of("defaultConnectionSize", "1", "Default requested traffic volume per connection (in Erlangs)"));
        parameters.add(Triple.of("defaultHoldingTime", "120", "Default average connection duration (in seconds)"));
	parameters.add(Triple.of("randomSeed", "-1", "Seed for the random generator (-1 means random)"));
        
        return parameters;
    }

    @Override
    public List<CACEvent> initialize(NetPlan netPlan, ConnectionNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        int D = netPlan.getNumberOfDemands();
        if (D == 0) throw new Net2PlanException("No demands are defined");
        
	long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
	if (seed == -1) seed = RandomUtils.random(0, Long.MAX_VALUE - 1);
        Random seedGenerator = new Random(seed);
        
        int defaultConnectionSize = Integer.parseInt(algorithmParameters.get("defaultConnectionSize"));
        if (defaultConnectionSize <= 0) throw new Net2PlanException("'defaultConnectionSize' must be greater than zero");
        
        double defaultHoldingTime = Double.parseDouble(algorithmParameters.get("defaultHoldingTime"));
        if (defaultHoldingTime <= 0) throw new Net2PlanException("'defaultHoldingTime' must be greater than zero");
        
        iat_d = new AbstractDoubleDistribution[D];
        ht_d = new AbstractDoubleDistribution[D];
        s_d = new double[D];

        List<CACEvent> events = new LinkedList<CACEvent>();
        
        for(int demandId = 0; demandId < D; demandId++)
        {
	    double h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);
            
            String aux_str_s_d = netPlan.getDemandAttribute(demandId, "connectionSize");
            double aux_s_d;
            try { aux_s_d = Double.parseDouble(aux_str_s_d); }
            catch(Exception ex) { aux_s_d = defaultConnectionSize; }
            if (aux_s_d <= 0) throw new Net2PlanException("'connectionSize' for demand " + demandId + " is lower or equal than zero");
            
            String aux_str_holdTime = netPlan.getDemandAttribute(demandId, "holdingTime");
            double aux_holdTime;
            try { aux_holdTime = Double.parseDouble(aux_str_holdTime); }
            catch(Exception ex) { aux_holdTime = defaultHoldingTime; }
            if (aux_holdTime <= 0) throw new Net2PlanException("'holdingTime' for demand " + demandId + " is lower or equal than zero");
            
            s_d[demandId] = aux_s_d;
            double holdingTime = aux_holdTime;
            double interArrivalTime = s_d[demandId] * holdingTime / h_d;
            
            iat_d[demandId] = new Exponential(1 / interArrivalTime, new MersenneTwister64(seedGenerator.nextInt()));
            ht_d[demandId] = new Exponential(1 / holdingTime, new MersenneTwister64(seedGenerator.nextInt()));
            
            events.add(scheduleNewConnectionArrival(demandId, 0));
        }
        
        return events;
    }

    @Override
    public List<CACEvent> processEvent(NetPlan netPlan, ConnectionNetState netState, CACEvent event)
    {
	List<CACEvent> events = new LinkedList<CACEvent>();

	if (event.getEventType() == CACEvent.EventType.CONNECTION_REQUEST)
	{
            int demandId = event.getRequestDemandId();
            double simTime = event.getEventTime();
            
            events.add(scheduleNewConnectionArrival(demandId, simTime));
        }

	return events;
    }

    private CACEvent scheduleNewConnectionArrival(int demandId, double simTime)
    {
        double nextArrivalTime = simTime + iat_d[demandId].nextDouble();
        double nextDuration = ht_d[demandId].nextDouble();
        
        double trafficVolumeInErlangs = s_d[demandId];
        Map<String, String> attributes = new HashMap<String, String>();
        
        return new CACEvent(nextArrivalTime, demandId, nextDuration, trafficVolumeInErlangs, attributes);
    }
}