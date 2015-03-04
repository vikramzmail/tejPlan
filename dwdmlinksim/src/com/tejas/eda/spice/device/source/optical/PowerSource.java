/*
 * VSrcSine.java
 * 
 * Created on Nov 13, 2007, 3:28:50 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device.source.optical;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.EnvVars;
import com.tejas.eda.spice.Mode;
import com.tejas.eda.spice.Network;
import com.tejas.eda.spice.OpticalEnvVars;
import com.tejas.eda.spice.OpticalWorkEnv;
import com.tejas.eda.spice.Temporal;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.eda.spice.EnumConsts.MODE;
import com.tejas.math.numbers.Complex;

import javolution.lang.MathLib;

/**
 *
 * @author Kristopher T. Beck
 */
public class PowerSource extends OpticalSource {

	protected Network nwk;
    protected OpticalWorkEnv wrk;
    private OpticalEnvVars env;
    /* Branch equation index */
//    protected int branchIndex;
//    protected Complex ibrIbrNode;
//    protected Complex ibrPosNode;
//    protected Complex ibrNegNode;
//    protected Complex posIbrNode;
//    protected Complex negIbrNode;
    protected Complex posNode;
	private Temporal tmprl;
    private double power; // Always in mW
    
    public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = Math.pow(10.0, ((power)/10)); // Conversion to mW
	}

	public PowerSource() {
    }

    @Override
    public boolean init(Network nwk) {
    	this.nwk = nwk;
        wrk = nwk.getWrk();
        env = nwk.getEnv();
//        if (branchIndex == 0) {
//            branchIndex = nwk.makeCurrentNode(instName
//                    + "#branch").getIndex();
//        }
//        posIbrNode = wrk.aquireNode(posIndex, branchIndex);
//        negIbrNode = wrk.aquireNode(negIndex, branchIndex);
//        ibrNegNode = wrk.aquireNode(branchIndex, negIndex);
//        ibrPosNode = wrk.aquireNode(branchIndex, posIndex);
        posNode = wrk.aquireNode((wrk.getMatrix().getColSize()-1),posIndex);
        return true;
    }

    
	@Override
	public boolean accept(Mode mode) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean unSetup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean load(String mode) {
		if(mode.equals("power")){
			posNode.plusEq(1.0);
			//        negIbrNode.minusEq(1.0);
			//        ibrPosNode.plusEq(1.0);
			//        ibrNegNode.minusEq(1.0);

			wrk.getRhsAt(wrk.getMatrix().getColSize() - 1).realPlusEq(power);
		} else if (mode.equals("noise")){
			posNode.plusEq(1.0);
		} else if (mode.equals("dispersion")){
			posNode.plusEq(1.0);
		}
		return true;
	}

	@Override
	public boolean acLoad(Mode mode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean temperature() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double truncateTimeStep(double timeStep) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean convTest(String mode) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean loadInitCond() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInstName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getModelName() {
		// TODO Auto-generated method stub
		return null;
	}
}
