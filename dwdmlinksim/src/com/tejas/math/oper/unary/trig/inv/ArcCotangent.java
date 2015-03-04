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
public class ArcCotangent extends InverseTrigonometricOp {

    public Real op(Real r) {
        return r.acot();
    }

    public Complex op(Complex c) {
        return c.acot();
    }

    public Text toText() {
        return Text.intern("acot");
    }
}
