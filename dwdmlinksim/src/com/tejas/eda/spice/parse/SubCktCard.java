/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse;

import com.tejas.eda.spice.device.Instance;

/**
 *
 * @author Kristopher T. Beck
 */
public class SubCktCard extends InstanceCard {

    public SubCktCard() {
        prefix = "X";
    }

    public SubCktCard(String cardString) throws ParserException {
        super(cardString);
        parse(cardString);
    }

    @Override
    public Instance createInstance(Deck deck) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void parse(String cardString) throws ParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initNodes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
