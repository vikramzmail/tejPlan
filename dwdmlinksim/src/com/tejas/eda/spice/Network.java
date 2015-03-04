/*
 * Circuit.java
 *
 * Created on May 31, 2007, 1:39:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice;

import java.util.HashMap;
import java.util.Map;

import com.tejas.eda.node.Node;
import com.tejas.eda.output.Statistics;
import com.tejas.eda.output.StopWatch;
import com.tejas.eda.spice.EnumConsts.MODE;
import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.OpticalInstance;
import com.tejas.eda.spice.device.sources.VoltageInstance;
import com.tejas.eda.spice.node.CurrentNode;
import com.tejas.eda.spice.node.PowerNode;
import com.tejas.eda.spice.node.SpiceNode;
import com.tejas.eda.spice.node.VoltageNode;
import com.tejas.math.numbers.Complex;

import javolution.util.FastList;
import javolution.util.FastTable;
import javolution.util.StandardLog;

/**
 *
 * @author Kristopher T. Beck
 */
public class Network {

    private String name;
    public static final String LOAD = "Load";
    public static final String ACLOAD = "ACLoad";
    protected StopWatch loadTime;
    protected StopWatch acLoadTime;
    protected FastList<OpticalInstance> instances = new FastList();
    protected FastTable<SpiceNode> nodes;
    protected Map powerMap = new HashMap();
    protected Map noiseMap = new HashMap();
    protected Map dispersionMap = new HashMap();
    protected Map lengthMap = new HashMap();
    protected FastList<Node> diags = new FastList();
    public OpticalWorkEnv wrk;
    private Temporal tmprl = new Temporal();
    private NetworkStateTable stateTable;
    protected OpticalEnvVars env;
    protected FastTable<String> nodeNames;
    private boolean nonConverged;

    public Network() {
        env = new OpticalEnvVars();
        wrk = new OpticalWorkEnv();
        stateTable = new NetworkStateTable(this);
        loadTime = Statistics.addTime(LOAD);
        acLoadTime = Statistics.addTime(ACLOAD);
    }

    public OpticalWorkEnv getWrk() {
        return wrk;
    }

    public Temporal getTemporal() {
        return tmprl;
    }

    public OpticalEnvVars getEnv() {
        return env;
    }

    public void setEnv(OpticalEnvVars env) {
        this.env = env;
    }

    public NetworkStateTable getStateTable() {
        return stateTable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FastTable<SpiceNode> getNodes() {
        return nodes;
    }

    public FastList<OpticalInstance> getModels() {
        return instances;
    }

    public double getPowerMap(int key) {
		return Double.parseDouble(powerMap.get(key).toString());
	}

	public void setPowerMap(int key, double value) {
		powerMap.put(key, value);
	}

	public double getNoiseMap(int key) {
		return Double.parseDouble(noiseMap.get(key).toString());
	}

	public void setNoiseMap(int key, double value) {
		noiseMap.put(key, value);
	}
	
	public double getDispersionMap(int key) {
		return Double.parseDouble(dispersionMap.get(key).toString());
	}

	public void setDispersionMap(int key, double value) {
		dispersionMap.put(key, value);
	}
	public double getLengthMap(int key) {
		return Double.parseDouble(lengthMap.get(key).toString());
	}

	public void setLengthMap(int key, double value) {
		lengthMap.put(key, value);
	}
	
	public void addInstance(OpticalInstance inst) {
        instances.add(inst);
    }

    public OpticalInstance findInstance(String name) {
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (m.getValue().getInstName().equals(name)) {
                return m.getValue();
            }
        }
        return null;
    }

    public void initNodes(FastTable<String> nodeTable) {
        nodes = new FastTable<SpiceNode>(nodeTable.size());
        for (int i = 0; i < nodeTable.size(); i++) {
            PowerNode node = PowerNode.valueOf(nodeTable.get(i), i);
            nodes.add(node);
        }
    }

