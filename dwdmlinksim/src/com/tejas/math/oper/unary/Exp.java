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
public class Exp extends UnaryOp {

    public Real op(Real r) {
        return r.exp();
    }

    public Complex op(Complex c) {
        return c.exp();
    }

    public Text toText() {
        return Text.intern("exp");
    }
}
