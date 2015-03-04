/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.source.optical;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.Network;
import com.tejas.eda.spice.OpticalWorkEnv;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.math.numbers.Complex;

/**
 *
 * @author owenbad
 */
public abstract class OpticalSourceElement {

    protected OpticalWorkEnv wrk;
    protected String name;

    public boolean init(Network nwk) {
        wrk = nwk.getWrk();
        return true;
    }

    public void setWrk(OpticalWorkEnv wrk) {
        this.wrk = wrk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract Complex getValue();
}
