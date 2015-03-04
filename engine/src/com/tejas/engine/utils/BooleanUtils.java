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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Provides extra functionality for <code>boolean</code> primitives.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class BooleanUtils
{
    /**
     * Converts from a <code>boolean</code> array to a <code>Boolean</code> array.
     *
     * @param array <code>boolean</code> array
     * @return Equivalent <code>Boolean</code> array
     * @since 0.2.0
     */
    public static Boolean[] asObjectArray(boolean[] array)
    {
        Boolean[] objectArray = new Boolean[array.length];

        for(int i = 0; i < array.length; i++)
            objectArray[i] = array[i];

        return objectArray;
    }

    /**
     * Converts from a <code>Boolean</code> array to a <code>boolean</code> array.
     *
     * @param array <code>Boolean</code> array
     * @return Equivalent <code>boolean</code> array
     * @since 0.2.0
     */
    public static boolean[] asPrimitiveArray(Boolean[] array)
    {
        boolean[] primitiveArray = new boolean[array.length];

        for(int i = 0; i < array.length; i++)
            primitiveArray[i] = array[i];

        return primitiveArray;
    }

    /**
     * Returns a set of selected elements from an input array. It is not backed
     * in the input array, thus changes in this array are not reflected in the
     * input array.
     *
     * @param array Input array
     * @param indexes Position of elements to be selected.
     * @return Set of elements in the order given by <code>indexes</code>
     * @since 0.2.0
     */
    public static boolean[] select(boolean[] array, int[] indexes)
    {
	boolean[] out = new boolean[indexes.length];

	for(int i = 0; i < indexes.length; i++)
	    out[i] = array[indexes[i]];

	return out;
    }

    /**
     * Returns the position(s) where a given value can be found into an array.
     *
     * @param array Input array
     * @param value Value to be searched for
     * @param searchType Indicates whether the first, the last, or all minimum positions are returned
     * @return Position(s) in which the minimum value is found
     * @since 0.2.0
     */
    public static int[] find(boolean[] array, boolean value, Constants.SearchType searchType)
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
     * Converts a collection (<code>List</code>, <code>Set</code>...) of <code>Boolean</code> objects to a <code>boolean</code> array.
     *
     * @param list Input list
     * @return <code>boolean</code> array
     * @since 0.2.0
     */
    public static boolean[] toArray(Collection<Boolean> list)
    {
        return asPrimitiveArray(list.toArray(new Boolean[list.size()]));
    }

    /**
     * Converts from a <code>boolean</code> array to a list.
     *
     * @param array Input array
     * @return A list of <code>Boolean</code> objects
     * @since 0.2.0
     */
    public static List<Boolean> toList(boolean[] array)
    {
        List<Boolean> list = new ArrayList<Boolean>();
        for(int i = 0; i < array.length; i++)
            list.add(array[i]);

        return list;
    }

}
