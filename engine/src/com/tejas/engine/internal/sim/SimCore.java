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

package com.tejas.engine.internal.sim;

import com.tejas.engine.internal.sim.EndSimulationException;
import com.tejas.engine.internal.sim.FutureEventList;
import com.tejas.engine.internal.sim.IEventCallback;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.SystemUtils.UserInterface;

import java.util.List;

/**
 * Core-class of the discrete event simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class SimCore implements Runnable
{
    private final IEventCallback callback;
    private final FutureEventList futureEventList;
    private double cpuTime;
    private double refreshTimeInSeconds;
    private double timeSinceLastRefresh;

    private long totalSimEvents;
    private long totalTransitoryEvents;
    private double totalSimTime;
    private double totalTransitoryTime;
    
    private boolean isInTransitory;

    public enum SimState {NOT_STARTED, RUNNING, STEP, PAUSED, STOPPED};
    private SimState simulationState;
    
    private boolean processingEvent;
    
    public FutureEventList getFutureEventList() { return futureEventList; }

    public SimCore(IEventCallback callback)
    {
	this.callback = callback;
	futureEventList = new FutureEventList();

	reset();
    }
    
    private void checkSimulationNotStartedYet()
    {
	if (simulationState != SimState.NOT_STARTED)
	    throw new RuntimeException("Simulation was already started. No configuration changes allowed");
    }

    public void setTotalSimulationEvents(long totalSimEvents)
    {
        checkSimulationNotStartedYet();
        
        if (totalSimEvents <= 0 && totalSimEvents != -1)
            throw new Net2PlanException("'totalSimEvents' must be in range [1, Long.MAX_VALUE], or -1 for no limit");
        
        this.totalSimEvents = totalSimEvents;
    }

    public void setTotalSimulationTime(double totalSimTime)
    {
        checkSimulationNotStartedYet();
        
        if (totalSimTime <= 0 && totalSimTime != -1)
            throw new Net2PlanException("'totalSimTime' must be in range (0, Double.MAX_VALUE], or -1 for no limit");
        
        this.totalSimTime = totalSimTime;
    }

    public void setTotalTransitoryTime(double totalTransitoryTime)
    {
        checkSimulationNotStartedYet();
        
        if (totalTransitoryTime <= 0 && totalTransitoryTime != -1)
            throw new Net2PlanException("'totalTransitoryTime' must be in range (0, Double.MAX_VALUE], or -1 for no transitory");
        
        this.totalTransitoryTime = totalTransitoryTime;
    }

    public void setTotalTransitoryEvents(long totalTransitoryEvents)
    {
        checkSimulationNotStartedYet();
        
        if (totalTransitoryEvents <= 0 && totalTransitoryEvents != -1)
            throw new Net2PlanException("'totalTransitoryEvents' must be in range [1, Long.MAX_VALUE], or -1 for no transitory");
        
        this.totalTransitoryEvents = totalTransitoryEvents;
    }

    public void setRefreshTimeInSeconds(double refreshTimeInSeconds)
    {
        checkSimulationNotStartedYet();
        
        if (refreshTimeInSeconds < 0)
            throw new Net2PlanException("'refreshTimeInSeconds' must be in range [0, Double.MAX_VALUE]. To avoid refreshing set it to an arbitrarely large value");
        
        this.refreshTimeInSeconds = refreshTimeInSeconds;
    }

    /**
     *
     * @return
     */
    public double getCurrentCPUTime()
    {
	return cpuTime;
    }

    public SimState getSimulationState()
    {
	return simulationState;
    }

    public void setSimulationState(SimState simulationState)
    {
	setSimulationState(simulationState, null);
    }
    
    private void setSimulationState(SimState simulationState, Throwable reason)
    {
	this.simulationState = simulationState;
        
        while(processingEvent)
        {
            try { Thread.sleep(1); }
            catch(Throwable e) {}
        }
        
	callback.simulationStateChanged(simulationState, reason);
    }
    
    
    /**
     * Resets the simulation.
     *
     * @since 0.2.2
     */
    public void reset()
    {
	cpuTime = 0;
	futureEventList.reset();
	timeSinceLastRefresh = 0;
        
        refreshTimeInSeconds = 60;
        totalSimEvents = -1;
        totalTransitoryEvents = -1;
        totalSimTime = -1;
        totalTransitoryTime = -1;
        isInTransitory = true;

        processingEvent = false;

        setSimulationState(SimState.NOT_STARTED);
    }

    @Override
    public void run()
    {
        if (simulationState == SimState.NOT_STARTED)
            throw new RuntimeException("Bad - Simulation not started yet");
        
        isInTransitory = true;
        if (totalTransitoryEvents == -1 && totalTransitoryTime == -1) isInTransitory = false;
        
	while(simulationState != SimState.STOPPED)
	{
	    while(futureEventList.hasMoreEvents())
	    {
                synchronized(callback)
                {
                    double nextEventTime = futureEventList.getNextEventSimulationTime();
                    if (nextEventTime == -1) throw new RuntimeException("Bad");
                    
                    if (isInTransitory)
                    {
                        if (totalTransitoryTime != -1 && nextEventTime >= totalTransitoryTime)
                        {
                            callback.endOfTransitory(totalTransitoryTime);
                            isInTransitory = false;
                        }
                        else if (totalTransitoryEvents != -1 && futureEventList.getNumberOfProcessedEvents() == totalTransitoryEvents)
                        {
                            callback.endOfTransitory(futureEventList.getCurrentSimulationTime());
                            isInTransitory = false;
                        }
                    }
                    
                    if (totalSimTime != -1 && nextEventTime >= totalSimTime)
                    {
                        setSimulationState(SimState.STOPPED, new EndSimulationException());
                        return;
                    }
                    else if (totalSimEvents != -1 && futureEventList.getNumberOfProcessedEvents() == totalSimEvents)
                    {
                        setSimulationState(SimState.STOPPED, new EndSimulationException());
                        return;
                    }

                    // Process next event in the future event list
                    long start = System.nanoTime();
                    
                    SimEvent event = futureEventList.getNextEvent();
                    
//                    if (event instanceof CACEvent.EndSimulation)
//                    {
//                        setSimulationState(SimState.STOPPED);
//                        return;
//                    }
//                    else if (event instanceof SimEvent.EndTransitory)
//                    {
//                        callback.endOfTransitory(futureEventList.getCurrentSimulationTime());
//                    }
                    
                    processingEvent = true;

                    try
                    {
                        if (event == null) throw new RuntimeException("Event is a null object");

                        List<SimEvent> newEvents = callback.processEvent(event);
                        futureEventList.addEvents(newEvents);
                    }
                    catch (Throwable e)
                    {
                        processingEvent = false;
                        setSimulationState(SimCore.SimState.STOPPED, e);

                        long end = System.nanoTime();
                        cpuTime += ((double) (end - start)) / 1e9;
                        callback.refresh(true);
                        
                        return;
                    }
                    
                    processingEvent = false;
                    long end = System.nanoTime();

                    cpuTime += ((double) (end - start)) / 1e9;
                    
                    if (cpuTime - timeSinceLastRefresh >= refreshTimeInSeconds)
                    {
                        callback.refresh(false);
                        timeSinceLastRefresh = cpuTime;
                    }
                    
                    if (futureEventList.getNumberOfProcessedEvents() == Long.MAX_VALUE)
                    {
                        setSimulationState(SimState.STOPPED);
                        return;
                    }

                    if (simulationState == SimState.STEP)
                        setSimulationState(SimState.PAUSED);

                    if (simulationState != SimState.RUNNING)
                        break;
                }
	    }
            
            callback.refresh(true);
            timeSinceLastRefresh = cpuTime;
            
            if (SystemUtils.getUserInterface() == UserInterface.CLI) setSimulationState(SimState.STOPPED, new EndSimulationException());
            
            simulationState = SimState.PAUSED;
            
	    while (simulationState == SimState.PAUSED)
	    {
		try
		{
		    Thread.sleep(1);
		}
		catch (InterruptedException ex)
		{
		    setSimulationState(SimState.STOPPED);
		    break;
		}
	    }
	}
    }
}