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
import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceEvent;
import com.tejas.engine.interfaces.resilienceSimulation.ResilienceNetState;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.impl.ResilienceSimulation;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.CellRenderers;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.CurrentAndPlannedStateTableSorter;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.FixedColumnDecorator;
import com.tejas.engine.utils.FullScrollPaneLayout;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.StringLabeller;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.TableCursorNavigation;
import com.tejas.engine.utils.WiderJComboBox;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
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
 * Graphical-user interface for the resilience simulation.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class GUIResilienceSimulation extends GUISimulationTemplate
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
    public GUIResilienceSimulation()
    {
        super("RESILIENCE SIMULATION", new ResilienceSimulation(), SimulatorType.ASYNCHRONOUS);
    }

    private final static String[][] netPlanTablesHeader = new String[][]
    {
	{ "Id", "Name", "xCoord", "yCoord", "Ingress traffic (E)", "Egress traffic (E)", "Traversing traffic (E)", "Total traffic (E)", "Attributes" },
	{ "Id", "Origin node", "Destination node", "Capacity (E)", "Carried traffic (E)", "Utilization", "Length (km)", "# Routes", "Is bottleneck?", "Attributes" },
	{ "Id", "Ingress node", "Egress node", "Offered traffic (E)", "Carried traffic (E)", "% Lost traffic", "Bifurcated", "Attributes" },
	{ "Id", "Demand", "Ingress node", "Egress node", "Carried traffic (E)", "Sequence of links", "Sequence of nodes", "Bottleneck utilization", "Backup segments", "Attributes" },
	{ "Id", "Origin node", "Destination node", "Used bandwidth (E)", "Reserved bandwidth (E)", "Sequence of links", "Sequence of nodes", "Length (km)", "Dedicated/Shared", "Attributes" },
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
	    if (itemId < netPlanTables.length - 3)
		netPlanTables[itemId].setRowSorter(new CurrentAndPlannedStateTableSorter(model[itemId]));
	    else
		netPlanTables[itemId].setAutoCreateRowSorter(true);

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
        networkPanel.add(scrollPane[6], "grow, spanx 2");

        tabPane.addTab("Network", networkPanel);
	tabPane.addTab("Nodes", scrollPane[0]);
	tabPane.addTab("Links", scrollPane[1]);
	tabPane.addTab("Demands", scrollPane[2]);
	tabPane.addTab("Routes", scrollPane[3]);
	tabPane.addTab("Protection segments", scrollPane[4]);
	tabPane.addTab("Shared-risk groups", scrollPane[5]);

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
                ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
                
                if (!SwingUtilities.isRightMouseButton(e) && netState.hasRoutes() && row != -1)
		{
		    int itemId = netPlanTables[3].convertRowIndexToModel(row);
		    itemId = (int) Math.floor((double) itemId / 2);
                    long routeId = (long) netPlanTables[3].getModel().getValueAt(2 * itemId, 0);

                    switch (e.getClickCount())
		    {
			case 1:
			    int[] pri_seqLinks = netState.getRoutePrimaryPathSequenceOfLinks(routeId);
			    int[] sec_seqLinks = netState.convertBackupRoute2SequenceOfLinks(netState.getRouteCurrentSequenceOfLinksAndSegments(routeId));
			    showRoutes(pri_seqLinks, sec_seqLinks);
			    break;
			case 2:

			    int col = netPlanTables[3].convertColumnIndexToModel(netPlanTables[3].columnAtPoint(e.getPoint()));
			    if (col == -1 || col >= netPlanTables[3].getColumnCount()) return;

			    int demandId = netState.getRouteDemand(routeId);
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

	MouseAdapter segmentsTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[4].rowAtPoint(e.getPoint());

                ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
                
		if (!SwingUtilities.isRightMouseButton(e) && netState.hasProtectionSegments() && row != -1)
		{
		    row = netPlanTables[4].convertRowIndexToModel(row);
                    long segmentId = (long) netPlanTables[4].getModel().getValueAt(row, 0);
                    int[] seqLinks = netState.getProtectionSegmentSequenceOfLinks(segmentId);
		    showRoute(seqLinks);
		}
	    }
	};

	netPlanTables[4].addMouseListener(segmentsTableAdapter);
	decorators[4].getFixedTable().addMouseListener(segmentsTableAdapter);

	MouseAdapter srgsTableAdapter = new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int row = netPlanTables[5].rowAtPoint(e.getPoint());

                NetPlan netPlan = simKernel.getNetPlan();
		if (!SwingUtilities.isRightMouseButton(e) && netPlan.hasProtectionSegments() && row != -1)
		{
		    int srgId = netPlanTables[5].convertRowIndexToModel(row);

		    showSRG(srgId);
		}
	    }
	};

	netPlanTables[5].addMouseListener(srgsTableAdapter);
	decorators[5].getFixedTable().addMouseListener(srgsTableAdapter);

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

		for (int tableId = 0; tableId < netPlanTables.length - 3; tableId++)
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
    public void addManuallyEvent()
    {
        final NetPlan netPlan = simKernel.getNetPlan();
        
        int numSRGs = netPlan.getNumberOfSRGs();
        if (numSRGs == 0)
        {
            ErrorHandling.showErrorDialog("There are no defined SRGs", "Error including new failure/reparation event");
            return;
        }
        
        final JComboBox cmb_srgSelector = new WiderJComboBox();
        
        for(int srgId = 0; srgId < numSRGs; srgId++)
        {
            int[] nodeIds = netPlan.getSRGNodes(srgId);
            int[] linkIds = netPlan.getSRGLinks(srgId);
            
            String srgLabel = String.format("SRG %d (nodes: %s, links: %s)", srgId, nodeIds.length == 0 ? "none" : IntUtils.join(nodeIds, ", "), linkIds.length == 0 ? "none" : IntUtils.join(linkIds, ", "));
            cmb_srgSelector.addItem(StringLabeller.of(srgId, srgLabel));
        }
        
        cmb_srgSelector.setSelectedIndex(0);
        
        JTextField txt_timeToFail = new JTextField();
        JTextField txt_timeToRepair = new JTextField();

        JPanel pane = new JPanel(new GridLayout(0, 2));
        pane.add(new JLabel("SRG: "));
        pane.add(cmb_srgSelector);
        pane.add(new JLabel("Time to fail (in hours, from now): "));
        pane.add(txt_timeToFail);
        pane.add(new JLabel("Time to repair (in hours): "));
        pane.add(txt_timeToRepair);

        while (true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Configure new failure/reparation event", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            try
            {
                int srgId = (Integer) ((StringLabeller) cmb_srgSelector.getSelectedItem()).getObject();
                double timeToFail = Double.parseDouble(txt_timeToFail.getText());
                double timeToRepair = Double.parseDouble(txt_timeToRepair.getText());
                
                if (timeToFail < 0) throw new RuntimeException("Time to fail must be greater or equal than zero");
                if (timeToRepair <= 0) throw new RuntimeException("Time to repair must be greater than zero");
                
                double simTime = simKernel.getSimCore().getFutureEventList().getCurrentSimulationTime();

                ResilienceEvent failureEvent = new ResilienceEvent(simTime + timeToFail, srgId, ResilienceEvent.EventType.SRG_FAILURE);
                ResilienceEvent reparationEvent = new ResilienceEvent(simTime + timeToFail + timeToRepair, srgId, ResilienceEvent.EventType.SRG_REPARATION);
                
                List<ResilienceEvent> aux1 = new LinkedList<ResilienceEvent>();
                aux1.add(failureEvent);
                aux1.add(reparationEvent);
                
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
    
    private class LinkTableCellRenderer extends CellRenderers.CurrentAndPlannedStateCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public LinkTableCellRenderer() { super(); }

        public LinkTableCellRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public void setCurrentState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
	{
            ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
            
            int[] linksDown = netState.getLinksDown();
            if (IntUtils.contains(linksDown, itemId))
                c.setBackground(Color.RED);
	}
    }

    private class SRGTableCellRenderer extends CellRenderers.NumberCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public SRGTableCellRenderer() { super(); }

        public SRGTableCellRenderer(boolean markNonEditable) { super(markNonEditable); }

        @Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
	    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected && row != -1)
            {
                int srgId = table.convertRowIndexToModel(row);
                ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
            
                int[] srgsDown = netState.getSRGsDown();
                if (IntUtils.contains(srgsDown, srgId))
                    c.setBackground(Color.RED);
            }

	    return c;
	}
    }

    private class NodeTableCellRenderer extends CellRenderers.CurrentAndPlannedStateCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public NodeTableCellRenderer() { super(); }

        public NodeTableCellRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public void setCurrentState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
	{
            ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
            
            int[] nodesDown = netState.getNodesDown();
            if (IntUtils.contains(nodesDown, itemId))
                c.setBackground(Color.RED);
	}
    }

    private class RouteTableCellRenderer extends CellRenderers.CurrentAndPlannedStateCellRenderer
    {
        private static final long serialVersionUID = 1L;
        
        public RouteTableCellRenderer() { super(); }

        public RouteTableCellRenderer(boolean markNonEditable) { super(markNonEditable); }

	@Override
	public void setCurrentState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
	{
            double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

            ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
            
            long routeId = (long) table.getModel().getValueAt(2 * itemId, 0);
            double plannedCarriedTraffic = netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);
            double currentCarriedTraffic = netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);

            if (DoubleUtils.isEqualWithinRelativeTolerance(plannedCarriedTraffic, currentCarriedTraffic, PRECISIONFACTOR))
            {
                int[] plannedSequenceOfLinks = netState.getRoutePrimaryPathSequenceOfLinks(routeId);
                int[] currentSequenceOfLinksAndSegments = netState.convertBackupRoute2SequenceOfLinks(netState.getRouteCurrentSequenceOfLinksAndSegments(routeId));

                if (!Arrays.equals(plannedSequenceOfLinks, currentSequenceOfLinksAndSegments))
                {
                    c.setBackground(Color.YELLOW);
                    c.setForeground(Color.BLACK);
                }
            }
            else if (DoubleUtils.isEqualWithinAbsoluteTolerance(currentCarriedTraffic, 0, PRECISIONFACTOR))
            {
                c.setBackground(Color.RED);
            }
            else
            {
                c.setBackground(Color.ORANGE);
            }
	}
    }

    private class DemandTableCellRenderer extends CellRenderers.CurrentAndPlannedStateCellRenderer
    {
        private static final long serialVersionUID = 1L;

        public DemandTableCellRenderer() { super(); }

        public DemandTableCellRenderer(boolean markNonEditable) { super(markNonEditable); }

        @Override
        public void setCurrentState(Component c, JTable table, int itemId, int rowIndexModel, int columnIndexModel, boolean isSelected)
        {
            double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

            NetPlan netPlan = simKernel.getNetPlan();
            ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
            
            int demandId = itemId;
            double h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);
            double r_d = 0;
            
            long[] routeIds = netState.getDemandRoutes(demandId);
            for(long routeId : routeIds)
                r_d += netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
            
            if (DoubleUtils.isEqualWithinAbsoluteTolerance(r_d, 0, PRECISIONFACTOR)) c.setBackground(Color.RED);
            else if (r_d < h_d - PRECISIONFACTOR) c.setBackground(Color.ORANGE);
        }
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
        ResilienceNetState netState = (ResilienceNetState) simKernel.getNetState();
        
        long[] activeRouteIds = netState.getRouteIds();
        long[] activeSegmentIds = netState.getProtectionSegmentIds();
        
	int N = netPlan.getNumberOfNodes();
	int E = netPlan.getNumberOfLinks();
	int D = netPlan.getNumberOfDemands();
	int R = activeRouteIds.length;
	int S = activeSegmentIds.length;
        int numSRGs = netPlan.getNumberOfSRGs();
	Map<String, String> networkAttributes = netPlan.getNetworkAttributes();

	double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

	Object[][] nodeData = new Object[2 * N][netPlanTablesHeader[0].length];
	Object[][] linkData = new Object[2 * E][netPlanTablesHeader[1].length];
	Object[][] demandData = new Object[2 * D][netPlanTablesHeader[2].length];
	Object[][] routeData = new Object[2 * R][netPlanTablesHeader[3].length];
	Object[][] segmentData = new Object[S][netPlanTablesHeader[4].length];
	Object[][] srgData = new Object[numSRGs][netPlanTablesHeader[5].length];
	Object[][] networkData = new Object[networkAttributes.size()][netPlanTablesHeader[6].length];

	double[] u_e_planned = netPlan.getLinkCapacityNotReservedForProtectionInErlangsVector();
	double[] u_e_current = netPlan.getLinkCapacityInErlangsVector();
	double[] y_e_planned = netPlan.getLinkCarriedTrafficInErlangsVector();
	double[] y_e_current = netState.getLinkCurrentCarriedTrafficInErlangsVector();
	double[] rho_e_planned = new double[E];
	double[] rho_e_current = new double[E];

	for (int linkId = 0; linkId < E; linkId++)
	{
	    rho_e_planned[linkId] = u_e_planned[linkId] == 0 ? 0 : y_e_planned[linkId] / u_e_planned[linkId];
	    rho_e_current[linkId] = u_e_current[linkId] == 0 ? 0 : y_e_current[linkId] / u_e_current[linkId];
	}

	if (N > 0)
	{
            double[][] plannedTrafficPerNode = netState.getNodePrimaryTrafficInErlangsVector();
            double[][] currentTrafficPerNode = netState.getNodeCurrentTrafficInErlangsVector();

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

		nodeData[2 * nodeId][4] = currentTrafficPerNode[nodeId][0];
		nodeData[2 * nodeId + 1][4] = plannedTrafficPerNode[nodeId][0];

		nodeData[2 * nodeId][5] = currentTrafficPerNode[nodeId][1];
		nodeData[2 * nodeId + 1][5] = plannedTrafficPerNode[nodeId][1];

		nodeData[2 * nodeId][6] = currentTrafficPerNode[nodeId][2];
		nodeData[2 * nodeId + 1][6] = plannedTrafficPerNode[nodeId][2];

		nodeData[2 * nodeId][7] = DoubleUtils.sum(currentTrafficPerNode[nodeId]);;
		nodeData[2 * nodeId + 1][7] = DoubleUtils.sum(plannedTrafficPerNode[nodeId]);

		nodeData[2 * nodeId][8] = StringUtils.mapToString(netPlan.getNodeSpecificAttributes(nodeId), "=", ", ");
		nodeData[2 * nodeId + 1][8] = null;
	    }

	    netPlanTables[0].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[0].getModel()).setDataVector(nodeData, netPlanTablesHeader[0]);

	    for (int columnId = 0; columnId < decorators[0].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[0].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new NodeTableCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[0].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[0].getColumnModel().getColumn(columnId).setCellRenderer(new NodeTableCellRenderer(true));
	}

	if (E > 0)
	{
	    double maxRho_planned = DoubleFactory1D.dense.make(rho_e_planned).getMaxLocation()[0];
	    double maxRho_current = DoubleFactory1D.dense.make(rho_e_current).getMaxLocation()[0];

	    for (int linkId = 0; linkId < E; linkId++)
	    {
		int traversingRoutes_planned = netState.getLinkTraversingPrimaryRoutes(linkId).length;
		int traversingRoutes_current = netState.getLinkTraversingCurrentRoutes(linkId).length;

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
		decorators[1].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new LinkTableCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[1].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[1].getColumnModel().getColumn(columnId).setCellRenderer(new LinkTableCellRenderer(true));
	}

	if (D > 0)
	{
	    double[] r_d_planned = netPlan.getDemandCarriedTrafficInErlangsVector();
	    double[] r_d_current = netState.getDemandCurrentCarriedTrafficInErlangsVector();

	    for (int demandId = 0; demandId < D; demandId++)
	    {
		long[] traversingRoutes = netState.getDemandRoutes(demandId);

		int numberOfRoutesCarryingTraffic_planned = 0;
		int numberOfRoutesCarryingTraffic_current = 0;
		boolean isBifurcated_planned = false;
		boolean isBifurcated_current = false;
		for (long routeId : traversingRoutes)
		{
                    if (netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId) > 0) numberOfRoutesCarryingTraffic_planned++;
                    if (netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId) > 0) numberOfRoutesCarryingTraffic_current++;

		    if (numberOfRoutesCarryingTraffic_planned > 1) isBifurcated_planned = true;
		    if (numberOfRoutesCarryingTraffic_current > 1) isBifurcated_current = true;

                    if (isBifurcated_current && isBifurcated_planned) break;
		}

		double h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);

		demandData[2 * demandId][0] = demandId;
		demandData[2 * demandId + 1][0] = null;

		demandData[2 * demandId][1] = netPlan.getDemandIngressNode(demandId);
		demandData[2 * demandId + 1][1] = null;

		demandData[2 * demandId][2] = netPlan.getDemandEgressNode(demandId);
		demandData[2 * demandId + 1][2] = null;

		demandData[2 * demandId][3] = h_d;
		demandData[2 * demandId + 1][3] = null;

		demandData[2 * demandId][4] = r_d_current[demandId];
		demandData[2 * demandId + 1][4] = r_d_planned[demandId];

		demandData[2 * demandId][5] = h_d == 0 ? 0 : 100 * (1 - r_d_current[demandId] / h_d);
		demandData[2 * demandId + 1][5] = h_d == 0 ? 0 : 100 * (1 - r_d_planned[demandId] / h_d);

		demandData[2 * demandId][6] = isBifurcated_current ? "Yes" : "No";
		demandData[2 * demandId + 1][6] = isBifurcated_planned ? "Yes" : "No";

		demandData[2 * demandId][7] = StringUtils.mapToString(netPlan.getDemandSpecificAttributes(demandId), "=", ", ");
		demandData[2 * demandId + 1][7] = null;
	    }

	    netPlanTables[2].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[2].getModel()).setDataVector(demandData, netPlanTablesHeader[2]);

	    for (int columnId = 0; columnId < decorators[2].getFixedTable().getColumnModel().getColumnCount(); columnId++)
	    {
		decorators[2].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new DemandTableCellRenderer(true));
	    }

	    for (int columnId = 0; columnId < netPlanTables[2].getColumnModel().getColumnCount(); columnId++)
	    {
		netPlanTables[2].getColumnModel().getColumn(columnId).setCellRenderer(new DemandTableCellRenderer(true));
	    }
	}

	if (R > 0)
	{
            int i = 0;
	    for (long routeId : activeRouteIds)
	    {
		int demandId = netState.getRouteDemand(routeId);
                
                int[] current_seqLinks = netState.convertBackupRoute2SequenceOfLinks(netState.getRouteCurrentSequenceOfLinksAndSegments(routeId));
                int[] current_seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(current_seqLinks);
                
                int[] planned_seqLinks = netState.getRoutePrimaryPathSequenceOfLinks(routeId);
                int[] planned_seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(planned_seqLinks);

		routeData[2 * i][0] = routeId;
		routeData[2 * i + 1][0] = null;

		routeData[2 * i][1] = demandId;
		routeData[2 * i + 1][1] = null;

		routeData[2 * i][2] = netPlan.getDemandIngressNode(demandId);
		routeData[2 * i + 1][2] = null;

		routeData[2 * i][3] = netPlan.getDemandEgressNode(demandId);
		routeData[2 * i + 1][3] = null;

		routeData[2 * i][4] = netState.getRouteCurrentCarriedTrafficVolumeInErlangs(routeId);
		routeData[2 * i + 1][4] = netState.getRoutePrimaryPathCarriedTrafficVolumeInErlangs(routeId);

		routeData[2 * i][5] = IntUtils.join(current_seqLinks, " => ");
		routeData[2 * i + 1][5] = IntUtils.join(planned_seqLinks, " => ");

		routeData[2 * i][6] = IntUtils.join(current_seqNodes, " => ");
		routeData[2 * i + 1][6] = IntUtils.join(planned_seqNodes, " => ");

		routeData[2 * i][7] = DoubleUtils.maxValue(DoubleUtils.select(rho_e_current, current_seqLinks));
		routeData[2 * i + 1][7] = DoubleUtils.maxValue(DoubleUtils.select(rho_e_planned, current_seqLinks));

		routeData[2 * i][8] = LongUtils.join(netState.getRouteBackupSegmentList(routeId), " => ");
		routeData[2 * i + 1][8] = null;

		routeData[2 * i][9] = StringUtils.mapToString(netState.getRouteSpecificAttributes(routeId), "=", ", ");
		routeData[2 * i + 1][9] = null;
                i++;
	    }

	    netPlanTables[3].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[3].getModel()).setDataVector(routeData, netPlanTablesHeader[3]);

	    for (int columnId = 0; columnId < decorators[3].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[3].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new RouteTableCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[3].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[3].getColumnModel().getColumn(columnId).setCellRenderer(new RouteTableCellRenderer(true));
	}

	if (S > 0)
	{
            int i = 0;
	    for (long segmentId : activeSegmentIds)
	    {
		double segmentLength = 0;
		int[] sequenceOfLinks = netState.getProtectionSegmentSequenceOfLinks(segmentId);
		int[] sequenceOfNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(sequenceOfLinks);
		for (int linkId : sequenceOfLinks) segmentLength += netPlan.getLinkLengthInKm(linkId);
                
		int numberOfRoutes = 0;
		for (long routeId : activeRouteIds)
		{
		    long[] backupSegments = netState.getRouteBackupSegmentList(routeId);
		    Arrays.sort(backupSegments);
		    if (Arrays.binarySearch(backupSegments, segmentId) >= 0) numberOfRoutes++;
		}

		segmentData[i][0] = segmentId;
		segmentData[i][1] = sequenceOfNodes[0];
		segmentData[i][2] = sequenceOfNodes[sequenceOfNodes.length - 1];
		segmentData[i][3] = netState.getProtectionSegmentCurrentCarriedTrafficInErlangs(segmentId);
		segmentData[i][4] = netState.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
		segmentData[i][5] = IntUtils.join(sequenceOfLinks, " => ");
		segmentData[i][6] = IntUtils.join(sequenceOfNodes, " => ");
		segmentData[i][7] = segmentLength;
		segmentData[i][8] = numberOfRoutes > 1 ? String.format("Shared (%d routes)", numberOfRoutes) : (numberOfRoutes == 0 ? "Not used" : "Dedicated");
		segmentData[i][9] = StringUtils.mapToString(netState.getProtectionSegmentSpecificAttributes(segmentId), "=", ", ");
                i++;
	    }

	    netPlanTables[4].setEnabled(true);
	    ((DefaultTableModel) netPlanTables[4].getModel()).setDataVector(segmentData, netPlanTablesHeader[4]);
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

            netPlanTables[5].setEnabled(true);
            ((DefaultTableModel) netPlanTables[5].getModel()).setDataVector(srgData, netPlanTablesHeader[5]);

	    for (int columnId = 0; columnId < decorators[5].getFixedTable().getColumnModel().getColumnCount(); columnId++)
		decorators[5].getFixedTable().getColumnModel().getColumn(columnId).setCellRenderer(new SRGTableCellRenderer(true));

	    for (int columnId = 0; columnId < netPlanTables[5].getColumnModel().getColumnCount(); columnId++)
		netPlanTables[5].getColumnModel().getColumn(columnId).setCellRenderer(new SRGTableCellRenderer(true));
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
        
	netPlanTables[6].setEnabled(true);
	((DefaultTableModel) netPlanTables[6].getModel()).setDataVector(networkData, netPlanTablesHeader[6]);

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
        
        ITopologyCanvas g = pan_topology.getCanvas();
        g.resetDownState();

        int[] nodesDown = netState.getNodesDown();
        int[] linksDown = netState.getLinksDown();
        
        for(int nodeId : nodesDown)
            g.setNodeDown(nodeId);
        
        for(int linkId : linksDown)
            g.setLinkDown(linkId);
        
        g.refresh();
    }
}
