package com.tejas.eda.spice.device.optical;

import com.tejas.eda.spice.device.Model;

/**
 *
 * @author Kristopher T. Beck
 */

public class DCFModelValues extends DCFValues implements Model {
	
	protected double dispComp = 0;
	protected double pLoss = 0;
	
	public double getDispComp() {
		return dispComp;
	}
	public void setDispComp(double dispComp) {
		this.dispComp = dispComp; //Conversion to a linear quantity
	}
	public double getPLoss() {
		return pLoss;
	}
	public void setPLoss(double pLoss) {
		this.pLoss = Math.pow(10.0, ((pLoss)/10)); //Conversion to a linear quantity
	}
	
	
}
