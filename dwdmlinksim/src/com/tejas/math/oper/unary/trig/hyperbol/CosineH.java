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
public class CosineH extends HyperbolicOp {

    public Real op(Real r) {
        return r.cosh();
    }

    public Complex op(Complex c) {
        return c.cosh();
    }

    public Text toText() {
        return Text.intern("cosh");
    }
}
