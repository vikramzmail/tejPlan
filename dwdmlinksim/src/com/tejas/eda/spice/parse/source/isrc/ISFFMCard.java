/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.isrc;

import com.tejas.eda.spice.device.sources.isrcs.ISrcSFFM;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.SffmSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class ISFFMCard extends SffmSrcCard {

    public ISFFMCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "I";
    }

    @Override
    public ISrcSFFM createInstance(Deck deck) {
        ISrcSFFM instance = new ISrcSFFM();
        initAuxValues(instance);
        initSrcValues(instance, deck);
        return instance;
    }

    protected void initAuxValues(ISrcSFFM instance) {
        instance.setOffset(parseDouble(offset));
        instance.setAmplitude(parseDouble(amplitude));
        instance.setCarrierFreq(parseDouble(carrierFreq));
        instance.setModulIndex(parseDouble(modulationIndex));
        instance.setSignalFreq(parseDouble(signalFreq));
    }
}
