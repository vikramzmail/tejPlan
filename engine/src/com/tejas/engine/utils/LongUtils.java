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

import com.tejas.engine.utils.Constants;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;

import static com.tejas.engine.utils.Constants.SearchType.ALL;
import static com.tejas.engine.utils.Constants.SearchType.FIRST;
import static com.tejas.engine.utils.Constants.SearchType.LAST;

import java.util.*;

/**
 * <p>Provides extra functionality for <code>long</code> primitives.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class LongUtils
{
    /**
     * Converts from an <code>long</code> array to an <code>Long</code> array.
     *
     * @param array <code>long</code> array
     * @return Equivalent <code>Long</code> array
     * @since 0.2.3
     */
    public static Long[] asObjectArray(long[] array)
    {
        Long[] objectArray = new Long[array.length];

        for(int i = 0; i < array.length; i++)
            objectArray[i] = array[i];

        return objectArray;
    }

    /**
     * Converts from an <code>Long</code> array to an <code>long</code> array.
     *
     * @param array <code>Long</code> array
     * @return Equivalent <code>long</code> array
     * @since 0.2.3
     */
    public static long[] asPrimitiveArray(Long[] array)
    {
        long[] primitiveArray = new long[array.length];

        for(int i = 0; i < array.length; i++)
            primitiveArray[i] = array[i];

        return primitiveArray;
    }

    /**
     * Concatenates a series of arrays.
     *
     * @param arrays List of arrays
     * @return Concatenated array
     * @since 0.2.3
     */
    public static long[] concatenate(long[]... arrays)
    {
	List<Long> list = new LinkedList<Long>();

	for(int i = 0; i < arrays.length; i++)
	    list.addAll(toList(arrays[i]));

        long[] concatArray = toArray(list);

        return concatArray;
    }

    /**
     * Checks if an input array contains a given value
     *
     * @param array Input array
     * @param value Value to search
     * @return <code>true</code> if <code>value</code> is present in <code>array</code>, and false otherwise
     * @since 0.2.3
     */
    public static boolean contains(long[] array, long value)
    {
        for(int i = 0; i < array.length; i++)
            if (array[i] == value)
                return true;

        return false;
    }

    /**
     * Checks if an array contains all numbers from another.
     *
     * @param array1 Container array
     * @param array2 Array with elements to be checked
     * @return <code>true</code> if <code>value</code> is present in <code>array</code>, and false otherwise
     * @since 0.2.3
     */
    public static boolean containsAll(long[] array1, long[] array2)
    {
	if (array2.length == 0) return true;

	Set<Long> set = new HashSet<Long>(LongUtils.toList(array2));
	set.removeAll(LongUtils.toList(array1));

	return set.isEmpty();
    }

    /**
     * Checks whether any element of an array is present in another.
     *
     * @param array1 Container array
     * @param array2 Array with elements to be checked
     * @return <code>true</code> if any number in <code>array1</code> is present in <code>array2</code>, and false otherwise
     * @since 0.2.3
     */
    public static boolean containsAny(long[] array1, long[] array2)
    {
        for(int i = 0; i < array1.length; i++)
            for(int j = 0; j < array2.length; j++)
                if (array1[i] == array2[j]) return true;
        return false;
    }

    /**
     * Returns a deep copy of the input <code>array</code>.
     *
     * @param array Input array
     * @return Deep copy of <code>array</code>
     * @since 0.2.3
     */
    public static long[] copy(long[] array)
    {
	return Arrays.copyOf(array, array.length);
    }

    /**
     * Returns the elements contained in the first array, but not any of the others.
     *
     * @param arrays Input arrays
     * @return Difference set
     * @since 0.2.3
     */
    public static long[] setdiff(long[]... arrays)
    {
	Set<Long> differenceSet = new HashSet<Long>();
        for(int i = 0; i < arrays[0].length; i++)
            differenceSet.add(arrays[0][i]);

	for(int i = 1; i < arrays.length; i++)
	{
            List<Long> aux = toList(arrays[i]);
            differenceSet.removeAll(aux);
	}

        long[] differenceArray = toArray(differenceSet);

	return differenceArray;
    }

    /**
     * Converts a collection (<code>List</code>, <code>Set</code>...) of <code>Long</code> objects to a <code>long</code> array.
     *
     * @param list Input list
     * @return <code>long</code> array
     * @since 0.2.3
     */
    public static long[] toArray(Collection<Long> list)
    {
        return asPrimitiveArray(list.toArray(new Long[list.size()]));
    }

    /**
     * Converts from a <code>long</code> array to a list.
     *
     * @param array Input array
     * @return A list of <code>Long</code> objects
     * @since 0.2.3
     */
    public static List<Long> toList(long[] array)
    {
        List<Long> list = new ArrayList<Long>();
        for(int i = 0; i < array.length; i++)
            list.add(array[i]);

        return list;
    }

    /**
     * Joins the elements in an input array using a given separator. It is an improved version of <code>Arrays.toString()</code>.
     *
     * @param array Input array
     * @param separator Separator
     * @return <code>String</code> representation of the input <code>array</code>
     * @since 0.2.0
     */
    public static String join(long[] array, String separator)
    {
        if (array.length == 0) return "";

        StringBuilder out = new StringBuilder();
        out.append(array[0]);

        for(int i = 1; i < array.length; i++)
            out.append(separator).append(array[i]);

        return out.toString();
    }
    
    /**
     * Converts a <code>long</code> array to an <code>int</code> array (truncation may happen).
     *
     * @param array Input array
     * @return <code>int</code> array
     * @since 0.2.3
     */
    public static int[] toIntArray(long[] array)
    {
	int[] out = new int[array.length];
	for(int i = 0; i < out.length; i++)
	    out[i] = (int) array[i];

	return out;
    }

    /**
     * Returns an array with all elements in input arrays (no repetitions). There is no order guarantee.
     *
     * @param arrays Input arrays
     * @return A new array with all elements in input arrays
     * @since 0.2.3
     */
    public static long[] union(long[]... arrays)
    {
	Set<Long> unionSet = new HashSet<Long>();

	for(int i = 0; i < arrays.length; i++)
	    for(int j = 0; j < arrays[i].length; j++)
		unionSet.add(arrays[i][j]);

        long[] unionArray = toArray(unionSet);

        return unionArray;
    }

    /**
     * Returns the position(s) where a given value can be found into an array.
     *
     * @param array Input array
     * @param value Value to be searched for
     * @param searchType Indicates whether the first, the last, or all minimum positions are returned
     * @return Position(s) in which the minimum value is found
     * @since 0.2.3
     */
    public static int[] find(long[] array, long value, Constants.SearchType searchType)
    {
        List<Integer> candidateIndexes = new ArrayList<Integer>();

	for(int i = 0; i < array.length; i++)
        {
            if (array[i] == value)
            {
                switch(searchType)
                {
                    case ALL:
                        candidateIndexes.add(i);
                        break;

                    case FIRST:
			candidateIndexes.add(i);
			return IntUtils.toArray(candidateIndexes);

                    case LAST:
                        candidateIndexes.clear();
                        candidateIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }
        }

        return IntUtils.toArray(candidateIndexes);
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
    public static long[] select(long[] array, int[] indexes)
    {
	long[] out = new long[indexes.length];

	for(int i = 0; i < indexes.length; i++)
	    out[i] = array[indexes[i]];

	return out;
    }
}
