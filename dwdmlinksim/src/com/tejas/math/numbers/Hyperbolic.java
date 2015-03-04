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

public interface Hyperbolic<T extends Mathlet> {

    public T cosh();

    public T sinh();

    public T tanh();

    public T sech();

    public T csch();
    
    public T coth();
}
