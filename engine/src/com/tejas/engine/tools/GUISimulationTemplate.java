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

import com.jom.JOMException;
import com.tejas.engine.tools.GUISimulationTemplate;
import com.net2plan.tools.IGUIModule;

import static com.tejas.engine.internal.sim.SimCore.SimState.NOT_STARTED;
import static com.tejas.engine.tools.IGUIModule.CURRENT_DIR;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.IReport;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.IExternal;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.sim.EndSimulationException;
import com.tejas.engine.internal.sim.IGUISimulationListener;
import com.tejas.engine.internal.sim.SimCore;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimKernel;
import com.tejas.engine.internal.sim.SimCore.SimState;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.CellRenderers;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.ClassLoaderUtils;
import com.tejas.engine.utils.ColumnFitAdapter;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.ParameterValueDescriptionPanel;
import com.tejas.engine.utils.ProportionalResizeJSplitPaneListener;
import com.tejas.engine.utils.ReportBrowser;
import com.tejas.engine.utils.ReportParameterPanel;
import com.tejas.engine.utils.RunnableSelector;
import com.tejas.engine.utils.TabIcon;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.topology.INetworkCallback;
import com.tejas.engine.utils.topology.plugins.ITopologyCanvasPlugin;
import com.tejas.engine.utils.topology.plugins.JUNGCanvas;
import com.tejas.engine.utils.topology.plugins.PanGraphPlugin;
import com.tejas.engine.utils.topology.plugins.TopologyPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import net.miginfocom.swing.MigLayout;

