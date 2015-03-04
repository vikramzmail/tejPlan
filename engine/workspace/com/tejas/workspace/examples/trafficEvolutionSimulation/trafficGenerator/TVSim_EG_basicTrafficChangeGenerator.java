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

package com.tejas.workspace.examples.trafficEvolutionSimulation.trafficGenerator;

import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.ITrafficGenerator;
import com.tejas.engine.utils.RandomUtils;
import com.tejas.engine.utils.Triple;

import java.util.*;

/**
 * <p>Updates the current traffic demand volumes at each time slot using a
 * random Gaussian model of mean value equal to the original offered demand volume
 * and coefficient of variation (quotient between standard deviation and mean value)
 * given as a parameter.</p>
 * 
 * <p>The coefficient of variation is useful because the standard deviation of 
 * data must always be understood in the context of the mean of the data. 
 * In contrast, the actual value of the CV is independent of the unit in which 
 * the measurement has been taken, so it is a dimensionless number.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.1, January 2014
 */
public class TVSim_EG_basicTrafficChangeGenerator implements ITrafficGenerator
{
    private double cv, truncationParameter;
    private double[] h_d;
    private Random r;

    @Override
    public double[] execute(NetPlan netPlan, Calendar currentDate)
    {
        int D = h_d.length;
        double[] new_h_d = new double[D];
        for(int demandId = 0; demandId < D; demandId++)
        {
            double sigma = h_d[demandId] * cv;
            double variationFromMeanValue = r.nextGaussian() * sigma;
            if (variationFromMeanValue > truncationParameter * sigma) variationFromMeanValue = truncationParameter * sigma;
            else if (variationFromMeanValue < -truncationParameter * sigma) variationFromMeanValue = -truncationParameter * sigma;

            new_h_d[demandId] = h_d[demandId] + variationFromMeanValue;
            if (new_h_d[demandId] < 0) new_h_d[demandId] = 0;
        }

	return new_h_d;
    }

    @Override
    public String getDescription()
    {
	return "Updates the current traffic demand volumes at each time slot using a random Gaussian model of mean value equal to the original offered demand volume and coefficient of variation given as a parameter";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
	List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
	parameters.add(Triple.of("cv", "1", "Coefficient of variation (quotient between standard deviation and mean value)"));
	parameters.add(Triple.of("truncationParameter", "5", "Maximum deviation from the mean value (measured in units of the number of standard deviations away from the mean)"));
	parameters.add(Triple.of("randomSeed", "-1", "Seed for the random generator (-1 means random)"));

	return parameters;
    }

    @Override
    public void initialize(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
	cv = Double.parseDouble(algorithmParameters.get("cv"));
	if (cv < 0) throw new Net2PlanException("'cv' must be greater or equal than zero");

    	truncationParameter = Double.parseDouble(algorithmParameters.get("truncationParameter"));
	if (truncationParameter < 0) throw new Net2PlanException("'truncationParameter' must be greater or equal than zero");
        
	long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
	if (seed == -1) seed = RandomUtils.random(0, Long.MAX_VALUE - 1);
	r = new Random(seed);
        
        h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
    }
}