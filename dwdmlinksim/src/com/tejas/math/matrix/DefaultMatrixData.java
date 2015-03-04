/*
 * DefaultMatrixData.java
 *
 * Created on May 12, 2006, 1:49 PM
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
public class DefaultMatrixData<N extends Numeric<N>> extends MatrixData<N> {

    N[][] data;

    public boolean isFillCopied() {
        return copyFill;
    }

    public void setCopyFill(boolean copyFill) {
        this.copyFill = copyFill;
    }

    /**
     * Creates a new instance of DefaultMatrixData
     */
    protected DefaultMatrixData() {
    }

    public void fillInNulls() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (data[i][j] == null) {
                    data[i][j] = (N) fill.copy();
                }
            }
        }
    }

    public static <N extends Numeric<N>> DefaultMatrixData<N> newInstance(int rows, int cols, N fill) {
        DefaultMatrixData<N> core = new DefaultMatrixData<N>();
        core.data = (N[][]) new Numeric[rows][cols];
        core.rows = rows;
        core.cols = cols;
        core.fill = fill;
        core.fillInNulls();
        return core;
    }

    public static <N extends Numeric<N>> DefaultMatrixData<N> newInstance(N[][] elements) {
        DefaultMatrixData<N> core = new DefaultMatrixData<N>();
        core.data = elements;
        core.rows = elements.length;
        core.cols = elements[0].length;
        core.fill = (N) findFirstNoneNull(elements).copy();
        core.fill.setZero();
        for (int i = 0; i < core.rows; i++) {
            for (int j = 0; j < core.cols; j++) {
                if (elements[i][j] == null) {
                    elements[i][j] = (N) core.fill.copy();
                }
            }
        }
        return core;
    }

    public static <N extends Numeric<N>> DefaultMatrixData<N> newInstance(MatrixData<N> data) {
        DefaultMatrixData<N> core = new DefaultMatrixData<N>();
        core.rows = data.getRowSize();
        core.cols = data.getColSize();
        core.data = (N[][]) new Numeric[core.rows][core.cols];
        core.fill = (N) data.fill;
        if (core.fill != null) {
            core.fill.setZero();
        }
        for (int i = 0; i < core.rows; i++) {
            for (int j = 0; j < core.cols; j++) {
                N n = data.getAt(i, j);
                if (n == null) {
                    n = (N) core.fill.copy();
                }
                core.data[i][j] = n;
            }
        }
        return core;
    }

    public static <N extends Numeric> Numeric findFirstNoneNull(N[][] elements) {
        N elem;
        for (int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[0].length; j++) {
                elem = elements[i][j];
                if (elem != null) {
                    return (N) elem;
                }
            }
        }
        return null;
    }

    protected void resize(int rows, int cols) {
        int _rows = Math.max(rows, this.rows);
        int _cols = Math.max(cols, this.cols);
        N[][] tmp = (N[][]) new Numeric[_rows][_cols];
        if (fill == null) {
            fill = findFill();
        }
        if (data != null) {
            for (int i = 0; i < _rows; i++) {
                for (int j = 0; j < _cols; j++) {
                    if (i >= this.rows || j >= this.cols) {
                        if (fill != null) {
                            if (copyFill && fill != null) {
                                tmp[i][j] = (N) fill.copy();
                            } else {
                                tmp[i][j] = fill;
                            }
                        }
                    } else {
                        tmp[i][j] = data[i][j];
                    }
                }
            }
        }
        this.rows = _rows;
        this.cols = _cols;
        data = tmp;
    }

    void fillIn(int begin, int end) {
        for (int i = begin; i <= rows; i++) {
            for (int j = end; (j >= cols && i >= rows) || j >= 0; j--) {
                data[i][j] = (N) fill.copy();
            }
        }
    }

    N findFill() {
        N n = null;
        loop:
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows; j++) {
                if ((n = data[i][j]) != null) {
                    break loop;
                }
            }
        }
        return n;
    }

    public N getAt(int row, int col) {
        return (N) data[row][col];
    }

    public void setAt(int row, int col, N elem) {
        data[row][col] = elem;
    }

    public void reset() {
        rows = 0;
        cols = 0;
        data = null;
    }

    public void clear() {
        Numeric elem;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                elem = data[i][j];
                if (elem != null) {
                    elem.setZero();
                }
            }
        }
    }

    public DefaultMatrixData toDirect() {
        return newInstance(data);
    }

    public void setSize(int rows, int cols) {
        if (this.rows != rows || this.cols != cols) {
            resize(rows, cols);
        }
    }

    public void setSize(int size) {
        if (rows != size || cols != size) {
            resize(size, size);
        }
    }

    public MatrixData<N> copy() {
        N[][] nData = (N[][]) new Numeric[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                nData[i][j] = (N) data[i][j].copy();
            }
        }
        MatrixData d = newInstance(nData);
        d.setFill(fill);
        return d;
    }

    @Override
    public void swapRows(int row1, int row2) {
        N[] tmp = data[row1];
        data[row1] = data[row2];
        data[row2] = tmp;
    }

    @Override
    public void swapCols(int col1, int col2) {
        for (int i = 0; i < rows; i++) {
            N tmp = data[i][col1];
            data[i][col1] = data[i][col2];
//            System.out.println(toString());
            data[i][col2] = tmp;
//            System.out.println(toString());
        }
    }
}
