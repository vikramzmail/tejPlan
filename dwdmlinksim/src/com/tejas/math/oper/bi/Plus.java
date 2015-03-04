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
public class Plus extends BinaryOp {

    public Real op(Real lhs, Real rhs) {
        return lhs.plus(rhs);
    }

    public Complex op(Real lhs, Complex rhs) {
        return rhs.plus(lhs);
    }

    public Complex op(Complex lhs, Real rhs) {
        return lhs.plus(rhs);
    }

    public Complex op(Complex lhs, Complex rhs) {
        return lhs.plus(rhs);
    }

    public Text toText() {
        return Text.valueOf('+');
    }
}
