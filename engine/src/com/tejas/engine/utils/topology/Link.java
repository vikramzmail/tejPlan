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

import com.tejas.engine.utils.topology.Node;

/**
 * Class representing a link.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Link
{
    private final Node originNode;
    private final Node destinationNode;
    private int id;
    private String label;
    
    /**
     * Sets the new link identifier.
     * 
     * @param id New link identifier
     * @since 0.2.0
     */
    public void setId(int id)
    {
        this.id = id;
    }
    
    /**
     * Returns the origin node of the link.
     * 
     * @return Origin node
     * @since 0.2.0
     */
    public Node getOriginNode()
    {
        return originNode;
    }
    
    /**
     * Returns the destination node of the link.
     * 
     * @return Destination node
     * @since 0.2.0
     */
    public Node getDestinationNode()
    {
        return destinationNode;
    }

    /**
     * Default constructor.
     * 
     * @param id Link identifier
     * @param originNode Origin node identifier
     * @param destinationNode Destination node identifier
     * @since 0.2.0
     */
    public Link(int id, Node originNode, Node destinationNode)
    {
	this(id, originNode, destinationNode, null);
    }

    /**
     * Constructor that allows to set a link label.
     * 
     * @param id Link identifier
     * @param originNode Origin node identifier
     * @param destinationNode Destination node identifier
     * @param label Link label (may be null, default: "e" + link identifier)
     * @since 0.2.0
     */
    public Link(int id, Node originNode, Node destinationNode, String label)
    {
	this.id = id;
	this.originNode = originNode;
	this.destinationNode = destinationNode;
        
	this.label = label == null ? "e" + id : label;
    }

    /**
     * Returns the link identifier.
     * 
     * @return Link identifier
     * @since 0.2.0
     */
    public int getId()
    {
	return id;
    }

    /**
     * Returns the link label.
     * 
     * @return Link label
     * @since 0.2.0
     */
    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
	return getLabel();
    }
}
