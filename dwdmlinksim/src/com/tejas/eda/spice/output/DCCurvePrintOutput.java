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
public class DCCurvePrintOutput extends PrintOutput {

    public DCCurvePrintOutput(String title) {
        super(title);
        refName = "Sweep";
    }

    public DCCurvePrintOutput(String title, PrintStream out) {
        super(title, out);
    }

    @Override
    public void update(AnalysisEvent evt) {
        out.format("%-10d %-20g", index, evt.refValue);
        for (FastList.Node<SourceElement> n = srcElements.head(),
                end = srcElements.tail(); (n = n.getNext()) != end;) {
            SourceElement src = n.getValue();
            Complex c = src.getValue();
            out.format("%-20g", c.getReal());
        }
        out.println();
        index++;
    }
}
