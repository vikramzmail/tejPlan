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

import java.util.Random;

/**
 * <p>Provides static methods to obtain both random numbers and vectors.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class RandomUtils
{
    /**
     * Returns a random double in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @return Random double in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static double random(double minValue, double maxValue)
    {
	return random(minValue, maxValue, new Random());
    }

    /**
     * Returns a random double in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param r Random generator
     * @return Random double in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static double random(double minValue, double maxValue, Random r)
    {
	if (maxValue <= minValue) throw new RuntimeException("'minValue' must be lower than 'maxValue'");

	double value = minValue + (r.nextDouble()*(maxValue - minValue));
	if (value < minValue || value > maxValue) throw new RuntimeException("Bad - Random double value out of range");

	return value;
    }

    /**
     * Returns a random integer in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @return Random integer in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static int random(int minValue, int maxValue)
    {
	return random(minValue, maxValue, new Random());
    }

    /**
     * Returns a random integer in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param r Random generator
     * @return Random integer in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static int random(int minValue, int maxValue, Random r)
    {
	if (maxValue <= minValue) throw new RuntimeException("'minValue' must be lower than 'maxValue'");
	if (maxValue - minValue + 1 < 0) throw new RuntimeException("Range is so large. Please, reduce it");

	int value = minValue + r.nextInt(maxValue - minValue + 1);
	if (value < minValue || value > maxValue) throw new RuntimeException("Bad - Random int value out of range");

	return value;
    }

    /**
     * Returns a random long in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @return Random long in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static long random(long minValue, long maxValue)
    {
	return random(minValue, maxValue, new Random());
    }

    /**
     * Returns a random long in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param r Random generator
     * @return Random long in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static long random(long minValue, long maxValue, Random r)
    {
	return minValue + ((long)(r.nextDouble()*(maxValue - minValue + 1)));
    }

    /**
     * Returns a random double vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @return A random double vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static double[] random(double minValue, double maxValue, int size)
    {
	return random(minValue, maxValue, size, new Random());
    }

    /**
     * Returns a random double vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @param r Random generator
     * @return A random double vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static double[] random(double minValue, double maxValue, int size, Random r)
    {
	double[] out = new double[size];
	for(int i = 0; i < size; i++)
	    out[i] = random(minValue, maxValue, r);

	return out;
    }

    /**
     * Returns a random integer vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @return A random integer vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static int[] random(int minValue, int maxValue, int size)
    {
	return random(minValue, maxValue, size, new Random());
    }

    /**
     * Returns a random integer vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @param r Random generator
     * @return A random integer vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static int[] random(int minValue, int maxValue, int size, Random r)
    {
	int[] out = new int[size];
	for(int i = 0; i < size; i++)
	    out[i] = random(minValue, maxValue, r);

	return out;
    }

    /**
     * Returns a random long vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @return A random long vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static long[] random(long minValue, long maxValue, int size)
    {
	return random(minValue, maxValue, size, new Random());
    }

    /**
     * Returns a random long vector, with elements in range [minValue, maxValue]
     *
     * @param minValue Minimum value (inclusive)
     * @param maxValue Maximum value (inclusive)
     * @param size Number of elements
     * @param r Random generator
     * @return A random long vector, with elements in range [minValue, maxValue]
     * @since 0.2.0
     */
    public static long[] random(long minValue, long maxValue, int size, Random r)
    {
	long[] out = new long[size];
	for(int i = 0; i < size; i++)
	    out[i] = random(minValue, maxValue, r);

	return out;
    }

    /**
     * Implements Fisher–Yates shuffle.
     *
     * @param array Array to be shuffled
     * @since 0.2.3
     */
    public static void shuffle(int[] array)
    {
        Random r = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            int index = r.nextInt(i + 1);

            int a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
    }

    /**
     * Implements Fisher–Yates shuffle.
     *
     * @param array Array to be shuffled
     * @param r Random generator
     * @since 0.2.3
     */
    public static void shuffle(int[] array, Random r)
    {
        for (int i = array.length - 1; i > 0; i--)
        {
            int index = r.nextInt(i + 1);

            int a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
    }
}
