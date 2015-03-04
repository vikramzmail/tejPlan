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

import java.util.concurrent.TimeUnit;

/**
 * <p>Class representing a simulation event.</p>
 *
 * <p>Regardless specific event details, every event is defined by the event arrival time and a priority value. The highest priority event is called first.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see Wikipedia, "Discrete event simulation," <i>Wikipedia</i>, <i>The Free Encyclopedia</i>. [Online] <a href='http://en.wikipedia.org/wiki/Discrete_event_simulation'>http://en.wikipedia.org/wiki/Discrete_event_simulation</a>, [Last accessed: March 2, 2013]
 */
public class SimEvent implements Comparable<SimEvent>
{
    public final static int DEFAULT_PRIORITY = 0;

    private final double eventTime;
    private final int priority;
    
    /**
     * Default constructor.
     * 
     * @param eventTime Event time
     * @since 0.2.0
     */
    public SimEvent(double eventTime)
    {
	this(eventTime, DEFAULT_PRIORITY);
    }

    /**
     * Constructor that allows to set a priority to the event.
     * 
     * @param eventTime Event time
     * @param priority Event priority (the higher, the more priority)
     * @since 0.2.0
     */
    private SimEvent(double eventTime, int priority)
    {
	this.eventTime = eventTime;
	this.priority = priority;
    }

    /**
     * Returns the event time.
     * 
     * @return Event time
     * @since 0.2.0
     */
    public final double getEventTime()
    {
	return eventTime;
    }

    /**
     * Returns the event priority.
     * 
     * @return Event priority
     * @since 0.2.0
     */
    public final int getEventPriority()
    {
	return priority;
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
	if (o == null) return false;
	if (o == this) return true;
	if (!(o instanceof SimEvent)) return false;

	SimEvent e = (SimEvent) o;
	if (getEventTime() == e.getEventTime() && getEventPriority() == e.getEventPriority()) return true;

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
        int hash = 7;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.eventTime) ^ (Double.doubleToLongBits(this.eventTime) >>> 32));
        hash = 59 * hash + this.priority;
        return hash;
    }

    @Override
    public final int compareTo(SimEvent e)
    {
        if (equals(e)) return 0;
        
	// Compare event time
	if (getEventTime() < e.getEventTime())
	{
	    return -1;
	}
	else
	{
	    if (getEventTime() > e.getEventTime())
	    {
		return 1;
	    }
	    else
	    {
		// If equal, sort by priority
		if (getEventPriority() < e.getEventPriority())
		{
		    return 1;
		}
		else
		{
		    return getEventPriority() > e.getEventPriority() ? -1 : 0;
		}
	    }
	}
    }
    
    /**
     * Converts a timestamp in seconds into its equivalent representation in days,
     * hours, minutes and seconds.
     * 
     * @param seconds Timestamp
     * @return String representation in days, hours, minutes and seconds
     * @since 0.2.2
     */
    public static String secondsToYearsDaysHoursMinutesSeconds(double seconds)
    {
        if (seconds == Double.MAX_VALUE) return "infinity";

        long aux_seconds = (long) seconds;
        
        long days = TimeUnit.SECONDS.toDays(aux_seconds);
        
        long months = (long) Math.floor(((double) days / 30));
        long years = (long) Math.floor(((double) months / 12));
        
        months %= 12;
        days %= 30;
        
        long hours = TimeUnit.SECONDS.toHours(aux_seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(aux_seconds) % 60;
        seconds = seconds % 60;
        
        StringBuilder out = new StringBuilder();

        if (years > 0) out.append(String.format("%d years", years));

        if (months > 0)
        {
            if (out.length() > 0) out.append(", ");
            out.append(String.format("%d months", months));
        }

        if (days > 0)
        {
            if (out.length() > 0) out.append(", ");
            out.append(String.format("%d days", days));
        }
        
        if (hours > 0)
        {
            if (out.length() > 0) out.append(", ");
            out.append(String.format("%d h", hours));
        }
        
        if (minutes > 0)
        {
            if (out.length() > 0) out.append(", ");
            out.append(String.format("%d m", minutes));
        }

        if (seconds > 0)
        {
            if (out.length() > 0) out.append(", ");
            out.append(String.format("%.3f s", seconds));
        }
        
        if (out.length() == 0) out.append("0 s");

        return out.toString();
    }
}