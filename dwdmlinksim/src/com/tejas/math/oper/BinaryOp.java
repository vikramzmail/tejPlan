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
public abstract class BinaryOp implements Operation {

    public Mathlet op(Mathlet lhs, Mathlet rhs) {
        return Mathlet.NULL;
    }

    ;

    public abstract Real op(Real lhs, Real rhs);

    public abstract Complex op(Real lhs, Complex rhs);

    public abstract Complex op(Complex lhs, Real rhs);

    public abstract Complex op(Complex lhs, Complex rhs);

    @Override
    public String toString() {
        return toText().toString();
    }
}
