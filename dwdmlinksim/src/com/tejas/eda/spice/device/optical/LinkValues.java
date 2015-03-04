/*
 * DiodeValues.java
 * 
 * Created on Nov 13, 2007, 3:19:34 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.optical;

import com.tejas.eda.spice.device.DWDMNetworkDevice;
import com.tejas.eda.spice.device.SpiceDevice;

/**
 *
 * @author Kristopher T. Beck
 */
public class LinkValues extends DWDMNetworkDevice {

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
