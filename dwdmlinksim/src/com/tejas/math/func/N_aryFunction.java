/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.math.func;

import com.tejas.math.Mathlet;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class N_aryFunction implements Function{

    Expression expression = new Expression();

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }


    public Mathlet eval() {
        return expression.eval();
    }

    public Text toText() {
        return expression.toText();
    }

}
