/*
 * MatrixData.java
 *
 * Created on May 12, 2006, 1:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;

/**
 *
 * @param N 
 * @author Kristopher T. Beck
 */
public abstract class MatrixData<N extends Numeric<N>> {

    int rows;
    int cols;
    boolean copyFill = true;
    N fill;

    public abstract void clear();

    public abstract void reset();

    public abstract N getAt(int row, int col);

    public abstract void setAt(int row, int col, N elem);

    public N getFill() {
        N n = fill;
        if (n == null) {
            loop:
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if ((n = getAt(rows, cols)) != null) {
                        fill = n;
                        break loop;
                    }
                }
            }
        }
        return fill;
    }

    public void setFill(N fill) {
        this.fill = fill;
    }

    public int getRowSize() {
        return rows;
    }

    public abstract void setSize(int rows, int cols);

    public int getColSize() {
        return cols;
    }

    public abstract void setSize(int size);

    public N getDiag(int index) {
        return getAt(index, index);
    }

    public boolean isSquare() {
        return rows == cols ? true : false;
    }

    public abstract void swapRows(int row1, int row2);

    public abstract void swapCols(int col1, int col2);

    public abstract MatrixData<N> copy();
}
