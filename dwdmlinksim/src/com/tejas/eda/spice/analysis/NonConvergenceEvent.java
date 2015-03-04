/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.eda.spice.analysis;

import com.tejas.eda.spice.device.Instance;

/**
 *
 * @author Kristopher T. Beck
 */
public class NonConvergenceEvent extends AnalysisEvent{
    private Instance badModel;
    
    public NonConvergenceEvent(Analysis source, Instance badModel, String msg) {
        super(source);
        this.badModel = badModel;
        this.msg = msg;
    }

}
