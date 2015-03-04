/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.optical;

import java.util.regex.Pattern;

import com.tejas.eda.spice.device.source.optical.OpticalSource;
import com.tejas.eda.spice.device.source.optical.OpticalSourceInstance;
import com.tejas.eda.spice.device.sources.IndependentLinearSource;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.OpticalInstanceCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class OpticalSourceCard extends OpticalInstanceCard {

    public String posNodeName;
    public String negNodeName;
    
    public OpticalSourceCard() {
    }

    public OpticalSourceCard(String cardString) throws ParserException {
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
    
    private void setNegIndex(int nodeIndex) {
		// TODO Auto-generated method stub
		
	}

	private void setPosIndex(int nodeIndex) {
		// TODO Auto-generated method stub
		
	}
	
	public void parse(String cardString){
		String[] elements = cardString.replaceAll(",|=", " ").split(" ");
        instName = elements[0];
        posNodeName = elements[1];
        negNodeName = elements[2];
	}
}
