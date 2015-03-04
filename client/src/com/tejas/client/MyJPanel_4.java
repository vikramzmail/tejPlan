package com.tejas.client;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import org.jdom.Attribute;
import org.jdom.Element;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.tejas.eda.output.Graph;
import com.tejas.eda.spice.DWDMRouteToSpiceConverter;
import com.tejas.engine.CLINet2Plan;

public class MyJPanel_4 extends JPanel {

	private JTextField text;
	private JButton btn,btn1,btn2, btn3, btn4, btn5;
	//private Object cell;
	MyCanvas canvas = new MyCanvas();

	public MyJPanel_4(){
		init();
	}

	private void init() {
		this.setBounds(303, 55, 679, 481);
		this.setBorder(
				BorderFactory.createTitledBorder(
						BorderFactory.createEtchedBorder(
								EtchedBorder.RAISED, Color.GRAY
								, Color.DARK_GRAY), "Main Panel 4"));
		this.setLayout(null);

		JLabel lblPhysicalTopology = new JLabel("Topology Viewer");
		lblPhysicalTopology.setForeground(new Color(255, 99, 71));
		lblPhysicalTopology.setBounds(12, 23, 167, 24);
		lblPhysicalTopology.setFont(new Font("Tahoma", Font.PLAIN, 18));
		this.add(lblPhysicalTopology);

		JLabel label = new JLabel("");
		label.setBounds(374, 13, 0, 0);
		this.add(label);

		canvas.setBounds(12, 50, 655, 422);
		//canvas.setBackground(Color.WHITE);
		canvas.setVisible(true);
		this.add(canvas); 

//		text = new JTextField(10);
//		this.add(text);
//		text.setPreferredSize(new Dimension(420, 21));
		setLayout(new FlowLayout(FlowLayout.LEFT));

		btn = new JButton("Add Node");
		this.add(btn);
		btn.addActionListener(new ActionListener() {                        
			public void actionPerformed(ActionEvent e) {
				addNode();
			}
		});


		btn2 = new JButton("Add Link");
		this.add(btn2);
		btn2.addActionListener(new ActionListener() {            
			public void actionPerformed(ActionEvent e) {
				addLink();
			}
		});

		btn1 = new JButton("Delete Node/Link");
		this.add(btn1);	
		btn1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent m) {
//				DelNode del = new DelNode();
				delCell();
			}
		});
		SimButtonListener simBtnListener = new SimButtonListener();
		
		btn3 = new JButton("Simulate");
		this.add(btn3);
		btn3.addActionListener(simBtnListener);
		
		btn4 = new JButton("Get IMG");
