package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.OpticalInstance;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.OpticalDeck;
import com.tejas.eda.spice.parse.OpticalInstanceCard;
import com.tejas.eda.spice.parse.ParserException;

public class LinkCard extends OpticalInstanceCard {
	
	private String posNodeName;
	private String negNodeName;
	private String alpha;
    private String length;
    
    public LinkCard() {
    	nodeCount = 2;
    	prefix = "L";
    }
    
	public LinkCard(String cardString) throws ParserException {
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

	public String getAlpha() {
		return alpha;
	}

	public void setAlpha(String alpha) {
		this.alpha = alpha;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	@Override
	public OpticalInstance createInstance(OpticalDeck deck) {
		LinkModelCard card = (LinkModelCard) deck.getModelCard(modelName);
        LinkInstance instance = card.createInstance();
        instance.setInstName(instName);
        instance.setPosIndex(deck.getNodeIndex(posNodeName));
        instance.setNegIndex(deck.getNodeIndex(negNodeName));
        if(length != null){
        	instance.setLength(parseDouble(length));
        }
        if(alpha != null){
        	instance.setAlpha(parseDouble(alpha));
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
        if (elements.length > 4) {
            for (int i = 4; i < elements.length; i++) {
                if (elements[i].equals("LEN")) {
                    setLength(elements[++i]);
                } 
            }
        }
	}

	@Override
	protected void initNodes() {
		nodes[0] = posNodeName;
		nodes[1] = negNodeName;
	}

}
