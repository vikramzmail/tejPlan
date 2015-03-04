/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.analysis;

import java.util.EventObject;

import com.tejas.eda.spice.analysis.optical.OpticalAnalysis;

/**
 *
 * @author Kristopher T. Beck
 */
public class AnalysisEvent extends EventObject {

    public String refName;
    public double refValue;
    public String msg;

    public AnalysisEvent(Analysis source) {
        super(source);

    }

    public AnalysisEvent(Analysis source, double refValue) {
        super(source);
        this.refValue = refValue;
    }


    public AnalysisEvent(OpticalAnalysis source) {
    	super(source);
	}

	public AnalysisEvent(OpticalAnalysis source, double refValue) {
		super(source);
		this.refValue = refValue;
	}

	public Analysis getAnalysis(){
        return (Analysis)getSource();
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
