/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;
import com.tejas.math.vector.Vector;

import javolution.lang.Realtime;
import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class Matrix<N extends Numeric<N>> implements Realtime {

//    int rows;
//    int cols;
    boolean shouldFill = true;
    MatrixData<N> data;
    int permutations;
    boolean valid;
    boolean sorted;
//    boolean ordered;
//    boolean reordered;
    boolean factored;
    int[] pivots;
//    double relThreshold = 1.0e-3;
//    double absThreshold = 0;

    public void reset() {
        clear();
        data.reset();
    }

    public void setSize(int size) {
        data.setSize(size);
    }

    public void setSize(int rows, int cols) {
        data.setSize(rows, cols);
    }

    public boolean containsAt(int row, int col) {
        if (row >= data.getRowSize() || col >= data.getColSize() || getAt(row, col) == null) {
            return false;
        }
        return true;
    }

    public void add(int row, int col, N elem) {
        if (row >= data.getRowSize() || col >= data.getColSize()) {
            setSize(row + 1, col + 1);
        }
        setAt(row, col, elem);
    }

    public N getAt(int row, int col) {
        return data.getAt(row, col);
    }

    public void setAt(int row, int col, N elem) {
        data.setAt(row, col, elem);
    }

    public int getColSize() {
        return data.getColSize();
    }

    public int getRowSize() {
        return data.getRowSize();
    }

    public abstract int findLargestInRow(int row);

    public abstract int findLargestInCol(int col);

    public abstract Matrix<N> factor();

    public abstract N determinant();

    public abstract boolean rowColElimination(int rowPvt, int colPvt);

    public abstract N[] solve(N[] rhs);

    public abstract void solve(N[] rhs, N[] sol);

    public boolean isSquare() {
        return data.getRowSize() == data.getColSize() ? true : false;
    }

    public N getElementAt(int row, int col, boolean createIfMissing) {
        N elem = data.getAt(row, col);
        if (elem == null && createIfMissing) {
            elem = (N) data.getFill().copy();
            data.setAt(row, col, elem);
        }
        return elem;
    }

    public N getDiag(int index) {
        return data.getDiag(index);
    }

    public void swapRows(int row1, int row2) {
        data.swapRows(row1, row2);
        int tmp = pivots[row1];
        pivots[row1] = pivots[row2];
        pivots[row2] = tmp;
        permutations++;
    }

    public void swapCols(int col1, int col2) {
        data.swapCols(col1, col2);
//        reordered = true;
        permutations++;
    }

    public abstract Matrix<N> plus(Matrix<N> matrix);

    public abstract Matrix<N> minus(Matrix<N> matrix);

    public abstract Matrix<N> times(Matrix<N> matrix);

    public abstract Matrix<N> transpose();

    public void clear() {
        data.clear();
        factored = false;
//        ordered = false;
//        reordered = false;
    }

    public Vector<N> getRowVector(int index) {
        Vector<N> v = (Vector<N>) Vector.FACTORY.object();
        for (int j = 0; j < data.getColSize(); j++) {
            v.add(data.getAt(index, j));
        }
        return v;
    }

    public Vector<N> getColVector(int index) {
        Vector<N> v = (Vector<N>) Vector.FACTORY.object();
        for (int i = 0; i < data.getRowSize(); i++) {
            v.add(data.getAt(i, index));
        }
        return v;
    }

    public Vector<N> getDiagVector(int index) {
        Vector<N> v = (Vector<N>) Vector.FACTORY.object();
        int _size = Math.min(data.getRowSize(), data.getColSize());
        for (int i = 0; i < _size; i++) {
            v.add(data.getAt(i, i));
        }
        return v;
    }

    public boolean isFactored() {
        return factored;
    }

    public int getPermutations() {
        return permutations;
    }

    public int[] getPivots() {
        if (pivots == null || pivots.length != getRowSize()) {
            pivots = new int[getRowSize()];
            for (int i = 0; i < getRowSize(); i++) {
                pivots[i] = i;
            }
        }
        return pivots;
    }

    public abstract Matrix<N> copy();

    public Text toText() {
        Text text = Text.valueOf('{');
        Numeric elem;
        for (int i = 0; i < getRowSize(); i++) {
            if (i != 0) {
                text = text.concat(Text.valueOf('\n'));
            }
            text = text.concat(Text.valueOf('{'));

            for (int j = 0; j < getColSize(); j++) {
                elem = data.getAt(i, j);
                if (elem == null) {
                    text = text.concat(Text.intern("null"));
                } else {
                    text = text.concat(Text.intern(elem.toText()));
                }
                if (j < getColSize() - 1) {
                    text = text.concat(Text.intern(", "));
                }
            }
            text = text.concat(Text.valueOf('}'));
            if(i < getRowSize()-1){
                text=text.concat(Text.valueOf(','));
            }
        }
        text = text.concat(Text.valueOf('}'));
        return text;
    }

    @Override
    public String toString() {
        return toText().toString();
    }
}
