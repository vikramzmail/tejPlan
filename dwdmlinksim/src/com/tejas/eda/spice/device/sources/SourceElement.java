/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.math.numbers.Complex;

/**
 *
 * @author owenbad
 */
public abstract class SourceElement {

    protected WorkEnv wrk;
    protected String name;

    public boolean init(Circuit ckt) {
        wrk = ckt.getWrk();
        return true;
    }

    public void setWrk(WorkEnv wrk) {
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
