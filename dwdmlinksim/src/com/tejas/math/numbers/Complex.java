/*
 * DefaultComplex.java
 *
 * Created on April 8, 2006, 11:39 PM
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
 * Many functions converted from gsl sources.
 * @author Kristopher T. Beck
 */
public class Complex extends Numeric<Complex> implements Trigonometric<Complex>, Field<Complex> {

    protected double real;
    protected double imag;
    protected static final ObjectFactory<Complex> FACTORY = new ObjectFactory<Complex>() {

        protected Complex create() {
            return new Complex();
        }
    };

    /**
     * Creates a new instance of DefaultComplex
     */
    protected Complex() {
    }

    public static Complex zero() {
        Complex c = FACTORY.object();
        c.setZero();
        return c;
    }

    public static Complex one() {
        Complex c = FACTORY.object();
        c.setOne();
        return c;
    }

    public static Complex valueOf(double real, double imag) {
        Complex c = FACTORY.object();
        c.real = real;
        c.imag = imag;
        return c;
    }

    public Complex valueOfPolar(double r, double theta) {
        return Complex.valueOf(r * MathLib.cos(theta), r * MathLib.sin(theta));
    }

    public Complex copy() {
        Complex c = FACTORY.object();
        c.real = real;
        c.imag = imag;
        return c;
    }

    public void reset() {
        real = 0;
        imag = 0;
    }

    public double getReal() {
        return real;
    }

    public double getImag() {
        return imag;
    }

    public void setZero() {
        real = 0;
        imag = 0;
    }

    public void setOne() {
        real = 1;
        imag = 0;
    }

    public void set(Real r) {
        real = r.get();
        imag = 0;
    }

    public void set(Complex c) {
        real = c.getReal();
        imag = c.getImag();
    }

