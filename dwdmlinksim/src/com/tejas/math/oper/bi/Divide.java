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
public class Divide extends BinaryOp {

    public Real op(Real lhs, Real rhs) {
        if (rhs.isZero()) {
            throw new ArithmeticException("Can't divide by 0");
        }
        return lhs.divide(rhs);
    }

    public Complex op(Real lhs, Complex rhs) {
        if (rhs.getReal() == 0) {
            throw new ArithmeticException("Can't divide by 0");
        }
        return Complex.valueOf(lhs.get() / rhs.getReal(), rhs.getImag());
    }

    public Complex op(Complex lhs, Real rhs) {
        if (rhs.isZero()) {
            throw new ArithmeticException("Can't divide by 0");
        }
        return lhs.divide(rhs);
    }

    public Complex op(Complex lhs, Complex rhs) {
        if (rhs.getReal() == 0) {
            throw new ArithmeticException("Can't divide by 0");
        }
        return lhs.divide(rhs);
    }

    public Text toText() {
        return Text.valueOf('/');
    }
}
