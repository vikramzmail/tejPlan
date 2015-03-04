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

package com.tejas.engine.tools;

import cern.colt.matrix.tdouble.DoubleFactory1D;

import com.tejas.engine.tools.GUISimulationTemplate;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.impl.ConnectionSimulation;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.CellRenderers;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.CurrentAndPlannedStateTableSorter;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.FixedColumnDecorator;
import com.tejas.engine.utils.FullScrollPaneLayout;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.StringLabeller;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.TableCursorNavigation;
import com.tejas.engine.utils.WiderJComboBox;
import com.tejas.engine.utils.CellRenderers.CurrentAndPlannedStateCellRenderer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import net.miginfocom.swing.MigLayout;

/**
 * Graphical-user interface for the connection-admission-control simulation.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class GUIConnectionSimulation extends GUISimulationTemplate
{
    private static final long serialVersionUID = 1L;

    private JTable[] netPlanTables;
    private FixedColumnDecorator[] decorators;

    private JTextField networkName;
    private JTextArea networkDescription;
    
    /**
     * Default constructor.
     * 
     * @since 0.2.2
     */
    public GUIConnectionSimulation()
    {
        super("CONNECTION-ADMISSION-CONTROL SIMULATION", new ConnectionSimulation(), SimulatorType.ASYNCHRONOUS);
    }

    private final static String[][] netPlanTablesHeader = new String[][]
    {
	{ "Id", "Name", "xCoord", "yCoord", "Ingress traffic (E)", "Egress traffic (E)", "Traversing traffic (E)", "Total traffic (E)", "Attributes" },
	{ "Id", "Origin node", "Destination node", "Capacity (E)", "Carried traffic (E)", "Utilization", "Length (km)", "# Routes", "Is bottleneck?", "Attributes" },
	{ "Id", "Ingress node", "Egress node", "Offered traffic (E)", "Carried traffic (E)", "Bifurcated", "Attributes" },
	{ "Id", "Demand", "Ingress node", "Egress node", "Carried traffic (E)", "Sequence of links", "Sequence of nodes", "Attributes" },
	{ "Id", "Demand", "Ingress node", "Egress node", "Arrival time", "Duration", "Finish time", "Requested traffic volume (E)", "Current traffic volume (E)", "Bifurcated", "Attributes" },
	{ "Id", "Connection", "Ingress node", "Egress node", "Carried traffic (E)", "Sequence of links", "Sequence of nodes", "Attributes" },
	{ "Id", "Origin node", "Destination node", "Reserved bandwidth (E)", "Sequence of links", "Sequence of nodes", "Length (km)", "Dedicated/Shared", "# Routes", "Attributes"},
        { "Id", "MTTF (hours)", "MTTR (hours)", "Nodes", "Links", "# Affected routes", "Attributes" },
        { "Attribute", "Value" }
    };

    @Override
    public Pair<JComponent, String> getNetPlanView()
    {
	JTabbedPane tabPane = new JTabbedPane();

	netPlanTables = new JTable[netPlanTablesHeader.length];
	final JScrollPane[] scrollPane = new JScrollPane[netPlanTables.length];
	DefaultTableModel[] model = new DefaultTableModel[netPlanTables.length];
	ScrollPaneLayout[] scrollPaneLayout = new ScrollPaneLayout[netPlanTables.length];

	for (int modelId = 0; modelId < model.length; modelId++)
	{
	    Object[][] data = new Object[1][netPlanTablesHeader[modelId].length];
	    String[] header = netPlanTablesHeader[modelId];
	    model[modelId] = new ClassAwareTableModel(data, header);
	}

	KeyListener cursorNavigation = new TableCursorNavigation();

	decorators = new FixedColumnDecorator[netPlanTables.length - 1];

	for (int itemId = 0; itemId < netPlanTables.length; itemId++)
	{
	    netPlanTables[itemId] = new AdvancedJTable(model[itemId]);
            
            switch(itemId)
            {
                case 0: // Nodes
                case 1: // Links
                case 2: // Demands
                    netPlanTables[itemId].setRowSorter(new CurrentAndPlannedStateTableSorter(model[itemId]));
                    break;
                    
                default:
                    netPlanTables[itemId].setAutoCreateRowSorter(true);
                    break;
            }
            
	    scrollPane[itemId] = new JScrollPane(netPlanTables[itemId]);
	    scrollPaneLayout[itemId] = new FullScrollPaneLayout();
	    scrollPane[itemId].setLayout(scrollPaneLayout[itemId]);
	    scrollPane[itemId].setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	    netPlanTables[itemId].addKeyListener(cursorNavigation);

	    if (itemId < netPlanTables.length - 1)
	    {
//		TableRowFilterSupport.forTable(netPlanTables[itemId]).apply();
		decorators[itemId] = new FixedColumnDecorator(1, scrollPane[itemId]);
		decorators[itemId].getFixedTable().addKeyListener(cursorNavigation);
	    }
	}

        networkName = new JTextField();
        networkDescription = new JTextArea();
        networkName.setEditable(false);
        networkDescription.setEditable(false);
        networkDescription.setFont(new JLabel().getFont());
	networkDescription.setLineWrap(true);
	networkDescription.setWrapStyleWord(true);
        
	DefaultCaret caret = (DefaultCaret) networkDescription.getCaret();
	caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        
        JPanel networkPanel = new JPanel(new MigLayout("", "[][grow]", "[][][grow]"));
        networkPanel.add(new JLabel("Name"));
        networkPanel.add(networkName, "grow, wrap");
        networkPanel.add(new JLabel("Description"), "aligny top");
        networkPanel.add(new JScrollPane(networkDescription), "grow, wrap, height 100::");
        networkPanel.add(scrollPane[8], "grow, spanx 2");

        tabPane.addTab("Network", networkPanel);
	tabPane.addTab("Nodes", scrollPane[0]);
	tabPane.addTab("Links", scrollPane[1]);
	tabPane.addTab("Demands", scrollPane[2]);
	tabPane.addTab("Routes", scrollPane[3]);
	tabPane.addTab("Active connections", scrollPane[4]);
	tabPane.addTab("Active connection routes", scrollPane[5]);
	tabPane.addTab("Protection segments", scrollPane[6]);
	tabPane.addTab("Shared-risk groups", scrollPane[7]);

	MouseAdapter nodeTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[0].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasNodes() && row != -1)
		{
		    int nodeId = netPlanTables[0].convertRowIndexToModel(row);
		    nodeId = (int) Math.floor((double) nodeId / 2);

		    showNode(nodeId);
		}
	    }
	};

	netPlanTables[0].addMouseListener(nodeTableAdapter);
	decorators[0].getFixedTable().addMouseListener(nodeTableAdapter);

	MouseAdapter linkTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[1].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasLinks() && row != -1)
		{
		    int linkId = netPlanTables[1].convertRowIndexToModel(row);
		    linkId = (int) Math.floor((double) linkId / 2);

		    switch (e.getClickCount())
		    {
			case 1:
			    showLink(linkId);
			    break;
			case 2:

			    int col = netPlanTables[1].convertColumnIndexToModel(netPlanTables[1].columnAtPoint(e.getPoint()));
			    if (col == -1 || col >= netPlanTables[1].getColumnCount()) return;

                            switch (col)
			    {
				case 1:
				    showNode(netPlan.getLinkOriginNode(linkId));
				    break;
				case 2:
				    showNode(netPlan.getLinkDestinationNode(linkId));
				    break;
                                default:
                                    break;
			    }
			    break;
		    }
		}
	    }
	};

	netPlanTables[1].addMouseListener(linkTableAdapter);
	decorators[1].getFixedTable().addMouseListener(linkTableAdapter);

	MouseAdapter demandTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[2].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasDemands() && row != -1)
		{
		    int demandId = netPlanTables[2].convertRowIndexToModel(row);
		    demandId = (int) Math.floor((double) demandId / 2);
                    
		    switch (e.getClickCount())
		    {
			case 1:
			    showDemand(demandId);
			    break;
			case 2:

			    int col = netPlanTables[2].convertColumnIndexToModel(netPlanTables[2].columnAtPoint(e.getPoint()));
			    if (col == -1 || col >= netPlanTables[2].getColumnCount()) return;

                            switch (col)
			    {
				case 1:
				    showNode(netPlan.getDemandIngressNode(demandId));
				    break;
				case 2:
				    showNode(netPlan.getDemandEgressNode(demandId));
				    break;
                                default:
                                    break;
			    }
			    break;
		    }
		}
	    }
	};

	netPlanTables[2].addMouseListener(demandTableAdapter);
	decorators[2].getFixedTable().addMouseListener(demandTableAdapter);

	MouseAdapter routesTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[3].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasRoutes() && row != -1)
		{
		    int routeId = netPlanTables[3].convertRowIndexToModel(row);

		    switch (e.getClickCount())
		    {
			case 1:
			    showRoute(routeId);
			    break;
			case 2:

			    int col = netPlanTables[3].convertColumnIndexToModel(netPlanTables[3].columnAtPoint(e.getPoint()));
			    if (col == -1 || col >= netPlanTables[3].getColumnCount()) return;

			    int demandId = netPlan.getRouteDemand(routeId);
			    switch (col)
			    {
				case 1:
				    showDemand(demandId);
				    break;
				case 2:
				    showNode(netPlan.getDemandIngressNode(demandId));
				    break;
				case 3:
				    showNode(netPlan.getDemandEgressNode(demandId));
				    break;
                                default:
                                    break;
			    }
			    break;
		    }
		}
	    }
	};

	netPlanTables[3].addMouseListener(routesTableAdapter);
	decorators[3].getFixedTable().addMouseListener(routesTableAdapter);

	MouseAdapter connectionsTableAdapter = new MouseAdapter()
	{
	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[4].rowAtPoint(e.getPoint());
                
                ConnectionNetState netState = (ConnectionNetState) simKernel.getNetState();

		if (!SwingUtilities.isRightMouseButton(e) && netState != null && netState.hasConnections() && row != -1)
		{
                    row = netPlanTables[4].convertRowIndexToModel(row);
		    long connId = (Long) netPlanTables[4].getModel().getValueAt(row, 0);

		    showConnection(connId);
		}
	    }
	};

        netPlanTables[4].addMouseListener(connectionsTableAdapter);
	decorators[4].getFixedTable().addMouseListener(connectionsTableAdapter);
        
	MouseAdapter connectionRoutesTableAdapter = new MouseAdapter()
	{
	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[5].rowAtPoint(e.getPoint());
                
                ConnectionNetState netState = (ConnectionNetState) simKernel.getNetState();

		if (!SwingUtilities.isRightMouseButton(e) && netState != null && netState.hasConnections() && row != -1)
		{
                    row = netPlanTables[4].convertRowIndexToModel(row);
		    long connRouteId = (Long) netPlanTables[5].getModel().getValueAt(row, 0);

		    showConnectionRoute(connRouteId);
		}
	    }
	};

        netPlanTables[5].addMouseListener(connectionRoutesTableAdapter);
	decorators[5].getFixedTable().addMouseListener(connectionRoutesTableAdapter);

	MouseAdapter segmentsTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[6].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasProtectionSegments() && row != -1)
		{
		    int segmentId = netPlanTables[6].convertRowIndexToModel(row);

		    showSegment(segmentId);
		}
	    }
	};

	netPlanTables[6].addMouseListener(segmentsTableAdapter);
	decorators[6].getFixedTable().addMouseListener(segmentsTableAdapter);

	MouseAdapter srgsTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[7].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasProtectionSegments() && row != -1)
		{
		    int srgId = netPlanTables[7].convertRowIndexToModel(row);

		    showSRG(srgId);
		}
	    }
	};

	netPlanTables[7].addMouseListener(srgsTableAdapter);
	decorators[7].getFixedTable().addMouseListener(srgsTableAdapter);

        JCheckBox togglePlanned = new JCheckBox("Toggle show/hide planning information", true);
	togglePlanned.addItemListener(new ItemListener()
	{

	    @Override
	    public void itemStateChanged(ItemEvent e)
	    {
		RowFilter rowFilter = e.getStateChange() == ItemEvent.SELECTED ? null : new RowFilter()
		{

		    @Override
		    public boolean include(RowFilter.Entry entry)
		    {
			return (Integer) entry.getIdentifier() % 2 == 0 ? true : false;
		    }
		};

		for (int tableId = 0; tableId < 3; tableId++)
		{
		    ((TableRowSorter) netPlanTables[tableId].getRowSorter()).setRowFilter(rowFilter);
		}

	    }
	});

	togglePlanned.setSelected(false);

	JPanel pane = new JPanel();
	pane.setLayout(new BorderLayout());
	pane.add(tabPane, BorderLayout.CENTER);
	pane.add(togglePlanned, BorderLayout.NORTH);

	return Pair.of((JComponent) pane, "View current network state");
    }

    @Override
    public void updateNetPlanView()
    {
	for (int tableId = 0; tableId < netPlanTables.length; tableId++)
	{
	    netPlanTables[tableId].setEnabled(false);
	    ((DefaultTableModel) netPlanTables[tableId].getModel()).setDataVector(new Object[1][netPlanTablesHeader[tableId].length], netPlanTablesHeader[tableId]);
	}
        
        NetPlan netPlan = simKernel.getNetPlan();
        ConnectionNetState netState = (ConnectionNetState) simKernel.getNetState();

        int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int D = netPlan.getNumberOfDemands();
	int R = netPlan.getNumberOfRoutes();
	int C = netState.getNumberOfConnections();
	int CR = netState.getNumberOfConnectionRoutes();
	int S = netPlan.getNumberOfProtectionSegments();
        int numSRGs = netPlan.getNumberOfSRGs();
	Map<String, String> networkAttributes = netPlan.getNetworkAttributes();

	double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

	Object[][] nodeData = new Object[2 * N][netPlanTablesHeader[0].length];
	Object[][] linkData = new Object[2 * E][netPlanTablesHeader[1].length];
	Object[][] demandData = new Object[2 * D][netPlanTablesHeader[2].length];
	Object[][] routeData = new Object[R][netPlanTablesHeader[3].length];
	Object[][] connectionData = new Object[C][netPlanTablesHeader[4].length];
	Object[][] connectionRouteData = new Object[CR][netPlanTablesHeader[5].length];
	Object[][] segmentData = new Object[S][netPlanTablesHeader[6].length];
	Object[][] srgData = new Object[numSRGs][netPlanTablesHeader[7].length];
	Object[][] networkData = new Object[networkAttributes.size()][netPlanTablesHeader[8].length];

	double[] u_e_planned = netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector();
	double[] u_e_current = netPlan.getLinkCapacityInErlangsVector();
	double[] y_e_planned = netPlan.getLinkCarriedTrafficInErlangsVector();
	double[] y_e_current = netState.getLinkCurrentCarriedTrafficInErlangsVector();
	double[] x_p = netPlan.getRouteCarriedTrafficInErlangsVector();
	double[] rho_e_planned = new double[E];
	double[] rho_e_current = new double[E];

	for (int linkId = 0; linkId < E; linkId++)
	{
	    rho_e_planned[linkId] = u_e_planned[linkId] == 0 ? 0 : y_e_planned[linkId] / u_e_planned[linkId];
	    rho_e_current[linkId] = u_e_current[linkId] == 0 ? 0 : y_e_current[linkId] / u_e_current[linkId];
	}

	if (N > 0)
	{
	    double[] currentIngressTrafficPerNode = netState.getNodeCurrentIngressTrafficInErlangsVector();
	    double[] currentEgressTrafficPerNode = netState.getNodeCurrentEgressTrafficInErlangsVector();
	    double[] currentTraversingTrafficPerNode = netState.getNodeCurrentTraversingTrafficInErlangsVector();
	    double[] plannedIngressTrafficPerNode = netPlan.getNodeIngressTrafficInErlangsVector();
	    double[] plannedEgressTrafficPerNode = netPlan.getNodeEgressTrafficInErlangsVector();
	    double[] plannedTraversingTrafficPerNode = netPlan.getNodeTraversingTrafficInErlangsVector();

	    for (int nodeId = 0; nodeId < N; nodeId++)
	    {
		nodeData[2 * nodeId][0] = nodeId;
		nodeData[2 * nodeId + 1][0] = null;

		nodeData[2 * nodeId][1] = netPlan.getNodeName(nodeId);
		nodeData[2 * nodeId + 1][1] = null;

		nodeData[2 * nodeId][2] = netPlan.getNodeXYPosition(nodeId)[0];
		nodeData[2 * nodeId + 1][2] = null;

		nodeData[2 * nodeId][3] = netPlan.getNodeXYPosition(nodeId)[1];
		nodeData[2 * nodeId + 1][3] = null;

		nodeData[2 * nodeId][4] = currentIngressTrafficPerNode[nodeId];
		nodeData[2 * nodeId + 1][4] = plannedIngressTrafficPerNode[nodeId];

		nodeData[2 * nodeId][5] = currentEgressTrafficPerNode[nodeId];
		nodeData[2 * nodeId + 1][5] = plannedEgressTrafficPerNode[nodeId];

		nodeData[2 * nodeId][6] = currentTraversingTrafficPerNode[nodeId];
		nodeData[2 * nodeId + 1][6] = plannedTraversingTrafficPerNode[nodeId];

		nodeData[2 * nodeId][7] = currentIngressTrafficPerNode[nodeId] + currentEgressTrafficPerNode[nodeId] + currentTraversingTrafficPerNode[nodeId];
		nodeData[2 * nodeId + 1][7] = plannedIngressTrafficPerNode[nodeId] + plannedEgressTrafficPerNode[nodeId] + plannedTraversingTrafficPerNode[nodeId];

		nodeData[2 * nodeId][8] = StringUtils.mapToString(netPlan.getNodeSpecificAttributes(nodeId), "=", ", ");
		nodeData[2 * nodeId + 1][8] = null;
	    }

	    netPlanTables[0].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[0].getModel()).setDataVector(nodeData, netPlanTablesHeader[0]);

	    for (int columnId = 0; columnId < decorators[0].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[0].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new CurrentAndPlannedStateCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[0].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[0].getColumnModel().getColumn(columnId).setCellRenderer(new CurrentAndPlannedStateCellRenderer(true));
	}

	if (E > 0)
	{
	    double maxRho_planned = DoubleFactory1D.dense.make(rho_e_planned).getMaxLocation()[0];
	    double maxRho_current = DoubleFactory1D.dense.make(rho_e_current).getMaxLocation()[0];

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int traversingRoutes_planned = netPlan.getLinkTraversingRoutes(linkId).length;
		int traversingRoutes_current = netState.getLinkCurrentTraversingConnectionRoutes(linkId).length;

		linkData[2 * linkId][0] = linkId;
		linkData[2 * linkId + 1][0] = null;

		linkData[2 * linkId][1] = netPlan.getLinkOriginNode(linkId);
		linkData[2 * linkId + 1][1] = null;

		linkData[2 * linkId][2] = netPlan.getLinkDestinationNode(linkId);
		linkData[2 * linkId + 1][2] = null;

		linkData[2 * linkId][3] = netPlan.getLinkCapacityInErlangs(linkId);
		linkData[2 * linkId + 1][3] = null;

		linkData[2 * linkId][4] = y_e_current[linkId];
		linkData[2 * linkId + 1][4] = y_e_planned[linkId];

		linkData[2 * linkId][5] = rho_e_current[linkId];
		linkData[2 * linkId + 1][5] = rho_e_planned[linkId];

		linkData[2 * linkId][6] = netPlan.getLinkLengthInKm(linkId);
		linkData[2 * linkId + 1][6] = null;

		linkData[2 * linkId][7] = traversingRoutes_current;
		linkData[2 * linkId + 1][7] = traversingRoutes_planned;

		linkData[2 * linkId][8] = DoubleUtils.isEqualWithinRelativeTolerance(maxRho_current, rho_e_current[linkId], PRECISIONFACTOR) ? "Yes" : "No";
		linkData[2 * linkId + 1][8] = DoubleUtils.isEqualWithinRelativeTolerance(maxRho_planned, rho_e_planned[linkId], PRECISIONFACTOR) ? "Yes" : "No";

		linkData[2 * linkId][9] = StringUtils.mapToString(netPlan.getLinkSpecificAttributes(linkId), "=", ", ");
		linkData[2 * linkId + 1][9] = null;
	    }

	    netPlanTables[1].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[1].getModel()).setDataVector(linkData, netPlanTablesHeader[1]);

	    for (int columnId = 0; columnId < decorators[1].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[1].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new CurrentAndPlannedStateCellRenderer(true));

            for (int columnId = 0; columnId < netPlanTables[1].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[1].getColumnModel().getColumn(columnId).setCellRenderer(new CurrentAndPlannedStateCellRenderer(true));
	}

        if (D > 0)
	{
	    for (int demandId = 0; demandId < D; demandId++)
	    {
                int[] routeIds = netPlan.getDemandRoutes(demandId);
                long[] connectionIds = netState.getDemandCurrentConnections(demandId);

                double planned_h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);
                double plannedCarriedTraffic = 0;
                double currentCarriedTraffic = 0;

                int numberOfRoutesCarryingTraffic = 0;
                int numberOfConnectionsCarryingTraffic = 0;

                for(int routeId : routeIds)
                {
		    if (x_p[routeId] > PRECISIONFACTOR)
		    {
			plannedCarriedTraffic += x_p[routeId];
			numberOfRoutesCarryingTraffic++;
		    }
                }
                
                double current_h_d = 0;

                for(long connId : connectionIds)
                {
                    current_h_d += netState.getConnectionRequestedTrafficInErlangs(connId);
		    double trafficVolume = netState.getConnectionCurrentCarriedTrafficInErlangs(connId);

		    if (trafficVolume > PRECISIONFACTOR)
		    {
			currentCarriedTraffic += trafficVolume;
			numberOfConnectionsCarryingTraffic++;
		    }
                }

		demandData[2 * demandId][0] = demandId;
		demandData[2 * demandId + 1][0] = null;

		demandData[2 * demandId][1] = netPlan.getDemandIngressNode(demandId);
		demandData[2 * demandId + 1][1] = null;

		demandData[2 * demandId][2] = netPlan.getDemandEgressNode(demandId);
		demandData[2 * demandId + 1][2] = null;

		demandData[2 * demandId][3] = current_h_d;
		demandData[2 * demandId + 1][3] = planned_h_d;

		demandData[2 * demandId][4] = currentCarriedTraffic;
		demandData[2 * demandId + 1][4] = plannedCarriedTraffic;

		demandData[2 * demandId][5] = numberOfConnectionsCarryingTraffic > 1 ? "Yes (" + numberOfConnectionsCarryingTraffic + ")": "No";
		demandData[2 * demandId + 1][5] = numberOfRoutesCarryingTraffic > 1 ? "Yes (" + numberOfRoutesCarryingTraffic + ")" : "No";

		demandData[2 * demandId][6] = StringUtils.mapToString(netPlan.getDemandSpecificAttributes(demandId), "=", ", ");
		demandData[2 * demandId + 1][6] = null;
	    }

	    netPlanTables[2].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[2].getModel()).setDataVector(demandData, netPlanTablesHeader[2]);

	    for (int columnId = 0; columnId < decorators[2].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[2].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new CellRenderers.CurrentAndPlannedStateCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[2].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[2].getColumnModel().getColumn(columnId).setCellRenderer(new CellRenderers.CurrentAndPlannedStateCellRenderer(true));
        }

	if (R > 0)
	{
	    for (int routeId = 0; routeId < R; routeId++)
	    {
		int demandId = netPlan.getRouteDemand(routeId);

		routeData[routeId][0] = routeId;
		routeData[routeId][1] = demandId;
		routeData[routeId][2] = netPlan.getDemandIngressNode(demandId);
		routeData[routeId][3] = netPlan.getDemandEgressNode(demandId);
		routeData[routeId][4] = x_p[routeId];
		routeData[routeId][5] = IntUtils.join(netPlan.getRouteSequenceOfLinks(routeId), " => ");
		routeData[routeId][6] = IntUtils.join(netPlan.getRouteSequenceOfNodes(routeId), " => ");
		routeData[routeId][7] = StringUtils.mapToString(netPlan.getRouteSpecificAttributes(routeId), "=", ", ");
	    }

	    netPlanTables[3].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[3].getModel()).setDataVector(routeData, netPlanTablesHeader[3]);
	}

	if (C > 0)
	{
            long[] connectionIds = netState.getConnectionIds();
            
            for(int seqId = 0; seqId < C; seqId++)
            {
                long connectionId = connectionIds[seqId];
                int demandId = netState.getConnectionDemand(connectionId);
                int ingressNodeId = netPlan.getDemandIngressNode(demandId);
                int egressNodeId = netPlan.getDemandEgressNode(demandId);
                
                int numRoutes = netState.getConnectionRoutes(connectionId).length;

                connectionData[seqId][0] = connectionId;
                connectionData[seqId][1] = demandId;
                connectionData[seqId][2] = String.format("n%d (%s)", ingressNodeId, netPlan.getNodeName(ingressNodeId));
                connectionData[seqId][3] = String.format("n%d (%s)", egressNodeId, netPlan.getNodeName(egressNodeId));
                connectionData[seqId][4] = SimEvent.secondsToYearsDaysHoursMinutesSeconds(netState.getConnectionArrivalTime(connectionId));
                connectionData[seqId][5] = SimEvent.secondsToYearsDaysHoursMinutesSeconds(netState.getConnectionDuration(connectionId));
                connectionData[seqId][6] = SimEvent.secondsToYearsDaysHoursMinutesSeconds(netState.getConnectionArrivalTime(connectionId) + netState.getConnectionDuration(connectionId));
                connectionData[seqId][7] = netState.getConnectionRequestedTrafficInErlangs(connectionId);
                connectionData[seqId][8] = netState.getConnectionCurrentCarriedTrafficInErlangs(connectionId);
                connectionData[seqId][9] = numRoutes > 1 ? "Yes" : "No";
                connectionData[seqId][10] = StringUtils.mapToString(netState.getConnectionAttributes(connectionId), "=", ", ");
            }

	    netPlanTables[4].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[4].getModel()).setDataVector(connectionData, netPlanTablesHeader[4]);
	}

	if (CR > 0)
	{
            long[] connectionRouteIds = netState.getConnectionRouteIds();
            
            for(int seqId = 0; seqId < CR; seqId++)
            {
                long connectionRouteId = connectionRouteIds[seqId];
                long connectionId = netState.getConnectionRouteConnection(connectionRouteId);
                int demandId = netState.getConnectionDemand(connectionId);
                int ingressNodeId = netPlan.getDemandIngressNode(demandId);
                int egressNodeId = netPlan.getDemandEgressNode(demandId);
                
                connectionRouteData[seqId][0] = connectionRouteId;
                connectionRouteData[seqId][1] = connectionId;
                connectionRouteData[seqId][2] = String.format("n%d (%s)", ingressNodeId, netPlan.getNodeName(ingressNodeId));
                connectionRouteData[seqId][3] = String.format("n%d (%s)", egressNodeId, netPlan.getNodeName(egressNodeId));
                connectionRouteData[seqId][4] = netState.getConnectionRouteCarriedTrafficInErlangs(connectionRouteId);
                connectionRouteData[seqId][5] = IntUtils.join(netState.getConnectionRouteSequenceOfLinks(connectionRouteId), " => ");
                connectionRouteData[seqId][6] = IntUtils.join(netPlan.convertSequenceOfLinks2SequenceOfNodes(netState.getConnectionRouteSequenceOfLinks(connectionRouteId)), " => ");
                connectionRouteData[seqId][7] = StringUtils.mapToString(netState.getConnectionRouteAttributes(connectionRouteId), "=", ", ");
            }

	    netPlanTables[5].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[5].getModel()).setDataVector(connectionRouteData, netPlanTablesHeader[5]);
	}
        
	if (S > 0)
	{
	    for (int segmentId = 0; segmentId < S; segmentId++)
	    {
		double segmentLength = 0;
		int[] sequenceOfLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
		int[] sequenceOfNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
		for (int linkId : sequenceOfLinks)
		{
		    segmentLength += netPlan.getLinkLengthInKm(linkId);
		}

		int numberOfRoutes = 0;
		for (int routeId = 0; routeId < R; routeId++)
		{
		    int[] backupSegments = netPlan.getRouteBackupSegmentList(routeId);
		    Arrays.sort(backupSegments);
		    if (Arrays.binarySearch(backupSegments, segmentId) >= 0)
		    {
			numberOfRoutes++;
		    }
		}

		segmentData[segmentId][0] = segmentId;
		segmentData[segmentId][1] = netPlan.getProtectionSegmentOriginNode(segmentId);
		segmentData[segmentId][2] = netPlan.getProtectionSegmentDestinationNode(segmentId);
		segmentData[segmentId][3] = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
		segmentData[segmentId][4] = IntUtils.join(sequenceOfLinks, " => ");
		segmentData[segmentId][5] = IntUtils.join(sequenceOfNodes, " => ");
		segmentData[segmentId][6] = segmentLength;
		segmentData[segmentId][7] = numberOfRoutes > 1 ? String.format("Shared (%d routes)", numberOfRoutes) : (numberOfRoutes == 0 ? "Not used" : "Dedicated");
		segmentData[segmentId][8] = StringUtils.mapToString(netPlan.getProtectionSegmentSpecificAttributes(segmentId), "=", ", ");
	    }

	    netPlanTables[6].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[6].getModel()).setDataVector(segmentData, netPlanTablesHeader[6]);
	}
        
        if (numSRGs > 0)
        {
            for (int srgId = 0; srgId < numSRGs; srgId++)
            {
                int[] routeIds = netPlan.getSRGRoutes(srgId);
                int numRoutes = routeIds.length;
                
                srgData[srgId][0] = srgId;
                srgData[srgId][1] = netPlan.getSRGMeanTimeToFailInHours(srgId);
                srgData[srgId][2] = netPlan.getSRGMeanTimeToRepairInHours(srgId);
                srgData[srgId][3] = IntUtils.join(netPlan.getSRGNodes(srgId), ", ");
                srgData[srgId][4] = IntUtils.join(netPlan.getSRGLinks(srgId), ", ");
                srgData[srgId][5] = numRoutes + (numRoutes > 0 ? " (" + IntUtils.join(routeIds, ",")  + ")" : "");
                srgData[srgId][6] = StringUtils.mapToString(netPlan.getSRGSpecificAttributes(srgId), "=", ", ");
            }

            netPlanTables[7].setEnabled(true);
            ((DefaultTableModel) netPlanTables[7].getModel()).setDataVector(srgData, netPlanTablesHeader[7]);
        }

        int attribId = 0;
	for(Map.Entry<String, String> entry : networkAttributes.entrySet())
	{
	    networkData[attribId][0] = entry.getKey();
	    networkData[attribId][1] = entry.getValue();
	}

        networkName.setText(netPlan.getNetworkName());
        networkDescription.setText(netPlan.getNetworkDescription());
	networkDescription.setCaretPosition(0);
        
	netPlanTables[8].setEnabled(true);
	((DefaultTableModel) netPlanTables[8].getModel()).setDataVector(networkData, netPlanTablesHeader[8]);

        for (FixedColumnDecorator decorator : decorators)
        {
            List<JTable> list = new LinkedList<JTable>();
            list.add(decorator.getFixedTable());
            list.add(decorator.getMainTable());
            
            for(JTable table : list)
            {
                table.setDefaultRenderer(Boolean.class, new CellRenderers.CheckBoxRenderer(true));
                table.setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(Object.class, new CellRenderers.NonEditableCellRenderer(true));
                table.setDefaultRenderer(Float.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(Integer.class, new CellRenderers.NumberCellRenderer(true));
                table.setDefaultRenderer(String.class, new CellRenderers.NonEditableCellRenderer(true));
            }
        }

	for (JTable table : netPlanTables)
	    table.revalidate();
    }
    
    @Override
    public void addManuallyEvent()
    {
        final NetPlan netPlan = simKernel.getNetPlan();
        
        int D = netPlan.getNumberOfDemands();
        
        if (D == 0)
        {
            ErrorHandling.showErrorDialog("There are no traffic demands", "Error including new connection request");
            return;
        }

        JComboBox cmb_demandSelector = new WiderJComboBox();
        for(int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);
            String[] nodePairNames = netPlan.getNodeNames(new int[] { ingressNodeId, egressNodeId });
            
            String demandString = String.format("d%d [n%d (%s) -> n%d (%s)]", demandId, ingressNodeId, nodePairNames[0], egressNodeId, nodePairNames[1]);
            cmb_demandSelector.addItem(demandString);
        }
        
        cmb_demandSelector.setSelectedIndex(0);
        
        JTextField txt_trafficVolume = new JTextField();
        JTextField txt_arrivalTime = new JTextField();
        JTextField txt_holdingTime = new JTextField();

        JPanel pane = new JPanel(new GridLayout(0, 2));
        pane.add(new JLabel("Traffic demand: "));
        pane.add(cmb_demandSelector);
        pane.add(new JLabel("Traffic volume (in Erlangs): "));
        pane.add(txt_trafficVolume);
        pane.add(new JLabel("Arrival time (in seconds, from now): "));
        pane.add(txt_arrivalTime);
        pane.add(new JLabel("Holding time (in seconds): "));
        pane.add(txt_holdingTime);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Configure new connection request", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try
            {
                int demandId = cmb_demandSelector.getSelectedIndex();
                double trafficVolume = Double.parseDouble(txt_trafficVolume.getText());
                double arrivalTime = Double.parseDouble(txt_arrivalTime.getText());
                double holdingTime = Double.parseDouble(txt_holdingTime.getText());
                
                if (trafficVolume <= 0) throw new RuntimeException("Traffic volume must be greater than zero");
                if (arrivalTime < 0) throw new RuntimeException("Arrival time must be greater or equal than zero");
                if (holdingTime <= 0) throw new RuntimeException("Holding time must be greater than zero");
                
                double simTime = simKernel.getSimCore().getFutureEventList().getCurrentSimulationTime();
                CACEvent newEvent = new CACEvent(simTime + arrivalTime, demandId, holdingTime, trafficVolume, null);
                
                List<CACEvent> aux1 = new LinkedList<CACEvent>();
                aux1.add(newEvent);
                
                List<? extends SimEvent> aux = simKernel.checkEvents(aux1);
                simKernel.getSimCore().getFutureEventList().addEvents(aux);

                break;
            }
            catch (Throwable ex)
            {
                ErrorHandling.showErrorDialog(ex.getMessage(), "Error including new connection request");
            }
        }
    }
    
    private void showConnection(long connId)
    {
	NetPlan aux_netPlan = simKernel.getNetPlan();
        ConnectionNetState netState = (ConnectionNetState) simKernel.getNetState();
        
        int demandId = netState.getConnectionDemand(connId);
        
	pan_topology.getCanvas().showNodes(new int[] { aux_netPlan.getDemandIngressNode(demandId), aux_netPlan.getDemandEgressNode(demandId) });
	pan_topology.getCanvas().refresh();
    }

    private void showConnectionRoute(long connRouteId)
    {
        ConnectionNetState netState = (ConnectionNetState) simKernel.getNetState();
	showRoute(netState.getConnectionRouteSequenceOfLinks(connRouteId));
    }

    private static class DemandSelector implements ActionListener {

        private final JComboBox cmb_routeSelector;
        private final NetPlan netPlan;
        private final double[] spareCapacityInErlangs;

        public DemandSelector(JComboBox cmb_routeSelector, NetPlan netPlan, double[] spareCapacityInErlangs) {
            this.cmb_routeSelector = cmb_routeSelector;
            this.netPlan = netPlan;
            this.spareCapacityInErlangs = spareCapacityInErlangs;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int demandId = ((JComboBox) e.getSource()).getSelectedIndex();
            
            cmb_routeSelector.removeAllItems();
            cmb_routeSelector.addItem(StringLabeller.of(-1, "No route"));
            
            int[] routeIds = netPlan.getDemandRoutes(demandId);
            for(int routeId : routeIds)
            {
                int[] seqLinks = netPlan.getRouteSequenceOfLinks(routeId);
                double spareCapacity = DoubleUtils.minValue(DoubleUtils.select(spareCapacityInErlangs, seqLinks));
                
                String routeLabel = String.format("r%d (spare capacity = %f E)", routeId, spareCapacity);
                cmb_routeSelector.addItem(StringLabeller.of(routeId, routeLabel));
            }
            
        }
    }
}