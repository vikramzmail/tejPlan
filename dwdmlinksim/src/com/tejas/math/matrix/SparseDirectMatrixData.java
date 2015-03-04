/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import java.util.Arrays;

import com.tejas.math.numbers.Numeric;

/**
 *
 * @author Kristopher T. Beck
 */
public class SparseDirectMatrixData<N extends Numeric<N>>     extends SparseMatrixData<N> {

    N[][] elements;
    int[] firstColumnIndex;
    int MAX = 32767;
    int OUT_OF_BOUNDS = MAX + 1;

    @Override
    public void clear() {
        Arrays.fill(firstColumnIndex, OUT_OF_BOUNDS);
    }

    @Override
    public void reset() {
        elements = (N[][]) new Numeric[0][];
        firstColumnIndex = new int[0];
        rows = 0;
        cols = 0;
    }

    @Override
    public N getAt(int row, int col) {
        col = col - firstColumnIndex[row];
        if (col < 0 || col > elements[row].length) {
            return null;
        }
        return elements[row][col];
    }

    @Override
    public void setAt(int row, int col, N elem) {
        col = col - firstColumnIndex[row];
        if (col < 0 || col > elements[row].length) {
            add(row, col, elem);
        } else {
            elements[row][col].set(elem);
        }
    }

    @Override
    public void setSize(int rows, int cols) {
        N[][] oldElems = elements;
        elements = (N[][]) new Numeric[rows][];
        int min = this.rows < rows ? this.rows : rows;
        for (int i = 0; i < min; i++) {
            elements[i] = oldElems[i];
        }
        for (int i = this.rows; i < rows; i++) {
            elements[i] = (N[]) new Numeric[0];
        }
        firstColumnIndex = Arrays.copyOf(firstColumnIndex, rows);
        for (int i = this.rows; i < rows; i++) {
            firstColumnIndex[i] = OUT_OF_BOUNDS;
        }
        this.rows = rows;
        this.cols = cols;
    }

    @Override
    public void setSize(int size) {
        setSize(size, size);
    }

    @Override
    public void swapRows(int row1, int row2) {
        N[] tmp = elements[row1];
        elements[row1] = elements[row2];
        elements[row2] = tmp;
        int itmp = firstColumnIndex[row1];
        firstColumnIndex[row1] = firstColumnIndex[row2];
        firstColumnIndex[row2] = itmp;
    }

    @Override
    public void swapCols(int col1, int col2) {
        N[] tmp = (N[]) new Numeric[rows];
        for (int i = 0; i < rows; i++) {
            tmp[i] = getAt(i, col1);
            remove(i, col1);
        }
        for (int i = 0; i < rows; i++) {
            int fir = firstColumnIndex[i];
            int lir = firstColumnIndex[i] + elements[i].length - 1;
            N n = getAt(i, col2);
            if (n != null) {
                if (col2 < fir || col2 > lir) {
                    add(i, col1, n);
                } else {
                    elements[i][col1] = n;
                }
            }
            remove(i, col2);
        }
        for (int i = 0; i < rows; i++) {
            int fir = firstColumnIndex[i];
            int lir = firstColumnIndex[i] + elements[i].length - 1;
            N n = tmp[i];
            if (n != null) {
                if (col1 < fir || col1 > lir) {
                    add(i, col2, n);
                } else {
                    elements[i][col2] = n;
                }
            }
        }
    }

    @Override
    public MatrixData<N> copy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(int row, int col, N elem) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove(int row, int col) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
