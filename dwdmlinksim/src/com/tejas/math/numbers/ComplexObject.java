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
public abstract class ComplexObject extends Complex {

    public abstract void load();

    public abstract void store();

    @Override
    public double abs() {
        load();
        return super.abs();
    }

    @Override
    public Complex acos() {
        load();
        return super.acos();
    }

    @Override
    public Complex acosReal(double a) {
        load();
        return super.acosReal(a);
    }

    @Override
    public Complex acosh() {
        load();
        return super.acosh();
    }

    @Override
    public Complex acoshReal(double a) {
        load();
        return super.acoshReal(a);
    }

    @Override
    public Complex acot() {
        load();
        return super.acot();
    }

    @Override
    public Complex acoth() {
        load();
        return super.acoth();
    }

    @Override
    public Complex acsc() {
        load();
        return super.acsc();
    }

    @Override
    public Complex acscReal(double a) {
        load();
        return super.acscReal(a);
    }

    @Override
    public Complex acsch() {
        load();
        return super.acsch();
    }

    @Override
    public double arg() {
        load();
        return super.arg();
    }

    @Override
    public Complex asec() {
        load();
        return super.asec();
    }

    @Override
    public Complex asecReal(double a) {
        load();
        return super.asecReal(a);
    }

    @Override
    public Complex asech() {
        load();
        return super.asech();
    }

    @Override
    public Complex asin() {
        load();
        return super.asin();
    }

    @Override
    public Complex asinReal(double a) {
        load();
        return super.asinReal(a);
    }

    @Override
    public Complex asinh() {
        load();
        return super.asinh();
    }

    @Override
    public Complex atan() {
        load();
        return super.atan();
    }

    @Override
    public Complex atanh() {
        load();
        return super.atanh();
    }

    @Override
    public Complex atanhReal(double a) {
        load();
        return super.atanhReal(a);
    }

    @Override
    public Complex conjNegate() {
        load();
        return super.conjNegate();
    }

    @Override
    public Complex conjugate() {
        load();
        return super.conjugate();
    }

    @Override
    public Complex copy() {
        load();
        return super.copy();
    }

    @Override
    public Complex cos() {
        load();
        return super.cos();
    }

    @Override
    public Complex cosh() {
        load();
        return super.cosh();
    }

    @Override
    public Complex cot() {
        load();
        return super.cot();
    }

    @Override
    public Complex coth() {
        load();
        return super.coth();
    }

    @Override
    public Complex csc() {
        load();
        return super.csc();
    }

    @Override
    public Complex csch() {
        load();
        return super.csch();
    }

