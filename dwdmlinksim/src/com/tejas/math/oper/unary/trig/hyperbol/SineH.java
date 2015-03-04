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
public class SineH extends HyperbolicOp {

    public Real op(Real r) {
        return r.sinh();
    }

    public Complex op(Complex c) {
        return c.sinh();
    }

    public Text toText() {
        return Text.intern("sinh");
    }
}
