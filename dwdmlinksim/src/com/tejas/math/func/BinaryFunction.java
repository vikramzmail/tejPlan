/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.func;

import com.tejas.math.Mathlet;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Numeric;
import com.tejas.math.numbers.Real;
import com.tejas.math.oper.BinaryOp;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class BinaryFunction implements Function {

    Mathlet rtArg;
    Mathlet ltArg;
    Mathlet result;
    BinaryOp op;

    public BinaryFunction(BinaryOp op) {
        this.op = op;
    }

    
    public Mathlet getRHS() {
        return rtArg;
    }

    public void setRHS(Mathlet arg1) {
        this.rtArg = arg1;
    }

    public Mathlet getLHS() {
        return ltArg;
    }

    public void setLHS(Mathlet arg2) {
        this.ltArg = arg2;
    }

    public Mathlet getResult() {
        return result;
    }

    public Mathlet eval() {
        return result = eval(rtArg, ltArg);
    }

    protected Mathlet eval(Mathlet m1, Mathlet m2) {
        return null;
    }

    protected Mathlet eval(Function f1, Mathlet m2) {
        Mathlet mr = f1.eval();
        if(mr instanceof Numeric){
            return eval(mr, m2);
        }
        return this;
    }

    protected Mathlet eval(Mathlet m1, Function f2) {
        Mathlet mr = f2.eval();
        if(mr instanceof Numeric){
            return eval(m1, mr);
        }
        return this;
    }

    protected Mathlet eval(Real r1, Real r2) {
        return op.op(r1, r2);
    }

    protected Mathlet eval(Real r1, Complex c2) {
        return op.op(r1, c2);
    }

    protected Mathlet eval(Complex c1, Real r2) {
        return op.op(c1, r2);
    }

    protected Mathlet eval(Complex c1, Complex c2) {
        return op.op(c1, c2);
    }

    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
