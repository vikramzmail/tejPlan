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

import com.tejas.engine.tools.IGUIModule;
import com.tejas.engine.utils.CellRenderers;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.ProportionalResizeJSplitPaneListener;
import com.tejas.engine.utils.TabIcon;
import com.tejas.engine.utils.WiderJComboBox;
import com.tejas.engine.utils.WrapLayout;

import static com.tejas.engine.tools.IGUIModule.CURRENT_DIR;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.libraries.TrafficMatrixGenerationModels;
import com.tejas.engine.utils.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Assists users in the process of generating and normalizing traffic matrices,
 * for instance following random models found in the literature.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class GUITrafficDesign extends IGUIModule
{
    private static final long serialVersionUID = 1L;

    private List<JTable> trafficMatrices;
    private JTabbedPane tabbedPane;

    private DefaultTableModel nodeInfoTableModel, levelMatrixTableModel;
    private JTextField txt_randomFactor, txt_distanceOffset, txt_distancePower, txt_populationOffset, txt_populationPower;

    private JFileChooser fc_netPlan, fc_trafficMatrix;
    
    private final JButton applyPopDist, applyPopDist_applyBatch, btn_normalizationPattern, btn_normalizationPatternAll, btn_trafficModelPattern_apply, btn_trafficModelPattern_applyBatch, btn_resetAll, btn_resetThis, btn_symmetrizeAll, btn_symmetrizeThis;
    
    private final JComboBox cmb_normalizationPattern, cmb_trafficModelPattern;
    
    private final JCheckBox chk_populationDistanceModelSymmetric;
    
    private final JButton btn_incrementalPattern, btn_incrementalPatternAll;
    private final JComboBox cmb_incrementalPattern;
    
    private int maxLevel;

    private void checkNetPlanFileChooser()
    {
        if (fc_netPlan == null)
        {
            File netPlan_DIRECTORY = new File(CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies");
            netPlan_DIRECTORY = netPlan_DIRECTORY.isDirectory() ? netPlan_DIRECTORY : CURRENT_DIR;
            fc_netPlan = new FileChooserConfirmOverwrite(netPlan_DIRECTORY);
            fc_netPlan.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc_netPlan.setFileFilter(new FileNameExtensionFilter("n2p files", "n2p"));
            fc_netPlan.setAcceptAllFileFilterUsed(false);
        }        
    }
    
    private void checkTrafficMatrixFileChooser()
    {
        if (fc_trafficMatrix == null)
        {
            File demands_DIRECTORY = new File(CURRENT_DIR + "\\workspace\\data\\trafficMatrices");
            demands_DIRECTORY = demands_DIRECTORY.isDirectory() ? demands_DIRECTORY : CURRENT_DIR;
            fc_trafficMatrix = new FileChooserConfirmOverwrite(demands_DIRECTORY);
            fc_trafficMatrix.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc_trafficMatrix.setFileFilter(new FileNameExtensionFilter("n2p files", "n2p"));
            fc_trafficMatrix.setAcceptAllFileFilterUsed(false);
        }
    }

    /**
     *
     */
    public GUITrafficDesign()
    {
	super("TRAFFIC MATRIX DESIGN");

	trafficMatrices = new ArrayList<JTable>();

	tabbedPane = new JTabbedPane();
        
	tabbedPane.addContainerListener(new ContainerListener()
	{
	    @Override
	    public void componentRemoved(ContainerEvent e)
	    {
		int numTabs = tabbedPane.getTabCount() - 1;
		for (int i = 0; i < numTabs; i++)
                {
		    tabbedPane.setTitleAt(i, "TM " + Integer.toString(i));
                    tabbedPane.setIconAt(i, closeTabIcon);
                }
                
                if (numTabs == 1) tabbedPane.setIconAt(0, null);
	    }

            @Override
            public void componentAdded(ContainerEvent e)
            {
		int numTabs = tabbedPane.getTabCount() - 1;
                
                if (numTabs > 1)
                    for (int i = 0; i < numTabs; i++)
                        tabbedPane.setIconAt(i, closeTabIcon);
            }
	});
        
	tabbedPane.addMouseListener(new MouseAdapter()
	{

	    @Override
	    public void mouseClicked(MouseEvent e)
	    {
		int tabNumber = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());

		if (tabNumber >= 0)
		{
		    if (tabNumber == tabbedPane.getTabCount() - 1)
		    {
			addTrafficMatrix();
			tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
		    }
		    else
		    {
			Rectangle rect = ((TabIcon) tabbedPane.getIconAt(tabNumber)).getBounds();
			if (rect.contains(e.getX(), e.getY()) && tabbedPane.getTabCount() > 2)
			{
			    trafficMatrices.remove(tabNumber);
			    tabbedPane.removeTabAt(tabNumber);
			}
			if (tabNumber == tabbedPane.getTabCount() - 1)
			{
			    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
			}
		    }
		}
	    }
	});
        
	tabbedPane.addTab("", new TabIcon(null, TabIcon.PLUS_SIGN), new JPanel());
	addTrafficMatrix();
	tabbedPane.setSelectedIndex(0);

	JPanel leftPane = new JPanel();
	JPanel rightPane = new JPanel();

	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setLeftComponent(leftPane);
	splitPane.setRightComponent(rightPane);
	splitPane.setResizeWeight(0.5);
	splitPane.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
	contentPane.add(splitPane, "grow");

	// Button bar
	JButton btn_resizeThis = new JButton("Resize this")
	{
            private static final long serialVersionUID = 1L;

	    {
		addActionListener(new ActionListener()
		{

		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
			int matrixId = getSelectedTrafficMatrix();

			int N;

			while (true)
			{
			    String str = JOptionPane.showInputDialog(null, "Number of nodes", "Resize traffic matrix/matrices", JOptionPane.QUESTION_MESSAGE);
			    if (str == null) return;

                            try
			    {
				N = Integer.parseInt(str);
				if (N < 2) throw new IllegalArgumentException("Number of nodes must be greater than 1");
				break;
			    }
			    catch (NumberFormatException ex)
			    {
				ErrorHandling.showErrorDialog("Non-valid number of nodes. Please, introduce an integer number greater than 1", "Error resizing matrix/matrices");
			    }
			    catch (Exception ex)
			    {
				ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
				ErrorHandling.showErrorDialog("Error resizing matrix");
			    }
			}

                        double[][] trafficMatrix = getTrafficMatrix(matrixId);
                        double[][] newTrafficMatrix = resizeTrafficMatrix(trafficMatrix, N);
                        
                        setTrafficMatrix(newTrafficMatrix, matrixId);
		    }
		});
	    }
	};

	JButton btn_resizeAll = new JButton("Resize all")
	{
            private static final long serialVersionUID = 1L;

	    {
		addActionListener(new ActionListener()
		{

		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
			int N;

			while (true)
			{
			    String str = JOptionPane.showInputDialog(null, "Number of nodes", "Resize traffic matrix/matrices", JOptionPane.QUESTION_MESSAGE);
			    if (str == null) return;

			    try
			    {
				N = Integer.parseInt(str);
				if (N < 2)
				{
				    throw new IllegalArgumentException("Number of nodes must be greater than 1");
				}
				break;
			    }
			    catch (NumberFormatException ex)
			    {
				ErrorHandling.showErrorDialog("Non-valid number of nodes. Please, introduce an integer number greater than 1", "Error resizing matrix/matrices");
			    }
			    catch (Exception ex)
			    {
				ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
				ErrorHandling.showErrorDialog("Error resizing matrices");
			    }
			}

                        int numMatrices = getNumberOfTrafficMatrices();
                        for(int matrixId = 0; matrixId < numMatrices; matrixId++)
                        {
                            double[][] trafficMatrix = getTrafficMatrix(matrixId);
                            double[][] newTrafficMatrix = resizeTrafficMatrix(trafficMatrix, N);

                            setTrafficMatrix(newTrafficMatrix, matrixId);
                        }
		    }
		});
	    }
	};

	JButton btn_load = new JButton("Load")
	{
            private static final long serialVersionUID = 1L;


	    {
		addActionListener(new ActionListener()
		{

		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
                        checkTrafficMatrixFileChooser();
                        
			fc_trafficMatrix.setMultiSelectionEnabled(true);
			int rc = fc_trafficMatrix.showOpenDialog(null);
			if (rc == JFileChooser.CANCEL_OPTION) return;

			File[] files = fc_trafficMatrix.getSelectedFiles();
			try
			{
			    for (int fileId = 0; fileId < files.length; fileId++)
			    {
				NetPlan aux = new NetPlan(files[fileId]);

				int D = aux.getNumberOfDemands();

				if (D == 0) throw new Exception("File " + files[fileId].getCanonicalPath() + " doesn't contain any demand");

				double[][] trafficMatrix = aux.getTrafficMatrix();
                                
				addTrafficMatrix();
				setTrafficMatrix(trafficMatrix, getNumberOfTrafficMatrices() - 1);
			    }
			}
			catch (Exception ex)
			{
			    ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
			    ErrorHandling.showErrorDialog("Error loading matrix/matrices");
			    return;
			}
			finally
			{
			    fc_trafficMatrix.setMultiSelectionEnabled(false);
			}

			tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
		    }
		});
	    }
	};

	JButton btn_saveThis = new JButton("Save this")
	{
            private static final long serialVersionUID = 1L;

            {
		addActionListener(new ActionListener()
		{

		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
                        checkTrafficMatrixFileChooser();
                        
			int rc = fc_trafficMatrix.showSaveDialog(null);
			if (rc != JFileChooser.APPROVE_OPTION) return;

                        File file = fc_trafficMatrix.getSelectedFile();

			try
			{
			    final NetPlan netPlan = new NetPlan();

			    int matrixId = getSelectedTrafficMatrix();

			    double[][] trafficMatrix = getTrafficMatrix(matrixId);
                            netPlan.setTrafficMatrix(trafficMatrix);

			    if (!netPlan.hasDemands())
				throw new Exception("This matrix has no demands (all entries are zero)");

			    netPlan.saveToFile(file);
			}
			catch (Exception ex)
			{
			    ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
			    ErrorHandling.showErrorDialog("Error saving n2p");
			    return;
			}

			ErrorHandling.showInformationDialog("Traffic matrix saved successfully", "Save traffic matrix");
		    }
		});
	    }
	};

	JButton btn_saveAll = new JButton("Save all")
	{
            private static final long serialVersionUID = 1L;

            {
		addActionListener(new ActionListener()
		{

		    @Override
		    public void actionPerformed(ActionEvent e)
		    {
                        checkTrafficMatrixFileChooser();
                        
			int rc = fc_trafficMatrix.showSaveDialog(null);
			if (rc != JFileChooser.APPROVE_OPTION) return;

                        File file = fc_trafficMatrix.getSelectedFile();
			String filePath = file.getPath();
			if (filePath.toLowerCase(Locale.getDefault()).endsWith(".n2p"))
			{
			    filePath = filePath.substring(0, filePath.length() - 4);
			    file = new File(filePath);
			}

			try
			{
			    ListIterator<JTable> it = trafficMatrices.listIterator();
			    while (it.hasNext())
			    {
				final NetPlan netPlan = new NetPlan();

				int matrixId = it.nextIndex();
				double[][] trafficMatrix = getTrafficMatrix(matrixId);
                                netPlan.setTrafficMatrix(trafficMatrix);

                                if (netPlan.hasDemands())
				    netPlan.saveToFile(new File(file.getAbsoluteFile() + "_" + matrixId + ".n2p"));

                                it.next();
			    }
			}
			catch (Throwable ex)
			{
                            ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
                            ErrorHandling.showErrorDialog("Error saving n2p");
			    return;
			}

			ErrorHandling.showInformationDialog("Traffic matrices saved successfully", "Save traffic matrices");
		    }
		});
	    }
	};

        ActionListener symmetrizeMatrices = new SymmetrizeMatrices();

	btn_symmetrizeThis = new JButton("Make symmetric this");
        btn_symmetrizeThis.addActionListener(symmetrizeMatrices);

	btn_symmetrizeAll = new JButton("Make symmetric all");
        btn_symmetrizeAll.addActionListener(symmetrizeMatrices);

        ActionListener resetMatrices = new ResetMatrices();

	btn_resetThis = new JButton("Reset this");
        btn_resetThis.addActionListener(resetMatrices);
        
	btn_resetAll = new JButton("Reset all");
        btn_resetAll.addActionListener(resetMatrices);

        ActionListener arithmeticOperationListener = new ArithmeticOperationListener();

        btn_sumAll = new JButton("Sum all");
        btn_sumAll.addActionListener(arithmeticOperationListener);
        
        btn_multiplyThis = new JButton("Multiply this");
        btn_multiplyThis.addActionListener(arithmeticOperationListener);

        btn_multiplyAll = new JButton("Multiply all");
        btn_multiplyAll.addActionListener(arithmeticOperationListener);
        
        JButton btn_clearAll = new JButton("Clear all");
        btn_clearAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                int N = 4;
                double[][] trafficMatrix = new double[N][N];
                setTrafficMatrix(trafficMatrix);
            }
        });
        
        JPanel pnl_buttonBar = new JPanel(new WrapLayout());
        
	pnl_buttonBar.add(btn_resizeThis); pnl_buttonBar.setToolTipText("Resize the selected traffic matrix");
	pnl_buttonBar.add(btn_resizeAll); pnl_buttonBar.setToolTipText("Resize all traffic matrices");
	pnl_buttonBar.add(btn_load); pnl_buttonBar.setToolTipText("Load a traffic matrix (or matrices) from a .n2p file(s)");
	pnl_buttonBar.add(btn_saveThis); pnl_buttonBar.setToolTipText("Save the selected traffic matrix to a .n2p file");
	pnl_buttonBar.add(btn_saveAll);  pnl_buttonBar.setToolTipText("Save all matrices to .n2p files");
	pnl_buttonBar.add(btn_symmetrizeThis); pnl_buttonBar.setToolTipText("Make the selected traffic matrix symmetric (using upper triangular part)");
	pnl_buttonBar.add(btn_symmetrizeAll);  pnl_buttonBar.setToolTipText("Make all traffic matrices symmetric (using upper triangular part)");
	pnl_buttonBar.add(btn_resetThis); pnl_buttonBar.setToolTipText("Reset the selected traffic matrix to zero-traffic");
	pnl_buttonBar.add(btn_resetAll); pnl_buttonBar.setToolTipText("Reset all traffic matrices to zero-traffic");
	pnl_buttonBar.add(btn_clearAll); pnl_buttonBar.setToolTipText("Remove all traffic matrices");
	pnl_buttonBar.add(btn_sumAll); pnl_buttonBar.setToolTipText("Make a new traffic matrix summing up all the existing ones");
	pnl_buttonBar.add(btn_multiplyThis); pnl_buttonBar.setToolTipText("Multiply the selected traffic matrix by a given factor");
	pnl_buttonBar.add(btn_multiplyAll); pnl_buttonBar.setToolTipText("Multiply all traffic matrices by a given factor");
        
	JPanel pnl_generalTrafficModelsPattern = new JPanel();
	JPanel pnl_popDistBasedTrafficModelPattern = new JPanel();
	JPanel pnl_normalizationPattern = new JPanel();
	JPanel pnl_incrementalPattern = new JPanel();

	pnl_generalTrafficModelsPattern.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Traffic generation: general traffic models"));
	pnl_popDistBasedTrafficModelPattern.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Traffic generation: population-distance traffic model"));
	pnl_normalizationPattern.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Traffic normalization"));
	pnl_incrementalPattern.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Create a set of traffic matrices from a seminal one"));

	// Traffic generation: general traffic models
	cmb_trafficModelPattern = new WiderJComboBox();
	cmb_trafficModelPattern.addItem("Select a traffic pattern");
	cmb_trafficModelPattern.addItem("1. Constant");
	cmb_trafficModelPattern.addItem("2. Uniform (0, 10)");
	cmb_trafficModelPattern.addItem("3. Uniform (0, 100)");
	cmb_trafficModelPattern.addItem("4. 50% Uniform (0, 100) & 50% Uniform(0, 10)");
	cmb_trafficModelPattern.addItem("5. 25% Uniform (0, 100) & 75% Uniform(0, 10)");
	cmb_trafficModelPattern.addItem("6. Gravity model");
        
        ActionListener applyTrafficModels = new ApplyTrafficModels();

	btn_trafficModelPattern_apply = new JButton("Apply this");
	btn_trafficModelPattern_apply.addActionListener(applyTrafficModels);
        btn_trafficModelPattern_apply.setToolTipText("Use the selected model to compute a new traffic matrix");

	btn_trafficModelPattern_applyBatch = new JButton("Apply batch");
	btn_trafficModelPattern_applyBatch.addActionListener(applyTrafficModels);
        btn_trafficModelPattern_applyBatch.setToolTipText("Use the selected model to compute new traffic matrices");

	pnl_generalTrafficModelsPattern.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
	pnl_generalTrafficModelsPattern.add(cmb_trafficModelPattern, "grow"); //, wmin 50");
	pnl_generalTrafficModelsPattern.add(btn_trafficModelPattern_apply);
	pnl_generalTrafficModelsPattern.add(btn_trafficModelPattern_applyBatch);

	// Traffic normalization
	cmb_normalizationPattern = new WiderJComboBox();
	cmb_normalizationPattern.addItem("Select a normalization pattern");
	cmb_normalizationPattern.addItem("1. Total normalization");
	cmb_normalizationPattern.addItem("2. Rowwise normalization"); // Why not, ingress normalization?
	cmb_normalizationPattern.addItem("3. Columnwise normalization");
	cmb_normalizationPattern.addItem("4. Normalize to maximum traffic that can be carried");

        ActionListener normalizeMatrices = new NormalizeMatrices();

	btn_normalizationPattern = new JButton("Apply");
	btn_normalizationPattern.addActionListener(normalizeMatrices);
        btn_normalizationPattern.setToolTipText("Apply the normalization pattern to the selected traffic matrix");

        btn_normalizationPatternAll = new JButton("Apply all");
	btn_normalizationPatternAll.addActionListener(normalizeMatrices);
        btn_normalizationPatternAll.setToolTipText("Apply the normalization pattern to all the traffic matrices");

	pnl_normalizationPattern.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
	pnl_normalizationPattern.add(cmb_normalizationPattern, "grow, wmin 50");
	pnl_normalizationPattern.add(btn_normalizationPattern);
	pnl_normalizationPattern.add(btn_normalizationPatternAll);

	cmb_incrementalPattern = new WiderJComboBox();
	cmb_incrementalPattern.addItem("Select a model");
	cmb_incrementalPattern.addItem("1. New matrices with a compound annual growth rate");
	cmb_incrementalPattern.addItem("2. Uniform random variations");
	cmb_incrementalPattern.addItem("3. Gaussian random variations");

        ActionListener incrementalMatrices = new IncrementalMatrices();

	btn_incrementalPattern = new JButton("Apply");
	btn_incrementalPattern.addActionListener(incrementalMatrices);
        btn_incrementalPattern.setToolTipText("Generate new matrices from the selected one");

        btn_incrementalPatternAll = new JButton("Apply all");
	btn_incrementalPatternAll.addActionListener(incrementalMatrices);
        btn_incrementalPatternAll.setToolTipText("Generate new matrices from all the traffic matrices");

	pnl_incrementalPattern.setLayout(new MigLayout("insets 0 0 0 0", "[grow][][]", "[grow]"));
	pnl_incrementalPattern.add(cmb_incrementalPattern, "grow, wmin 50");
	pnl_incrementalPattern.add(btn_incrementalPattern);
	pnl_incrementalPattern.add(btn_incrementalPatternAll);

        pnl_popDistBasedTrafficModelPattern.setLayout(new MigLayout("insets 0 0 0 0", "[][grow]", "[][grow][grow]"));

	Object[][] data2 = new Object[][] {{"1", 1.0}};

	String[] header2 = new String[]{"", "1"};

	levelMatrixTableModel = new LevelMatrixTableModel();

	final JTable table2 = new AdvancedJTable(levelMatrixTableModel);

	levelMatrixTableModel.addTableModelListener(new TableModelListener()
	{

	    @Override
	    public void tableChanged(TableModelEvent e)
	    {
		if (e.getType() == TableModelEvent.DELETE)
		{
		    levelMatrixTableModel.setColumnCount(levelMatrixTableModel.getRowCount() + 1);
		}

		if (e.getType() == TableModelEvent.INSERT)
		{
		    int start = e.getFirstRow();
		    int end = e.getLastRow();

		    int N = levelMatrixTableModel.getRowCount();

		    if (start == 0)
		    {
			levelMatrixTableModel.addColumn("", new String[]
				{
				    "1"
				});
		    }

		    for (int i = start; i <= end; i++)
		    {
			levelMatrixTableModel.setValueAt(Integer.toString(i + 1), i, 0);

			for (int j = 0; j < start; j++)
			{
			    levelMatrixTableModel.setValueAt(1.0, i, j + 1);
			}

			Double[] aux = new Double[N];
			Arrays.fill(aux, 1.0);
			levelMatrixTableModel.addColumn(String.format(Integer.toString(i + 1), i), aux);
		    }
		}

		JTableHeader header = table2.getTableHeader();
		if (header != null && table2.getColumnCount() > 0)
		{
		    DefaultTableCellRenderer aux = new DefaultTableCellRenderer();
		    aux.setOpaque(true);
		    aux.setBorder(BorderFactory.createBevelBorder(EtchedBorder.RAISED));
		    aux.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
		    aux.setForeground(header.getForeground());
		    aux.setBackground(header.getBackground());
		    aux.setFont(header.getFont());
		    table2.getColumnModel().getColumn(0).setCellRenderer(aux);
		}

		table2.revalidate();
		table2.repaint();
	    }
	});

	levelMatrixTableModel.setDataVector(data2, header2);

	Object[][] data = new Object[][]
	{
	    {
		"0", "Node 0", 0.0, 0.0, 250000, 1
	    },
	    {
		"1", "Node 1", 0.0, 0.0, 50000, 1
	    },
	    {
		"2", "Node 2", 0.0, 0.0, 25000, 1
	    },
	    {
		"3", "Node 3", 0.0, 0.0, 125000, 1
	    }
	};

	final String[] header = new String[]
	{
	    "Id", "Name", "X", "Y", "Population", "Level"
	};

	nodeInfoTableModel = new ClassAwareTableModel(data, header)
	{
            private static final long serialVersionUID = 1L;

	    @Override
	    public boolean isCellEditable(int row, int column)
	    {
		return column != 0;
	    }

	    @Override
	    public void setValueAt(Object value, int row, int column)
	    {
		if (column == 4 || column == 5)
		{
		    if (value instanceof Integer)
		    {
			int aux = (Integer) value;

			if (aux < 0)
			{
			    ErrorHandling.showErrorDialog("Population/level must be an integer number greater than zero", "Error editing population/level attribute");
			    return;
			}

			if (column == 5)
			{
			    maxLevel = -1;
			    int N = getRowCount();
			    for (int nodeId = 0; nodeId < N; nodeId++)
			    {
				int level;

				if (nodeId != row) level = Integer.parseInt(getValueAt(nodeId, column).toString());
				else level = aux;

				if (maxLevel < level) maxLevel = level;
			    }

			    ((DefaultTableModel) levelMatrixTableModel).setRowCount(maxLevel);

			    table2.revalidate();
			}
		    }
		    else
		    {
			ErrorHandling.showErrorDialog("Population/level must be an integer number greater than zero", "Error editing population/level attribute");
			return;
		    }
		}

		super.setValueAt(value, row, column);
	    }
	};

	JTable table = new AdvancedJTable(nodeInfoTableModel);

	JPanel aux1 = new JPanel();
	final JTextField numNodes = new JTextField("4", 5);
	aux1.add(new JLabel("Number of nodes"));
	aux1.add(numNodes);

	JPanel aux2 = new JPanel(new MigLayout("insets 0 0 0 0", "[][]", "[][][][][][][grow]"));
	aux2.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Model parameters"));

	txt_randomFactor = new JTextField("0.5", 5);   txt_randomFactor.setHorizontalAlignment(JTextField.CENTER);
	txt_distanceOffset = new JTextField("1", 5);   txt_distanceOffset.setHorizontalAlignment(JTextField.CENTER);
	txt_distancePower = new JTextField("1", 5);    txt_distancePower.setHorizontalAlignment(JTextField.CENTER);
	txt_populationOffset = new JTextField("1", 5); txt_populationOffset.setHorizontalAlignment(JTextField.CENTER);
	txt_populationPower = new JTextField("2", 5);  txt_populationPower.setHorizontalAlignment(JTextField.CENTER);
        chk_populationDistanceModelSymmetric = new JCheckBox();

	aux2.add(new JLabel("Random factor"));
	aux2.add(txt_randomFactor, "align right, wrap");
	aux2.add(new JLabel("Population offset"));
	aux2.add(txt_populationOffset, "align right, wrap");
	aux2.add(new JLabel("Population power"));
	aux2.add(txt_populationPower, "align right, wrap");
	aux2.add(new JLabel("Distance offset"));
	aux2.add(txt_distanceOffset, "align right, wrap");
	aux2.add(new JLabel("Distance power"));
	aux2.add(txt_distancePower, "align right, wrap");
	aux2.add(new JLabel("Symmetric traffic?"));
        aux2.add(chk_populationDistanceModelSymmetric, "align center, wrap");
        
        ActionListener applyPopulationDistanceModel = new ApplyPopulationDistanceModel();

	applyPopDist = new JButton("Apply");
	applyPopDist.addActionListener(applyPopulationDistanceModel);
        applyPopDist.setToolTipText("Generate a new traffic matrix using the population-distance model");
        
	applyPopDist_applyBatch = new JButton("Apply batch");
	applyPopDist_applyBatch.addActionListener(applyPopulationDistanceModel);
        applyPopDist.setToolTipText("Generate a set of traffic matrices using the population-distance model");
        
        JPanel aux2_panel = new JPanel();
        aux2_panel.add(applyPopDist);
        aux2_panel.add(applyPopDist_applyBatch);
        
	aux2.add(aux2_panel, "top, center, growx, spanx 2");

	JPanel aux3 = new JPanel(new MigLayout("insets 0 0 0 0", "[grow][]", "[grow]"));
	aux3.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Topology information"));

	final JScrollPane pane = new JScrollPane(table);
	aux3.add(pane, "grow, wrap");
	new FixedColumnDecorator(1, pane);

	numNodes.setHorizontalAlignment(JTextField.CENTER);

	numNodes.addKeyListener(new KeyAdapter()
	{

	    @Override
	    public void keyPressed(KeyEvent e)
	    {
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_ENTER)
		{
		    JTextField src = (JTextField) e.getSource();

		    try
		    {
			int oldN = nodeInfoTableModel.getRowCount();
			int newN = Integer.parseInt(src.getText());
			if (newN < 2) throw new Exception("Number of nodes must be greater than 1");

			if (newN > oldN)
			    for (int nodeId = oldN; nodeId < newN; nodeId++)
                                nodeInfoTableModel.addRow(new Object[] {Integer.toString(nodeId), "Node " + nodeId, 0.0, 0.0, 1, 1});
                        else
                            nodeInfoTableModel.setRowCount(newN);
		    }
		    catch (Exception ex)
		    {
                        ex.printStackTrace();
			ErrorHandling.addErrorOrException(ex, GUITrafficDesign.class);
			ErrorHandling.showErrorDialog("Error changing number of nodes");

			if (src.getText().isEmpty())
			{
			    src.setText(Integer.toString(nodeInfoTableModel.getRowCount()));
			}
		    }
		}
	    }
	});
        
	numNodes.addFocusListener(new NumNodesListener());

	JButton loadTop = new JButton("Load from file");
	loadTop.addActionListener(new ActionListener()
	{

	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
                checkNetPlanFileChooser();
                
                int rc = fc_netPlan.showOpenDialog(null);
		if (rc != JFileChooser.APPROVE_OPTION) return;

		try
		{
		    // 1. Load and check network structure from n2p file
		    NetPlan aux = new NetPlan(fc_netPlan.getSelectedFile());

		    int N = aux.getNumberOfNodes();

		    if (N == 0)
		    {
			throw new Exception("Network structure doesn't contain a physical topology");
		    }

		    Object[][] data = new Object[N][6];
		    for (int nodeId = 0; nodeId < N; nodeId++)
		    {
			int population, level;

			try { population = Integer.parseInt(aux.getNodeAttribute(nodeId, "population")); }
			catch(Exception ex) { population = 1; }

			try { level = Integer.parseInt(aux.getNodeAttribute(nodeId, "level")); }
			catch(Exception ex) { level = 1; }

			data[nodeId][0] = Integer.toString(nodeId);
			data[nodeId][1] = aux.getNodeName(nodeId);
			data[nodeId][2] = aux.getNodeXYPosition(nodeId)[0];
			data[nodeId][3] = aux.getNodeXYPosition(nodeId)[1];
			data[nodeId][4] = population < 1 ? 1 : population;
			data[nodeId][5] = level < 1 ? 1 : level;
		    }

		    nodeInfoTableModel.setDataVector(data, header);
		    numNodes.setText(Integer.toString(N));
//			new FixedColumnDecorator(1, pane);
		}
		catch (Exception ex)
		{
		    ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network structure");
		}
	    }
	});

	aux3.add(loadTop, "align center");

	JPanel aux4 = new JPanel(new MigLayout("fill, insets 0 0 0 0", "", ""));
	aux4.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Level matrix"));


	JScrollPane pane2 = new JScrollPane(table2);
	aux4.add(pane2, "grow");
