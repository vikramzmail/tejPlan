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

package com.tejas.engine.libraries;

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import com.jom.OptimizationProblem;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.RandomUtils;
import com.tejas.engine.utils.Constants.ShortestPathType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * <p>Set of methods implementing different traffic generation models based on traffic matrices. All of them are based on those detailed in [1].</p>
 *
 * <p><b>Important</b>: In <code>Net2Plan</code> self-demands are not allowed, thus the diagonal of the traffic matrices must be always zero.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see <code>[1] R. S. Cahn, <i>Wide area network design: concepts and tools for optimization</i>, Morgan Kaufmann Publishers Inc., 1998</code>
 */
public class TrafficMatrixGenerationModels
{
    /**
     * Returns the activity factor of a node, given the current UTC hour and its timezone (see [1]).
     *
     * @see [1] J. Milbrandt, M. Menth, S. Kopf, "Adaptive Bandwidth Allocation: Impact of Traffic Demand Models for Wide Area Networks," University of Würzburg, Germany, Report No. 363, June 2005
     *
     * @param UTC Universal Time Coordinated hour in decimal format (e.g. 10:30h is represented as 10.5)
     * @param timezoneOffset Timezone offset from UTC in range [-12, 12]
     * @param minValueFactor Minimum value at low-traffic hour in range [0,1] (in ref. [1] is equal to 0.1)
     * @param maxValueFactor Maximum value at peak hour in range [0,1] (in ref. [1] is equal to 1)
     * @return Activity factor
     * @since 0.2.2
     */
    public static double activityFactor(double UTC, int timezoneOffset, double minValueFactor, double maxValueFactor)
    {
	if (maxValueFactor < 0 || maxValueFactor > 1) throw new RuntimeException("'maxValueFactor' must be in range [0, 1]");
	if (minValueFactor < 0 || minValueFactor > 1) throw new RuntimeException("'minValueFactor' must be in range [0, 1]");
	if (minValueFactor > maxValueFactor) throw new RuntimeException("'minValueFactor' must be lower or equal than 'maxValueFactor'");

	double activity = minValueFactor;
	double localTime = (UTC + timezoneOffset + 24) % 24;

	if (localTime >= 6)
	    activity = maxValueFactor - (maxValueFactor - minValueFactor) * Math.pow(Math.cos(Math.PI * (localTime - 6) / 18), 10);

	return activity;
    }

    /**
     * Generates a traffic matrix using a bimodal uniform random distribution, that is, a distribution in which a value is taken for a uniform random distribution with probability <i>p</i>, and from the other one with probability <i>1-p</i>.
     *
     * @param N Number of nodes
     * @param percentageThreshold Mixture coefficient
     * @param minValueClass1 Minimum traffic value for class 1
     * @param maxValueClass1 Maximum traffic value for class 1
     * @param minValueClass2 Minimum traffic value for class 2
     * @param maxValueClass2 Maximum traffic value for class 2
     * @return Traffic matrix
     * @since 0.2.0
     */
    public static double[][] bimodalUniformRandom(int N, double percentageThreshold, double minValueClass1, double maxValueClass1, double minValueClass2, double maxValueClass2)
    {
        DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);
        
        IntArrayList list = new IntArrayList();
        for(int i = 0; i < N; i++) list.add(i);
        list.trimToSize();
        
        for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
	    list.shuffle();
            
