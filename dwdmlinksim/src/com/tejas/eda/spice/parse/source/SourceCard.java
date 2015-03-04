/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source;

import com.tejas.eda.spice.parse.InstanceCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class SourceCard extends InstanceCard {

    public String posNodeName;
    public String negNodeName;

    public SourceCard() {
    }

    public SourceCard(String cardString) throws ParserException {
        super(cardString);
        parse(cardString);
    }

    public String getNegNodeName() {
        return negNodeName;
    }

    public void setNegNodeName(String negNodeName) {
        this.negNodeName = negNodeName;
    }

    public String getPosNodeName() {
        return posNodeName;
    }

    public void setPosNodeName(String posNodeName) {
        this.posNodeName = posNodeName;
    }

    @Override
    protected void initNodes() {
        nodes[0] = posNodeName;
        nodes[1] = negNodeName;
    }
}
