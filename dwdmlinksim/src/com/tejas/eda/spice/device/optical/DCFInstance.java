
package com.tejas.eda.spice.device.optical;

import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.EnvVars;
import com.tejas.eda.spice.Mode;
import com.tejas.eda.spice.Network;
import com.tejas.eda.spice.NetworkStateTable;
import com.tejas.eda.spice.NetworkStateTable.StateVector;
import com.tejas.eda.spice.OpticalEnvVars;
import com.tejas.eda.spice.OpticalWorkEnv;
import com.tejas.eda.spice.Temporal;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.eda.spice.EnumConsts.MODE;
import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.OpticalInstance;
import com.tejas.eda.spice.device.semi.mos.MOSUtils;
import com.tejas.math.Bool;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;


import javolution.lang.MathLib;
import javolution.util.StandardLog;
import static com.tejas.eda.spice.Constants.*;

/**
 *
 * @author Kristopher T. Beck
 */
public class DCFInstance extends DCFModelValues implements OpticalInstance {

    private Network nwk;
    private OpticalWorkEnv wrk;
    private OpticalEnvVars env;
    private NetworkStateTable stateTable;
    private Temporal tmprl;

    private Complex posPosNode;
    private Complex negNegNode;
    private Complex posNegNode;
    private Complex negPosNode;
    private StateVector voltageStates;
    private StateVector currentStates;
    private StateVector conductStates;
    private StateVector capChargeStates;
    private StateVector capCurrentStates;
    Bool check = Bool.TRUE();

    /** Creates a new instance of  */
    public DCFInstance() {
    }

    public Network getNwk() {
		return nwk;
	}

	public void setNwk(Network nwk) {
		this.nwk = nwk;
	}

	public boolean init(Network nwk) {
        this.nwk = nwk;
        wrk = nwk.getWrk();
        env = nwk.getEnv();
        stateTable = nwk.getStateTable();
        tmprl = nwk.getTemporal();
        

        posPosNode = wrk.aquireNode(posIndex, posIndex);
        negNegNode = wrk.aquireNode(negIndex, negIndex);
        posNegNode = wrk.aquireNode(posIndex, negIndex);
        negPosNode = wrk.aquireNode(negIndex, posIndex);
        return true;
    }

    public boolean acLoad(Mode mode) {
return true;
    }

    public boolean convTest(String mode) {
   return true;
    }

    public boolean loadInitCond() {
    	
        return true;
    }

    public boolean load(String mode) {
    	
    	if(mode.equals("power") || mode.equals("noise")){
    		posPosNode.plusEq(1/pLoss);
    		posNegNode.minusEq(1.0);
    		
    	} else if (mode.equals("dispersion")){
    		posPosNode.plusEq(1.0);
    		posNegNode.minusEq(1.0);
    		wrk.getRhsAt(posIndex).plusEq(dispComp);
    	}
    	return true;
    }

    public boolean pzLoad(Complex s) {
       return true;
    }

    public boolean unSetup() {
     return true;
    }

    public boolean temperature() {
    	        return true;
    }

    public double truncateTimeStep(double timeStep) {
        return stateTable.terr(capChargeStates, capCurrentStates, timeStep);
    }

    public boolean accept(Mode mode) {
        return true;
    }
}
