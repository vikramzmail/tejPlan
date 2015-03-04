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

package com.tejas.workspace.examples.resilienceSimulation.eventGenerator;

import cern.jet.random.tdouble.AbstractDoubleDistribution;
import cern.jet.random.tdouble.Exponential;
import cern.jet.random.tdouble.engine.MersenneTwister64;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IResilienceEventGenerator;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.utils.RandomUtils;
import com.tejas.engine.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates random failures in SRGs according to a random process with an exponential distribution with MTTF and MTTR mean values.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 */
public class NRSim_EG_exponentialSRGFailureGenerator implements IResilienceEventGenerator
{
    private AbstractDoubleDistribution[] upPeriodDuration_srg;
    private AbstractDoubleDistribution[] downPeriodDuration_srg;
    
    @Override
    public String getDescription()
    {
	return "Generates random failures in SRGs according to a random process with an exponential distribution with MTTF and MTTR mean values.";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
	parameters.add(Triple.of("randomSeed", "-1", "Seed for the random generator (-1 means random)"));
        
        return parameters;
    }

    @Override
    public List<ResilienceEvent> initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        int numSRGs = netPlan.getNumberOfSRGs();
        if (numSRGs == 0) throw new Net2PlanException("No SRGs are defined");
        
	long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
	if (seed == -1) seed = RandomUtils.random(0, Long.MAX_VALUE - 1);
        Random seedGenerator = new Random(seed);
        
        List<ResilienceEvent> events = new LinkedList<ResilienceEvent>();
        
        upPeriodDuration_srg = new AbstractDoubleDistribution[numSRGs];
        downPeriodDuration_srg = new AbstractDoubleDistribution[numSRGs];
        
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            double mttf = netPlan.getSRGMeanTimeToFailInHours(srgId);
            double mttr = netPlan.getSRGMeanTimeToRepairInHours(srgId);
            
            upPeriodDuration_srg[srgId] = new Exponential(1 / mttf, new MersenneTwister64(seedGenerator.nextInt()));
            downPeriodDuration_srg[srgId] = new Exponential(1 / mttr, new MersenneTwister64(seedGenerator.nextInt()));
            
            events.addAll(scheduleNewFailureAndReparation(srgId, 0));
        }
        
        return events;
    }

    @Override
    public List<ResilienceEvent> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event)
    {
	List<ResilienceEvent> events = new LinkedList<ResilienceEvent>();

	if (event.getEventType() == ResilienceEvent.EventType.SRG_REPARATION)
	{
	    int srgId = event.getId();
	    double simTime = event.getEventTime();
            
            events.addAll(scheduleNewFailureAndReparation(srgId, simTime));
	}

	return events;
    }

    private List<ResilienceEvent> scheduleNewFailureAndReparation(int srgId, double simTime)
    {
        double nextSRGFailureTime = simTime + 3600 * upPeriodDuration_srg[srgId].nextDouble();
        double nextSRGReparationTime = nextSRGFailureTime + 3600 * downPeriodDuration_srg[srgId].nextDouble();
        
        List<ResilienceEvent> events = new LinkedList<ResilienceEvent>();
        events.add(new ResilienceEvent(nextSRGFailureTime, srgId, ResilienceEvent.EventType.SRG_FAILURE));
        events.add(new ResilienceEvent(nextSRGReparationTime, srgId, ResilienceEvent.EventType.SRG_REPARATION));
        
        return events;
    }
}