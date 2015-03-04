package com.tejas.client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.Document;

import org.w3c.dom.DOMImplementation;

import com.tejas.eda.spice.DWDMRouteToSpiceConverter;
import com.tejas.engine.CLINet2Plan;

import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class TopoFrame  
{
	JFrame frame = new JFrame("Design a Network Topology");
	public MyJPanel_1 panel_1;
	public MyJPanel_2 panel_2;
	public MyJPanel_3 panel_3;
	public MyJPanel_4 panel_4;
	public MyJPanel_5 panel_5;
    
    public TopoFrame()
    {
    	init();
    }
    
    private void init(){
        frame.setSize(1000,800);        
		frame.setLocationRelativeTo(null);
		frame.setDefaultLookAndFeelDecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mnFile.add(mntmNew);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mnFile.add(mntmOpen);
		
		JMenuItem mntmPrint = new JMenuItem("Print");
		mnFile.add(mntmPrint);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mnFile.add(mntmClose);
		
		JMenu mnNewMenu = new JMenu("Mode");
		menuBar.add(mnNewMenu);
		
		JMenuItem mntmNormalMode = new JMenuItem("Normal Mode");
		mnNewMenu.add(mntmNormalMode);
		
		JMenuItem mntmDebugMode = new JMenuItem("Debug Mode");
		mnNewMenu.add(mntmDebugMode);
		
		JMenuItem mntmSafeMode = new JMenuItem("Safe Mode");
		mnNewMenu.add(mntmSafeMode);
		
		JMenu mnTools = new JMenu("Tools");
		menuBar.add(mnTools);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmContents = new JMenuItem("Contents");
		mnHelp.add(mntmContents);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		frame.getContentPane().setLayout(null);
		
		JLabel lblWelcomeToTejas = new JLabel("Welcome to Tejas Networks");
		lblWelcomeToTejas.setHorizontalAlignment(SwingConstants.CENTER);
		lblWelcomeToTejas.setForeground(new Color(147, 112, 219));
		lblWelcomeToTejas.setFont(new Font("Tahoma", Font.BOLD, 20));
		lblWelcomeToTejas.setBounds(242, 0, 435, 42);
		frame.getContentPane().add(lblWelcomeToTejas);
		
		// Label wise components designed inside the container.
		
		// ********** Panel 1 **************//
		panel_1 = new MyJPanel_1();
		frame.getContentPane().add(panel_1);
		
		// ********** Panel 2 **************//

		panel_2 = new MyJPanel_2();
		panel_2.setSize(291, 229);
		panel_2.setLocation(12, 301);
		frame.getContentPane().add(panel_2);
		
		// ********** Panel 3 **************//

		panel_3 = new MyJPanel_3();
		frame.getContentPane().add(panel_3);
		
		// ********** Panel 4 **************//

		panel_4 = new MyJPanel_4();
		frame.getContentPane().add(panel_4);
		
		// ********** Panel 5 **************//
		
		panel_5 = new MyJPanel_5();
		frame.getContentPane().add(panel_5);
		
		frame.setResizable(false);		
		frame.setVisible(true); 
    }	  
}
