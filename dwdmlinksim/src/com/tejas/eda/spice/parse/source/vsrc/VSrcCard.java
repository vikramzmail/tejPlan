/*
 * VSrcCard.java
 * 
 * Created on Oct 31, 2007, 7:52:17 PM
 * 
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.vsrc;

import com.tejas.eda.spice.device.sources.vsrcs.VSrcInstance;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.IndependentSourceCard;

/**
 * @author Kristopher T. Beck
 */
public class VSrcCard extends IndependentSourceCard {

    public VSrcCard() {
        nodeCount = 2;
        prefix = "V";
    }

    public VSrcCard(String cardString) throws ParserException {
        this();
        this.cardString = cardString;
        parse(cardString);
    }

    public VSrcInstance createInstance(Deck deck) {
        VSrcInstance instance = new VSrcInstance();
        initSrcValues(instance, deck);
        return instance;
    }
}
