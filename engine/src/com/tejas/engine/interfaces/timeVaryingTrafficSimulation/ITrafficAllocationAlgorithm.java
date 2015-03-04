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

package com.tejas.engine.interfaces.timeVaryingTrafficSimulation;

import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TimeVaryingNetState;
import com.tejas.engine.interfaces.timeVaryingTrafficSimulation.TrafficAllocationAction;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that a traffic allocation algorithm can be
 * run in <code>Net2Plan</code>.</p>
 * 
 * <p>Given the new traffic volume for every demand, the algorithm is in charge
 * to allocate the traffic, being able to add links, to modify their capacity, to establish
 * new routes...
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public interface ITrafficAllocationAlgorithm extends IExternal
{
    /**
     * Processes a new event.
     *
     * @param netPlan Network plan
     * @param netState Current network state
     * @param h_d New traffic demand volumes
     * @param currentDate Current date
     * @return A list of actions (add/modify/remove link/segment/route/SRG)
     * @since 0.2.1
     */
    public List<TrafficAllocationAction> processEvent(NetPlan netPlan, TimeVaryingNetState netState, double[] h_d, Calendar currentDate);

    /**
     * Initializes the allocation algorithm (i.e. reading input parameters).
     *
     * @param netPlan Network plan
     * @param netState Current network state
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.1
     */
    public void initialize(NetPlan netPlan, TimeVaryingNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);

    /**
     * Returns an algorithm-specific report.
     *
     * @param output Container for the report
     * @param finishDate Current time
     * @return Report title (return <code>null</code>, or an empty <code>output</code> to omit it)
     * @since 0.2.1
     */
    public String finish(StringBuilder output, Calendar finishDate);
    
    /**
     * Performs some transitory-finished action.
     * 
     * @param currentDate Current data
     * @since 0.2.3
     */
    public void finishTransitory(Calendar currentDate);

    /**
     * Returns the description.
     *
     * @return Description
     * @since 0.2.1
     */
    @Override
    public String getDescription();

    /**
     * Returns the list of required parameters, where the first item of each element is the parameter name, the second one is the parameter value, and the third one is the parameter description.
     *
     * @return List of specific parameters
     * @since 0.2.1
     */
    @Override
    public List<Triple<String, String, String>> getParameters();
}
