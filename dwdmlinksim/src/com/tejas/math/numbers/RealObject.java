/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.math.numbers;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class RealObject extends Real{

    public abstract void load();

    public abstract void store();

    @Override
    public double abs() {
        load();
        return super.abs();
    }

    @Override
    public Real acos() {
        load();
        return super.acos();
    }

    @Override
    public Real acosh() {
        load();
        return super.acosh();
    }

    @Override
    public Real acot() {
        load();
        return super.acot();
    }

    @Override
    public Real acoth() {
        load();
        return super.acoth();
    }

    @Override
    public Real acsc() {
        load();
        return super.acsc();
    }

    @Override
    public Real acsch() {
        load();
        return super.acsch();
    }

    @Override
    public Real asec() {
        load();
        return super.asec();
    }

    @Override
    public Real asech() {
        load();
        return super.asech();
    }

    @Override
    public Real asin() {
        load();
        return super.asin();
    }

    @Override
    public Real asinh() {
        load();
        return super.asinh();
    }

    @Override
    public Real atan() {
        load();
        return super.atan();
    }

    @Override
    public Real atanh() {
        load();
        return super.atanh();
    }

    @Override
    public Real copy() {
        load();
        return super.copy();
    }

    @Override
    public Real cos() {
        load();
        return super.cos();
    }

    @Override
    public Real cosh() {
        load();
        return super.cosh();
    }

    @Override
    public Real cot() {
        load();
        return super.cot();
    }

    @Override
    public Real coth() {
        load();
        return super.coth();
    }

    @Override
    public Real csc() {
        load();
        return super.csc();
    }

    @Override
    public Real csch() {
        load();
        return super.csch();
    }

    @Override
    public void divideEq(double d) {
        load();
        super.divideEq(d);
        store();
    }

    @Override
    public void divideEq(Real r) {
        load();
        super.divideEq(r);
        store();
    }
/*
    @Override
    public void divideEq(Complex c) {
        load();
        super.divideEq(c);
        store();
    }
*/
    @Override
    public Real exp() {
        load();
        return super.exp();
    }

    @Override
    public double get() {
        load();
        return super.get();
    }

    @Override
    public void inverseEq() {
        load();
        super.inverseEq();
        store();
    }

    @Override
    public boolean isOne() {
        load();
        return super.isOne();
    }

    @Override
    public boolean isZero() {
        load();
        return super.isZero();
    }

    @Override
    public Real log() {
        load();
        return super.log();
    }

    @Override
    public Real log10() {
        load();
        return super.log10();
    }

    @Override
    public void minusEq(double d) {
        load();
        super.minusEq(d);
        store();
    }

    @Override
    public void minusEq(Real r) {
        load();
        super.minusEq(r);
        store();
    }
/*
    @Override
    public void minusEq(Complex c) {
        load();
        super.minusEq(c);
        store();
    }
*/
    @Override
    public void negateEq() {
        load();
        super.negateEq();
        store();
    }

    @Override
    public void plusEq(double d) {
        load();
        super.plusEq(d);
        store();
    }

    @Override
    public void plusEq(Real r) {
        load();
        super.plusEq(r);
        store();
    }
/*
    @Override
    public void plusEq(Complex c) {
        load();
        super.plusEq(c);
        store();
    }
*/
    @Override
    public Real pow(double exp) {
        load();
        return super.pow(exp);
    }

    @Override
    public Real pow(Real exp) {
        load();
        return super.pow(exp);
    }

    @Override
    public void reset() {
        load();
        super.reset();
        store();
    }

    @Override
    public Real sec() {
        load();
        return super.sec();
    }

    @Override
    public Real sech() {
        load();
        return super.sech();
    }

    @Override
    public void set(double real) {
        super.set(real);
        store();
    }

    @Override
    public void set(Real r) {
        super.set(r);
        store();
    }

    @Override
    public void setOne() {
        super.setOne();
        store();
    }

    @Override
    public void setZero() {
        super.setZero();
        store();
    }

    @Override
    public Real sin() {
        load();
        return super.sin();
    }

    @Override
    public Real sinh() {
        load();
        return super.sinh();
    }

    @Override
    public Real sqrt() {
        load();
        return super.sqrt();
    }

    @Override
    public Real tan() {
        load();
        return super.tan();
    }

    @Override
    public Real tanh() {
        load();
        return super.tanh();
    }

    @Override
    public void timesEq(double d) {
        load();
        super.timesEq(d);
        store();
    }

    @Override
    public void timesEq(Real r) {
        load();
        super.timesEq(r);
        store();
    }
/*
    @Override
    public void timesEq(Complex c) {
        load();
        super.timesEq(c);
        store();
    }
*/
    @Override
    public Text toText() {
        load();
        return super.toText();
    }

    @Override
    public String toString() {
        load();
        return super.toString();
    }


}
