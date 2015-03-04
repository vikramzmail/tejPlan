package com.tejas.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import com.mxgraph.view.mxGraph;

import com.tejas.eda.spice.DWDMRouteToSpiceConverter;
import com.tejas.engine.CLINet2Plan;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MyCanvas extends JPanel {
	protected static mxGraph graph = new mxGraph();
	protected static HashMap m = new HashMap();
	public mxGraphComponent graphComponent;
	protected static mxCell cell;
	protected static Object currentobj;
	protected static Map trafficMap = new HashMap();
	protected static int numTrafficEntries = 0;
	
	public static HashMap getM() {
		return m;
	}

	public static mxGraph getGraph() {
		return graph;
	}

	public MyCanvas(){
		super();
		designGUI();
	}

	private void designGUI() {
		setSize(100, 100);

		graphComponent = new mxGraphComponent(graph);
		graphComponent.setPreferredSize(new Dimension(640, 380));
		this.add(graphComponent);
		
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
		{		
			public void mouseReleased(MouseEvent e)
			{
				currentobj = graphComponent.getCellAt(e.getX(), e.getY());
			}
		});
		

		
	}
}