    public Node makeCurrentNode(String base) {
        int i = 1;
        FastTable<String> names = getNodeNames();
        String id = base;
        while (names.contains(id)) {
            id = base + i++;
        }
        CurrentNode n = CurrentNode.valueOf(id, nodes.size());
        nodes.add(n);
        names.add(id);
        return n;
    }

    public Node makeVoltNode(String base) {
        FastTable<String> names = getNodeNames();
        int i = 0;
        String id = base;
        while (names.contains(id)) {
            id = base + ++i;
        }
        VoltageNode n = VoltageNode.valueOf(id, nodes.size());
        nodes.add(n);
        names.add(id);
        return n;
    }
    
    public Node makePowerNode(String base) {
        int i = 1;
        FastTable<String> names = getNodeNames();
        String id = base;
        while (names.contains(id)) {
            id = base + i++;
        }
        PowerNode n = PowerNode.valueOf(id, nodes.size());
        nodes.add(n);
        names.add(id);
        return n;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public Node getNode(int index) {
        return nodes.get(index);
    }

    public Node getNode(String name) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n.getID().equals(name)) {
                return n;
            }
        }
        return null;
    }

    public void deleteNode(int index) {
        nodes.remove(index);
    }

    public FastTable<String> getNodeNames() {
        if (nodeNames == null) {
            nodeNames = new FastTable<String>();
            Node node;
            for (int i = 0; i < nodes.size(); i++) {
                node = nodes.get(i);
                nodeNames.add(node.getID());
            }
        }
        return nodeNames;
    }

    public String getNodeNameAt(int index) {
        return nodes.get(index).getID();
    }

    public int findBranch(String name) {
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            OpticalInstance inst = m.getValue();
            if (inst instanceof VoltageInstance) {
                int j = ((VoltageInstance) inst).findBranch(name);
                if (j != 0) {
                    return j;
                }
            }
        }
        return 0;
    }

    public boolean init() {
//        wrk.setSize(nodes.size());
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            m.getValue().init(this);
        }
        
        //TODO - May be not required for network simulation
//        if (env.isRShuntEnabled()) {
//            for (int i = 0; i < nodes.size(); i++) {
//                Node node = nodes.get(i);
//                if (node instanceof VoltageNode && node.getIndex() != 0) {
//                    diags.add(node);
//                }
//            }
//        }
        wrk.init();
        stateTable.initArrays();
        return true;
    }

    public boolean unSetup() {
        for (int i = 0; i < nodes.size(); i++) {
            SpiceNode node = nodes.get(i);
            if (node.isICGiven() || node.isNSGiven()) {
                node.getValue().setZero();
            }
        }
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (!m.getValue().unSetup()) {
                return false;
            }
        }
        wrk.clear();
        return true;
    }

    public boolean load(String mode) {
        loadTime.start();
        wrk.clearRhs();
        wrk.clearMatrix();
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (!m.getValue().load(mode)) {
    //            StandardLog.warning("Device type " + m.getValue().getInstName() + " nonconvergence");
                return false;
            }
//            System.out.println(wrk.getMatrix());
        }
//        if (env.isRShuntEnabled()) {
//            for (int i = 0; i < diags.size(); i++) {
//                diags.get(i).getValue().plusEq(env.getGShunt());
//            }
        
