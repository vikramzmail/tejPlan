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

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that a traffic generator can be run
 * in the <code>Time-varying traffic simulator</code> included within <code>Net2Plan</code>.</p>
 * 
 * <p><b>Important</b>: Contrary to the other simulation models, in this type of
 * simulation events are scheduled in a period fashion by the kernel, and the generator
 * is in charge of return only the new traffic volume for each demand.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public interface ITrafficGenerator extends IExternal
{
    /**
     * Processes a new event.
     *
     * @param netPlan Current network design (including traffic demands)
     * @param currentDate Current date
     * @return New traffic demand volumes
     * @since 0.2.1
     */
    public double[] execute(NetPlan netPlan, Calendar currentDate);

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

    /**
     * Initializes the traffic generator (i.e. reading input parameters)
     *
     * @param netPlan Complete network design (including traffic demands)
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.1
     */
    public void initialize(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);
}
