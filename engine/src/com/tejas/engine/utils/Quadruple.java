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
 * <p>A tuple consisting of four elements. There is no restriction on the type of the objects that may be stored.</p>
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
 * <code>List list = new ArrayList();</code>
 * <br/>
 * <br/>
 * <code>Quadruple&lt;Map&lt;String, Object&gt;, Integer, String, List&gt; data = new Quadruple&lt;&gt;(map, number, string, list);</code>
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
 * <br/>
 * <code>List myList = data.getFourth();</code>
 *
 * @param <A> Class type for the first element
 * @param <B> Class type for the second element
 * @param <C> Class type for the third element
 * @param <D> Class type for the fourth element
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Quadruple<A, B, C, D>
{
    private A a;
    private B b;
    private C c;
    private D d;

    /**
     * Default constructor.
     *
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @param c The third element, may be <code>null</code>
     * @param d The fourth element, may be <code>null</code>
     * @since 1.0
     */
    public Quadruple(A a, B b, C c, D d)
    {
	this.a = a;
	this.b = b;
	this.c = c;
	this.d = d;
    }

    /**
     * Returns the first element from this quadruple.
     *
     * @return The first element from this quadruple
     * @since 0.2.0
     */
    public A getFirst() { return a; }

    /**
     * Returns the second element from this quadruple.
     *
     * @return The second element from this quadruple
     * @since 0.2.0
     */
    public B getSecond() { return b; }

    /**
     * Returns the third element from this quadruple.
     *
     * @return The third element from this quadruple
     * @since 0.2.0
     */
    public C getThird() { return c; }

    /**
     * Returns the fourth element from this quadruple.
     *
     * @return The fourth element from this quadruple
     * @since 0.2.0
     */
    public D getFourth() { return d; }

    /**
     * This factory allows the quadruple to be created using inference to obtain the generic types.
     *
     * @param <A> Class type for the first element
     * @param <B> Class type for the second element
     * @param <C> Class type for the third element
     * @param <D> Class type for the fourth element
     * @param a The first element, may be <code>null</code>
     * @param b The second element, may be <code>null</code>
     * @param c The third element, may be <code>null</code>
     * @param d The fourth element, may be <code>null</code>
     * @return A quadruple formed from four parameters
     * @since 0.2.0
     */
    public static <A, B, C, D> Quadruple<A, B, C, D> of(A a, B b, C c, D d)
    {
	return new Quadruple<A, B, C, D>(a, b, c, d);
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
	if (!(o instanceof Quadruple)) return false;

	Quadruple p = (Quadruple) o;
	if (getFirst().equals(p.getFirst()) && getSecond().equals(p.getSecond()) && getThird().equals(p.getThird()) && getFourth().equals(p.getFourth())) return true;

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
	int hash = 3;
	hash = 97 * hash + Objects.hashCode(getFirst());
	hash = 97 * hash + Objects.hashCode(getSecond());
	hash = 97 * hash + Objects.hashCode(getThird());
	hash = 97 * hash + Objects.hashCode(getFourth());
	return hash;
    }

    /**
     * Sets the first element from this quadruple.
     *
     * @param a The first element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setFirst(A a)
    {
	this.a = a;
    }

    /**
     * Sets the second element from this quadruple.
     *
     * @param b The second element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setSecond(B b)
    {
	this.b = b;
    }

    /**
     * Sets the third element from this quadruple.
     *
     * @param c The third element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setThird(C c)
    {
	this.c = c;
    }

    /**
     * Sets the fourth element from this quadruple.
     *
     * @param d The fourth element, may be <code>null</code>
     * @since 0.2.0
     */
    public void setFourth(D d)
    {
	this.d = d;
    }

    /**
     * Returns a <code>String</code> representation of this quadruple using the format (first, second, third, fourth).
     *
     * @return A <code>String</code> representation of this quadruple
     * @since 0.2.0
     */
    @Override
    public String toString()
    {
	return "(" + getFirst() + ", " + getSecond() + ", " + getThird() + ", " + getFourth() + ")";
    }
}