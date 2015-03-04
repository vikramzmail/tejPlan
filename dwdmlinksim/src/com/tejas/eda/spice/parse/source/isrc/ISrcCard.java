/*
 * ISrc.java
 * 
 * Created on Oct 15, 2007, 1:31:05 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.isrc;

import com.tejas.eda.spice.device.sources.isrcs.ISrcInstance;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.IndependentSourceCard;

/**
 * 
 * @author Kristopher T. Beck
 */
public class ISrcCard extends IndependentSourceCard {

    public ISrcCard() {
        nodeCount = 2;
        prefix = "I";
    }

    public ISrcCard(String cardString) throws ParserException {
        this();
        this.cardString = cardString;
        parse(cardString);
    }

    public ISrcInstance createInstance(Deck deck) {
        ISrcInstance instance = new ISrcInstance();
        initSrcValues(instance, deck);
        return instance;
    }
}
