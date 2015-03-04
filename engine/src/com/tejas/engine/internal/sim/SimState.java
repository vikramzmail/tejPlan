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

import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.NetState;

import java.util.List;

/**
 * Template for network state objects of simulators.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class SimState extends NetState
{
    /**
     * Reference to the initial network design.
     * 
     * @since 0.2.3
     */
    protected NetPlan netPlan;
    
    /**
     * Default constructor.
     * 
     * @param netPlan Reference to the initial network design
     * @since 0.2.3
     */
    public SimState(NetPlan netPlan) { this.netPlan = netPlan; }
    
    /**
     * Returns a network design from the current network state.
     * 
     * @return Network design
     * @since 0.2.3
     */
    public abstract NetPlan convertToNetPlan();
    
    /**
     * Update the network state.
     *
     * @param event Current simulation event
     * @param actions List of actions to perform
     * @return Object
     * @since 0.2.3
     */
    public abstract Object update(SimEvent event, List actions);
}
