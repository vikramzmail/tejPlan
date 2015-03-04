/*
 * Instance.java
 *
 * Created on May 13, 2007, 11:09 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.device;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.Mode;
import com.tejas.eda.spice.Network;

/**
 *
 * @author Kristopher T. Beck
 */
public interface OpticalInstance {

    public boolean accept(Mode mode);

    public boolean init(Network network);

    public boolean unSetup();

    public boolean load(String mode);

    public boolean acLoad(Mode mode);

    public boolean temperature();

    public double truncateTimeStep(double timeStep);

    public boolean convTest(String mode);

    public boolean loadInitCond();

    public String getInstName();

    public String getModelName();
//    public boolean resetInitCond();
//    public boolean pzSetup();
    /*public boolean pzLoad(Complex cmplx){
    return true;
    }

    /*    public boolean distor(DISTOR mode){
    return true;
    }
     */  /*
    public boolean noise(NZ_MODE i1, NZ_OPER i2, NData nData, double d){
    return true;
    }
    
    public boolean destroy() {
    return true;
    }
     */

}