//	new FixedColumnDecorator(1, pane2);

	pnl_popDistBasedTrafficModelPattern.add(aux1, "align left, spanx 2, wrap");
	pnl_popDistBasedTrafficModelPattern.add(aux2, "grow");
	pnl_popDistBasedTrafficModelPattern.add(aux3, "grow, wrap");
	pnl_popDistBasedTrafficModelPattern.add(aux4, "grow, spanx 2");

	leftPane.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[][grow]"));
	leftPane.add(pnl_generalTrafficModelsPattern, "growx, wrap");
	leftPane.add(pnl_popDistBasedTrafficModelPattern, "grow");

//	rightPane.setLayout(new MigLayout("fill, insets 0 0 0 0")); //, "[grow,fill]", "[grow,fill]"));
//	rightPane.add(pnl_buttonBar, "growx, right, dock north");
//	rightPane.add(tabbedPane, "grow");
//	rightPane.add(pnl_normalizationPattern, "growx, dock north, h 50");
//	rightPane.add(pnl_incrementalPattern, "growx, dock north, h 50");
        
        rightPane.setLayout(new BorderLayout());
	rightPane.add(pnl_buttonBar, BorderLayout.NORTH);
        rightPane.add(tabbedPane, BorderLayout.CENTER);
        
        pnl_buttonBar.setMinimumSize( new Dimension(0, 0) );
        
        JPanel pane1 = new JPanel(new BorderLayout());
        pane1.add(pnl_normalizationPattern, BorderLayout.NORTH);
        pane1.add(pnl_incrementalPattern, BorderLayout.SOUTH);
        rightPane.add(pane1, BorderLayout.SOUTH);

