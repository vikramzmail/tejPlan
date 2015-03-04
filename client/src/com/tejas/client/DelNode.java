package com.tejas.client;

public class DelNode extends MyCanvas {

    public DelNode() {

        this.getGraph().getModel().beginUpdate();
        try {
            this.graph.getModel().remove(currentobj);
        } finally {
            this.getGraph().getModel().endUpdate();
        }
    }
}
