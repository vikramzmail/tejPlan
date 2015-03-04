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
 * <p>Class to wrap an object with a <code>toString()</code> method independent of
 * the object's internal <code>toString()</code>. It is useful for <code>JComboBox</code>.</p>
 * 
 * <p>Example:</p>
 * <p>
 * <code>Integer number = new Integer(3);</code>
 * <br />
 * <code>StringLabeller labelledNumber = StringLabeller.of(number, "label");</code>
 * <br />
 * <code>System.out.println(number);</code> (returns 3);
 * <br />
 * <code>System.out.println(labelledNumber);</code> (returns "label");
 * <br />
 * </p>
 * 
 * <p><b<Important</b>: The object is not cloned or copied, so changes in the original
 * object are reflected here.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class StringLabeller
{
    private Object object;
    private String label;
    
    /**
     * Default constructor.
     * 
     * @param object Object to be wrapped
     * @param label New label for the wrapped object
     * @since 0.2.2
     */
    public StringLabeller(Object object, String label)
    {
        this.object = object;
        this.label = label;
    }
    
    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.2
     */
    @Override
    public String toString() { return label; }
    
    /**
     * Returns the original object.
     * 
     * @return The original object
     * @since 0.2.2
     */
    public Object getObject() { return object; }
    
    /**
     * Factory method.
     * 
     * @param object Object to be wrapped
     * @param label New label for the wrapped object
     * @return A new <code>StringLabeller</code> object
     * @since 0.2.2
     */
    public static StringLabeller of(Object object, String label) { return new StringLabeller(object, label); }
}
