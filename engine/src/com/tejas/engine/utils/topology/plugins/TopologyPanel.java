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

package com.tejas.engine.utils.topology.plugins;

import com.tejas.engine.utils.topology.plugins.ITopologyCanvas;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvasPlugin;
import com.tejas.engine.utils.topology.plugins.JUNGCanvas;
import com.tejas.engine.utils.topology.plugins.TopologyPanel;
import com.tejas.engine.GUINet2Plan;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.Version;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.FileChooserConfirmOverwrite;
import com.tejas.engine.utils.FileChooserNetworkDesign;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.topology.ITopologyCallback;
import com.tejas.engine.utils.topology.TopologyCanvas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import net.miginfocom.swing.MigLayout;

/**
 *
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class TopologyPanel extends JPanel implements ActionListener
{
    private ITopologyCanvas canvas;
    private JButton btn_load, btn_loadDemand, btn_save, btn_zoomIn, btn_zoomOut, btn_zoomAll, btn_takeSnapshot, btn_showNodeName, btn_reset;
    private FileChooserNetworkDesign fc_netPlan;
    private JFileChooser fc_demands;
    private final ITopologyCallback callback;
    private final File defaultDesignDirectory, defaultDemandDirectory;
    
    /**
     * Simplified constructor that does not require to indicate default locations 
     * for <code>.n2p</code> files.
     * 
     * @param callback Topology callback listening plugin events
     * @param canvasType Canvas type (i.e. JUNG)
     * @param plugins List of plugins to be included (it may be null)
     * @since 0.2.3
     */
    public TopologyPanel(ITopologyCallback callback, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
    {
        this(callback, null, null, canvasType, plugins);
    }
    
    /**
     * Default constructor.
     * 
     * @param callback Topology callback listening plugin events
     * @param defaultDesignDirectory Default location for design <code>.n2p</code> files (it may be null, then default is equal to <code>net2planFolder/workspace/data/networkTopologies</code>)
     * @param defaultDemandDirectory Default location for design <code>.n2p</code> files (it may be null, then default is equal to <code>net2planFolder/workspace/data/trafficMatrices</code>)
     * @param canvasType Canvas type (i.e. JUNG)
     * @param plugins List of plugins to be included (it may be null)
     * @since 0.2.0
     */
    public TopologyPanel(ITopologyCallback callback, File defaultDesignDirectory, File defaultDemandDirectory, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
    {
        File currentDir = SystemUtils.getCurrentDir();
        
        this.callback = callback;
        this.defaultDesignDirectory = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
        this.defaultDemandDirectory = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;

        try { canvas = canvasType.newInstance(); }
        catch(Throwable e) { throw new RuntimeException(e); }
        
        if (plugins != null)
            for(ITopologyCanvasPlugin plugin : plugins)
                canvas.addPlugin(plugin);
        
	setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[][grow]"));

	JToolBar buttonBar = new JToolBar();
	buttonBar.setRollover(true);
	buttonBar.setFloatable(false);
	buttonBar.setOpaque(false);
	buttonBar.setBorderPainted(false);
	add(buttonBar, "growx, wrap");
        
        JComponent canvasComponent = canvas.getComponent();
        canvasComponent.setBorder(LineBorder.createBlackLineBorder());
	add(canvasComponent, "grow");

        btn_load = new JButton(); btn_load.setToolTipText("Load a network design");
        btn_loadDemand = new JButton(); btn_loadDemand.setToolTipText("Load a traffic demand set");
        btn_save = new JButton(); btn_save.setToolTipText("Save current state to a file");
	btn_zoomIn = new JButton(); btn_zoomIn.setToolTipText("Zoom in");
	btn_zoomOut = new JButton(); btn_zoomOut.setToolTipText("Zoom out");
	btn_zoomAll = new JButton(); btn_zoomAll.setToolTipText("Zoom all");
	btn_takeSnapshot = new JButton(); btn_takeSnapshot.setToolTipText("Take a snapshot of the canvas");
	btn_showNodeName = new JButton("Show/hide node name");
	btn_reset = new JButton("Reset"); btn_reset.setToolTipText("Reset the user interface");

	btn_load.setIcon(new ImageIcon(getClass().getResource("/resources/loadDesign.png")));
	btn_loadDemand.setIcon(new ImageIcon(getClass().getResource("/resources/loadDemand.png")));
	btn_save.setIcon(new ImageIcon(getClass().getResource("/resources/saveDesign.png")));
	btn_showNodeName.setBorderPainted(true);
	btn_zoomIn.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-in.png")));
	btn_zoomOut.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-out.png")));
	btn_zoomAll.setIcon(new ImageIcon(getClass().getResource("/resources/zoom-all.png")));
	btn_takeSnapshot.setIcon(new ImageIcon(getClass().getResource("/resources/take-snapshot.png")));

	btn_load.addActionListener(this);
	btn_loadDemand.addActionListener(this);
	btn_save.addActionListener(this);
	btn_showNodeName.addActionListener(this);
	btn_zoomIn.addActionListener(this);
	btn_zoomOut.addActionListener(this);
	btn_zoomAll.addActionListener(this);
	btn_takeSnapshot.addActionListener(this);
	btn_reset.addActionListener(this);

	buttonBar.add(btn_load);
	buttonBar.add(btn_loadDemand);
	buttonBar.add(btn_save);
        buttonBar.add(new JToolBar.Separator());
	buttonBar.add(btn_zoomIn);
	buttonBar.add(btn_zoomOut);
	buttonBar.add(btn_zoomAll);
	buttonBar.add(btn_takeSnapshot);
	buttonBar.add(btn_showNodeName);
        buttonBar.add(Box.createHorizontalGlue());
	buttonBar.add(btn_reset);
    }
    
    /**
     *
     * @param isAllowed
     */
    public void setAllowLoadTrafficDemand(boolean isAllowed)
    {
        btn_loadDemand.setVisible(isAllowed);
    }
    
    /**
     * Returns a reference to the topology canvas.
     * 
     * @return Reference to the topology canvas
     * @since 0.2.3
     */
    public ITopologyCanvas getCanvas() { return canvas; }
    
    /**
     * Main entry point. Creates a topology panel where users are able to examine
     * their design without running Net2Plan.
     * 
     * @param args Command-line argument (unused)
     * @since 0.2.3
     */
    public static void main(String[] args)
    {
        SystemUtils.configureEnvironment(GUINet2Plan.class, SystemUtils.UserInterface.GUI);
        
        final List<String> nodeNames = new ArrayList<String>();
        final List<int[]> linkTable = new ArrayList<int[]>();
        final List<int[]> demandTable = new ArrayList<int[]>();
        final List<Double> h_d = new ArrayList<Double>();
        final List<int[]> routes = new ArrayList<int[]>();
        final List<Integer> d_p = new ArrayList<Integer>();
        final List<Double> x_p = new ArrayList<Double>();
        final List<int[]> segments = new ArrayList<int[]>();
        final List<Double> u_s = new ArrayList<Double>();

        final TopologyPanel canvasPanel = new TopologyPanel(null, null, null, JUNGCanvas.class, null);
        
        JLabel label = new JLabel("Select a Net2Plan (.n2p) / SNDLib (.xml) file:");

        Path currentRelativePath = Paths.get("");
        File path = currentRelativePath.toAbsolutePath().toFile();
        final FileChooserNetworkDesign fc = new FileChooserNetworkDesign(path, FileChooserNetworkDesign.TYPE.NETWORK_DESIGN);
        
        final JComboBox cmb_element = new JComboBox();
        cmb_element.addItem("Select a network element");
        cmb_element.addItem("Nodes");
        cmb_element.addItem("Links");
        cmb_element.addItem("Demands");
        cmb_element.addItem("Routes");
        cmb_element.addItem("Protection segments");
        cmb_element.addItem("SRGs");
        
        final JComboBox cmb_item = new JComboBox();
        
        final Set<Integer> allowedElements = new HashSet<Integer>();
        
        cmb_element.setRenderer(new BasicComboBoxRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                boolean isEnabled = index == 0 || allowedElements.contains(index - 1);
                Component c = super.getListCellRendererComponent(list, value, index, isSelected && isEnabled(), cellHasFocus);
                if (!isEnabled) { c.setForeground(Color.GRAY); }                
                
                return c;
            }
        });

        ActionListener cmb_listener = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (e.getSource() == cmb_element)
                {
                    cmb_item.removeAllItems();
                    
                    int elementType = cmb_element.getSelectedIndex() - 1;
                    if (elementType < 0 || !allowedElements.contains(elementType)) return;
                    
                    switch(elementType)
                    {
                        case 0:
                            cmb_item.removeAllItems();
                            
                            int N = nodeNames.size();
                            for(int nodeId = 0; nodeId < N; nodeId++)
                                cmb_item.addItem("Node " + nodeId + " (" + nodeNames.get(nodeId) + ")");
                            break;
                        case 1:
                            cmb_item.removeAllItems();
                            
                            int E = linkTable.size();
                            for(int linkId = 0; linkId < E; linkId++)
                                cmb_item.addItem("Link " + linkId + " (n" + linkTable.get(linkId)[0] + " -> n" + linkTable.get(linkId)[1] + ")");
                            break;
                        case 2:
                            cmb_item.removeAllItems();
                            
                            int D = demandTable.size();
                            for(int demandId = 0; demandId < D; demandId++)
                            {
                                int ingressNodeId = demandTable.get(demandId)[0];
                                int egressNodeId = demandTable.get(demandId)[1];
                                double offeredTrafficInErlangs = h_d.get(demandId);
                                
                                cmb_item.addItem("Demand " + demandId + " (n" + ingressNodeId + " -> n" + egressNodeId + ", " + offeredTrafficInErlangs + " E)");
                            }
                            break;
                        case 3:
                            cmb_item.removeAllItems();
                            
                            int R = routes.size();
                            for(int routeId = 0; routeId < R; routeId++)
                            {
                                int demandId = d_p.get(routeId);
                                int ingressNodeId = demandTable.get(demandId)[0];
                                int egressNodeId = demandTable.get(demandId)[1];
                                double offeredTrafficInErlangs = h_d.get(demandId);
                                double carriedTrafficInErlangs = x_p.get(routeId);
                                
                                cmb_item.addItem(String.format("Route %d (d%d, n%d -> n%d, %.3f/%.3f E)", routeId, demandId, ingressNodeId, egressNodeId, carriedTrafficInErlangs, offeredTrafficInErlangs));
                            }
                            break;
                    }
                }
                else if (e.getSource() == cmb_item)
                {
                    int elementType = cmb_element.getSelectedIndex() - 1;
                    int elementId = cmb_item.getSelectedIndex() - 1;
                    if (elementId < 0) return;
                    
                    switch(elementType)
                    {
                        case 0:
                            canvasPanel.getCanvas().showNode(elementId);
                            break;
                        case 1:
                            canvasPanel.getCanvas().showLink(elementId);
                            break;
                        case 2:
                            int[] nodeIds = demandTable.get(elementId);
                            canvasPanel.getCanvas().showNodes(nodeIds);
                            break;
                        case 3:
                            int[] routeLinkIds = routes.get(elementId);
                            canvasPanel.getCanvas().showRoute(routeLinkIds);
                            break;
                        case 4:
                            int[] segmentLinkIds = segments.get(elementId);
                            canvasPanel.getCanvas().showRoute(segmentLinkIds);
                            break;
                        default:
                            break;
                    }
                }
            }
        };
        
        cmb_element.addActionListener(cmb_listener);
        cmb_item.addActionListener(cmb_listener);
        
        final JButton load = new JButton("Load");
        load.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int rc = fc.showOpenDialog(null);
                if (rc != JFileChooser.APPROVE_OPTION) return;

                NetPlan netPlan = fc.getNetPlan();
                
                canvasPanel.getCanvas().updateTopology(netPlan);
                canvasPanel.getCanvas().zoomAll();

                if (netPlan.hasNodes()) allowedElements.add(0);
                if (netPlan.hasLinks()) allowedElements.add(1);
                if (netPlan.hasDemands()) allowedElements.add(2);
                if (netPlan.hasRoutes()) allowedElements.add(3);
                if (netPlan.hasProtectionSegments()) allowedElements.add(4);
                
                nodeNames.clear(); nodeNames.addAll(StringUtils.toList(netPlan.getNodeNameVector()));
                linkTable.clear(); linkTable.addAll(Arrays.asList(netPlan.getLinkTable()));
                demandTable.clear(); demandTable.addAll(Arrays.asList(netPlan.getDemandTable()));
                h_d.clear(); h_d.addAll(DoubleUtils.toList(netPlan.getDemandOfferedTrafficInErlangsVector()));
                routes.clear(); routes.addAll(netPlan.getRouteAllSequenceOfLinks());
                d_p.clear(); d_p.addAll(IntUtils.toList(netPlan.getRouteDemandVector()));
                x_p.clear(); x_p.addAll(DoubleUtils.toList(netPlan.getRouteCarriedTrafficInErlangsVector()));
                segments.clear(); segments.addAll(netPlan.getProtectionSegmentAllSequenceOfLinks());
                u_s.clear(); u_s.addAll(DoubleUtils.toList(netPlan.getProtectionSegmentReservedBandwithInErlangsVector()));
                
                cmb_element.revalidate();
                cmb_element.setSelectedIndex(0);
            }
        });
        
        JPanel extraPanel = new JPanel(new MigLayout("fill, insets 5 5 5 5"));
        extraPanel.add(label, "left,spanx 3");
        extraPanel.add(load, "right, wrap");
        extraPanel.add(new JLabel("View network element:"));
        extraPanel.add(cmb_element);
        extraPanel.add(new JLabel("Select an item:"));
        extraPanel.add(cmb_item);
        
        BorderLayout layout = new BorderLayout(1, 2);
        layout.setVgap(5);
        
        JPanel aux = new JPanel(layout);
        aux.add(extraPanel, BorderLayout.CENTER);
        aux.add(new JSeparator(), BorderLayout.SOUTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(aux, BorderLayout.NORTH);
        mainPanel.add(canvasPanel, BorderLayout.CENTER);

        final JFrame f = new JFrame(Version.getVersion() + " - Topology viewer");
        f.setSize(600, 400);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(mainPanel);
        f.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
	Object src = e.getSource();

        if (src == btn_load)
            loadDesign();
        else if (src == btn_loadDemand)
            loadTrafficDemands();
        else if (src == btn_save)
            saveDesign();
        else if (src == btn_showNodeName)
	    canvas.showNodeName();
	else if (src == btn_takeSnapshot)
	    canvas.takeSnapshot();
	else if (src == btn_zoomIn)
	    canvas.zoomIn();
	else if (src == btn_zoomOut)
	    canvas.zoomOut();
	else if (src == btn_zoomAll)
	    canvas.zoomAll();
        else if (src == btn_reset)
            callback.reset();
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

    /**
     *
     */
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
    
    /**
     *
     */
    public void saveDesign()
    {
        try
        {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showSaveDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            List<NetPlan> netPlans = callback.getDesign();
            if (netPlans.isEmpty()) return;
            
            if (netPlans.size() > 1) fc_netPlan.saveNetPlans(netPlans);
            else fc_netPlan.saveNetPlan(netPlans.get(0));
            
            ErrorHandling.showInformationDialog("Design saved successfully", "Save design");
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

    /**
     *
     */
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