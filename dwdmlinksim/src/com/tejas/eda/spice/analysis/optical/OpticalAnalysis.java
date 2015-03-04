/*
 * Analysis.java
 *
 * Created on April 1, 2006, 3:31 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.analysis.optical;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.tejas.eda.node.Node;
import com.tejas.eda.output.Count;
import com.tejas.eda.output.Statistics;
import com.tejas.eda.spice.Circuit;
import com.tejas.eda.spice.EnvVars;
import com.tejas.eda.spice.Mode;
import com.tejas.eda.spice.Network;
import com.tejas.eda.spice.NetworkStateTable;
import com.tejas.eda.spice.OpticalEnvVars;
import com.tejas.eda.spice.OpticalWorkEnv;
import com.tejas.eda.spice.StateTable;
import com.tejas.eda.spice.Temporal;
import com.tejas.eda.spice.WorkEnv;
import com.tejas.eda.spice.EnumConsts.DOMAIN;
import com.tejas.eda.spice.EnumConsts.MODE;
import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.eda.spice.analysis.AnalysisEventListener;
import com.tejas.eda.spice.analysis.MultiSetAnalysisEvent;
import com.tejas.eda.spice.analysis.NonConvergenceListener;
import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.sources.isrcs.ISrcInstance;
import com.tejas.eda.spice.device.sources.vsrcs.VSrcInstance;
import com.tejas.eda.spice.node.SpiceNode;
import com.tejas.eda.spice.node.VoltageNode;
import com.tejas.math.matrix.MatrixException;
import com.tejas.math.matrix.SingularException;

import javolution.lang.MathLib;
import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastTable;
import javolution.util.StandardLog;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class OpticalAnalysis {

	protected String name;
	protected String desc;
	protected FastList<OpticalAnalysisEventListener> listeners = new FastList<OpticalAnalysisEventListener>();
	protected FastList<OpticalNonConvergenceListener> nclisteners = new FastList<OpticalNonConvergenceListener>();
	protected Network nwk;
	protected OpticalWorkEnv wrk;
	protected OpticalEnvVars env;
	protected NetworkStateTable stateTable;
	protected Temporal tmprl;
	protected String mode;
	public static final String ITERATIONS = "ITERATIONS";
	protected Count iters;
	//Count defaultIters = new Count();
	protected double[] state0;
	boolean shouldReorder;
	boolean reordered;
	boolean uninitialized;
	boolean acShouldReordered;
	boolean acReordered;
	boolean acUninitialized;
	boolean didPreorder;
	boolean pzShouldReorder;
	protected boolean lastIterCall;
	protected boolean pause;
	protected DOMAIN domain = DOMAIN.NONE;
	protected boolean doIC;
	private static final EnumSet<MODE> jctMode = EnumSet.of(MODE.DCOP, MODE.INIT_JCT);
	private static final EnumSet<MODE> floatMode = EnumSet.of(MODE.DCOP, MODE.INIT_FLOAT);
	public static final EnumSet<MODE> initSmSig = EnumSet.of(MODE.DCOP, MODE.INIT_SMSIG);

	public boolean init(Network nwk) {
		this.nwk = nwk;
		env = nwk.getEnv();
		wrk = nwk.wrk;
		stateTable = nwk.getStateTable();
		tmprl = nwk.getTemporal();
		iters = Statistics.addCount(ITERATIONS);
		mode = "";
		return true;
	}

	public String getName() {
		return name;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public Network getNetwork() {
		return nwk;
	}

	public OpticalWorkEnv getWrk() {
		return wrk;
	}

	public void addAnalysisEventListener(OpticalAnalysisEventListener listener) {
		listeners.add(listener);
	}

	public void addNonConverganceEventListener(OpticalNonConvergenceListener listener) {
		nclisteners.add(listener);
	}

	public void setDomain(DOMAIN domain) {
		this.domain = domain;
	}

	public DOMAIN getDomain() {
		return domain;
	}

	public void beginOutput() {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		fireBeginOutputEvent(evt);
	}

	public void beginOutput(String refName) {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		evt.refName = refName;
		fireBeginOutputEvent(evt);
	}

	public void beginOutput(String name, List<String> varIds) {
		OpticalMultiSetAnalysisEvent evt = new OpticalMultiSetAnalysisEvent(this);
		//TODO transfunc needs work
		evt.setDataIds(varIds);
		evt.refName = name;
		fireBeginOutputEvent(evt);
	}

	public void restartOutput() {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		fireRestartOutputEvent(evt);
	}

	public void endOutput() {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		fireEndOutputEvent(evt);
	}

	public abstract boolean analyze(boolean restart);

	public boolean doQPointOperations()	{
		boolean powerStatus = doOperations("power", env.getqPointMaxIter());
//		boolean noiseStatus = doOperations("noise", env.getqPointMaxIter());
//		boolean dispersionStatus = doOperations("dispersion", env.getqPointMaxIter());
//		return (powerStatus && noiseStatus && dispersionStatus);
		return powerStatus;
	}

	//    public boolean doDCOperations() {
	//        return doOperations(jctMode, floatMode, env.getDcMaxIter());
	//    }

	public boolean doACOperations(int iterLim) {
		return doOperations(null, iterLim);
	}

	public boolean doOperations(String mode, int iterlim) {
		boolean converged = false;
		//        mode.setModes(first);
		if (!env.isNoOpIter()) {
			if (env.getNumGMinSteps() <= 0 && env.getNumSrcSteps() <= 0) {
				lastIterCall = true;
			} else {
				lastIterCall = false;
			}
			converged = iterate(iterlim);
		}

		//TODO - To be checked for convergence - Anant
		//        if (!converged) {
		//            if (env.getNumGMinSteps() >= 1) {
		//                if (env.getNumGMinSteps() == 1) {
		//                    if (dynamicGMin(first, cont, iterlim)) {
		//                        return true;
		//                    }
		//                } else if (spice3GMin(first, cont, iterlim)) {
		//                    return true;
		//                }
		//            }
		//            if (env.getNumSrcSteps() >= 1) {
		//                if (env.getNumSrcSteps() == 1) {
		//                    if (gillespieSrc(first, cont, iterlim)) {
		//                        lastIterCall = false;
		//                        return true;
		//                    }
		//                } else if (spice3Src(first, cont, iterlim)) {
		//                    lastIterCall = false;
		//                    return true;
		//                }
		//            }
		//        }
		return converged;
	}

	//    public boolean dynamicGMin(EnumSet<MODE> first, EnumSet<MODE> cont, int iterlim) {
	//        boolean converged = false;
	//        double[] oldRhsOld = new double[wrk.getSize()];
	//        mode.setModes(first);
	//        StandardLog.info("Starting dynamic GMin stepping");
	//        double oldState0[] = null;
	//        wrk.clearRhsOld();
	//        stateTable.clearColumn(0);
	//        double factor = env.getGMinFactor();
	//        double oldGMin = 1e-2;
	//        env.setDiagGMin(oldGMin / factor);
	//        double gTarget = MathLib.max(env.getGMin(), env.getGShunt());
	//        boolean success = false;
	//        boolean failed = false;
	//        while (!success && !failed) {
	//            StandardLog.info("Trying GMin = " + env.getDiagGMin());
	//            int _iters = iters.getCount();
	//            converged = iterate(env.getDcTrcvMaxIter());
	//            _iters = iters.getCount() - _iters;
	//            if (converged) {
	//                mode.setModes(cont);
	//                StandardLog.info("One successful GMin step");
	//                if (env.getDiagGMin() <= gTarget) {
	//                    success = true;
	//                } else {
	//                    for (int i = 0; i < wrk.getSize(); i++) {
	//                        oldRhsOld[i] = wrk.getRhsOldRealAt(i);
	//                    }
	//                    oldState0 = Arrays.copyOf(stateTable.getColumnAt(0), stateTable.getSize());
	//                    if (_iters <= (env.getDcTrcvMaxIter() / 4)) {
	//                        factor *= MathLib.sqrt(factor);
	//                        if (factor > env.getGMinFactor()) {
	//                            factor = env.getGMinFactor();
	//                        }
	//                    }
	//                    if (_iters > (3 * env.getDcTrcvMaxIter() / 4)) {
	//                        factor = MathLib.sqrt(factor);
	//                    }
	//                    oldGMin = env.getDiagGMin();
	//                    if (env.getDiagGMin() < (factor * gTarget)) {
	//                        factor = env.getDiagGMin() / gTarget;
	//                        env.setDiagGMin(gTarget);
	//                    } else {
	//                        env.setDiagGMin(env.getDiagGMin() / factor);
	//                    }
	//                }
	//            } else {
	//                if (factor < 1.00005) {
	//                    failed = true;
	//                    StandardLog.warning("Last GMin step failed");
	//                } else {
	//                    factor = MathLib.sqrt(MathLib.sqrt(factor));
	//                    env.setDiagGMin(oldGMin / factor);
	//                    for (int i = 0; i < wrk.getSize(); i++) {
	//                        wrk.setRhsOldRealAt(i, oldRhsOld[i]);
	//                    }
	//                    if (oldState0 != null) {
	//                        stateTable.setColumnAt(0, oldState0);
	//                    }
	//                }
	//            }
	//        }
	//        oldRhsOld = null;
	//        oldState0 = null;
	//        env.setDiagGMin(env.getGShunt());
	//        if (env.getNumSrcSteps() <= 0) {
	//            lastIterCall = true;
	//        } else {
	//            lastIterCall = false;
	//        }
	//        if (!iterate(iterlim)) {
	//            StandardLog.warning("Dynamic GMin stepping failed");
	//            return false;
	//        }
	//        StandardLog.info("Dynamic GMin stepping completed");
	//        lastIterCall = false;
	//        return true;
	//    }
	//
	//    public boolean spice3GMin(EnumSet<MODE> first, EnumSet<MODE> cont, int iterlim) {
	//        mode.setModes(first);
	//        double diagGMin;
	//        StandardLog.info("Starting GMin stepping");
	//        if (env.getGShunt() == 0) {
	//            diagGMin = env.getGMin();
	//        } else {
	//            diagGMin = env.getGShunt();
	//        }
	//        for (int i = 0; i < env.getNumGMinSteps(); i++) {
	//            diagGMin *= env.getGMinFactor();
	//        }
	//        env.setDiagGMin(diagGMin);
	//        for (int i = 0; i <= env.getNumGMinSteps(); i++) {
	//            StandardLog.info(String.format("Trying GMin = %12.4E ",
	//                    env.getDiagGMin()));
	//            if (!iterate(env.getDcTrcvMaxIter())) {
	//                env.setDiagGMin(env.getGShunt());
	//                StandardLog.warning("GMin step failed");
	//                break;
	//            }
	//            env.setDiagGMin(env.getDiagGMin() / env.getGMinFactor());
	//            StandardLog.info("One successful GMin step");
	//            mode.setModes(cont);
	//        }
	//        env.setDiagGMin(env.getGShunt());
	//        if (env.getNumSrcSteps() <= 0) {
	//            lastIterCall = true;
	//        } else {
	//            lastIterCall = false;
	//        }
	//
	//        if (iterate(iterlim)) {
	//            StandardLog.info("GMin stepping completed");
	//            lastIterCall = false;
	//            return true;
	//        }
	//        StandardLog.warning("GMin stepping failed");
	//        return false;
	//
	//    }
	//
	//    public boolean gillespieSrc(EnumSet<MODE> first, EnumSet<MODE> cont, int iterlim) {
	//        //int iters;
	//        mode.setModes(first);
	//        StandardLog.info("Starting source stepping");
	//        wrk.setSrcFact(0);
	//        double raise = 0.001;
	//        double convFact = 0;
	//        boolean converged = false;
	//        double[] oldRhsOld = new double[wrk.getSize()];
	//        double[] oldState0 = null;
	//        wrk.clearRhsOld();
	//        stateTable.clearColumn(0);
	//        StandardLog.info(String.format("Supplies reduced to %8.4f ",
	//                wrk.getSrcFact() * 100));
	//        if (!iterate(env.getDcTrcvMaxIter())) {
	//            if (env.getGShunt() <= 0) {
	//                env.setDiagGMin(env.getGMin());
	//            } else {
	//                env.setDiagGMin(env.getGShunt());
	//            }
	//            env.setDiagGMin(env.getDiagGMin() * 10000000000.0);
	//            for (int i = 0; i <= 10; i++) {
	//                StandardLog.info(String.format("Trying env.getGMin() ="
	//                        + " %12.4E ", env.getDiagGMin()));
	//                lastIterCall = true;
	//                if (!iterate(env.getDcTrcvMaxIter())) {
	//                    env.setDiagGMin(env.getGShunt());
	//                    StandardLog.warning("GMin step failed");
	//                    lastIterCall = false;
	//                    converged = false;
	//                    break;
	//                }
	//                env.setDiagGMin(env.getDiagGMin() / 10);
	//                mode.setModes(cont);
	//                StandardLog.info("One successful GMin step");
	//                converged = true;
	//            }
	//            env.setDiagGMin(env.getGShunt());
	//        }
	//        if (converged) {
	//            for (int i = 0; i < wrk.getSize(); i++) {
	//                oldRhsOld[i] = wrk.getRhsOldRealAt(i);
	//            }
	//            oldState0 = Arrays.copyOf(stateTable.getColumnAt(0), stateTable.getSize());
	//            StandardLog.info("One successful source step");
	//            wrk.setSrcFact(convFact + raise);
	//            do {
	//                StandardLog.info("Supplies reduced to %8.4f "
	//                        + wrk.getSrcFact() * 100);
	//                int _iters = iters.getCount();
	//                lastIterCall = true;
	//                converged = iterate(env.getDcTrcvMaxIter());
	//                _iters = iters.getCount() - _iters;
	//                mode.setModes(cont);
	//                if (converged) {
	//                    convFact = wrk.getSrcFact();
	//                    for (int i = 0; i < nwk.getNodeCount(); i++) {
	//                        oldRhsOld[i] = wrk.getRhsOldRealAt(i);
	//                    }
	//                    for (int i = 0; i < stateTable.getSize(); i++) {
	//                        oldState0[i] = stateTable.getStateAt(0, i);
	//                    }
	//                    StandardLog.info("One successful source step");
	//                    wrk.setSrcFact(convFact + raise);
	//                    if (iters.getCount() <= (env.getDcTrcvMaxIter() / 4)) {
	//                        raise = raise * 1.5;
	//                    }
	//                    if (iters.getCount() > (3 * env.getDcTrcvMaxIter() / 4)) {
	//                        raise = raise * 0.5;
	//                    }
	//                } else {
	//                    if ((wrk.getSrcFact() - convFact) < 1e-8) {
	//                        break;
	//                    }
	//                    raise = raise / 10;
	//                    if (raise > 0.01) {
	//                        raise = 0.01;
	//                    }
	//                    wrk.setSrcFact(convFact);
	//                    for (int i = 0; i < wrk.getSize(); i++) {
	//                        wrk.setRhsOldRealAt(i, oldRhsOld[i]);
	//                    }
	//                    for (int i = 0; i < stateTable.getSize(); i++) {
	//                        stateTable.setStateAt(i, 0, oldState0[i]);
	//                    }
	//                }
	//                if (wrk.getSrcFact() > 1) {
	//                    wrk.setSrcFact(1);
	//                }
	//            } while ((raise >= 1e-7) && (convFact < 1));
	//        }
	//        wrk.setSrcFact(1);
	//        if (convFact != 1) {
	//            wrk.setSrcFact(1);
	//            StandardLog.warning("Source stepping failed");
	//        } else {
	//            StandardLog.info("Source stepping completed");
	//            return true;
	//        }
	//        return false;
	//    }
	//
	//    public boolean spice3Src(EnumSet<MODE> first, EnumSet<MODE> cont, int iterlim) {
	//        mode.setModes(first);
	//        StandardLog.info("Starting source stepping");
	//        for (int i = 0; i <= env.getNumSrcSteps(); i++) {
	//            wrk.setSrcFact(((double) i) / ((double) env.getNumSrcSteps()));
	//            if (!iterate(env.getDcTrcvMaxIter())) {
	//                wrk.setSrcFact(1);
	//                StandardLog.warning("Source stepping failed");
	//                lastIterCall = false;
	//                return false;
	//            }
	//            lastIterCall = true;
	//            StandardLog.info("One successful source step");
	//            mode.setModes(cont);
	//        }
	//        StandardLog.info("Source stepping completed");
	//        wrk.setSrcFact(1);
	//        return true;
	//    }

	public boolean convTest() {
		SpiceNode node;
		double tol;
		FastTable<SpiceNode> nodes = nwk.getNodes();
		
		// For noise only
		for (int i = 1; i < nodes.size(); i++) {
			node = nodes.get(i);
			double old = nwk.getDispersionMap(i-1);
			double cur = wrk.getRhsRealAt(i-1);
			if (node instanceof VoltageNode) {
				tol = env.getRelTol() * MathLib.max(MathLib.abs(old),
						MathLib.abs(cur)) + env.getVoltTol();
			} else {
				tol = env.getRelTol() * MathLib.max(MathLib.abs(old),
						MathLib.abs(cur)) + env.getAbsTol();
			}
			if (MathLib.abs(cur - old) > tol) {
				trouble(node, null, "Non-convergence at node " + node.getID());
				return false;
			}
		}
		
		return nwk.convTest(mode);
	}

	public boolean iterate(int maxIter) {
		int iterNo = iters.getCount();
		int oldIters = iters.getCount();
		if (maxIter < 100) {
			maxIter = 100;
		}
		double[] oldState0 = null;
		if (uninitialized) {
			if (!reInit()) {
				StandardLog.warning("Re-init returned error");
				return false;
			}
			uninitialized = false;
		}
		while (true) {
			nwk.setNonConverged(false);
			if (nwk.load("power")) {
				iterNo = iters.increment() - oldIters;
			} else {
				StandardLog.warning("Load returned error");
				oldState0 = null;
				return false;
			}

			if (iterNo == 1) {
				shouldReorder = true;
			}
			if (shouldReorder) {
				wrk.factor();
				shouldReorder = false;
			} else {
				try {
					wrk.factor();
				} catch (SingularException ex) {
					shouldReorder = true;
					StandardLog.warning("Forced reordering...");
					continue;
				} catch (MatrixException ex) {
					StandardLog.warning("LU Factor returned error");
					oldState0 = null;
					return false;
				}
			}
			if (oldState0 == null) {
				oldState0 = Arrays.copyOf(stateTable.getColumnAt(0), stateTable.getSize());
			}

			wrk.solve();

			if (iters.getCount() - oldIters > maxIter) {
				StandardLog.warning("Iteration limit exceeded");
				oldState0 = null;
				return false;
			}
			wrk.advanceRhs();
			for(int i = 0; i < wrk.getSize(); i++){
				nwk.setPowerMap(i,wrk.getRhsOldRealAt(i));
			}
			
			// Clean-up of all power calculation data
			wrk.clearMatrix();
			wrk.clearRhs();
			wrk.clearRhsOld();
			
			// Start of noise calculation
			nwk.setNonConverged(false);
			if (nwk.load("noise")) {
//				iterNo = iters.increment() - oldIters;
			} else {
				StandardLog.warning("Load returned error");
				oldState0 = null;
				return false;
			}

			if (iterNo == 1) {
				shouldReorder = true;
			}
			if (shouldReorder) {
				wrk.factor();
				shouldReorder = false;
			} else {
				try {
					wrk.factor();
				} catch (SingularException ex) {
					shouldReorder = true;
					StandardLog.warning("Forced reordering...");
					continue;
				} catch (MatrixException ex) {
					StandardLog.warning("LU Factor returned error");
					oldState0 = null;
					return false;
				}
			}
			if (oldState0 == null) {
				oldState0 = Arrays.copyOf(stateTable.getColumnAt(0), stateTable.getSize());
			}

			wrk.solve();

			if (iters.getCount() - oldIters > maxIter) {
				StandardLog.warning("Iteration limit exceeded");
				oldState0 = null;
				return false;
			}
			wrk.advanceRhs();
			for(int i = 0; i < wrk.getSize(); i++){
				nwk.setNoiseMap(i,wrk.getRhsOldRealAt(i));
			}
			
			// Clean-up of all noise calculation data
			wrk.clearMatrix();
			wrk.clearRhs();
			wrk.clearRhsOld();
			
			// Start of dispersion calculation
			nwk.setNonConverged(false);
			if (nwk.load("dispersion")) {
//				iterNo = iters.increment() - oldIters;
			} else {
				StandardLog.warning("Load returned error");
				oldState0 = null;
				return false;
			}

			if (iterNo == 1) {
				shouldReorder = true;
			}
			if (shouldReorder) {
				wrk.factor();
				shouldReorder = false;
			} else {
				try {
					wrk.factor();
				} catch (SingularException ex) {
					shouldReorder = true;
					StandardLog.warning("Forced reordering...");
					continue;
				} catch (MatrixException ex) {
					StandardLog.warning("LU Factor returned error");
					oldState0 = null;
					return false;
				}
			}
			if (oldState0 == null) {
				oldState0 = Arrays.copyOf(stateTable.getColumnAt(0), stateTable.getSize());
			}

			wrk.solve();

			if (iters.getCount() - oldIters > maxIter) {
				StandardLog.warning("Iteration limit exceeded");
				oldState0 = null;
				return false;
			}
			if (!nwk.isNonConverged() && iterNo != 1) {
				nwk.setNonConverged(!convTest());
			} else {
				nwk.setNonConverged(true);
			}

			if (!nwk.isNonConverged()) {
				oldState0 = null;
				return true;
			}
			wrk.advanceRhs();
			for(int i = 0; i < wrk.getSize(); i++){
				nwk.setDispersionMap(i,wrk.getRhsOldRealAt(i));
//				nwk.setLengthMap(i, value);
			}
		}
	}

	//    public boolean acIterate() {
	//        while (true) {
	//            if (!nwk.acLoad(mode)) {
	//                return false;
	//            }
	//            if (acShouldReordered) {
	//                try {
	//                    if (!wrk.reorder(env.getPivotAbsTol(), env.getPivotRelTol())) {
	//                        acShouldReordered = false;
	//                        return false;
	//                    }
	//                } catch (MatrixException ex) {
	//                    StandardLog.severe(ex.getLocalizedMessage());
	//                }
	//            } else {
	//                try {
	//                    if (!wrk.factor(env.getPivotAbsTol())) {
	//                        return false;
	//                    }
	//                } catch (SingularException ex) {
	//                    acShouldReordered = true;
	//                    continue;
	//                } catch (Exception ex) {
	//                    return false;
	//                }
	//            }
	//            break;
	//        }
	//        wrk.solve();
	//
	//        wrk.getRhsAt(0).setZero();
	//        wrk.getRhsOldAt(0).setZero();
	//        //wrk.getC(0).setZero();
	//
	//        wrk.advanceRhs();
	//        return true;
	//    }

	public boolean reInit() {
		//TODO fix make sure arrays and matrix are the right size and initialized!!!
		//oldState0 = new double[wrk.getSize()][numStates];
		//
		//    for (int i = 0; i < 8; i++) {
		//}
		//  shouldReorder = true;
		//        acShouldReordered = true;
		//        pzShouldReorder = true;
		return true;
	}

	public void update() {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		fireUpdateEvent(evt);
	}

	public void update(double refValue) {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this, refValue);
		fireUpdateEvent(evt);
	}

	public void fireUpdateEvent(OpticalAnalysisEvent evt) {
		for (FastList.Node<OpticalAnalysisEventListener> n = listeners.head(), end = listeners.tail();
				(n = n.getNext()) != end;) {
			n.getValue().update(evt);
		}
	}

	public void fireBeginOutputEvent(OpticalAnalysisEvent evt) {
		for (FastList.Node<OpticalAnalysisEventListener> n = listeners.head(), end = listeners.tail();
				(n = n.getNext()) != end;) {
			n.getValue().beginOutput(evt);
		}
	}

	public void fireRestartOutputEvent(OpticalAnalysisEvent evt) {
		for (FastList.Node<OpticalAnalysisEventListener> n = listeners.head(), end = listeners.tail();
				(n = n.getNext()) != end;) {
			n.getValue().restartOutput(evt);
		}
	}

	public void fireEndOutputEvent(OpticalAnalysisEvent evt) {
		for (FastList.Node<OpticalAnalysisEventListener> n = listeners.head(), end = listeners.tail();
				(n = n.getNext()) != end;) {
			n.getValue().endOutput(evt);
		}
	}

	public String trouble(Node node, Instance instance, String optMsg) {
		TextBuilder msg = new TextBuilder();
		msg.append(getName());
		if (optMsg != null && optMsg.equals("")) {
			msg.append(optMsg);
		}
		switch (getDomain()) {
		case TIME:
			if (tmprl.getTime() == 0.0) {
				msg.append("Initial timepoint: ");
			} else {
				msg.append("Time = " + tmprl.getTime() + ": timestep = " + tmprl.getDelta());
			}
			break;
		case FREQUENCY:
			double d = (tmprl.getOmega() / (2.0 * Math.PI));
			msg.append("Frequency = " + d);
			break;
		case SWEEP:
			//TODO This should be checked during sweeping - Anant
			//                if (this instanceof DcTrCurv) {
			//                    DcTrCurv tc = (DcTrCurv) this;
			//                    for (int i = 0; i <= tc.nestLevel; i++) {
			//                        if (tc.trvcs[i].inst.getClass() == VSrcInstance.class) {
			//                            msg.append(tc.trvcs[i].name + " = "
			//                                    + ((VSrcInstance) tc.trvcs[i].inst).getDcValue());
			//                        } else {
			//                            msg.append(tc.trvcs[i].name + " = "
			//                                    + ((ISrcInstance) tc.trvcs[i].inst).getDcValue());
			//                        }
			//                    }
			//                }
			break;
		case NONE:
		default:
			break;
		}
		if (node != null) {
			msg.append("Trouble with node " + node.getID());
		} else if (instance != null) {
			msg.append("Trouble with instance: " + instance.getInstName());
		} else {
			msg.append("Cause unrecorded.");
		}

		return msg.toString();
	}

	void displayNames() {
		for (int i = 0; i < nwk.getNodeCount(); i++) {
			StandardLog.info(i + " " + nwk.getNode(i).getID());
		}
	}

	public void fireNonConvEvent() {
		OpticalAnalysisEvent evt = new OpticalAnalysisEvent(this);
		for (FastList.Node<OpticalNonConvergenceListener> n = nclisteners.head(), end = nclisteners.tail();
				(n = n.getNext()) != end;) {
			n.getValue().nonConvergence(evt);
		}
	}

	/*
	 *
    public boolean dIter(){
    double[] temp;
    ckt.setConverged(false);
    while(true){
    if(acShouldReordered) {
    try {
    acShouldReordered = false;
    wrk.reorder(env.getPivotAbsTol(), env.getPivotRelTol());
    break;
    } catch (SingularException ex) {
    Logger.getLogger("global").log(Level.SEVERE, null, ex);
    } catch (MatrixException ex) {
    Logger.getLogger("global").log(Level.SEVERE, null, ex);
    }
    } else {
    try{
    wrk.factor(env.getPivotAbsTol());
    } catch(SingularException ex){
    acShouldReordered = true;
    continue;
    } catch(Exception ex){
    return false;
    }
    break;
    }
    ckt.setConverged(true);
    wrk.swapAnC();
    ckt.ACLOAD();
    wrk.swapAnC();
    }
    wrk.solve();
    wrk.getRhsAt(0).setZero();
    wrk.getRhsOld(0).setZero();
    wrk.getC(0).setZero();
    wrk.advanceRhs();
    return true;
    }

    public void nzIter(int posDrive, int negDrive) {
    wrk.clearRhs();
    wrk.setRhsRealAt(posDrive, 1.0);
    wrk.setRhsImagAt(negDrive, -1.0);
    wrk.solve();
    wrk.getRhsAt(0).setZero();
    }

    int pzTrapped;

    void normEq(double x1, double x2){
    x1 = Math.sqrt(x1 * x1 + x2 * x2);
    }
    void zaddeq(double a, int amag, double x, int xmag, double y, int ymag) {
    if (xmag > ymag) {
    amag = xmag;
    if (xmag > 50 + ymag)
    y = 0.0;
    else
    for (xmag -= ymag; xmag > 0; xmag--)
    y /= 2.0;
    } else {
    amag = ymag;
    if (ymag > 50 + xmag)
    x = 0.0;
    else
    for (ymag -= xmag; ymag > 0; ymag--)
    x /= 2.0;
    }
    a = x + y;
    if (a == 0.0)
    amag = 0;
    else {
    while (MathLib.abs(a) > 1.0) {
    a /= 2.0;
    amag += 1;
    }
    while (MathLib.abs(a) < 0.5) {
    a *= 2.0;
    amag -= 1;
    }
    }
    }
	 */
	public static boolean approx(double a, double b, int maxUlps) {
		long aInt = Double.doubleToLongBits(a);
		if (aInt < 0) {
			aInt = 0x8000000000000000L - aInt;
		}
		long bInt = Double.doubleToLongBits(b);
		if (bInt < 0) {
			bInt = 0x8000000000000000L - bInt;
		}
		long intDiff = Math.abs(aInt - bInt);
		if (intDiff <= maxUlps) {
			return true;
		}
		return false;
	}
}
