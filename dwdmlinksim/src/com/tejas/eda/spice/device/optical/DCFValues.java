package com.tejas.eda.spice.device.optical;

import com.tejas.eda.spice.device.DWDMNetworkDevice;
import com.tejas.eda.spice.device.SpiceDevice;

/**
 *
 * @author Kristopher T. Beck
 */
public class DCFValues extends DWDMNetworkDevice {

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
