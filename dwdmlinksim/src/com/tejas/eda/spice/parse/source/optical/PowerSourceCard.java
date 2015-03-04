package com.tejas.eda.spice.parse.source.optical;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tejas.eda.spice.device.source.optical.PowerSource;
import com.tejas.eda.spice.parse.OpticalDeck;
import com.tejas.eda.spice.parse.ParserException;

public class PowerSourceCard extends OpticalSourceCard {
	
	protected String power;
	
	public static final String POWER_STRING = "SIN\\s*\\(\\s*"
            + DOUBLE_VALUE_STRING + "\\s+" + DOUBLE_VALUE_STRING
            + "(\\s+" + DOUBLE_VALUE_STRING + "(\\s+"
            + DOUBLE_VALUE_STRING + "(\\s+" + DOUBLE_VALUE_STRING
            + ")?)?)?\\s*\\)";
    public static final Pattern POWER_PATTERN = Pattern.compile(POWER_STRING);

	public PowerSourceCard(String cardString) throws ParserException {
		super(cardString);
		prefix = "P";
	}

	@Override
	public PowerSource createInstance(OpticalDeck deck) {
		PowerSource instance = new PowerSource();
		initSrcValues(instance, deck);
		return instance;
	}

	private void initSrcValues(PowerSource instance, OpticalDeck deck) {
		instance.setInstName(instName);
        instance.setPosIndex(deck.getNodeIndex(posNodeName));
        instance.setNegIndex(deck.getNodeIndex(negNodeName));
        
        if (power != null){
        	instance.setPower(Double.parseDouble(power));
        }
	}
	
	public void parse(String cardString) {
        Matcher m = POWER_PATTERN.matcher(cardString);
        power = cardString.split(" ")[3];
//        if (m.find()) {
//            /*
//            System.out.println("SIN Src");
//            for (int i = 0; i < m.groupCount(); i++) {
//                System.out.println("group " + i + " = " + m.group(i));
//            }
//             */
//            offset = m.group(1);
//            amplitude = m.group(10);
//            freq = m.group(20);
//            delayTime = m.group(30);
//            theta = m.group(40);
//            super.parse(cardString.substring(0, m.start() - 1) + cardString.substring(m.end()));
//        } else {
//            throw new ParserException();
//        }
        super.parse(cardString);
    }

}
