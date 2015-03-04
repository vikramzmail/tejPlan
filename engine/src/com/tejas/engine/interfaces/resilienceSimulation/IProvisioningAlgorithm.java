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

package com.tejas.engine.interfaces.resilienceSimulation;

import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that a provisioning algorithm can be run in <code>Net2Plan</code>.</p>
 *
 * <p>The key difference with {@link com.net2plan.interfaces.IAlgorithm algorithms} is that provisioning algorithms are used to react to failure/reparation events. In contrast, {@link com.net2plan.interfaces.IAlgorithm algorithms} are used just for network planning.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see com.net2plan.interfaces.IAlgorithm
 */
public interface IProvisioningAlgorithm extends IExternal
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
     * Executes the provisioning algorithm.
     *
     * @param netPlan Network plan
     * @param netState Current network state
     * @param event Resilience event: node/link/SRG failure, node/link/SRG reparation
     * @return List of actions to perform
     * @since 0.2.0
     */
    public List<ProvisioningAction> processEvent(NetPlan netPlan, ResilienceNetState netState, ResilienceEvent event);

    /**
     * Initializes the provisioning algorithm (i.e. reading input parameters).
     *
     * @param netPlan Network plan
     * @param netState Current network state
     * @param algorithmParameters A key-value map with specific algorithm parameters.<br /><br /><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.0
     */
    public void initialize(NetPlan netPlan, ResilienceNetState netState, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);

    /**
     * Returns an algorithm-specific report.
     *
     * @param output Container for the report
     * @param simTime Current simulation time
     * @return Report title (return <code>null</code>, or an empty <code>output</code> to omit it)
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
