/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.eda.spice.analysis.optical;

import com.tejas.eda.spice.device.OpticalInstance;

/**
 *
 * @author Kristopher T. Beck
 */
public class OpticalNonConvergenceEvent extends OpticalAnalysisEvent{
    private OpticalInstance badModel;
    
    public OpticalNonConvergenceEvent(OpticalAnalysis source, OpticalInstance badModel, String msg) {
        super(source);
        this.badModel = badModel;
        this.msg = msg;
    }

}
