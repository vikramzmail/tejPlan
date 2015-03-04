/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.optical;

import com.tejas.eda.spice.device.Model;

/**
 *
 * @author Kristopher T. Beck
 */
public class EDFAModelValues extends EDFAValues implements Model {
	
	protected double gain = 0;
	protected double noiseFigure = 0;
	
	public double getGain() {
		return gain;
	}
	public void setGain(double gain) {
		this.gain = Math.pow(10.0, ((gain)/10)); //Conversion to a linear quantity
	}
	public double getNoiseFigure() {
		return noiseFigure;
	}
	public void setNoiseFigure(double noiseFigure) {
		this.noiseFigure = Math.pow(10.0, ((noiseFigure)/10)); //Conversion to a linear quantity
	}
	
	
}
