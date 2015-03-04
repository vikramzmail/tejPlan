package com.tejas.client;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class MyJPanel_3 extends JPanel {
	
	public MyJPanel_3(){
		init();
	}

	private void init() {
		this.setBounds(9, 535, 291, 193);
		this.setBorder(
	            BorderFactory.createTitledBorder(
	            BorderFactory.createEtchedBorder(
	                    EtchedBorder.RAISED, Color.GRAY
	                    , Color.DARK_GRAY), "Main Panel 3"));
		this.setLayout(null);
	}

}