            for (int pos = 0; pos < N; pos++)
            {
                if (ingressNodeId == list.getQuick(pos)) continue;
                
                if ((double) pos/N < percentageThreshold)
                    trafficMatrix.setQuick(ingressNodeId, list.getQuick(pos), minValueClass1 + Math.random() * (maxValueClass1 - minValueClass1));
                else
                    trafficMatrix.setQuick(ingressNodeId, list.getQuick(pos), minValueClass2 + Math.random() * (maxValueClass2 - minValueClass2));
            }
        }

        return trafficMatrix.toArray();
    }

    /**
     * Generates a constant traffic matrix.
     *
     * @param N Number of nodes
     * @param value Traffic value
     * @return Traffic matrix
     * @since 0.2.3
     */
    public static double[][] constant(int N, double value)
    {
	DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N, value);
	for(int nodeId = 0; nodeId < N; nodeId++) trafficMatrix.setQuick(nodeId, nodeId, 0);

	return trafficMatrix.toArray();
    }
    
    /**
     * <p>Generates a traffic matrix using a 'gravity model' (see [1]). This basic model predicts the demand between node <i>i</i> and <i>j</i> as:</p>
     * <p><i>s<sub>ij</sub>=Ct<sub>e(i)</sub>t<sub>x(j)</sub></i></p>
     * <p>where <i>C</i> is a normalization constant that makes the sum of estimated demands equal to the total traffic entering/leaving the network.</p>
     * 
     * @see [1] Anders Gunnar, Mikael Johansson, Thomas Telkamp, "Traffic Matrix Estimation on a Large IP Backbone – A Comparison on Real Data," in Proc. of IMC'04, October 2004
     * 
     * @param ingressTrafficPerNode Ingress traffic per node
     * @param egressTrafficPerNode Egress traffic per node
     * @return Traffic matrix
     * @since 0.2.3
     */
    public static double[][] gravityModel(double[] ingressTrafficPerNode, double[] egressTrafficPerNode)
    {
        int N = ingressTrafficPerNode.length;
        
        double totalIngressTraffic = DoubleUtils.sum(ingressTrafficPerNode);
        double totalEgressTraffic = DoubleUtils.sum(egressTrafficPerNode);
        if (totalIngressTraffic != totalEgressTraffic)
            throw new Net2PlanException(String.format("Total ingress traffic (%f E) must be equal to the total egress traffic (%f E)", totalIngressTraffic, totalEgressTraffic));
        
        double[][] trafficMatrix = new double[N][N];
        for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
            {
                if (ingressNodeId == egressNodeId) continue;

                trafficMatrix[ingressNodeId][egressNodeId] = ingressTrafficPerNode[ingressNodeId] * egressTrafficPerNode[egressNodeId] / totalEgressTraffic;
            }
        }
        
        return trafficMatrix;
    }

    /**
     * Generates a traffic matrix using a uniform random distribution.
     *
     * @param N Number of nodes
     * @param minValue Minimum traffic value
     * @param maxValue Maximum traffic value
     * @return Traffic matrix
     * @since 0.2.0
     */
    public static double[][] uniformRandom(int N, double minValue, double maxValue)
    {
	DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.random(N, N);
	trafficMatrix.assign(DoubleFunctions.mult(maxValue - minValue));
	trafficMatrix.assign(DoubleFunctions.plus(minValue));

	for(int nodeId = 0; nodeId < N; nodeId++) trafficMatrix.setQuick(nodeId, nodeId, 0);

	return trafficMatrix.toArray();
    }

    /**
     * Generates a traffic matrix using the population-distance model detailed in [1].
     *
     * @param distanceMatrix Distance matrix, where cell (<i>i</i>, <i>j</i>) represents the distance from node <i>i</i> to node <i>j</i>
     * @param populationVector Vector with <i>N</i> elements in which each element is the population of the corresponding node
     * @param levelVector Vector with <i>N</i> elements in which each element is the level (i.e. type) of the corresponding node
     * @param levelMatrix Level matrix
     * @param randomFactor Random factor
     * @param populationOffset Population offset
     * @param populationPower Population power
     * @param distanceOffset Distance offset
     * @param distancePower Distance power
     * @return Traffic matrix
     * @since 0.2.0
     * @see For more information, see <code>[1]</code>
     */
    public static double[][] populationDistanceModel(double[][] distanceMatrix, int[] populationVector, int[] levelVector, double[][] levelMatrix, double randomFactor, double populationOffset, double populationPower, double distanceOffset, double distancePower)
    {
        int N = distanceMatrix.length;

        DoubleMatrix2D trafficMatrix = DoubleFactory2D.dense.make(N, N);

        // First, compute dist_max and pop_max
        double dist_max = -1;
        int pop_max = -1;
        for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            pop_max = Math.max(pop_max, populationVector[ingressNodeId]);

            for (int egressNodeId = ingressNodeId + 1; egressNodeId < N; egressNodeId++)
                dist_max = Math.max(dist_max, distanceMatrix[ingressNodeId][egressNodeId]);
        }

        // Then, compute the traffic matrix
        for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
	{
            for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
            {
                if (ingressNodeId == egressNodeId) continue;

                double populationCoeff = Math.pow(populationOffset + (double) populationVector[ingressNodeId] * (double) populationVector[egressNodeId] / Math.pow(pop_max, 2), populationPower);
                double distanceCoeff = Math.pow(distanceOffset + distanceMatrix[ingressNodeId][egressNodeId] / dist_max, distancePower);
		distanceCoeff = Double.isNaN(distanceCoeff) ? 1 : distanceCoeff;
                double levelCoeff = levelMatrix[levelVector[ingressNodeId]-1][levelVector[egressNodeId]-1];
                double randomCoeff = 1 - randomFactor + 2 * randomFactor * Math.random();

                trafficMatrix.setQuick(ingressNodeId, egressNodeId, levelCoeff * randomCoeff * populationCoeff / distanceCoeff);
            }
	}

        return trafficMatrix.toArray();
    }

    /**
     * Normalizes the input traffic matrix with respect to a given outgoing traffic vector.
     *
     * @param trafficMatrix Input traffic matrix
     * @param outgoingTraffic Vector of outgoing traffic to each node
     * @return Traffic matrix normalized to the specified outgoing traffic per node
     * @since 0.2.0
     */
    public static double[][] normalizationPattern_outgoingTraffic(double[][] trafficMatrix, double[] outgoingTraffic)
    {
	checkTrafficMatrix(trafficMatrix);

	DoubleMatrix2D auxTrafficMatrix = DoubleFactory2D.dense.make(trafficMatrix);

	int N = auxTrafficMatrix.rows();
	if (outgoingTraffic.length != N) throw new Net2PlanException("'incomingTraffic' size doesn't match the number of nodes");

	DoubleMatrix2D trafficMatrix_out = auxTrafficMatrix.copy();

	for(int rowId = 0; rowId < N; rowId++)
	{
	    DoubleMatrix1D row = trafficMatrix_out.viewRow(rowId);
	    double outgoingTraffic_old = row.zSum();
	    if (outgoingTraffic_old == 0) continue;

	    row.assign(DoubleFunctions.mult(outgoingTraffic[rowId] / outgoingTraffic_old));
	}

	return trafficMatrix_out.toArray();
    }

    /**
     * Normalizes the input traffic matrix with respect to a given incoming traffic vector.
     *
     * @param trafficMatrix Input traffic matrix
     * @param incomingTraffic Vector of incoming traffic to each node
     * @return Traffic matrix normalized to the specified incoming traffic per node
     * @since 0.2.0
     */
    public static double[][] normalizationPattern_incomingTraffic(double[][] trafficMatrix, double[] incomingTraffic)
    {
	checkTrafficMatrix(trafficMatrix);

	DoubleMatrix2D auxTrafficMatrix = DoubleFactory2D.dense.make(trafficMatrix);

	int N = auxTrafficMatrix.rows();
	if (incomingTraffic.length != N) throw new Net2PlanException("'incomingTraffic' size doesn't match the number of nodes");

	DoubleMatrix2D trafficMatrix_out = auxTrafficMatrix.copy();

	for(int columnId = 0; columnId < N; columnId++)
	{
	    DoubleMatrix1D column = trafficMatrix_out.viewColumn(columnId);
	    double incomingTraffic_old = column.zSum();
	    if (incomingTraffic_old == 0) continue;

	    column.assign(DoubleFunctions.mult(incomingTraffic[columnId] / incomingTraffic_old));
	}

	return trafficMatrix_out.toArray();
    }

    /**
     * Normalizes the input traffic matrix so that the sum of all entries is equal to a given value.
     *
     * @param trafficMatrix Input traffic matrix
     * @param totalTraffic Total traffic expected
     * @return Traffic matrix normalized to the specified total traffic
     * @since 0.2.0
     */
    public static double[][] normalizationPattern_totalTraffic(double[][] trafficMatrix, double totalTraffic)
    {
	checkTrafficMatrix(trafficMatrix);

	DoubleMatrix2D auxTrafficMatrix = DoubleFactory2D.dense.make(trafficMatrix);
	double H = auxTrafficMatrix.zSum();
	if (H == 0) return auxTrafficMatrix.copy().toArray();

	double scaleFactor = totalTraffic / H;
	return auxTrafficMatrix.copy().assign(DoubleFunctions.mult(scaleFactor)).toArray();
    }

    /**
     * <p>Normalizes the load of a traffic matrix in an effort to assess the merits
     * of the algorithms for different traffic load conditions. The value
     * <code>load</code> represents the average amount of traffic between two nodes,
     * among all the entries, during the highest loaded time, measured in number of links.</p>
     *
     * <p>Consequently, a value of <code>load</code>=0.5 corresponds to the case
     * when the (maximum) average traffic between two nodes equals 50% of a single link capacity.
     * In constrast, a value of <code>load</code>=10 captures cases in which the
     * (maximum) average traffic between two nodes fills on average 10 links.
     *
     * @param trafficMatrix Input traffic matrix
     * @param linkCapacity Link capacity (in same units as <code>trafficMatrix</code>)
     * @param load Load factor (measured in number of links). The highest value of the resulting traffic matrix will be equal to <code>load</code>*<code>linkCapacity</code>
     * @return Traffic matrix normalized to the specified link load factor
     * @since 0.2.2
     */
    public static double[][] normalizeToLinkLoad(double[][] trafficMatrix, double linkCapacity, double load)
    {
	checkTrafficMatrix(trafficMatrix);

	DoubleMatrix2D auxTrafficMatrix = DoubleFactory2D.dense.make(trafficMatrix);

	double maxValue = auxTrafficMatrix.getMaxLocation()[0];
	if (maxValue == 0) return auxTrafficMatrix.copy().toArray();

	double scaleFactor = linkCapacity * load / maxValue;
	return auxTrafficMatrix.copy().assign(DoubleFunctions.mult(scaleFactor)).toArray();
    }

    private static void checkTrafficMatrix(double[][] trafficMatrix)
    {
	if (trafficMatrix == null) throw new NullPointerException("Traffic matrix cannot be null");

	int N = trafficMatrix.length;

	for(int rowId = 0; rowId < N; rowId++)
	{
	    if (trafficMatrix[rowId].length != N) throw new RuntimeException("Traffic matrix must be a square matrix");

	    for(int columnId = 0; columnId < N; columnId++)
	    {
		if (rowId == columnId && trafficMatrix[rowId][columnId] != 0) throw new RuntimeException("Self-demands are not allowed");
		if (trafficMatrix[rowId][columnId] < 0) throw new RuntimeException("Offered traffic from node " + rowId + " to node " + columnId + " must be greater or equal than zero");
	    }
	}
    }
    
    /**
     * Reads a 2D matrix from a file. Each line in the file is a row of the matrix.
     * Items can be separated by spaces or tabs. Lines starting with '#' are skipped.
     * 
     * @param f Input file
     * @param checkSameColumns Checks whether the number of columns is the same for every row
     * @return A 2D matrix
     * @since 0.2.2
     */
    public static double[][] read2DMatrixFromFile(File f, boolean checkSameColumns)
    {
        try
        {
            List<DoubleArrayList> list;
            try (BufferedReader in = new BufferedReader(new FileReader(f)))
            {
                list = new LinkedList<DoubleArrayList>();
                String line;
                while((line = in.readLine()) != null)
                {
                    if (line.startsWith("#")) continue;
                    
                    DoubleArrayList aux = new DoubleArrayList();
                    StringTokenizer tokenizer = new StringTokenizer(line, " \t");
                    
                    while(tokenizer.hasMoreTokens())
                        aux.add(Double.parseDouble(tokenizer.nextToken()));
                    
                    aux.trimToSize();
                    list.add(aux);
                }
            }
            
            int numRows = list.size();
            if (numRows == 0) throw new Net2PlanException("Empty matrix");
            
            double[][] matrix = new double[numRows][];
            ListIterator<DoubleArrayList> it = list.listIterator();
            while(it.hasNext())
                matrix[it.nextIndex()] = it.next().elements();
            
            if (checkSameColumns)
            {
                int columns = matrix[0].length;
                for(int rowId = 1; rowId < matrix.length; rowId++)
                    if (matrix[rowId].length != columns)
                        throw new Net2PlanException("All rows don't have the same number of columns");
            }
            
            return matrix;
        }
        catch(Throwable e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Symmetrizes the input traffic matrix setting each node-pair traffic value 
     * equal to the average between the traffic in both directions.
     * 
     * @param trafficMatrix Traffic matrix
     * @since 0.2.3
     */
    public static void symmetrizeTrafficMatrix(double[][] trafficMatrix)
    {
        checkTrafficMatrix(trafficMatrix);
        
        int N = trafficMatrix.length;
        
        for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            for(int egressNodeId = ingressNodeId + 1; egressNodeId < N; egressNodeId++)
            {
                double meanValue = (trafficMatrix[ingressNodeId][egressNodeId] + trafficMatrix[egressNodeId][ingressNodeId]) / 2.0;
                
                trafficMatrix[ingressNodeId][egressNodeId] = meanValue;
                trafficMatrix[egressNodeId][ingressNodeId] = meanValue;
            }
        }
    }
    
    /**
     * <p>Returns the maximum scaled version of the offered traffic vector so 
     * that the network capacity (summation of capacity of all links) is exhausted.</p>
     * 
     * @param netPlan Network design with physical topology (nodes and links) and a set of demands
     * @param shortestPathType Shortest path type (hops or km)
     * @return Scaled version of the offered traffic vector
     * @since 0.2.3
     */
    public static double[] normalizeTraffic_networkCapacity(NetPlan netPlan, ShortestPathType shortestPathType)
    {
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a demand set are required");
        
        boolean isConnected = GraphUtils.isConnected(netPlan);
        if (!isConnected) throw new Net2PlanException("Physical topology must be connected");
        
        double[] linkWeight = shortestPathType == ShortestPathType.KM ? netPlan.getLinkLengthInKmVector() : DoubleUtils.ones(E);
        CandidatePathList cpl = new CandidatePathList(netPlan, linkWeight, "K", "1");
        
        double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        int[] d_p = cpl.getDemandIdsPerPath();
        double[] h_p = DoubleUtils.select(h_d, d_p);
        
        double[] costVector = DoubleUtils.ones(E);
        double[] n_p = cpl.computeWeightPerPath(costVector);
        
        double totalInNetworkTraffic = DoubleUtils.scalarProduct(h_p, n_p);
        
        double[] u_e = netPlan.getLinkCapacityInErlangsVector();
        double U_e = DoubleUtils.sum(u_e);
        
        double alphaFactor = totalInNetworkTraffic == 0 ? 0 : U_e / totalInNetworkTraffic;
        
        double[] new_h_d = DoubleUtils.mult(h_d, alphaFactor);
        return new_h_d;
    }

    /**
     * <p>Returns the maximum scaled version of the offered traffic vector that 
     * can be carried by the network, provided that no link is oversubscribed. 
     * It is assumed no protection capacity is reserved.</p>
     * 
     * <p><b>Important</b>: <code>JOM</code> library is required here.</p>
     * 
     * @param netPlan Network design with physical topology (nodes and links) and a set of demands
     * @param solverName The solver name to be used by JOM
     * @param solverLibraryName The solver library full or relative path, to be used by JOM. Leave blank to use JOM default
     * @return Scaled version of the offered traffic vector
     * @since 0.2.3
     */
    public static double[] normalizeTraffic_linkCapacity_xde(NetPlan netPlan, String solverName, String solverLibraryName)
    {
        int N = netPlan.getNumberOfNodes();
        int E = netPlan.getNumberOfLinks();
        int D = netPlan.getNumberOfDemands();
        
        if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a demand set are required");
        
        boolean isConnected = GraphUtils.isConnected(netPlan);
        if (!isConnected) throw new Net2PlanException("Physical topology must be connected");
        
        double[] u_e = netPlan.getLinkCapacityInErlangsVector();
        double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
        
        OptimizationProblem op = new OptimizationProblem();
        
        op.setInputParameter("u_e", u_e, "row");
        op.setInputParameter("h_d", h_d, "row");
        op.setInputParameter("A_ne", GraphUtils.getNodeLinkIncidenceMatrix(netPlan));
        op.setInputParameter("A_nd", GraphUtils.getNodeDemandIncidenceMatrix(netPlan));
        
        op.addDecisionVariable("x_de", false , new int[] {D, E}, 0, Double.MAX_VALUE);
        op.addDecisionVariable("alphaFactor", false, new int[] {1, 1}, 0, Double.MAX_VALUE);
        
        op.setObjectiveFunction("maximize", "alphaFactor");
        
        op.addConstraint("A_ne * x_de' == alphaFactor * A_nd");
        op.addConstraint("h_d * x_de <= u_e");

	op.solve(solverName, "solverLibraryName" , solverLibraryName);
        
        if (!op.solutionIsOptimal()) throw new Net2PlanException("A solution cannot be found");
        double alphaFactor = op.getPrimalSolution("alphaFactor").toValue();
        
        double[] new_h_d = DoubleUtils.mult(h_d, alphaFactor);
        return new_h_d;
    }
    
    /**
     * <p>Computes a set of matrices from a seminal one, using a traffic forecast 
     * based on the compound annual growth rate (CAGR) concept.</p>
     * 
     * <p>Traffic forecast for node pair <i>(i,j)</i> on year <i>k</i> from the 
     * reference one is given by:</p>
     * 
     * <p><i>TM(i,j,k) = TM(i,j,0) * (1+CAGR<sup>k</sup>)</i></p>
     * 
     * @param trafficMatrix Seminal traffic matrix
     * @param cagr Compound Annual Growth Rate (0.2 means an increase of 20% with respect to the previous year)
     * @param numMatrices Number of matrices to generate
     * @return New traffic matrices
     * @since 0.2.3
     */
    public static List<double[][]> computeMatricesCAGR(double[][] trafficMatrix, double cagr, int numMatrices)
    {
        checkTrafficMatrix(trafficMatrix);
        
        if (cagr <= 0) throw new Net2PlanException("Compound annual growth rate must be greater than zero");
        if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");
        
        List<double[][]> newMatrices = new LinkedList<double[][]>();
        for(int matrixId = 0; matrixId < numMatrices; matrixId++)
        {
            double multiplicativeFactor = Math.pow(1 + cagr, matrixId + 1);
            newMatrices.add(DoubleUtils.mult(trafficMatrix, multiplicativeFactor));
        }
        
        return newMatrices;
    }
    
    /**
     * Computes a set of matrices from a seminal one, using a random Gaussian distribution.
     * 
     * @param trafficMatrix Seminal traffic matrix
     * @param cv Coefficient of variation
     * @param maxRelativeVariation Maximum relative variation from the mean value (0.2 means a maximum variation of +-20%)
     * @param numMatrices Number of matrices to generate
     * @return New traffic matrices
     * @since 0.2.3
     */
    public static List<double[][]> computeMatricesRandomGaussianVariation(double[][] trafficMatrix, double cv, double maxRelativeVariation, int numMatrices)
    {
        checkTrafficMatrix(trafficMatrix);
        
        if (cv <= 0) throw new Net2PlanException("Coefficient of variation must be greater than zero");
        if (maxRelativeVariation <= 0) throw new Net2PlanException("Maximum relative variation must be greater than zero");
        if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");
        
        int N = trafficMatrix.length;
        Random r = new Random();
        List<double[][]> newMatrices = new LinkedList<double[][]>();
        
        for(int matrixId = 0; matrixId < numMatrices; matrixId++)
        {
            double[][] newTrafficMatrix = new double[N][N];
            
            for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
            {
                for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                {
                    if (trafficMatrix[ingressNodeId][egressNodeId] == 0) continue;
                    
                    double sigma = cv * trafficMatrix[ingressNodeId][egressNodeId];
                    double variationFromMeanValue = r.nextGaussian() * sigma;
                    if (variationFromMeanValue > maxRelativeVariation) variationFromMeanValue = maxRelativeVariation;
                    else if (variationFromMeanValue < -maxRelativeVariation) variationFromMeanValue = -maxRelativeVariation;
                    
                    newTrafficMatrix[ingressNodeId][egressNodeId] = trafficMatrix[ingressNodeId][egressNodeId] * (1 + variationFromMeanValue);
                    if (newTrafficMatrix[ingressNodeId][egressNodeId] < 0) newTrafficMatrix[ingressNodeId][egressNodeId] = 0;
                }
            }

            newMatrices.add(newTrafficMatrix);
        }
        
        return newMatrices;
    }

    /**
     * Computes a set of matrices from a seminal one, using a random uniform distribution.
     * 
     * @param trafficMatrix Seminal traffic matrix
     * @param maxRelativeVariation Maximum relative variation from the mean value (0.2 means a maximum variation of +-20%)
     * @param numMatrices Number of matrices to generate
     * @return New traffic matrices
     * @since 0.2.3
     */
    public static List<double[][]> computeMatricesRandomUniformVariation(double[][] trafficMatrix, double maxRelativeVariation, int numMatrices)
    {
        checkTrafficMatrix(trafficMatrix);
        
        if (maxRelativeVariation <= 0) throw new Net2PlanException("Maximum relative variation must be greater than zero");
        if (numMatrices < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");
        
        int N = trafficMatrix.length;
        Random r = new Random();
        List<double[][]> newMatrices = new LinkedList<double[][]>();
        
        for(int matrixId = 0; matrixId < numMatrices; matrixId++)
        {
            double[][] newTrafficMatrix = new double[N][N];
            
            for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
            {
                for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                {
                    if (trafficMatrix[ingressNodeId][egressNodeId] == 0) continue;
                    
                    double variationFromMeanValue = RandomUtils.random(-maxRelativeVariation, maxRelativeVariation, r);
                    
                    newTrafficMatrix[ingressNodeId][egressNodeId] = trafficMatrix[ingressNodeId][egressNodeId] * (1 + variationFromMeanValue);
                    if (newTrafficMatrix[ingressNodeId][egressNodeId] < 0) newTrafficMatrix[ingressNodeId][egressNodeId] = 0;
                }
            }

            newMatrices.add(newTrafficMatrix);
        }
        
        return newMatrices;
    }
}