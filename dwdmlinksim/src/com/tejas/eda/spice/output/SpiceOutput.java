/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.output;

import java.util.Date;

import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.eda.spice.analysis.AnalysisEventListener;
import com.tejas.eda.spice.device.sources.SourceElement;

import javolution.util.FastList;

/**
 *
 * @author owenbad
 */
public abstract class SpiceOutput implements AnalysisEventListener {

    protected String title;
    protected String analName;
    protected String refName;
    protected Date date;
    protected FastList<SourceElement> srcElements = new FastList<SourceElement>();

    public SpiceOutput(String title) {
        this.title = title;
    }

    public void addElement(SourceElement element) {
        srcElements.add(element);
    }

    public abstract void update(AnalysisEvent evt);

    public abstract void beginOutput(AnalysisEvent evt);
}
