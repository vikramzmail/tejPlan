/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import com.tejas.math.numbers.Real;

import javolution.lang.MathLib;
import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Pi extends Constant<Real> {

    public Pi() {
        super(Real.valueOf(MathLib.PI));
    }

    @Override
    public Text toText() {
        return Text.intern("pi");
    }
}
