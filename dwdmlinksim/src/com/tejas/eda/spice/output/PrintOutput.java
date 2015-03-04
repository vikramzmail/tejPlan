/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.output;

import java.io.PrintStream;

import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.eda.spice.device.sources.SourceElement;
import com.tejas.math.numbers.Complex;

import javolution.util.FastList;

/**
 *
 * @author owenbad
 */
public abstract class PrintOutput extends SpiceOutput {

    protected PrintStream out;
    protected int index;

    public PrintOutput(String title) {
        super(title);
        out = System.out;
    }

    public PrintOutput(String title, PrintStream out) {
        super(title);
        this.out = out;
    }

    public void setPrintStream(PrintStream out) {
        this.out = out;
    }

    @Override
    public void beginOutput(AnalysisEvent evt) {
        out.println(evt.getAnalysis().getName());
        String str = evt.getRefName();
        if (str != null && !str.isEmpty()) {
            refName = str;
        }
        out.println("------------------");
        out.format("%-10s %-20s ", "Index", refName);
        for (FastList.Node<SourceElement> n = srcElements.head(),
                end = srcElements.tail(); (n = n.getNext()) != end;) {
            SourceElement src = n.getValue();
            Complex c = src.getValue();
            out.format("%-20s ", src.getName());
        }
        out.println();
        index = 0;
    }

    public void endOutput(AnalysisEvent evt) {
        out.flush();
    }

    public void restartOutput(AnalysisEvent evt) {
    }

    @Override
    protected void finalize() throws Throwable {
        out.close();
        super.finalize();
    }
}
