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

package com.tejas.engine.internal.sim.impl;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficAllocationAlgorithm;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficGenerator;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficChangeEvent;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.NetState;
import com.tejas.engine.internal.sim.SimCore;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimKernel;
import com.tejas.engine.internal.sim.stats.NetworkEvolution;
import com.tejas.engine.internal.sim.stats.SimStats;
import com.tejas.engine.utils.Triple;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Simulates the network operation, where traffic demand volumes vary with time
 * according to a user-defined pattern. Targeted to evaluate the performances of
 * built-in or user-defined schemes that react to traffic variations (i.e. traffic
 * rerouting schemes, on-demand capacity-provisioning schemes, etc.).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class TimeVaryingTrafficSimulation extends SimKernel
{
    private TimeVaryingNetState netState;
    private List<SimStats> stats;

    private Calendar currentDate;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public TimeVaryingTrafficSimulation()
    {
        super();
        
	stats = new LinkedList<SimStats>();
    }

    @Override
    public void initializeNetState()
    {
        if (getSimCore().getSimulationState() != SimCore.SimState.NOT_STARTED)
            throw new Net2PlanException("Network state cannot be re-initialized"
                    + "once the simulation was started");
        
	netState = new TimeVaryingNetState(netPlan);
    }

    @Override
    public List<? extends SimEvent> checkEvents(List<? extends SimEvent> events)
    {
        for(SimEvent event : events)
        {
            if (!(event instanceof TrafficChangeEvent))
                throw new Net2PlanException("Event must be a traffic change event");
        }
        
        return events;
    }

    @Override
    public void checkLoadedNetPlan(NetPlan netPlan)
    {
        int N = netPlan.getNumberOfNodes();
        int D = netPlan.getNumberOfDemands();

        if (N == 0 || D == 0) throw new Net2PlanException("A set of nodes and a set of demands are required");
    }

    @Override
    public Class<? extends IExternal> getEventGeneratorClass() { return ITrafficGenerator.class; }

    @Override
    public String getEventGeneratorLabel() { return "Traffic change generator"; }

    @Override
    public Class<? extends IExternal> getEventProcessorClass() { return ITrafficAllocationAlgorithm.class; }

    @Override
    public String getEventProcessorLabel() { return "Allocation algorithm"; }

    @Override
    public NetState getNetState() { return netState; }

    @Override
    public String getSimulationReport()
    {
	double simTime = getSimCore().getFutureEventList().getCurrentSimulationTime();
	double cpuTime = getSimCore().getCurrentCPUTime();
	long processedEvents = getSimCore().getFutureEventList().getNumberOfProcessedEvents();
	int pendingEvents = getSimCore().getFutureEventList().getNumberOfPendingEvents();
	double simulationSpeed = cpuTime == 0 ? 0 : (double) processedEvents / cpuTime;

	StringBuilder info = new StringBuilder();

	info.append("<html><head><title>Simulation report</title></head>");
	info.append("<body>");
	info.append("<h1>General information</h1>");

	info.append("<table border='1'><tr><th>Parameter</th><th>Value</th></tr>");
        info.append(String.format("<tr><td>Current simulation time</td><td>%s</td></tr>", SimEvent.secondsToYearsDaysHoursMinutesSeconds(simTime)));
	info.append(String.format("<tr><td>Current CPU time</td><td>%s</td></tr>", SimEvent.secondsToYearsDaysHoursMinutesSeconds(cpuTime)));
	info.append(String.format("<tr><td>Number of time periods</td><td>%d (%f ev/sec)</td></tr>", processedEvents, simulationSpeed));
	info.append("</table>");

	info.append("<h1>Results</h1>");

        if (stats.isEmpty()) info.append("No results available since 'disableStatistics' was set to 'true'");

        for (SimStats stat : stats) info.append(stat.getResults(getSimCore().getFutureEventList().getCurrentSimulationTime()));
        
	StringBuilder html = new StringBuilder();
	String out = ((ITrafficAllocationAlgorithm) eventProcessor).finish(html, currentDate);
	if (out != null && html.length() > 0)
	{
	    info.append(String.format("<h1>%s</h1>", out));
	    info.append(html);
	}

	info.append("</body></html>");

	return info.toString();
    }

    @Override
    public List<? extends SimEvent> initialize()
    {
	int weekDay = Integer.parseInt(simulationParameters.get("startWeekDay"));
	String startTime = simulationParameters.get("startTime");
	String[] values = startTime.split(":");

	Calendar calendar = GregorianCalendar.getInstance();
	calendar.setFirstDayOfWeek(Calendar.MONDAY);
	calendar.setWeekDate(1, 1, weekDay);
	calendar.set(2013, 1, 1, Integer.parseInt(values[0]), Integer.parseInt(values[1]));

	currentDate = calendar;

        ((ITrafficGenerator) eventGenerator).initialize(netPlan, eventGeneratorParameters, net2planParameters);
	((ITrafficAllocationAlgorithm) eventProcessor).initialize(netPlan, netState, eventProcessorParameters, net2planParameters);

        if (!disableStatistics)
        {
            SimStats evolutionStats = new NetworkEvolution(netPlan, netState, simulationParameters, net2planParameters);
            stats.add(evolutionStats);
        }

	List<TrafficChangeEvent> events = new LinkedList<TrafficChangeEvent>();

	TrafficChangeEvent initializeEvent = new TrafficChangeEvent(0, calendar);
	events.add(initializeEvent);

	return events;
    }

    @Override
    public void simulationLoop(SimEvent event, List<SimEvent> newEvents)
    {
        if (!(event instanceof TrafficChangeEvent)) throw new RuntimeException("Bad - Invalid event type");
        
        TrafficChangeEvent trafficChangeEvent = (TrafficChangeEvent) event;
        
	currentDate = trafficChangeEvent.getDate();
        
        int timeGranularityInSeconds = Integer.parseInt(simulationParameters.get("timeGranularityInSeconds"));
        Calendar nextDate = (Calendar) currentDate.clone();

        nextDate.add(Calendar.SECOND, timeGranularityInSeconds);
        newEvents.add(new TrafficChangeEvent(event.getEventTime() + timeGranularityInSeconds, nextDate));

	double[] h_d = ((ITrafficGenerator) eventGenerator).execute(netPlan, currentDate);
        
        int D = netPlan.getNumberOfDemands();
        if (h_d.length != D) throw new Net2PlanException("Traffic from generator does not match the number of demands");
        for(int demandId = 0; demandId < D; demandId++)
            if (h_d[demandId] < 0) throw new Net2PlanException("Traffic from generator for demand " + demandId + " is lower than zero");

	List<TrafficAllocationAction> actions = ((ITrafficAllocationAlgorithm) eventProcessor).processEvent(netPlan, netState, h_d, currentDate);
        checkActions(trafficChangeEvent, actions);
	lastActions.addAll(actions);
        
        netState.update(new __INTERNAL_TrafficEvent(event.getEventTime(), h_d), lastActions);
        netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);

	for (SimStats stat : stats) stat.computeNextState(event, lastActions);
    }
    
    /**
     * Internal event which encloses the new traffic demand volume vector.
     * 
     * @since 0.2.3
     */
    public static class __INTERNAL_TrafficEvent extends SimEvent
    {
        /**
         * New traffic demand volume vector.
         * 
         * @since 0.2.3
         */
        public final double[] h_d;
                
        /**
         * Default constructor.
         * 
         * @param simTime Current simulation time
         * @param h_d New traffic demand volume vector
         * @since 0.2.3
         */
        public __INTERNAL_TrafficEvent(double simTime, double[] h_d) { super(simTime); this.h_d = h_d; }
    }

    /**
     * Checks for validity of actions to a given event.
     * 
     * @param event Event
     * @param actions Corresponding actions
     * @since 0.2.2
     */
    public void checkActions(TrafficChangeEvent event, List<TrafficAllocationAction> actions) { }

    @Override
    public NetPlan getCurrentNetPlan()
    {
        if (netState == null) throw new RuntimeException("Bad");
        
        return netState.convertToNetPlan();
    }

    @Override
    public void resetModule() { stats.clear(); }

    @Override
    public void endOfTransitory(double currentSimTime)
    {
        ((ITrafficAllocationAlgorithm) eventProcessor).finishTransitory(((TrafficChangeEvent) lastEvent).getDate());
        
	for (SimStats stat : stats)
	    stat.reset(currentSimTime);
    }

    @Override
    protected List<Triple<String, String, String>> getSimulationSpecificParameters()
    {
	List<Triple<String, String, String>> defaultParameters = new LinkedList<Triple<String, String, String>>();
	defaultParameters.add(Triple.of("startWeekDay", "1", "Week day at the start of the simulation (1 = Monday, ... 7 = Sunday)"));
	defaultParameters.add(Triple.of("startTime", "12:00", "Time (in 24-hour hh:mm format) at the start of the simulation"));
	defaultParameters.add(Triple.of("timeGranularityInSeconds", "300", "Minimum time between traffic changes"));

	return defaultParameters;
    }

    @Override
    public String getCommandLineSpecificHelp()
    {
        return "Simulates the network operation, "
            + "where traffic demand volumes vary with time according to a "
            + "user-defined pattern. Targeted to evaluate the performances "
            + "of built-in or user-defined schemes that react to traffic "
            + "variations (i.e. traffic rerouting schemes, on-demand "
            + "capacity-provisioning schemes, etc.)";
    }
}
