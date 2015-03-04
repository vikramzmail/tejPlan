/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.output;

import java.io.PrintStream;

import com.tejas.eda.node.Node;
import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.EnvVars;
import com.tejas.eda.spice.Network;
import com.tejas.eda.spice.OpticalWorkEnv;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.eda.spice.analysis.NonConvergenceListener;
import com.tejas.eda.spice.analysis.optical.OpticalAnalysisEvent;
import com.tejas.eda.spice.analysis.optical.OpticalNonConvergenceListener;
import com.tejas.eda.spice.node.VoltageNode;
import com.tejas.math.numbers.Complex;

import javolution.lang.MathLib;
import javolution.util.FastTable;
import javolution.util.StandardLog;

/**
 *
 * @author Kristopher T. Beck
 */
public class OpticalDefaultOutput extends OpticalSpiceOutput implements OpticalNonConvergenceListener {

    protected PrintStream out;
    protected PrintStream ncOut;
    protected FastTable<String> nodeNames;

    public OpticalDefaultOutput(String title) {
        super(title);
        ncOut = System.err;
        out = System.out;
    }

    public void setNcOut(PrintStream ncOut) {
        this.ncOut = ncOut;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void update(OpticalAnalysisEvent evt) {
        OpticalWorkEnv wrk = evt.getAnalysis().getWrk();
        Network nwk = evt.getAnalysis().getNetwork();
        out.println("Node Powers");
        out.println("------------------");
        out.format("%-30s %-20s %-20s %-20s %-20s\n", "Node", "Power (in dBm)","Optical Noise (in dBm)","OSNR (in dB)","Dispersion (in ps/nm)");
        for (int i = 0; i < wrk.getSize(); i++) {
            out.format("%-30s %-20s %-20s %-20s %-20s\n", nodeNames.get(i), (10*Math.log10(nwk.getPowerMap(i))), (10*Math.log10(nwk.getNoiseMap(i))), (10*Math.log10(nwk.getPowerMap(i)/nwk.getNoiseMap(i))), nwk.getDispersionMap(i));
        }
    }

    public void nonConvergence(AnalysisEvent evt) {
        Circuit ckt = evt.getAnalysis().getCircuit();
        WorkEnv wrk = ckt.getWrk();
        EnvVars env = ckt.getEnv();
        ncOut.println("Last Complex Voltages");
        ncOut.println("------------------");
        ncOut.format("%-30s %-20s %-20s\n", "Node", "Last Voltage", "Previous Iter");
        nodeNames = ckt.getNodeNames();
        for (int i = 1; i < nodeNames.size(); i++) {
            Node node = ckt.getNode(i);
            double tol;
            if (node.getID().startsWith("#branch") || !node.getID().startsWith("#")) {
                double newd = wrk.getRhsOldRealAt(i);
                double old = wrk.getRhsRealAt(i);
                ncOut.format("\n%-30s %-20g %-20g", node.getID(), newd, old);
                if (node instanceof VoltageNode) {
                    tol = env.getRelTol() * (MathLib.max(MathLib.abs(old),
                            MathLib.abs(newd))) + env.getVoltTol();
                } else {
                    tol = env.getRelTol() * (MathLib.max(MathLib.abs(old),
                            MathLib.abs(newd))) + env.getAbsTol();
                }
                if (MathLib.abs(newd - old) > tol) {
                    ncOut.print(" *");
                }
            }
        }
    }

    public void acUpdate(AnalysisEvent evt) {
        Circuit ckt = evt.getAnalysis().getCircuit();
        WorkEnv wrk = ckt.getWrk();
        StandardLog.info("Node Voltages");
        StandardLog.info("------------------");
        out.format("%-30s %-20g %-20g", "Node", "Voltage real", "imaginary");
        for (int i = 1; i < wrk.getSize(); i++) {
            Complex c = wrk.getRhsOldAt(i);
            out.format("%-30s %-20g %-20g", nodeNames.get(i), c.getReal(), c.getImag());
        }
    }

    public void beginOutput(OpticalAnalysisEvent evt) {
        Network nwk = evt.getAnalysis().getNetwork();
        nodeNames = nwk.getNodeNames();
        if (out == null) {
            out = System.out;
        }
        if (ncOut == null) {
            ncOut = System.err;
        }
    }

    public void endOutput(AnalysisEvent evt) {
    }

    public void restartOutput(AnalysisEvent evt) {
    }

	@Override
	public void update(AnalysisEvent evt) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public void beginOutput(OpticalAnalysisEvent evt) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void endOutput(OpticalAnalysisEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restartOutput(OpticalAnalysisEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nonConvergence(OpticalAnalysisEvent evt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beginOutput(AnalysisEvent evt) {
		// TODO Auto-generated method stub
		
	}
}
