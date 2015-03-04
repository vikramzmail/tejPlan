/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.sources.isrcs;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.Mode;
import com.tejas.eda.spice.Temporal;
import com.tejas.eda.spice.EnumConsts.MODE;

/**
 *
 * @author Kristopher T. Beck
 */
public class ISrcPWL extends ISrcInstance {

    private Temporal tmprl;
    private double[] times;
    private double[] values;

    public ISrcPWL() {
    }

    @Override
    public boolean init(Circuit ckt) {
        tmprl = ckt.getTemporal();
        return super.init(ckt);
    }

    public double[] getTimes() {
        return times;
    }

    public void setTimes(double[] times) {
        this.times = times;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    @Override
    public boolean accept(Mode mode) {
        int functionOrder = times.length;
        if (!mode.contains(MODE.INIT_TRAN) && !mode.contains(MODE.TRANOP)) {
            return true;
        } else {
            if (tmprl.getTime() < times[0]) {
                if (tmprl.isBreak()) {
                    tmprl.setCurrentBreak(times[0]);
                }
                return true;
            }
            for (int i = 0; i < functionOrder - 1; i++) {
                if (times[i] == tmprl.getTime()) {
                    if (tmprl.isBreak()) {
                        tmprl.setCurrentBreak(times[i + 1]);
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected double computeValue(Mode mode) {
        double value;
        double time;
        int functionOrder = times.length;
        do {
            if (mode.contains(MODE.DC)) {
                time = 0;
            } else {
                time = tmprl.getTime();
            }
            if (time < times[0]) {
                value = (values[0]);
                break;
            }
            for (int i = 0; i <= functionOrder - 1; i++) {
                if (times[i] == time) {
                    value = values[1];
                    break;
                }
                if (times[i] < time && times[i + 1] > time) {
                    value = values[i] + (time - times[i])
                            / (times[i + 1] - times[i])
                            * (values[i + 1] - values[i]);
                    break;
                }
            }
            value = (values[functionOrder - 1]);
        } while (false);
        return value;
    }
}