    @Override
    public Complex divide(Real r) {
        load();
        return super.divide(r);
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

    @Override
    public void divideEq(Complex c) {
        load();
        super.divideEq(c);
        store();
    }

    @Override
    public Complex exp() {
        load();
        return super.exp();
    }

    @Override
    public double getImag() {
        load();
        return super.getImag();
    }

    @Override
    public double getReal() {
        load();
        return super.getReal();
    }

    @Override
    public void imagDivideEq(double d) {
        load();
        super.imagDivideEq(d);
        store();
    }

    @Override
    public void imagDivideEq(Real r) {
        load();
        super.imagDivideEq(r);
        store();
    }

    @Override
    public void imagMinusEq(double d) {
        load();
        super.imagMinusEq(d);
        store();
    }

    @Override
    public void imagMinusEq(Real r) {
        load();
        super.imagMinusEq(r);
        store();
    }

    @Override
    public void imagPlusEq(double d) {
        load();
        super.imagPlusEq(d);
        store();
    }

    @Override
    public void imagPlusEq(Real r) {
        load();
        super.imagPlusEq(r);
        store();
    }

    @Override
    public void imagTimesEq(double d) {
        load();
        super.imagTimesEq(d);
        store();
    }

    @Override
    public void imagTimesEq(Real r) {
        load();
        super.imagTimesEq(r);
        store();
    }

    @Override
    public Complex inverse() {
        load();
        return super.inverse();
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
    public Complex log() {
        load();
        return super.log();
    }

    @Override
    public Complex log10() {
        load();
        return super.log10();
    }

    @Override
    public double logabs() {
        load();
        return super.logabs();
    }

    @Override
    public Complex logb(Complex b) {
        load();
        return super.logb(b);
    }

    @Override
    public Complex minus(Real r) {
        load();
        return super.minus(r);
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

    @Override
    public void minusEq(Complex c) {
        load();
        super.minusEq(c);
        store();
    }

    @Override
    public void negateEq() {
        load();
        super.negateEq();
        store();
    }

    @Override
    public double norm() {
        load();
        return super.norm();
    }

    @Override
    public double norm1() {
        load();
        return super.norm1();
    }

    @Override
    public Complex plus(double real, double imag) {
        load();
        return super.plus(real, imag);
    }

    @Override
    public Complex plus(Real r) {
        load();
        return super.plus(r);
    }

    @Override
    public void plusEq(double x, double y) {
        load();
        super.plusEq(x, y);
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

    @Override
    public void plusEq(Complex c) {
        load();
        super.plusEq(c);
        store();
    }

    @Override
    public Complex pow(Complex exp) {
        load();
        return super.pow(exp);
    }

    @Override
    public Complex pow(double exp) {
        load();
        return super.pow(exp);
    }

    @Override
    public Complex pow(Real exp) {
        load();
        return super.pow(exp);
    }

    @Override
    public void powEq(double exp) {
        load();
        super.powEq(exp);
        store();
    }

    @Override
    public void realDivideEq(double d) {
        load();
        super.realDivideEq(d);
        store();
    }

    @Override
    public void realDivideEq(Real r) {
        load();
        super.realDivideEq(r);
        store();
    }

    @Override
    public void realMinusEq(double d) {
        load();
        super.realMinusEq(d);
        store();
    }

    @Override
    public void realMinusEq(Real r) {
        load();
        super.realMinusEq(r);
        store();
    }

    @Override
    public void realPlusEq(double d) {
        load();
        super.realPlusEq(d);
        store();
    }

    @Override
    public void realPlusEq(Real r) {
        load();
        super.realPlusEq(r);
        store();
    }

    @Override
    public void realTimesEq(double d) {
        load();
        super.realTimesEq(d);
        store();
    }

    @Override
    public void realTimesEq(Real r) {
        load();
        super.realTimesEq(r);
        store();
    }

    @Override
    public void reset() {
        super.reset();
        store();
    }

    @Override
    public Complex sec() {
        load();
        return super.sec();
    }

    @Override
    public Complex sech() {
        load();
        return super.sech();
    }

    @Override
    public void set(Real r) {
        super.set(r);
        store();
    }

    @Override
    public void set(Complex c) {
        super.set(c);
        store();
    }

    @Override
    public void set(double real, double imag) {
        super.set(real, imag);
        store();
    }

    @Override
    public void setImag(double imag) {
        load();
        super.setImag(imag);
        store();
    }

    @Override
    public void setImag(Real imag) {
        load();
        super.setImag(imag);
        store();
    }

    @Override
    public void setOne() {
        super.setOne();
        store();
    }

    @Override
    public void setReal(double real) {
        load();
        super.setReal(real);
        store();
    }

    @Override
    public void setReal(Real real) {
        load();
        super.setReal(real);
        store();
    }

    @Override
    public void setZero() {
        super.setZero();
        store();
    }

    @Override
    public Complex sin() {
        load();
        return super.sin();
    }

    @Override
    public Complex sinh() {
        load();
        return super.sinh();
    }

    @Override
    public Complex sqrt() {
        load();
        return super.sqrt();
    }

    @Override
    public Complex sqrtReal(double x) {
        load();
        return super.sqrtReal(x);
    }

    @Override
    public Complex tan() {
        load();
        return super.tan();
    }

    @Override
    public Complex tanh() {
        load();
        return super.tanh();
    }

    @Override
    public Complex times(Real r) {
        load();
        return super.times(r);
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

    @Override
    public void timesEq(Complex c) {
        load();
        super.timesEq(c);
        store();
    }

    @Override
    public String toString() {
        load();
        return super.toString();
    }

    @Override
    public Text toText() {
        load();
        return super.toText();
    }

    @Override
    public Complex valueOfPolar(double r, double theta) {
        load();
        return super.valueOfPolar(r, theta);
    }
}
