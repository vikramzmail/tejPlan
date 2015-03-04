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
 * @author Kristopher
 */
public class ArcCosecant extends InverseTrigonometricOp {

    public Real op(Real r) {
        return r.acsc();
    }

    public Complex op(Complex c) {
        return c.acsc();
    }

    public Text toText() {
        return Text.intern("acsc");
    }
}
