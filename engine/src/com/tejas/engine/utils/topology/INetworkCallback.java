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

package com.tejas.engine.utils.topology;

import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Pair;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public interface INetworkCallback extends ITopologyCallback
{
    public Pair<Integer, NetPlan> getCurrentNetPlan();
    public void showDemand(int demandId);
    public void showRoute(int routeId);
    public void showSegment(int segmentId);
    public void showSRG(int srgId);
    public void updateNetPlanView();
}
