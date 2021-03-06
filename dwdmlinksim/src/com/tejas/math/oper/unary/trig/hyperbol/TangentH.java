/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.unary.trig.hyperbol;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class TangentH extends HyperbolicOp {

    public Real op(Real r) {
        return r.tanh();
    }

    public Complex op(Complex c) {
        return c.tanh();
    }

    public Text toText() {
        return Text.intern("tanh");
    }
}
