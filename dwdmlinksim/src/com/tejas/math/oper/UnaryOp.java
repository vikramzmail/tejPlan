/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.oper;

import com.tejas.math.Mathlet;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class UnaryOp implements Operation {

    public Mathlet op(Mathlet m) {
        return Mathlet.NULL;
    }

    public abstract Real op(Real r);

    public abstract Complex op(Complex c);

    @Override
    public String toString() {
        return toText().toString();
    }
}
