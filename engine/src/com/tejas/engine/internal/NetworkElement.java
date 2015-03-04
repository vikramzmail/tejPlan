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

package com.tejas.engine.internal;

import com.tejas.engine.utils.StringUtils;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for each network element handling user-defined attributes
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class NetworkElement
{
    private Map<String, String> attributes;

    /**
     * Default constructor.
     * 
     * @since 0.2.3
     */
    public NetworkElement() { attributes = new AttributeMap(); }

    /**
     * Constructor that copies the value set of the input attributes.
     * 
     * @param attributes Attributes to be copied (if null, it will be initialized as empty)
     * @since 0.2.3
     */
    public NetworkElement(Map<String, String> attributes)
    {
        super();
        
        setAttributes(attributes);
    }

    /**
     * Sets the attributes of the element as a copy of the input ones.
     * 
     * @param attributes Map to be copied (if null, it will be initialized as empty)
     * @since 0.2.3
     */
    public final void setAttributes(Map<String, String> attributes)
    {
        this.attributes = new AttributeMap(attributes);
    }

    /**
     * Adds a new attribute. If the attribute already exists, the value will be 
     * overriden.
     * 
     * @param key Attribute name
     * @param value Attribute value (it may be null)
     * @since 0.2.3
     */
    public final void setAttribute(String key, String value) { attributes.put(key, value); }

    /**
     * Returns the value of the given attribute (or <code>null</code>, if 
     * attribute is not defined).
     * 
     * @param key Attribute name
     * @return Attribute value (or <code>null</code>, if attribute is not defined)
     * @since 0.2.3
     */
    public final String getAttribute(String key) { return attributes.get(key); }

    /**
     * Gets a copy of all defined attributes and their corresponding values.
     * 
     * @return Attributes and values
     * @since 0.2.3
     */
    public final Map<String, String> getAttributes() { return new AttributeMap(attributes); }

    /**
     * Removes the given attribute. If it is not defined, nothing happens.
     * 
     * @param key Attribute name
     * @since 0.2.3
     */
    public final void removeAttribute(String key) { attributes.remove(key); }

    /**
     * Extends <code>HashMap</code> to allow only key values that can be saved
     * according to XML rules.
     * 
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     * @since 0.2.3
     */
    private final static class AttributeMap extends HashMap<String, String>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         * 
         * @since 0.2.3
         */
        public AttributeMap() { super(); }

        /**
         * Constructor that copies the value set of the input map.
         * 
         * @param map Map to be copied (if null, it will be initialized as empty)
         * @since 0.2.3
         */
        public AttributeMap(Map<String, String> m)
        {
            super();

            if (m == null) return;

            for(Map.Entry<String, String> entry : m.entrySet())
                put(entry.getKey(), entry.getValue());
        }

        @Override
        public String put(String key, String value)
        {
            StringUtils.checkAttributeName(key);

            return super.put(key, value);
        }
    }
    
    public interface PathElement
    {
	public Node getFirstNode();
	public Node getLastNode();
	public LinkedList<Node> getSequenceOfNodes();
	public LinkedList<Link> getSequenceOfLinks();
	// public List<Link> getSubLinkName();
    }
    
    public static class Network extends NetworkElement
    {
        public String name;
        public String description;
        
        public Network()
        {
            super();
            
            name = "";
            description = "";
        }
    }

    public static class Demand extends NetworkElement
    {
	public Node ingressNode;
	public Node egressNode;
	public double offeredTrafficInErlangs;

	public Demand(Node ingressNode, Node egressNode)
	{
	    super();
	    this.ingressNode = ingressNode;
	    this.egressNode = egressNode;
	    this.offeredTrafficInErlangs = 0;
	}
    }

    public static class Link extends NetworkElement implements PathElement
    {
	public Node originNode;
	public Node destinationNode;
	public double linkCapacityInErlangs;
	public double linkLengthInKm;
	public String sublink ;
     
	
	public Link(Node originNode, Node destinationNode)
	{
	    this.originNode = originNode;
	    this.destinationNode = destinationNode;

	    linkCapacityInErlangs = 0;
	    linkLengthInKm = 0;
	}

	@Override
	public LinkedList<Node> getSequenceOfNodes()
	{
	    LinkedList<Node> sequenceOfNodes = new LinkedList<Node>();
	    sequenceOfNodes.add(originNode);
	    sequenceOfNodes.add(destinationNode);

	    return sequenceOfNodes;
	}

	@Override
	public LinkedList<Link> getSequenceOfLinks()
	{
	    LinkedList<Link> sequenceOfLinks = new LinkedList<Link>();
	    sequenceOfLinks.add(this);

	    return sequenceOfLinks;
	}

	@Override
	public Node getFirstNode() { return originNode; }

	@Override
	public Node getLastNode() { return destinationNode; }
    }

    public static class Node extends NetworkElement
    {
	public Point2D position;
	public String name;

	public Node()
        {
            super();
            
            position = new Point2D.Double();
            name = "";
        }
    }

    public static class Route extends NetworkElement implements PathElement
    {
	public Demand demand;
	public List<Link> plannedRoute;
	public double carriedTrafficInErlangs;
	public List<Segment> backupSegments;

	public Route(Demand demand, List<Link> route, double carriedTrafficInErlangs)
	{
	    super();

	    this.demand = demand;
	    this.plannedRoute = route;
	    this.carriedTrafficInErlangs = carriedTrafficInErlangs;
	    backupSegments = new LinkedList<Segment>();
	}

	@Override
	public Node getFirstNode() { return plannedRoute.get(0).originNode; }

	@Override
	public Node getLastNode() { return plannedRoute.get(plannedRoute.size() - 1).destinationNode; }

	@Override
	public LinkedList<Link> getSequenceOfLinks()
	{
	    LinkedList<Link> sequenceOfLinks = new LinkedList<Link>();
	    Iterator<? extends PathElement> it = plannedRoute.iterator();
	    while (it.hasNext()) sequenceOfLinks.addAll(it.next().getSequenceOfLinks());

	    return sequenceOfLinks;
	}

	@Override
	public LinkedList<Node> getSequenceOfNodes()
	{
	    LinkedList<Node> sequenceOfNodes = new LinkedList<Node>();
	    Iterator<? extends PathElement> it = plannedRoute.iterator();
	    int ct = 0;
	    while (it.hasNext())
	    {
		if (ct > 0)
		{
		    sequenceOfNodes.removeLast();
		}
		sequenceOfNodes.addAll(it.next().getSequenceOfNodes());
		ct++;
	    }

	    return sequenceOfNodes;
	}
    }

    public static class Segment extends NetworkElement implements PathElement
    {
	public List<Link> route;
	public double reservedBandwithInErlangs;

	public Segment(List<Link> route, double reservedBandwithInErlangs)
	{
	    this.route = route;
	    this.reservedBandwithInErlangs = reservedBandwithInErlangs;
	}

	@Override
	public LinkedList<Node> getSequenceOfNodes()
	{
	    LinkedList<Node> sequenceOfNodes = new LinkedList<Node>();

	    Iterator<Link> it = route.iterator();
	    int ct = 0;
	    while (it.hasNext())
	    {
		if (ct > 0) sequenceOfNodes.removeLast();
		sequenceOfNodes.addAll(it.next().getSequenceOfNodes());
		ct++;
	    }

	    return sequenceOfNodes;
	}

	@Override
	public LinkedList<Link> getSequenceOfLinks()
	{
	    LinkedList<Link> sequenceOfLinks = new LinkedList<Link>();

	    Iterator<Link> it = route.iterator();
	    while (it.hasNext()) sequenceOfLinks.add(it.next());

	    return sequenceOfLinks;
	}

	@Override
	public Node getFirstNode() { return route.get(0).originNode; }

	@Override
	public Node getLastNode() { return route.get(route.size() - 1).destinationNode; }
    }
    
    public static class SRG extends NetworkElement
    {
        public double mttf;
        public double mttr;
        public Set<Node> nodes;
        public Set<Link> links;
    }
}