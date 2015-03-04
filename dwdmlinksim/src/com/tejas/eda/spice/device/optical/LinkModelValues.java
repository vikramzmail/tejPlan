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
public class LinkModelValues extends LinkValues implements Model {
	
	protected double length = 0;
	protected double alpha = 0.2;
	protected double dee = -17.0;
	
	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	
	public void setDee(double dee){
		this.dee = dee;
	}
	
}