//		this.add(btn4);
		btn4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createImage();           	
			}
		});     

		btn5 = new JButton("Add Traffic");
		this.add(btn5);
		btn5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addTrafficDemand();
			}
		});
		
		btn5 = new JButton("View Traffic");
		this.add(btn5);	
		btn5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewTrafficDemand();
			}
		});

	}

	protected void viewTrafficDemand() {
		Set keyset = canvas.trafficMap.keySet();
		String trafficDetails = "<html><body><table><tr><td>Source</td><td>Destination</td><td>Traffic (Gbps)</td></tr>";
		int temp = 0;
		for (Iterator iterator = keyset.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String[] trafficMashUp = canvas.trafficMap.get(key).toString().split("\\^\\&\\*");
			trafficDetails = trafficDetails+ "<tr><td>" + trafficMashUp[0] + "</td><td>" + trafficMashUp[1] + "</td><td>" + trafficMashUp[2] + "</td></tr>";
			temp++;
		}
		
		trafficDetails = trafficDetails + "</table></body></html>";
		
		JOptionPane.showMessageDialog(null, trafficDetails, "Message Dialog", JOptionPane.PLAIN_MESSAGE);
	}

	protected void addTrafficDemand() {
//		String inStr = JOptionPane.showInputDialog(null, "Number of nodes to be configured",
//				"Traffic Pattern", JOptionPane.PLAIN_MESSAGE);
//		int cnt_node = Integer.parseInt(inStr);
//		int cnt_link = (cnt_node*(cnt_node-1))/2;

//		JOptionPane.showMessageDialog(null, "For "+cnt_node+" Nodes, "+cnt_link+" Links can be configured for Mesh Network.",
//				"Message Dialog", JOptionPane.PLAIN_MESSAGE);

//		System.out.println("For "+cnt_node+" Nodes,"+cnt_link+" Links can be configured.");

//		while(cnt_link != 0){
		
			String str1 = JOptionPane.showInputDialog("From node: ");
			if(!isNodeNameValid(str1)){
				JOptionPane.showMessageDialog(null, "Node " + str1 + " is not available in the topology",
						"Message Dialog", JOptionPane.PLAIN_MESSAGE);
			}
			String str2 = JOptionPane.showInputDialog("To node: ");
			if(!isNodeNameValid(str2)){
				JOptionPane.showMessageDialog(null, "Node " + str2 + " is not available in the topology",
						"Message Dialog", JOptionPane.PLAIN_MESSAGE);
			}
			String str3 = JOptionPane.showInputDialog("Traffic between two nodes(in Gbps)");

//			list.add(str1);            
//			list.add(str2);
//			list.add(str3);     	

//			cnt_link--;

//			System.out.println(list);	      
//			writeXMLfile(list);

//			int No = JOptionPane.showConfirmDialog(null, "Do you want to add more nodes ?");        
//			if (No == JOptionPane.NO_OPTION)
//			{                 	
//				System.exit(0);		
//			}

//		}	
			canvas.trafficMap.put(String.valueOf(canvas.numTrafficEntries++), str1 + "^&*" + str2 + "^&*" + str3);
//			XMLWriter xmlpg = new XMLWriter();
//			String baseFileName = xmlpg.writeXML("tempsave");
	}

	private boolean isNodeNameValid(String nodeName) {
		boolean status = true;
		if (canvas.getM().get(nodeName) == null) {
			status = false;
		}
		return status;
	}

	protected void createImage() {
		BufferedImage image = mxCellRenderer.createBufferedImage(canvas.getGraph(), null, 1, Color.WHITE, true, null);
		try {
			ImageIO.write(image, "PNG", new File("tempfiles/graph.png"));
		} catch(IOException e){
			e.printStackTrace();
		}		
	}

	protected void addNode() {
		canvas.getGraph().getModel().beginUpdate();
		Object parent = canvas.getGraph().getDefaultParent(); 
		String name = JOptionPane.showInputDialog("Node Name").toString();
		Object v1 = canvas.getGraph().insertVertex(parent, null, name, 300, 165, 35, 30);
//		Map m = new HashMap();
//		m = ((mxGraphModel) canvas.getGraph().getModel()).getCells();
//		Set keys = m.keySet();
//		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
//			String key = (String) iterator.next();
//			String value = m.get(key).toString();
//			mxCell cell = (mxCell) m.get(key);
//			if (cell.getValue().toString().equals(name)){
//				cell.setAttribute("type", "Add-Drop");
//			}
//		}
		canvas.getM().put(name, v1);
		canvas.getGraph().getModel().endUpdate();		
	}
	


	protected void delCell() {
		Object cell = canvas.currentobj;
		canvas.getGraph().getModel().beginUpdate();
		try {
			canvas.graph.getModel().remove(cell);
		} finally {
			canvas.getGraph().getModel().endUpdate();
		}
	}

	protected void addLink() {
		Object parent = canvas.getGraph().getDefaultParent();
		Object v1 = canvas.getM().get(JOptionPane.showInputDialog("From Node"));
		if (v1 == null){
			JOptionPane.showMessageDialog(null, "Node " + v1 + " is not available in the topology",
					"Message Dialog", JOptionPane.PLAIN_MESSAGE);
		}
		Object v2 = canvas.getM().get(JOptionPane.showInputDialog("To Node"));
		if (v2 == null){
			JOptionPane.showMessageDialog(null, "Node " + v2 + " is not available in the topology",
					"Message Dialog", JOptionPane.PLAIN_MESSAGE);
		}
		//String cap = JOptionPane.showInputDialog("Link Capacity");

		String name = JOptionPane.showInputDialog("Name of the Link");
		canvas.getGraph().insertEdge(parent, null, name, v1, v2);
		canvas.getGraph().insertEdge(parent, null, name, v2, v1);
	}
}
