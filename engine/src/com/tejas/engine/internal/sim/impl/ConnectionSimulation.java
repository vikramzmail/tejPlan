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

import static com.tejas.engine.interfaces.cacSimulation.CACAction.ActionType.ACCEPT_REQUEST;
import static com.tejas.engine.interfaces.cacSimulation.CACAction.ActionType.BLOCK_REQUEST;
import static com.tejas.engine.interfaces.cacSimulation.CACEvent.EventType.CONNECTION_RELEASE;
import static com.tejas.engine.interfaces.cacSimulation.CACEvent.EventType.CONNECTION_REQUEST;

import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.cacSimulation.ICACAlgorithm;
import com.tejas.engine.interfaces.cacSimulation.IConnectionEventGenerator;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.sim.EndSimulationException;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimKernel;
import com.tejas.engine.internal.sim.SimCore.SimState;
import com.tejas.engine.internal.sim.stats.ConnectionPerformance;
import com.tejas.engine.internal.sim.stats.SimStats;
import com.tejas.engine.utils.Triple;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Simulates the network operation, where traffic demands are the source of
 * connection requests. Targeted to evaluate built-in or user-defined CAC
 * (Connection-Admission-Control) algorithms, which dynamically allocate
 * resources to connection requests.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class ConnectionSimulation extends SimKernel
{
    private final List<SimStats> stats;
    private final static Comparator<SimEvent> comparator = new ComparatorSameReleaseConnId();

    private ConnectionNetState netState;
    private long acceptedConnections, blockedConnections;
    private boolean incrementalModel;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public ConnectionSimulation()
    {
        super();
        
	stats = new LinkedList<SimStats>();
    }

    @Override
    public List<? extends SimEvent> checkEvents(List<? extends SimEvent> events)
    {
        List<CACEvent> auxEvents = new LinkedList<CACEvent>();

        for(SimEvent event : events)
        {
            if (!(event instanceof CACEvent))
                throw new Net2PlanException("Event must be a CAC event");
            
            CACEvent cacEvent = (CACEvent) event;
            
            if (incrementalModel)
            {
                switch(cacEvent.getEventType())
                {
                    case CONNECTION_REQUEST:
                        double simTime = cacEvent.getEventTime();
                        int demandId = cacEvent.getRequestDemandId();
                        double duration = Double.MAX_VALUE;
                        double trafficVolumeInErlangs = cacEvent.getRequestTrafficVolumeInErlangs();
                        Map<String, String> attributes = cacEvent.getRequestAttributes();
                        CACEvent newEvent = new CACEvent(simTime, demandId, duration, trafficVolumeInErlangs, attributes);
                        auxEvents.add(newEvent);
                        break;
                    
                    case CONNECTION_RELEASE:
                        throw new Net2PlanException("Connection release event in incremental model");
                        
                    default:
                        break;
                }
                if (cacEvent.getEventType() == CONNECTION_RELEASE)
                    throw new Net2PlanException("Connection release event in incremental model");
            }
            else
            {
                auxEvents.add(cacEvent);
            }
        }

        return auxEvents;
    }

    @Override
    public void checkLoadedNetPlan(NetPlan netPlan)
    {
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();

        if (N == 0 || E == 0) throw new Net2PlanException("A complete network topology is required");
    }

    @Override
    public Class<? extends IExternal> getEventGeneratorClass() { return IConnectionEventGenerator.class; }

    @Override
    public String getEventGeneratorLabel() { return "Connection event generator"; }

    @Override
    public Class<? extends IExternal> getEventProcessorClass() { return ICACAlgorithm.class; }

    @Override
    public String getEventProcessorLabel() { return "CAC algorithm"; }

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
	info.append(String.format("<tr><td>Number of processed events</td><td>%d (%f ev/sec)</td></tr>", processedEvents, simulationSpeed));
	info.append(String.format("<tr><td>Number of pending events</td><td>%d</td></tr>", pendingEvents));
	info.append(String.format("<tr><td>Number of accepted connections</td><td>%d</td></tr>", acceptedConnections));
	info.append(String.format("<tr><td>Number of blocked connections</td><td>%d</td></tr>", blockedConnections));
	info.append(String.format("<tr><td>Current active connections</td><td>%d</td></tr>", netState.getNumberOfConnections()));
	info.append(String.format("<tr><td>Finished connections</td><td>%d</td></tr>", acceptedConnections - netState.getNumberOfConnections()));
	info.append("</table>");

	info.append("<h1>Results</h1>");
        
        if (stats.isEmpty()) info.append("No results available since 'disableStatistics' was set to 'true'");

	for (SimStats stat : stats)
	    info.append(stat.getResults(getSimCore().getFutureEventList().getCurrentSimulationTime()));

	StringBuilder html = new StringBuilder();
	String out = ((ICACAlgorithm) eventProcessor).finish(html, simTime);
	if (out != null && html.length() > 0)
	{
	    info.append(String.format("<h1>%s</h1>", out));
	    info.append(html);
	}

	info.append("</body></html>");

	return info.toString();
    }
    
    @Override
    public void initializeNetState()
    {
        if (getSimCore().getSimulationState() != SimState.NOT_STARTED)
            throw new Net2PlanException("Network state cannot be re-initialized"
                    + "once the simulation was started");
        
	netState = new ConnectionNetState(netPlan);
    }
    
    @Override
    public List<? extends SimEvent> initialize()
    {
        if (eventGenerator == null) throw new Net2PlanException("An event generator must be selected");
        if (eventProcessor == null) throw new Net2PlanException("An event processor must be selected");
        
        if (!(eventGenerator instanceof IConnectionEventGenerator)) throw new Net2PlanException("Event generator is not an instance of the required class");
        if (!(eventProcessor instanceof ICACAlgorithm)) throw new Net2PlanException("Event processor is not an instance of the required class");
            
        if (!simulationParameters.containsKey("incrementalModel")) throw new Net2PlanException("'incrementalModel' parameter is not configured");
        incrementalModel = Boolean.parseBoolean(simulationParameters.get("incrementalModel"));
                
	List<CACEvent> aux_events = ((IConnectionEventGenerator) eventGenerator).initialize(netPlan, netState, eventGeneratorParameters, net2planParameters);
        List<? extends SimEvent> events = checkEvents(aux_events);
        
	((ICACAlgorithm) eventProcessor).initialize(netPlan, netState, eventProcessorParameters, net2planParameters);

        if (!disableStatistics)
        {
            SimStats connectionStats = new ConnectionPerformance(netPlan, netState, simulationParameters, net2planParameters);
            stats.add(connectionStats);
        }

	return events;
    }

    @Override
    public void simulationLoop(SimEvent event, List<SimEvent> newEvents)
    {
        if (!(event instanceof CACEvent)) throw new RuntimeException("Bad - Invalid event type");

	CACEvent cacEvent = (CACEvent) event;

	switch(cacEvent.getEventType())
	{
	    case CONNECTION_REQUEST:
                
		List<? extends SimEvent> newEventsForRequestEvent = ((IConnectionEventGenerator) eventGenerator).processEvent(netPlan, netState.unmodifiableView(), cacEvent);
                newEventsForRequestEvent = checkEvents(newEventsForRequestEvent);
		newEvents.addAll(newEventsForRequestEvent);

		List<CACAction> actionsForRequestEvent = ((ICACAlgorithm) eventProcessor).processEvent(netPlan, netState.unmodifiableView(), cacEvent);
                checkActions(cacEvent, actionsForRequestEvent);
		lastActions.addAll(actionsForRequestEvent);

                long newConnId = (long) netState.update(cacEvent, actionsForRequestEvent);
                
                if (newConnId != -1)
                {
                    double arrivalTimeInSeconds = cacEvent.getEventTime();
                    double durationInSeconds = cacEvent.getRequestDurationInSeconds();
                    
                    if (durationInSeconds != Double.MAX_VALUE)
                    {
                        CACEvent connectionReleaseEvent = new CACEvent(arrivalTimeInSeconds + durationInSeconds, newConnId);
                        newEvents.add(connectionReleaseEvent);
                    }
                }

                boolean isDecisionMade;
		for(CACAction action : actionsForRequestEvent)
		{
		    switch(action.getActionType())
		    {
			case ACCEPT_REQUEST:
			    isDecisionMade = true;
			    acceptedConnections++;
			    break;

			case BLOCK_REQUEST:
			    isDecisionMade = true;
                            if (incrementalModel) throw new EndSimulationException();
			    blockedConnections++;
			    break;

			default:
			    continue;
		    }

		    if (isDecisionMade) break;
		}

		break;

	    case CONNECTION_RELEASE:
                if (!netState.isActiveConnection(cacEvent.getReleaseConnectionId())) return;
                
		List<? extends SimEvent> newEventsForReleaseEvent = ((IConnectionEventGenerator) eventGenerator).processEvent(netPlan, netState.unmodifiableView(), cacEvent);
                newEventsForReleaseEvent = checkEvents(newEventsForReleaseEvent);
		newEvents.addAll(newEventsForReleaseEvent);
                
		List<CACAction> actionsForReleaseEvent = ((ICACAlgorithm) eventProcessor).processEvent(netPlan, netState.unmodifiableView(), cacEvent);
                checkActions(cacEvent, actionsForReleaseEvent);
		lastActions.addAll(actionsForReleaseEvent);
		netState.update(cacEvent, actionsForReleaseEvent);

		break;

	    default:
		throw new RuntimeException("Bad - Unknown CAC event type");
	}
        
        netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);

	for (SimStats stat : stats)
	    stat.computeNextState(cacEvent, Collections.unmodifiableList(lastActions));
    }
    
    private static class ComparatorSameReleaseConnId implements Comparator<SimEvent>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(SimEvent event1, SimEvent event2)
        {
            if (!(event1 instanceof CACEvent)) throw new RuntimeException("Bad");
            if (!(event2 instanceof CACEvent)) throw new RuntimeException("Bad");

            CACEvent cacEvent1 = (CACEvent) event1;
            CACEvent cacEvent2 = (CACEvent) event2;

            if (cacEvent1.getEventType() == CACEvent.EventType.CONNECTION_RELEASE && cacEvent2.getEventType() == CACEvent.EventType.CONNECTION_RELEASE)
            {
                long connId1 = cacEvent1.getReleaseConnectionId();
                long connId2 = cacEvent2.getReleaseConnectionId();

                if (connId1 == connId2) return 0;
            }

            return -1;
        }
    }

    /**
     * Checks for validity of actions to a given event.
     * 
     * @param event Event
     * @param actions Corresponding actions
     * @since 0.2.2
     */
    public void checkActions(CACEvent event, List<CACAction> actions)
    {
    }

    @Override
    public NetPlan getCurrentNetPlan() { return netState.convertToNetPlan(); }
    
    @Override
    public ConnectionNetState getNetState() { return netState; }

    @Override
    public void resetModule()
    {
        acceptedConnections = 0;
        blockedConnections = 0;
        
        stats.clear();
    }

    @Override
    public void endOfTransitory(double currentSimTime)
    {
        ((ICACAlgorithm) eventProcessor).finishTransitory(currentSimTime);
        
	for(SimStats stat : stats)
	    stat.reset(currentSimTime);
    }
    
    @Override
    protected List<Triple<String, String, String>> getSimulationSpecificParameters()
    {
	List<Triple<String, String, String>> defaultParameters = new LinkedList<Triple<String, String, String>>();
        defaultParameters.add(Triple.of("allowExcessCarriedTraffic","true","Indicates whether or not carried traffic may be greater than the offered one for some connection"));
        defaultParameters.add(Triple.of("incrementalModel", "false", "In the incremental model the simulation stops after the first connection request blocking event"));

	return defaultParameters;
    }

    @Override
    public String getCommandLineSpecificHelp()
    {
        return "Simulates the network operation, where traffic demands are the source of connection "
                + "requests. Targeted to evaluate built-in or user-defined CAC "
                + "(Connection-Admission-Control) algorithms, which dynamically "
                + "allocate resources to connection requests";
    }
}
