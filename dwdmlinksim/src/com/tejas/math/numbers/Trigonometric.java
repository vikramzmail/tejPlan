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
public interface Trigonometric<T extends Mathlet> {

    public T sin();

    public T cos();

    public T tan();
    
    public T sec();

    public T csc();
    
    public T cot();

}
