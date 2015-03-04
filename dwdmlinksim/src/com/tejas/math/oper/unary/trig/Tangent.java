/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.unary.trig;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Tangent extends TrigonometricOp {

    public Real op(Real r) {
        return r.tan();
    }

    public Complex op(Complex c) {
        return c.tan();
    }

    public Text toText() {
        return Text.intern("tan");
    }
}
