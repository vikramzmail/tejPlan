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

import com.tejas.engine.internal.io.IONet2Plan;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.utils.Pair;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import javax.swing.filechooser.FileFilter;

/**
 *
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public abstract class __IOImporterExporter extends FileFilter
{
    public enum Features { LOAD_DESIGN, LOAD_DESIGN_ML, SAVE_DESIGN, SAVE_DESIGN_ML, LOAD_DEMANDS, SAVE_DEMANDS };

    public final static Set<Class<? extends __IOImporterExporter>> implementingClasses;

    static
    {
	implementingClasses = new LinkedHashSet<Class<? extends __IOImporterExporter>>();
	implementingClasses.add(IONet2Plan.class);
    }

    @Override
    public final String getDescription()
    {
	return getDefaultExtensions().getFirst();
    }

    @Override
    public final boolean accept(File f)
    {
	if (f == null) return false;
	if (f.isDirectory()) return true;

	String fileName = f.getName();
	int i = fileName.lastIndexOf('.');
	if (i > 0 && i < fileName.length() - 1)
	{
	    Set<String> extensions = getDefaultExtensions().getSecond();
	    String desiredExtension = fileName.substring(i+1).toLowerCase(Locale.ENGLISH);

	    for (String extension : extensions)
		if (desiredExtension.equalsIgnoreCase(extension))
		    return true;
	}

	return false;
    }

    public abstract NetPlan loadNetworkDesign(File file);

    public abstract void saveNetworkDesign(File file, NetPlan netPlan);

    public abstract Pair<String, Set<String>> getDefaultExtensions();

    public List<NetPlan> loadNetworkDesignMultiLayer(File[] files)
    {
	List<NetPlan> netPlans = new LinkedList<NetPlan>();
	for(File file : files)
	    netPlans.add(loadNetworkDesign(file));

	return netPlans;
    }

    public void saveNetworkDesignMultiLayer(File[] files, List<NetPlan> netPlans)
    {
	ListIterator<NetPlan> it = netPlans.listIterator();

	while(it.hasNext())
	{
	    int index = it.nextIndex();
	    saveNetworkDesign(files[index], it.next());
	}
    }
}
