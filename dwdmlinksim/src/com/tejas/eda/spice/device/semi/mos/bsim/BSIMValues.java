/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.semi.mos.bsim;

import com.tejas.eda.spice.device.semi.mos.MOSModelValues;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class BSIMValues extends MOSModelValues {
    protected  double drnPerimeter;
    protected  double srcPerimeter;

    public BSIMValues() {
        l = 5e-6;
        w = 5e-6;
    }

    public double getDrnPerimeter() {
        return drnPerimeter;
    }

    public void setDrnPerimeter(double drnPerimeter) {
        this.drnPerimeter = drnPerimeter;
    }

    public double getSrcPerimeter() {
        return srcPerimeter;
    }

    public void setSrcPerimeter(double srcPerimeter) {
        this.srcPerimeter = srcPerimeter;
    }

}
