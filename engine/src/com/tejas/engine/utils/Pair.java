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

import java.util.Objects;

/**
 * <p>A tuple consisting of two elements. There is no restriction on the type of the objects that may be stored.</p>
 *
 * <p>Example:</p>
 * <code>Map&lt;String, Object&gt; map = new HashMap&lt;&gt;();</code>
 * <br/>
 * <code>map.put("parameter", "value");</code>
 * <br/>
 * <code>Integer number = new Integer(3);</code>
 * <br/>
 * <br/>
 * <code>Triple&lt;Map&lt;String, Object&gt;, Integer&gt; data = new Triple&lt;&gt;(map, number);</code>
 * <br/>
 * <br/>
 * ...
 * <br/>
 * <br/>
 * <code>Map&lt;String, Object&gt; myMap = data.getFirst();</code>
 * <br/>
 * <code>Integer myNumber = data.getSecond();</code>
 *
 * @param <A> Class type for the first element
 * @param <B> Class type for the second element
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Pair<A, B>
{
    private A a;
    private B b;

    /**
     * Default constructor.
     *
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @since 0.2.0
     */
    public Pair(A a, B b)
    {
	this.a = a;
	this.b = b;
    }

    /**
     * Returns the first element from this pair.
     *
     * @return The first element from this pair
     * @since 0.2.0
     */
    public A getFirst() { return a; }

    /**
     * Returns the second element from this pair.
     *
     * @return The second element from this pair
     * @since 0.2.0
     */
    public B getSecond() { return b; }

    /**
     * This factory allows the pair to be created using inference to obtain the generic types.
     *
     * @param <A> Class type for the first element
     * @param <B> Class type for the second element
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @return A pair formed from two parameters
     * @since 0.2.0
     */
    public static<A, B> Pair<A, B> of(A a, B b)
    {
	return new Pair<A, B>(a, b);
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
	if (!(o instanceof Pair)) return false;

	Pair p = (Pair) o;
	if (getFirst().equals(p.getFirst()) && getSecond().equals(p.getSecond())) return true;

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
	hash = 17 * hash + Objects.hashCode(getFirst());
	hash = 17 * hash + Objects.hashCode(getSecond());
	return hash;
    }

    /**
     * Sets the first element from this pair.
     *
     * @param a The first element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setFirst(A a)
    {
	this.a = a;
    }

    /**
     * Sets the second element from this pair.
     *
     * @param b The second element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setSecond(B b)
    {
	this.b = b;
    }

    /**
     * Returns a <code>String</code> representation of this pair using the format (first, second).
     *
     * @return A <code>String</code> representation of this pair
     * @since 0.2.0
     */
    @Override
    public String toString()
    {
        return "(" + getFirst() + ", " + getSecond() + ")";
    }
}