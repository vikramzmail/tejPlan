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
public class SparseMatrix<N extends Numeric<N>> extends Matrix<N>{

    @Override
    public int findLargestInRow(int row) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int findLargestInCol(int col) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> factor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public N determinant() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
/*
    @Override
    public void order() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> orderAndFactor(double relThreshold, double absThreshold, boolean diagPivoting) throws MatrixException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
*/
    @Override
    public boolean rowColElimination(int rowPvt, int colPvt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public N[] solve(N[] rhs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> plus(Matrix<N> matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> minus(Matrix<N> matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> times(Matrix<N> matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> transpose() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Matrix<N> copy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void solve(N[] rhs, N[] sol) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
