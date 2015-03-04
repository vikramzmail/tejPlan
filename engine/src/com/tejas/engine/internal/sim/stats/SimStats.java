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

package com.tejas.engine.internal.sim.stats;

import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;

import java.util.List;
import java.util.Map;

/**
 * Abstract class defining a template for statistics classes for simulations.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class SimStats
{
    /**
     * Reference to the planned network design.
     * 
     * @since 0.2.3
     */
    protected final NetPlan netPlan;

    /**
     * Reference to the current network state. 
     * 
     * @since 0.2.3
     */
    protected final SimState netState;

    /**
     * Reference to the current simulation parameters. 
     * 
     * @since 0.2.3
     */
    protected final Map<String, String> simulationParameters;

    /**
     * Reference to the current Net2Plan-wide options. 
     * 
     * @since 0.2.3
     */
    protected final Map<String, String> net2planParameters;

    /**
     * Default constructor.
     * 
     * @param netPlan  Reference to the planned network design
     * @param netState Reference to the current network state
     * @param simulationParameters A key-value map with simulation options
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.3
     */
    public SimStats(NetPlan netPlan, SimState netState, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
    {
	this.netPlan = netPlan;
	this.netState = netState;
        this.net2planParameters = net2planParameters;
        this.simulationParameters = simulationParameters;
    }

    /**
     * Computes statistics for the current simulation time.
     *
     * @param event  Event
     * @param actions Corresponding actions
     * @since 0.2.3
     */
    public abstract void computeNextState(SimEvent event, List actions);

    /**
     * Resets the statistics.
     * 
     * @param currentSimTime Current simulation time
     * @since 0.2.3
     */
    public abstract void reset(double currentSimTime);

    /**
     * Returns a HTML <code>String</code> with statistics.
     * 
     * @param currentSimTime Current simulation time
     * @return Statistics in HTML format
     * @since 0.2.3
     */
    public abstract String getResults(double currentSimTime);
}