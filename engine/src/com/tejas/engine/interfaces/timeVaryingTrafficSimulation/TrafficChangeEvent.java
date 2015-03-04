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

package com.tejas.engine.interfaces.timeVaryingTrafficSimulation;

import com.tejas.engine.internal.sim.SimEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

/**
 * Represents a traffic change event.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.1
 */
public class TrafficChangeEvent extends SimEvent
{
    private final Calendar date;
    private final static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Default constructor.
     * 
     * @param eventTime Event time
     * @param date Calendar day for the event
     * @since 0.2.1
     */
    public TrafficChangeEvent(double eventTime, Calendar date)
    {
        super(eventTime);
        this.date = (Calendar) date.clone();
    }

    /**
     * Returns the calendar date for the event.
     * 
     * @return Current calendar date
     * @since 0.2.1
     */
    public Calendar getDate()
    {
	return date;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o Reference object with which to compare
     * @return <code>true</code> if this object is the same as the <code>o</code> argument; <code>false</code> otherwise
     * @since 0.2.1
     */
    @Override
    public boolean equals(Object o)
    {
        if (super.equals(o))
        {
            if (!(o instanceof TrafficChangeEvent)) return false;
            
            TrafficChangeEvent e = (TrafficChangeEvent) o;
            
            if (getDate().equals(e.getDate()))
                return true;
        }

	return false;
    }

    /**
     * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by <code>HashMap</code>.
     *
     * @return Hash code value for this object
     * @since 0.2.1
     */
    @Override
    public int hashCode()
    {
        int hash = super.hashCode();
        hash = 71 * hash + Objects.hashCode(this.date);
        return hash;
    }

    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.1
     */
    @Override
    public String toString()
    {
        return "Traffic update on " + dateFormat.format(date.getTime()) + " at t=" + SimEvent.secondsToYearsDaysHoursMinutesSeconds(getEventTime());
    }
}
