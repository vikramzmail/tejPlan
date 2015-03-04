package com.tejas.client;

import java.util.HashMap;

import javax.swing.JOptionPane;

import com.mxgraph.view.mxGraph;

public class AddLink extends MyCanvas {
    public AddLink() {
        Object parent = this.getGraph().getDefaultParent();
        Object v1 = this.getM().get(JOptionPane.showInputDialog("From Node"));
        Object v2 = this.getM().get(JOptionPane.showInputDialog("To Node"));

        String name = JOptionPane.showInputDialog("Name of the Link");
        this.getGraph().insertEdge(parent, null, name, v1, v2);

    }
}