/**
 * Specific template for simulation tools within Net2Plan.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public abstract class GUISimulationTemplate extends IGUIModule implements ActionListener, INetworkCallback, IGUISimulationListener
{
    public enum SimulatorType { ASYNCHRONOUS, SYNCHRONOUS };
    private static final long serialVersionUID = 1L;

    private boolean isRunning;
    private final JButton btn_run, btn_step, btn_pause, btn_stop, btn_newEvent; //, btn_reset;
    private final JButton btn_viewEventList, btn_updateReport;
    private final JPanel simReport;
    private final JCheckBox chk_refresh;
    private final JPanel pan_simulationController;
    private final JTabbedPane reportsPane;
    private final JTabbedPane rightPanel;
    private final JTextArea simInfo;
    private final JToolBar toolbar;
    protected final TopologyPanel pan_topology;
    private final JSplitPane splitPaneConfiguration;

    private Thread simThread;

    private final ParameterValueDescriptionPanel simulationConfigurationPanel;
    private final RunnableSelector eventGeneratorPanel, eventProcessorPanel, reportSelector;
    
    /**
     * Reference to the simulation kernel.
     * 
     * @since 0.2.2
     */
    protected final SimKernel simKernel;

    /**
     * Adds manually a new event to the simulation.
     * 
     * @since 0.2.2
     */
    public abstract void addManuallyEvent();
    
    /**
     * Returns a graphical representation of the current network state
     * (and possibly the planned one). Typically, it is a <code>JTabbedPane</code>
     * with a number of tabs (i.e. nodes, links, demands...) and the title of the
     * representation.
     * 
     * @return Graphical representation of the network state
     * @since 0.2.2
     */
    public abstract Pair<JComponent, String> getNetPlanView();
    
    /**
     * Updates the graphical representation of the network state.
     * 
     * @since 0.2.2
     */
    @Override
    public abstract void updateNetPlanView();
    
    @Override
    public Pair<Integer, NetPlan> getCurrentNetPlan() { return Pair.of(0, simKernel.getCurrentNetPlan()); }
    
    /**
     * Default constructor.
     * 
     * @param title Title of the simulator
     * @param simKernel Reference to the employed simulation kernel (i.e. resilience simulator)
     * @param simulatorType Simulator type
     * @since 0.2.2
     */
    public GUISimulationTemplate(String title, final SimKernel simKernel, SimulatorType simulatorType)
    {
        super(title);
        
        this.simKernel = simKernel;
        simKernel.setGUIListener(this);
        
        List<ITopologyCanvasPlugin> plugins = new LinkedList<ITopologyCanvasPlugin>();
	ITopologyCanvasPlugin panningPlugin = new PanGraphPlugin(this, MouseEvent.BUTTON1_MASK);
	plugins.add(panningPlugin);
        
	pan_topology = new TopologyPanel(this, JUNGCanvas.class, plugins);
	pan_topology.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology: Untitled"));
        pan_topology.setAllowLoadTrafficDemand(false);

	pan_simulationController = new JPanel(new MigLayout("fill, insets 0 0 0 0"));
	pan_simulationController.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Simulation controller"));
	rightPanel = new JTabbedPane();

	JSplitPane splitPaneTopology = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPaneTopology.setTopComponent(pan_topology);
	splitPaneTopology.setBottomComponent(pan_simulationController);

	splitPaneTopology.setResizeWeight(0.7);
	splitPaneTopology.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
	splitPaneTopology.setBorder(new LineBorder(contentPane.getBackground()));

	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setLeftComponent(splitPaneTopology);
	splitPane.setRightComponent(rightPanel);
	splitPane.setResizeWeight(0.5);
	splitPane.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());

	splitPane.setBorder(BorderFactory.createEmptyBorder());
	contentPane.add(splitPane, "grow");

	File ALGORITHMS_DIRECTORY = new File(CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
	ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : CURRENT_DIR;

	File REPORTS_DIRECTORY = new File(CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
	REPORTS_DIRECTORY = REPORTS_DIRECTORY.isDirectory() ? REPORTS_DIRECTORY : CURRENT_DIR;
	reportSelector = new RunnableSelector("Report", null, IReport.class, REPORTS_DIRECTORY, new ReportParameterPanel());

        eventGeneratorPanel = new RunnableSelector(simKernel.getEventGeneratorLabel(), "File", simKernel.getEventGeneratorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());
        eventProcessorPanel = new RunnableSelector(simKernel.getEventProcessorLabel(), "File", simKernel.getEventProcessorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());

        simulationConfigurationPanel = new ParameterValueDescriptionPanel();
        simulationConfigurationPanel.setParameters(simKernel.getSimulationParameters());

	JTabbedPane config = new JTabbedPane();
	config.addTab(simKernel.getEventGeneratorLabel(), eventGeneratorPanel);
	config.addTab(simKernel.getEventProcessorLabel(), eventProcessorPanel);

	JPanel topPane = new JPanel(new MigLayout("insets 0 0 0 0", "[][grow][]", "[][grow]"));
	topPane.add(new JLabel("Simulation parameters"), "spanx 3, wrap");
	topPane.add(simulationConfigurationPanel, "spanx 3, grow, wrap");

	splitPaneConfiguration = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPaneConfiguration.setTopComponent(topPane);
	splitPaneConfiguration.setBottomComponent(config);

	splitPaneConfiguration.setResizeWeight(0.5);
	splitPaneConfiguration.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
	splitPaneConfiguration.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Simulation execution"));

	JPanel pan_execution = new JPanel(new MigLayout("fill, insets 0 0 0 0"));
	pan_execution.add(splitPaneConfiguration, "grow");

        btn_updateReport = new JButton("Update"); btn_updateReport.setToolTipText("Update the simulation report");
        btn_updateReport.addActionListener(this);
        
	simReport = new JPanel();
	simReport.setLayout(new BorderLayout());
	simReport.add(btn_updateReport, BorderLayout.NORTH);
        
	Pair<JComponent, String> aux = getNetPlanView();
	rightPanel.add(aux.getSecond(), aux.getFirst());
	rightPanel.add("Execution controller", pan_execution);
	rightPanel.add("Simulation report", simReport);

	final JSplitPane pan_viewReports = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	final JPanel pnl_reportButtons = new JPanel(new MigLayout("insets 0 0 0 0", "[center, grow]", "[]"));

	reportsPane = new JTabbedPane();
	reportsPane.setVisible(false);

	((JComponent) this).registerKeyboardAction(new ActionListener()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		int tab = reportsPane.getSelectedIndex();
		if (tab == -1) return;

		reportsPane.remove(tab);
	    }
	}, KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

	reportsPane.addContainerListener(new ContainerListener()
	{
	    @Override
	    public void componentAdded(ContainerEvent e)
	    {
		reportsPane.setVisible(true);
		pan_viewReports.setDividerLocation(0.5);
	    }

	    @Override
	    public void componentRemoved(ContainerEvent e)
	    {
		if (reportsPane.getTabCount() == 0)
                    reportsPane.setVisible(false);
	    }
	});

	JButton btn_show = new JButton("Show"); btn_show.setToolTipText("Show the report");
	btn_show.addActionListener(new ActionListener()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
		try
		{
		    Triple<File, String, Class> report = reportSelector.getRunnable();
		    Map<String, String> reportParameters = reportSelector.getRunnableParameters();
		    Map<String, String> net2planParameters = Configuration.getOptions();
		    IReport instance = ClassLoaderUtils.getInstance(report.getFirst(), report.getSecond(), IReport.class);
		    Pair<String, ? extends JPanel> aux = Pair.of(instance.getTitle(), new ReportBrowser(instance.executeReport(simKernel.getCurrentNetPlan().copy(), reportParameters, net2planParameters)));
		    ((Closeable) instance.getClass().getClassLoader()).close();

		    reportsPane.addTab(aux.getFirst(), new TabIcon(null, TabIcon.TIMES_SIGN), aux.getSecond());
		    reportsPane.setSelectedIndex(reportsPane.getTabCount() - 1);
		}
		catch(Net2PlanException ex1)
		{
		    ErrorHandling.showErrorDialog(ex1.getMessage(), "Error executing report");
		}
		catch (Exception ex)
		{
		    ErrorHandling.addErrorOrException(ex, GUISimulationTemplate.class);
		    ErrorHandling.showErrorDialog("Error showing report");
		}
	    }
	});

	reportsPane.addMouseListener(new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int tabNumber = reportsPane.getUI().tabForCoordinate(reportsPane, e.getX(), e.getY());

		if (tabNumber >= 0)
		{
		    Rectangle rect = ((TabIcon) reportsPane.getIconAt(tabNumber)).getBounds();
		    if (rect.contains(e.getX(), e.getY()))
		    {
			reportsPane.removeTabAt(tabNumber);
		    }
		}
	    }
	});

	pnl_reportButtons.add(btn_show);
        
        JPanel pane = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html><b>Important</b>: Current network state is used as a reference</html>");
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        pane.add(label, BorderLayout.NORTH);
        pane.add(reportSelector, BorderLayout.CENTER);
        pane.add(pnl_reportButtons, BorderLayout.SOUTH);
	pan_viewReports.setTopComponent(pane);
        
	pan_viewReports.setBottomComponent(reportsPane);
	pan_viewReports.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
	pan_viewReports.setResizeWeight(0.5);
	rightPanel.add("View reports", pan_viewReports);

	simInfo = new JTextArea();
	simInfo.setFont(new JLabel().getFont());
	DefaultCaret caret = (DefaultCaret) simInfo.getCaret();
	caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

	toolbar = new JToolBar();

	btn_run = new JButton("Run"); btn_run.addActionListener(this); btn_run.setToolTipText("Execute the simulation");
	btn_step = new JButton("Step"); btn_step.addActionListener(this); btn_step.setToolTipText("Execute the next scheduled event and pause");
	btn_pause = new JButton("Pause/Continue"); btn_pause.addActionListener(this); btn_pause.setToolTipText("Pause the simulation (if active) or continue (if paused)");
	btn_stop = new JButton("Stop"); btn_stop.addActionListener(this); btn_stop.setToolTipText("Stop the simulation (it cannot be resumed later)");
	btn_newEvent = new JButton("Manually generate new event"); btn_newEvent.addActionListener(this); btn_newEvent.setToolTipText("Add a new event independent from the event generator");
