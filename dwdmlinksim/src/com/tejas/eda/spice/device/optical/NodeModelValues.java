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
public class NodeModelValues extends NodeValues implements Model {
	
	protected double NLOSS = 30;

	
	public double getnLoss() {
		return NLOSS;
	}
	public void setnLoss(double NLOSS) {
		this.NLOSS = NLOSS; //Conversion to a linear quantity
	}
		
}
