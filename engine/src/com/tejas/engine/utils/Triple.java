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
 * <p>A tuple consisting of three elements. There is no restriction on the type of the objects that may be stored.</p>
 *
 * <p>Example:</p>
 * <code>Map&lt;String, Object&gt; map = new HashMap&lt;&gt;();</code>
 * <br/>
 * <code>map.put("parameter", "value");</code>
 * <br/>
 * <code>Integer number = new Integer(3);</code>
 * <br/>
 * <code>String string = "Test";</code>
 * <br/>
 * <br/>
 * <code>Triple&lt;Map&lt;String, Object&gt;, Integer, String&gt; data = new Triple&lt;&gt;(map, number, string);</code>
 * <br/>
 * <br/>
 * ...
 * <br/>
 * <br/>
 * <code>Map&lt;String, Object&gt; myMap = data.getFirst();</code>
 * <br/>
 * <code>Integer myNumber = data.getSecond();</code>
 * <br/>
 * <code>String myString = data.getThird();</code>
 *
 * @param <A> Class type for the first element
 * @param <B> Class type for the second element
 * @param <C> Class type for the third element
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Triple<A, B, C>
{
    private A a;
    private B b;
    private C c;

    /**
     * Default constructor.
     *
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @param c The third element, may be <code>null</code>
     * @since 0.2.0
     */
    public Triple(A a, B b, C c)
    {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * Returns the first element from this triple.
     *
     * @return The first element from this triple
     * @since 0.2.0
     */
    public A getFirst() { return a; }

    /**
     * Returns the second element from this triple.
     *
     * @return The second element from this triple
     * @since 0.2.0
     */
    public B getSecond() { return b; }

    /**
     * Returns the third element from this triple.
     *
     * @return The third element from this triple
     * @since 0.2.0
     */
    public C getThird() { return c; }

    /**
     * This factory allows the triple to be created using inference to obtain the generic types.
     *
     * @param <A> Class type for the first element
     * @param <B> Class type for the second element
     * @param <C> Class type for the third element
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @param c The third element, may be <code>null</code>
     * @return A triple formed from three parameters
     * @since 0.2.0
     */
    public static<A, B, C> Triple<A, B, C> of(A a, B b, C c)
    {
	return new Triple<A, B, C>(a, b, c);
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
	int hash = 3;
	hash = 59 * hash + Objects.hashCode(getFirst());
	hash = 59 * hash + Objects.hashCode(getSecond());
	hash = 59 * hash + Objects.hashCode(getThird());
	return hash;
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
	if (!(o instanceof Triple)) return false;

	Triple p = (Triple) o;
	if (getFirst().equals(p.getFirst()) && getSecond().equals(p.getSecond()) && getThird().equals(p.getThird())) return true;

	return false;
    }

    /**
     * Sets the first element from this triple.
     *
     * @param a The first element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setFirst(A a)
    {
	this.a = a;
    }

    /**
     * Sets the second element from this triple.
     *
     * @param b The second element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setSecond(B b)
    {
	this.b = b;
    }

    /**
     * Sets the third element from this triple.
     *
     * @param c The third element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setThird(C c)
    {
	this.c = c;
    }

    /**
     * Returns a <code>String</code> representation of this triple using the format (first, second, third).
     *
     * @return A <code>String</code> representation of this triple
     * @since 0.2.0
     */
    @Override
    public String toString()
    {
        return "(" + getFirst() + ", " + getSecond() + ", " + getThird() + ")";
    }
}