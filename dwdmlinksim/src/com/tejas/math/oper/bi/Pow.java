/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.bi;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;
import com.tejas.math.oper.BinaryOp;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Pow extends BinaryOp {

    public Real op(Real r, double e) {
        return r.pow(e);
    }

    public Complex op(Complex c, double e) {
        return c.pow(e);
    }

    public Real op(Real r, Real e) {
        return r.pow(r);
    }

    public Complex op(Complex c, Real e) {
        return c.pow(c);
    }

    public Complex op(Real r, Complex e) {
        return Complex.valueOf(r.pow(e.getReal()).get(), 0);
    }

    public Complex op(Complex c, Complex e) {
        return c.pow(e);
    }

    public Text toText() {
        return Text.valueOf('^');
    }
}
