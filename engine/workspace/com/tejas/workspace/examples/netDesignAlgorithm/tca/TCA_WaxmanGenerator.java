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

package com.tejas.workspace.examples.netDesignAlgorithm.tca;

import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Triple;

import java.awt.geom.Point2D;
import java.util.*;

/** <p>Algorithm for topology and capacity assignment problems which generates a random network topology according to [<a href='#reference_1'>1</a>].</p>
 *
 * <p>The Waxman's generator is a geographic model for the growth of a network. In this model nodes are uniformly distributed in a given area and links are added according to probabilities that depend on the distances between the nodes. The probability to have a link between nodes <i>i</i> and <i>j</i> is given by:</p>
 *
 * <center><p><i>P</i>(<i>i,j</i>)=<i>&alpha;</i>*<i>exp</i>(-<i>d</i>/[<i>&beta;</i>*<i>d<sub>max</sub></i>])</i></p></center>
 *
 * <p>where 0&lt;<i>&alpha;</i>, <i>&beta;</i>&le;1, <i>d</i> is the distance from <i>i</i> to <i>j</i>, and <i>d<sub>max</sub></i> is the maximum distance between any node pair. An increase in the parameter <i>&alpha;</i> increases the probability of edges between any nodes in the network, while an increase in <i>&beta;</i> yields a larger ratio of long links to short links.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.0, March 2013
 * @see <a name='reference_1'></a><code>[1] B.M. Waxman, "Routing of multipoint connections", <i>IEEE Journal on Selected Areas in Communications</i>, vol. 6, no. 9, pp. 1617-1622, 198</code>
 */
public class TCA_WaxmanGenerator implements IAlgorithm
{
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		Random r = new Random();

		int N = Integer.parseInt(algorithmParameters.get("N"));
		double alpha = Double.parseDouble(algorithmParameters.get("alpha"));
		double beta = Double.parseDouble(algorithmParameters.get("beta"));
		double xmax = Double.parseDouble(algorithmParameters.get("xmax"));
		double xmin = Double.parseDouble(algorithmParameters.get("xmin"));
		double ymax = Double.parseDouble(algorithmParameters.get("ymax"));
		double ymin = Double.parseDouble(algorithmParameters.get("ymin"));
		double linkCapacities = Double.parseDouble(algorithmParameters.get("linkCapacities"));

		netPlan.reset();

		Point2D[] nodeXYPositionTable = new Point2D.Double[N];

		// Generate node XY position table
		for (int nodeId = 0; nodeId < N; nodeId++)
		{
			nodeXYPositionTable[nodeId] = new Point2D.Double(xmin + (xmax - xmin) * r.nextDouble(), ymin + (ymax - ymin) * r.nextDouble());
			netPlan.addNode(nodeXYPositionTable[nodeId].getX(), nodeXYPositionTable[nodeId].getY(), "Node " + nodeId, new HashMap<String, String>());
		}

		double dist_max = -Double.MAX_VALUE;
		for (int originNodeId = 0; originNodeId < N; originNodeId++)
		{
			for (int destinationNodeId = originNodeId + 1; destinationNodeId < N; destinationNodeId++)
			{
				double dist = nodeXYPositionTable[originNodeId].distance(nodeXYPositionTable[destinationNodeId]);

				if (dist > dist_max) dist_max = dist;
			}
		}

		// Generate a directed link between each node pair with probability p = alpha * exp(-distance/(beta * max_distance)
		for (int originNodeId = 0; originNodeId < N; originNodeId++)
		{
			for (int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
			{
				if (originNodeId == destinationNodeId) continue;

				double dist = nodeXYPositionTable[originNodeId].distance(nodeXYPositionTable[destinationNodeId]);
				double p = alpha * Math.exp(-dist / (beta * dist_max));

				if (r.nextDouble() < p) netPlan.addLink(originNodeId, destinationNodeId, linkCapacities, dist, new HashMap<String, String>());
			}
		}

		return "Ok!";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("N", "30", "Number of nodes"));
		algorithmParameters.add(Triple.of("alpha", "0.4", "'alpha' factor"));
		algorithmParameters.add(Triple.of("beta", "0.4", "'beta' factor"));
		algorithmParameters.add(Triple.of("xmax", "100", "Right endpoint for the x-axis"));
		algorithmParameters.add(Triple.of("xmin", "0", "Left endpoint for the x-axis"));
		algorithmParameters.add(Triple.of("ymax", "100", "Upper endpoint for the y-axis"));
		algorithmParameters.add(Triple.of("ymin", "0", "Lower endpoint for the y-axis"));
		algorithmParameters.add(Triple.of("linkCapacities", "100", "The capacities to set in the links"));
		return algorithmParameters;
	}

	@Override
	public String getDescription()
	{
		String NEWLINE = String.format("%n");

		StringBuilder aux = new StringBuilder();
		aux.append("This algorithm implements the random network topology generator introduced in Waxman (1988). The Waxman's generator is a geographic model for the growth of a network. In this model nodes are uniformly distributed in a given area and links are added according to probabilities that depend on the distances between the nodes. The probability to have a link between nodes i and j is given by:");
		aux.append(NEWLINE);
		aux.append(NEWLINE);
		aux.append("P(i, j) = alpha * exp(-d/(beta * d_max)");
		aux.append(NEWLINE);
		aux.append(NEWLINE);
		aux.append("where 0<alpha, beta<=1, d is the distance from i to j, and d_max is the maximum distance between any two nodes. An increase in the parameter alpha increases the probability of edges between any nodes in the network, while an increase in beta yields a larger ratio of long links to short links.");

		return aux.toString();
	}
}
