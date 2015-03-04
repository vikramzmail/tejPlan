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

package com.tejas.engine.utils;

import cern.colt.list.tint.IntArrayList;

import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.AttributesCellEditor;
import com.tejas.engine.utils.ButtonColumn;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.StringLabeller;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.WiderJComboBox;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.libraries.SRGUtils;
import com.tejas.engine.tools.GUINetworkDesign;
import com.tejas.engine.utils.topology.INetworkCallback;
import com.tejas.engine.utils.topology.plugins.TopologyPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Class in charge of showing a popup menu in the 'edit network plan' tab of the 
 * Offline Network Design tool.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class PopupMenuNetPlan extends MouseAdapter
{
    private final JTable table;
    private final String networkElement;
    private final boolean isEditable;
    private final INetworkCallback callback;
    private final TopologyPanel topologyPanel;
    private final List<NetPlan> netPlans;

    /**
     * Default constructor.
     * 
     * @param callback
     * @param topologyPanel
     * @param netPlans
     * @param table
     * @param networkElement
     * @param isEditable
     * @since 0.2.3
     */
    public PopupMenuNetPlan(INetworkCallback callback, TopologyPanel topologyPanel, List<NetPlan> netPlans, JTable table, String networkElement, boolean isEditable)
    {
        this.callback = callback;
        this.networkElement = networkElement;
        this.table = table;
        this.isEditable = isEditable;
        this.topologyPanel = topologyPanel;
        this.netPlans = netPlans;
    }

    private void showInCanvas(MouseEvent e, int itemId)
    {
        if (isTableEmpty()) return;

        int clickCount = e.getClickCount();

        switch(networkElement)
        {
            case "node":
                callback.showNode(itemId);
                break;

            case "link":
                if (clickCount == 1)
                {
                    callback.showLink(itemId);
                }
                else if (clickCount == 2)
                {
                    int col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
                    if (col == -1 || col >= table.getColumnCount()) return;

                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();

                    switch (col)
                    {
                        case 1:
                            callback.showNode(netPlan.getLinkOriginNode(itemId));
                            break;
                        case 2:
                            callback.showNode(netPlan.getLinkDestinationNode(itemId));
                            break;
                        default:
                            break;
                    }
                }

                break;

            case "demand":
                if (clickCount == 1)
                {
                    callback.showDemand(itemId);
                }
                else if (clickCount == 2)
                {
                    int col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
                    if (col == -1 || col >= table.getColumnCount()) return;

                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();

                    switch (col)
                    {
                        case 1:
                            callback.showNode(netPlan.getDemandIngressNode(itemId));
                            break;
                        case 2:
                            callback.showNode(netPlan.getDemandEgressNode(itemId));
                            break;
                        default:
                            break;
                    }
                }

                break;

            case "route":

                if (clickCount == 1)
                {
                    callback.showRoute(itemId);
                }
                else if (clickCount == 2)
                {
                    int col = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
                    if (col == -1 || col >= table.getColumnCount()) return;

                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();
                    int demandId = netPlan.getRouteDemand(itemId);

                    switch (col)
                    {
                        case 1:
                            callback.showDemand(demandId);
                            break;
                        case 2:
                            callback.showNode(netPlan.getDemandIngressNode(demandId));
                            break;
                        case 3:
                            callback.showNode(netPlan.getDemandEgressNode(demandId));
                            break;
                        default:
                            break;
                    }
                }

                break;

            case "protection segment":
                callback.showSegment(itemId);
                break;

            case "SRG":
                callback.showSRG(itemId);
                break;
                
            default:
                throw new RuntimeException("Bad");
        }
    }

    private boolean isTableEmpty()
    {
        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
        NetPlan netPlan = aux.getSecond();

        switch(networkElement)
        {
            case "node":
                return !netPlan.hasNodes();

            case "link":
                return !netPlan.hasLinks();

            case "demand":
                return !netPlan.hasDemands();

            case "route":
                return !netPlan.hasRoutes();

            case "protection segment":
                return !netPlan.hasProtectionSegments();

            case "SRG":
                return !netPlan.hasSRGs();

            default:
                throw new RuntimeException("Bad");
        }
    }

    private void doPopup(MouseEvent e)
    {
        final int row = table.rowAtPoint(e.getPoint());

        JPopupMenu popup = new JPopupMenu();

        JMenuItem addItem = getAddOption();
        if (addItem != null) popup.add(addItem);

        List<JMenuItem> extraAddOptions = getExtraAddOptions();
        if (!extraAddOptions.isEmpty())
            for(JMenuItem item : extraAddOptions)
                popup.add(item);

        if (!isTableEmpty())
        {
            if (row != -1)
            {
                if (popup.getSubElements().length > 0) popup.addSeparator();

                JMenuItem removeItem = new JMenuItem("Remove " + networkElement);

                removeItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int itemId = table.convertRowIndexToModel(row);

                            switch(networkElement)
                            {
                                case "node":
                                    netPlan.removeNode(itemId);
                                    topologyPanel.getCanvas().updateTopology(netPlan);
                                    break;

                                case "link":
                                    netPlan.removeLink(itemId);
                                    topologyPanel.getCanvas().updateTopology(netPlan);
                                    break;

                                case "demand":
                                    netPlan.removeDemand(itemId);
                                    break;

                                case "route":
                                    netPlan.removeRoute(itemId);
                                    break;

                                case "protection segment":
                                    netPlan.removeProtectionSegment(itemId);
                                    break;

                                case "SRG":
                                    netPlan.removeSRG(itemId);
                                    break;

                                default:
                                    throw new RuntimeException("Bad");
                            }

                            callback.updateNetPlanView();
                        }
                        catch (Throwable ex)
                        {
                            netPlans.set(aux.getFirst(), aux_netPlan);
                            ErrorHandling.addErrorOrException(ex, GUINetworkDesign.class);
                            ErrorHandling.showErrorDialog("Unable to remove " + networkElement);
                        }
                    }
                });

                popup.add(removeItem);

                JMenuItem addAttribute = new JMenuItem("Add/edit attribute");
                addAttribute.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        int itemId = table.convertRowIndexToModel(row);

                        JTextField txt_key = new JTextField(20);
                        JTextField txt_value = new JTextField(20);

                        JPanel pane = new JPanel();
                        pane.add(new JLabel("Attribute: "));
                        pane.add(txt_key);
                        pane.add(Box.createHorizontalStrut(15));
                        pane.add(new JLabel("Value: "));
                        pane.add(txt_value);

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        while (true)
                        {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try
                            {
                                if (txt_key.getText().isEmpty()) throw new Exception("Please, insert an attribute name");

                                String attribute = txt_key.getText();
                                String value = txt_value.getText();

                                switch(networkElement)
                                {
                                    case "node":
                                        netPlan.setNodeAttribute(itemId, attribute, value);
                                        break;

                                    case "link":
                                        netPlan.setLinkAttribute(itemId, attribute, value);
                                        break;

                                    case "demand":
                                        netPlan.setDemandAttribute(itemId, attribute, value);
                                        break;

                                    case "route":
                                        netPlan.setRouteAttribute(itemId, attribute, value);
                                        break;

                                    case "protection segment":
                                        netPlan.setProtectionSegmentAttribute(itemId, attribute, value);
                                        break;

                                    case "SRG":
                                        netPlan.setSRGAttribute(itemId, attribute, value);
                                        break;

                                    default:
                                        throw new RuntimeException("Bad");
                                }

                                break;
                            }
                            catch (Exception ex)
                            {
                                ErrorHandling.addErrorOrException(ex, GUINetworkDesign.class);
                                ErrorHandling.showErrorDialog("Error adding/editing attribute");
                            }
                        }

                        try
                        {
                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            netPlans.set(aux.getFirst(), aux_netPlan);
                            ErrorHandling.addErrorOrException(ex, GUINetworkDesign.class);
                            ErrorHandling.showErrorDialog("Unable to add attribute");
                        }
                    }
                });

                popup.add(addAttribute);

                JMenuItem viewAttributes = new JMenuItem("View/edit all attributes");
                viewAttributes.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        
                        switch(networkElement)
                        {
                            case "node":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.NODE);
                                break;

                            case "link":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.LINK);
                                break;

                            case "demand":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.DEMAND);
                                break;

                            case "route":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.ROUTE);
                                break;

                            case "protection segment":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.SEGMENT);
                                break;

                            case "SRG":
                                AttributesCellEditor.editAttributes(netPlan, AttributesCellEditor.NetworkElement.SRG);
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }
                        
                        callback.updateNetPlanView();
                    }
                });

                popup.add(viewAttributes);

                JMenuItem removeAttribute = new JMenuItem("Remove attribute");

                removeAttribute.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int itemId = table.convertRowIndexToModel(row);

                            String[] attributeList;

                            switch(networkElement)
                            {
                                case "node":
                                    attributeList = StringUtils.toArray(netPlan.getNodeSpecificAttributes(itemId).keySet());
                                    break;

                                case "link":
                                    attributeList = StringUtils.toArray(netPlan.getLinkSpecificAttributes(itemId).keySet());
                                    break;

                                case "demand":
                                    attributeList = StringUtils.toArray(netPlan.getDemandSpecificAttributes(itemId).keySet());
                                    break;

                                case "route":
                                    attributeList = StringUtils.toArray(netPlan.getRouteSpecificAttributes(itemId).keySet());
                                    break;

                                case "protection segment":
                                    attributeList = StringUtils.toArray(netPlan.getProtectionSegmentSpecificAttributes(itemId).keySet());
                                    break;

                                case "SRG":
                                    attributeList = StringUtils.toArray(netPlan.getSRGSpecificAttributes(itemId).keySet());
                                    break;

                                default:
                                    throw new RuntimeException("Bad");
                            }

                            if (attributeList.length == 0) throw new Exception("No attribute to remove");

                            Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute", JOptionPane.QUESTION_MESSAGE, null, attributeList, attributeList[0]);
                            if (out == null) return;

                            String attributeToRemove = out.toString();

                            switch(networkElement)
                            {
                                case "node":
                                    netPlan.removeNodeAttribute(itemId, attributeToRemove);
                                    break;

                                case "link":
                                    netPlan.removeLinkAttribute(itemId, attributeToRemove);
                                    break;

                                case "demand":
                                    netPlan.removeDemandAttribute(itemId, attributeToRemove);
                                    break;

                                case "route":
                                    netPlan.removeRouteAttribute(itemId, attributeToRemove);
                                    break;

                                case "protection segment":
                                    netPlan.removeProtectionSegmentAttribute(itemId, attributeToRemove);
                                    break;

                                case "SRG":
                                    netPlan.removeSRGAttribute(itemId, attributeToRemove);
                                    break;

                                default:
                                    throw new RuntimeException("Bad");
                            }

                            callback.updateNetPlanView();
                        }
                        catch (Throwable ex)
                        {
                            netPlans.set(aux.getFirst(), aux_netPlan);
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute");
                        }
                    }
                });

                popup.add(removeAttribute);
            }

            if (popup.getSubElements().length > 0) popup.addSeparator();

            JMenuItem addAttributeAll = new JMenuItem("Add/edit attribute to all");
            addAttributeAll.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    JTextField txt_key = new JTextField(20);
                    JTextField txt_value = new JTextField(20);

                    JPanel pane = new JPanel();
                    pane.add(new JLabel("Attribute: "));
                    pane.add(txt_key);
                    pane.add(Box.createHorizontalStrut(15));
                    pane.add(new JLabel("Value: "));
                    pane.add(txt_value);

                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();
                    NetPlan aux_netPlan = netPlan.copy();

                    while (true)
                    {
                        int result = JOptionPane.showConfirmDialog(null, pane, "Please enter an attribute name and its value", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                        try
                        {
                            if (txt_key.getText().isEmpty()) throw new Exception("Please, insert an attribute name");

                            String attribute = txt_key.getText();
                            String value = txt_value.getText();

                            switch(networkElement)
                            {
                                case "node":
                                    int N = netPlan.getNumberOfNodes();
                                    for(int nodeId = 0; nodeId < N; nodeId++)
                                        netPlan.setNodeAttribute(nodeId, attribute, value);
                                    break;

                                case "link":
                                    int E = netPlan.getNumberOfLinks();
                                    for(int linkId = 0; linkId < E; linkId++)
                                        netPlan.setLinkAttribute(linkId, attribute, value);
                                    break;

                                case "demand":
                                    int D = netPlan.getNumberOfDemands();
                                    for(int demandId = 0; demandId < D; demandId++)
                                        netPlan.setDemandAttribute(demandId, attribute, value);
                                    break;

                                case "route":
                                    int R = netPlan.getNumberOfRoutes();
                                    for(int routeId = 0; routeId < R; routeId++)
                                        netPlan.setRouteAttribute(routeId, attribute, value);
                                    break;

                                case "protection segment":
                                    int S = netPlan.getNumberOfProtectionSegments();
                                    for(int segmentId = 0; segmentId < S; segmentId++)
                                        netPlan.setProtectionSegmentAttribute(segmentId, attribute, value);
                                    break;

                                case "SRG":
                                    int numSRGs = netPlan.getNumberOfSRGs();
                                    for(int srgId = 0; srgId < numSRGs; srgId++)
                                        netPlan.setSRGAttribute(srgId, attribute, value);
                                    break;

                                default:
                                    throw new RuntimeException("Bad");
                            }

                            break;
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding/editing attribute to all " + networkElement + "s");
                        }
                    }

                    try
                    {
                        callback.updateNetPlanView();
                    }
                    catch (Exception ex)
                    {
                        netPlans.set(aux.getFirst(), aux_netPlan);
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add attribute to all nodes");
                    }
                }
            });

            popup.add(addAttributeAll);

            JMenuItem removeAttributeAll = new JMenuItem("Remove attribute from all " + networkElement + "s");

            removeAttributeAll.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();
                    NetPlan aux_netPlan = netPlan.copy();

                    try
                    {
                        int totalItems;
                        Set<String> attributeSet = new HashSet<String>();

                        switch(networkElement)
                        {
                            case "node":
                                totalItems = netPlan.getNumberOfNodes();
                                for(int nodeId = 0; nodeId < totalItems; nodeId++)
                                    attributeSet.addAll(netPlan.getNodeSpecificAttributes(nodeId).keySet());
                                break;

                            case "link":
                                totalItems = netPlan.getNumberOfLinks();
                                for(int linkId = 0; linkId < totalItems; linkId++)
                                    attributeSet.addAll(netPlan.getLinkSpecificAttributes(linkId).keySet());
                                break;

                            case "demand":
                                totalItems = netPlan.getNumberOfDemands();
                                for(int demandId = 0; demandId < totalItems; demandId++)
                                    attributeSet.addAll(netPlan.getDemandSpecificAttributes(demandId).keySet());
                                break;

                            case "route":
                                totalItems = netPlan.getNumberOfRoutes();
                                for(int routeId = 0; routeId < totalItems; routeId++)
                                    attributeSet.addAll(netPlan.getRouteSpecificAttributes(routeId).keySet());
                                break;

                            case "protection segment":
                                totalItems = netPlan.getNumberOfProtectionSegments();
                                for(int segmentId = 0; segmentId < totalItems; segmentId++)
                                    attributeSet.addAll(netPlan.getProtectionSegmentSpecificAttributes(segmentId).keySet());
                                break;

                            case "SRG":
                                totalItems = netPlan.getNumberOfSRGs();
                                for(int srgId = 0; srgId < totalItems; srgId++)
                                    attributeSet.addAll(netPlan.getSRGSpecificAttributes(srgId).keySet());
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        if (attributeSet.isEmpty()) throw new Exception("No attribute to remove");

                        Object out = JOptionPane.showInputDialog(null, "Please, select an attribute to remove", "Remove attribute from all nodes", JOptionPane.QUESTION_MESSAGE, null, attributeSet.toArray(new String[attributeSet.size()]), attributeSet.iterator().next());
                        if (out == null) return;

                        String attributeToRemove = out.toString();

                        switch(networkElement)
                        {
                            case "node":
                                for(int nodeId = 0; nodeId < totalItems; nodeId++)
                                    netPlan.removeNodeAttribute(nodeId, attributeToRemove);
                                break;

                            case "link":
                                for(int linkId = 0; linkId < totalItems; linkId++)
                                    netPlan.removeLinkAttribute(linkId, attributeToRemove);
                                break;

                            case "demand":
                                for(int demandId = 0; demandId < totalItems; demandId++)
                                    netPlan.removeDemandAttribute(demandId, attributeToRemove);
                                break;

                            case "route":
                                for(int routeId = 0; routeId < totalItems; routeId++)
                                    netPlan.removeRouteAttribute(routeId, attributeToRemove);
                                break;

                            case "protection segment":
                                for(int segmentId = 0; segmentId < totalItems; segmentId++)
                                    netPlan.removeProtectionSegmentAttribute(segmentId, attributeToRemove);
                                break;

                            case "SRG":
                                for(int srgId = 0; srgId < totalItems; srgId++)
                                    netPlan.removeSRGAttribute(srgId, attributeToRemove);
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        callback.updateNetPlanView();
                    }
                    catch (Throwable ex)
                    {
                        netPlans.set(aux.getFirst(), aux_netPlan);
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attribute from all " + networkElement + "s");
                    }
                }
            });

            popup.add(removeAttributeAll);

            JMenuItem removeAttributes = new JMenuItem("Remove all attributes from all " + networkElement + "s");

            removeAttributes.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();
                    NetPlan aux_netPlan = netPlan.copy();

                    try
                    {
                        int totalItems;

                        switch(networkElement)
                        {
                            case "node":
                                totalItems = netPlan.getNumberOfNodes();
                                for(int nodeId = 0; nodeId < totalItems; nodeId++)
                                    netPlan.setNodeAttributes(nodeId, null);
                                break;

                            case "link":
                                totalItems = netPlan.getNumberOfLinks();
                                for(int linkId = 0; linkId < totalItems; linkId++)
                                    netPlan.setLinkAttributes(linkId, null);
                                break;

                            case "demand":
                                totalItems = netPlan.getNumberOfDemands();
                                for(int demandId = 0; demandId < totalItems; demandId++)
                                    netPlan.setDemandAttributes(demandId, null);
                                break;

                            case "route":
                                totalItems = netPlan.getNumberOfRoutes();
                                for(int routeId = 0; routeId < totalItems; routeId++)
                                    netPlan.setRouteAttributes(routeId, null);
                                break;

                            case "protection segment":
                                totalItems = netPlan.getNumberOfProtectionSegments();
                                for(int segmentId = 0; segmentId < totalItems; segmentId++)
                                    netPlan.setProtectionSegmentAttributes(segmentId, null);
                                break;

                            case "SRG":
                                totalItems = netPlan.getNumberOfSRGs();
                                for(int srgId = 0; srgId < totalItems; srgId++)
                                    netPlan.setSRGAttributes(srgId, null);
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        callback.updateNetPlanView();
                    }
                    catch (Throwable ex)
                    {
                        netPlans.set(aux.getFirst(), aux_netPlan);
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Error removing attributes");
                    }
                }
            });

            popup.add(removeAttributes);

            JMenuItem removeItems = new JMenuItem("Remove all " + networkElement + "s");

            removeItems.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                    NetPlan netPlan = aux.getSecond();
                    NetPlan aux_netPlan = netPlan.copy();

                    try
                    {
                        switch(networkElement)
                        {
                            case "node":
                                netPlan.removeAllNodes();
                                topologyPanel.getCanvas().updateTopology(netPlan);
                                
                                break;

                            case "link":
                                netPlan.removeAllLinks();
                                topologyPanel.getCanvas().updateTopology(netPlan);

                                break;

                            case "demand":
                                netPlan.removeAllDemands();
                                break;

                            case "route":
                                netPlan.removeAllRoutes();
                                break;

                            case "protection segment":
                                netPlan.removeAllProtectionSegments();
                                break;

                            case "SRG":
                                netPlan.removeAllSRGs();
                                break;

                            default:
                                throw new RuntimeException("Bad");
                        }

                        callback.updateNetPlanView();
                    }
                    catch (Exception ex)
                    {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to remove all " + networkElement + "s");
                        netPlans.set(aux.getFirst(), aux_netPlan);
                    }
                }
            });

            popup.add(removeItems);
            
            int itemId = row == -1 ? row : table.convertRowIndexToModel(row);
            List<JMenuItem> extraOptions = getExtraOptions(itemId);
            if (!extraOptions.isEmpty())
            {
                popup.addSeparator();
                for(JMenuItem item : extraOptions)
                    popup.add(item);
            }
        }

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private JMenuItem getAddOption()
    {
        JMenuItem addItem;
        
        switch(networkElement)
        {
            case "node":
            case "link":
            case "demand":
            case "route":
            case "protection segment":
            case "SRG":
                addItem = new JMenuItem("Add " + networkElement);
                break;

            default:
                throw new RuntimeException("Bad");
        }

        addItem.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                NetPlan netPlan = aux.getSecond();
                NetPlan aux_netPlan = netPlan.copy();
                int N = netPlan.getNumberOfNodes();

                try
                {
                    switch(networkElement)
                    {
                        case "SRG":
                            netPlan.addSRG(null, null, 8748, 12, null);
                            break;

                        case "node":
                            netPlan.addNode(0, 0, null, null);
                            topologyPanel.getCanvas().updateTopology(netPlan);
                            break;

                        case "link":
                        case "demand":

                            JTextField txt_originNode = new JTextField(5);
                            JTextField txt_destinationNode = new JTextField(5);

                            JPanel pane = new JPanel();
                            pane.add(networkElement.equals("link") ? new JLabel("Origin node: ") : new JLabel("Ingress node: "));
                            pane.add(txt_originNode);
                            pane.add(Box.createHorizontalStrut(15));
                            pane.add(networkElement.equals("link") ? new JLabel("Destination node: ") : new JLabel("Egress node: "));
                            pane.add(txt_destinationNode);

                            while (true)
                            {
                                int result = JOptionPane.showConfirmDialog(null, pane, "Please enter end nodes for the new " + networkElement, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (result != JOptionPane.OK_OPTION) return;

                                try
                                {
                                    int originNode = Integer.parseInt(txt_originNode.getText());
                                    int destinationNode = Integer.parseInt(txt_destinationNode.getText());
                                    if (originNode < 0 || originNode > N - 1)
                                    {
                                        if (networkElement.equals("link"))
                                            throw new Exception(String.format("Origin node must be in range [0,%d]", N - 1));
                                        else
                                            throw new Exception(String.format("Ingress node must be in range [0,%d]", N - 1));
                                    }
                                    if (destinationNode < 0 || destinationNode > N - 1)
                                    {
                                        if (networkElement.equals("link"))
                                            throw new Exception(String.format("Destination node must be in range [0,%d]", N - 1));
                                        else
                                            throw new Exception(String.format("Egress node must be in range [0,%d]", N - 1));
                                    }

                                    if (networkElement.equals("link"))
                                    {
                                        netPlan.addLink(originNode, destinationNode, 0, netPlan.getNodePairPhysicalDistance(originNode, destinationNode), null);
                                        topologyPanel.getCanvas().updateTopology(netPlan);
                                    }
                                    else
                                    {
                                        netPlan.addDemand(originNode, destinationNode, 0, null);
                                    }

                                    break;
                                }
                                catch (Throwable ex)
                                {
                                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding " + networkElement);
                                }
                            }

                            break;
                            
                        case "route":
                            createRouteGUI(topologyPanel, netPlan);
                            break;
                            
                        case "protection segment":
                            createProtectionSegmentGUI(topologyPanel, netPlan);
                            break;

                        default:
                            throw new RuntimeException("Bad");
                    }

                    callback.updateNetPlanView();
                }
                catch (Throwable ex)
                {
                    netPlans.set(aux.getFirst(), aux_netPlan);
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add " + networkElement);
                }
            }
        });

        if (networkElement.equals("link") || networkElement.equals("demand") || networkElement.equals("protection segment"))
        {
            Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
            NetPlan netPlan = aux.getSecond();
            int N = netPlan.getNumberOfNodes();

            if (N < 2) addItem.setEnabled(false);
        }
        else if (networkElement.equals("route"))
        {
            Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
            NetPlan netPlan = aux.getSecond();
            if (!netPlan.hasDemands()) addItem.setEnabled(false);
        }

        return addItem;
    }

    private List<JMenuItem> getExtraOptions(final int itemId)
    {
        List<JMenuItem> options = new LinkedList<JMenuItem>();

        switch(networkElement)
        {
            case "node":
            case "demand":
            case "protection segment":
                break;

            case "route":
                
                if (itemId != -1)
                {
                    JMenuItem viewEditProtectionSegments = new JMenuItem("View/edit backup segment list");
                    viewEditProtectionSegments.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                            NetPlan netPlan = aux.getSecond();
                            NetPlan aux_netPlan = netPlan.copy();

                            try
                            {
                                viewEditBackupSegmentListGUI(callback, topologyPanel, netPlan, itemId);
                            }
                            catch (Exception ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error viewing/editing backup segment list");
                                netPlans.set(aux.getFirst(), aux_netPlan);
                            }
                        }
                    });

                    options.add(viewEditProtectionSegments);
                }
                
                break;
                
            case "SRG":
                JMenuItem mttfValue = new JMenuItem("Set MTTF to all");

                mttfValue.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double mttf;

                        while (true)
                        {
                            String str = JOptionPane.showInputDialog(null, "MTTF (in hours, zero or negative value means no failure)", "Set MTTF to all SRGs", JOptionPane.QUESTION_MESSAGE);
                            if (str == null) return;

                            try
                            {
                                mttf = Double.parseDouble(str);
                                break;
                            }
                            catch (NumberFormatException ex) { ErrorHandling.showErrorDialog("Non-valid MTTF value", "Error setting MTTF value"); }
                            catch (Exception ex) { ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTF"); }
                        }

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int numSRGs = netPlan.getNumberOfSRGs();

                            for (int srgId = 0; srgId < numSRGs; srgId++)
                                netPlan.setSRGMeanTimeToFailInHours(srgId, mttf);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTF to all SRGs");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(mttfValue);

                JMenuItem mttrValue = new JMenuItem("Set MTTR to all");

                mttrValue.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double mttr;

                        while (true)
                        {
                            String str = JOptionPane.showInputDialog(null, "MTTR (in hours)", "Set MTTR to all SRGs", JOptionPane.QUESTION_MESSAGE);
                            if (str == null) return;

                            try
                            {
                                mttr = Double.parseDouble(str);
                                if (mttr <= 0) throw new NumberFormatException();
                                break;
                            }
                            catch (NumberFormatException ex) { ErrorHandling.showErrorDialog("Non-valid MTTR value. Please, introduce a non-zero non-negative number", "Error setting MTTR value"); }
                            catch (Exception ex) { ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting MTTR"); }
                        }

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int numSRGs = netPlan.getNumberOfSRGs();

                            for (int srgId = 0; srgId < numSRGs; srgId++)
                                netPlan.setSRGMeanTimeToRepairInHours(srgId, mttr);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set MTTR to all SRGs");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(mttrValue);

                break;

            case "link":

                JMenuItem caFixValue = new JMenuItem("Set capacity value to all");

                caFixValue.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double C;

                        while (true)
                        {
                            String str = JOptionPane.showInputDialog(null, "Capacity value (in Erlangs)", "Set capacity value to all links", JOptionPane.QUESTION_MESSAGE);
                            if (str == null)
                            {
                                return;
                            }
                            try
                            {
                                C = Double.parseDouble(str);
                                if (C < 0)
                                {
                                    throw new Net2PlanException("Capacity value must be greater or equal than zero");
                                }
                                break;
                            }
                            catch (NumberFormatException ex)
                            {
                                ErrorHandling.showErrorDialog("Non-valid capacity value. Please, introduce a non-negative number", "Error setting capacity value");
                            }
                            catch (Exception ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting capacity value");
                            }
                        }

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int E = netPlan.getNumberOfLinks();

                            for (int linkId = 0; linkId < E; linkId++)
                                netPlan.setLinkCapacityInErlangs(linkId, C);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set capacity value to all links");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(caFixValue);

                JMenuItem lengthToAll = new JMenuItem("Set link length value to all");

                lengthToAll.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double L;

                        while (true)
                        {
                            String str = JOptionPane.showInputDialog(null, "Link length value (in km)", "Set link length value to all links", JOptionPane.QUESTION_MESSAGE);
                            if (str == null)
                            {
                                return;
                            }
                            try
                            {
                                L = Double.parseDouble(str);
                                if (L < 0)
                                {
                                    throw new Net2PlanException("Link length value must be greater or equal than zero");
                                }
                                break;
                            }
                            catch (NumberFormatException ex)
                            {
                                ErrorHandling.showErrorDialog("Non-valid link length value. Please, introduce a non-negative number", "Error setting link length value");
                            }
                            catch (Exception ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting link length value");
                            }
                        }

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int E = netPlan.getNumberOfLinks();

                            for (int linkId = 0; linkId < E; linkId++)
                                netPlan.setLinkLengthInKm(linkId, L);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(lengthToAll);

                JMenuItem lengthToAllEuclidean = new JMenuItem("Set (all) link length to node euclidean distance");

                lengthToAllEuclidean.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int E = netPlan.getNumberOfLinks();

                            for (int linkId = 0; linkId < E; linkId++)
                            {
                                int originNodeId = netPlan.getLinkOriginNode(linkId);
                                int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
                                double euclideanDistance = netPlan.getNodePairPhysicalDistance(originNodeId, destinationNodeId);
                                netPlan.setLinkLengthInKm(linkId, euclideanDistance);
                            }

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to set link length value to all links");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(lengthToAllEuclidean);

                JMenuItem scaleLinkLength = new JMenuItem("Scale (all) link length");

                scaleLinkLength.addActionListener(new ActionListener()
                {

                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double scaleFactor;

                        while (true)
                        {
                            String str = JOptionPane.showInputDialog(null, "(Multiplicative) Scale factor", "Scale (all) link length", JOptionPane.QUESTION_MESSAGE);
                            if (str == null) return;

                            try
                            {
                                scaleFactor = Double.parseDouble(str);
                                if (scaleFactor < 0)
                                {
                                    throw new Net2PlanException("Link length value must be greater or equal than zero");
                                }
                                break;
                            }
                            catch (NumberFormatException ex)
                            {
                                ErrorHandling.showErrorDialog("Non-valid scale value. Please, introduce a non-negative number", "Error setting scale factor");
                            }
                            catch (Exception ex)
                            {
                                ErrorHandling.showErrorDialog(ex.getMessage(), "Error setting scale factor");
                            }
                        }

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            int E = netPlan.getNumberOfLinks();

                            for (int linkId = 0; linkId < E; linkId++)
                                netPlan.setLinkLengthInKm(linkId, netPlan.getLinkLengthInKm(linkId) * scaleFactor);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to scale link length");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }
                    }
                });

                options.add(scaleLinkLength);

                break;

            default:
                throw new RuntimeException("Bad");
        }

        return options;
    }

    private List<JMenuItem> getExtraAddOptions()
    {
        List<JMenuItem> options = new LinkedList<JMenuItem>();

        switch(networkElement)
        {
            case "node":
            case "demand":
            case "route":
            case "protection segment":
            case "link":
                break;

            case "SRG":

                JMenu submenu = new JMenu("Add SRGs from model"); options.add(submenu);

                final JMenuItem onePerNode = new JMenuItem("One SRG per node"); submenu.add(onePerNode);
                final JMenuItem onePerLink = new JMenuItem("One SRG per unidirectional link"); submenu.add(onePerLink);
                final JMenuItem onePerLinkBundle = new JMenuItem("One SRG per unidirectional bundle of links"); submenu.add(onePerLinkBundle);
                final JMenuItem onePerBidiLinkBundle = new JMenuItem("One SRG per bidirectional bundle of links"); submenu.add(onePerBidiLinkBundle);

                ActionListener srgModel = new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        double mttf;
                        double mttr;

                        JTextField txt_mttf = new JTextField(5);
                        JTextField txt_mttr = new JTextField(5);
                        JCheckBox chk_removeExistingSRGs = new JCheckBox();
                        chk_removeExistingSRGs.setSelected(true);

                        JPanel pane = new JPanel(new GridLayout(0,2));
                        pane.add(new JLabel("MTTF (in hours, zero or negative value means no failure): "));
                        pane.add(txt_mttf);
                        pane.add(new JLabel("MTTR (in hours): "));
                        pane.add(txt_mttr);
                        pane.add(new JLabel("Remove existing SRGs: "));
                        pane.add(chk_removeExistingSRGs);

                        while (true)
                        {
                            int result = JOptionPane.showConfirmDialog(null, pane, "Add SRGs from model", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.OK_OPTION) return;

                            try
                            {
                                mttf = Double.parseDouble(txt_mttf.getText());
                                mttr = Double.parseDouble(txt_mttr.getText());
                                if (mttr <= 0) throw new IllegalArgumentException("MTTR must be greater than zero");

                                break;
                            }
                            catch (Throwable ex) { ErrorHandling.showErrorDialog(ex.getMessage(), "Error adding SRGs from model"); }
                        }

                        boolean removeExistingSRGs = chk_removeExistingSRGs.isSelected();

                        Pair<Integer, NetPlan> aux = callback.getCurrentNetPlan();
                        NetPlan netPlan = aux.getSecond();
                        NetPlan aux_netPlan = netPlan.copy();

                        try
                        {
                            if (e.getSource() == onePerNode)
                                SRGUtils.configureSRGs(netPlan, mttf, mttr, SRGUtils.SharedRiskModel.PER_NODE, removeExistingSRGs);
                            else if (e.getSource() == onePerLink)
                                SRGUtils.configureSRGs(netPlan, mttf, mttr, SRGUtils.SharedRiskModel.PER_LINK, removeExistingSRGs);
                            else if (e.getSource() == onePerLinkBundle)
                                SRGUtils.configureSRGs(netPlan, mttf, mttr, SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, removeExistingSRGs);
                            else if (e.getSource() == onePerBidiLinkBundle)
                                SRGUtils.configureSRGs(netPlan, mttf, mttr, SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, removeExistingSRGs);

                            callback.updateNetPlanView();
                        }
                        catch (Exception ex)
                        {
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to add SRGs from model");
                            netPlans.set(aux.getFirst(), aux_netPlan);
                        }

                    }
                };

                onePerNode.addActionListener(srgModel);
                onePerLink.addActionListener(srgModel);
                onePerLinkBundle.addActionListener(srgModel);
                onePerBidiLinkBundle.addActionListener(srgModel);

                break;

            default:
                throw new RuntimeException("Bad");
        }

        return options;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        table.clearSelection();
        topologyPanel.getCanvas().resetPickedState();

        if (SwingUtilities.isRightMouseButton(e) && isEditable)
        {
            doPopup(e);
            return;
        }

        final int row = table.rowAtPoint(e.getPoint());
        if (row == -1) return;

        int itemId = table.convertRowIndexToModel(row);
        showInCanvas(e, itemId);
    }
    
    private static void createRouteGUI(final TopologyPanel topologyPanel, final NetPlan netPlan)
    {
        final int D = netPlan.getNumberOfDemands();
        
        final JComboBox demandSelector = new WiderJComboBox();
        
        final JTextField txt_carriedTrafficInErlangs = new JTextField();
        
        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));
        
        demandSelector.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                StringLabeller item = (StringLabeller) e.getItem();
                int demandId = (Integer) item.getObject();
                
                double h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);
                double r_d = netPlan.getDemandCarriedTrafficInErlangs(demandId);
                txt_carriedTrafficInErlangs.setText(Double.toString(Math.max(0, h_d - r_d)));
                
                seqLinks_cmb.clear();
                seqLinks_pnl.removeAll();

                int ingressNodeId = netPlan.getDemandIngressNode(demandId);
                String ingressNodeName = netPlan.getNodeName(ingressNodeId);
                
                int[] outgoingLinks = netPlan.getNodeOutgoingLinks(ingressNodeId);
                final JComboBox firstLink = new WiderJComboBox();
                for(int linkId : outgoingLinks)
                {
                    int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
                    String destinationNodeName = netPlan.getNodeName(destinationNodeId);
                    firstLink.addItem(StringLabeller.of(linkId, String.format("e%d: n%d (%s) => n%d (%s)", linkId, ingressNodeId, ingressNodeName, destinationNodeId, destinationNodeName)));
                }
                
                firstLink.addItemListener(new ItemListener()
                {
                    @Override
                    public void itemStateChanged(ItemEvent e)
                    {
                        JComboBox me = (JComboBox) e.getSource();
                        Iterator<JComboBox> it = seqLinks_cmb.iterator();
                        while(it.hasNext()) { if (it.next() == me) break; }
                        while(it.hasNext())
                        {
                            JComboBox aux = it.next();
                            seqLinks_pnl.remove(aux);
                            it.remove();
                        }
                        
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();
                
                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                setMaxSize(firstLink);

                seqLinks_cmb.add(firstLink);
                seqLinks_pnl.add(firstLink);
                
                JPanel pane = new JPanel(new FlowLayout());
                JButton addLink_btn = new JButton("Add new link");
                addLink_btn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        int linkId = (Integer) ((StringLabeller) seqLinks_cmb.get(seqLinks_cmb.size() - 1).getSelectedItem()).getObject();
                        int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
                        String destinationNodeName = netPlan.getNodeName(destinationNodeId);
                        
                        int[] outgoingLinks = netPlan.getNodeOutgoingLinks(destinationNodeId);
                        if (outgoingLinks.length == 0)
                        {
                            ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                            return;
                        }
                        
                        final JComboBox newLink = new WiderJComboBox();
                        for(int nextLinkId : outgoingLinks)
                        {
                            int nextDestinationNodeId = netPlan.getLinkDestinationNode(nextLinkId);
                            String nextDestinationNodeName = netPlan.getNodeName(nextDestinationNodeId);
                            newLink.addItem(StringLabeller.of(nextLinkId, String.format("e%d: n%d (%s) => n%d (%s)", nextLinkId, destinationNodeId, destinationNodeName, nextDestinationNodeId, nextDestinationNodeName)));
                        }
                
                        newLink.addItemListener(new ItemListener()
                        {
                            @Override
                            public void itemStateChanged(ItemEvent e)
                            {
                                JComboBox me = (JComboBox) e.getSource();
                                Iterator<JComboBox> it = seqLinks_cmb.iterator();
                                while(it.hasNext()) { if (it.next() == me) break; }
                                while(it.hasNext())
                                {
                                    JComboBox aux = it.next();
                                    seqLinks_pnl.remove(aux);
                                    it.remove();
                                }

                                seqLinks_pnl.revalidate();

                                IntArrayList seqLinks = new IntArrayList();
                                for(JComboBox link : seqLinks_cmb)
                                    seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                                seqLinks.trimToSize();

                                topologyPanel.getCanvas().showRoute(seqLinks.elements());
                            }
                        });
                
                        setMaxSize(newLink);

                        seqLinks_cmb.add(newLink);
                        seqLinks_pnl.add(newLink, seqLinks_pnl.getComponentCount() - 1);
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();

                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                pane.add(addLink_btn);
                
                JButton removeLink_btn = new JButton("Remove last link");
                removeLink_btn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        if (seqLinks_cmb.size() < 2)
                        {
                            ErrorHandling.showErrorDialog("Initial link cannot be removed", "Error");
                            return;
                        }
                        
                        JComboBox cmb = seqLinks_cmb.get(seqLinks_cmb.size() - 1);
                        seqLinks_cmb.remove(cmb);
                        seqLinks_pnl.remove(cmb);
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();

                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                pane.add(removeLink_btn);
                seqLinks_pnl.add(pane);

                seqLinks_pnl.revalidate();

                IntArrayList seqLinks = new IntArrayList();
                for(JComboBox link : seqLinks_cmb)
                    seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                seqLinks.trimToSize();

                topologyPanel.getCanvas().showRoute(seqLinks.elements());
            }
        });
        
        for(int demandId = 0; demandId < D; demandId++)
        {
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);
            
            final String ingressNodeName = netPlan.getNodeName(ingressNodeId);
            final String egressNodeName = netPlan.getNodeName(egressNodeId);
            
            final int[] outgoingLinks = netPlan.getNodeOutgoingLinks(ingressNodeId);
            if (outgoingLinks.length == 0) continue;
            
            String demandLabel = "Demand " + demandId;
            demandLabel += ": n" + ingressNodeId;
            if (!ingressNodeName.isEmpty()) demandLabel += " (" + ingressNodeName + ")";
            demandLabel += " => n" + egressNodeId;
            if (!egressNodeName.isEmpty()) demandLabel += " (" + egressNodeName + ")";
            
            double h_d = netPlan.getDemandOfferedTrafficInErlangs(demandId);
            double r_d = netPlan.getDemandCarriedTrafficInErlangs(demandId);
            
            demandLabel +=", offered traffic = " + h_d + " E";
            demandLabel +=", carried traffic = " + r_d + " E";
            
            demandSelector.addItem(StringLabeller.of(demandId, demandLabel));
        }
        
        if (demandSelector.getItemCount() == 0) throw new Net2PlanException("Bad - No node has outgoing links");
        
        demandSelector.setSelectedIndex(0);
        
        final JScrollPane scrollPane = new JScrollPane(seqLinks_pnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Sequence of links"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);
        
        final JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][grow][]"));
        pane.add(new JLabel("Demand"));
        pane.add(demandSelector, "growx, wrap, wmin 50");
        pane.add(scrollPane, "grow, spanx 2, wrap");
        pane.add(new JLabel("Carried traffic (in Erlangs)"));
        pane.add(txt_carriedTrafficInErlangs, "grow");
        pane.setPreferredSize(new Dimension(400, 400));
        
        while(true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new route", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;
            
            int demandId = (Integer) ((StringLabeller) demandSelector.getSelectedItem()).getObject();
        
            double carriedTrafficInErlangs;
            
            try
            {
                carriedTrafficInErlangs = Double.parseDouble(txt_carriedTrafficInErlangs.getText());
                if (carriedTrafficInErlangs < 0) throw new RuntimeException();
            }
            catch(Throwable e)
            {
                ErrorHandling.showErrorDialog("Carried traffic must be a non-negative number", "Error adding route");
                continue;
            }
            
            IntArrayList seqLinks = new IntArrayList();
            for(JComboBox link : seqLinks_cmb)
                seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
            
            seqLinks.trimToSize();
            
            try
            {
                netPlan.addRoute(demandId, carriedTrafficInErlangs, seqLinks.elements(), null, null);
            }
            catch(Throwable e)
            {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding route");
                continue;
            }
            
            break;
        }        
        
        topologyPanel.getCanvas().resetPickedState();
    }
    
    private static void setMaxSize(JComponent c) 
    { 
        final Dimension max = c.getMaximumSize(); 
        final Dimension pref = c.getPreferredSize(); 
        
        max.height = pref.height; 
        c.setMaximumSize(max); 
    }    
    
    private static void createProtectionSegmentGUI(final TopologyPanel topologyPanel, final NetPlan netPlan)
    {
        final int N = netPlan.getNumberOfNodes();
        
        final JComboBox nodeSelector = new WiderJComboBox();
        
        final List<JComboBox> seqLinks_cmb = new LinkedList<JComboBox>();
        final JPanel seqLinks_pnl = new JPanel();
        seqLinks_pnl.setLayout(new BoxLayout(seqLinks_pnl, BoxLayout.Y_AXIS));
        
        nodeSelector.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                StringLabeller item = (StringLabeller) e.getItem();
                
                int nodeId = (Integer) item.getObject();
                String nodeName = netPlan.getNodeName(nodeId);
                
                seqLinks_cmb.clear();
                seqLinks_pnl.removeAll();

                int[] outgoingLinks = netPlan.getNodeOutgoingLinks(nodeId);
                final JComboBox firstLink = new WiderJComboBox();
                for(int linkId : outgoingLinks)
                {
                    int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
                    String destinationNodeName = netPlan.getNodeName(nodeId);
                    firstLink.addItem(StringLabeller.of(linkId, String.format("e%d: n%d (%s) => n%d (%s)", linkId, nodeId, nodeName, destinationNodeId, destinationNodeName)));
                }
                
                firstLink.addItemListener(new ItemListener()
                {
                    @Override
                    public void itemStateChanged(ItemEvent e)
                    {
                        JComboBox me = (JComboBox) e.getSource();
                        Iterator<JComboBox> it = seqLinks_cmb.iterator();
                        while(it.hasNext()) { if (it.next() == me) break; }
                        while(it.hasNext())
                        {
                            JComboBox aux = it.next();
                            seqLinks_pnl.remove(aux);
                            it.remove();
                        }
                        
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();
                
                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                setMaxSize(firstLink);

                seqLinks_cmb.add(firstLink);
                seqLinks_pnl.add(firstLink);
                
                JPanel pane = new JPanel(new FlowLayout());
                JButton addLink_btn = new JButton("Add new link");
                addLink_btn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        int linkId = (Integer) ((StringLabeller) seqLinks_cmb.get(seqLinks_cmb.size() - 1).getSelectedItem()).getObject();
                        int destinationNodeId = netPlan.getLinkDestinationNode(linkId);
                        String destinationNodeName = netPlan.getNodeName(destinationNodeId);
                        
                        int[] outgoingLinks = netPlan.getNodeOutgoingLinks(destinationNodeId);
                        if (outgoingLinks.length == 0)
                        {
                            ErrorHandling.showErrorDialog("Last node has no outgoing links", "Error");
                            return;
                        }
                        
                        final JComboBox newLink = new WiderJComboBox();
                        for(int nextLinkId : outgoingLinks)
                        {
                            int nextDestinationNodeId = netPlan.getLinkDestinationNode(nextLinkId);
                            String nextDestinationNodeName = netPlan.getNodeName(nextDestinationNodeId);
                            newLink.addItem(StringLabeller.of(nextLinkId, String.format("e%d: n%d (%s) => n%d (%s)", nextLinkId, destinationNodeId, destinationNodeName, nextDestinationNodeId, nextDestinationNodeName)));
                        }
                
                        newLink.addItemListener(new ItemListener()
                        {
                            @Override
                            public void itemStateChanged(ItemEvent e)
                            {
                                JComboBox me = (JComboBox) e.getSource();
                                Iterator<JComboBox> it = seqLinks_cmb.iterator();
                                while(it.hasNext()) { if (it.next() == me) break; }
                                while(it.hasNext())
                                {
                                    JComboBox aux = it.next();
                                    seqLinks_pnl.remove(aux);
                                    it.remove();
                                }

                                seqLinks_pnl.revalidate();

                                IntArrayList seqLinks = new IntArrayList();
                                for(JComboBox link : seqLinks_cmb)
                                    seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                                seqLinks.trimToSize();

                                topologyPanel.getCanvas().showRoute(seqLinks.elements());
                            }
                        });
                
                        setMaxSize(newLink);

                        seqLinks_cmb.add(newLink);
                        seqLinks_pnl.add(newLink, seqLinks_pnl.getComponentCount() - 1);
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();

                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                pane.add(addLink_btn);
                
                JButton removeLink_btn = new JButton("Remove last link");
                removeLink_btn.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        if (seqLinks_cmb.size() < 2)
                        {
                            ErrorHandling.showErrorDialog("Initial link cannot be removed", "Error");
                            return;
                        }
                        
                        JComboBox cmb = seqLinks_cmb.get(seqLinks_cmb.size() - 1);
                        seqLinks_cmb.remove(cmb);
                        seqLinks_pnl.remove(cmb);
                        seqLinks_pnl.revalidate();

                        IntArrayList seqLinks = new IntArrayList();
                        for(JComboBox link : seqLinks_cmb)
                            seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                        seqLinks.trimToSize();

                        topologyPanel.getCanvas().showRoute(seqLinks.elements());
                    }
                });
                
                pane.add(removeLink_btn);
                seqLinks_pnl.add(pane);

                seqLinks_pnl.revalidate();

                IntArrayList seqLinks = new IntArrayList();
                for(JComboBox link : seqLinks_cmb)
                    seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
                seqLinks.trimToSize();

                topologyPanel.getCanvas().showRoute(seqLinks.elements());
            }
        });
        
        for(int nodeId = 0; nodeId < N; nodeId++)
        {
            final String nodeName = netPlan.getNodeName(nodeId);
            final int[] outgoingLinks = netPlan.getNodeOutgoingLinks(nodeId);
            if (outgoingLinks.length == 0) continue;
            
            String nodeLabel = "Node " + nodeId;
            if (!nodeName.isEmpty()) nodeLabel += " (" + nodeName + ")";
            
            nodeSelector.addItem(StringLabeller.of(nodeId, nodeLabel));
        }
        
        if (nodeSelector.getItemCount() == 0) throw new Net2PlanException("Bad - No node has outgoing links");
        
        nodeSelector.setSelectedIndex(0);
        
        final JTextField txt_reservedBandwidthInErlangs = new JTextField();
        
        final JScrollPane scrollPane = new JScrollPane(seqLinks_pnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Sequence of links"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);
        
        final JPanel pane = new JPanel(new MigLayout("fill", "[][grow]", "[][grow][]"));
        pane.add(new JLabel("Origin node"));
        pane.add(nodeSelector, "grow, wrap, wmin 50");
        pane.add(scrollPane, "grow, spanx 2, wrap");
        pane.add(new JLabel("Reserved bandwidth (in Erlangs)"));
        pane.add(txt_reservedBandwidthInErlangs, "grow");
        pane.setPreferredSize(new Dimension(400, 400));
        
        while(true)
        {
            int result = JOptionPane.showConfirmDialog(null, pane, "Add new protection segment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) break;
            
            double reservedBandwidthInErlangs;
            
            try
            {
                reservedBandwidthInErlangs = Double.parseDouble(txt_reservedBandwidthInErlangs.getText());
                if (reservedBandwidthInErlangs < 0) throw new RuntimeException();
            }
            catch(Throwable e)
            {
                ErrorHandling.showErrorDialog("Reserved bandwidth must be a non-negative number", "Error adding protection segment");
                continue;
            }
            
            IntArrayList seqLinks = new IntArrayList();
            for(JComboBox link : seqLinks_cmb)
                seqLinks.add((Integer) ((StringLabeller) link.getSelectedItem()).getObject());
            
            seqLinks.trimToSize();
            
            try
            {
                netPlan.addProtectionSegment(seqLinks.elements(), reservedBandwidthInErlangs, null);
            }
            catch(Throwable e)
            {
                ErrorHandling.showErrorDialog(e.getMessage(), "Error adding protection segment");
                continue;
            }
            
            break;
        }        
        
        topologyPanel.getCanvas().resetPickedState();
    }
    
    private static void viewEditBackupSegmentListGUI(final INetworkCallback callback, final TopologyPanel topologyPanel, final NetPlan netPlan, final int routeId)
    {
        Set<Integer> candidateSegmentIds = new TreeSet<Integer>();
        Set<Integer> currentSegmentIds = new TreeSet<Integer>();
        
        final int[] seqLinks = netPlan.getRouteSequenceOfLinks(routeId);
        
        int S = netPlan.getNumberOfProtectionSegments();
        for(int segmentId = 0; segmentId < S; segmentId++)
        {
            boolean isApplicable = netPlan.checkProtectionSegmentMergeabilityToRoute(routeId, segmentId);
            if (!isApplicable) continue;
            
            candidateSegmentIds.add(segmentId);
        }
        
        if (candidateSegmentIds.isEmpty()) throw new Net2PlanException("No segment can be applied to this route");
        
        currentSegmentIds.addAll(IntUtils.toList(netPlan.getRouteBackupSegmentList(routeId)));
        candidateSegmentIds.removeAll(currentSegmentIds);
        
        final JComboBox segmentSelector = new WiderJComboBox();
        
        final DefaultTableModel model = new ClassAwareTableModel(new Object[1][6], new String[] {"Id", "Seq. links", "Seq. nodes", "Reserved BW (E)", "", ""})
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return columnIndex == 4 || columnIndex == 5 ? true : false;
            }
        };
        final JTable table = new AdvancedJTable(model);
        table.setEnabled(false);
        
        final JPanel addSegment_pnl = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        JButton addSegment_btn = new JButton("Add");
        addSegment_btn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object selectedItem = segmentSelector.getSelectedItem();
                int segmentId = (Integer) ((StringLabeller) selectedItem).getObject();
                netPlan.addProtectionSegmentToRouteBackupSegmentList(segmentId, routeId);
                callback.updateNetPlanView();
                
                segmentSelector.removeItem(selectedItem);
                if (segmentSelector.getItemCount() == 0) addSegment_pnl.setVisible(false);

                int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
                int[] segmentSeqNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
                double reservedBandwidthInErlangs = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
                
                if (!table.isEnabled()) model.removeRow(0);
                model.addRow(new Object[] {segmentId, IntUtils.join(segmentSeqLinks, " => "), IntUtils.join(segmentSeqNodes, " => "), reservedBandwidthInErlangs, "Remove", "View"});
                
                table.setEnabled(true);
            }
        });
        
        JButton viewSegment_btn1 = new JButton("View");
        viewSegment_btn1.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Object selectedItem = segmentSelector.getSelectedItem();
                int segmentId = (Integer) ((StringLabeller) selectedItem).getObject();
                
                int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
                topologyPanel.getCanvas().showRoutes(seqLinks, segmentSeqLinks);
            }
        });
        
        addSegment_pnl.add(segmentSelector, "growx, wmin 50");
        addSegment_pnl.add(addSegment_btn);
        addSegment_pnl.add(viewSegment_btn1);
        
        Action delete = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    JTable table = (JTable)e.getSource();
                    int modelRow = Integer.valueOf(e.getActionCommand());

                    int segmentId = (Integer) table.getModel().getValueAt(modelRow, 0);
                    netPlan.removeProtectionSegmentFromRouteBackupSegmentList(segmentId, routeId);
                    callback.updateNetPlanView();

                    int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
                    int[] segmentSeqNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
                    double reservedBandwidthInErlangs = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);

                    String segmentLabel = "Protection segment " + segmentId;
                    segmentLabel += ": seq. links = " + IntUtils.join(segmentSeqLinks, " => ");
                    segmentLabel += ", seq. nodes = " + IntUtils.join(segmentSeqNodes, " => ");
                    segmentLabel += ", reserved bandwidth = " + reservedBandwidthInErlangs + " E";

                    segmentSelector.addItem(StringLabeller.of(segmentId, segmentLabel));

                    ((DefaultTableModel)table.getModel()).removeRow(modelRow);

                    table.setEnabled(true);
                    
                    if (table.getModel().getRowCount() == 0)
                    {
                        ((DefaultTableModel)table.getModel()).addRow(new Object[6]);
                        table.setEnabled(false);
                    }
                }
                catch(Throwable e1) { }
            }
        };

        Action view = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    JTable table = (JTable)e.getSource();
                    int modelRow = Integer.valueOf(e.getActionCommand());
                
                    int segmentId = (Integer) table.getModel().getValueAt(modelRow, 0);
                    int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
                    topologyPanel.getCanvas().showRoutes(seqLinks, segmentSeqLinks);
                }
                catch(Throwable e1) { }
            }
        };

        new ButtonColumn(table, delete, 4);
        new ButtonColumn(table, view, 5);
        
        final JScrollPane scrollPane = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Current backup segment list"));
        scrollPane.setAlignmentY(JScrollPane.TOP_ALIGNMENT);

        final JDialog dialog = new JDialog();
        dialog.setLayout(new BorderLayout());
        dialog.add(addSegment_pnl, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        for(int segmentId : candidateSegmentIds)
        {
            int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
            int[] segmentSeqNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
            double reservedBandwidthInErlangs = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
            
            String segmentLabel = "Protection segment " + segmentId;
            segmentLabel += ": seq. links = " + IntUtils.join(segmentSeqLinks, " => ");
            segmentLabel += ", seq. nodes = " + IntUtils.join(segmentSeqNodes, " => ");
            segmentLabel += ", reserved bandwidth = " + reservedBandwidthInErlangs + " E";
            
            segmentSelector.addItem(StringLabeller.of(segmentId, segmentLabel));
        }
        
        if (segmentSelector.getItemCount() == 0)
        {
            addSegment_pnl.setVisible(false);
        }
        else
        {
            segmentSelector.setSelectedIndex(0);
        }
        
        if (!currentSegmentIds.isEmpty())
        {
            model.removeRow(0);
            
            for(int segmentId : currentSegmentIds)
            {
                int[] segmentSeqLinks = netPlan.getProtectionSegmentSequenceOfLinks(segmentId);
                int[] segmentSeqNodes = netPlan.getProtectionSegmentSequenceOfNodes(segmentId);
                double reservedBandwidthInErlangs = netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId);
                
                model.addRow(new Object[] {segmentId, IntUtils.join(segmentSeqLinks, " => "), IntUtils.join(segmentSeqNodes, " => "), reservedBandwidthInErlangs, "Remove", "View"});
            }
            
            table.setEnabled(true);
        }
        
        table.setDefaultRenderer(Boolean.class, new MyRenderer());
        table.setDefaultRenderer(Double.class, new MyRenderer());
        table.setDefaultRenderer(Object.class, new MyRenderer());
        table.setDefaultRenderer(Float.class, new MyRenderer());
        table.setDefaultRenderer(Integer.class, new MyRenderer());
        table.setDefaultRenderer(String.class, new MyRenderer());
        
        double x_p = netPlan.getRouteCarriedTrafficInErlangs(routeId);
        dialog.setTitle("View/edit backup segment list for route " + routeId + " (" + x_p + " E)");
        ((JComponent) dialog.getContentPane()).registerKeyboardAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
	dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
                
        topologyPanel.getCanvas().resetPickedState();
    }
    
private static class MyRenderer extends DefaultTableCellRenderer {
public Component getTableCellRendererComponent(JTable table,
Object value,
boolean isSelected,
boolean hasFocus,
int row,
int column) {
return super.getTableCellRendererComponent(table, value,
isSelected,
false, // Always false
row, column);
}
}    
}