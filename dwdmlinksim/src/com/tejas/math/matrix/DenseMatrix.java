/*
 * DefaultMatrix.java
 *
 * Created on April 11, 2006, 3:52 PM
 *
 * To change this template, choose Tools | template Manager
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;

import javolution.context.ObjectFactory;
import javolution.lang.Realtime;

/**
 *
 * @param N
 * @author Kristopher T. Beck
 */
public class DenseMatrix<N extends Numeric<N>> extends Matrix<N> implements Realtime {

    public static final ObjectFactory<DenseMatrix> FACTORY = new ObjectFactory<DenseMatrix>() {

        protected DenseMatrix create() {
            return new DenseMatrix();
        }
    };

    protected DenseMatrix() {
    }

    public int findLargestInRow(int row) {
        double largest = 0;
        int index = 0;
        for (int j = 0; j < data.getColSize(); j++) {
            double magnitude = getAt(row, j).abs();
            if (magnitude > largest) {
                largest = magnitude;
                index = j;
            }
        }
        return index;
    }

    public int findLargestInCol(int col) {
        double largest = 0;
        int index = 0;
        for (int i = 0; i < data.getRowSize(); i++) {
            double magnitude = getAt(i, col).abs();
            if (magnitude > largest) {
                largest = magnitude;
                index = i;
            }
        }
        return index;
    }
    /*
    public DenseMatrix<N> factor0() {
    DenseMatrix<N> lu = copy();
    N elem;
    N sum = lu.getAt(0, 0);
    sum.inverseEq();
    for (int i = 0; i < data.getRowSize() - 1; i++) {
    for (int j = 0; j < data.getColSize() - 1; j++) {
    elem = lu.getAt(i, j);
    for (int k = j + 1, l = i + 1; k != j && l != i; k++, l++) {
    elem.timesEq(lu.getAt(l, k));
    if (k == data.getColSize() - 1) {
    k = -1;
    }
    if (l == data.getRowSize() - 1) {
    l = -1;
    }
    }
    sum.plusEq(elem);
    }
    }
    for (int i = 0; i < data.getRowSize() - 1; i++) {
    for (int j = 0; j < data.getColSize() - 1; j++) {
    elem = lu.getAt(i, j);
    for (int k = j + 1, l = i + 1; k != j && l != i; k--, l--) {
    elem.timesEq(lu.getAt(l, k));
    if (k == 0) {
    k = data.getColSize();
    }
    if (l == 0) {
    l = data.getRowSize();
    }
    }
    sum.minusEq(elem);
    }
    }
    return lu;
    }

    public DenseMatrix<N> decompose() {
    DenseMatrix<N> lu = factor0();
    return lu;
    }
     */

    public N determinant() {
        if (factored) {
            N sum = getAt(0, 0).copy();
            for (int i = 1; i < data.getRowSize(); i++) {
                sum.timesEq(getAt(i, i));
            }
            return (Math.IEEEremainder(permutations, 2) == 0) ? sum : sum.negate();
        } else {
            return (N) factor().determinant();
        }
    }

    public boolean rowColElimination(int rowPvt, int colPvt) {
        N pvtElem = getAt(rowPvt, colPvt).copy();
        if (pvtElem.isZero()) {
            throw new SingularException(rowPvt, colPvt);
        }
        pvtElem.inverseEq();
        for (int i = rowPvt + 1; i < data.getRowSize(); i++) {
            N uElem = getAt(i, colPvt);
            uElem.timesEq(pvtElem);
            for (int j = colPvt + 1; j < data.getColSize(); j++) {
                N lElem = getAt(rowPvt, j);
                N sElem = getAt(i, j);
                sElem.plusEq(uElem.times(lElem.negate()));
            }
        }
        return true;
    }

    /* LU Decompositioning */
    public DenseMatrix<N> factor1() {
        if (!isSquare()) {
            throw new DimensionException("Matrix is not square");
        }
//        if (!ordered) {
//            return orderAndFactor(0.0, 0.0, true);
//        }
//        System.out.println(toString());
        DenseMatrix<N> lu = copy();
//        System.out.println(lu);
        if (lu.getDiag(0).isZero()) {
            throw new ZeroPivotException(0);
        }
        for (int p = 0; p < data.getRowSize(); p++) {
            lu.rowColElimination(p, p);
        }
        lu.factored = true;
        return lu;
    }

