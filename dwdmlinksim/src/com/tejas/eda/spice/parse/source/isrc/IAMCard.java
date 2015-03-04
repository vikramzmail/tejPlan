/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.isrc;

import com.tejas.eda.spice.device.sources.isrcs.ISrcAM;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.AmSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class IAMCard extends AmSrcCard {

    public IAMCard(String cardString) throws ParserException {
        super(cardString);
        prefix="I";
    }

    @Override
    public ISrcAM createInstance(Deck deck) {
        ISrcAM instance = new ISrcAM();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(ISrcAM instance) {
        instance.setOffset(parseDouble(offset));
        instance.setAmplitude(parseDouble(amplitude));
        instance.setCarrierFreq(parseDouble(carrierFreq));
        instance.setModulFreq(parseDouble(modulFreq));
        instance.setDelayTime(parseDouble(delayTime));
    }
}
