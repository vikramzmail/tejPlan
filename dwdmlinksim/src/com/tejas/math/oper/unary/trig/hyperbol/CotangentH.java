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
 * @author Kristopher
 */
public class CotangentH extends HyperbolicOp {

    public Real op(Real r) {
        return r.coth();
    }

    public Complex op(Complex c) {
        return c.coth();
    }

    public Text toText() {
        return Text.intern("coth");
    }
}
