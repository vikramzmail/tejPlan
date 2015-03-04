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

import com.tejas.engine.utils.ActionTableCellEditor;
import com.tejas.engine.utils.AdvancedJTable;
import com.tejas.engine.utils.ClassAwareTableModel;
import com.tejas.engine.utils.FixedColumnDecorator;
import com.tejas.engine.utils.ParamValueTable;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.ErrorHandling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

/**
 *
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class AttributesCellEditor extends ActionTableCellEditor
{
    public static enum NetworkElementType { NETWORK, NODE, LINK, DEMAND, ROUTE, SEGMENT, SRG };

    private DefaultTableModel model;
    private NetworkElementType type;
    private NetPlan netPlan;
    private int itemId;
    private static final String[] header = {"Attribute", "Value"};

    /**
     * Default constructor.
     * 
     * @param editor Table cell editor
     * @since 0.2.0
     */
    public AttributesCellEditor(TableCellEditor editor)
    {
	super(editor);

	type = null;
	netPlan = null;
    }

    private void updateData(DefaultTableModel model)
    {
	Map<String, String> aux;

	switch (type)
	{
	    case NETWORK:
		aux = netPlan.getNetworkAttributes();
		break;

	    case NODE:
		aux = netPlan.getNodeSpecificAttributes(itemId);
		break;

	    case LINK:
		aux = netPlan.getLinkSpecificAttributes(itemId);
		break;

	    case DEMAND:
		aux = netPlan.getDemandSpecificAttributes(itemId);
		break;

	    case ROUTE:
		aux = netPlan.getRouteSpecificAttributes(itemId);
		break;

	    case SEGMENT:
		aux = netPlan.getProtectionSegmentSpecificAttributes(itemId);
		break;
                
            case SRG:
                aux = netPlan.getSRGSpecificAttributes(itemId);
                break;

	    default:
		throw new IllegalArgumentException("Invalid network element type");
	}

	int numParams = aux.size();

	String[][] data = new String[numParams > 0 ? numParams : 1][2];

	int paramId = 0;
	for(Entry<String, String> entry : new TreeMap<String, String>(aux).entrySet())
	{
	    data[paramId][0] = entry.getKey();
	    data[paramId][1] = entry.getValue();
	    paramId++;
	}

	model.setDataVector(data, header);
    }

    /**
     *
     * @param netPlan
     */
    public void setnetPlan(NetPlan netPlan)
    {
	this.netPlan = netPlan;
    }

    /**
     *
     * @param type
     */
    public void setNetworkElementType(NetworkElementType type)
    {
	this.type = type;
    }

    /**
     *
     * @param table
     * @param row
     * @param column
     */
    @Override
    protected void editCell(JTable table, int row, int column)
    {
	if (type == null)
	{
	    throw new IllegalArgumentException("A network element type (NETWORK, NODE, LINK, ...) must be specified");
	}
	if (netPlan == null)
	{
	    throw new IllegalArgumentException("A network structure must be provided");
	}

	itemId = table.convertRowIndexToModel(row);
	if (itemId == -1)
	{
	    return;
	}

	try
	{
	    String dialogHeader;

	    switch (type)
	    {
		case NETWORK:
		    dialogHeader = "Network attributes";
		    break;

		case NODE:
		    dialogHeader = String.format("Attributes from node %d", itemId);
		    break;

		case LINK:
		    dialogHeader = String.format("Attributes from link %d", itemId);
		    break;

		case DEMAND:
		    dialogHeader = String.format("Attributes from demand %d", itemId);
		    break;

		case ROUTE:
		    dialogHeader = String.format("Attributes from route %d", itemId);
		    break;

		case SEGMENT:
		    dialogHeader = String.format("Attributes from segment %d", itemId);
		    break;
                    
                case SRG:
                    dialogHeader = String.format("Attributes from segment %d", itemId);

		default:
		    throw new IllegalArgumentException("Invalid network element type");
	    }

	    Object [][] data = new Object[1][2];
	    model = new ClassAwareTableModel(data, header)
	    {

		@Override
		public boolean isCellEditable(int row, int col)
		{
		    return true;
		}

		@Override
		public void setValueAt(Object value, int row, int col)
		{
                    String oldValue = getValueAt(row, col).toString();
                    String newValue = value.toString();

                    if (newValue.equals(oldValue)) return;
                    
		    super.setValueAt(newValue, row, col);
		}
	    };

	    updateData(model);

	    final JTable paramTable = new AdvancedJTable(model);
	    JOptionPane.showMessageDialog(null, new JScrollPane(paramTable), dialogHeader, JOptionPane.PLAIN_MESSAGE);
	}
        catch (Throwable ex)
	{
	    ErrorHandling.showErrorDialog(ex.getMessage(), "Error editing node attributes");
	}
    }
    
    public static void main(String[] args)
    {
        NetPlan netPlan = new NetPlan(new File("NSFNet_N14_E42_complete.n2p"));
        
        editAttributes(netPlan, NetworkElement.NODE);
    }
    
    public static void editAttributes(final NetPlan netPlan, final NetworkElementType type, final int itemId)
    {
        String[] columnNames = {"Attribute", "Value"};
        
        DefaultTableModel model = new ClassAwareTableModel(new Object[1][2], columnNames)
        {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 1; }

            @Override
            public void setValueAt(Object value, int row, int col)
            {
                String oldValue = getValueAt(row, col).toString();
                String newValue = value.toString();

                if (newValue.equals(oldValue)) return;
                
                if (col == 0)
                {
                    super.setValueAt(newValue, row, col);
                    return;
                }
                
                String key = getValueAt(row, 0).toString();
                
                try
                {
                    switch (type)
                    {
                        case NETWORK:
                            if (newValue.isEmpty()) netPlan.removeNetworkAttribute(key);
                            else netPlan.setNetworkAttribute(key, newValue);
                            break;

                        case NODE:
                            if (newValue.isEmpty()) netPlan.removeNodeAttribute(itemId, key);
                            else netPlan.setNodeAttribute(itemId, key, newValue);
                            break;

                        case LINK:
                            if (newValue.isEmpty()) netPlan.removeLinkAttribute(itemId, key);
                            else netPlan.setLinkAttribute(itemId, key, newValue);
                            break;

                        case DEMAND:
                            if (newValue.isEmpty()) netPlan.removeDemandAttribute(itemId, key);
                            else netPlan.setDemandAttribute(itemId, key, newValue);
                            break;

                        case ROUTE:
                            if (newValue.isEmpty()) netPlan.removeRouteAttribute(itemId, key);
                            else netPlan.setRouteAttribute(itemId, key, newValue);
                            break;

                        case SEGMENT:
                            if (newValue.isEmpty()) netPlan.removeProtectionSegmentAttribute(itemId, key);
                            else netPlan.setProtectionSegmentAttribute(itemId, key, newValue);
                            break;

                        case SRG:
                            if (newValue.isEmpty()) netPlan.removeSRGAttribute(itemId, key);
                            else netPlan.setSRGAttribute(itemId, key, newValue);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid network element type");
                    }

                    super.setValueAt(newValue, row, col);
                }
                catch(Throwable e)
                {
                }
            }
        };

        ParamValueTable attributes = new ParamValueTable(model, columnNames);
        attributes.setAutoCreateRowSorter(true);
        
	Map<String, String> aux;
        String dialogHeader;

	switch (type)
	{
	    case NETWORK:
		aux = netPlan.getNetworkAttributes();
                dialogHeader = String.format("Network attributes", itemId);
		break;

	    case NODE:
		aux = netPlan.getNodeSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from node %d", itemId);
		break;

	    case LINK:
		aux = netPlan.getLinkSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from link %d", itemId);
		break;

	    case DEMAND:
		aux = netPlan.getDemandSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from demand %d", itemId);
		break;

	    case ROUTE:
		aux = netPlan.getRouteSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from route %d", itemId);
		break;

	    case SEGMENT:
		aux = netPlan.getProtectionSegmentSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from protection segment %d", itemId);
		break;
                
            case SRG:
                aux = netPlan.getSRGSpecificAttributes(itemId);
                dialogHeader = String.format("Attributes from SRG %d", itemId);
                break;

	    default:
		throw new IllegalArgumentException("Invalid network element type");
	}
        
        if (!aux.isEmpty()) attributes.setData(aux);
        
        JOptionPane.showMessageDialog(null, new JScrollPane(attributes), dialogHeader, JOptionPane.PLAIN_MESSAGE);
    }
    
    public static enum NetworkElement { NODE, LINK, DEMAND, ROUTE, SEGMENT, SRG };
        
    public static void editAttributes(final NetPlan netPlan, final NetworkElement type)
    {
        String dialogHeader;
        Object[][] data;
        String[] columnNames;
        Set<String> attributes;

        switch (type)
        {
            case NODE:
                int N = netPlan.getNumberOfNodes();
                if (N == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[N+1];
                columnNames[0] = "Attribute";
                for(int nodeId = 0; nodeId < N; nodeId++)
                {
                    columnNames[nodeId+1] = "Node " + nodeId;
                    attributes.addAll(netPlan.getNodeSpecificAttributes(nodeId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][N+1];
                }
                else
                {
                    data = new Object[attributes.size()][N+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int nodeId = 0; nodeId < N; nodeId++)
                        {
                            data[itemId][nodeId+1] = netPlan.getNodeAttribute(nodeId, attribute);
                            if (data[itemId][nodeId+1] == null) data[itemId][nodeId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit node attributes";

                break;

            case LINK:
                int E = netPlan.getNumberOfLinks();
                if (E == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[E+1];
                columnNames[0] = "Attribute";
                for(int linkId = 0; linkId < E; linkId++)
                {
                    columnNames[linkId+1] = "Link " + linkId;
                    attributes.addAll(netPlan.getLinkSpecificAttributes(linkId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][E+1];
                }
                else
                {
                    data = new Object[attributes.size()][E+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int linkId = 0; linkId < E; linkId++)
                        {
                            data[itemId][linkId+1] = netPlan.getLinkAttribute(linkId, attribute);
                            if (data[itemId][linkId+1] == null) data[itemId][linkId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit link attributes";
                break;

            case DEMAND:
                int D = netPlan.getNumberOfDemands();
                if (D == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[D+1];
                columnNames[0] = "Attribute";
                for(int demandId = 0; demandId < D; demandId++)
                {
                    columnNames[demandId] = "Demand " + demandId;
                    attributes.addAll(netPlan.getDemandSpecificAttributes(demandId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][D+1];
                }
                else
                {
                    data = new Object[attributes.size()][D+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int demandId = 0; demandId < D; demandId++)
                        {
                            data[itemId][demandId+1] = netPlan.getDemandAttribute(demandId, attribute);
                            if (data[itemId][demandId+1] == null) data[itemId][demandId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit demand attributes";
                break;

            case ROUTE:
                int R = netPlan.getNumberOfRoutes();
                if (R == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[R+1];
                columnNames[0] = "Attribute";
                for(int routeId = 0; routeId < R; routeId++)
                {
                    columnNames[routeId+1] = "Route " + routeId;
                    attributes.addAll(netPlan.getRouteSpecificAttributes(routeId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][R+1];
                }
                else
                {
                    data = new Object[attributes.size()][R+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int routeId = 0; routeId < R; routeId++)
                        {
                            data[itemId][routeId+1] = netPlan.getRouteAttribute(routeId, attribute);
                            if (data[itemId][routeId+1] == null) data[itemId][routeId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit route attributes";
                break;

            case SEGMENT:
                int S = netPlan.getNumberOfProtectionSegments();
                if (S == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[S+1];
                columnNames[0] = "Attribute";
                for(int segmentId = 0; segmentId < S; segmentId++)
                {
                    columnNames[segmentId+1] = "Segment " + segmentId;
                    attributes.addAll(netPlan.getProtectionSegmentSpecificAttributes(segmentId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][S+1];
                }
                else
                {
                    data = new Object[attributes.size()][S+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int segmentId = 0; segmentId < S; segmentId++)
                        {
                            data[itemId][segmentId+1] = netPlan.getProtectionSegmentAttribute(segmentId, attribute);
                            if (data[itemId][segmentId+1] == null) data[itemId][segmentId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit protection segment attributes";
                break;

            case SRG:
                int numSRGs = netPlan.getNumberOfSRGs();
                if (numSRGs == 0) return;

                attributes = new HashSet<String>();
                columnNames = new String[numSRGs+1];
                columnNames[0] = "Attribute";
                for(int srgId = 0; srgId < numSRGs; srgId++)
                {
                    columnNames[srgId+1] = "SRG " + srgId;
                    attributes.addAll(netPlan.getSRGSpecificAttributes(srgId).keySet());
                }

                if (attributes.isEmpty())
                {
                    data = new Object[1][numSRGs+1];
                }
                else
                {
                    data = new Object[attributes.size()][numSRGs+1];

                    int itemId = 0;
                    Iterator<String> it = attributes.iterator();
                    while(it.hasNext())
                    {
                        String attribute = it.next();
                        data[itemId][0] = attribute;
                        for (int srgId = 0; srgId < numSRGs; srgId++)
                        {
                            data[itemId][srgId+1] = netPlan.getSRGAttribute(srgId, attribute);
                            if (data[itemId][srgId+1] == null) data[itemId][srgId+1] = "";
                        }

                        itemId++;
                    }
                }

                dialogHeader = "Edit SRG attributes";
                break;

            default:
                throw new IllegalArgumentException("Invalid network element type");
        }

        DefaultTableModel model = new ClassAwareTableModel(data, columnNames)
        {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return columnIndex == 0 ? false : true;
            }

            @Override
            public void setValueAt(Object value, int row, int col)
            {
                String oldValue = getValueAt(row, col).toString();
                String newValue = value.toString();

                if (newValue.equals(oldValue)) return;

                if (col == 0)
                {
                    super.setValueAt(newValue, row, col);
                    return;
                }

                int itemId = col - 1;
                String key = getValueAt(row, 0).toString();

                try
                {
                    switch (type)
                    {
                        case NODE:
                            if (newValue.isEmpty()) netPlan.removeNodeAttribute(itemId, key);
                            else netPlan.setNodeAttribute(itemId, key, newValue);
                            break;

                        case LINK:
                            if (newValue.isEmpty()) netPlan.removeLinkAttribute(itemId, key);
                            else netPlan.setLinkAttribute(itemId, key, newValue);
                            break;

                        case DEMAND:
                            if (newValue.isEmpty()) netPlan.removeDemandAttribute(itemId, key);
                            else netPlan.setDemandAttribute(itemId, key, newValue);
                            break;

                        case ROUTE:
                            if (newValue.isEmpty()) netPlan.removeRouteAttribute(itemId, key);
                            else netPlan.setRouteAttribute(itemId, key, newValue);
                            break;

                        case SEGMENT:
                            if (newValue.isEmpty()) netPlan.removeProtectionSegmentAttribute(itemId, key);
                            else netPlan.setProtectionSegmentAttribute(itemId, key, newValue);
                            break;

                        case SRG:
                            if (newValue.isEmpty()) netPlan.removeSRGAttribute(itemId, key);
                            else netPlan.setSRGAttribute(itemId, key, newValue);
                            break;

                        default:
                            throw new IllegalArgumentException("Invalid network element type");
                    }

                    super.setValueAt(newValue, row, col);
                }
                catch(Throwable e)
                {
                }
            }
        };

        JTable table = new AdvancedJTable(model);
        table.addMouseListener(new PopupAttributeMenu(table, attributes));
        
        JScrollPane pane = new JScrollPane(table);
        new FixedColumnDecorator(1, pane);

        JOptionPane.showMessageDialog(null, pane, dialogHeader, JOptionPane.PLAIN_MESSAGE);
    }
    
    private static class PopupAttributeMenu extends MouseAdapter
    {
        private final JTable table;
        private final Set<String> attributes;

        public PopupAttributeMenu(JTable table, Set<String> attributes)
        {
            this.table = table;
            this.attributes = attributes;
        }

        private boolean isTableEmpty()
        {
            return (table.getModel().getValueAt(0, 0) instanceof String) ? false : true;
        }

        private void doPopup(MouseEvent e)
        {
            final int row = table.rowAtPoint(e.getPoint());

            JPopupMenu popup = new JPopupMenu();

            JMenuItem addAttribute = new JMenuItem("Add attribute");
            addAttribute.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String attributeName;
                    
                    while(true)
                    {
                        attributeName = JOptionPane.showInputDialog("Please enter new attribute name: ");
                        if (attributeName == null) return;
                        
                        try
                        {
                            if (attributes.contains(attributeName))
                                throw new RuntimeException("Attribute '" + attributeName + "' already exists");
                            
                            StringUtils.checkAttributeName(attributeName);
                        }
                        catch(Throwable ex)
                        {
                            ErrorHandling.showWarningDialog(ex.getMessage(), "Error adding attribute");
                            continue;
                        }
                        
                        break;
                    }
                    
                    int numColumns = table.getModel().getColumnCount();
                    String[] newRow = new String[numColumns];
                    Arrays.fill(newRow, "");
                    
                    newRow[0] = attributeName;
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    model.addRow(newRow);
                }
            });
            
            popup.add(addAttribute);
            
            if (!isTableEmpty() && row != -1)
            {
                JMenuItem removeAttribute = new JMenuItem("Remove attribute");
                removeAttribute.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        int actualRow = table.convertRowIndexToModel(row);
                        String attributeName = (String) model.getValueAt(actualRow, 0);
                        
                        int rc = JOptionPane.showConfirmDialog(null, "Are you sure?", "Removing attribute '" + attributeName + "'", JOptionPane.YES_NO_OPTION);
                        if (rc != JOptionPane.YES_OPTION) return;
                        
                        int numColumns = model.getColumnCount();
                        for(int columnId = 1; columnId < numColumns; columnId++)
                            model.setValueAt("", actualRow, columnId);
                    }
                });

                JMenuItem setValueToAll = new JMenuItem("Set value attribute for all elements");
                setValueToAll.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        int actualRow = table.convertRowIndexToModel(row);
                        String attributeName = (String) model.getValueAt(actualRow, 0);
                        
                        String attributeValue;

                        while(true)
                        {
                            attributeValue = JOptionPane.showInputDialog("Please enter new value for attribute '" + attributeName + "': ");
                            if (attributeValue == null) return;

                            break;
                        }
                        
                        int numColumns = model.getColumnCount();
                        for(int columnId = 1; columnId < numColumns; columnId++)
                            model.setValueAt(attributeValue, actualRow, columnId);
                    }
                });

                popup.add(removeAttribute);
                popup.add(setValueToAll);
            }
            
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            table.clearSelection();

            if (SwingUtilities.isRightMouseButton(e)) doPopup(e);
        }
    }
}