//	rightPane.setLayout(new MigLayout("insets 0 0 0 0", "[grow,center]", "[][grow][][]"));
//	rightPane.add(pnl_buttonBar, "growx, wrap");
//	rightPane.add(tabbedPane, "grow, wrap");
//	rightPane.add(pnl_normalizationPattern, "growx, wrap, h 30");
//	rightPane.add(pnl_incrementalPattern, "growx, h 30");
    }

    /**
     *
     * @param trafficMatrixId
     * @return
     */
    public double[][] getTrafficMatrix(int trafficMatrixId)
    {
	TableModel aux = trafficMatrices.get(trafficMatrixId).getModel();
	int N = aux.getRowCount() - 1;

	double[][] trafficMatrix = new double[N][N];

	for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
	{
	    for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
	    {
		Number value = (Number) aux.getValueAt(ingressNodeId, egressNodeId + 1);
		trafficMatrix[ingressNodeId][egressNodeId] = value.doubleValue();
	    }
	}

	return trafficMatrix;
    }

    /**
     *
     */
    public void removeAllMatrices()
    {
	while (tabbedPane.getTabCount() > 1) tabbedPane.remove(0);

        trafficMatrices.clear();
    }

    /**
     *
     * @param trafficMatrix
     */
    public void setTrafficMatrix(double[][] trafficMatrix)
    {
	removeAllMatrices();

	addTrafficMatrix();
	setTrafficMatrix(trafficMatrix, 0);
	tabbedPane.setSelectedIndex(0);
    }

    /**
     *
     * @param trafficMatrix
     * @param trafficMatrixId
     */
    public void setTrafficMatrix(double[][] trafficMatrix, int trafficMatrixId)
    {
	int N = trafficMatrix.length;

        String[] columnHeaders = new String[N+2];
        Object[][] data = new Object[N+1][N+2];
        
        double[] ingressTrafficPerNode = new double[N];
        double[] egressTrafficPerNode = new double[N];
        double totalTraffic = 0;

        for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
            {
                double currentTrafficValue = trafficMatrix[ingressNodeId][egressNodeId];

                ingressTrafficPerNode[ingressNodeId] += currentTrafficValue;
                egressTrafficPerNode[egressNodeId] += currentTrafficValue;
                totalTraffic += currentTrafficValue;
            }
        }
        
        for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            String aux = "Node " + ingressNodeId;
            columnHeaders[1 + ingressNodeId] = aux;
            
            data[ingressNodeId][0] = aux;
            data[ingressNodeId][data[ingressNodeId].length - 1] = ingressTrafficPerNode[ingressNodeId];

            for(int i = 0; i < N; i++)
                data[ingressNodeId][i+1] = trafficMatrix[ingressNodeId][i];
        }
        
        Arrays.fill(data[data.length - 1], 0.0);
        data[data.length - 1][0] = "Total";
        
        for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
            data[data.length - 1][egressNodeId + 1] = egressTrafficPerNode[egressNodeId];
        
        data[data.length - 1][data[data.length - 1].length - 1] = totalTraffic;
        columnHeaders[0] = "";
        columnHeaders[columnHeaders.length - 1] = "Total";
        
	DefaultTableModel model = (DefaultTableModel) trafficMatrices.get(trafficMatrixId).getModel();
        model.setDataVector(data, columnHeaders);
    }
    
    private static double[][] resizeTrafficMatrix(double[][] trafficMatrix, int N)
    {
        int oldN = trafficMatrix.length;
        int minN = Math.min(oldN, N);
        
        double[][] newTrafficMatrix = new double[N][N];
        for(int ingressNodeId = 0; ingressNodeId < minN; ingressNodeId++)
            for(int egressNodeId = 0; egressNodeId < minN; egressNodeId++)
            newTrafficMatrix[ingressNodeId][egressNodeId] = trafficMatrix[ingressNodeId][egressNodeId];
        
        return newTrafficMatrix;
    }
    
    private int addTrafficMatrix()
    {
        int N = 4;
        Object[][] data = new Object[N+1][N+2];
        String[] columnHeaders = new String[N+2];
        
        for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
        {
            String aux = "Node " + ingressNodeId;
            columnHeaders[1 + ingressNodeId] = aux;
            
            Arrays.fill(data[ingressNodeId], 0.0);
            data[ingressNodeId][0] = aux;
        }
        
        Arrays.fill(data[data.length - 1], 0.0);
        data[data.length - 1][0] = "";
        
        columnHeaders[0] = "";
        columnHeaders[columnHeaders.length - 1] = "";
        
	final DefaultTableModel model = new ClassAwareTableModel(data, columnHeaders)
	{
            private static final long serialVersionUID = 1L;

	    @Override
	    public boolean isCellEditable(int row, int column)
	    {
                int columnCount = getColumnCount();
                int rowCount = getRowCount();
                
		return (column == 0 || column == columnCount - 1 || row == rowCount - 1 || row == column - 1) ? false : true;
	    }

	    @Override
	    public void setValueAt(Object newValue, int row, int column)
	    {
		Object oldValue = getValueAt(row, column);

		if (newValue instanceof Double)
		{
		    // If value doesn't change, exit from function
		    if (newValue.equals(oldValue)) return;

		    double trafficAmount = (Double) newValue;

		    if (trafficAmount < 0)
		    {
			ErrorHandling.showErrorDialog("Traffic amount must be greater or equal than zero", "Error updating traffic matrix");
			return;
		    }
                }
                
		super.setValueAt(newValue, row, column);
                
		if (newValue instanceof Number)
		{
                    int columnCount = getColumnCount();
                    int rowCount = getRowCount();
                    
                    if (column != columnCount - 1 && row != rowCount - 1)
                    {
                        ListIterator<JTable> it = trafficMatrices.listIterator();
                        while(it.hasNext())
                        {
                            int tableId = it.nextIndex();
                            if (it.next().getModel() != this) continue;
                            
                            double[][] trafficMatrix = getTrafficMatrix(tableId);
                            int N = trafficMatrix.length;

                            double[] ingressTrafficPerNode = new double[N];
                            double[] egressTrafficPerNode = new double[N];
                            double totalTraffic = 0;
                            
                            for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
                            {
                                for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                                {
                                    double currentTrafficValue = trafficMatrix[ingressNodeId][egressNodeId];

                                    ingressTrafficPerNode[ingressNodeId] += currentTrafficValue;
                                    egressTrafficPerNode[egressNodeId] += currentTrafficValue;
                                    totalTraffic += currentTrafficValue;
                                }
                            }

                            for(int nodeId = 0; nodeId < N; nodeId++)
                            {
                                setValueAt(ingressTrafficPerNode[nodeId], nodeId, columnCount - 1);
                                setValueAt(egressTrafficPerNode[nodeId], rowCount - 1, nodeId + 1);
                            }

                            setValueAt(totalTraffic, rowCount - 1, columnCount - 1);
                        }
                        
                    }
		}
	    }
	};

	final JTable table = new AdvancedJTable(model);
        table.setDefaultRenderer(Object.class, new TotalRowColumn());
        table.setDefaultRenderer(Double.class, new TotalRowColumn());
        table.setDefaultRenderer(Number.class, new TotalRowColumn());
        table.setDefaultRenderer(Integer.class, new TotalRowColumn());
        table.setDefaultRenderer(String.class, new TotalRowColumn());
        
	trafficMatrices.add(table);

	JScrollPane pane = new JScrollPane(table);
//	new FixedColumnDecorator(1, pane);

	tabbedPane.insertTab("TM " + Integer.toString(tabbedPane.getTabCount() - 1), null, pane, null, tabbedPane.getTabCount() - 1);
        
        int tabId = getNumberOfTrafficMatrices() - 1;
        return tabId;
    }
    
    private final static TabIcon closeTabIcon = new TabIcon(null, TabIcon.TIMES_SIGN);
    
    private static class TotalRowColumn extends CellRenderers.NumberCellRenderer
    {
        private final static Color bgColor = new Color(240, 240, 240);
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (row == table.getRowCount() - 1 || column == table.getColumnCount() - 1)
            {
                c.setBackground(bgColor);
                if (isSelected) c.setForeground(Color.BLACK);
            }
            
            return c;
        }
    }
    
    private class NormalizeMatrices implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int option = cmb_normalizationPattern.getSelectedIndex();
            
            if (option == 0)
            {
                ErrorHandling.showWarningDialog("Please, select a normalization pattern", "Error applying normalization patterns");
                return;
            }
            
            int initialMatrixId = getSelectedTrafficMatrix();
            int N = getTrafficMatrix(initialMatrixId).length;
            int numMatrices = 1;
            
            boolean normalizeAll = e.getSource() == btn_normalizationPatternAll;
            
            if (normalizeAll)
            {
                initialMatrixId = 0;
		numMatrices = getNumberOfTrafficMatrices();

		Iterator<JTable> it = trafficMatrices.iterator();
		while (it.hasNext())
		{
		    TableModel aux = it.next().getModel();
		    if (N == -1)
                    {
                        N = aux.getRowCount() - 1;
		    }
		    else if (N != aux.getRowCount() - 1)
		    {
			ErrorHandling.showErrorDialog("All matrices must have the same size", "Error applying normalization pattern");
			return;
		    }
		}
            }
            
            switch (option)
            {
                case 1:

                    double totalTraffic;

                    while (true)
                    {
                        String str = JOptionPane.showInputDialog(null, "Total traffic (in Erlangs)", "Traffic normalization: total traffic", JOptionPane.QUESTION_MESSAGE);
                        if (str == null) return;

                        try
                        {
                            totalTraffic = Double.parseDouble(str);
                            if (totalTraffic <= 0) throw new IllegalArgumentException("Traffic amount must be greater or equal than zero");

                            break;
                        }
                        catch (Throwable ex)
                        {
                            ErrorHandling.showErrorDialog("Non-valid total traffic value. Please, introduce a number greater or equal than zero", "Error applying normalization pattern");
                        }
                    }

                    for (int matrixId = initialMatrixId; matrixId < initialMatrixId + numMatrices; matrixId++)
                        setTrafficMatrix(TrafficMatrixGenerationModels.normalizationPattern_totalTraffic(getTrafficMatrix(matrixId), totalTraffic), matrixId);

                    break;

                case 2:
                case 3:

                    DefaultTableModel model = new ClassAwareTableModel()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isCellEditable(int row, int col)
                        {
                            return true;
                        }

                        @Override
                        public void setValueAt(Object newValue, int row, int column)
                        {
                            Object oldValue = getValueAt(row, column);

                            // If value doesn't change, exit from function
                            if (newValue.equals(oldValue)) return;

                            double trafficAmount = (Double) newValue;

                            if (trafficAmount < 0)
                            {
                                ErrorHandling.showErrorDialog("Traffic amount must be greater or equal than zero", "Error introducing traffic amount");
                                return;
                            }

                            super.setValueAt(newValue, row, column);
                        }
                    };

                    Object[][] data = new Object[N][1];
                    for (int nodeId = 0; nodeId < N; nodeId++) data[nodeId][0] = Double.valueOf(0);

                    String[] header = new String[] { option == 2 ? "Total ingress traffic per node" : "Total egress traffic per node" };
                    model.setDataVector(data, header);

                    JTable table = new AdvancedJTable(model);

                    JPanel pane = new JPanel();
                    pane.add(new JScrollPane(table));

                    int result = JOptionPane.showConfirmDialog(null, pane, option == 2 ? "Please enter total ingress traffic per node (one value per row)" : "Please enter total egress traffic per node (one value per row)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    double[] newValue = new double[N];
                    for (int nodeId = 0; nodeId < N; nodeId++) newValue[nodeId] = (Double) model.getValueAt(nodeId, 0);

                    for (int matrixId = initialMatrixId; matrixId < initialMatrixId + numMatrices; matrixId++)
                        setTrafficMatrix(option == 2 ? TrafficMatrixGenerationModels.normalizationPattern_outgoingTraffic(getTrafficMatrix(matrixId), newValue) : TrafficMatrixGenerationModels.normalizationPattern_incomingTraffic(getTrafficMatrix(matrixId), newValue), matrixId);

                    break;
                    
                case 4:
                    
                    checkNetPlanFileChooser();
                    int rc = fc_netPlan.showOpenDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;

                    NetPlan aux;
                    
                    try
                    {
                        aux = new NetPlan(fc_netPlan.getSelectedFile());
                    }
                    catch(Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
                        return;
                    }
                    
                    String[] options = new String[] {"Estimated (upper bound)", "Exact (using JOM)"};
                    
                    String instructions = "<html><body>These methods multiply the reference matrix by a factor <i>alpha</i>, "
                            + "so that the resulting matrix represents the maximum traffic matrix that can be carried "
                            + "by the network. Two methods are available:<ul>"
                            + "<li>Estimated (upper bound): Computes the maximum traffic so that the shortest-path routing (in hops) does not over-subscribe the total installed capacity</li>"
                            + "<li>Exact (JOM is used, may take a while): It solves a formulation with unconstrained routing so that link capacities are not violated</li>"
                            + "</ul></body></html>";
                    
                    int out = JOptionPane.showOptionDialog(null, instructions, "Select a computation method", 
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                    
                    if (out != 0 && out != 1) return;
                    
                    for (int matrixId = initialMatrixId; matrixId < initialMatrixId + numMatrices; matrixId++)
                    {
                        double[][] trafficMatrix = getTrafficMatrix(matrixId);
                        aux.setTrafficMatrix(trafficMatrix);
                        
                        double[] h_d;
                        if (out == 0)
                        {
                            h_d = TrafficMatrixGenerationModels.normalizeTraffic_networkCapacity(aux, Constants.ShortestPathType.HOPS);
                        }
                        else
                        {
                            String solverName = Configuration.getOption("defaultILPSolver");
                            String solverLibraryName = Configuration.getOption(solverName + "SolverLibraryName");
                            
                            h_d = TrafficMatrixGenerationModels.normalizeTraffic_linkCapacity_xde(aux, solverName, solverLibraryName);
                        }
                        
                        aux.setDemandOfferedTrafficInErlangsVector(h_d);
                        setTrafficMatrix(aux.getTrafficMatrix(), matrixId);
                    }
                    
                    break;

                default:

                    ErrorHandling.showWarningDialog("Please, select a normalization pattern", "Apply normalization pattern");
                    break;
            }
        }
    }

    private class IncrementalMatrices implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int option = cmb_incrementalPattern.getSelectedIndex();
            
            if (option == 0)
            {
                ErrorHandling.showWarningDialog("Please, select a model", "Error generating new matrices");
                return;
            }
            
            int initialMatrixId = getSelectedTrafficMatrix();
            int numMatrices = 1;
            
            boolean applyAll = e.getSource() == btn_incrementalPatternAll;
            
            if (applyAll)
            {
                initialMatrixId = 0;
		numMatrices = getNumberOfTrafficMatrices();
            }
            
            double cagr;
            double cv;
            double maxRelativeVariation;
            int numMatricesToGenerate;

            JTextField txt_cagr = new JTextField("0.2", 5);
            JTextField txt_cv = new JTextField("0.1", 5);
            JTextField txt_maxRelativeVariation = new JTextField("0.2", 5);
            JTextField txt_numMatricesToGenerate = new JTextField("3", 5);

            JPanel pane = new JPanel(new GridLayout(0,2));
            
            if (option == 1)
            {
                pane.add(new JLabel("Compound Annual Grow Rate (i.e. 0.2 means 20% year-over-year):"));
                pane.add(txt_cagr);
            }
            
            if (option == 3)
            {
                pane.add(new JLabel("Coefficient of variation (quotient between standard deviation and mean value):"));
                pane.add(txt_cv);
            }
            
            if (option == 2 || option == 3)
            {
                pane.add(new JLabel("Maximum relative variation (i.e. 0.5 means a maximum relative deviation from seminal value equal 50%):"));
                pane.add(txt_maxRelativeVariation);
            }

            pane.add(new JLabel("Number of new matrices (the seminal matrix is not modified):"));
            pane.add(txt_numMatricesToGenerate);

            while (true)
            {
                int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the number of nodes and matrices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                try
                {
                    cagr = Double.parseDouble(txt_cagr.getText());
                    cv = Double.parseDouble(txt_cv.getText());
                    maxRelativeVariation = Double.parseDouble(txt_maxRelativeVariation.getText());
                    numMatricesToGenerate = Integer.parseInt(txt_numMatricesToGenerate.getText());

                    if (cagr <= 0) throw new Net2PlanException("Compound annual growth rate must be greater than zero");
                    if (cv <= 0) throw new Net2PlanException("Coefficient of variation must be greater than zero");
                    if (maxRelativeVariation <= 0) throw new Net2PlanException("Maximum relative variation must be greater than zero");
                    if (numMatricesToGenerate < 1) throw new Net2PlanException("Number of matrices must be greater or equal than one");

                    break;
                }
                catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error generating new matrices");
                }
            }

            for (int matrixId = initialMatrixId; matrixId < initialMatrixId + numMatrices; matrixId++)
            {
                double[][] trafficMatrix = getTrafficMatrix(matrixId);
                
                List<double[][]> newMatrices;
                
                switch (option)
                {
                    case 1:
                        newMatrices = TrafficMatrixGenerationModels.computeMatricesCAGR(trafficMatrix, cagr, numMatricesToGenerate);
                        break;

                    case 2:
                        newMatrices = TrafficMatrixGenerationModels.computeMatricesRandomUniformVariation(trafficMatrix, maxRelativeVariation, numMatricesToGenerate);
                        break;

                    case 3:
                        newMatrices = TrafficMatrixGenerationModels.computeMatricesRandomGaussianVariation(trafficMatrix, cv, maxRelativeVariation, numMatricesToGenerate);
                        break;

                    default:
                        ErrorHandling.showWarningDialog("Please, select a model", "Error generating new matrices");
                        return;
                }
                
                for(double[][] matrix: newMatrices)
                {
                    int id = addTrafficMatrix();
                    setTrafficMatrix(matrix, id);
                }
            }
        }
    }

    private class SymmetrizeMatrices implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int initialMatrixId = getSelectedTrafficMatrix();
            int numMatrices = 1;
            
            boolean symmetrizeAll = e.getSource() == btn_symmetrizeAll;
            
            if (symmetrizeAll)
            {
                initialMatrixId = 0;
		numMatrices = getNumberOfTrafficMatrices();
            }
            
            for (int matrixId = initialMatrixId; matrixId < initialMatrixId + numMatrices; matrixId++)
            {
                double[][] trafficMatrix = getTrafficMatrix(matrixId);
                TrafficMatrixGenerationModels.symmetrizeTrafficMatrix(trafficMatrix);
                
                setTrafficMatrix(trafficMatrix, matrixId);
            }
        }
    }

    private class ResetMatrices implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean resetAll = e.getSource() == btn_resetAll ? true : false;
            
            if (resetAll)
            {
                ListIterator<JTable> it = trafficMatrices.listIterator();
                while (it.hasNext())
                {
                    TableModel aux = it.next().getModel();
                    int N = aux.getRowCount() - 1;

                    for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
                        for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                            aux.setValueAt(0, ingressNodeId, egressNodeId + 1);
                }
            }
            else
            {
                int matrixId = getSelectedTrafficMatrix();
                TableModel aux = trafficMatrices.get(matrixId).getModel();
                int N = aux.getRowCount() - 1;

                for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
                    for (int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                        aux.setValueAt(0, ingressNodeId, egressNodeId + 1);
            }
        }
        
    }

    private class ApplyTrafficModels implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean isBatch = e.getSource() == btn_trafficModelPattern_applyBatch;
            
            int option = cmb_trafficModelPattern.getSelectedIndex();

            if (option == 0)
            {
                ErrorHandling.showWarningDialog("Please, select a traffic model", "Error applying traffic model");
                return;
            }
            
            int N;
            int numMatrices;
            double constantValue;

            JTextField numberOfNodes = new JTextField(5);
            JTextField numberOfMatrices = new JTextField(5);
            JTextField txt_constantValue = new JTextField(5);
            JCheckBox chk_symmetricTraffic = new JCheckBox();
            chk_symmetricTraffic.setSelected(true);

            JPanel pane = new JPanel(new GridLayout(0,2));
            pane.add(new JLabel("Number of nodes: "));
            pane.add(numberOfNodes);
            
            if (isBatch)
            {
                if (option == 6)
                {
                    ErrorHandling.showErrorDialog("This model only is applicable to compute one single matrix", "Error applying gravity model");
                    return;
                }
                
                pane.add(new JLabel("Number of matrices: "));
                pane.add(numberOfMatrices);
            }
            
            if (option == 1)
            {
                pane.add(new JLabel("Constant value: "));
                pane.add(txt_constantValue);
            }
            
            if (option != 1 && option != 6)
            {
                pane.add(new JLabel("Symmetric traffic: "));
                pane.add(chk_symmetricTraffic);
            }

            while (true)
            {
                int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the number of nodes and matrices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.OK_OPTION) return;

                try
                {
                    N = Integer.parseInt(numberOfNodes.getText());
                    numMatrices = isBatch ? Integer.parseInt(numberOfMatrices.getText()) : 1;
                    constantValue = option == 1 ? Double.parseDouble(txt_constantValue.getText()) : Double.MAX_VALUE;

                    if (N < 2) throw new IllegalArgumentException("Number of nodes must be greater than 1");
                    if (numMatrices < 0) throw new IllegalArgumentException("Number of matrices must be greater or equal than 1");
                    if (constantValue < 0) throw new IllegalArgumentException("Constant value must be greater or equal than zero");

                    break;
                }
                catch (Throwable ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error applying traffic model");
                }
            }
            
            boolean removeExistingMatrices = askForRemovingExistingMatrices();
            if (removeExistingMatrices) removeAllMatrices();
            
            boolean isSymmetric = chk_symmetricTraffic.isSelected();
            
            int firstMatrixId = -1;
            
            for(int i = 0; i < numMatrices; i++)
            {
                int matrixId = addTrafficMatrix();
                
                if (i == 0) firstMatrixId = matrixId;
                
                double[][] trafficMatrix;
                
		switch (option)
		{
                    case 1:
                        trafficMatrix = TrafficMatrixGenerationModels.constant(N, constantValue);
                        break;
                        
		    case 2:
                        trafficMatrix = TrafficMatrixGenerationModels.uniformRandom(N, 0, 10);
			break;

		    case 3:
                        trafficMatrix = TrafficMatrixGenerationModels.uniformRandom(N, 0, 100);
			break;

		    case 4:
                        trafficMatrix = TrafficMatrixGenerationModels.bimodalUniformRandom(N, 0.5, 0, 100, 0, 10);
			break;

		    case 5:
                        trafficMatrix = TrafficMatrixGenerationModels.bimodalUniformRandom(N, 0.25, 0, 100, 0, 10);
			break;

                    case 6:

                        DefaultTableModel gravityModelTableModel = new ClassAwareTableModel()
                        {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public boolean isCellEditable(int row, int col)
                            {
                                return true;
                            }

                            @Override
                            public void setValueAt(Object newValue, int row, int column)
                            {
                                Object oldValue = getValueAt(row, column);

                                // If value doesn't change, exit from function
                                if (newValue.equals(oldValue))
                                {
                                    return;
                                }

                                double trafficAmount = (Double) newValue;

                                if (trafficAmount < 0)
                                {
                                    ErrorHandling.showErrorDialog("Traffic amount must be greater or equal than zero", "Error introducing traffic amount");
                                    return;
                                }

                                super.setValueAt(newValue, row, column);
                            }
                        };

                        Object[][] gravityModelData = new Object[N][2];
                        for (int nodeId = 0; nodeId < N; nodeId++)
                        {
                            gravityModelData[nodeId][0] = Double.valueOf(0);
                            gravityModelData[nodeId][1] = Double.valueOf(0);
                        }

                        String[] gravityModelHeader = new String[] { "Total ingress traffic per node", "Total egress traffic per node" };
                        gravityModelTableModel.setDataVector(gravityModelData, gravityModelHeader);

                        JTable gravityModelTable = new AdvancedJTable(gravityModelTableModel);

                        JPanel gravityModelPanel = new JPanel();
                        gravityModelPanel.add(new JScrollPane(gravityModelTable));
                        
                        double totalIngressTraffic, totalEgressTraffic;
                        
                        double[] ingressTrafficPerNode = new double[N];
                        double[] egressTrafficPerNode = new double[N];
                        
                        while(true)
                        {
                            int gravityModelResult = JOptionPane.showConfirmDialog(null, gravityModelPanel, "Please enter total ingress/egress traffic per node (one value per row)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (gravityModelResult != JOptionPane.OK_OPTION) return;

                            totalIngressTraffic = 0;
                            totalEgressTraffic = 0;

                            for (int nodeId = 0; nodeId < N; nodeId++)
                            {
                                ingressTrafficPerNode[nodeId] = (Double) gravityModelTableModel.getValueAt(nodeId, 0);
                                egressTrafficPerNode[nodeId] = (Double) gravityModelTableModel.getValueAt(nodeId, 1);

                                totalIngressTraffic += ingressTrafficPerNode[nodeId];
                                totalEgressTraffic += egressTrafficPerNode[nodeId];
                            }
                            
                            if (totalIngressTraffic == 0 || totalEgressTraffic == 0)
                                ErrorHandling.showErrorDialog("Total ingress and egress traffic must be greater than zero", "Error applying gravity model");
                            else if (totalIngressTraffic == totalEgressTraffic)
                                break;
                            else
                                ErrorHandling.showErrorDialog(String.format("Total ingress traffic (%f E) must be equal to the total egress traffic (%f E)", totalIngressTraffic, totalEgressTraffic), "Error applying gravity model");
                        }

                        trafficMatrix = TrafficMatrixGenerationModels.gravityModel(ingressTrafficPerNode, egressTrafficPerNode);

                        setTrafficMatrix(trafficMatrix, matrixId);

                        break;

                    default:
                        throw new RuntimeException("Bad");
		}
                
                if (isSymmetric) TrafficMatrixGenerationModels.symmetrizeTrafficMatrix(trafficMatrix);
                    
                setTrafficMatrix(trafficMatrix, matrixId);
            }
            
            tabbedPane.setSelectedIndex(firstMatrixId);
        }
    }
    
    private class ArithmeticOperationListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JButton button = (JButton) e.getSource();
            
            if (button == btn_sumAll)
            {
                int numTrafficMatrices = getNumberOfTrafficMatrices();
                
                double[][] referenceTrafficMatrix = getTrafficMatrix(0);
                int N = referenceTrafficMatrix.length;
                
                for(int matrixId = 1; matrixId < numTrafficMatrices; matrixId++)
                {
                    double[][] aux_trafficMatrix = getTrafficMatrix(matrixId);
                    
                    int aux_N = aux_trafficMatrix.length;
                    if (aux_N != N)
                    {
                        ErrorHandling.showErrorDialog("All matrices must have the same size", "Error summing all matrices");
                        return;
                    }
                    
                    for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
                        for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                            referenceTrafficMatrix[ingressNodeId][egressNodeId] += aux_trafficMatrix[ingressNodeId][egressNodeId];
                }
                
                int matrixId = addTrafficMatrix();
                setTrafficMatrix(referenceTrafficMatrix, matrixId);
                
                tabbedPane.setSelectedIndex(getNumberOfTrafficMatrices() - 1);
            }
            else
            {
                int initialMatrixId, numMatrices;
                
                if (button == btn_multiplyAll)
                {
                    initialMatrixId = 0;
                    numMatrices = getNumberOfTrafficMatrices();
                }
                else
                {
                    initialMatrixId = getSelectedTrafficMatrix();
                    numMatrices = 1;
                }

                int lastMatrixId = initialMatrixId + numMatrices - 1;

                double multiplicativeFactor;
                JTextField txt_multiplicativeFactor = new JTextField(5);

                JPanel pane = new JPanel(new GridLayout(1,2));
                pane.add(new JLabel("Multiplicative factor: "));
                pane.add(txt_multiplicativeFactor);
            
                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the number of nodes and matrices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try
                    {
                        multiplicativeFactor = Double.parseDouble(txt_multiplicativeFactor.getText());
                        if (multiplicativeFactor <= 0) throw new IllegalArgumentException("Multiplicative factor must be greater than zero");

                        break;
                    }
                    catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error multiplying matrix by a factor");
                    }
                }
                
                for(int matrixId = initialMatrixId; matrixId <= lastMatrixId; matrixId++)
                {
                    double[][] trafficMatrix = getTrafficMatrix(matrixId);
                    
                    int N = trafficMatrix.length;
                    for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
                    {
                        for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
                        {
                            if (ingressNodeId == egressNodeId) continue;
                            
                            trafficMatrix[ingressNodeId][egressNodeId] *= multiplicativeFactor;
                        }
                    }

                    setTrafficMatrix(trafficMatrix, matrixId);
                }
            }
        }
    }
    
    private JButton btn_sumAll, btn_multiplyThis, btn_multiplyAll;
    
    private class ApplyPopulationDistanceModel implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean isBatch = e.getSource() == applyPopDist_applyBatch;
            
            int numMatrices = 1;
            
            JTextField numberOfMatrices = new JTextField(5);

            if (isBatch)
            {
                JPanel pane = new JPanel();
                pane.add(new JLabel("Number of matrices: "));
                pane.add(numberOfMatrices);

                while (true)
                {
                    int result = JOptionPane.showConfirmDialog(null, pane, "Please enter the number of nodes and matrices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (result != JOptionPane.OK_OPTION) return;

                    try
                    {
                        numMatrices = Integer.parseInt(numberOfMatrices.getText());
                        if (numMatrices < 0) throw new IllegalArgumentException("Number of matrices must be greater or equal than 1");

                        break;
                    }
                    catch (Throwable ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error applying traffic model");
                    }
                }
            }
            
            int N = nodeInfoTableModel.getRowCount();
            int L = levelMatrixTableModel.getRowCount();

            Point2D[] nodeXYPositionTable = new Point2D[N];
            int[] populationVector = new int[N];
            int[] levelVector = new int[N];
            double[][] levelMatrix = new double[L][L];

            for (int nodeId = 0; nodeId < N; nodeId++)
            {
                nodeXYPositionTable[nodeId] = new Point2D.Double(Double.parseDouble(nodeInfoTableModel.getValueAt(nodeId, 2).toString()), Double.parseDouble(nodeInfoTableModel.getValueAt(nodeId, 3).toString()));
                populationVector[nodeId] = Integer.parseInt(nodeInfoTableModel.getValueAt(nodeId, 4).toString());
                levelVector[nodeId] =  Integer.parseInt(nodeInfoTableModel.getValueAt(nodeId, 5).toString());
            }

            for (int fromLevel = 0; fromLevel < L; fromLevel++)
            {
                for (int toLevel = 0; toLevel < L; toLevel++)
                {
                    levelMatrix[fromLevel][toLevel] = (Double) levelMatrixTableModel.getValueAt(fromLevel, toLevel + 1);
                }
            }

            double randomFactor = Double.parseDouble(txt_randomFactor.getText());
            double distanceOffset = Double.parseDouble(txt_distanceOffset.getText());
            double distancePower = Double.parseDouble(txt_distancePower.getText());
            double populationOffset = Double.parseDouble(txt_populationOffset.getText());
            double populationPower = Double.parseDouble(txt_populationPower.getText());

            double[][] distanceMatrix = new double[N][N];
            double dist_max = -1;
            for (int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
            {
                for (int egressNodeId = ingressNodeId + 1; egressNodeId < N; egressNodeId++)
                {
                    if (ingressNodeId == egressNodeId) continue;

                    distanceMatrix[ingressNodeId][egressNodeId] = nodeXYPositionTable[ingressNodeId].distance(nodeXYPositionTable[egressNodeId]);
                    distanceMatrix[egressNodeId][ingressNodeId] = distanceMatrix[ingressNodeId][egressNodeId];

                    dist_max = Math.max(dist_max, distanceMatrix[ingressNodeId][egressNodeId]);
                }
            }

            if (dist_max == 0)
            {
                ErrorHandling.showWarningDialog("The maximum distance between nodes is zero, so traffic matrix would have only zero entries", "Warning");
                return;
            }
        
            boolean removeExistingMatrices = askForRemovingExistingMatrices();
            if (removeExistingMatrices) removeAllMatrices();
            
            boolean isSymmetric = chk_populationDistanceModelSymmetric.isSelected();
            
            int firstMatrixId = -1;
            
            for(int i = 0; i < numMatrices; i++)
            {
                int matrixId = addTrafficMatrix();
                
                if (i == 0) firstMatrixId = matrixId;
                
		double[][] trafficMatrix = TrafficMatrixGenerationModels.populationDistanceModel(distanceMatrix, populationVector, levelVector, levelMatrix, randomFactor, populationOffset, populationPower, distanceOffset, distancePower);
                if (isSymmetric) TrafficMatrixGenerationModels.symmetrizeTrafficMatrix(trafficMatrix);
                
		setTrafficMatrix(trafficMatrix, matrixId);
            }
            
            tabbedPane.setSelectedIndex(firstMatrixId);
        }
    }

    private boolean askForRemovingExistingMatrices()
    {
        int reply = JOptionPane.showConfirmDialog(null, "Existing traffic matrices should be removed?", "", JOptionPane.YES_NO_OPTION);
        
        return reply == JOptionPane.YES_OPTION ? true : false;
    }
    
    private int getNumberOfTrafficMatrices() { return trafficMatrices.size(); }
    private int getSelectedTrafficMatrix() { return tabbedPane.getSelectedIndex(); }

    private static class LevelMatrixTableModel extends ClassAwareTableModel
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) { return column != 0; }
    }

    private static class NumNodesListener extends FocusAdapter
    {
        @Override
        public void focusLost(FocusEvent evt)
        {
            JTextField src = (JTextField) evt.getSource();
            src.dispatchEvent(new KeyEvent(src, KeyEvent.KEY_PRESSED, new java.util.Date().getTime(), 0, KeyEvent.VK_ENTER, '0'));
        }
    }

}
