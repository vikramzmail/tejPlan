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

import com.jom.JOMException;
import com.tejas.engine.internal.sim.EndSimulationException;
import com.tejas.engine.internal.sim.IEventCallback;
import com.tejas.engine.internal.sim.IGUISimulationListener;
import com.tejas.engine.internal.sim.SimCore;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.CommandLineParser;
import com.tejas.engine.internal.ICLIModule;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.NetState;
import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

/**
 * Core-class for simulators. Users are only responsible to implement their
 * simulation-specific methods (check input data, process events...)
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public abstract class SimKernel implements IEventCallback, ICLIModule
{
    private final SimCore simCore;
    private NetPlan originalNetPlan;
    protected NetPlan netPlan;
    protected SimEvent lastEvent;
    protected final List lastActions;
    protected Map<String, String> simulationParameters, net2planParameters, eventGeneratorParameters, eventProcessorParameters;
    protected IExternal eventGenerator, eventProcessor;
    protected boolean disableStatistics, omitProtectionSegments, allowExcessCarriedTraffic, allowLinkOversubscription;
    
    protected IGUISimulationListener guiListener;
    
    protected Throwable lastReason = null;
        
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public SimKernel()
    {
        originalNetPlan = null;
	netPlan = new NetPlan();
        simCore = new SimCore(this);
        lastActions = new LinkedList();

        initializeNetState();
    }
    
    /**
     *
     * @param stateListener
     */
    public void setGUIListener(IGUISimulationListener stateListener)
    {
        if (this.guiListener != null)
            throw new RuntimeException("A state listener was already installed");
        
        this.guiListener = stateListener;
    }

    /**
     * Returns the initial network plan.
     * 
     * @return The initial network plan
     * @since 0.2.2
     */
    public NetPlan getNetPlan()
    {
        return netPlan;
    }
    
    /**
     * Returns the current network state.
     * 
     * @return Current network state
     * @since 0.2.2
     */
    public abstract NetState getNetState();

    /**
     * <p>Sets the initial network plan.</p>
     * 
     * <p><b>Important</b>: Once the simulation is started, the initial network
     * plan cannot be changed.</p>
     * 
     * @param netPlan Initial network plan
     * @since 0.2.2
     */
    public void setNetPlan(NetPlan netPlan)
    {
        if (simCore.getSimulationState() != SimCore.SimState.NOT_STARTED)
            throw new Net2PlanException("Network design cannot be changed once simulation was started");
        
	checkLoadedNetPlan(netPlan);

	this.netPlan = netPlan.copy().unmodifiableView();
        
        initializeNetState();
    }

    /**
     * Checks for validity of events for the simulation.
     * 
     * @param events Event list
     * @since 0.2.2
     */
    public abstract List<? extends SimEvent> checkEvents(List<? extends SimEvent> events);

    /**
     * Checks for validity of the loaded network design.
     *
     * @param netPlan An input network design
     * @since 0.2.2
     */
    public abstract void checkLoadedNetPlan(NetPlan netPlan);

    /**
     * Returns the command 
     * @return
     * @since 0.2.2
     */
    public abstract String getCommandLineSpecificHelp();

    /**
     * Returns the current network plan corresponding to the network state.
     * 
     * @return Current network plan corresponding to the network state
     * @since 0.2.2
     */
    public abstract NetPlan getCurrentNetPlan();

    /**
     * Returns the class of the event generator.
     * 
     * @return Class of the event generator
     * @since 0.2.2
     */
    public abstract Class<? extends IExternal> getEventGeneratorClass();

    /**
     * Returns the label for the event generator.
     * 
     * @return Label for the event generator
     * @since 0.2.2
     */
    public abstract String getEventGeneratorLabel();

    /**
     * Returns the class of the event processor.
     * 
     * @return Class of the event processor
     * @since 0.2.2
     */
    public abstract Class<? extends IExternal> getEventProcessorClass();

    /**
     * Returns the label for the event processor.
     * 
     * @return Label for the event processor
     * @since 0.2.2
     */
    public abstract String getEventProcessorLabel();

    /**
     * Returns the simulation report.
     * 
     * @return Simulation report
     * @since 0.2.2
     */
    public abstract String getSimulationReport();

    /**
     * Initialize the simulation (event generator, event processor...)
     * 
     * @return First events to be scheduled
     * @since 0.2.2
     */
    public abstract List<? extends SimEvent> initialize();

    /**
     * Resets the simulation.
     * 
     * @since 0.2.2
     */
    public void reset()
    {
        simCore.reset();
        lastActions.clear();
        
        if (originalNetPlan != null)
        {
            setNetPlan(originalNetPlan);
            originalNetPlan = null;
        }
        else
        {
            initializeNetState();
        }
        
        lastReason = null;
        
        resetModule();
    }
    
    /**
     * Resets the current simulation module.
     * 
     * @since 0.2.2
     */
    public abstract void resetModule();

    /**
     * Initializes the current network state from a initial network plan.
     * 
     * @since 0.2.2
     */
    public abstract void initializeNetState();

    /**
     * Processes the event in the simulation.
     * 
     * @param event Current event
     * @param newEvents List of new events to be scheduled. It is an output parameter
     * @since 0.2.2
     */
    public abstract void simulationLoop(SimEvent event, List<SimEvent> newEvents);
    
    @Override
    public final String getCommandLineHelp()
    {
        StringBuilder out = new StringBuilder();
        out.append(getCommandLineSpecificHelp());
        out.append(StringUtils.getLineSeparator());
        out.append(StringUtils.getLineSeparator());
        out.append("This mode is built on top of a discrete-event simulator");
        return out.toString();
    }

    @Override
    public final Options getCommandLineOptions()
    {
	String generatorLabel = getEventGeneratorLabel();
	String processorLabel = getEventProcessorLabel();

	Options options = new Options();
        
	options.addOption(OptionBuilder.withLongOpt("config-file")
	    .withDescription("(Optional) configuration file with net2plan-wide parameters")
	    .withType(PatternOptionBuilder.FILE_VALUE)
	    .hasArg().withArgName("file").create());

	options.addOption(OptionBuilder.withLongOpt("input-file")
	    .withDescription("Input .n2p file")
	    .withType(PatternOptionBuilder.FILE_VALUE)
	    .hasArg().withArgName("file").isRequired().create());

	options.addOption(OptionBuilder.withLongOpt("sim-param")
	    .withDescription("Simulation parameter (use one of this for each parameter)")
	    .withArgName("property=value").hasArgs(2)
	    .withValueSeparator('=').create());

	options.addOption(OptionBuilder.withLongOpt("generator-class-file")
	    .withDescription(generatorLabel + " .class/.jar file")
	    .withType(PatternOptionBuilder.FILE_VALUE)
	    .hasArg().withArgName("file").isRequired().create());

	options.addOption(OptionBuilder.withLongOpt("generator-class-name")
	    .withDescription(generatorLabel + " class name (package.name)")
	    .withType(PatternOptionBuilder.STRING_VALUE)
	    .hasArg().withArgName("classname").isRequired().create());

	options.addOption(OptionBuilder.withLongOpt("generator-param")
	    .withDescription(generatorLabel + " parameter (use one of this for each parameter)")
	    .withArgName("property=value").hasArgs(2)
	    .withValueSeparator('=').create());

	options.addOption(OptionBuilder.withLongOpt("processor-class-file")
	    .withDescription(processorLabel + " .class/.jar file")
	    .withType(PatternOptionBuilder.FILE_VALUE)
	    .hasArg().withArgName("file").isRequired().create());

	options.addOption(OptionBuilder.withLongOpt("processor-class-name")
	    .withDescription(processorLabel + " class name (package.name)")
	    .withType(PatternOptionBuilder.STRING_VALUE)
	    .hasArg().withArgName("classname").isRequired().create());

	options.addOption(OptionBuilder.withLongOpt("processor-param")
	    .withDescription(processorLabel + " parameter (use one of this for each parameter)")
	    .withArgName("property=value").hasArgs(2)
	    .withValueSeparator('=').create());

	options.addOption(OptionBuilder.withLongOpt("output-file")
	    .withDescription("Output HTML file with the simulation report")
	    .withType(PatternOptionBuilder.FILE_VALUE)
	    .hasArg().withArgName("file").isRequired().create());
	return options;
    }

    /**
     * Returns the parameters for the simulation.
     * 
     * @return Parameters for the simulation
     * @since 0.2.2
     */
    public final List<Triple<String, String, String>> getSimulationParameters()
    {
	List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
	parameters.add(Triple.of("disableStatistics", "false", "Disable compilation of simulation statistics (only simulation information, and optionally algorithm-specific information, is collected)"));
	parameters.add(Triple.of("refreshTime", "10", "Refresh time (in seconds)"));
	parameters.add(Triple.of("omitProtectionSegments", "false", "Remove protection segments from the network plan to free their reserved bandwidth"));
	parameters.add(Triple.of("simEvents", "-1", "Total simulation events (including transitory period) (-1 means no limit). In case that 'simTime' and 'simEvents' are specified, the transitory period will finish when one of the previous values is reached"));
	parameters.add(Triple.of("transitoryEvents", "-1", "Number of events for transitory period (-1 means no transitory period). In case that 'transitoryTime' and 'transitoryEvents' are specified, the transitory period will finish when one of the previous values is reached"));
	parameters.add(Triple.of("simTime", "-1", "Total simulation time (in seconds, including transitory period) (-1 means no limit). In case that 'simTime' and 'simEvents' are specified, the transitory period will finish when one of the previous values is reached"));
	parameters.add(Triple.of("transitoryTime", "-1", "Transitory time (in seconds) (-1 means no transitory period). In case that 'transitoryTime' and 'transitoryEvents' are specified, the transitory period will finish when one of the previous values is reached"));
        parameters.add(Triple.of("allowLinkOversubscription","true","Indicates whether or not links may carry more traffic than their capacity"));
        parameters.add(Triple.of("allowExcessCarriedTraffic","true","Indicates whether or not carried traffic may be greater than the offered one for some demand"));

        List<Triple<String, String, String>> simSpecificParameters = getSimulationSpecificParameters();
        if (simSpecificParameters != null)
        {
            for(Triple<String, String, String> specificParameter : simSpecificParameters)
            {
                String paramName = specificParameter.getFirst();
                Iterator<Triple<String, String, String>> it = parameters.iterator();
                while(it.hasNext())
                    if (it.next().getFirst().equals(paramName))
                        it.remove();
            }

            parameters.addAll(getSimulationSpecificParameters());
        }

	return parameters;
    }

    /**
     * Returns the parameters for a specific simulation tool. It should not be
     * called externally, since it is already called by {@link #getSimulationParameters() getSimulationParameters} method.
     * 
     * @return Parameters for a specific simulation tool
     * @since 0.2.2
     */
    protected List<Triple<String, String, String>> getSimulationSpecificParameters()
    {
        return new LinkedList<Triple<String, String, String>>();
    }
    
    @Override
    public final void executeFromCommandLine(String[] args) throws ParseException
    {
	Options options = getCommandLineOptions();

	Class<? extends IExternal> eventGeneratorClass = getEventGeneratorClass();
	Class<? extends IExternal> eventProcessorClass = getEventProcessorClass();

	CommandLineParser parser = new CommandLineParser();
	CommandLine cli = parser.parse(options, args);
        
        Map<String, String> aux_net2planParameters;
        
	// 1. Read options file
        if (cli.hasOption("config-file"))
        {
            try { Configuration.readFromOptionsFile((File) cli.getParsedOptionValue("config-file")); }
            catch(IOException e) { throw new ParseException("Options file not loaded"); }
        }

        aux_net2planParameters = Configuration.getOptions();

	// 2. Load event generator and event processor objects
	File generatorClassFile = (File) cli.getParsedOptionValue("generator-class-file");
	String generatorClassName = (String) cli.getParsedOptionValue("generator-class-name");
	File provisioningClassFile = (File) cli.getParsedOptionValue("processor-class-file");
	String provisioningClassName = (String) cli.getParsedOptionValue("processor-class-name");

	IExternal aux_eventGenerator = ClassLoaderUtils.getInstance(generatorClassFile, generatorClassName, eventGeneratorClass);
	IExternal aux_eventProcessor = ClassLoaderUtils.getInstance(provisioningClassFile, provisioningClassName, eventProcessorClass);

	// 3. Read simulation, event generator and event processor parameters
	Map<String, String> aux_simulationParameters = CommandLineParser.getParameters(getSimulationParameters(), cli.getOptionProperties("sim-param"));
	Map<String, String> aux_eventGeneratorParameters = CommandLineParser.getParameters(aux_eventGenerator.getParameters(), cli.getOptionProperties("generator-param"));
	Map<String, String> aux_eventProcessorParameters = CommandLineParser.getParameters(aux_eventProcessor.getParameters(), cli.getOptionProperties("processor-param"));

	// 4. Read the input netPlan file
	File inputFile = (File) cli.getParsedOptionValue("input-file");
        File outputFile = (File) cli.getParsedOptionValue("output-file");

        NetPlan aux_netPlan = new NetPlan(inputFile);
        setNetPlan(aux_netPlan);
        
        // 5. Initialize simulation
        simCore.reset();
        configureSimulation(aux_simulationParameters, aux_net2planParameters, aux_eventGenerator, aux_eventGeneratorParameters, aux_eventProcessor, aux_eventProcessorParameters);
        long simEvents = Long.parseLong(simulationParameters.get("simEvents"));
        double simTime = Double.parseDouble(simulationParameters.get("simTime"));
        if (simEvents <= 0 && simTime <= 0)
            throw new Net2PlanException("In the command-line interface the number of total simulated events, or the total simulation time, must be a positive number");
        
	List<? extends SimEvent> events = initialize();
        events = checkEvents(events);
        simCore.getFutureEventList().addEvents(events);
        
        System.out.println("Net2Plan parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Simulation parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(simulationParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println(getEventGeneratorLabel() + " parameters");
        System.out.println("-----------------------------");
        System.out.println(eventGeneratorParameters.isEmpty() ? "None" : StringUtils.mapToString(eventGeneratorParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println(getEventProcessorLabel() + " parameters");
        System.out.println("-----------------------------");
        System.out.println(eventProcessorParameters.isEmpty() ? "None" : StringUtils.mapToString(eventProcessorParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Simulation started...");
        System.out.println();

        // 6. Run simulation
        long init = System.nanoTime();
	simCore.setSimulationState(SimCore.SimState.RUNNING);
        simCore.run();
        
        if (lastReason != null && !(lastReason instanceof EndSimulationException)) return;
        
        long end = System.nanoTime();

        String html = getSimulationReport();
        HTMLUtils.exportToHTML(outputFile, html);
        
        System.out.println(String.format("%n%nSimulation finished successfully in %f seconds", (end - init) / 1e9));
    }
    
    /**
     * Configures the simulation.
     * 
     * @param simulationParameters Simulation parameters
     * @param net2planParameters Net2Plan-wide configuration parameters
     * @param eventGenerator An instance of the event generator
     * @param eventGeneratorParameters Parameter-value map for the event generator
     * @param eventProcessor An instance of the event processor
     * @param eventProcessorParameters Parameter-value map for the event processor
     * @since 0.2.2
     */
    public void configureSimulation(Map<String, String> simulationParameters, Map<String, String> net2planParameters, IExternal eventGenerator, Map<String, String> eventGeneratorParameters, IExternal eventProcessor, Map<String, String> eventProcessorParameters)
    {
        this.simulationParameters = new HashMap<String, String>(simulationParameters);
        
        if (!simulationParameters.containsKey("disableStatistics")) throw new Net2PlanException("'disableStatistics' parameter is not configured");
        disableStatistics = Boolean.parseBoolean(simulationParameters.get("disableStatistics"));
        
        if (!simulationParameters.containsKey("allowLinkOversubscription")) throw new Net2PlanException("'allowLinkOversubscription' parameter is not configured");
        allowLinkOversubscription = Boolean.parseBoolean(simulationParameters.get("allowLinkOversubscription"));
        
        if (!simulationParameters.containsKey("allowExcessCarriedTraffic")) throw new Net2PlanException("'allowExcessCarriedTraffic' parameter is not configured");
        allowExcessCarriedTraffic = Boolean.parseBoolean(simulationParameters.get("allowExcessCarriedTraffic"));

        if (!simulationParameters.containsKey("omitProtectionSegments")) throw new Net2PlanException("'omitProtectionSegments' parameter is not configured");
        omitProtectionSegments = Boolean.parseBoolean(simulationParameters.get("omitProtectionSegments"));
        
        originalNetPlan = netPlan.copy();
        
        if (omitProtectionSegments && netPlan.hasProtectionSegments())
        {
            NetPlan aux_netPlan = netPlan.copy();
            aux_netPlan.removeAllProtectionSegments();
            setNetPlan(aux_netPlan);
        }
        
        if (!simulationParameters.containsKey("refreshTime")) throw new Net2PlanException("'refreshTime' parameter is not configured");
        double refreshTimeInSeconds = Double.parseDouble(simulationParameters.get("refreshTime"));
        simCore.setRefreshTimeInSeconds(refreshTimeInSeconds);
            
        if (!simulationParameters.containsKey("simEvents")) throw new Net2PlanException("'simEvents' parameter is not configured");
        long simEvents = Long.parseLong(simulationParameters.get("simEvents"));
        simCore.setTotalSimulationEvents(simEvents);
        
        if (!simulationParameters.containsKey("transitoryEvents")) throw new Net2PlanException("'transitoryEvents' parameter is not configured");
        long transitoryEvents = Long.parseLong(simulationParameters.get("transitoryEvents"));
        simCore.setTotalTransitoryEvents(transitoryEvents);

        if (!simulationParameters.containsKey("transitoryTime")) throw new Net2PlanException("'transitoryTime' parameter is not configured");
        double transitoryTime = Double.parseDouble(simulationParameters.get("transitoryTime"));
        simCore.setTotalTransitoryTime(transitoryTime);
        
        if (!simulationParameters.containsKey("simTime")) throw new Net2PlanException("'simTime' parameter is not configured");
        double simTime = Double.parseDouble(simulationParameters.get("simTime"));
        simCore.setTotalSimulationTime(simTime);

        this.net2planParameters = new HashMap<String, String>(net2planParameters);
        this.eventGenerator = eventGenerator;
        this.eventGeneratorParameters = new HashMap<String, String>(eventGeneratorParameters);
        this.eventProcessor = eventProcessor;
        this.eventProcessorParameters = new HashMap<String, String>(eventProcessorParameters);
    }

    @Override
    public void refresh(boolean forceRefresh)
    {
        if (guiListener != null)
            guiListener.refresh(forceRefresh);
        else
            System.out.println(getSimulationInfo());
    }
    
    /**
     * Returns a reference to the simulation core.
     * 
     * @return Reference to the simulation core
     * @since 0.2.0
     */
    public SimCore getSimCore()
    {
        return simCore;
    }

    /**
     * Returns a brief simulation information report (current simulation time,
     * last event processed...).
     * 
     * @return Simulation information
     * @since 0.2.2
     */
    public String getSimulationInfo()
    {
        if (simCore.getSimulationState() == SimCore.SimState.NOT_STARTED)
            return "Simulation not started yet";
                    
	String NEWLINE = StringUtils.getLineSeparator();

	double simTime = simCore.getFutureEventList().getCurrentSimulationTime();
	double cpuTime = simCore.getCurrentCPUTime();
	long processedEvents = simCore.getFutureEventList().getNumberOfProcessedEvents();
	int pendingEvents = simCore.getFutureEventList().getNumberOfPendingEvents();
	double simulationSpeed = cpuTime == 0 ? 0 : (double) processedEvents / cpuTime;

	StringBuilder info = new StringBuilder();
	info.append(String.format("Current simulation time: %s", SimEvent.secondsToYearsDaysHoursMinutesSeconds(simTime)));
	info.append(NEWLINE);
	info.append(String.format("Current CPU time: %s", SimEvent.secondsToYearsDaysHoursMinutesSeconds(cpuTime)));
	info.append(NEWLINE);
	info.append(String.format("Number of processed events: %d (%.3f ev/sec)", processedEvents, simulationSpeed));
	info.append(NEWLINE);
	info.append(String.format("Number of pending events: %d", pendingEvents));
	info.append(NEWLINE);
	info.append(String.format("Last event processed: %s", lastEvent == null ? "None" : lastEvent.toString()));
	info.append(NEWLINE);

	if (lastActions == null)
	{
	    info.append("Last actions performed: None");
	}
	else
	{
	    info.append("Last actions performed:");
	    info.append(NEWLINE);

            boolean isFirstItem = true;
	    for(Object item : ((List) lastActions))
            {
                if (!isFirstItem) info.append(NEWLINE);
                info.append(item);
                isFirstItem = false;
                
            }
	}

	return info.toString();
    }

    @Override
    public final List<SimEvent> processEvent(SimEvent event)
    {
	lastEvent = event;
	lastActions.clear();
	List<SimEvent> newEvents = new ArrayList<SimEvent>();

        simulationLoop(event, newEvents);

        return newEvents;
    }
    
    @Override
    public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason)
    {
        this.lastReason = reason;
        
        if (guiListener != null)
        {
            guiListener.simulationStateChanged(simulationState, reason);
        }
        else
        {
            if (reason == null || reason instanceof EndSimulationException) return;
            
            if (reason instanceof Net2PlanException || reason instanceof JOMException)
            {
                System.out.println("Error executing simulation");
                System.out.println();
                System.out.println(reason.getMessage());
            }
            else
            {
                System.out.println("Fatal error");
                System.out.println();
                reason.printStackTrace();
            }
        }
    }
}