//        }
//        if (mode.contains(MODE.DC)) {
//            if (mode.contains(MODE.INIT_JCT, MODE.INIT_FIX)) {
//                for (int i = 0; i < nodes.size(); i++) {
//                    SpiceNode node = nodes.get(i);
//                    if (node.isNSGiven()) {
//                        if (zeroNonCurRow(node.getIndex())) {
//                            wrk.setRhsRealAt(node.getIndex(), 1.0e10
//                                    * node.getNS() * wrk.getSrcFact());
//                            node.getValue().setReal(1e10);
//                        } else {
//                            wrk.setRhsRealAt(node.getIndex(),
//                                    node.getNS() * wrk.getSrcFact());
//                            node.getValue().setReal(1);
//                        }
//                    }
//                }
//            }
//            if (mode.contains(MODE.TRANOP) && !mode.isUseIC()) {
//                for (int i = 0; i < nodes.size(); i++) {
//                    SpiceNode node = nodes.get(i);
//                    if (node.isICGiven()) {
//                        if (zeroNonCurRow(node.getIndex())) {
//                            wrk.setRhsRealAt(node.getIndex(), 1.0e10
//                                    * node.getIC().getReal() * wrk.getSrcFact());
//                            node.getValue().plusEq(1.0e10);
//                        } else {
//                            wrk.setRhsRealAt(node.getIndex(), node.getIC().getReal() * wrk.getSrcFact());
//                            node.getValue().setReal(1);
//                        }
//                    }
//                }
//            }
//        }
        loadTime.stop();
        return true;
    }

    public boolean acLoad(Mode mode) {
        acLoadTime.start();
        wrk.clearRhs();
        wrk.clearMatrix();
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            m.getValue().acLoad(mode);
        }
        if (env.isRShuntEnabled()) {
            for (int i = 0; i < diags.size(); i++) {
                diags.get(i).getValue().plusEq(env.getGShunt());
            }
        }
        acLoadTime.stop();
        return true;
    }

    public boolean convTest(String mode) {
        boolean converged = true;
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (!m.getValue().convTest(mode)) {
                converged = false;
            }
        }
        return converged;
    }

    public boolean zeroNonCurRow(int rownum) {
        boolean currents = false;
        for (int i = 0; i < nodes.size(); i++) {
            SpiceNode node = nodes.get(i);
            Complex c = wrk.getElement(rownum, node.getIndex());
            if (c != null) {
                if (node instanceof CurrentNode) {
                    currents = true;
                } else {
                    c.setZero();
                }
            }
        }
        return currents;
    }

    public boolean loadInitCond(Mode mode) {
        SpiceNode node;
        wrk.clearRhs();
        for (int i = 0; i < nodes.size(); i++) {
            node = nodes.get(i);
            if (node.isNSGiven()) {
                node.getValue().set(node.getIC());
                wrk.setRhsRealAt(node.getIndex(), node.getNS());
                env.setHadNodeSet(true);
            }
            if (node.isICGiven()) {
                if (node.getValue().isZero()) {
                    node.getValue().setReal(wrk.getElement(node.getIndex(), node.getIndex()).getReal());
                }
                wrk.setRhsRealAt(node.getIndex(), node.getIC().getReal());
            }
        }
        if (mode.isUseIC()) {
            for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                    (m = m.getNext()) != end;) {
                m.getValue().loadInitCond();
            }
        }
        return true;
    }

    public boolean destroy() {
        return false;
    }

    public boolean temperature() {
        env.setVT(Constants.KoverQ * env.getTemp());
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (!m.getValue().temperature()) {
                return false;
            }
        }
        return true;
    }

    public boolean accept(Mode mode) {
        for (FastList.Node<OpticalInstance> m = instances.head(), end = instances.tail();
                (m = m.getNext()) != end;) {
            if (!m.getValue().accept(mode)) {
                return false;
            }
        }
        /*?PREDICTOR
        int i;
        int error;
        double temp;
        int size;
        double tmp = wrk.sols[7];
        for (int i = 7; i > 0; i--) {
        wrk.sols[i] = wrk.sols[i-1];
        }
        wrk.sols[0]=tmp;
        size = matrix.size();
        for(i=0;i<=size;i++) {
        wrk.sols[0][i]=wrk.rhs[i];
        }
        PREDICTOR */
        return true;
    }

    public boolean isNonConverged() {
        return nonConverged;
    }

    public void setNonConverged(boolean b) {
        nonConverged=b;
    }
}
