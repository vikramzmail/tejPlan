/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources.asrc;

import com.tejas.eda.spice.device.sources.SpiceSource;
import com.tejas.math.func.Function;
import com.tejas.math.numbers.Real;

/**
 *
 * @author Kristopher T. Beck
 */
public class ASrcValues extends SpiceSource {

    Function<Real> function;

    public Function<Real> getFunction() {
        return function;
    }

    public void setFunction(Function<Real> function) {
        this.function = function;
    }
}
