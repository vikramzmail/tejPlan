package com.tejas.client;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.JApplet;

/* <APPLET
CODE=applet.class
WIDTH=600
HEIGHT=800 >
<PARAM NAME = string VALUE = "Hello from Java !!!">
</APPLET> */ 

public class TestApplet extends JApplet {
	public String s = "Hi I am Vivek";
	TextArea text;
	Button btn1, btn2, btn3;
	public void init() {
		setBackground(Color.BLUE);         
		addMouseListener(new MouseAdapter() {
	public void mousePressed(MouseEvent me){
			s = "Hello to Java 1";
			repaint();
		}}); 		
	}
	public void paint(Graphics g) {
		g.drawString(s, 60, 100);
		 URL url=getCodeBase();
         String msg = "Code Base : "+url.toString();
         g.drawString(msg,10,20);

         url=getDocumentBase();
         msg="Document Base : "+url.toString();
         g.drawString(msg,10,40);
	}
}
