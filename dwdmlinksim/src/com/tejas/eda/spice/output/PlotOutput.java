/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.output;

import com.tejas.eda.output.MultiGraph;
import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.eda.spice.device.sources.SourceElement;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.util.FastList;

/**
 *
 * @author owenbad
 */
public class PlotOutput extends SpiceOutput {

    MultiGraph graph;

    public PlotOutput(String title) {
        super(title);
    }

    public void update(AnalysisEvent evt) {
        FastList<Real> data = FastList.newInstance();
        for (FastList.Node<SourceElement> n = srcElements.head(),
                end = srcElements.tail(); (n = n.getNext()) != end;) {
            SourceElement src = n.getValue();
            Complex c = src.getValue();
            data.add(Real.valueOf(c.getReal()));
        }
        graph.update(evt.refValue, data);
    }

    public void beginOutput(AnalysisEvent evt) {
        String str = evt.getRefName();
        if (str != null && !str.isEmpty()) {
            refName = str;
            graph.setxAxisLabel(refName);
        }
        for (FastList.Node<SourceElement> n = srcElements.head(),
                end = srcElements.tail(); (n = n.getNext()) != end;) {
            SourceElement src = n.getValue();
            graph.addDataSet(src.getName());
        }
    }

    public void endOutput(AnalysisEvent evt) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void restartOutput(AnalysisEvent evt) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