//	btn_reset = new JButton("Reset"); btn_reset.addActionListener(this);
	chk_refresh = new JCheckBox("Refresh");
        chk_refresh.setSelected(true);
        
        isRunning = false;
        
        btn_viewEventList = new JButton("View FEL"); btn_viewEventList.setToolTipText("View future event list (FEL)");
        btn_viewEventList.addActionListener(this);

	toolbar.setFloatable(false);
	toolbar.add(btn_run);
	toolbar.add(btn_step);
	toolbar.add(btn_pause);
	toolbar.add(btn_stop);
	if (simulatorType == SimulatorType.ASYNCHRONOUS)
        {
            toolbar.addSeparator();
            toolbar.add(btn_newEvent);
        }
//	toolbar.add(btn_reset);
	toolbar.add(chk_refresh);
	if (simulatorType == SimulatorType.ASYNCHRONOUS)
        {
            toolbar.add(Box.createHorizontalGlue());
            toolbar.add(btn_viewEventList);
        }

	pan_simulationController.add(toolbar, "dock north");
	pan_simulationController.add(new JScrollPane(simInfo), "grow");

        this.simKernel.reset();
    }

    @Override
    public final void addNode(Point2D pos) { throw new UnsupportedOperationException("Bad"); }

    @Override
    public final void addLink(int originNodeId, int destinationNodeId) { throw new UnsupportedOperationException("Bad"); }

    @Override
    public final void moveNode(int nodeId, Point2D pos) { throw new UnsupportedOperationException("Bad"); }

    @Override
    public final void removeNode(int nodeId) { throw new UnsupportedOperationException("Bad"); }

    @Override
    public final void removeLink(int linkId) { throw new UnsupportedOperationException("Bad"); }

    @Override
    public void resetView() { pan_topology.getCanvas().resetPickedState(); }

    @Override
    public void showNode(int nodeId)
    {
	pan_topology.getCanvas().showNode(nodeId);
	pan_topology.getCanvas().refresh();
    }

    @Override
    public void showLink(int linkId)
    {
	pan_topology.getCanvas().showLink(linkId);
	pan_topology.getCanvas().refresh();
    }

    /**
     * Shows the end nodes for the given demand.
     * 
     * @param demandId Demand identifier
     * @since 0.2.2
     */
    @Override
    public void showDemand(int demandId)
    {
	NetPlan _netPlan = simKernel.getNetPlan();

	pan_topology.getCanvas().showNodes(new int[]
		{
		    _netPlan.getDemandIngressNode(demandId), _netPlan.getDemandEgressNode(demandId)
		});
	pan_topology.getCanvas().refresh();
    }

    /**
     * Shows a route given a sequence of links.
     * 
     * @param linkIds Set of links to show
     * @since 0.2.2
     */
    protected void showRoute(int[] linkIds)
    {
        pan_topology.getCanvas().showRoute(linkIds);
        pan_topology.getCanvas().refresh();
    }

    /**
     * Shows the original route and the current one. The first one will show a
     * heavy line, while the second one will show a heavy dashed line.
     * 
     * @param primaryLinkIds Link identifiers for the first set of links
     * @param backupLinkIds Link identifiers for the second set of links
     * @since 0.2.2
     */
    protected void showRoutes(int[] primaryLinkIds, int[] backupLinkIds)
    {
        pan_topology.getCanvas().showRoutes(primaryLinkIds, backupLinkIds);
        pan_topology.getCanvas().refresh();
    }

    /**
     * Shows the sequence of links for a planned route.
     * 
     * @param routeId Route identifier
     * @since 0.2.2
     */
    @Override
    public void showRoute(int routeId)
    {
	NetPlan _netPlan = simKernel.getNetPlan();
	pan_topology.getCanvas().showRoute(_netPlan.getRouteSequenceOfLinks(routeId));
	pan_topology.getCanvas().refresh();
    }
    
    @Override
    public void showSRG(int srgId)
    {
	NetPlan _netPlan = simKernel.getNetPlan();
        int[] nodeIds = _netPlan.getSRGNodes(srgId);
        int[] linkIds = _netPlan.getSRGLinks(srgId);
	pan_topology.getCanvas().showNodesAndLinks(nodeIds, linkIds);
	pan_topology.getCanvas().refresh();
    }

    /**
     *
     * @param segmentId
     */
    @Override
    public void showSegment(int segmentId)
    {
	NetPlan _netPlan = simKernel.getNetPlan();
	pan_topology.getCanvas().showRoute(_netPlan.getProtectionSegmentSequenceOfLinks(segmentId));
	pan_topology.getCanvas().refresh();
    }
    
    @Override
    public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason)
    {
        isRunning = false;
        
        simulationConfigurationPanel.setEnabled(false);
        eventGeneratorPanel.setEnabled(false);
        eventProcessorPanel.setEnabled(false);
        
        switch(simulationState)
        {
            case NOT_STARTED:
                btn_run.setEnabled(true);
                btn_step.setEnabled(true);
                btn_pause.setEnabled(false);
//                btn_reset.setEnabled(false);
                btn_stop.setEnabled(false);
                btn_newEvent.setEnabled(true);
                
                simulationConfigurationPanel.setEnabled(true);
                eventGeneratorPanel.setEnabled(true);
                eventProcessorPanel.setEnabled(true);
                
                try { if (simThread != null) simThread.stop(); }
                catch(Throwable e) { }
                
                break;
                
            case RUNNING:
            case STEP:
                btn_run.setEnabled(false);
                btn_step.setEnabled(false);
                btn_pause.setEnabled(true);
                isRunning = true;
//                btn_reset.setEnabled(false);
                btn_stop.setEnabled(true);
                btn_newEvent.setEnabled(false);
                break;
                
            case PAUSED:
                btn_run.setEnabled(false);
                btn_step.setEnabled(true);
                btn_pause.setEnabled(true);
//                btn_reset.setEnabled(true);
                btn_stop.setEnabled(true);
                btn_newEvent.setEnabled(true);
                break;
                
            case STOPPED:
                btn_run.setEnabled(false);
                btn_step.setEnabled(false);
                btn_pause.setEnabled(false);
//                btn_reset.setEnabled(true);
                btn_stop.setEnabled(false);
                btn_newEvent.setEnabled(false);
                
                break;

            default:
                throw new RuntimeException("Bad - Unknown simulation state");
        }
        
        if (simulationState == SimState.NOT_STARTED || simulationState == SimState.PAUSED || simulationState == SimState.STOPPED)
        {
	    updateSimulationInfo();
            updateNetPlanView();
            resetView();
        }
        
        if (reason == null) return;
        
        if (reason instanceof EndSimulationException)
        {
            ErrorHandling.showInformationDialog("Simulation finished", "Information");
//            simInfo.setText(simInfo.getText() + String.format("%n%n") + "Simulation ended");
        }
        else if (reason instanceof Net2PlanException || reason instanceof JOMException)
        {
            ErrorHandling.showErrorDialog(reason.getMessage(), "Error executing simulation");
        }
        else
        {
            ErrorHandling.addErrorOrException(reason, SimKernel.class);
            ErrorHandling.showErrorDialog("Fatal error");
        }
    }
    
    @Override
    public void refresh(boolean forceRefresh)
    {
        if (chk_refresh.isSelected() || forceRefresh)
            updateSimulationInfo();
    }
    
    private void runSimulation(final boolean stepByStep)
    {
        try
        {
            if (simKernel.getSimCore().getSimulationState() == NOT_STARTED)
            {
                simKernel.checkLoadedNetPlan(simKernel.getNetPlan());
                Map<String, String> simulationParameters = simulationConfigurationPanel.getParameters();
                Map<String, String> net2planParameters = Configuration.getOptions();
                
                Triple<File, String, Class> aux;
                
                aux = eventGeneratorPanel.getRunnable();
                Map<String, String> eventGeneratorParameters = eventGeneratorPanel.getRunnableParameters();
                IExternal eventGenerator = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventGeneratorClass());
                
                aux = eventProcessorPanel.getRunnable();
                IExternal eventProcessor = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventProcessorClass());
                Map<String, String> eventProcessorParameters = eventProcessorPanel.getRunnableParameters();
                
                simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
                List<? extends SimEvent> events = simKernel.initialize();
                events = simKernel.checkEvents(events);
                simKernel.getSimCore().getFutureEventList().addEvents(events);
                simKernel.getSimCore().setSimulationState(stepByStep ? SimCore.SimState.STEP : SimCore.SimState.RUNNING);

                simInfo.setText(null);
                simInfo.setText("Simulation running...");
                simInfo.setCaretPosition(0);
                
                simThread = new Thread(simKernel.getSimCore());
                simThread.start();
            }
            else
            {
//                try
//                {
//                    SwingUtilities.invokeLater(new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
                            simKernel.getSimCore().setSimulationState(stepByStep ? SimCore.SimState.STEP : SimCore.SimState.RUNNING);
//                        }
//                    });
//                }
//                catch(Throwable e)
//                {
//                    throw new RuntimeException(e);
//                }
            }
        }
        catch(Net2PlanException e)
        {
            simKernel.reset();
            ErrorHandling.showErrorDialog(e.getMessage(), "Unable to execute simulation");
        }
        catch(Throwable e)
        {
            simKernel.reset();
            ErrorHandling.addErrorOrException(e, GUISimulationTemplate.class);
            ErrorHandling.showErrorDialog("Error execution simulation");
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e)
    {
        try
        {
            Object src = e.getSource();

            if (src == btn_run)
            {
                runSimulation(false);
            }
            else if (src == btn_step)
                runSimulation(true);
            else if (src == btn_pause)
                simKernel.getSimCore().setSimulationState(simKernel.getSimCore().getSimulationState() == SimState.PAUSED ? SimState.RUNNING : SimState.PAUSED);
            else if (src == btn_stop)
                simKernel.getSimCore().setSimulationState(SimState.STOPPED);
//            else if (src == btn_reset)
//                reset();
            else if (src == btn_newEvent)
            {
                if (simKernel.getSimCore().getSimulationState() == SimCore.SimState.NOT_STARTED)
                {
                    ErrorHandling.showErrorDialog("Simulation is not started yet", "Error adding a new event");
                    return;
                }

                addManuallyEvent();
            }
            else if (src == btn_viewEventList)
                viewFutureEventList();
            else if (src == btn_updateReport)
                updateSimReport();
            else
                throw new Net2PlanException("Bad");
        }
        catch(Net2PlanException ex)
        {
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error executing simulation");
        }
        catch(Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, GUISimulationTemplate.class);
            ErrorHandling.showErrorDialog("An error happened");
        }
    }
    
    @Override
    public List<NetPlan> getDesign()
    {
        List<NetPlan> aux = new LinkedList<NetPlan>();
        aux.add(simKernel.getCurrentNetPlan());
        
        return aux;
    }
    
    private void updateSimReport()
    {
        try
        {
            if (simKernel.getSimCore().getSimulationState() == SimCore.SimState.NOT_STARTED)
                throw new Net2PlanException("Simulation not started yet");
            
            JPanel pane = new ReportBrowser(simKernel.getSimulationReport());
            try { simReport.remove(((BorderLayout) simReport.getLayout()).getLayoutComponent(BorderLayout.CENTER)); }
            catch(Throwable ex) { }
            simReport.add(pane, BorderLayout.CENTER);
            simReport.revalidate();
        }
        catch(Net2PlanException ex)
        {
            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to show simulation report");
        }
        catch(Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, GUISimulationTemplate.class);
            ErrorHandling.showErrorDialog("Error updating simulation report");
        }
    }
    
    private void updateSimulationInfo()
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            simInfo.setText(null);
            simInfo.setText(simKernel.getSimulationInfo());
            simInfo.setCaretPosition(0);
        }
        else
        {
            try
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        simInfo.setText(null);
                        simInfo.setText(simKernel.getSimulationInfo());
                        simInfo.setCaretPosition(0);
                    }
                });
            }
            catch(Throwable e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public void loadDesign(NetPlan netPlan)
    {
        simKernel.checkLoadedNetPlan(netPlan);
        simKernel.setNetPlan(netPlan);

        pan_topology.getCanvas().updateTopology(netPlan);
        pan_topology.getCanvas().zoomAll();
        
        String networkName = netPlan.getNetworkName();
        if (networkName.isEmpty()) networkName = "Untitled";
	pan_topology.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology: " + networkName));

        updateNetPlanView();
        resetView();
        
        rightPanel.setSelectedIndex(0);
    }
    
    @Override
    public void loadTrafficDemands(NetPlan netPlan)
    {
        throw new Net2PlanException("Traffic demands cannot be loaded in this simulator. They should be included in the design");
    }
    
    /**
     * Shows the current list of future events.
     * 
     * @since 0.2.2
     */
    public void viewFutureEventList()
    {
	final JDialog dialog = new JDialog();
        dialog.setTitle("Future event list");
        ((JComponent) dialog.getContentPane()).registerKeyboardAction(new CloseDialogOnEscape(dialog), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
	dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0 0 0 0"));
        
        final String[] tableHeader = new String[] { "Id", "Event time", "Event priority", "Description" };
        Object[][] data = new Object[1][tableHeader.length];
        
        DefaultTableModel model = new ClassAwareTableModel();
        model.setDataVector(new Object[1][2], tableHeader);
        
        JTable table = new AdvancedJTable(model);
        dialog.add(new JScrollPane(table), "grow");
        
//        Collection<SimEvent> futureEventList = simKernel.getSimCore().getFutureEventList().getPendingEvents();
        
//        if (!futureEventList.isEmpty())
//        {
//            int numEvents = futureEventList.size();
//            data = new Object[numEvents][tableHeader.length];
//            
//            int eventId = 0;
//            Iterator<SimEvent> it = futureEventList.iterator();
//            while(it.hasNext())
//            {
//                SimEvent aux = it.next();
//
//                data[eventId][0] = eventId;
//                data[eventId][1] = SimEvent.secondsToYearsDaysHoursMinutesSeconds(aux.getEventTime());
//                data[eventId][2] = aux.getEventPriority();
//                data[eventId][3] = aux.toString();
//                
//                eventId++;
//            }
//        }

        PriorityQueue<SimEvent> futureEventList = simKernel.getSimCore().getFutureEventList().getPendingEvents();
        if (!futureEventList.isEmpty())
        {
            int numEvents = futureEventList.size();
            SimEvent[] futureEventList_array = futureEventList.toArray(new SimEvent[numEvents]);
            Arrays.sort(futureEventList_array, futureEventList.comparator());
            data = new Object[numEvents][tableHeader.length];
            
            for(int eventId = 0; eventId < numEvents; eventId++)
            {
                data[eventId][0] = eventId;
                data[eventId][1] = SimEvent.secondsToYearsDaysHoursMinutesSeconds(futureEventList_array[eventId].getEventTime());
                data[eventId][2] = futureEventList_array[eventId].getEventPriority();
                data[eventId][3] = futureEventList_array[eventId].toString();
            }
        }
        
        model.setDataVector(data, tableHeader);
        table.getTableHeader().addMouseListener(new ColumnFitAdapter());
        table.setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());

        dialog.setVisible(true);
    }

    @Override
    public void reset()
    {
        if (simKernel.getSimCore().getSimulationState() != SimState.NOT_STARTED && simKernel.getSimCore().getSimulationState() != SimState.STOPPED)
        {
            ErrorHandling.showErrorDialog("Simulation is running. First, stop the simulation", "Unable to reset");
            return;
        }
        
        boolean reset = askForReset();

        if (reset)
        {
            simKernel.reset();
            updateNetPlanView();
            resetView();
        }
    }

    private static class CloseDialogOnEscape implements ActionListener {

        private final JDialog dialog;

        public CloseDialogOnEscape(JDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) { dialog.setVisible(false); }
    }
}
