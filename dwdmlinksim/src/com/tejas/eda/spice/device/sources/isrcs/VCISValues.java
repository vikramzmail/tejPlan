/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources.isrcs;

import com.tejas.eda.spice.device.sources.DependentLinearSource;

/**
 *
 * @author Kristopher T. Beck
 */
public class VCISValues extends DependentLinearSource {

    protected int contPosIndex;
    protected int contNegIndex;

    /* Coefficient */
    protected double coeff;

    public int getContPosIndex() {
        return contPosIndex;
    }

    public void setContPosIndex(int contPosIndex) {
        this.contPosIndex = contPosIndex;
    }

    public int getContNegIndex() {
        return contNegIndex;
    }

    public void setContNegIndex(int contNegIndex) {
        this.contNegIndex = contNegIndex;
    }

    public double getCoeff() {
        return coeff;
    }

    public void setCoeff(double coeff) {
        this.coeff = coeff;
    }
}
