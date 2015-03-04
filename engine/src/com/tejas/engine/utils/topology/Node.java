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

import java.awt.geom.Point2D;

/**
 * Class representing a node.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Node
{
    private Point2D pos;
    private int id;
    private final String label;
    
    /**
     * Returns the node identifier.
     * 
     * @return Node identifier
     * @since 0.2.0
     */
    public int getId()
    {
        return id;
    }
    
    /**
     * Sets the new node identifier.
     * 
     * @param id New node identifier
     * @since 0.2.0
     */
    public void setId(int id)
    {
        this.id = id;
    }
    
    /**
     * Returns the node label.
     * 
     * @return Node label
     * @since 0.2.0
     */
    public String getLabel()
    {
        return label;
    }
    
    /**
     * Returns the node position.
     * 
     * @return Node position
     * @since 0.2.0
     */
    public Point2D getPosition()
    {
        return pos;
    }

    /**
     * Sets the new node position.
     * 
     * @param pos New node position
     * @since 0.2.0
     */
    public void setPosition(Point2D pos)
    {
	this.pos = pos;
    }

    /**
     * Default constructor.
     * 
     * @param id Node identifier
     * @param pos Node position
     * @since 0.2.0
     */
    public Node(int id, Point2D pos)
    {
        this(id, pos, null);
    }

    /**
     * Constructor that allows to set a node label.
     *
     * @param id Node identifier
     * @param pos Node position
     * @param label Node label (may be null, default: "Node " + node identifier)
     * @since 0.2.0
     */
    public Node(int id, Point2D pos, String label)
    {
        this.id = id;
        this.pos = pos;
        this.label = label == null ? "Node " + id : label;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }
}
