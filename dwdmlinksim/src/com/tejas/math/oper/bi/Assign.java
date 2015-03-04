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
public class Assign extends BinaryOp {

    @Override
    public Real op(Real lhs, Real rhs) {
        lhs.set(rhs);
        return lhs;
    }

    @Override
    public Complex op(Real lhs, Complex rhs) {
        lhs.set(rhs.getReal());
        return rhs;
    }

    @Override
    public Complex op(Complex lhs, Real rhs) {
        lhs.set(rhs);
        return lhs;
    }

    @Override
    public Complex op(Complex lhs, Complex rhs) {
        lhs.set(rhs);
        return lhs;
    }

    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
