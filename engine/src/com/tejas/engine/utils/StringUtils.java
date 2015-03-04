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

import com.sun.org.apache.xerces.internal.util.XMLChar;
import java.util.Map.Entry;
import java.util.*;

/**
 * <p>Provides extra functionality for String objects.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class StringUtils
{
    private final static String lineSeparator;
    
    static
    {
        lineSeparator = String.format("%n");
    }
    
    /**
     * Returns the line separator. Each operating system uses a different one (e.g. \r\n on Microsoft Windows, \n for Unix systems...).
     *
     * @return The line separator
     * @since 0.2.0
     */
    public static String getLineSeparator()
    {
        return lineSeparator;
    }

    /**
     * Returns a set of selected elements from an input array. It is not backed
     * in the input array, thus changes in this array are not reflected in the
     * input array.
     *
     * @param array Input array
     * @param indexes Position of elements to be selected.
     * @return Set of elements in the order given by <code>indexes</code>
     * @since 0.2.3
     */
    public static String[] select(String[] array, int[] indexes)
    {
	String[] out = new String[indexes.length];

	for(int i = 0; i < indexes.length; i++)
	    out[i] = array[indexes[i]];

	return out;
    }

    /**
     * Joins elements from a given array into a String.
     *
     * @param array Input array
     * @param separator Entry separator
     * @return A String
     * @since 0.2.0
     */
    public static String join(String[] array, String separator)
    {
        if (array.length == 0) throw new NoSuchElementException("Empty array");

        StringBuilder out = new StringBuilder();
        out.append(array[0]);

        for(int i = 1; i < array.length; i++)
            out.append(separator).append(array[i]);

        return out.toString();
    }

    /**
     * Outputs entries from a <code>Map</code> to a <code>String</code>
     *
     * @param map Input map
     * @param keyValueSeparator Separator between keys and values
     * @param entrySeparator Separator between key-value pairs
     * @return <code>String</code> representing the map
     * @since 0.2.0
     */
    public static String mapToString(Map map, String keyValueSeparator, String entrySeparator)
    {
	StringBuilder string = new StringBuilder();

	Iterator it = map.entrySet().iterator();
	while(it.hasNext())
	{
	    Entry entry = (Entry) it.next();
	    if (string.length() > 0) string.append(entrySeparator);
	    string.append(entry.getKey());
	    string.append(keyValueSeparator);
	    string.append(entry.getValue());
	}

	return string.toString();
    }

    /**
     * Splits a String into an array asumming items are separated by spaces.
     *
     * @param string Input string
     * @return String array
     * @since 0.2.0
     */
    public static String[] split(String string)
    {
	if (string == null) return new String[0];

        List<String> list = new LinkedList<String>();
        StringTokenizer tokens = new StringTokenizer(string, " ");
        while(tokens.hasMoreTokens())
            list.add(tokens.nextToken());

        return toArray(list);
    }

    /**
     * Splits a String into an array according to a set of separators.
     *
     * @param string Input string
     * @param separators Set of separators
     * @return String array
     * @since 0.2.0
     */
    public static String[] split(String string, String separators)
    {
	if (string == null) return new String[0];

        List<String> list = new LinkedList<String>();
        StringTokenizer tokens = new StringTokenizer(string, separators);
        while(tokens.hasMoreTokens())
            list.add(tokens.nextToken());

        return toArray(list);
    }

    /**
     * Converts a collection (<code>List</code>, <code>Set</code>...) of objects to a <code>String</code> array. If objects are not instances of <code>String</code>, <code>toString()</code> will be used.
     *
     * @param list Input list
     * @return <code>String</code> array
     * @since 0.2.0
     */
    public static String[] toArray(Collection list)
    {
        if (list == null) return new String[0];

        String[] out = new String[list.size()];
        int i = 0;
        Iterator it = list.iterator();
        while(it.hasNext())
            out[i++] = it.next().toString();

        return out;
    }

    /**
     * Converts a <code>String</code> array to a <code>boolean</code> array.
     *
     * @param array Input array
     * @param valueForNull Value for <code>null</code> positions
     * @return New <code>boolean</code> array
     * @since 0.2.0
     */
    public static boolean[] toBooleanArray(String[] array, boolean valueForNull)
    {
	boolean[] newArray = new boolean[array.length];
	for (int i = 0; i < array.length; i++) newArray[i] = array[i] == null ? valueForNull : Boolean.parseBoolean(array[i]);
	return newArray;
    }

    /**
     * Converts a <code>String</code> array to a <code>double</code> array.
     *
     * @param array Input array
     * @return New <code>double</code> array
     * @since 0.2.0
     */
    public static double[] toDoubleArray(String[] array)
    {
	double[] newArray = new double[array.length];
	for (int i = 0; i < array.length; i++)
	{
	    if (array[i] == null) throw new RuntimeException("Null value in position " + i);
	    newArray[i] = Double.parseDouble(array[i]);
	}
	return newArray;
    }

    /**
     * Converts a <code>String</code> array to a <code>double</code> array.
     *
     * @param array Input array
     * @param valueForNull Value for <code>null</code> positions
     * @return New <code>double</code> array
     * @since 0.2.0
     */
    public static double[] toDoubleArray(String[] array, double valueForNull)
    {
	double[] newArray = new double[array.length];
	for (int i = 0; i < array.length; i++) newArray[i] = array[i] == null ? valueForNull : Double.parseDouble(array[i]);
	return newArray;
    }

    /**
     * Converts a <code>String</code> array to an <code>int</code> array.
     *
     * @param array Input array
     * @return New <code>int</code> array
     * @since 0.2.0
     */
    public static int[] toIntArray(String[] array)
    {
	int[] newArray = new int[array.length];
	for (int i = 0; i < array.length; i++)
	{
	    if (array[i] == null) throw new RuntimeException("Null value in position " + i);
	    newArray[i] = Integer.parseInt(array[i]);
	}
	return newArray;
    }

    /**
     * Converts a <code>String</code> array to an <code>int</code> array.
     *
     * @param array Input array
     * @param valueForNull Value for <code>null</code> positions
     * @return New <code>int</code> array
     * @since 0.2.0
     */
    public static int[] toIntArray(String[] array, int valueForNull)
    {
	int[] newArray = new int[array.length];
	for (int i = 0; i < array.length; i++) newArray[i] = array[i] == null ? valueForNull : Integer.parseInt(array[i]);
	return newArray;
    }

    /**
     * Converts from a <code>String</code> array to a list.
     *
     * @param array Input array
     * @return A list of <code>String</code> objects
     * @since 0.2.0
     */
    public static List<String> toList(String[] array)
    {
        return new ArrayList<String>(Arrays.asList(array));
    }

    /**
     * Checks whether an attribute name is valid according to the rules defined in
     * XML Specification 1.0 [XML].
     * 
     * @param key Attribute name
     * @since 0.2.3
     * @see World Wide Web Consortium (W3C), "Extensible Markup Language (XML) 1.0 (Fifth Edition)," W3C Recommendation, November 2008. [Online] http://www.w3.org/TR/xml/#NT-Name
     */
    public static void checkAttributeName(String key)
    {
        if (!XMLChar.isValidName(key))
            throw new RuntimeException("Not valid attribute value (i.e. contains spaces or invalid symbols)");
    }
}