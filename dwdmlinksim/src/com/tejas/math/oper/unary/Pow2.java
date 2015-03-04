/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.unary;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;
import com.tejas.math.oper.UnaryOp;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Pow2 extends UnaryOp {

    public Real op(Real r) {
        return r.pow2();
    }

    public Complex op(Complex c) {
        return c.pow2();
    }

    public Text toText() {
        return Text.intern("^2");
    }
}
