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

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.CommandLineParser;
import com.tejas.engine.internal.ICLIModule;
import com.tejas.engine.libraries.TrafficMatrixGenerationModels;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;

/**
 * Traffic matrix design tool (CLI mode).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class CLITrafficDesign implements ICLIModule
{
    private final static Options options;
    private final static Map<String, String> trafficPatterns;

    static
    {
        options = new Options();

        trafficPatterns = new HashMap<String, String>();
        trafficPatterns.put("constant", "Constant");
        trafficPatterns.put("gravity-model", "Gravity model");
        trafficPatterns.put("uniform-random-10", "Uniform (0, 10)");
        trafficPatterns.put("uniform-random-100", "Uniform (0, 100)");
        trafficPatterns.put("uniform-random-bimodal-50-50", "50% Uniform (0, 100) & 50% Uniform(0, 10)");
        trafficPatterns.put("uniform-random-bimodal-25-75", "25% Uniform (0, 100) & 75% Uniform(0, 10)");
        trafficPatterns.put("population-distance-model", "Population-distance model");

        Option inputFile = OptionBuilder.withLongOpt("input-file")
            .withDescription("Input .n2p file including a topology/demand set")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg().withArgName("file").create();

        Option numNodes = OptionBuilder.withLongOpt("num-nodes")
            .withDescription("Number of nodes")
            .withType(PatternOptionBuilder.NUMBER_VALUE)
            .hasArg().withArgName("nodes").create();

        OptionGroup inputData = new OptionGroup();
        inputData.addOption(inputFile);
        inputData.addOption(numNodes);
        inputData.setRequired(true);
        options.addOptionGroup(inputData);

        Option outputFile = OptionBuilder.withLongOpt("output-file")
            .withDescription("Output .n2p file (multiple traffic matrix designs will be saved as a sequence of '-tmX.n2p', where X is the matrix index)")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg().withArgName("file").isRequired().create();

        options.addOption(outputFile);

        Option randomFactor = OptionBuilder.withLongOpt("random-factor").withDescription("Random factor (only for population-distance-model, default 0.5)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("value").create();
        Option populationOffset = OptionBuilder.withLongOpt("pop-offset").withDescription("Population offset (only for population-distance-model, default 0)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("value").create();
        Option populationPower = OptionBuilder.withLongOpt("pop-power").withDescription("Population power (only for population-distance-model, default 1)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("value").create();
        Option distanceOffset = OptionBuilder.withLongOpt("distance-offset").withDescription("Distance offset (only for population-distance-model, default 0)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("value").create();
        Option distancePower = OptionBuilder.withLongOpt("distance-power").withDescription("Distance power (only for population-distance-model, default 1)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("value").create();

        options.addOption(randomFactor);
        options.addOption(populationOffset);
        options.addOption(populationPower);
        options.addOption(distanceOffset);
        options.addOption(distancePower);

        Option numMatrices = OptionBuilder.withLongOpt("num-matrices")
            .withDescription("Number of generated matrices (if no traffic pattern is specified it will be ignored, default 1)")
            .withType(PatternOptionBuilder.NUMBER_VALUE).hasArg().withArgName("matrices").create();
        options.addOption(numMatrices);

        Option trafficPattern = OptionBuilder.withLongOpt("traffic-pattern")
            .withDescription("Traffic pattern: " + StringUtils.join(StringUtils.toArray(trafficPatterns.keySet()), ", "))
            .hasArg().withArgName("patternname").create();
        options.addOption(trafficPattern);

        Option levelMatrix = OptionBuilder.withLongOpt("level-matrix-file")
            .withDescription("Input file with a 2D level matrix (only for population-distance-model)")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg().withArgName("file").create();
        options.addOption(levelMatrix);

        Option normalizationPattern = OptionBuilder.withLongOpt("normalization-pattern-file")
            .withDescription("Input file with a 2D matrix representing the normalization pattern")
            .withType(PatternOptionBuilder.FILE_VALUE)
            .hasArg().withArgName("file").create();
        options.addOption(normalizationPattern);
    }

    @Override
    public void executeFromCommandLine(String[] args) throws ParseException
    {
        long init = System.nanoTime();

        final CommandLineParser parser = new CommandLineParser();
        final CommandLine cli = parser.parse(options, args);

        int numNodes;
        NetPlan netPlan;

        if (cli.hasOption("num-nodes") && cli.hasOption("input-file"))
            throw new ParseException("'num-nodes' and 'input-file' are mutually exclusive");

        if (cli.hasOption("num-nodes"))
        {
            numNodes = ((Number) cli.getParsedOptionValue("num-nodes")).intValue();
            if (numNodes < 2) throw new Net2PlanException("Traffic matrix requires at least 2 nodes");

            netPlan = new NetPlan();
            for(int nodeId = 0; nodeId < numNodes; nodeId++)
                netPlan.addNode(0, 0, null, null);
        }
        else
        {
            netPlan = new NetPlan((File) cli.getParsedOptionValue("input-file"));
            numNodes = netPlan.getNumberOfNodes();
        }

        int numMatrices = 1;
        String trafficPattern = null;

        if (cli.hasOption("traffic-pattern"))
        {
            trafficPattern = cli.getOptionValue("traffic-pattern");
            if (!trafficPatterns.containsKey(trafficPattern)) throw new Net2PlanException("Unknown traffic pattern");

            if (cli.hasOption("num-matrices"))
            {
                numMatrices = ((Number) cli.getParsedOptionValue("num-matrices")).intValue();
                if (numMatrices < 1) throw new Net2PlanException("Number of traffic matrices must be positive");
            }
        }

        double[][][] trafficMatrices = new double[numMatrices][numNodes][numNodes];

        if (trafficPattern != null)
        {
            switch(trafficPattern)
            {
                case "uniform-random-10":
                    for(int tmId = 0; tmId < numMatrices; tmId++)
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.uniformRandom(numNodes, 0, 10);
                    break;

                case "uniform-random-100":
                    for(int tmId = 0; tmId < numMatrices; tmId++)
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.uniformRandom(numNodes, 0, 100);
                    break;

                case "uniform-random-bimodal-50-50":
                    for(int tmId = 0; tmId < numMatrices; tmId++)
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.bimodalUniformRandom(numNodes, 0.5, 0, 100, 0, 10);
                    break;

                case "uniform-random-bimodal-25-75":
                    for(int tmId = 0; tmId < numMatrices; tmId++)
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.bimodalUniformRandom(numNodes, 0.25, 0, 100, 0, 10);
                    break;

                case "population-distance-model":

                    double randomFactor = 0.5;
                    double populationOffset = 0;
                    double populationPower = 1;
                    double distanceOffset = 0;
                    double distancePower = 1;

                    if (cli.hasOption("random-factor")) randomFactor = ((Number) cli.getParsedOptionValue("random-factor")).doubleValue();
                    if (cli.hasOption("pop-offset")) randomFactor = ((Number) cli.getParsedOptionValue("pop-offset")).doubleValue();
                    if (cli.hasOption("pop-power")) randomFactor = ((Number) cli.getParsedOptionValue("pop-power")).doubleValue();
                    if (cli.hasOption("distance-offset")) randomFactor = ((Number) cli.getParsedOptionValue("distance-offset")).doubleValue();
                    if (cli.hasOption("distance-power")) randomFactor = ((Number) cli.getParsedOptionValue("distance-power")).doubleValue();

                    if (!cli.hasOption("level-matrix-file")) throw new Net2PlanException("The level-matrix file is required");
                    double[][] levelMatrix = TrafficMatrixGenerationModels.read2DMatrixFromFile((File) cli.getParsedOptionValue("level-matrix-file"), true);

                    double[][] distanceMatrix = netPlan.getPhysicalDistanceMatrix();
                    int[] populationVector = StringUtils.toIntArray(netPlan.getNodeAttributeVector("population"), 0);
                    int[] levelVector = StringUtils.toIntArray(netPlan.getNodeAttributeVector("level"), 1);

                    for(int tmId = 0; tmId < numMatrices; tmId++)
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.populationDistanceModel(distanceMatrix, populationVector, levelVector, levelMatrix, randomFactor, populationOffset, populationPower, distanceOffset, distancePower);

                    break;

                default:
                    throw new RuntimeException("Bad");
            }
        }
        else
        {
            trafficMatrices[0] = netPlan.getTrafficMatrix();
        }

        if (cli.hasOption("normalization-pattern-file"))
        {
            double[] normalizationPatternVector;
            int patternId;

            File normalizationPattern = (File) cli.getParsedOptionValue("normalization-pattern-file");
            double[][] normalizationPatternMatrix = TrafficMatrixGenerationModels.read2DMatrixFromFile(normalizationPattern, true);

            if (normalizationPatternMatrix.length == 1 && normalizationPatternMatrix[0].length == 1)
            {
                patternId = 0;
                normalizationPatternVector = new double[] { normalizationPatternMatrix[0][0] };
            }
            else if (normalizationPatternMatrix.length == 1 && normalizationPatternMatrix[0].length > 1)
            {
                patternId = 1;
                normalizationPatternVector = normalizationPatternMatrix[0];
            }
            else if (normalizationPatternMatrix.length > 1 && normalizationPatternMatrix[0].length == 1)
            {
                patternId = 2;
                normalizationPatternVector = DoubleUtils.selectColumn(normalizationPatternMatrix, 0);
            }
            else
            {
                throw new Net2PlanException("Bad normalization pattern - Neither a scalar, a column vector or a row vector");
            }

            for(int tmId = 0; tmId < numMatrices; tmId++)
            {
                switch(patternId)
                {
                    case 0:
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_totalTraffic(trafficMatrices[tmId], normalizationPatternVector[0]);
                        break;

                    case 1:
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_incomingTraffic(trafficMatrices[tmId], normalizationPatternVector);
                        break;

                    case 2:
                        trafficMatrices[tmId] = TrafficMatrixGenerationModels.normalizationPattern_outgoingTraffic(trafficMatrices[tmId], normalizationPatternVector);
                        break;

                    default:
                        throw new RuntimeException("Bad");
                }
            }
        }

        List<NetPlan> outputDemandSets = new LinkedList<NetPlan>();

        for(int tmId = 0; tmId < numMatrices; tmId++)
        {
            NetPlan aux = new NetPlan();

            aux.setTrafficMatrix(trafficMatrices[tmId]);
            outputDemandSets.add(aux);

            trafficMatrices[tmId] = null;
        }

        File outputFile = (File) cli.getParsedOptionValue("output-file");

        if (outputDemandSets.size() == 1)
        {
            outputDemandSets.get(0).saveToFile(outputFile);
        }
        else
        {
            String templateFileName = outputFile.getAbsoluteFile().toString();
            if (templateFileName.endsWith(".n2p"))
                templateFileName = templateFileName.substring(0, templateFileName.lastIndexOf("."));

            ListIterator<NetPlan> netPlanIt = outputDemandSets.listIterator();
            while(netPlanIt.hasNext())
            {
                int tmId = netPlanIt.nextIndex();
                netPlanIt.next().saveToFile(new File(templateFileName + "-tm" + tmId + ".n2p"));
            }                
        }

        long end = System.nanoTime();

        System.out.println(String.format("%n%nTraffic matrix generation finished successfully in %f seconds", (end - init) / 1e9));
    }

    @Override
    public String getCommandLineHelp()
    {
        return "Assists users in the process of "
                + "generating and normalizing traffic matrices i.e. following "
                + "random models found in the literature";
    }

    @Override
    public Options getCommandLineOptions() { return options; }
}