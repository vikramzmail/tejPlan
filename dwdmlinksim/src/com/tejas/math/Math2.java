/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import javolution.lang.MathLib;

/**
 *
 * @author Kristopher
 */
public class Math2 {

    public static double csc(double x) {
        return 1 / MathLib.sin(x);
    }

    public static double sec(double x) {
        return 1 / MathLib.cos(x);
    }

    public static double cot(double x) {
        return 1 / MathLib.tan(x);
    }

    public static double acsc(double x) {
        return MathLib.asin(1 / x);
    }

    public static double asec(double x) {
        return MathLib.HALF_PI - MathLib.asin(1 / x);
    }

    public static double acot(double x) {
        return MathLib.HALF_PI - MathLib.atan(x);
    }

    public static double csch(double x) {
        return 1 / MathLib.sinh(x);
    }

    public static double sech(double x) {
        return 1 / MathLib.sinh(x);
    }

    public static double coth(double x) {
        return 1 / MathLib.tanh(x);
    }

    public static double asinh(double x) {
        double y = x * x;
        y = y + 1;
        y = x + MathLib.sqrt(y);
        y = MathLib.log(y);
        return y;
    }

    public static double acosh(double x) {
        double y = x * x;
        y = 1 - y;
        y = x + MathLib.sqrt(y);
        y = MathLib.log(y);
        return y;
    }

    public static double atanh(double x) {
        double y = x * x;
        y = y + 1;
        y = MathLib.sqrt(y);
        y = y / (1 - x);
        y = MathLib.log(y);
        return y;
    }

    public static double acsch(double x) {
        double y = x * x;
        y = 1 / y;
        y = y + 1;
        y = MathLib.sqrt(y);
        y = y + (1 / x);
        y = MathLib.log(y);
        return y;
    }

    public static double asech(double x) {
        double y = x * x;
        y = 1 / y;
        y = y - 1;
        y = MathLib.sqrt(y);
        y = y + (1 / x);
        y = MathLib.log(y);
        return y;
    }

    public static double acoth(double x) {
        double y = x + 1 / x - 1;
        y = MathLib.log(y);
        y /= 2;
        return y;
    }

    /**
     * Converted from gsl sources.
     **/
    public static double hypot(double x, double y) {
        double xabs = MathLib.abs(x);
        double yabs = MathLib.abs(y);
        double min, max;

        if (xabs < yabs) {
            min = xabs;
            max = yabs;
        } else {
            min = yabs;
            max = xabs;
        }

        if (min == 0) {
            return max;
        }
        double u = min / max;
        return max * MathLib.sqrt(1 + u * u);
    }

    /**
     * Converted from gsl sources.
     **/
    public static double hypot3(double x, double y, double z) {
        double xabs = MathLib.abs(x);
        double yabs = MathLib.abs(y);
        double zabs = MathLib.abs(z);
        double w = MathLib.max(xabs, MathLib.max(yabs, zabs));

        if (w == 0.0) {
            return (0.0);
        } else {
            double r = w * MathLib.sqrt((xabs / w) * (xabs / w)
                    + (yabs / w) * (yabs / w)
                    + (zabs / w) * (zabs / w));
            return r;
        }
    }

    /**
     * Converted from gsl sources.
     **/
    public static double log1p(double x) {
        double y, z;
        y = 1 + x;
        z = y - 1;
        return MathLib.log(y) - (z - x) / y;  /* cancels errors with IEEE arithmetic */
    }
}