    public void set(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    public void setImag(double imag) {
        this.imag = imag;
    }

    public void setImag(Real imag) {
        this.imag = imag.get();
    }

    public void setReal(double real) {
        this.real = real;
    }

    public void setReal(Real real) {
        this.real = real.get();
    }

    public boolean isZero() {
        return (real == 0 && imag == 0) ? true : false;
    }

    public boolean isOne() {
        return (real == 1 && imag == 0) ? true : false;
    }

    public Complex plus(double real, double imag) {
        Complex c = copy();
        c.plusEq(real, imag);
        return c;
    }

    public void plusEq(double x, double y) {
        real += x;
        imag += y;
    }

    public Complex plus(Real r) {
        return valueOf(real + r.get(), imag);
    }

    public void plusEq(double d) {
        real += d;
    }

    public void plusEq(Real r) {
        real += r.get();
    }

    public void plusEq(Complex c) {
        real += c.real;
        imag += c.imag;
    }

    public void realPlusEq(double d) {
        real += d;
    }

    public void realPlusEq(Real r) {
        real += r.get();
    }

    public Complex minus(Real r) {
        Complex c = copy();
        c.minusEq(r);
        return c;
    }

    public void realMinusEq(double d) {
        real -= d;
    }

    public void realMinusEq(Real r) {
        real -= r.get();
    }

    public void realTimesEq(double d) {
        real *= d;
    }

    public void realTimesEq(Real r) {
        real *= r.get();
    }

    public void realDivideEq(double d) {
        real /= d;
    }

    public void realDivideEq(Real r) {
        real /= r.get();
    }

    public void imagPlusEq(double d) {
        imag += d;
    }

    public void imagPlusEq(Real r) {
        imag += r.get();
    }

    public void imagMinusEq(double d) {
        imag -= d;
    }

    public void imagMinusEq(Real r) {
        imag -= r.get();
    }

    public void imagTimesEq(double d) {
        imag *= d;
    }

    public void imagTimesEq(Real r) {
        imag *= r.get();
    }

    public void imagDivideEq(double d) {
        imag /= d;
    }

    public void imagDivideEq(Real r) {
        imag /= r.get();
    }

    public void minusEq(double d) {
        real -= d;
    }

    public void minusEq(Real r) {
        real -= r.get();
    }

    public void minusEq(Complex c) {
        real -= c.real;
        imag -= c.imag;
    }

    public Complex times(Real r) {
        return valueOf(real * r.get(), imag * r.get());
    }

    public void timesEq(double d) {
        real *= d;
        imag *= d;
    }

    public void timesEq(Real r) {
        real *= r.get();
        imag *= r.get();
    }

    public void timesEq(Complex c) {
        double x = real * c.getReal() - imag * c.getImag();
        double y = real * c.getImag() + imag * c.getReal();
        real = x;
        imag = y;
    }

    public Complex divide(Real r) {
        Complex c = copy();
        c.divideEq(r.get());
        return c;
    }

    public void divideEq(double d) {
        if (d == 0) {
            throw new ArithmeticException("Can't divide by 0");
        }
        real /= d;
        imag /= d;
    }

    public void divideEq(Real r) {
        divideEq(r.get());
    }

    public void divideEq(Complex c) {
        double t = c.real * c.real + c.imag * c.imag;
        double r = c.real / t;
        double s = -c.imag / t;
        double x = real * r - imag * s;
        double y = real * s + imag * r;
        real = x;
        imag = y;
    }

    double mag() {
        return MathLib.sqrt(real * real + imag * imag);
    }

    public double arg() {
        if (real == 0.0 && imag == 0.0) {
            return 0;
        }
        return MathLib.atan2(imag, real);
    }

    public double abs() {
        return mag();
        //return Math2.hypot(real, imag);
    }

    public double logabs() {
        double x = MathLib.abs(real);
        double y = MathLib.abs(imag);
        double max, u;

        if (x >= y) {
            max = x;
            u = y / x;
        } else {
            max = y;
            u = x / y;
        }

        return MathLib.log(max) + 0.5 * Math2.log1p(u * u);
    }

    public Complex conjugate() {
        return Complex.valueOf(real, -imag);
    }

    @Override
    public Complex inverse() {
        double s = 1.0 / abs();
        return Complex.valueOf((real * s) * s, -(imag * s) * s);
    }

    public void inverseEq() {
        double tmp = (real * real)
                + (imag * imag);
        real = real / tmp;
        imag = -imag / tmp;
    }

    public Complex conjNegate() {
        return Complex.valueOf(-real, imag);
    }

    public double norm1() {
        return MathLib.abs(real) + MathLib.abs(imag);
    }

    public double norm() {
        return MathLib.max(MathLib.abs(real),
                MathLib.abs(imag));
    }

    public void sqrtEq() {
        if (imag == 0) {
            if (real < 0.0) {
                imag = MathLib.sqrt(-real);
                real = 0;
            } else {
                real = MathLib.sqrt(real);
                imag = 0;
            }
        } else {
            double mag = MathLib.sqrt(real * real
                    + imag * imag);
            double a = (mag - real) / 2.0;
            if (a <= 0.0) {
                real = MathLib.sqrt(mag);
                imag = imag / (2.0 * real);
            } else {
                a = MathLib.sqrt(a);
                real = imag / (2.0 * a);
                imag = a;
            }
        }
    }

    @Override
    public Complex sqrt() {
        Complex z = Complex.zero();

        if (real != 0.0 && imag != 0.0) {
            double x = MathLib.abs(real);
            double y = MathLib.abs(imag);
            double w;

            if (x >= y) {
                double t = y / x;
                w = MathLib.sqrt(x) * MathLib.sqrt(0.5 * (1.0 + MathLib.sqrt(1.0 + t * t)));
            } else {
                double t = x / y;
                w = MathLib.sqrt(y) * MathLib.sqrt(0.5 * (t + MathLib.sqrt(1.0 + t * t)));
            }

            if (real >= 0.0) {
                double ai = imag;
                z.set(w, ai / (2.0 * w));
            } else {
                double ai = imag;
                double vi = (ai >= 0) ? w : -w;
                z.set(ai / (2.0 * vi), vi);
            }
        }
        return z;
    }

    public Complex sqrtReal(double x) {
        if (x >= 0) {
            return Complex.valueOf(MathLib.sqrt(x), 0.0);
        } else {
            return Complex.valueOf(0.0, MathLib.sqrt(-x));
        }
    }

    public Complex exp() {
        double rho = MathLib.exp(real);
        return Complex.valueOf(rho * MathLib.cos(imag),
                rho * MathLib.sin(imag));
    }

    @Override
    public Complex pow(Complex exp) {
        if (real == 0 && imag == 0.0) {
            if (exp.real == 0 && exp.imag == 0.0) {
                return Complex.valueOf(1.0, 0.0);
            } else {
                return Complex.valueOf(0.0, 0.0);
            }
        } else if (exp.real == 1.0 && exp.imag == 0.0) {
            return copy();
        } else if (exp.real == -1.0 && exp.imag == 0.0) {
            return inverse();
        } else {
            double logr = logabs();
            double theta = arg();
            double rho = MathLib.exp(logr * exp.real - exp.imag * theta);
            double beta = theta * exp.real + exp.imag * logr;
            return Complex.valueOf(rho * MathLib.cos(beta),
                    rho * MathLib.sin(beta));
        }
    }

    public void powEq(double exp) {
        double m = MathLib.pow(mag(), exp);
        double p = exp * arg();
        real = m * MathLib.cos(p);
        imag = m * MathLib.sin(p);
    }

    /**
     * Converted from gsl sources.
     **/
    public Complex pow(double exp) {
        if (real == 0 && imag == 0) {
            return Complex.valueOf(0, 0);
        } else {
            double logr = logabs();
            double rho = MathLib.exp(logr * exp);
            double beta = arg() * exp;
            return Complex.valueOf(rho * MathLib.cos(beta),
                    rho * MathLib.sin(beta));
        }
    }

    public Complex pow(Real exp) {
        return pow(exp.real);
    }

    public Complex log() {
        return Complex.valueOf(logabs(), arg());
    }

    public Complex log10() {
        Complex z = log();
        z.realTimesEq(1 / MathLib.log(10));
        return z;
    }

    public Complex logb(Complex b) {
        return log().divide(b.log());
    }

    public Complex sin() {
        if (imag == 0.0) {
            return Complex.valueOf(MathLib.sin(real), 0.0);
        } else {
            return Complex.valueOf(MathLib.sin(real) * MathLib.cosh(imag),
                    MathLib.cos(real) * MathLib.sinh(imag));
        }
    }

    public Complex cos() {
        if (imag == 0.0) {
            return Complex.valueOf(MathLib.cos(real), 0.0);
        } else {
            return Complex.valueOf(MathLib.cos(real) * MathLib.cosh(imag),
                    MathLib.sin(real) * MathLib.sinh(-imag));
        }
    }

    public Complex tan() {
        if (MathLib.abs(imag) < 1) {
            double d = MathLib.pow(MathLib.cos(real), 2.0) + MathLib.pow(MathLib.sinh(imag), 2.0);
            return Complex.valueOf(0.5 * MathLib.sin(2 * real) / d, 0.5 * MathLib.sinh(2 * imag) / d);
        } else {
            double u = MathLib.exp(-imag);
            double c = 2 * u / (1 - MathLib.pow(u, 2.0));
            double d = 1 + MathLib.pow(MathLib.cos(real), 2.0) * MathLib.pow(c, 2.0);
            double s = MathLib.pow(c, 2.0);
            double t = 1.0 / MathLib.tanh(imag);
            return Complex.valueOf(0.5 * MathLib.sin(2 * real) * s / d, t / d);
        }
    }

    public Complex sec() {
        return cos().inverse();
    }

    public Complex csc() {
        return sin().inverse();
    }

    public Complex cot() {
        return tan().inverse();
    }
    /**
     * Converted from gsl sources.
     **/
    private static final double A_CROSSOVER = 1.5;
    private static final double B_CROSSOVER = 0.6417;

    /**
     * Converted from gsl sources.
     **/
    public Complex asin() {
        if (imag == 0) {
            return asinReal(real);
        } else {
            double r = Math2.hypot(real + 1, imag), s = Math2.hypot(real - 1, imag);
            double a = 0.5 * (r + s);
            double b = real / a;
            double y2 = imag * imag;
            double zr, zi;
            if (b <= B_CROSSOVER) {
                zr = MathLib.asin(b);
            } else {
                if (real <= 1) {
                    double D = 0.5 * (a + real) * (y2 / (r + real + 1) + (s + (1 - real)));
                    zr = MathLib.atan(real / MathLib.sqrt(D));
                } else {
                    double Apx = a + real;
                    double D = 0.5 * (Apx / (r + real + 1) + Apx / (s + (real - 1)));
                    zr = MathLib.atan(real / (imag * MathLib.sqrt(D)));
                }
            }
            if (a <= A_CROSSOVER) {
                double am1;
                if (real < 1) {
                    am1 = 0.5 * (y2 / (r + (real + 1)) + y2 / (s + (1 - real)));
                } else {
                    am1 = 0.5 * (y2 / (r + (real + 1)) + (s + (real - 1)));
                }
                zi = Math2.log1p(am1 + MathLib.sqrt(am1 * (a + 1)));
            } else {
                zi = MathLib.log(a + MathLib.sqrt(a * a - 1));
            }
            return Complex.valueOf((real >= 0) ? zr : -zr, (imag >= 0) ? zi : -zi);
        }
    }

    public Complex asinReal(double a) {
        if (MathLib.abs(a) <= 1.0) {
            return Complex.valueOf(MathLib.asin(a), 0.0);
        } else {
            if (a < 0.0) {
                return Complex.valueOf(-MathLib.TWO_PI, Math2.acosh(-a));
            } else {
                return Complex.valueOf(MathLib.TWO_PI, -Math2.acosh(a));
            }
        }
    }

    /**
     * Converted from gsl sources.
     **/
    public Complex acos() {
        if (imag == 0) {
            return acosReal(real);
        } else {
            double x = MathLib.abs(real);
            double y = MathLib.abs(imag);
            double r = Math2.hypot(x + 1, y), s = Math2.hypot(x - 1, y);
            double a = 0.5 * (r + s);
            double b = x / a;
            double y2 = y * y;

            double zr, zi;

            if (b <= B_CROSSOVER) {
                zr = MathLib.acos(b);
            } else {
                if (x <= 1) {
                    double d = 0.5 * (a + x) * (y2 / (r + x + 1) + (s + (1 - x)));
                    zr = MathLib.atan(MathLib.sqrt(d) / x);
                } else {
                    double apx = a + x;
                    double d = 0.5 * (apx / (r + x + 1) + apx / (s + (x - 1)));
                    zr = MathLib.atan((y * MathLib.sqrt(d)) / x);
                }
            }

            if (a <= A_CROSSOVER) {
                double am1;

                if (x < 1) {
                    am1 = 0.5 * (y2 / (r + (x + 1)) + y2 / (s + (1 - x)));
                } else {
                    am1 = 0.5 * (y2 / (r + (x + 1)) + (s + (x - 1)));
                }

                zi = Math2.log1p(am1 + MathLib.sqrt(am1 * (a + 1)));
            } else {
                zi = MathLib.log(a + MathLib.sqrt(a * a - 1));
            }

            return valueOf((x >= 0) ? zr : MathLib.PI - zr, (y >= 0) ? -zi : zi);
        }
    }

    public Complex acosReal(double a) {
        if (MathLib.abs(a) <= 1.0) {
            return Complex.valueOf(MathLib.acos(a), 0);
        } else {
            if (a < 0.0) {
                return Complex.valueOf(MathLib.PI, -Math2.acosh(-a));
            } else {
                return Complex.valueOf(0, Math2.acosh(a));
            }
        }
    }

    /**
     * Converted from gsl sources.
     **/
    public Complex atan() {

        if (imag == 0) {
            return valueOf(MathLib.atan(real), 0);
        } else {
            double r = Math2.hypot(real, imag);
            double i;
            double u = 2 * imag / (1 + r * r);

            if (MathLib.abs(u) < 0.1) {
                i = 0.25 * (Math2.log1p(u) - Math2.log1p(-u));
            } else {
                double a = Math2.hypot(real, imag + 1);
                double b = Math2.hypot(real, imag - 1);
                i = 0.5 * MathLib.log(a / b);
            }

            if (real == 0) {
                if (imag > 1) {
                    return valueOf(MathLib.TWO_PI, i);
                } else if (imag < -1) {
                    return valueOf(-MathLib.TWO_PI, i);
                } else {
                    return valueOf(0, i);
                }
            } else {
                return valueOf(0.5 * MathLib.atan2(2 * real, ((1 + r) * (1 - r))), i);
            }
        }
    }

    public Complex asec() {
        return inverse().acos();
    }

    public Complex asecReal(double a) {
        if (a <= -1.0 || a >= 1.0) {
            return Complex.valueOf(MathLib.acos(1 / a), 0.0);
        } else {
            if (a >= 0.0) {
                return Complex.valueOf(0, Math2.acosh(1 / a));
            } else {
                return Complex.valueOf(MathLib.PI, -Math2.acosh(-1 / a));
            }
        }
    }

    public Complex acsc() {
        return inverse().asin();
    }

    public Complex acscReal(double a) {
        if (a <= -1.0 || a >= 1.0) {
            return Complex.valueOf(MathLib.asin(1 / a), 0.0);
        } else {
            if (a >= 0.0) {
                return Complex.valueOf(MathLib.TWO_PI, -Math2.acosh(1 / a));
            } else {
                return Complex.valueOf(-MathLib.TWO_PI, Math2.acosh(-1 / a));
            }
        }
    }

    public Complex acot() {
        if (real == 0.0 && imag == 0.0) {
            return Complex.valueOf(MathLib.TWO_PI, 0);
        } else {
            return inverse().atan();
        }
    }

    public Complex sinh() {
        return Complex.valueOf(MathLib.sinh(real) * MathLib.cos(imag), MathLib.cosh(real) * MathLib.sin(imag));
    }

    public Complex cosh() {
        return Complex.valueOf(MathLib.cosh(real) * MathLib.cos(imag), MathLib.sinh(real) * MathLib.sin(imag));
    }

    /**
     * Converted from gsl sources.
     **/
    public Complex tanh() {
        double u = MathLib.pow(MathLib.cos(imag), 2.0)
                + MathLib.pow(MathLib.sinh(real), 2.0);
        if (MathLib.abs(real) < 1.0) {
            return Complex.valueOf(MathLib.sinh(real) * MathLib.cosh(real) / u,
                    0.5 * MathLib.sin(2 * imag) / u);
        } else {
            double t = 1 + MathLib.pow(MathLib.cos(imag) / MathLib.sinh(real), 2.0);

            return Complex.valueOf(1.0 / (MathLib.tanh(real) * t), 0.5 * MathLib.sin(2 * imag) / u);
        }
    }

    public Complex sech() {
        return cosh().inverse();
    }

    public Complex csch() {
        return sinh().inverse();
    }

    public Complex coth() {
        return tanh().inverse();
    }

    public Complex asinh() {
        return asin().conjugate();
    }

    public Complex acosh() {
        Complex z = acos();
        if (z.imag > 0) {
            z.conjugate();
        }
        return z;
    }

    public Complex acoshReal(double a) {
        if (a >= 1) {
            return valueOf(Math2.acosh(a), 0);
        } else {
            if (a >= -1.0) {
                return valueOf(0, MathLib.acos(a));
            } else {
                return valueOf(Math2.acosh(-a), MathLib.PI);
            }
        }
    }

    public Complex atanh() {
        if (imag == 0.0) {
            return atanhReal(real);
        }
        return atan().conjugate();
    }

    /**
     * Converted from gsl sources.
     **/
    public Complex atanhReal(double a) {
        if (a > -1.0 && a < 1.0) {
            return valueOf(Math2.atanh(a), 0);
        } else {
            return valueOf(Math2.atanh(1 / a),
                    (a < 0) ? MathLib.TWO_PI : -MathLib.TWO_PI);
        }
    }

    public Complex asech() {
        return inverse().acosh();
    }

    public Complex acsch() {
        return inverse().asinh();
    }

    public Complex acoth() {
        return inverse().atanh();
    }

    /* optional implementations
    public Complex tan() {
    double x = MathLib.toRadians(real);
    double y = MathLib.toRadians(imag);
    double x1 = MathLib.sin(x) * MathLib.cosh(y);
    double y1 = MathLib.cos(x) * MathLib.sinh(y);
    double x2 = MathLib.cos(x) * MathLib.cosh(y);
    double y2 = MathLib.sin(x) * MathLib.sinh(y);
    double z = x2 * x2 + y2 * y2;
    x = x2 / z;
    y = -y2 / z;
    return Complex.valueOf(x1 * x - y1 * y, x1 * y + y1 * x);
    }
    public Complex sin() {
    double x = MathLib.toRadians(real);
    double y = MathLib.toRadians(imag);
    return Complex.valueOf(MathLib.sin(x) * MathLib.cosh(y),
    MathLib.cos(x) * MathLib.sinh(y));
    }
    public Complex cos() {
    double x = MathLib.toRadians(real);
    double y = MathLib.toRadians(imag);
    return Complex.valueOf(MathLib.cos(x) * MathLib.cosh(y),
    -MathLib.sin(x) * MathLib.sinh(y));
    }
    public Complex sinh() {
    double x = MathLib.toRadians(real);
    double y = MathLib.toRadians(imag);
    return Complex.valueOf(MathLib.sinh(x) * MathLib.cos(y),
    MathLib.cosh(x) * MathLib.sin(y));
    }
    public Complex cosh() {
    double x = MathLib.toRadians(real);
    double y = MathLib.toRadians(imag);
    return Complex.valueOf(MathLib.cosh(x) * MathLib.cos(y),
    MathLib.sinh(x) * MathLib.sin(y));
    }
     */
    public Text toText() {
        Text t = Text.valueOf(real);
        if(imag < 0){
            t.concat(Text.intern(" - "));
        }else{
            t.concat(Text.intern(" + "));
        }
        t.concat(Text.valueOf(imag));
        t.concat(Text.valueOf('i'));
        return t;
    }

    @Override
    public String toString() {
        return toText().toString();
    }

    @Override
    public void negateEq() {
        real = -real;
        imag = -imag;
    }
}

