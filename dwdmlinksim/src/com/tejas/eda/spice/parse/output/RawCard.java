/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.output;

import com.tejas.eda.spice.output.RawOutput;
import com.tejas.eda.spice.parse.Deck;

/**
 *
 * @author owenbad
 */
public class RawCard extends OutputCard {

    public RawCard(String cardString) {
        super(cardString);
        type = "SAVE";
    }

    @Override
    public RawOutput createOutput(Deck deck) {
        RawOutput output = new RawOutput(deck.getTitle());
        initParameters(output, deck);
        return output;
    }
}
