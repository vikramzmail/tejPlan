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

package com.tejas.engine.interfaces.cacSimulation;

import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Contract that must be fulfilled such that a call admission control algorithm can be run in the <code>Connection simulator</code> included within <code>Net2Plan</code>.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface ICACAlgorithm extends IExternal
{
    /**
     * Returns the description.
     *
     * @return Description
     * @since 0.2.0
     */
    @Override
    public String getDescription();

    /**
     * Returns the list of required parameters, where the first item of each element is the parameter name, the second one is the parameter value, and the third one is the parameter description.
     *
     * @return List of specific parameters
     * @since 0.2.0
     */
    @Override
    public List<Triple<String, String, String>> getParameters();

    /**
     * Initializes the CAC algorithm (i.e. reading input parameters).
     *
     * @param netPlan Complete network design (including traffic demands)
     * @param netState Current network state
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.0
     */
    public void initialize(NetPlan netPlan, ConnectionNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);

    /**
     * Processes a new event.
     *
     * @param netPlan Complete network design (including traffic demands)
     * @param netState Current network state
     * @param event A CAC event (connection request, connection release)
     * @return A list of actions (accept/block request -only under request events-, modify existing connection, or release an existing connection
     * @since 0.2.0
     */
    public List<CACAction> processEvent(NetPlan netPlan, ConnectionNetState netState, CACEvent event);

    /**
     * Returns an algorithm-specific report.
     *
     * @param output Container for the report
     * @param simTime Current simulation time
     * @return Report title (return <code>null</code> to omit it)
     * @since 0.2.0
     */
    public String finish(StringBuilder output, double simTime);

    /**
     * Performs some transitory-finished action.
     * 
     * @param simTime Current time
     * @since 0.2.3
     */
    public void finishTransitory(double simTime);
}
