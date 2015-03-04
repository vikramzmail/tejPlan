/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.analysis.optical;

import java.util.EventObject;

import com.tejas.eda.spice.analysis.optical.OpticalAnalysis;

/**
 *
 * @author Kristopher T. Beck
 */
public class OpticalAnalysisEvent extends EventObject {

    public String refName;
    public double refValue;
    public String msg;

    public OpticalAnalysisEvent(OpticalAnalysis source) {
        super(source);

    }

    public OpticalAnalysisEvent(OpticalAnalysis source, double refValue) {
        super(source);
        this.refValue = refValue;
    }

	public OpticalAnalysis getAnalysis(){
        return (OpticalAnalysis)getSource();
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public double getRefValue() {
        return refValue;
    }

    public void setRefValue(double refValue) {
        this.refValue = refValue;
    }
    
}
