/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper.unary.trig.hyperbol.inv;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.text.Text;

/**
 *
 * @author Kristopher
 */
public class ArCosecantH extends InverseHyperbolicOp {

    public Real op(Real r) {
        return r.acsch();
    }

    public Complex op(Complex c) {
        return c.acsch();
    }

    public Text toText() {
        return Text.intern("acsch");
    }
}
