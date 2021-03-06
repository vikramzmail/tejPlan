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
public class Times extends BinaryOp {

    public Real op(Real lhs, Real rhs) {
        return lhs.times(rhs);
    }

    public Complex op(Real lhs, Complex rhs) {
        return Complex.valueOf(lhs.times(rhs.getReal()).get(), rhs.getImag());
    }

    public Complex op(Complex lhs, Real rhs) {
        return lhs.times(rhs);
    }

    public Complex op(Complex lhs, Complex rhs) {
        return lhs.times(rhs);
    }

    public Text toText() {
        return Text.valueOf('*');
    }
}
