/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.math.func;

import com.tejas.math.*;

/**
 *
 * @author Kristopher T. Beck
 */
public interface Function<T extends Mathlet> extends Mathlet{

    public T eval();
}
