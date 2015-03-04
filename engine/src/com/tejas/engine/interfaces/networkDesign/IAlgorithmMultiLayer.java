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

package com.tejas.engine.interfaces.networkDesign;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that an multilayer algorithm can be run in <code>Net2Plan</code>.</p>
 *
 * <p>Integration of new multilayer algorithms follows a similar scheme as for
 * {@link com.tejas.engine.interfaces.networkDesign.IAlgorithm IAlgorithm}. In this case,
 * the algorithm receives a list of {@link com.tejas.engine.interfaces.networkDesign.NetPlan NetPlan}
 * objects, each one representing a network layer.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public interface IAlgorithmMultiLayer extends IExternal
{
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
     * Execute the algorithm.
     *
     * @param netPlans A set of network plans which serves as input and output
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @return An output <code>String</code>
     * @since 0.2.1
     */
    public String executeAlgorithm(List<NetPlan> netPlans, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);
}
