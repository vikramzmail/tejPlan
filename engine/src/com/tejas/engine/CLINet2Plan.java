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

package com.tejas.engine;

import com.jom.JOMException;
import com.tejas.engine.CLINet2Plan;
import com.tejas.engine.CLINet2Plan.IllegalModeException;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.CommandLineParser;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.ICLIModule;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.Version;
import com.tejas.engine.internal.sim.SimKernel;
import com.tejas.engine.internal.sim.impl.ConnectionSimulation;
import com.tejas.engine.internal.sim.impl.ResilienceSimulation;
import com.tejas.engine.internal.sim.impl.TimeVaryingTrafficSimulation;
import com.tejas.engine.tools.CLINetworkDesign;
import com.tejas.engine.tools.CLIReport;
import com.tejas.engine.tools.CLITrafficDesign;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.apache.commons.cli.*;

/**
 * Main class for the command-line user interface (CLI).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CLINet2Plan
{
    private final static Map<String, Class<? extends ICLIModule>> modes;
    private final static Options options;
    private final static int lineWidth;

    static
    {
        lineWidth = 80;
        
	modes = new LinkedHashMap<String, Class<? extends ICLIModule>>();
	modes.put("net-design", CLINetworkDesign.class);
	modes.put("report", CLIReport.class);
        modes.put("traffic-design", CLITrafficDesign.class);
	modes.put("resilience-sim", ResilienceSimulation.class);
	modes.put("cac-sim", ConnectionSimulation.class);
	modes.put("time-varying-traffic-sim", TimeVaryingTrafficSimulation.class);

        options = new Options();
        
        Option help = OptionBuilder.withLongOpt("help")
            .withDescription("Show the complete help information. 'modename' is optional")
            .hasArg()
            .hasOptionalArg()
            .withArgName("modename")
            .create();
        
        Option mode = OptionBuilder.withLongOpt("mode")
            .withDescription("Mode: " + StringUtils.join(StringUtils.toArray(modes.keySet()), ", "))
            .hasArg()
            .withArgName("modename")
            .create();

        OptionGroup group = new OptionGroup();
        group.addOption(help);
        group.addOption(mode);
        
        options.addOptionGroup(group);
    }
    
    /**
     * Default constructor.
     * 
     * @param args Command-line arguments
     * @since 0.2.0
     */
    public CLINet2Plan(String args[])
    {
        SystemUtils.configureEnvironment(CLINet2Plan.class, SystemUtils.UserInterface.CLI);
        
        System.out.println();

        CommandLineParser parser = new CommandLineParser();
        
        try
        {
            CommandLine cli = parser.parse(options, args);
            
            if (cli.hasOption("help"))
            {
                String mode = cli.getOptionValue("help");
                if (mode == null) System.out.println(getCompleteHelp());
                else System.out.println(getModeHelp(mode));
            }
            else if (!cli.hasOption("mode"))
            {
                System.out.println(getMainHelp());
            }
            else
            {
                String mode = cli.getOptionValue("mode");

                if (modes.containsKey(mode))
                {
                    ICLIModule modeInstance = modes.get(mode).newInstance();

                    try
                    {
                        modeInstance.executeFromCommandLine(args);
                    }
                    catch(Net2PlanException | JOMException ex)
                    {
                        System.out.println("Execution stopped");
                        System.out.println();
                        System.out.println(ex.getMessage());
                    }
                    catch(ParseException ex)
                    {
                        System.out.println("Bad syntax: " + ex.getMessage());
                        System.out.println();
                        System.out.println(getModeHelp(mode));
                    }
                    catch(Throwable ex)
                    {
                        Throwable ex1 = ErrorHandling.getInternalThrowable(ex);
                        if (ex1 instanceof Net2PlanException || ex1 instanceof JOMException)
                        {
                            System.out.println("Execution stopped");
                            System.out.println();
                            System.out.println(ex1.getMessage());
                        }
                        else if (ex1 instanceof ParseException)
                        {
                            System.out.println("Bad syntax: " + ex1.getMessage());
                            System.out.println();
                            System.out.println(getModeHelp(mode));
                        }
                        else
                        {
                            System.out.println("Execution stopped. An unexpected error happened");
                            System.out.println();
                            printStackTrace(ex1);
                        }
                    }
                }
                else
                {
                    throw new IllegalModeException("Bad mode - " + mode);
                }
            }
        }
        catch(IllegalModeException e)
        {
            System.out.println(e.getMessage());
	    System.out.println();
	    System.out.println(getMainHelp());
        }
        catch(ParseException e)
        {
            System.out.println("Bad syntax: " + e.getMessage());
	    System.out.println();
	    System.out.println(getMainHelp());
        }
        catch(Throwable e)
        {
            printStackTrace(e);
        }
    }
    
    private static void printStackTrace(Throwable throwable)
    {
        System.out.println(throwable);
        System.out.println();
        
        StackTraceElement[] stack = throwable.getStackTrace();
        for(StackTraceElement line : stack)
            System.out.println(line);
    }
    
    private static String getModeHelp(String mode)
    {
        StringBuilder help = new StringBuilder();
        
        if (!modes.containsKey(mode)) throw new IllegalModeException("Bad mode - " + mode);
        
        try
        {
            ICLIModule modeInstance = modes.get(mode).newInstance();
            Options modeOptions = modeInstance.getCommandLineOptions();
            String modeHelp = modeInstance.getCommandLineHelp();

            StringWriter sw = new StringWriter();
	    PrintWriter w = new PrintWriter(sw);
	    HelpFormatter formatter = new HelpFormatter();
            formatter.printWrapped(w, lineWidth, "Mode: " + mode);
            formatter.printWrapped(w, lineWidth, "");
            formatter.printWrapped(w, lineWidth, modeHelp);
            formatter.printWrapped(w, lineWidth, "");
	    formatter.printHelp(w, lineWidth, "java -jar Net2Plan-cli.jar --mode " + mode, null, modeOptions, 0, 1, null, true);
            
            if (modeInstance instanceof SimKernel)
            {
                formatter.printWrapped(w, lineWidth, "");
                formatter.printWrapped(w, lineWidth, "Simulation parameters:");
                formatter.printWrapped(w, lineWidth, "");
                
                List<Triple<String, String, String>> simParam = ((SimKernel) modeInstance).getSimulationParameters();
                for(Triple<String, String, String> param : simParam)
                    formatter.printWrapped(w, lineWidth, 2, String.format("- %s: %s. Default: %s", param.getFirst(), param.getThird(), param.getSecond()));
            }            
	    w.flush();
            
            help.append(sw.toString());
        }
        catch(Throwable e) { throw new RuntimeException(e); }
        
        return help.toString();
    }
    
    private static String getMainHelp()
    {
        final StringBuilder help = new StringBuilder();
        
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        formatter.printWrapped(w, lineWidth, new Version().toString() + " Command-Line Interface");
        formatter.printWrapped(w, lineWidth, "");
        formatter.printHelp(w, lineWidth, "java -jar Net2Plan-cli.jar", null, options, 0, 1, null, true);
        formatter.printWrapped(w, lineWidth, "");
        formatter.printWrapped(w, lineWidth, "Select 'help' to show this information, or 'mode' to execute a specific tool. Optionally, if 'help' is accompanied of a mode name, the help information for this mode is shown");
        w.flush();
        help.append(sw.toString());
        
        return help.toString();
    }
    
    private static String getCompleteHelp()
    {
        final StringBuilder help = new StringBuilder();
        final String lineSeparator = StringUtils.getLineSeparator();
        
        help.append(getMainHelp());
        
        for(String mode : modes.keySet())
        {
            help.append(lineSeparator);
            help.append(lineSeparator);
            help.append(getModeHelp(mode));
        }
        
        return help.toString();
    }

    /**
     * Entry point for the command-line interface
     * 
     * @param args Command-line arguments
     * @since 0.2.0
     */
    public static void main(String[] args) {new CLINet2Plan(args);}
    
    public static class IllegalModeException extends RuntimeException
    {
        public IllegalModeException(String message) { super(message); }
    }
}
