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
public class ICVSValues extends DependentLinearSource {

    protected String contName;
    protected double coeff;

    public double getCoeff() {
        return coeff;
    }

    public void setCoeff(double coeff) {
        this.coeff = coeff;
    }

    public String getContName() {
        return contName;
    }

    public void setContName(String contName) {
        this.contName = contName;
    }    
}
