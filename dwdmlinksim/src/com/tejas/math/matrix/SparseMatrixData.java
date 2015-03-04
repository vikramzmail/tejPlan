/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class SparseMatrixData<N extends Numeric<N>> extends MatrixData<N> {

    public abstract void add(int row, int col, N elem);

    public abstract void remove(int row, int col);
}
