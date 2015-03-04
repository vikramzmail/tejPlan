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

package com.tejas.engine.interfaces.cacSimulation;

import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Provides a set of events to be used in the CAC simulator. These events are:</p>
 *
 * <ul>
 * <li>Connection request: A new connection request arrives to the system</li>
 * <li>Connection release: The holding time for a connection have just finished</li>
 * </ul>
 *
 * <p>Although the <code>CACEvent</code> class is common for all events, its meaning (i.e.
 * action type) depends on the constructor used to get an instance. Take a look
 * on the description of the constructors to obtain more information.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class CACEvent extends SimEvent
{
    /**
     * Type of event.
     *
     * @since 0.2.0
     */
    public enum EventType
    {
	/**
	 * Connection request.
	 *
	 * @since 0.2.0
	 */
	CONNECTION_REQUEST,

	/**
	 * Connection release.
	 *
	 * @since 0.2.0
	 */
	CONNECTION_RELEASE;
    };

    private EventType type;

    private long connectionRelease_connId;
    private int connectionRequest_demandId;
    private double connectionRequest_durationInSeconds;
    private double connectionRequest_trafficVolumeInErlangs;
    private Map<String, String> connectionRequest_attributes;
    
    /**
     * <p>Constructor to define a 'connection request' event. </p>
     *
     * @param eventTime Arrival time of the request
     * @param demandId Demand identifier
     * @param durationInSeconds Duration (in Erlangs)
     * @param trafficVolumeInErlangs Traffic volume (in Erlangs)
     * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
     * @since 0.2.0
     */
    public CACEvent(double eventTime, int demandId, double durationInSeconds, double trafficVolumeInErlangs, Map<String, String> attributes)
    {
	super(eventTime);

	reset();
        
	type = EventType.CONNECTION_REQUEST;
	connectionRequest_demandId = demandId;
	connectionRequest_durationInSeconds = durationInSeconds;
	connectionRequest_trafficVolumeInErlangs = trafficVolumeInErlangs;
	connectionRequest_attributes = attributes == null ? new HashMap<String, String>() : new HashMap<String, String>(attributes);
    }

    /**
     * <p>Constructor to define a 'connection release' event. </p>
     *
     * @param eventTime Event time
     * @param connIdToRelease Connection identifier
     * @since 0.2.0
     */
    public CACEvent(double eventTime, long connIdToRelease)
    {
	super(eventTime);

	reset();
	type = EventType.CONNECTION_RELEASE;
	connectionRelease_connId = connIdToRelease;
    }

    private void reset()
    {
	connectionRelease_connId = -1;
	connectionRequest_demandId = -1;
	connectionRequest_durationInSeconds = -1;
	connectionRequest_trafficVolumeInErlangs = -1;
	connectionRequest_attributes = new HashMap<String, String>();
    }

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
	    case CONNECTION_REQUEST:
		return String.format("Connection request at %s (demand id = %d, duration = %s, traffic volume = %f E, attributes = %s)", SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()), getRequestDemandId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getRequestDurationInSeconds()), getRequestTrafficVolumeInErlangs(), StringUtils.mapToString(connectionRequest_attributes, "=", ", "));

	    case CONNECTION_RELEASE:
		return String.format("Connection %d release at %s", getReleaseConnectionId(), SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime()));
                
	    default:
		throw new RuntimeException("Bad - Unknown event type");
	}
    }

    /**
     * Returns the demand identifier of the requested connection.
     *
     * @return Demand identifier (or -1, if no 'request' event)
     * @since 0.2.0
     */
    public int getRequestDemandId() { return connectionRequest_demandId; }

    /**
     * Returns the duration of the requested connection.
     *
     * @return Duration (or -1, if no 'request' event)
     * @since 0.2.0
     */
    public double getRequestDurationInSeconds() { return connectionRequest_durationInSeconds; }

    /**
     * Returns the traffic volume of the requested connection.
     *
     * @return Traffic volume (or -1, if no 'request' event)
     * @since 0.2.0
     */
    public double getRequestTrafficVolumeInErlangs() { return connectionRequest_trafficVolumeInErlangs; }

    /**
     * Returns the attributes of the requested connection.
     *
     * @return Attributes
     * @since 0.2.3
     */
    public Map<String, String> getRequestAttributes() { return new HashMap<String, String>(connectionRequest_attributes); }

    /**
     * Returns the connection identifier of the connection to be released.
     *
     * @return Connection identifier (or -1, if no 'release' event)
     * @since 0.2.0
     */
    public long getReleaseConnectionId() { return connectionRelease_connId; }
    
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
            if (!(o instanceof CACEvent)) return false;
            
            CACEvent e = (CACEvent) o;
            
            if (getEventType() == e.getEventType() &&
                    getReleaseConnectionId() == e.getReleaseConnectionId() &&
                    getRequestAttributes().equals(e.getRequestAttributes()) &&
                    getRequestDemandId() == e.getRequestDemandId() &&
                    getRequestDurationInSeconds() == e.getRequestDurationInSeconds() &&
                    getRequestTrafficVolumeInErlangs() == e.getRequestTrafficVolumeInErlangs())
            {
                System.out.println("Existing connection: " + this.toString());
                System.out.println("New connection: " + e.toString());
//                
//                System.out.println(getReleaseConnectionId() + " - " + e.getReleaseConnectionId());
                return true;
            }
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
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + (int) this.connectionRelease_connId;
        hash = 97 * hash + this.connectionRequest_demandId;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.connectionRequest_durationInSeconds) ^ (Double.doubleToLongBits(this.connectionRequest_durationInSeconds) >>> 32));
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.connectionRequest_trafficVolumeInErlangs) ^ (Double.doubleToLongBits(this.connectionRequest_trafficVolumeInErlangs) >>> 32));
        hash = 97 * hash + Objects.hashCode(this.connectionRequest_attributes);
        return hash;
    }
}
