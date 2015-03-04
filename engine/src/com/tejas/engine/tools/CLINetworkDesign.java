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

package com.tejas.engine.tools;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.IAlgorithmMultiLayer;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.CommandLineParser;
import com.tejas.engine.internal.ICLIModule;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

/**
 * Offline network design tool (CLI mode).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class CLINetworkDesign implements ICLIModule
{
    private final static Options options;

    static
    {
        options = new Options();

        options.addOption(OptionBuilder.withLongOpt("config-file")
            .withDescription("(Optional) configuration file with net2plan-wide parameters")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg().withArgName("file").create());

        options.addOption(OptionBuilder.withLongOpt("input-file")
            .withDescription(".n2p file")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
//                .isRequired()
            .create());
        options.addOption(OptionBuilder.withLongOpt("traffic-file")
            .withDescription("(optional) .n2p file with demands")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
            .create());
        options.addOption(OptionBuilder.withLongOpt("class-file")
            .withDescription(".class/.jar file")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
            .isRequired()
            .create());
        options.addOption(OptionBuilder.withLongOpt("class-name")
            .withDescription("Class name")
            .withType(PatternOptionBuilder.STRING_VALUE)
            .hasArg()
            .withArgName("classname")
            .isRequired()
            .create());
        options.addOption(OptionBuilder.withLongOpt("output-file")
            .withDescription("Output .n2p file (multilayer designs will be saved as a sequence of '-layerX.n2p', where X is the layer index, in descending order)")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
            .isRequired()
            .create());
        options.addOption(OptionBuilder.withLongOpt("alg-param")
            .withArgName("property=value")
            .hasArgs(2)
            .withValueSeparator()
            .withDescription("Algorithm parameters (use one of this for each parameter)")
            .create());
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException
    {
        final CommandLineParser parser = new CommandLineParser();
        final CommandLine cli = parser.parse(options, args);

        if (cli.hasOption("config-file"))
        {
            try { Configuration.readFromOptionsFile((File) cli.getParsedOptionValue("config-file")); }
            catch(IOException e) { throw new ParseException("Options file not loaded"); }
        }

        final File classFile = (File) cli.getParsedOptionValue("class-file");
        final String className = (String) cli.getParsedOptionValue("class-name");

        NetPlan aux;
        if (cli.hasOption("input-file"))
        {
            final File inputFile = (File) cli.getParsedOptionValue("input-file");
            aux = new NetPlan(inputFile); 
        }
        else
        {
            aux = new NetPlan();
        }

        if (cli.hasOption("traffic-file"))
            aux.addDemandsFrom(new NetPlan((File) cli.getParsedOptionValue("traffic-file")));

        final List<NetPlan> netPlans = new LinkedList<NetPlan>();
        netPlans.add(aux);

        final File outputFile = (File) cli.getParsedOptionValue("output-file");

        final IExternal algorithm = ClassLoaderUtils.getInstance(classFile, className, IExternal.class);

        final Map<String, String> algorithmParameters = CommandLineParser.getParameters(algorithm.getParameters(), cli.getOptionProperties("alg-param"));
        final Map<String, String> net2planParameters = Configuration.getOptions();
        System.out.println("Net2Plan parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Algorithm parameters");
        System.out.println("-----------------------------");
        System.out.println(algorithmParameters.isEmpty() ? "None" : StringUtils.mapToString(algorithmParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Executing algorithm...");
        System.out.println();

        String out = null;
        long init = System.nanoTime();
        if (algorithm instanceof IAlgorithm)
        {
            NetPlan netPlan = netPlans.get(0);
            out = ((IAlgorithm) algorithm).executeAlgorithm(netPlan, algorithmParameters, net2planParameters);

            netPlan.saveToFile(outputFile);
        } 
        else if (algorithm instanceof IAlgorithmMultiLayer)
        {
            out = ((IAlgorithmMultiLayer) algorithm).executeAlgorithm(netPlans, algorithmParameters, net2planParameters);
            if (netPlans.isEmpty()) throw new Net2PlanException("Bad - Multilayer algorithm returning a zero-layer design");

            String templateFileName = outputFile.getAbsoluteFile().toString();
            if (templateFileName.endsWith(".n2p"))
                templateFileName = templateFileName.substring(0, templateFileName.lastIndexOf("."));

            int ct = 0;
            for(NetPlan netPlan : netPlans)
                netPlan.saveToFile(new File(templateFileName + "-layer" + (ct++) + ".n2p"));
            	
        }
        else
        {
            throw new RuntimeException("No valid algorithm");
        }

        final long end = System.nanoTime();

        System.out.println(String.format("%n%nAlgorithm finished successfully in %f seconds%nOutput message: %s", (end - init) / 1e9, out));
    }

    @Override
    public String getCommandLineHelp()
    {
        return "Targeted to evaluate the network designs "
                + "generated by built-in or user-defined static planning "
                + "algorithms, deciding on aspects such as the network "
                + "topology, the traffic routing, link capacities, protection "
                + "routes and so on. Algorithms based on constrained optimization "
                + "formulations (i.e. ILPs) can be fast-prototyped using the "
                + "open-source Java Optimization Modeler library, to interface "
                + "to a number of external solvers such as GPLK, CPLEX or IPOPT";
    }

    @Override
    public Options getCommandLineOptions()
    {
        return options;
    }
}
