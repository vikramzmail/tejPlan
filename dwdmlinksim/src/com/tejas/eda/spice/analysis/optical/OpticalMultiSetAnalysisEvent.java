/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.analysis.optical;

import java.util.List;

import com.tejas.math.numbers.Real;

import javolution.util.FastList;

/**
 *
 * @author Kristopher T. Beck
 */
public class OpticalMultiSetAnalysisEvent extends OpticalAnalysisEvent {

    List<String> dataIds;
    FastList<Real> data;

    public OpticalMultiSetAnalysisEvent(OpticalAnalysis source, List<String> data) {
        super(source);
        this.dataIds = data;
    }

    public OpticalMultiSetAnalysisEvent(OpticalAnalysis source, double regValue, FastList<Real> data) {
        super(source);
        this.data = data;

    }
    
    public OpticalMultiSetAnalysisEvent(OpticalAnalysis source) {
    	super(source);
	}

	public FastList<Real> getData() {
        return data;
    }

    public void setData(FastList<Real> data) {
        this.data = data;
    }

    public List<String> getDataIds() {
        return dataIds;
    }

    public void setDataIds(List<String> dataIds) {
        this.dataIds = dataIds;
    }
}
