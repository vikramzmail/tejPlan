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
public class ACPlotOutput extends PlotOutput {

    public ACPlotOutput(String title) {
        super(title);
        JFrame frame = new JFrame();
        graph = new MultiGraph(title, "frequency", "value", PlotOrientation.HORIZONTAL);
        //graph.setPreferredSize(new Dimension(300, 200));
        frame.add(graph);
        frame.pack();
        frame.setVisible(true);
    }

    public void setLogarithmic() {
        graph.setLogarithmic();
    }
}
