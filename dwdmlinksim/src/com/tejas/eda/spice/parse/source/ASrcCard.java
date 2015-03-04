/*
 * ASrcCard.java
 * 
 * Created on Oct 15, 2007, 1:24:33 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source;

import com.tejas.eda.spice.device.sources.asrc.ASrcCurrent;
import com.tejas.eda.spice.device.sources.asrc.ASrcInstance;
import com.tejas.eda.spice.device.sources.asrc.ASrcVoltage;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.InstanceCard;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.math.Parser;

/**
 * @author Kristopher T. Beck
 */
public class ASrcCard extends InstanceCard {

    private String posNodeName;
    private String negNodeName;
    private String expression;

    public ASrcCard(String cardString) throws ParserException {
        nodeCount = 2;
        prefix = "B";
        parse(cardString);
    }

    public ASrcInstance createInstance(Deck deck) {
        ASrcInstance instance = null;
        if (expression.startsWith("V")) {
            instance = new ASrcVoltage();
        } else if (expression.startsWith("I")) {
            instance = new ASrcCurrent();
        }
        ASrcParser parser = new ASrcParser();
        instance.setFunction(parser.parse(expression));
        instance.setPosIndex(deck.getNodeIndex(posNodeName));
        instance.setNegIndex(deck.getNodeIndex(negNodeName));
        return instance;
    }

    @Override
    public void parse(String cardString) throws ParserException {
        String[] elements = cardString.replaceAll(",|=", " ").split(" ");
        instName = elements[0];
        posNodeName = elements[1];
        negNodeName = elements[2];
//TODO parse asrc expressions;
    }

    @Override
    protected void initNodes() {
        nodes[0] = posNodeName;
        nodes[1] = negNodeName;
    }

    public static class ASrcParser extends Parser {
    }
}
