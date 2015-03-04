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

import java.util.Map;


/**
 * Template for network state representation (both for design and simulation).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class NetState
{
    protected boolean isModifiable;
    protected final static String UNMODIFIABLE_EXCEPTION_STRING = "Unmodifiable NetState object - can't be changed";
    
    /**
     * Checks the current network state.
     *
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @param allowLinkOversubscription <code>true</code> if link capacity constraint may be violated. Otherwise, <code>false</code>
     * @param allowExcessCarriedTraffic <code>true</code> if carried traffic may be greater than the offered one for some demand. Otherwise, <code>false</code>
     * @since 0.2.3
     */
    public abstract void checkValidity(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic);
    
    /**
     * Resets the state of the network.
     * 
     * @since 0.2.3
     */
    public abstract void reset();

    /**
     * Returns a deep copy of the current state.
     * 
     * @return Deep copy of the current state
     * @since 0.2.3
     */
    public abstract NetState copy();
    
    /**
     * Returns an unmodifiable view of the network.
     *
     * @return An unmodifiable view of the network
     * @since 0.2.3
     */
    public abstract NetState unmodifiableView();
    
    protected final void checkIsModifiable() { if (!isModifiable) throw new UnsupportedOperationException(UNMODIFIABLE_EXCEPTION_STRING); }
}
