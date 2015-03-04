/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.numbers;

import com.tejas.math.Mathlet;

/**
 *
 * @author Kristopher T. Beck
 */
public interface Field<T extends Mathlet> {

    public T plus(T f);

    public T minus(T f);

    public T times(T f);

    public T divide(T f);
}
