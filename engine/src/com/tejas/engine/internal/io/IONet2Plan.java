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

package com.tejas.engine.internal.io;

import com.tejas.engine.internal.io.__IOImporterExporter;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Pair;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class IONet2Plan extends __IOImporterExporter
{
    @Override
    public NetPlan loadNetworkDesign(File file)
    {
	return new NetPlan(file);
    }

    @Override
    public void saveNetworkDesign(File file, NetPlan netPlan)
    {
	netPlan.saveToFile(file);
    }

    @Override
    public Pair<String, Set<String>> getDefaultExtensions()
    {
	Set<String> extensions = new HashSet<String>();
	extensions.add("n2p");

	return Pair.of("Net2Plan file (*.n2p)", extensions);
    }
}
