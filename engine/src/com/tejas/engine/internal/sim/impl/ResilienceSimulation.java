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

import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.SRG_FAILURE;
import static com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent.EventType.SRG_REPARATION;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.IProvisioningAlgorithm;
import com.tejas.engine.interfaces.resilienceSimulation.IResilienceEventGenerator;
import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.sim.SimCore;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimKernel;
import com.tejas.engine.internal.sim.stats.Availability;
import com.tejas.engine.internal.sim.stats.SimStats;
import com.tejas.engine.libraries.SRGUtils;
import com.tejas.engine.utils.Triple;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simulates the network operation, where failures in links and nodes randomly
 * appear according to the user-defined reliability information and the
 * definition of shared risk groups (SRG). Targeted to evaluate availability
 * performances of built-in or user-defined protection/restoration schemes.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class ResilienceSimulation extends SimKernel
{
    private ResilienceNetState netState;
    private List<SimStats> stats;

    private long processedFailureEvents, processedRepairEvents;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public ResilienceSimulation()
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
        
	netState = new ResilienceNetState(netPlan);
    }

    @Override
    public ResilienceNetState getNetState() { return netState; }

    @Override
    public List<? extends SimEvent> checkEvents(List<? extends SimEvent> events)
    {
        for(SimEvent event : events)
        {
            if (!(event instanceof ResilienceEvent))
                throw new Net2PlanException("Event must be a resilience event");

            ResilienceEvent aux = (ResilienceEvent) event;

            switch(aux.getEventType())
            {
                case SRG_FAILURE:
                case SRG_REPARATION:
                    break;

                default:
                    throw new Net2PlanException("Only SRG failure/reparation events can be scheduled");
            }
        }
        
        return events;
    }

    @Override
    public void checkLoadedNetPlan(NetPlan netPlan)
    {
        int R = netPlan.getNumberOfRoutes();

        if (R == 0) throw new Net2PlanException("A complete network design is required: topology, demands and traffic routes");
    }

    @Override
    public Class<? extends IExternal> getEventGeneratorClass() { return IResilienceEventGenerator.class; }

    @Override
    public String getEventGeneratorLabel() { return "Failure/reparation event generator"; }

    @Override
    public Class<? extends IExternal> getEventProcessorClass() { return IProvisioningAlgorithm.class; }

    @Override
    public String getEventProcessorLabel() { return "Provisioning algorithm"; }

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
        info.append(String.format("<tr><td>Number of processed failure events</td><td>%d</td></tr>", processedFailureEvents));
        info.append(String.format("<tr><td>Number of processed repair events</td><td>%d</td></tr>", processedRepairEvents));
        info.append(String.format("<tr><td>Number of pending events</td><td>%d</td></tr>", pendingEvents));
        info.append("</table>");

        info.append("<h1>Results</h1>");

        if (stats.isEmpty()) info.append("No results available since 'disableStatistics' was set to 'true'");

        for (SimStats stat : stats)
            info.append(stat.getResults(getSimCore().getFutureEventList().getCurrentSimulationTime()));

        StringBuilder html = new StringBuilder();
        String out = ((IProvisioningAlgorithm) eventProcessor).finish(html, simTime);

        if (out != null && html.length() > 0)
        {
            info.append(String.format("<h1>%s</h1>", out));
            info.append(html);
        }

        info.append("</body></html>");

        return info.toString();
    }

    @Override
    public void configureSimulation(Map<String, String> simulationParameters, Map<String, String> net2planParameters, IExternal eventGenerator, Map<String, String> eventGeneratorParameters, IExternal eventProcessor, Map<String, String> eventProcessorParameters)
    {
        super.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
        
        double defaultMTTFInHours = Double.parseDouble(simulationParameters.get("defaultMTTFInHours"));
        double defaultMTTRInHours = Double.parseDouble(simulationParameters.get("defaultMTTRInHours"));
        
        String failureModel = simulationParameters.get("failureModel");
        if (!failureModel.equals("SRGfromNetPlan"))
        {
            NetPlan aux_netPlan = netPlan.copy();
            
            switch (failureModel)
            {
                case "SRGfromNetPlan":
                    break;

                case "perNode":
                    SRGUtils.configureSRGs(aux_netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_NODE, true);
                    break;

                case "perLink":
                    SRGUtils.configureSRGs(aux_netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_LINK, true);
                    break;

                case "perDirectionalLinkBundle":
                    SRGUtils.configureSRGs(aux_netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true);
                    break;

                case "perBidirectionalLinkBundle":
                    SRGUtils.configureSRGs(aux_netPlan, defaultMTTFInHours, defaultMTTRInHours, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
                    break;

                default:
                    throw new Net2PlanException("Failure model not valid. Please, check parameters description");
            }

            setNetPlan(aux_netPlan);
        }
    }
    
    @Override
    public List<? extends SimEvent> initialize()
    {
        netState.reset();
        
        List<ResilienceEvent> generatorOut = ((IResilienceEventGenerator) eventGenerator).initialize(netPlan, (ResilienceNetState) netState.unmodifiableView(), eventGeneratorParameters, net2planParameters);

        ((IProvisioningAlgorithm) eventProcessor).initialize(netPlan, (ResilienceNetState) netState.unmodifiableView(), eventProcessorParameters, net2planParameters);

        if (!disableStatistics)
        {
            SimStats availabilityStats = new Availability(netPlan, netState, simulationParameters, net2planParameters);
            stats.add(availabilityStats);
        }

        return generatorOut;
    }

    @Override
    public void simulationLoop(SimEvent event, List<SimEvent> newEvents)
    {
        if (!(event instanceof ResilienceEvent)) throw new RuntimeException("Bad - Invalid event type");

        ResilienceEvent resilienceEvent = (ResilienceEvent) event;
        
        List<? extends SimEvent> events = ((IResilienceEventGenerator) eventGenerator).processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), resilienceEvent);
        events = checkEvents(events);
        newEvents.addAll(events);

        switch(resilienceEvent.getEventType())
        {
            case SRG_FAILURE:
                int[] faultySRGIds = new int[] { resilienceEvent.getId() };

                Set<Integer> nodesUp2Down = new HashSet<Integer>();
                Set<Integer> linksUp2Down = new HashSet<Integer>();
                netState.getNodeLinkStateChanges(faultySRGIds, new int[0], null, nodesUp2Down, null, linksUp2Down);

                // Nodes up -> down
                for(int nodeId : nodesUp2Down)
                {
                    ResilienceEvent singleEvent = new ResilienceEvent(event.getEventTime(), nodeId, ResilienceEvent.EventType.NODE_FAILURE);

                    List<ProvisioningAction> actions = ((IProvisioningAlgorithm) eventProcessor).processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);
                    checkActions(singleEvent, actions);
                    lastActions.addAll(actions);

                    netState.update(singleEvent, actions);
                    netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                }

                // Links up -> down
                for(int linkId : linksUp2Down)
                {
                    ResilienceEvent singleEvent = new ResilienceEvent(event.getEventTime(), linkId, ResilienceEvent.EventType.LINK_FAILURE);

                    List<ProvisioningAction> actions = ((IProvisioningAlgorithm) eventProcessor).processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);
                    checkActions(singleEvent, actions);
                    lastActions.addAll(actions);

                    netState.update(singleEvent, actions);
                    netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                }

                netState.update(resilienceEvent, null);

                processedFailureEvents++;

                break;

            case SRG_REPARATION:
                int[] repairedSRGIds = new int[] { resilienceEvent.getId() };

                Set<Integer> nodesDown2Up = new HashSet<Integer>();
                Set<Integer> linksDown2Up = new HashSet<Integer>();
                netState.getNodeLinkStateChanges(new int[0], repairedSRGIds, nodesDown2Up, null, linksDown2Up, null);

                // Nodes down -> up
                for(int nodeId : nodesDown2Up)
                {
                    ResilienceEvent singleEvent = new ResilienceEvent(event.getEventTime(), nodeId, ResilienceEvent.EventType.NODE_REPARATION);

                    List<ProvisioningAction> actions = ((IProvisioningAlgorithm) eventProcessor).processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);
                    checkActions(singleEvent, actions);
                    lastActions.addAll(actions);

                    netState.update(singleEvent, actions);
                    netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                }

                // Links down -> up
                for(int linkId : linksDown2Up)
                {
                    ResilienceEvent singleEvent = new ResilienceEvent(event.getEventTime(), linkId, ResilienceEvent.EventType.LINK_REPARATION);

                    List<ProvisioningAction> actions = ((IProvisioningAlgorithm) eventProcessor).processEvent(netPlan, (ResilienceNetState) netState.unmodifiableView(), singleEvent);
                    checkActions(singleEvent, actions);
                    lastActions.addAll(actions);

                    netState.update(singleEvent, actions);
                    netState.checkValidity(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
                }

                netState.update(resilienceEvent, null);

                processedRepairEvents++;

                break;

            default:
                throw new RuntimeException("Bad - Invalid resilience event type (only SRG failure/reparation events are valid)");
        }

        for (SimStats stat : stats)
            stat.computeNextState(event, lastActions);
    }

    /**
     * Checks for validity of actions to a given event.
     * 
     * @param event Event
     * @param actions Corresponding actions
     * @since 0.2.2
     */
    public void checkActions(ResilienceEvent event, List<ProvisioningAction> actions)
    {
//        Set<Integer> modifiedRouteIds = new HashSet<Integer>();
//        Set<Integer> restoredRouteIds = new HashSet<Integer>();
//
//        for(ProvisioningAction action : actions)
//        {
//            switch(action.getActionType())
//            {
//                case MODIFY_ROUTE:
//                    int modifyRouteId = action.getModifyRouteId();
//                    if (modifiedRouteIds.contains(modifyRouteId)) throw new RuntimeException("Route " + modifyRouteId + " was already modified");
//                    if (restoredRouteIds.contains(modifyRouteId)) throw new RuntimeException("Route " + modifyRouteId + " was already restored");
//
//                    modifiedRouteIds.add(modifyRouteId);
//                    break;
//
//                case RESTORE_PRIMARY:
//                    int restoreRouteId = action.getRestoreRouteId();
//                    if (modifiedRouteIds.contains(restoreRouteId)) throw new RuntimeException("Route " + restoreRouteId + " was already modified");
//                    if (restoredRouteIds.contains(restoreRouteId)) throw new RuntimeException("Route " + restoreRouteId + " was already restored");
//
//                    restoredRouteIds.add(restoreRouteId);
//                    break;
//
//                default:
//                    throw new RuntimeException("Bad - Unknown provisioning action type");
//            }
//        }
    }

    @Override
    public NetPlan getCurrentNetPlan()
    {
        return netState.convertToNetPlan();
    }

    @Override
    public void resetModule()
    {
        processedFailureEvents = 0;
        processedRepairEvents = 0;
        
        stats.clear();
    }

    @Override
    public void endOfTransitory(double currentSimTime)
    {
        ((IProvisioningAlgorithm) eventProcessor).finishTransitory(currentSimTime);
        
	for(SimStats stat : stats)
	    stat.reset(currentSimTime);
    }

    @Override
    public String getCommandLineSpecificHelp()
    {
        return "Simulates the network operation, where "
                + "failures in links and nodes randomly appear according to the "
                + "user-defined reliability information and the definition of "
                + "shared risk groups (SRG). Targeted to evaluate availability "
                + "performances of built-in or user-defined protection/restoration schemes";
    }
    
    @Override
    public List<Triple<String, String, String>> getSimulationSpecificParameters()
    {
        List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
        parameters.add(Triple.of("assumeOverSubscribedLinksAsFailedLinks", "false", "Indicates whether over-subscribed links are assumed as failed links in metrics. Each route traversing a over-subscribed link will be assumed to carry no traffic"));
	parameters.add(Triple.of("defaultMTTFInHours", "8748", "Default value for Mean Time To Fail (hours)"));
	parameters.add(Triple.of("defaultMTTRInHours", "12", "Default value for Mean Time To Repair (hours)"));
	parameters.add(Triple.of("failureModel", "perBidirectionalLinkBundle", "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle"));
        
        return parameters;
    }
}
