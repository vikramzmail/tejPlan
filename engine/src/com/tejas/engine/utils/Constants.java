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

package com.tejas.engine.utils;

/**
 * <p>Auxiliary class with several application-wide constants.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Constants
{
    /**
     * Constants for choosing the ordering type.
     * 
     * @since 0.2.0
     */
    public static enum OrderingType
    {
        /**
         * Ascending order.
         * 
         * @since 0.2.0
         */
        ASCENDING,
        /**
         * Descending order.
         * 
         * @since 0.2.0
         */
        DESCENDING
    };

    /**
     * Constants for searching methods. Even when only a single item is searched
     * for (i.e. the first one matching a certain condition), methods will return
     * it in an array-fashion, so that it should be accessed by out[0].
     * 
     * @since 0.2.0
     */
    public static enum SearchType
    {
        /**
         * Returns all elements matching the condition(s).
         * 
         * @since 0.2.0
         */
        ALL,

        /**
         * Returns the first element matching the condition(s).
         * 
         * @since 0.2.0
         */
        FIRST,

        /**
         * Returns the last element matching the condition(s).
         *
         * @since 0.2.0
         */
        LAST
    };
    
    /**
     * Constants for shortest path methods.
     * 
     * @since 0.2.3
     */
    public static enum ShortestPathType
    {
        /**
         * Shortest path measured in hops (each link has a weight equal to 1).
         * 
         * @since 0.2.3
         */
        HOPS,

        /**
         * Shortest path measured in km (each link has a weight equal to its length in km).
         * 
         * @since 0.2.3
         */
        KM
    };
}
