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

package com.tejas.engine.internal.sim;

import java.util.List;

import com.tejas.engine.internal.sim.IGUISimulationListener;
import com.tejas.engine.internal.sim.SimEvent;

/**
 * Contract that must be fulfilled by classes (i.e. simulation module) which calls the simulation core.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface IEventCallback extends IGUISimulationListener
{
    /**
     * Indicates the transitory period is finished.
     * 
     * @param currentSimTime Current simulation time
     * @since 0.2.0
     */
    public void endOfTransitory(double currentSimTime);

    /**
     * Processes a single event and returns a list of new events of the event
     * generator to be scheduled.
     * 
     * @param event Simulation event
     * @return A list of new events to schedule
     * @since 0.2.0
     */
    public List<SimEvent> processEvent(SimEvent event);
}
