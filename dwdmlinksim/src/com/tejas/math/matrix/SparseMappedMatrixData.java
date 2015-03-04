/*
 * SparseMappedMatrixData.java
 * 
 * Created on May 31, 2007, 9:34:31 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;

import javolution.util.FastCollection;
import javolution.util.FastCollection.Record;
import javolution.util.FastComparator;
import javolution.util.FastMap;
import javolution.util.FastTable;

/**
 *
 * @author Kristopher T. Beck
 */
public class SparseMappedMatrixData<N extends Numeric<N>> extends SparseMatrixData<N> {

    FastMap<Integer, N> map = FastMap.newInstance();
    FastTable<Integer> firstInRow = FastTable.newInstance();
    FastTable<Integer> elemsInRow = FastTable.newInstance();

    public SparseMappedMatrixData() {
        map.setKeyComparator(FastComparator.DIRECT);
    }

    public void clear() {
        FastCollection<N> c = (FastCollection<N>) map.values();
        for (Record r = c.head(), end = c.tail();
                (r = r.getNext()) != end;) {
            c.valueOf(r).setZero();
        }
        firstInRow.clear();
        elemsInRow.clear();
    }

    public void reset() {
        map.reset();
        firstInRow.reset();
        elemsInRow.reset();
    }

    public void setAt(int row, int col, N elem) {
        if (row > 32767 || col > 32767) {
            throw new IndexOutOfBoundsException();
        }
        int index = row << 16 + col;
        if (!map.containsKey(index)) {
            add(row, col, elem);
        } else {
            map.get(index).set(elem);
        }
    }

    public void setRowSize(int size) {
        if (size > 32767) {
            throw new IndexOutOfBoundsException();
        }
        if (size > rows) {
            for (int i = firstInRow.size(); i < size; i++) {
                firstInRow.add(i, 0);
            }
            for (int i = elemsInRow.size(); i < size; i++) {
                elemsInRow.add(i, 0);
            }
        }
        rows = size;
    }

    public void setColSize(int size) {
        if (size > 32767) {
            throw new IndexOutOfBoundsException();
        }
        cols = size;
    }

    public N getAt(int row, int col) {
        int index = getIndex(row, col);
        return map.get(index);
    }
    /*
    public N getDiag(int index) {
    if (index > 32767) {
    throw new IndexOutOfBoundsException();
    }
    int i = index << 16 + index;
    N n;
    if ((n = map.get(i)) == null) {
    n = (N) fill.copy();
    }
    map.put(i, n);
    return n;
    }
     */

    public DefaultMatrixData toDirect() {
        N[][] m = (N[][]) new Numeric[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                N n = map.get(i << 16 + j);
                if (n == null) {
                    m[i][j] = (N) fill.copy();
                } else {
                    m[i][j] = n;
                }
            }
        }
        return DefaultMatrixData.newInstance(m);
    }

    public void setSize(int rows, int cols) {
        if (rows > 32767 || cols > 32767) {
            throw new IndexOutOfBoundsException();
        }
        this.rows = rows;
        this.cols = cols;
    }

    public void setSize(int size) {
        setSize(size, size);
    }

    /*
    public static <N extends Numeric> SparseMappedMatrixData<N> newInstance(){
    SparseMappedMatrixData<N> data = new SparseMappedMatrixData<N>();
    }*/
    public MatrixData<N> copy() {
        SparseMappedMatrixData<N> data = new SparseMappedMatrixData<N>();
        FastMap<Integer, N> _map = data.map;
        for (FastMap.Entry<Integer, N> e = map.head(), end = map.tail(); (e = e.getNext()) != end;) {
            _map.put(e.getKey(), (N) e.getValue().copy());
        }
        return data;
    }

    @Override
    public void swapRows(int row1, int row2) {
        FastTable<N> tmp = FastTable.newInstance();
        int index;
        for (int j = firstInRow.get(row1); j < elemsInRow.get(row1); j++) {
            index = getIndex(row1, j);
            tmp.add(map.get(index));
            map.remove(index);
        }
        for (int j = firstInRow.get(row2); j < elemsInRow.get(row2); j++) {
            index = getIndex(row2, j);
            map.put(getIndex(row1, j), map.get(index));
            map.remove(index);
        }
        for (int j = firstInRow.get(row1); j < elemsInRow.get(row1); j++) {
            map.put(getIndex(row2, j), tmp.get(j));
        }
        int i = firstInRow.get(row1);
        firstInRow.set(row1, firstInRow.get(row2));
        firstInRow.set(row2, i);
        i = elemsInRow.get(row1);
        elemsInRow.set(row1, elemsInRow.get(row2));
        elemsInRow.set(row2, i);
    }

    @Override
    public void swapCols(int col1, int col2) {
        FastTable<N> tmp = FastTable.newInstance();
        int index;
        for (int i = 0; i < rows; i++) {
            index = getIndex(i, col1);
            tmp.add(map.get(index));
            map.remove(index);
        }
        for (int i = 0; i < rows; i++) {
            index = getIndex(i, col2);
            map.put(getIndex(i, col1), map.get(index));
            map.remove(index);
        }
        for (int i = 0; i < rows; i++) {
            map.put(getIndex(i, col2), tmp.get(i));
        }
    }

    @Override
    public void add(int row, int col, N elem) {
        int index = getIndex(row, col);
        if (row >= rows) {
            setRowSize(row + 1);
        }
        if (col >= cols) {
            setColSize(col + 1);
        }
        int i = firstInRow.get(row);
        if (index > i) {
            firstInRow.set(row, index);
        }
        i = elemsInRow.get(row);
        elemsInRow.set(row, i + 1);
        map.put(index, elem);
        if (fill == null) {
            fill = elem.copy();
            fill.setZero();
        }
        /*
        int fir = firstInRow.get(row);
        int lir = fir + elemsInRow.get(row) - 1;
        if (col < fir) {
        for (int j = col + 1; j < fir; j++) {
        map.put(getIndex(row, j), fill.copy());
        }
        firstInRow.set(row, col);
        } else if (col > lir) {
        for (int j = lir + 1; j < col; j++) {
        map.put(getIndex(row, j), fill.copy());
        }
        }
         */
    }

    @Override
    public void remove(int row, int col) {
        map.remove(getIndex(row, col));
        if (firstInRow.get(row) == col) {
            firstInRow.set(row, col - 1);
        }
    }

    protected int getIndex(int row, int col) {
        if (row > 32767 || col > 32767) {
            throw new IndexOutOfBoundsException();
        }
        return row << 16 + col;
    }
}
