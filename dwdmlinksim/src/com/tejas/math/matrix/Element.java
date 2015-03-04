/*
 * DefaultElement.java
 *
 * Created on May 26, 2006, 10:00 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.math.matrix;

import com.tejas.math.numbers.Numeric;

import javolution.context.ObjectFactory;
import javolution.lang.Realtime;
import javolution.text.Text;
/**
 *
 * @param N 
 * @author Kristopher T. Beck
 */

public class Element<N extends Numeric<N>> extends Numeric<N> implements Realtime {

    public static final ObjectFactory<Element> FACTORY = new ObjectFactory<Element>() {

                protected Element create() {
                    return new Element();
                }
            };
    /**
     * Creates a new instance of DefaultElement
     */

    public Element() {
    }
    public int row = 0;
    public int col = 0;
    N value = null;
    DenseMatrix matrix;

    public N getValue() {
        return value;
    }

    public void setValue(N value) {
        this.value = value;
    }

    public void reset() {
        value.reset();
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getCol() {
        return col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getRow() {
        return row;
    }

    public boolean isEmpty() {
        if (value == null) {
            return true;
        }
        return false;
    }

    public static <N extends Numeric<N>> Element valueOf(int row, int col,
            DenseMatrix<N> matrix) {
        Element<N> e = (Element<N>) FACTORY.object();
        e.row = row;
        e.col = col;
        e.matrix = matrix;
        e.value = matrix.getAt(row, col);
        return e;
    }
    private Element<N> next = null;
    private Element<N> prev = null;
    private Element<N> up = null;
    private Element<N> down = null;

    public Element<N> getDown() {
        if (down == null && row + 1 < matrix.getRowSize()) {
            down = Element.valueOf(row + 1, col, matrix);
            down.up = this;
        }
        return down;
    }

    public Element<N> getUp() {
        if (up == null && row - 1 >= 0) {
            up = Element.valueOf(row - 1, col, matrix);
            up.down = this;
        }
        return up;
    }

    public Element<N> getPrev() {
        if (prev == null && col + 1 >= 0) {
            prev = Element.valueOf(row, col - 1, matrix);
            prev.next = this;
        }
        return prev;
    }

    public Element<N> getNext() {
        if (next == null && col + 1 <= matrix.getColSize()) {
            next = Element.valueOf(row, col + 1, matrix);
            next.prev = this;
        }
        return down;
    }

    public void assign(N n) {
        value.set(n);
    }

    public void timesEq(double value) {
        this.value.timesEq(value);
    }

    public void timesEq(N value) {
        this.value.timesEq(value);
    }
/*
    public void sqrtEq() {
        value.sqrtEq();
    }
*/
    public void setZero() {
        value.setZero();
    }

    public void setOne() {
        value.setOne();
    }
/*
    public void powEq(double exp) {
        value.powEq(exp);
    }

    public void powEq(N exp) {
        value.powEq(exp);
    }
*/
    public void plusEq(double value) {
        this.value.plusEq(value);
    }

    public void plusEq(N value) {
        this.value.plusEq(value);
    }

    public void negateEq() {
        value.negateEq();
    }

    public void minusEq(double value) {
        this.value.minusEq(value);
    }

    public void minusEq(N value) {
        this.value.minusEq(value);
    }

    public boolean isZero() {
        return value.isZero();
    }

    public boolean isOne() {
        return value.isZero();
    }

    public void inverseEq() {
        value.inverseEq();
    }

    public void divideEq(double value) {
        this.value.divideEq(value);
    }

    public void divideEq(N value) {
        this.value.divideEq(value);
    }

    @Override
    public N divide(N value) {
        return (N) this.value.divide(value);
    }

    @Override
    public N divide(double value) {
        return (N) this.value.divide(value);
    }

    @Override
    public N minus(N value) {
        return (N) this.value.minus(value);
    }

    @Override
    public N minus(double value) {
        return (N) this.value.minus(value);
    }

    @Override
    public N times(double value) {
        return (N) this.value.times(value);
    }

    @Override
    public N times(N value) {
        return (N) this.value.times(value);
    }

    @Override
    public N plus(double value) {
        return (N) this.value.plus(value);
    }

    @Override
    public N plus(N value) {
        return (N) this.value.plus(value);
    }

    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public N copy() {
       return value.copy();
    }

    public void set(N n) {
        value.set(n);
    }

    @Override
    public N exp() {
        return value.exp();
    }

    @Override
    public N log() {
        return value.log();
    }

    @Override
    public N log10() {
        return value.log10();
    }

    @Override
    public N pow(double exp) {
        return value.pow(exp);
    }

    @Override
    public N sqrt() {
        return value.sqrt();
    }

    @Override
    public N pow(N exp) {
        return value.pow(exp);
    }

    @Override
    public double abs() {
        return value.abs();
    }
}
