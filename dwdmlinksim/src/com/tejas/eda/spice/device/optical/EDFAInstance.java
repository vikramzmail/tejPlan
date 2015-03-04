/*
 * .java
 *
 * Created on August 25, 2006, 2:13 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
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
public class EDFAInstance extends EDFAModelValues implements OpticalInstance {

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
    public EDFAInstance() {
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
//        attenuation = Math.pow(10.0, ((alpha * length)/10));

        
//        if (length == 0){
//        	posPrmIndex = posIndex;
//        } else if (posPrmIndex == 0){
//        	posPrmIndex = nwk.makePowerNode(getInstName() + "internal").getIndex();
//        }

        posPosNode = wrk.aquireNode(posIndex, posIndex);
        negNegNode = wrk.aquireNode(negIndex, negIndex);
        posNegNode = wrk.aquireNode(posIndex, negIndex);
        negPosNode = wrk.aquireNode(negIndex, posIndex);
        return true;
    }

    public boolean acLoad(Mode mode) {
    	//TODO Need to check - Anant
//        double gspr = tConductance * area * m;
//        double geq = conductStates.get(0);
//        double xceq = capCurrentStates.get(0) * tmprl.getOmega();
//        posPosNode.realPlusEq(gspr);
//        negNegNode.realPlusEq(geq);
//        negNegNode.imagPlusEq(xceq);
//        posPrmPosPrmNode.realPlusEq(geq + gspr);
//        posPrmPosPrmNode.imagPlusEq(xceq);
//        posPosPrmNode.realMinusEq(gspr);
//        negPosPrmNode.realMinusEq(geq);
//        negPosPrmNode.imagMinusEq(xceq);
//        posPrmPosNode.realMinusEq(gspr);
//        posPrmNegNode.realMinusEq(geq);
//        posPrmNegNode.imagMinusEq(xceq);
        return true;
    }

    public boolean convTest(String mode) {
    	//TODO - To checked later - Anant
//        double vd = wrk.getRhsOldAt(posPrmIndex).getReal()
//                - wrk.getRhsOldAt(negIndex).getReal();
//        double delvd = vd - voltageStates.get(0);
//        double cdhat = currentStates.get(0)
//                + conductStates.get(0) * delvd;
//        double cd = currentStates.get(0);
//        double tol = env.getRelTol()
//                * MathLib.max(MathLib.abs(cdhat), MathLib.abs(cd)) + env.getAbsTol();
//        if (MathLib.abs(cdhat - cd) > tol) {
//            return false;
//        }
        return true;
    }

    public boolean loadInitCond() {
    	//TODO Need to check - Anant
//        if (initCond == 0) {
//            initCond =
//                    wrk.getRhsAt(posIndex).getReal()
//                    - wrk.getRhsAt(negIndex).getReal();
//        }
        return true;
    }

    public boolean load(String mode) {
    	//TODO Need to check - Anant
    	if (mode.endsWith("power")){
    		posPosNode.plusEq(gain);
    		//    	negNegNode.plusEq(1/attenuation);
    		posNegNode.minusEq(1.0);
    		//    	negPosNode.minusEq(1.0);
    	} else if (mode.equals("noise")){
    		posPosNode.plusEq(gain);
    		//    	negNegNode.plusEq(1/attenuation);
    		posNegNode.minusEq(1.0);
    		//    	negPosNode.minusEq(1.0);
    		wrk.getRhsAt(posIndex).minusEq(gain*noiseFigure*Double.parseDouble("15991e-10"));   //ss earlier 15991e-10
    		// 15991e-10: (h)(nu)(deltaf) h = planck's constant= 6.626e-34, nu = frequency = 193THz,
    		// deltaf = bandwidth measurement for noisefigure = 12.5GHz = 0.1nm  
    	} else if (mode.equals("dispersion")){
    		posPosNode.plusEq(1.0);
    		posNegNode.minusEq(1.0);
    	}
    	return true;
    }

    public boolean pzLoad(Complex s) {
    	//TODO Need to check - Anant
//        double gspr = tConductance * area * m;
//        double geq = conductStates.get(0);
//        double xceq = capCurrentStates.get(0);
//        posPosNode.realPlusEq(gspr);
//        negNegNode.realPlusEq(geq + xceq * s.getReal());
//        negNegNode.imagPlusEq(xceq * s.getReal());
//        posPrmPosPrmNode.realPlusEq(geq + gspr + xceq * s.getReal());
//        posPrmPosPrmNode.imagPlusEq(xceq * s.getReal());
//        posPosPrmNode.realMinusEq(gspr);
//        negPosPrmNode.realMinusEq(geq + xceq * s.getReal());
//        negPosPrmNode.imagMinusEq(xceq * s.getReal());
//        posPrmPosNode.realMinusEq(gspr);
//        posPrmNegNode.realMinusEq(geq + xceq * s.getReal());
//        posPrmNegNode.imagMinusEq(xceq * s.getReal());
        return true;
    }

    public boolean unSetup() {
    	//TODO To be checked later - Anant
//        if (posPrmIndex != posIndex) {
//            nwk.deleteNode(posPrmIndex);
//            posPrmIndex = 0;
//        }
        return true;
    }

    public boolean temperature() {
    	//TODO Need to check - Anant
//        if(tnom == 0){
//            tnom = env.getNomTemp();
//        }
//        double vtnom = KoverQ * tnom;
//        double xfc = MathLib.log(1 - depletionCapCoeff);
//        double xfcs = MathLib.log(1 - depletionSWcapCoeff);
//        if (temp == 0) {
//            temp = env.getTemp() + dtemp;
//        }
//        // Junction grading temperature adjustment
//        double difference = temp - tnom;
//        double factor = 1 + (gradCoeffTemp1 * difference)
//                + (gradCoeffTemp2 * difference * difference);
//        tGradingCoeff = gradingCoeff * factor;
//        // Temperature corrected grading coefficient 0 0.9
//        if (tGradingCoeff > 0.9) {
//            StandardLog.warning(getInstName() + ": Temperature corrected grading coefficient too large, limited to 0.9");
//            tGradingCoeff = 0.9;
//        }
//        double vt = KoverQ * temp;
//        double fact2 = temp / REF_TEMP;
//        double egfet = 1.16 - (7.02e-4 * temp * temp)
//                / (temp + 1108);
//        double arg = -egfet / (2 * BOLTZMANN * temp)
//                + 1.1150877 / (BOLTZMANN * REF_TEMP + REF_TEMP);
//        double pbfact = -2 * vt * (1.5 * MathLib.log(fact2) + CHARGE * arg);
//        double egfet1 = 1.16 - (7.02e-4 * tnom * tnom)
//                / (tnom + 1108);
//        double arg1 = -egfet1 / (BOLTZMANN * 2 * tnom)
//                + 1.1150877 / (2 * BOLTZMANN * REF_TEMP);
//        double fact1 = tnom / REF_TEMP;
//        double pbfact1 = -2 * vtnom * (1.5 * MathLib.log(fact1) + CHARGE * arg1);
//        double pbo = (junctionPot - pbfact1) / fact1;
//        double gmaold = (junctionPot - pbo) / pbo;
//        tJctCap = junctionCap
//                / (1 + tGradingCoeff
//                * (400e-6 * (tnom - REF_TEMP) - gmaold));
//        tJctPot = pbfact + fact2 * pbo;
//        double gmanew = (tJctPot - pbo) / pbo;
//        tJctCap *= 1 + tGradingCoeff
//                * (400e-6 * (temp - REF_TEMP) - gmanew);
//        double pboSW = (junctionSWPot - pbfact1) / fact1;
//        double gmaSWold = (junctionSWPot - pboSW) / pboSW;
//        tJctSWCap = junctionSWCap
//                / (1 + gradingSWCoeff
//                * (400e-6 * (tnom - REF_TEMP) - gmaSWold));
//        tJctSWPot = pbfact + fact2 * pboSW;
//        double gmaSWnew = (tJctSWPot - pboSW) / pboSW;
//        tJctSWCap *= 1 + gradingSWCoeff
//                * (400e-6 * (temp - REF_TEMP) - gmaSWnew);
//        tSatCur = satCur * MathLib.exp(((temp / tnom) - 1)
//                * activationEnergy / (emissionCoeff * vt)
//                + saturationCurrentExp / emissionCoeff
//                * MathLib.log(temp / tnom));
//        tSatSWCur = satSWCur * MathLib.exp(((temp / tnom) - 1)
//                * activationEnergy / (emissionCoeff * vt)
//                + saturationCurrentExp / emissionCoeff
//                * MathLib.log(temp / tnom));
//        // Recompute for f1 after temperature adjustment
//        tF1 = tJctPot
//                * (1 - MathLib.exp((1 - tGradingCoeff) * xfc))
//                / (1 - tGradingCoeff);
//        // Recompute for Depletion Capacitance after temperature adjustment
//        tDepCap = depletionCapCoeff * tJctPot;
//        // Recompute for vCrit after temperature adjustment
//        double vte = emissionCoeff * vt;
//        tVcrit = vte * MathLib.log(vte / (ROOT2 * tSatCur * area));
//        // Compute breakdown voltage after temperature adjustment
//        boolean matched = false;
//        double cbv;
//        double xbv;
//        double xcbv = 0;
//        double tol;
//        if (breakdownVoltage != 0) {
//            cbv = breakdownCurrent * area * m;
//            if (cbv < tSatCur * area * m
//                    * breakdownVoltage / vt) {
//                cbv = tSatCur * area * m
//                        * breakdownVoltage / vt;
//                StandardLog.warning(String.format(": breakdown current increased to %g to resolve",
//                        cbv));
//                StandardLog.warning(getInstName());
//                StandardLog.warning("incompatibility with specified saturation current");
//                xbv = breakdownVoltage;
//            } else {
//                tol = env.getRelTol() * cbv;
//                xbv = breakdownVoltage - vt * MathLib.log(1 + cbv
//                        / (tSatCur * area * m));
//                for (int iter = 0; iter < 25; iter++) {
//                    xbv = breakdownVoltage - vt * MathLib.log(cbv
//                            / (tSatCur * area * m) + 1 - xbv / vt);
//                    xcbv = tSatCur * area * m
//                            * (MathLib.exp((breakdownVoltage - xbv) / vt) - 1 + xbv / vt);
//                    if (MathLib.abs(xcbv - cbv) <= tol) {
//                        matched = true;
//                    }
//                    break;
//                }
//                if (!matched) {
//                    StandardLog.warning(String.format(": unable to match forward and reverse diode"
//                            + " regions: bv = %g, ibv = %g",
//                            xbv, xcbv));
//                    StandardLog.warning(getInstName());
//                }
//            }
//            if (matched) {
//                tBrkdwnV = xbv;
//            }
//        }
//        // Transit time temperature adjustment
//        difference = temp - tnom;
//        factor = 1 + (tranTimeTemp1 * difference)
//                + (tranTimeTemp2 * difference * difference);
//        tTransitTime = transitTime * factor;
//        // Series resistance temperature adjustment
//        tConductance = conductance;
//        if (resist != 0) {
//            difference = temp - tnom;
//            factor = 1 + (resistTemp1) * difference
//                    + (resistTemp2 * difference * difference);
//            tConductance = conductance / factor;
//        }
//        tF2 = MathLib.exp((1 + tGradingCoeff) * xfc);
//        tF3 = 1 - depletionCapCoeff * (1 + tGradingCoeff);
//        tF2SW = MathLib.exp((1 + gradingSWCoeff) * xfcs);
//        tF3SW = 1 - depletionSWcapCoeff * (1 + gradingSWCoeff);
        return true;
    }

    public double truncateTimeStep(double timeStep) {
        return stateTable.terr(capChargeStates, capCurrentStates, timeStep);
    }

    public boolean accept(Mode mode) {
        return true;
    }
}
