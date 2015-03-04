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
import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.CommandLineParser;
import com.tejas.engine.internal.ICLIModule;
import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

/**
 * Reporting tool (CLI mode).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class CLIReport implements ICLIModule
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
            .withDescription("Input .n2p file")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
            .isRequired()
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
            .withDescription("Output .html file (extra .png files could be saved)")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg()
            .withArgName("file")
            .isRequired()
            .create());
        options.addOption(OptionBuilder.withLongOpt("report-param")
            .withArgName("property=value")
            .hasArgs(2)
            .withValueSeparator()
            .withDescription("Report parameters (use one of this for each parameter)")
            .create());
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException
    {
        CommandLineParser parser = new CommandLineParser();
        CommandLine cli = parser.parse(options, args);

        if (cli.hasOption("config-file"))
        {
            try { Configuration.readFromOptionsFile((File) cli.getParsedOptionValue("config-file")); }
            catch(IOException e) { throw new ParseException("Options file not loaded"); }
        }

        File classFile = (File) cli.getParsedOptionValue("class-file");
        String className = (String) cli.getParsedOptionValue("class-name");

        File inputFile = (File) cli.getParsedOptionValue("input-file");
        NetPlan netPlan = new NetPlan(inputFile);

        File outputFile = (File) cli.getParsedOptionValue("output-file");

        IReport report = ClassLoaderUtils.getInstance(classFile, className, IReport.class);

        Map<String, String> reportParameters = CommandLineParser.getParameters(report.getParameters(), cli.getOptionProperties("report-param"));
        Map<String, String> net2planParameters = Configuration.getOptions();

        System.out.println("Net2Plan parameters");
        System.out.println("-----------------------------");
        System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Report parameters");
        System.out.println("-----------------------------");
        System.out.println(reportParameters.isEmpty() ? "None" : StringUtils.mapToString(reportParameters, "=", String.format("%n")));
        System.out.println();
        System.out.println("Executing report...");
        System.out.println();

        long init = System.nanoTime();
        String html = report.executeReport(netPlan, reportParameters, net2planParameters);
        long end = System.nanoTime();

        HTMLUtils.exportToHTML(outputFile, html);

        System.out.println(String.format("%n%nReport finished successfully in %f seconds", (end - init) / 1e9));
    }

    @Override
    public String getCommandLineHelp()
    {
        return "Permits the generation of built-in or "
                + "user-defined reports, from any network design";
    }

    @Override
    public Options getCommandLineOptions()
    {
        return options;
    }
}
