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

import java.util.*;

/**
 * <p>Provides extra functionality for <code>int</code> primitives.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class IntUtils
{
    /**
     * Returns the average value of an array.
     *
     * @param array Input array
     * @return Average value
     * @since 0.2.0
     */
    public static double average(int[] array)
    {
	return array.length == 0 ? 0 : sum(array) / array.length;
    }

    /**
     * Returns the average value only among non-zero values
     *
     * @param array Input array
     * @return Average value among non-zero values
     * @since 0.2.0
     */
    public static double averageNonZeros(int[] array)
    {
	if (array.length == 0) return 0;

	int numNonZeros = 0;
	double sum = 0;
	for(int i = 0; i < array.length; i++)
	{
	    if (array[i] != 0)
	    {
		sum += array[i];
		numNonZeros++;
	    }
	}

	return numNonZeros == 0 ? 0 : sum / numNonZeros;
    }

    /**
     * Converts from an <code>int</code> array to an <code>Integer</code> array.
     *
     * @param array <code>int</code> array
     * @return Equivalent <code>Integer</code> array
     * @since 0.2.0
     */
    public static Integer[] asObjectArray(int[] array)
    {
        Integer[] objectArray = new Integer[array.length];

        for(int i = 0; i < array.length; i++)
            objectArray[i] = array[i];

        return objectArray;
    }

    /**
     * Converts from an <code>Integer</code> array to an <code>int</code> array.
     *
     * @param array <code>Integer</code> array
     * @return Equivalent <code>int</code> array
     * @since 0.2.0
     */
    public static int[] asPrimitiveArray(Integer[] array)
    {
        int[] primitiveArray = new int[array.length];

        for(int i = 0; i < array.length; i++)
            primitiveArray[i] = array[i];

        return primitiveArray;
    }

    /**
     * Checks if an input array contains a given value
     *
     * @param array Input array
     * @param value Value to search
     * @return <code>true</code> if <code>value</code> is present in <code>array</code>, and false otherwise
     * @since 0.2.0
     */
    public static boolean contains(int[] array, int value)
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
     * @since 0.2.0
     */
    public static boolean containsAll(int[] array1, int[] array2)
    {
	if (array2.length == 0) return true;

	Set<Integer> set = new HashSet<Integer>(IntUtils.toList(array2));
	set.removeAll(IntUtils.toList(array1));

	return set.isEmpty();
    }

    /**
     * Checks whether any element of an array is present in another. It is equivalent to assert if <code>intersection(array1, array2).length == 0</code>.
     *
     * @param array1 Container array
     * @param array2 Array with elements to be checked
     * @return <code>true</code> if any number in <code>array1</code> is present in <code>array2</code>, and false otherwise
     * @since 0.2.0
     */
    public static boolean containsAny(int[] array1, int[] array2)
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
     * @since 0.2.0
     */
    public static int[] copy(int[] array)
    {
	return Arrays.copyOf(array, array.length);
    }

    /**
     * Returns a deep copy of the input <code>array</code>.
     *
     * @param array Input array
     * @return Deep copy of <code>array</code>
     * @since 0.2.0
     */
    public static int[][] copy(int[][] array)
    {
	int[][] out = new int[array.length][];
	for(int rowId = 0; rowId < array.length; rowId++)
	    out[rowId] = copy(array[rowId]);

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
    public static int[] find(int[] array, int value, Constants.SearchType searchType)
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
			return toArray(candidateIndexes);

                    case LAST:
                        candidateIndexes.clear();
                        candidateIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }
        }

        return toArray(candidateIndexes);
    }

    /**
     * Computes the greatest absolute common divisor of an integer array.
     *
     * @param array Input array
     * @return The greatest absolute common divisor. By convention, <code>gcd(0, 0)</code> is equal to zero and <code>gcd([])</code> is equal to one
     * @since 0.2.0
     */
    public static int gcd(int[] array)
    {
	if (array.length == 0) return 1;

        int[] maxMinValue = maxMinValues(array);
        int minAbsValue = Math.min(Math.abs(maxMinValue[0]), Math.abs(maxMinValue[1]));

	for(int i = minAbsValue; i >= 1; i--)
        {
            int j;

            for(j = 0; j < array.length; ++j)
                if(Math.abs(array[j]) % i != 0)
                    break;

            if(j == array.length)
                return i;
	}

        return 1;
    }

    /**
     * Returns the intersection vector of a series of input arrays. There is no order guarantee.
     *
     * @param arrays Vector of input arrays
     * @return Intersection vector of input arrays
     * @since 0.2.0
     */
    public static int[] intersect(int[]... arrays)
    {
	if (arrays.length == 0) return new int[0];

	Set<Integer> intersectionSet = new HashSet<Integer>();
	intersectionSet.addAll(toList(arrays[0]));

	for(int i = 1; i < arrays.length; i++)
            intersectionSet.retainAll(toList(arrays[i]));

        return toArray(intersectionSet);
    }

    /**
     * Converts an <code>int</code> array to a <code>double</code> array
     *
     * @param array Input array
     * @return <code>double</code> array
     * @since 0.2.0
     */
    public static double[] toDoubleArray(int[] array)
    {
	double[] out = new double[array.length];
	for(int i = 0; i < out.length; i++)
	    out[i] = array[i];

	return out;
    }

    /**
     * Converts an <code>int</code> array to a <code>long</code> array
     *
     * @param array Input array
     * @return <code>long</code> array
     * @since 0.2.3
     */
    public static long[] toLongArray(int[] array)
    {
	long[] out = new long[array.length];
	for(int i = 0; i < out.length; i++)
	    out[i] = array[i];

	return out;
    }

    /**
     * Joins the elements in an input array using a given separator. It is an improved version of <code>Arrays.toString()</code>.
     *
     * @param array Input array
     * @param separator Separator
     * @return <code>String</code> representation of the input <code>array</code>
     * @since 0.2.0
     */
    public static String join(int[] array, String separator)
    {
        if (array.length == 0) return "";

        StringBuilder out = new StringBuilder();
        out.append(array[0]);

        for(int i = 1; i < array.length; i++)
            out.append(separator).append(array[i]);

        return out.toString();
    }

    /**
     * Returns the maximum value in the input array.
     *
     * @param array Input array
     * @return Maximum value
     * @since 0.2.0
     */
    public static int maxValue(int[] array)
    {
        if (array.length == 0) throw new NoSuchElementException("Empty array");

        int maxValue = array[0];
        for(int i = 1; i < array.length; i++)
            if (array[i] > maxValue)
                maxValue = array[i];

        return maxValue;
    }

    /**
     * Returns the maximum value in the input array.
     *
     * @param array Input array
     * @return Maximum value
     * @since 0.2.0
     */
    public static int maxValue(int[][] array)
    {
        if (array.length == 0) throw new NoSuchElementException("Empty array");

	int maxValue = Integer.MIN_VALUE;

	for(int i = 0; i < array.length; i++)
	    for(int j = 0; j < array[i].length; j++)
		if (array[i][j] > maxValue)
		    maxValue = array[i][j];

	return maxValue;
    }

    /**
     * Returns the position(s) in which the maximum value is found.
     *
     * @param array Input array
     * @param searchType Indicates whether the first, the last, or all maximum positions are returned
     * @return Position(s) in which the maximum value is found
     * @since 0.2.0
     */
    public static int[] maxIndexes(int[] array, Constants.SearchType searchType)
    {
        if (array.length == 0) return new int[0];

        List<Integer> candidateIndexes = new ArrayList<Integer>();
        candidateIndexes.add(0);
        
        int maxValue = array[0];
        for(int i = 1; i < array.length; i++)
        {
            if (array[i] > maxValue)
            {
                maxValue = array[i];
                candidateIndexes.clear();
                candidateIndexes.add(i);
            }
            else if (array[i] == maxValue)
            {
                switch(searchType)
                {
                    case ALL:
                        candidateIndexes.add(i);
                        break;

                    case FIRST:
                        continue;

                    case LAST:
                        candidateIndexes.clear();
                        candidateIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }
        }

        return toArray(candidateIndexes);
    }

    /**
     * Returns the maximum/minimum values of an input array.
     *
     * @param array Input array
     * @return Maximum/minimum values
     * @since 0.2.0
     */
    public static int[] maxMinValues(int[] array)
    {
        if (array.length == 0) throw new NoSuchElementException("Empty array");

        int maxValue = array[0];
        int minValue = array[0];
        for(int i = 1; i < array.length; i++)
        {
            if (array[i] > maxValue)
                maxValue = array[i];

            if (array[i] < minValue)
                minValue = array[i];
        }

        return new int[] {maxValue, minValue};
    }

    /**
     * <p>Returns the position(s) in which the maximum/minimum values are found.</p>
     *
     * <p><code>out[0]</code> are the maximum positions, while <code>out[1]</code> are the minimum positions</p>
     *
     * @param array Input array
     * @param searchType Indicates whether the first, the last, or all maximum positions are returned
     * @return Position(s) in which the maximum/minimum value are found.
     * @since 0.2.0
     */
    public static int[][] maxMinIndexes(int[] array, Constants.SearchType searchType)
    {
        if (array.length == 0) return new int[2][0];

        List<Integer> candidateMaxIndexes = new ArrayList<Integer>();
        candidateMaxIndexes.add(0);
        List<Integer> candidateMinIndexes = new ArrayList<Integer>();
        candidateMinIndexes.add(0);

        int maxValue = array[0];
        int minValue = array[0];
        for(int i = 1; i < array.length; i++)
        {
            if (array[i] > maxValue)
            {
                maxValue = array[i];
                candidateMaxIndexes.clear();
                candidateMaxIndexes.add(i);
            }
            else if (array[i] == maxValue)
            {
                switch(searchType)
                {
                    case ALL:
                        candidateMaxIndexes.add(i);
                        break;

                    case FIRST:
                        continue;

                    case LAST:
                        candidateMaxIndexes.clear();
                        candidateMaxIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }

            if (array[i] < minValue)
            {
                minValue = array[i];
                candidateMaxIndexes.clear();
                candidateMaxIndexes.add(i);
            }
            else if (array[i] == minValue)
            {
                switch(searchType)
                {
                    case ALL:
                        candidateMaxIndexes.add(i);
                        break;

                    case FIRST:
                        continue;

                    case LAST:
                        candidateMaxIndexes.clear();
                        candidateMaxIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }
        }

        return new int[][] {toArray(candidateMaxIndexes), toArray(candidateMinIndexes)};
    }

    /**
     * Returns the minimum value in the input array.
     *
     * @param array Input array
     * @return Minimum value
     * @since 0.2.0
     */
    public static int minValue(int[] array)
    {
        if (array.length == 0) throw new NoSuchElementException("Empty array");

        int minValue = array[0];
        for(int i = 1; i < array.length; i++)
            if (array[i] < minValue)
                minValue = array[i];

        return minValue;
    }

    /**
     * Returns the position(s) in which the minimum value is found.
     *
     * @param array Input array
     * @param searchType Indicates whether the first, the last, or all minimum positions are returned
     * @return Position(s) in which the minimum value is found
     * @since 0.2.0
     */
    public static int[] minIndexes(int[] array, Constants.SearchType searchType)
    {
        if (array.length == 0) return new int[0];

        List<Integer> candidateIndexes = new ArrayList<Integer>();
        candidateIndexes.add(0);
        
        int minValue = array[0];
        for(int i = 1; i < array.length; i++)
        {
            if (array[i] < minValue)
            {
                minValue = array[i];
                candidateIndexes.clear();
                candidateIndexes.add(i);
            }
            else if (array[i] == minValue)
            {
                switch(searchType)
                {
                    case ALL:
                        candidateIndexes.add(i);
                        break;

                    case FIRST:
                        continue;

                    case LAST:
                        candidateIndexes.clear();
                        candidateIndexes.add(i);
                        break;

                    default:
                        throw new Net2PlanException("Invalid search type argument");
                }
            }
        }

        return toArray(candidateIndexes);
    }

    /**
     * Returns an array filled with ones.
     *
     * @param N Number of elements
     * @return An all-one array of length <code>N</code>
     * @since 0.2.0
     */
    public static int[] ones(int N)
    {
	int[] array = new int[N];
	Arrays.fill(array, 1);

	return array;
    }

    /**
     * Returns the standard deviation of an array.
     *
     * @param array Input array
     * @return Standard deviation
     * @since 0.2.0
     */
    public static double std(int[] array)
    {
	if (array.length == 0) return 0;

	double avg = IntUtils.average(array);
	return avg / array.length;
    }

    /**
     * Reverses the order of the elements of the input array (it will be overriden).
     *
     * @param array Input array
     * @since 0.2.0
     */
    public static void reverse(int[] array)
    {
        for(int i = 0; i < array.length/2; ++i )
        {
            int temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    /**
     * Returns a column of a bidimensional input array.
     *
     * @param array Input array
     * @param column Column
     * @return Column of an array
     * @since 0.2.0
     */
    public static int[] selectColumn(int[][] array, int column)
    {
	int[] output = new int[array.length];

	for(int i = 0; i < output.length; i++)
	{
	    output[i] = array[i][column];
	}

	return output;
    }

    /**
     * Returns a row of a bidimensional input array.
     *
     * @param array Input array
     * @param row Row
     * @return Row of an array
     * @since 0.2.0
     */
    public static int[] selectRow(int[][] array, int row)
    {
	return array[row];
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
    public static int[] select(int[] array, int[] indexes)
    {
	int[] out = new int[indexes.length];

	for(int i = 0; i < indexes.length; i++)
	    out[i] = array[indexes[i]];

	return out;
    }

    /**
     * Returns the elements contained in the first array, but not any of the others.
     *
     * @param arrays Input arrays
     * @return Difference set
     * @since 0.2.0
     */
    public static int[] setdiff(int[]... arrays)
    {
	Set<Integer> differenceSet = new HashSet<Integer>();
        for(int i = 0; i < arrays[0].length; i++)
            differenceSet.add(arrays[0][i]);

	for(int i = 1; i < arrays.length; i++)
	{
            List<Integer> aux = toList(arrays[i]);
            differenceSet.removeAll(aux);
	}

        int[] differenceArray = toArray(differenceSet);

	return differenceArray;
    }

    /**
     * Sorts the input array (it will be overriden).
     *
     * @param array Input array
     * @param orderingType Ascending or descending
     * @since 0.2.0
     */
    public static void sort(int[] array, Constants.OrderingType orderingType)
    {
        Arrays.sort(array);

        switch(orderingType)
        {
            case ASCENDING:
                break;

            case DESCENDING:
                reverse(array);
                break;

            default:
                throw new Net2PlanException("Invalid ordering type argument");
        }
    }

    /**
     * Sorts indexes of the <code>array</code> into ascending/descending order in a stable way. Stable means that index order doesn't change if values are the same.
     *
     * @param array Array to be sorted
     * @param orderingType Ascending or descending
     * @return Sorted indexes
     * @since 0.2.0
     */
    public static int[] sortIndexes(int[] array, Constants.OrderingType orderingType)
    {
	Map<Integer, Integer> map = new TreeMap<Integer, Integer>();

	int[] indexes = new int[array.length];

	for (int n = 0; n < array.length; n++)
	    map.put(array[n] * array.length + n, n);

	int n = 0;

	if (orderingType == Constants.OrderingType.DESCENDING)
	    map = ((TreeMap) map).descendingMap();

	for (Integer index : map.values())
	    indexes[n++] = index;

	if (orderingType == Constants.OrderingType.DESCENDING)
	{
	    for (int fromIndex = 0; fromIndex < indexes.length; fromIndex++)
	    {
		int currentValue = array[indexes[fromIndex]];

		int toIndex = -1;
		for (int j = fromIndex + 1; j < indexes.length; j++)
		{
		    if (array[indexes[j]] == currentValue)
		    {
			toIndex = j;
		    }
		    else
		    {
			break;
		    }
		}

		if (toIndex != -1)
		{
		    int len = toIndex - fromIndex + 1;
		    int[] oldValues = Arrays.copyOfRange(indexes, fromIndex, toIndex + 1);

		    for (int i = 0; i < len; i++)
		    {
			indexes[fromIndex + i] = oldValues[len - i - 1];
		    }

		    fromIndex = toIndex;
		}
	    }
	}

	return indexes;
    }

    /**
     * Converts a collection (<code>List</code>, <code>Set</code>...) of <code>Integer</code> objects to an <code>int</code> array.
     *
     * @param list Input list
     * @return <code>int</code> array
     * @since 0.2.0
     */
    public static int[] toArray(Collection<Integer> list)
    {
        return asPrimitiveArray(list.toArray(new Integer[list.size()]));
    }

    /**
     * Converts from a <code>int</code> array to a list.
     *
     * @param array Input array
     * @return A list of <code>Integer</code> objects
     * @since 0.2.0
     */
    public static List<Integer> toList(int[] array)
    {
        List<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < array.length; i++)
            list.add(array[i]);

        return list;
    }

    /**
     * Concatenates a series of arrays.
     *
     * @param arrays List of arrays
     * @return Concatenated array
     * @since 0.2.0
     */
    public static int[] concatenate(int[]... arrays)
    {
	List<Integer> list = new LinkedList<Integer>();

	for(int i = 0; i < arrays.length; i++)
	    list.addAll(toList(arrays[i]));

        int[] concatArray = toArray(list);

        return concatArray;
    }

    /**
     * Returns an array with all elements in input arrays (no repetitions). There is no order guarantee.
     *
     * @param arrays Input arrays
     * @return A new array with all elements in input arrays
     * @since 0.2.0
     */
    public static int[] union(int[]... arrays)
    {
	Set<Integer> unionSet = new HashSet<Integer>();

	for(int i = 0; i < arrays.length; i++)
	    for(int j = 0; j < arrays[i].length; j++)
		unionSet.add(arrays[i][j]);

        int[] unionArray = toArray(unionSet);

        return unionArray;
    }

    /**
     * Returns the same values of the input <code>array</code> but with no repetitions. There is no order guarantee.
     *
     * @param array Input array
     * @return Same values as in <code>array</code> but with no repetitions
     * @since 0.2.0
     */
    public static int[] unique(int[] array)
    {
        Set<Integer> uniqueSet = new HashSet<Integer>();
        for(int i = 0; i < array.length; i++)
            uniqueSet.add(array[i]);

        int[] uniqueArray = toArray(uniqueSet);

        return uniqueArray;
    }

    /**
     * Returns an array filled with zeros.
     *
     * @param N Number of elements
     * @return An all-zero array of length <code>N</code>
     * @since 0.2.0
     */
    public static int[] zeros(int N)
    {
	int[] array = new int[N];
	Arrays.fill(array, 0);

	return array;
    }

    /**
     * Returns the sum of all elements in the array.
     *
     * @param array Input array
     * @return Sum of all array elements
     * @since 0.2.0
     */
    public static double sum(int[] array)
    {
	double out = 0;
	for(int i = 0; i < array.length; i++)
	    out += array[i];

	return out;
    }

    /**
     * Returns the element-wise sum of two arrays.
     *
     * @param array1 Input array 1
     * @param array2 Input array 2
     * @return A new array with the element-wise sum
     * @since 0.2.0
     */
    public static int[] sum(int[] array1, int[] array2)
    {
	int[] out = new int[array1.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array1[i] + array2[i];

	return out;
    }

    /**
     * Multiplies all elements in an array by a scalar.
     *
     * @param array Input array
     * @param value Scalar
     * @return A new array containing the input elements multiplied by the scalar
     * @since 0.2.0
     */
    public static int[] mult(int[] array, int value)
    {
	int[] out = new int[array.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array[i] * value;

	return out;
    }

    /**
     * Multiplies all elements in a matrix by a scalar.
     *
     * @param matrix Input array
     * @param value Scalar
     * @return A new matrix containing the input elements multiplied by the scalar
     * @since 0.2.3
     */
    public static int[][] mult(int[][] matrix, int value)
    {
	int[][] out = new int[matrix.length][];

	for(int i = 0; i < out.length; i++)
	    out[i] = IntUtils.mult(matrix[i], value);

	return out;
    }

    /**
     * Multiplies two arrays element-to-element.
     *
     * @param array1 Input array 1
     * @param array2 Input array 2
     * @return The element-wise multiplication of the input arrays
     * @since 0.2.0
     */
    public static int[] mult(int[] array1, int[] array2)
    {
	int[] out = new int[array1.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array1[i] * array2[i];

	return out;
    }

    /**
     * Returns the element-wise substraction of two arrays.
     *
     * @param array1 Input array 1
     * @param array2 Input array 2
     * @return A new array with the element-wise subtraction
     * @since 0.2.0
     */
    public static int[] substract(int[] array1, int[] array2)
    {
	int[] out = new int[array1.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array1[i] - array2[i];

	return out;
    }

    /**
     * Divides all elements in an array by a scalar.
     *
     * @param array Input array
     * @param value Scalar
     * @return A new array containing the input elements divided by the scalar
     * @since 0.2.0
     */
    public static int[] divide(int[] array, int value)
    {
	int[] out = new int[array.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array[i] / value;

	return out;
    }

    /**
     * Divides two arrays element-to-element.
     *
     * @param array1 Numerator
     * @param array2 Denominator
     * @return The element-wise division of the input arrays
     * @since 0.2.0
     */
    public static int[] divide(int[] array1, int[] array2)
    {
	int[] out = new int[array1.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array1[i] / array2[i];

	return out;
    }

    /**
     * Divides two arrays element-to-element, but when numerator and denominator = 0, returns 0 instead of a singularity (NaN)
     *
     * @param array1 Numerator
     * @param array2 Denominator
     * @return The element-wise division of the input arrays
     * @since 0.2.0
     */
    public static int[] divideNonSingular(int[] array1, int[] array2)
    {
	int[] out = new int[array1.length];

	for(int i = 0; i < out.length; i++)
	    out[i] = array1[i] == 0 && array2[i] == 0 ? 0 : array1[i] / array2[i];

	return out;
    }

    /**
     * Scalar product of two vectors.
     *
     * @param array1 Input array 1
     * @param array2 Input array 2
     * @return The scalar product of the input arrays
     * @since 0.2.2
     */
    public static double scalarProduct(int[] array1, int[] array2)
    {
	return sum(mult(array1, array2));
    }

    /**
     * Sorts a 2D <code>int</code> array based on a given column.
     *
     * @param columnId Column identifier
     * @param orderingType Ascending or descending
     * @return A comparator to be used with <code>Arrays.sort</code>
     * @since 0.2.2
     */
    public static Comparator<int[]> getMatrixSorter(final int columnId, final Constants.OrderingType orderingType)
    {
	return new Comparator<int[]>()
	{
	    @Override
	    public int compare(int[] o1, int[] o2)
	    {
		return orderingType == Constants.OrderingType.ASCENDING ? Integer.compare(o1[columnId], o2[columnId]) : Integer.compare(o2[columnId], o1[columnId]);
	    }
	};
    }

    /**
     * Returns an array filled with a given value.
     *
     * @param N Number of elements
     * @param value Value for all elements
     * @return An array of length <code>N</code> with the given value
     * @since 0.2.3
     */
    public static int[] constantArray(int N, int value)
    {
	int[] array = new int[N];
	Arrays.fill(array, value);

	return array;
    }
}
