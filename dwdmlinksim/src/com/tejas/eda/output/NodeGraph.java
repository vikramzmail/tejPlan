/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.output;

import java.awt.BorderLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.tejas.eda.node.Node;
import com.tejas.eda.spice.analysis.Analysis;
import com.tejas.eda.spice.analysis.AnalysisEvent;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Numeric;
import com.tejas.math.numbers.Real;

/**
 *
 * @author Kristopher T. Beck
 */
public class NodeGraph extends Graph{

    String nodeName;
    Node node;
    private XYSeries series;

    /**
     * Creates a new NodeGraph object
     */
    public NodeGraph(String nodeName){
        this.nodeName = nodeName;
        this.series = new XYSeries("Data");
        final XYSeriesCollection dataSet = new XYSeriesCollection(this.series);
        chart = ChartFactory.createXYLineChart(nodeName,
                "Time", "Value", dataSet, PlotOrientation.VERTICAL, true,
                true, false);
        ValueAxis axis = chart.getXYPlot().getDomainAxis();
        axis.setAutoRange(true);
        axis.setFixedAutoRange(60000.0);  // 60 seconds
        setValueRange(lowRange, highRange);
        final ChartPanel chartPanel = new ChartPanel(chart);
        this.setLayout(new BorderLayout());
        add(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    }

    public void update(AnalysisEvent evt) {
        if (node == null) {
            Analysis a = (Analysis) evt.getSource();
            node = a.getCircuit().getNode(nodeName);
        }
        Numeric n = node.getValue();
        double value = 0;
        if (n instanceof Real) {
            value = ((Real) n).get();
        } else if (n instanceof Complex) {
            value = ((Complex) n).getReal();
        }
        series.add(evt.getRefValue(), value);
    }

    public void beginOutput(AnalysisEvent evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void endOutput(AnalysisEvent evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void restartOutput(AnalysisEvent evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
