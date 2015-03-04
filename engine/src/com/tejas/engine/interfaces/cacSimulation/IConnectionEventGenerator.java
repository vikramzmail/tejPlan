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

import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that a connection event generator can be run in the <code>Connection simulator</code> included within <code>Net2Plan</code>.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface IConnectionEventGenerator extends IExternal
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
     * Initializes the connection generator (i.e. reading input parameters)
     *
     * @param netPlan Complete network design (including traffic demands)
     * @param netState Current network state
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @return A set of events (only connection requests, releases will be automatically generated upon connection establishment)
     * @since 0.2.0
     */
    public List<CACEvent> initialize(NetPlan netPlan, ConnectionNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);

    /**
     * Processes a new event.
     *
     * @param netPlan Complete network design (including traffic demands)
     * @param netState Current network state
     * @param event A CAC event (connection request, connection release)
     * @return A set of events (only connection requests, releases will be automatically generated upon connection establishment)
     * @since 0.2.0
     */
    public List<CACEvent> processEvent(NetPlan netPlan, ConnectionNetState netState, CACEvent event);
}
