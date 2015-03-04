package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.Model;
import com.tejas.eda.spice.device.optical.EDFAInstance;
import com.tejas.eda.spice.device.optical.EDFAModelValues;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.device.optical.LinkModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.OpticalModelCard;
import com.tejas.eda.spice.parse.ParserException;

public class EDFAModelCard extends OpticalModelCard<EDFAInstance, EDFAModelValues> {

	public EDFAModelCard(String cardString) throws ParserException {
		super(cardString);
	}

	@Override
	public EDFAInstance createInstance() {
		EDFAInstance instance = new EDFAInstance();
		initModelValues(instance);
		return instance;
	}

	@Override
	public void setProperty(EDFAModelValues model, String name, String value) {
		if (name.equals("G")) {
            model.setGain(parseDouble(value));
        } else if (name.equals("NF")){
        	model.setNoiseFigure(parseDouble(value));
        }
        /*else if (name.equals("LOSS")){
        	model.setEDFALoss(parseDouble(value));
        }*/
	}
}
