/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.engine.utils.topology;

import com.tejas.engine.utils.topology.GraphPanel;
import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.utils.FileChooserConfirmOverwrite;
import com.tejas.engine.utils.FileChooserNetworkDesign;

import edu.uci.ics.jung.visualization.control.GraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;

/**
 * Wrapper class for the graph canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class TopologyCanvas extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private GraphPanel g;
    private JButton btn_load, btn_loadDemand, btn_save, btn_zoomIn, btn_zoomOut, btn_zoomAll, btn_takeSnapshot, btn_showNodeName;
    private FileChooserNetworkDesign fc_netPlan;
    private JFileChooser fc_demands;
    private final ITopologyCallback callback;
    private final File defaultDesignDirectory, defaultDemandDirectory;

    /**
     * Returns a reference to the canvas.
     * 
     * @return Reference to the canvas
     * @since 0.2.0
     */
    public GraphPanel getGraphPanel() { return g; }

    /**
     * Default constructor.
     * 
     * @param callback 
     * @param defaultDesignDirectory 
     * @param defaultDemandDirectory
     * @param plugins List of plugins to be included (it may be null)
     * @since 0.2.0
     */
    public TopologyCanvas(ITopologyCallback callback, File defaultDesignDirectory, File defaultDemandDirectory, List<GraphMousePlugin> plugins)
    {
        File currentDir = SystemUtils.getCurrentDir();
        
        this.callback = callback;
        this.defaultDesignDirectory = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
        this.defaultDemandDirectory = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;
        
	g = new GraphPanel();
	g.setBorder(new LineBorder(Color.BLACK));

	GraphMousePlugin scalingPlugin = new ScalingGraphMousePlugin(g.getScalingControl(), MouseEvent.NOBUTTON);
	g.addPlugin(scalingPlugin);

//	GraphMousePlugin panningPlugin = new PanGraphPlugin(MouseEvent.BUTTON1_MASK);
//	g.addPlugin(panningPlugin);

	for (GraphMousePlugin plugin : plugins)
	    g.addPlugin(plugin);

	setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[][grow]"));

	JToolBar buttonBar = new JToolBar();
	buttonBar.setRollover(true);
	buttonBar.setFloatable(false);
	buttonBar.setOpaque(false);
	buttonBar.setBorderPainted(false);
	add(buttonBar, "growx, wrap");
	add(g, "grow");

        btn_load = new JButton();
        btn_loadDemand = new JButton();
	btn_zoomIn = new JButton();
	btn_zoomOut = new JButton();
	btn_zoomAll = new JButton();
	btn_takeSnapshot = new JButton();
	btn_showNodeName = new JButton("Show/hide node name");

	btn_load.setIcon(new ImageIcon(getClass().getResource("/resources/loadDesign.png")));
	btn_loadDemand.setIcon(new ImageIcon(getClass().getResource("/resources/loadDemand.png")));
	btn_showNodeName.setBorderPainted(true);
	btn_zoomIn.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-in.png")));
	btn_zoomOut.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-out.png")));
	btn_zoomAll.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-all.png")));
	btn_takeSnapshot.setIcon(new ImageIcon(getClass().getResource("/resources/take-snapshot.png")));

	btn_load.addActionListener(this);
	btn_loadDemand.addActionListener(this);
	btn_showNodeName.addActionListener(this);
	btn_zoomIn.addActionListener(this);
	btn_zoomOut.addActionListener(this);
	btn_zoomAll.addActionListener(this);
	btn_takeSnapshot.addActionListener(this);

	buttonBar.add(btn_load);
	buttonBar.add(btn_loadDemand);
        buttonBar.add(new JToolBar.Separator());
	buttonBar.add(btn_zoomIn);
	buttonBar.add(btn_zoomOut);
	buttonBar.add(btn_zoomAll);
	buttonBar.add(btn_takeSnapshot);
	buttonBar.add(btn_showNodeName);
    }

    /**
     * Refresh the graph with a new network plan.
     * 
     * @param netPlan Network plan
     * @since 0.2.0
     */
    public void refreshTopology(NetPlan netPlan)
    {
	double[][] nodeXYPositionTable = netPlan.getNodeXYPositionTable();
	String[] nodeName = netPlan.getNodeNameVector();
	int[][] linkTable = netPlan.getLinkTable();

	g.refreshTopology(nodeXYPositionTable, nodeName, linkTable);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
	Object src = e.getSource();
        
        if (src == btn_load)
        {
            ErrorHandling.showErrorDialog("musgo", "kk");
            loadDesign();
        }
        else if (src == btn_loadDemand)
            loadTrafficDemands();
        else if (src == btn_showNodeName)
	    g.toggleNodeLabel();
	else if (src == btn_zoomIn)
	    g.zoomIn();
	else if (src == btn_zoomOut)
	    g.zoomOut();
	else if (src == btn_zoomAll)
	    g.zoomAll();
	else if (src == btn_takeSnapshot)
	    g.takeSnapshot();
	else
	    throw new RuntimeException("Bad");
    }

    private void checkNetPlanFileChooser()
    {
        if (fc_netPlan == null)
            fc_netPlan = new FileChooserNetworkDesign(defaultDesignDirectory, FileChooserNetworkDesign.TYPE.NETWORK_DESIGN);
    }

    private void checkDemandFileChooser()
    {
        if (fc_demands == null)
        {
            fc_demands = new FileChooserConfirmOverwrite(defaultDemandDirectory);
            fc_demands.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc_demands.setFileFilter(new FileNameExtensionFilter("n2p files", "n2p"));
            fc_demands.setAcceptAllFileFilterUsed(false);
        }
    }

    public void loadDesign()
    {
        try
        {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan aux = fc_netPlan.getNetPlan();
            
            callback.loadDesign(aux);
        }
        catch(Net2PlanException ex)
        {
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
        }
        catch (Exception ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyCanvas.class);
            ErrorHandling.showErrorDialog("Error loading network design");
        }
    }

    public void loadTrafficDemands()
    {
        try
        {
            checkDemandFileChooser();

            int rc = fc_demands.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan aux_demands = new NetPlan(fc_demands.getSelectedFile());
            callback.loadTrafficDemands(aux_demands);
        }
        catch(Net2PlanException ex)
        {
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading traffic demands");
        }
        catch (Exception ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyCanvas.class);
            ErrorHandling.showErrorDialog("Error loading traffic demands");
        }
    }
}