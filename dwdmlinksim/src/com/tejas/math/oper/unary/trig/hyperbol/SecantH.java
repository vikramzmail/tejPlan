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
public class SecantH extends HyperbolicOp {

    public Real op(Real r) {
        return r.sech();
    }

    public Complex op(Complex c) {
        return c.sech();
    }

    public Text toText() {
        return Text.intern("sech");
    }
}
