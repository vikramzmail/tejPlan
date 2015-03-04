/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.func;

import com.tejas.math.Mathlet;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;
import com.tejas.math.oper.UnaryOp;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class UnaryFunction<T extends Mathlet> implements Function<T> {

    Mathlet right;
    T result;
    UnaryOp op;

    public UnaryFunction(UnaryOp op) {
        this.op = op;
    }

    public Mathlet getRight() {
        return right;
    }

    public void setRight(Mathlet right) {
        this.right = right;
    }

    public T eval() {
        return result = (T) eval(right);
    }

    public Mathlet eval(Mathlet m) {
        return null;
    }

    public Real eval(Real r) {
        return op.op(r);
    }

    public Complex eval(Complex c) {
        return op.op(c);
    }

    public T eval(Function<T> f) {
        T r = f.eval();
        if(r == null){
            return null;
        }
        return r;
    }

    public Text toText() {
        return op.toText().concat(Text.valueOf('(').concat(right.toText()).concat(Text.valueOf(')')));
    }
}
