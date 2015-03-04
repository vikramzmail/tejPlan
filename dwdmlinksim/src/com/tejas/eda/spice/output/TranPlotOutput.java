/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.output;

import javax.swing.JFrame;
import org.jfree.chart.plot.PlotOrientation;

import com.tejas.eda.output.MultiGraph;

/**
 *
 * @author owenbad
 */
public class TranPlotOutput extends PlotOutput {

    public TranPlotOutput(String title) {
        super(title);
        JFrame frame = new JFrame();
        graph = new MultiGraph(title, "Time", "Value", PlotOrientation.HORIZONTAL);
        frame.add(graph);
        frame.pack();
        frame.setVisible(true);
    }
}
