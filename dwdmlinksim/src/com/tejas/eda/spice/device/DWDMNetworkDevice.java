/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device;

/**
 *
 * @author Kristopher T. Beck
 */
public class DWDMNetworkDevice {

    protected String instName;
    protected String modelName;
    protected double attenuation;
    protected double dispersion;
    protected double noiseFigure;


    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getInstName() {
        return instName;
    }

    public void setInstName(String name) {
        this.instName = name;
    }
}
