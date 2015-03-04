/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device;

/**
 *
 * @author Kristopher T. Beck
 */
public class SpiceDevice {

    protected String instName;
    protected String modelName;

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
