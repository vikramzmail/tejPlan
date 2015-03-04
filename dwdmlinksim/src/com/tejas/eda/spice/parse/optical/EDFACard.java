package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.OpticalInstance;
import com.tejas.eda.spice.device.optical.EDFAInstance;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.OpticalDeck;
import com.tejas.eda.spice.parse.OpticalInstanceCard;
import com.tejas.eda.spice.parse.ParserException;

public class EDFACard extends OpticalInstanceCard {
	
	private String posNodeName;
	private String negNodeName;
	private String gain;
    private String noiseFigure;
    
    public EDFACard() {
    	nodeCount = 2;
    	prefix = "A";
    }
    
	public EDFACard(String cardString) throws ParserException {
		this();
		this.cardString = cardString;
		parse(cardString);
	}

	public String getPosNodeName() {
		return posNodeName;
	}

	public void setPosNodeName(String posNodeName) {
		this.posNodeName = posNodeName;
	}

	public String getNegNodeName() {
		return negNodeName;
	}

	public void setNegNodeName(String negNodeName) {
		this.negNodeName = negNodeName;
	}

	@Override
	public OpticalInstance createInstance(OpticalDeck deck) {
		EDFAModelCard card = (EDFAModelCard) deck.getModelCard(modelName);
        EDFAInstance instance = card.createInstance();
        instance.setInstName(instName);
        instance.setPosIndex(deck.getNodeIndex(posNodeName));
        instance.setNegIndex(deck.getNodeIndex(negNodeName));
        if(gain != null){
        	instance.setGain(parseDouble(gain));
        }
        if(noiseFigure != null){
        	instance.setNoiseFigure(parseDouble(noiseFigure));
        }
        return instance;
	}

	@Override
	public void parse(String cardString) throws ParserException {
		String[] elements = cardString.replaceAll(",|=", " ").split(" ");
        instName = elements[0];
        setPosNodeName(elements[1]);
        setNegNodeName(elements[2]);
        modelName = elements[3];
	}

	@Override
	protected void initNodes() {
		nodes[0] = posNodeName;
		nodes[1] = negNodeName;
	}

}
