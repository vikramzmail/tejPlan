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

import com.tejas.engine.internal.sim.SimEvent;

import java.util.Objects;

/**
 * <p>Provides a set of events to be returned by resilience event generators. These events are:</p>
 *
 * <ul>
 * <li>SRG/Node/Link failure: The corresponding element is going down</li>
 * <li>SRG/Node/Link reparation: The corresponding element is going up</li>
 * </ul>
 * 
 * <p><b>Important</b>: Generators only can generate events of type 'SRG', while
 * provisioning algorithms receive single 'node' or 'link' events, corresponding
 * to the network elements within the SRG.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ResilienceEvent extends SimEvent
{
    /**
     * Type of event.
     *
     * @since 0.2.0
     */
    public enum EventType
    {
	/**
	 * SRG failure.
	 *
         * @since 0.2.0
	 */
	SRG_FAILURE,

	/**
	 * SRG reparation.
	 *
         * @since 0.2.0
	 */
	SRG_REPARATION,

	/**
	 * Node failure.
	 *
         * @since 0.2.0
	 */
	NODE_FAILURE,

	/**
	 * Node reparation.
	 *
         * @since 0.2.0
	 */
	NODE_REPARATION,

	/**
	 * Link failure.
	 *
         * @since 0.2.0
	 */
	LINK_FAILURE,

	/**
	 * Link reparation.
	 *
         * @since 0.2.0
	 */
	LINK_REPARATION
    };

    private int id;
    private EventType type;

    private ResilienceEvent(double eventTime) { super(eventTime); }

    /**
     * <p>Constructor to define failure/reparation events.</p>
     *
     * <p><b>Important</b>: Users only can generate SRG events in the event generator,
     * while receive node or link ones in the provisioning algorithm
     *
     * @param eventTime Event time
     * @param id Element identifier (SRG, node or link)
     * @param type Event type
     * @since 0.2.0
     */
    public ResilienceEvent(double eventTime, int id, EventType type)
    {
	super(eventTime);

	this.id = id;
	this.type = type;
    }

    /**
     * Returns the identifier of the element referenced by the event (SRG, node or link).
     *
     * @return Identifier
     * @since 0.2.0
     */
    public int getId() { return id; }

    /**
     * Returns the event type.
     *
     * @return Event type
     * @since 0.2.0
     */
    public EventType getEventType() { return type; }

    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.0
     */
    @Override
    public String toString()
    {
	switch(getEventType())
	{
	    case LINK_FAILURE:
		return String.format("Link %d failed at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    case LINK_REPARATION:
		return String.format("Link %d repaired at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    case NODE_FAILURE:
		return String.format("Node %d failed at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    case NODE_REPARATION:
		return String.format("Node %d repaired at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    case SRG_FAILURE:
		return String.format("SRG %d failed at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    case SRG_REPARATION:
		return String.format("SRG %d repaired at %s", getId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));

	    default:
		throw new RuntimeException("Bad - Unknown event type");
	}
    }
    
    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o Reference object with which to compare
     * @return <code>true</code> if this object is the same as the <code>o</code> argument; <code>false</code> otherwise
     * @since 0.2.0
     */
    @Override
    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof ResilienceEvent)) return false;
            
            ResilienceEvent e = (ResilienceEvent) o;
            
            if (getEventType() == e.getEventType() && getId() == e.getId())
                return true;
        }

	return false;
    }

    /**
     * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by <code>HashMap</code>.
     *
     * @return Hash code value for this object
     * @since 0.2.0
     */
    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 89 * hash + this.id;
        hash = 89 * hash + Objects.hashCode(this.type);
        return hash;
    }
}
