/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources.vsrcs;

import com.tejas.eda.spice.device.sources.DependentLinearSource;

/**
 *
 * @author Kristopher T. Beck
 */
public class VCVSValues extends DependentLinearSource {

    /* Positive control source index */
    protected int contPosIndex;

    /* Negative control source index */
    protected int contNegIndex;

    /* Coefficient */
    protected double coeff;

    public double getCoeff() {
        return coeff;
    }

    public void setCoeff(double coeff) {
        this.coeff = coeff;
    }

    public int getContNegIndex() {
        return contNegIndex;
    }

    public void setContNegIndex(int contNegIndex) {
        this.contNegIndex = contNegIndex;
    }

    public int getContPosIndex() {
        return contPosIndex;
    }

    public void setContPosIndex(int contPosIndex) {
        this.contPosIndex = contPosIndex;
    }
}
