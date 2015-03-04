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

import com.tejas.engine.interfaces.networkDesign.NetPlan;

import java.awt.geom.Point2D;
import java.util.List;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface ITopologyCallback
{
    /**
     *
     * @param pos
     */
    public void addNode(Point2D pos);
    
    /**
     *
     * @param netPlan
     * @since 0.2.3
     */
    public void loadDesign(NetPlan netPlan);
    
    public List<NetPlan> getDesign();
    
    /**
     *
     * @param demands
     * @since 0.2.3
     */
    public void loadTrafficDemands(NetPlan demands);

    /**
     *
     * @param nodeId
     * @param pos
     */
    public void moveNode(int nodeId, Point2D pos);

    /**
     *
     * @param nodeId
     */
    public void removeNode(int nodeId);
    /**
     *
     * @param linkId
     */
    public void removeLink(int linkId);
    /**
     *
     * @param originNodeId
     * @param destinationNodeId
     */
    public void addLink(int originNodeId, int destinationNodeId);
    /**
     *
     */
    public void resetView();
    /**
     *
     * @param nodeId
     */
    public void showNode(int nodeId);
    /**
     *
     * @param linkId
     */
    public void showLink(int linkId);
    
    public void reset();
}
