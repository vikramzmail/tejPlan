/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.unary.trig.inv;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class ArcCosine extends InverseTrigonometricOp {

    public Real op(Real r) {
        return r.acos();
    }

    public Complex op(Complex c) {
        return c.acos();
    }

    public Text toText() {
        return Text.intern("acos");
    }
}
