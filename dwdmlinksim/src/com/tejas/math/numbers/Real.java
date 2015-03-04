/*
 * Real.java
 *
 * Created on April 9, 2006, 1:14 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.math.numbers;

import com.tejas.math.Math2;

import javolution.context.ObjectFactory;
import javolution.lang.MathLib;
import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Real extends Numeric<Real> implements Trigonometric<Real>, Hyperbolic<Real>, InverseTrigonometric<Real>, InverseHyperbolic<Real> {

    protected double real;
    public static final ObjectFactory FACTORY = new ObjectFactory() {

        protected Object create() {
            return new Real();
        }
    };

    /**
     * Creates a new instance of Real
     */
    protected Real() {
    }

    public void set(double real) {
        this.real = real;
    }

    public void set(Real r) {
        real = r.get();
    }

    public static Real valueOf(double d) {
        Real r = (Real) FACTORY.object();
        r.real = d;
        return r;
    }

    public static Real zero() {
        return valueOf(0);
    }

    public static Real one() {
        return valueOf(1);
    }

    public Real copy() {
        Real r = (Real) FACTORY.object();
        r.real = real;
        return r;
    }

    public void reset() {
        real = 0;
    }

    public double get() {
        return real;
    }

    public void plusEq(double d) {
        real += d;
    }

    public void plusEq(Real r) {
        real += r.get();
    }
    /*
    public void plusEq(Complex c) {
    real += c.getReal();
    }
     */

    public void minusEq(double d) {
        real -= d;
    }

    public void minusEq(Real r) {
        real -= r.get();
    }
    /*
    public void minusEq(Complex c) {
    real -= c.getReal();
    }
     */

    public void timesEq(double d) {
        real *= d;
    }

    public void timesEq(Real r) {
        real *= r.get();
    }
/*
    public void timesEq(Complex c) {
        real = (real * c.getReal()) - (real * c.getImag());
    }
*/
    public void divideEq(double d) {
        real /= d;
    }

    public void divideEq(Real r) {
        real /= r.get();
    }
/*
    public void divideEq(Complex c) {
        double s = c.real * c.real + c.imag * c.imag;
        double t = real * c.real;
        real = t / s;
    }
*/
    public void inverseEq() {
        real = 1 / real;
    }

    public boolean isOne() {
        return real == 1;
    }

    public boolean isZero() {
        return real == 0;
    }

    public void setOne() {
        real = 1;
    }

    public void setZero() {
        real = 0;
    }

    public void negateEq() {
        real = -real;
    }

    public Text toText() {
        return Text.valueOf(real);
    }

    @Override
    public String toString() {
        return String.valueOf(real);
    }

    public Real sin() {
        return valueOf(MathLib.sin(real));
    }

    public Real cos() {
        return valueOf(MathLib.cos(real));
    }

    public Real tan() {
        return valueOf(MathLib.tan(real));
    }

    public Real acos() {
        return valueOf(MathLib.acos(real));
    }

    public Real asin() {
        return valueOf(MathLib.asin(real));
    }

    public Real atan() {
        return valueOf(MathLib.atan(real));
    }

    public Real cosh() {
        return valueOf(MathLib.cosh(real));
    }

    public Real sinh() {
        return valueOf(MathLib.sinh(real));
    }

    public Real tanh() {
        return valueOf(MathLib.tanh(real));
    }

    @Override
    public Real exp() {
        return valueOf(MathLib.exp(real));
    }

    @Override
    public Real log() {
        return valueOf(MathLib.log(real));
    }

    @Override
    public Real log10() {
        return valueOf(MathLib.log(real));
    }

    public Real sec() {
        return valueOf(Math2.sec(real));
    }

    public Real csc() {
        return valueOf(Math2.csc(real));
    }

    public Real cot() {
        return valueOf(Math2.cot(real));
    }

    public Real sech() {
        return valueOf(Math2.sech(real));
    }

    public Real csch() {
        return valueOf(Math2.csch(real));
    }

    public Real coth() {
        return valueOf(Math2.coth(real));
    }

    public Real asec() {
        return valueOf(Math2.asec(real));
    }

    public Real acsc() {
        return valueOf(Math2.asec(real));
    }

    public Real acot() {
        return valueOf(Math2.acot(real));
    }

    public Real asinh() {
        return valueOf(Math2.asinh(real));
    }

    public Real acosh() {
        return valueOf(Math2.acosh(real));
    }

    public Real atanh() {
        return valueOf(Math2.atanh(real));
    }

    public Real asech() {
        return valueOf(Math2.asech(real));
    }

    public Real acsch() {
        return valueOf(Math2.acsch(real));
    }

    public Real acoth() {
        return valueOf(Math2.acoth(real));
    }

    @Override
    public Real pow(double exp) {
        return valueOf(MathLib.pow(real, exp));
    }

    @Override
    public Real pow(Real exp) {
        return valueOf(MathLib.pow(real, exp.real));
    }

    @Override
    public Real sqrt() {
        return valueOf(MathLib.sqrt(real));
    }

    @Override
    public double abs() {
        return MathLib.abs(real);
    }
}
