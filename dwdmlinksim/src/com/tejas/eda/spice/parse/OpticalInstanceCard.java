/*
 * InstanceCard.java
 *
 * Created on Oct 14, 2007, 1:55:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.OpticalInstance;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class OpticalInstanceCard extends Card {

    protected String instName;
    protected String modelName;
    protected int nodeCount;
    protected String[] nodes;
    protected String prefix;

    public OpticalInstanceCard() {
    }

    public OpticalInstanceCard(String cardString) throws ParserException {
        super(cardString);
    }

    public String getInstName() {
        return instName;
    }

    public void setInstName(String instName) {
        this.instName = instName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public String[] getNodes() {
        if (nodes == null) {
            nodes = new String[nodeCount];
            initNodes();
        }
        return nodes;
    }

    public String getPrefix() {
        return prefix;
    }

    public abstract OpticalInstance createInstance(OpticalDeck deck);

    public abstract void parse(String cardString) throws ParserException;

    protected abstract void initNodes();
	
}
