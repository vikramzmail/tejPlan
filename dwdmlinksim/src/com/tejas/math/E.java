/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import com.tejas.math.numbers.Real;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class E extends Constant<Real> {

    public E() {
        super(Real.valueOf(Math.E));
    }

    @Override
    public Text toText() {
        return Text.intern("e");
    }
}
