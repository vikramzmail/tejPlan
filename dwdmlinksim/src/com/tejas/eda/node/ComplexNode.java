/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.node;

import com.tejas.math.numbers.Complex;

/**
 *
 * @author owenbad
 */
public class ComplexNode extends Complex {

    int row;
    int col;

    public ComplexNode(int row, int col) {
        this.row = row;
        this.col = col;
        setZero();
    }

}