    public void solve(N[] rhs, N[] sol) {
        if (!isSquare() && rhs.length == data.getRowSize()) {
            throw new DimensionException();
        }
        //N[] sol = Arrays.copyOf(rhs, data.getRowSize());
        if (pivots != null) {
//            sol = (N[]) new Numeric[data.getRowSize()];
            for (int p = 0; p < rhs.length; p++) {
                sol[p] = rhs[pivots[p]].copy();
            }
        }/* else {
        //sol = Arrays.copyOf(rhs, data.getRowSize());
        }
         */
        for (int k = 0; k < data.getColSize(); k++) {
            for (int i = k + 1; i < data.getRowSize(); i++) {
                sol[i].minusEq(getAt(i, k).times(sol[k]));
            }
        }

        for (int i = data.getColSize() - 1; i >= 0; i--) {
            for (int j = i + 1; j < data.getRowSize(); j++) {
                sol[i].minusEq(getAt(i, j).times(sol[j]));
            }
            sol[i].timesEq(getAt(i, i).inverse());
        }
    }
/*
    public N[] solve2(N[] rhs) {
        if (!isSquare() && rhs.length == data.getRowSize()) {
            throw new DimensionException();
        }
        N[] sol = null;
        if (pivots != null) {
            sol = (N[]) new Numeric[data.getRowSize()];
            for (int p = 0; p < rhs.length; p++) {
                sol[p] = rhs[pivots[p]].copy();
            }
        } else {
            sol = Arrays.copyOf(rhs, data.getRowSize());
        }

        for (int k = 0; k < data.getColSize(); k++) {
            for (int i = k + 1; i < data.getRowSize(); i++) {
                sol[i].minusEq(getAt(i, k).times(sol[k]));
            }
        }

        for (int k = data.getColSize() - 1; k >= 0; k--) {
            sol[k].timesEq(getAt(k, k).inverse());
            for (int i = 0; i < k; i++) {
                sol[i].minusEq(getAt(i, k).times(sol[k]));
            }
        }
        return sol;
    }
    
    public N[] solve3(N[] rhs) {
    if (!isSquare() && rhs.length == data.getRowSize()) {
    throw new DimensionException();
    }
    N[] sol = null;
    if (pivots != null) {
    sol = (N[]) new Numeric[data.getRowSize()];
    for (int p = 0; p < rhs.length; p++) {
    sol[p] = rhs[pivots[p]].copy();
    }
    } else {
    sol = Arrays.copyOf(rhs, data.getRowSize());
    }

    for (int k = 0; k < data.getColSize(); k++) {
    sol[k].timesEq(getAt(k, k));//.inverse());
    for (int j = k + 1; j < data.getRowSize(); j++) {
    sol[k].minusEq(getAt(k, j).times(sol[j]));
    }
    }

    for (int i = data.getRowSize() - 1; i >= 0; i--) {
    for (int j = i + 1; j < data.getColSize(); j++) {
    sol[i].minusEq(getAt(i, j).times(sol[j]));
    }
    //sol[i].timesEq(getAt(i, i).inverse());
    }

    for (int p = 0; p < rhs.length; p++) {
    N tmp = sol[p];
    sol[p] = sol[pivots[p]];
    sol[pivots[p]] = tmp;
    }

    return sol;
    }
     */

    public DenseMatrix<N> plus(Matrix<N> matrix) {
        DenseMatrix<N> result = copy();
        for (int i = 0; i < data.getRowSize(); i++) {
            for (int j = 0; j < data.getColSize(); j++) {
                result.getAt(i, j).plusEq(matrix.getAt(i, j));
            }
        }
        return result;
    }

    public DenseMatrix<N> minus(Matrix<N> matrix) {
        DenseMatrix<N> result = copy();
        for (int i = 0; i < data.getRowSize(); i++) {
            for (int j = 0; j < data.getColSize(); j++) {
                result.getAt(i, j).minusEq(matrix.getAt(i, j));
            }
        }
        return result;
    }

    public DenseMatrix<N> times(Matrix<N> matrix) {
        int kMax = data.getColSize();
        if (kMax != matrix.data.getRowSize()) {
            throw new DimensionException("This matrix's columns must equal that matrix's data.getRowSize().");
        }
        int _rows = matrix.data.getRowSize();
        int _cols = matrix.data.getColSize();
        N elem = data.getFill().copy();
        elem.setZero();
        DenseMatrix result = newInstance(_rows, _cols, elem);
        for (int i = 0; i < _rows; i++) {
            for (int j = 0; j < _cols; j++) {
                elem = (N) result.getAt(i, j);
                for (int k = 0; k < kMax; k++) {
                    elem.plusEq(getAt(i, k).times(matrix.getAt(k, j)));
                }
            }
        }
        return result;
    }

    public DenseMatrix<N> transpose() {
        DenseMatrix<N> m = copy();
        for (int i = 0; i < data.getRowSize(); i++) {
            for (int j = 0; j < data.getColSize(); j++) {
                m.getAt(j, i).set(getAt(i, j));
            }
        }
        return m;
    }

    public static <N extends Numeric<N>> DenseMatrix<N> newInstance(int size, N fill) {
        DenseMatrix<N> m = FACTORY.object();
        m.data = DefaultMatrixData.newInstance(size, size, fill);
        m.setSize(size);
        return m;
    }

    public static <N extends Numeric<N>> DenseMatrix<N> newInstance(MatrixData<N> data) {
        DenseMatrix<N> m = FACTORY.object();
        m.data = DefaultMatrixData.newInstance(data);
        m.data.cols = data.cols;
        m.data.rows = data.rows;
        return m;
    }

    public static <N extends Numeric<N>> DenseMatrix<N> newInstance(N[][] elements) {
        DenseMatrix<N> m = FACTORY.object();
        m.data = DefaultMatrixData.newInstance(elements);
        m.setSize(elements.length, elements[0].length);
        return m;
    }

    public static <N extends Numeric<N>> DenseMatrix<N> newInstance(int rows, int cols, N fill) {
        DenseMatrix<N> m = FACTORY.object();
        m.data = DefaultMatrixData.newInstance(rows, cols, fill);
        m.setSize(rows, cols);
        return m;
    }

    public DenseMatrix<N> copy() {
        DenseMatrix<N> m = FACTORY.object();
        m.data = data.copy();
        m.data.rows = data.rows;
        m.data.cols = data.cols;
        m.permutations = permutations;
        //m.pivots = new int[pivots.length];
        //System.arraycopy(pivots, 0, m.pivots, 0, pivots.length);
//        m.relThreshold = relThreshold;
        return m;
    }

    @Override
    public DenseMatrix<N> factor() {
        return factor1();
    }

    @Override
    public N[] solve(N[] rhs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
