package com.tejas.client;

import java.util.HashMap;

import com.mxgraph.view.mxGraph;

public class AddNode extends MyCanvas {

    public AddNode(String name) {
        this.getGraph().getModel().beginUpdate();
        Object parent = this.getGraph().getDefaultParent();
        Object v1 = this.getGraph().insertVertex(parent, null, name, 300, 165,
                35, 30);
        this.getM().put(name, v1);
        this.getGraph().getModel().endUpdate();

        // Object node_no = "123";
        // graph.getChildVertices(graph.getDefaultParent());
        // int value = 0;
        // String tmp = (String) node_no;
        // value = Integer.parseInt((String)node_no);
        // System.out.println("no of nodes"+value);
    }

    public void deleteNode() {
        graph.getModel().remove(cell);
    }

}