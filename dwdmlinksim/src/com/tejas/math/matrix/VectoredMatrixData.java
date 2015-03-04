/*
 * IndirectcoreRows.java
 *
 * Created on May 24, 2006, 6:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;
import com.tejas.math.vector.Vector;

import javolution.util.FastTable;

/**
 *
 * @param N 
 * @author Kristopher T. Beck
 */
public class VectoredMatrixData<N extends Numeric<N>>
        extends MatrixData<N> {

    FastTable<Vector<N>> data;

    public boolean isCopyFill() {
        return copyFill;
    }

    public void setCopyFill(boolean copyFill) {
        this.copyFill = copyFill;
    }

    /**
     * Creates a new instance of IndirectcoreRows
     */
    public VectoredMatrixData() {
    }

    public void clear() {
    }

    public N createElement(int row, int col) {
        while (data.size() <= row) {
            data.add(Vector.FACTORY.object());
        }
        FastTable<N> rowTable = data.get(row);
        while (rowTable.size() <= col) {
            rowTable.add(null);
        }
        N e = (N) fill.copy();
        data.get(row).set(col, e);
        return e;
    }

    public void reset() {
        data.reset();
    }

    public void setSize(int size) {
        setSize(size, size);
    }

    public void setSize(int rows, int cols) {
        while (rows > data.size()) {
            Vector<N> t = Vector.FACTORY.object();
            for (int i = 0; i < cols; i++) {
                t.add((N) fill.copy());
            }
            data.add(t);
        }
        while (rows < data.size()) {
            data.removeLast();
        }
        while (cols > data.get(0).size()) {
            for (int i = 0; i < data.size(); i++) {
                data.get(i).add((N) fill.copy());
            }
        }
        while (cols < data.get(0).size()) {
            for (int i = 0; i < data.size(); i++) {
                data.get(i).removeLast();
            }
        }
        this.cols = cols;
        this.rows = rows;
    }

    public void setColSizeInRow(int row, int size) {
        FastTable<N> rowTable = data.get(row);
        while (rowTable.size() <= size) {
            rowTable.add(null);
        }
    }

    public Vector<N> getRowVector(int index) {
        return data.get(index);
    }

    public void setRowVector(int index, Vector<N> row) {
        data.set(index, row);
    }

    public N[] getRowArray(int row) {
        FastTable<N> rowTable = data.get(row);
        int size = rowTable.size();
        Numeric[] array = new Numeric[size];
        for (int i = 0; i < size; i++) {
            array[i] = rowTable.get(i);
        }
        return (N[]) array;
    }

    public void setRowArray(int row, N[] array) {
        FastTable<N> rowTable = data.get(row);
        int size = rowTable.size();
        Numeric e;
        for (int i = 0; i < size; i++) {
            e = rowTable.get(i);
            e.set(array[i]);
        }
    }

    public N getAt(int row, int col) {
        Vector<N> v = data.get(row);
        if (v != null) {
            return v.get(col);
        }
        return null;
    }

    public void setAt(int row, int col, N elem) {
        if (fill == null) {
            fill = elem.copy();
            fill.setZero();
        }
        data.get(row).set(col, elem);
    }

    public MatrixData<N> toDirect() {
        N[][] m = (N[][]) new Numeric[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = getAt(i, j);
            }
        }
        return DefaultMatrixData.newInstance(m);
    }

    public MatrixData<N> copy() {
        VectoredMatrixData<N> _data = new VectoredMatrixData<N>();
        FastTable<Vector<N>> table = FastTable.newInstance();
        for (int i = 0; i < this.data.size(); i++) {
            table.add(this.data.get(i).copy());
        }
        return _data;
    }

    public void swapRows(int row1, int row2) {
        Vector<N> tmp = data.get(row1);
        data.set(row1, data.get(row2));
        data.set(row2, tmp);
    }

    public void swapCols(int col1, int col2) {
        for (int row = 0; row < rows; row++) {
            N tmp = data.get(row).get(col1);
            data.get(row).set(col1, data.get(row).get(col2));
            data.get(row).set(col2, tmp);
        }
    }
}
