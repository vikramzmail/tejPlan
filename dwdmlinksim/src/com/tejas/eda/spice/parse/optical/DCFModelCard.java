package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.Model;
import com.tejas.eda.spice.device.optical.EDFAInstance;
import com.tejas.eda.spice.device.optical.EDFAModelValues;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.device.optical.LinkModelValues;
import com.tejas.eda.spice.device.optical.DCFInstance;
import com.tejas.eda.spice.device.optical.DCFModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.OpticalModelCard;
import com.tejas.eda.spice.parse.ParserException;	

	public class  DCFModelCard extends OpticalModelCard<DCFInstance, DCFModelValues> {

		public DCFModelCard(String cardString) throws ParserException {
			super(cardString);
		}

		@Override
		public DCFInstance createInstance() {
			DCFInstance instance = new DCFInstance();
			initModelValues(instance);
			return instance;
		}

		@Override
		public void setProperty(DCFModelValues model, String name, String value) {
			if (name.equals("DC")) {
	            model.setDispComp(parseDouble(value));
	        } else if (name.equals("PL")){
	        	model.setPLoss(parseDouble(value));
	        }
	     
		}
	}


