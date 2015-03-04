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

package com.tejas.engine.interfaces.resilienceSimulation;

import com.tejas.engine.interfaces.resilienceSimulation.ProvisioningAction;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Provides a set of actions to be returned by provisioning algorithms. These actions are:</p>
 *
 * <ul>
 * <li>Add/modify/remove route</li>
 * <li>Remove all routes</li>
 * <li>Add/modify/remove protection segment</li>
 * <li>Remove all protection segments</li>
 * <li>Add/remove protection segment to/from route backup list</li>
 * <li>Remove all protection segments from route backup list</li>
 * </ul>
 *
 * <p>Although the <code>ProvisioningAction</code> class is common for all actions, its meaning (i.e.
 * action type) depends on the static method used to get an instance. Take a look
 * on the description of the static methods to obtain more information.</p>
 * 
 * <p><b>Important</b>: Actions don't take effect within the algorithm. This means,
 * for example, that if you remove a route, methods like <code>netState.getNumberOfRoutes()</code> will return
 * the previous value including that route, instead of the current according
 * to the actions. Network state is actually modified by the kernel after
 * the execution of the algorithm, so users should deal with current network state
 * by their own.</p>
 *
 * <p><b>Important</b>: Identifiers for new elements (i.e. route, protection segment) 
 * follow an incremental scheme, and no index is reused when some element is removed. 
 * The (first) next identifier of each element can be accessed via <code>netState.getNextXXXId())</code> method.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ProvisioningAction
{
    /**
     * Type of action.
     *
     * @since 0.2.0
     */
    public enum ActionType
    {
	/**
	 * Add route.
	 *
	 * @since 0.2.3
	 */
        ADD_ROUTE,
        
	/**
	 * Modify route.
	 *
	 * @since 0.2.3
	 */
        MODIFY_ROUTE,
        
	/**
	 * Remove route.
	 *
	 * @since 0.2.3
	 */
        REMOVE_ROUTE,
        
	/**
	 * Remove all existing routes.
	 *
	 * @since 0.2.3
	 */
        REMOVE_ALL_ROUTES,

	/**
	 * Add a protection segment.
	 *
	 * @since 0.2.3
	 */
        ADD_PROTECTION_SEGMENT,

	/**
	 * Remove all existing routes.
	 *
	 * @since 0.2.3
	 */
        ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST,

	/**
	 * Remove a protection segment from a route backup list.
	 *
	 * @since 0.2.3
	 */
        REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST,

	/**
	 * Remove all protection segments from a route backup list.
	 *
	 * @since 0.2.3
	 */
        REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST,

	/**
	 * Remove protection segment.
	 *
	 * @since 0.2.3
	 */
        REMOVE_PROTECTION_SEGMENT,

	/**
	 * Remove all existing protection segments.
	 *
	 * @since 0.2.3
	 */
        REMOVE_ALL_PROTECTION_SEGMENTS;
    };

    private ActionType type;
    
    protected double addSegmentReservedBandwidthInErlangs;
    protected int[] addSegmentSeqLinks;
    protected Map<String, String> addSegmentAttributes;
    
    protected int addRouteDemandId;
    protected double addRouteTrafficVolumeInErlangs;
    protected int[] addRouteSeqLinks;
    protected long[] addRouteBackupSegments;
    protected Map<String, String> addRouteAttributes;
    
    protected long modifyRouteId;
    protected double modifyRouteTrafficVolumeInErlangs;
    protected long[] modifyRouteSeqLinksAndSegments;
    protected Map<String, String> modifyRouteAttributes;

    protected long removeSegmentId;
    protected long removeRouteId;
    
    protected long addSegmentToRouteSegmentId;
    protected long addSegmentToRouteRouteId;

    protected long removeSegmentFromRouteSegmentId;
    protected long removeSegmentFromRouteRouteId;
    
    protected long removeAllSegmentsFromRouteId;
    
    /**
     * Default constructor. Users must generate new actions via factory methods.
     *
     * @since 0.2.3
     */
    private ProvisioningAction(ActionType type) { reset(); this.type = type; }
    
    /**
     * Returns the action type.
     *
     * @return Action type
     * @since 0.2.0
     */
    public ActionType getActionType() { return type; }
    
    private void reset()
    {
        addSegmentReservedBandwidthInErlangs = -1;
        addSegmentSeqLinks = new int[0];
        addSegmentAttributes = new HashMap<String, String>();
    
        addRouteDemandId = -1;
        addRouteTrafficVolumeInErlangs = -1;
        addRouteSeqLinks = new int[0];
        addRouteBackupSegments = new long[0];
        addRouteAttributes = new HashMap<String, String>();
    
        modifyRouteId = -1;
        modifyRouteTrafficVolumeInErlangs = -1;
        modifyRouteSeqLinksAndSegments = null;
        modifyRouteAttributes = null;

        removeSegmentId = -1;
        removeRouteId = -1;
    
        addSegmentToRouteSegmentId = -1;
        addSegmentToRouteRouteId = -1;

        removeSegmentFromRouteSegmentId = -1;
        removeSegmentFromRouteRouteId = -1;
    
        removeAllSegmentsFromRouteId = -1;
    }
    
    /**
     * Adds a new protection segment.
     * 
     * @param reservedBandwidthInErlangs Reserved bandwidth in Erlangs
     * @param seqLinks Sequence of links
     * @param segmentAttributes Attributes (null means no attributes)
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction addProtectionSegment(double reservedBandwidthInErlangs, int[] seqLinks, Map<String, String> segmentAttributes)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.ADD_PROTECTION_SEGMENT);
        action.addSegmentReservedBandwidthInErlangs = reservedBandwidthInErlangs;
        action.addSegmentSeqLinks = IntUtils.copy(seqLinks);
        if (segmentAttributes != null) action.addSegmentAttributes = new HashMap<String, String>(segmentAttributes);
        
        return action;
    }
    
    /**
     * Adds a protection segment to the backup list of a given route. Note that 
     * end nodes of the protection segment must be within the route.
     * 
     * @param segmentId Protection segment identifier
     * @param routeId Route identifier
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction addProtectionSegmentToRouteBackupSegmentList(long segmentId, long routeId)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST);
        action.addSegmentToRouteSegmentId = segmentId;
        action.addSegmentToRouteRouteId = routeId;
        
        return action;
    }

    /**
     * Adds a route.
     * 
     * @param demandId Demand identifier
     * @param trafficVolumeInErlangs Traffic volume in Erlangs
     * @param seqLinks Sequence of links (protection segments cannot be used when adding routes)
     * @param backupSegmentList Backup segment list (null means empty)
     * @param routeAttributes Attributes (null means no attributes)
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction addRoute(int demandId, double trafficVolumeInErlangs, int[] seqLinks, long[] backupSegmentList, Map<String, String> routeAttributes)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.ADD_ROUTE);
        action.addRouteDemandId = demandId;
        action.addRouteTrafficVolumeInErlangs = trafficVolumeInErlangs;
        action.addRouteSeqLinks = IntUtils.copy(seqLinks);
        if (backupSegmentList != null) action.addRouteBackupSegments = LongUtils.copy(backupSegmentList);
        if (routeAttributes != null) action.addRouteAttributes = new HashMap<String, String>(routeAttributes);
        
        return action;
    }
    
    /**
     * Modifies a route. If backup segments become out of the route, kernel will throw an error.
     * 
     * @param routeId Route identifier
     * @param trafficVolumeInErlangs Traffic volume in Erlangs (-1 means no change)
     * @param seqLinksAndSegments Sequence of links and protection segments (null means no change). Protection segments are denoted with an identifier equal to <i>-1 - protection segment identifier</i>
     * @param routeAttributes Route attributes (null means no change)
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction modifyRoute(long routeId, double trafficVolumeInErlangs, long[] seqLinksAndSegments, Map<String, String> routeAttributes)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.MODIFY_ROUTE);
        action.modifyRouteId = routeId;
        action.modifyRouteTrafficVolumeInErlangs = trafficVolumeInErlangs;
        if (seqLinksAndSegments != null) action.modifyRouteSeqLinksAndSegments = LongUtils.copy(seqLinksAndSegments);
        if (routeAttributes != null) action.modifyRouteAttributes = new HashMap<String, String>(routeAttributes);
        
        return action;
    }

    /**
     * <p>Removes all protection segments associated to a given route.</p>
     * 
     * <p><b>Important</b>: If any of the segments is being used, the kernel will throw an error.</p>
     * 
     * @param routeId Route identifier
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction removeAllProtectionSegmentsFromRouteBackupSegmentList(long routeId)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST);
        action.removeAllSegmentsFromRouteId = routeId;
        
        return action;
    }
    
    /**
     * Removes all routes.
     * 
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction removeAllRoutes()
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.REMOVE_ALL_ROUTES);
        
        return action;
    }
    
    /**
     * <p>Removes a protection segment. Automatically is removed from the backup list 
     * of each associated route.</p>
     * 
     * <p><b>Important</b>: If it is being used, the kernel will throw an error.</p>
     * 
     * @param segmentId Protection segment identifier
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction removeProtectionSegment(long segmentId)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.REMOVE_PROTECTION_SEGMENT);
        action.removeSegmentId = segmentId;
        
        return action;
    }

    /**
     * Removes a protection segment from the backup list of a given route.
     * 
     * <p><b>Important</b>: If it is being used, the kernel will throw an error.</p>
     * 
     * @param segmentId Protection segment identifier
     * @param routeId Route identifier
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction removeProtectionSegmentFromRouteBackupSegmentList(long segmentId, long routeId)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST);
        action.removeSegmentFromRouteSegmentId = segmentId;
        action.removeSegmentFromRouteRouteId = routeId;
        
        return action;
    }
    
    /**
     * Removes a route.
     * 
     * @param routeId Route identifier
     * @return Provisioning action
     * @since 0.2.3
     */
    public static ProvisioningAction removeRoute(long routeId)
    {
        ProvisioningAction action = new ProvisioningAction(ActionType.REMOVE_ROUTE);
        action.removeRouteId = routeId;
        
        return action;
    }
    
    @Override
    public String toString()
    {
        if (type == ActionType.ADD_PROTECTION_SEGMENT)
        {
            double reservedBandwidthInErlangs = addSegmentReservedBandwidthInErlangs;
            int[] seqLinks = addSegmentSeqLinks;
            Map<String, String> segmentAttributes = addSegmentAttributes;
            
            return String.format("Add protection segment (reserved bandwidth = %.3f E, seq. links = %s, attributes = %s)", reservedBandwidthInErlangs, IntUtils.join(seqLinks, ", "), StringUtils.mapToString(segmentAttributes, "=", ", "));
        }
        else if (type == ActionType.ADD_PROTECTION_SEGMENT_TO_ROUTE_BACKUP_LIST)
        {
            long segmentId = addSegmentToRouteSegmentId;
            long routeId = addSegmentToRouteRouteId;
            
            return String.format("Add protection segment %d to backup list for route %d", segmentId, routeId);
        }
        else if (type == ActionType.ADD_ROUTE)
        {
            int demandId = addRouteDemandId;
            double carriedTraffic = addRouteTrafficVolumeInErlangs;
            int[] seqLinks = addRouteSeqLinks;
            long[] backupSegmentList = addRouteBackupSegments;
            Map<String, String> routeAttributes = addRouteAttributes;
            
            return String.format("Add route for demand %d (carried traffic = %.3f E, seq. links = %s, backup segment list = %s, attributes = %s)", demandId, carriedTraffic, convertSeqLinksToString(seqLinks), LongUtils.join(backupSegmentList, ", "), StringUtils.mapToString(routeAttributes, "=", ", "));
        }
        else if (type == ActionType.MODIFY_ROUTE)
        {
            long routeId = modifyRouteId;
            double carriedTraffic = modifyRouteTrafficVolumeInErlangs;
            long[] seqLinksAndSegments = modifyRouteSeqLinksAndSegments;
            Map<String, String> routeAttributes = modifyRouteAttributes;
            
            String aux_carriedTraffic = carriedTraffic == -1 ? "no change" : Double.toString(carriedTraffic) + " E";
            String aux_seqLinksAndSegments = seqLinksAndSegments == null ? "no change" : convertSeqLinksAndSegmentsToString(seqLinksAndSegments);
            String aux_routeAttributes = routeAttributes == null ? "no change" : StringUtils.mapToString(routeAttributes, "=", ", ");
            
            return String.format("Modify route %d (carried traffic = %s, seq. links and segments = %s, attributes = %s)", routeId, aux_carriedTraffic, aux_seqLinksAndSegments, aux_routeAttributes);
        }
        else if (type == ActionType.REMOVE_ALL_PROTECTION_SEGMENTS)
        {
            return "Remove all protection segments";
        }
        else if (type == ActionType.REMOVE_ALL_PROTECTION_SEGMENTS_FROM_ROUTE_BACKUP_LIST)
        {
            return "Remove all protection segments from backup list for route " + removeAllSegmentsFromRouteId;
        }
        else if (type == ActionType.REMOVE_ALL_ROUTES)
        {
            return "Remove all routes";
        }
        else if (type == ActionType.REMOVE_PROTECTION_SEGMENT)
        {
            return "Remove protection segment " + removeSegmentId;
        }
        else if (type == ActionType.REMOVE_PROTECTION_SEGMENT_FROM_ROUTE_BACKUP_LIST)
        {
            long segmentId = removeSegmentFromRouteSegmentId;
            long routeId = removeSegmentFromRouteRouteId;
            
            return String.format("Remove protection segment %d from backup list for route %d", segmentId, routeId);
        }
        else if (type == ActionType.REMOVE_ROUTE)
        {
            return "Remove route " + removeRouteId;
        }
        else
        {
            throw new RuntimeException("Bad - Unknown action type");
	}
    }
    
    private static String convertSeqLinksToString(int[] seqLinks)
    {
        return convertSeqLinksAndSegmentsToString(IntUtils.toLongArray(seqLinks));
    }

    private static String convertSeqLinksAndSegmentsToString(long[] seqLinksAndSegments)
    {
        StringBuilder aux = new StringBuilder();
        for(long linkId : seqLinksAndSegments)
        {
            if (aux.length() != 0) aux.append(", ");
            if (linkId < 0) aux.append("s").append(-1 - linkId);
            else aux.append("e").append(linkId);
        }

        return aux.toString();
    }
}
