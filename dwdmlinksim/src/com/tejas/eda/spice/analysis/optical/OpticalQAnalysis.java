/*
 * DcOpAnalysis.java
 *
 * Created on April 28, 2006, 7:13 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.analysis.optical;


import javolution.util.StandardLog;

/**
 *
 * @author Kristopher T. Beck
 */
public class OpticalQAnalysis extends OpticalAnalysis {

    /** Creates a new instance of DcOpAnalysis */
    public OpticalQAnalysis() {
        name = "Optical Q-Point Analysis";
    }

    public boolean analyze(boolean notUsed) {
        beginOutput();
        if (!doQPointOperations()) {
            StandardLog.severe("Q-point solution failed");
            fireNonConvEvent();
            return false;
        }
//        mode.setModes(initSmSig);
        if (nwk.load(mode)) {
            update();
        } else {
            StandardLog.warning("Error: Network reload failed.");
            return false;
        }
        endOutput();
        return true;
    }
}
