/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources;

import com.tejas.eda.spice.device.*;

/**
 *
 * @author Kristopher T. Beck
 */
public class SpiceSource extends SpiceDevice {

    protected int posIndex;
    protected int negIndex;

    public int getNegIndex() {
        return negIndex;
    }

    public void setNegIndex(int negIndex) {
        this.negIndex = negIndex;
    }

    public int getPosIndex() {
        return posIndex;
    }

    public void setPosIndex(int posIndex) {
        this.posIndex = posIndex;
    }
}
