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

import com.tejas.engine.utils.FileChooserConfirmOverwrite;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.SystemUtils;
import com.tejas.engine.internal.io.__IOImporterExporter;

import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class FileChooserNetworkDesign extends FileChooserConfirmOverwrite
{
    private static final long serialVersionUID = 1L;

    public enum TYPE { NETWORK_DESIGN, DEMANDS };

    private TYPE type;
    private boolean finishConfiguration;

    public FileChooserNetworkDesign(String defaultPath, TYPE type)
    {
        this(new File(defaultPath), type);
    }
    
    public FileChooserNetworkDesign(File defaultPath, TYPE type)
    {
	super(defaultPath);

	finishConfiguration = false;

	this.type = type;

	boolean isPredefinedFilter = false;

	Set<Class<? extends __IOImporterExporter>> classes = __IOImporterExporter.implementingClasses;
	for(Class<? extends __IOImporterExporter> _class : classes)
	{
	    try
	    {
		__IOImporterExporter filter = _class.newInstance();
                for(FileFilter existingFilter : super.getChoosableFileFilters()) super.removeChoosableFileFilter(existingFilter);
		super.addChoosableFileFilter(filter);

		if (!isPredefinedFilter)
		{
		    super.setFileFilter(filter);
		    isPredefinedFilter = true;
		}
	    }
	    catch(Throwable e)
	    {
		e.printStackTrace();
		//throw new RuntimeException(e);
	    }
	}

	super.setAcceptAllFileFilterUsed(false);
	super.setFileSelectionMode(JFileChooser.FILES_ONLY);

	finishConfiguration = true;
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter)
    {
	if (finishConfiguration)
	    throw new UnsupportedOperationException("Unsupported operation");
	else
	    super.addChoosableFileFilter(filter);
    }

    @Override
    public void setAcceptAllFileFilterUsed(boolean b)
    {
	if (finishConfiguration)
	    throw new UnsupportedOperationException("Unsupported operation");
	else
	    super.setAcceptAllFileFilterUsed(b);
    }

    @Override
    public void setFileSelectionMode(int mode)
    {
	if (finishConfiguration)
	    throw new UnsupportedOperationException("Unsupported operation");
	else
	    super.setFileSelectionMode(mode);
    }

    private __IOImporterExporter getCurrentIOFilter()
    {
	FileFilter currentFilter = getFileFilter();
	if (!(currentFilter instanceof __IOImporterExporter))
	    throw new RuntimeException("Bad filter");

	return (__IOImporterExporter) currentFilter;
    }

    public NetPlan getNetPlan()
    {
	__IOImporterExporter importer = getCurrentIOFilter();
	NetPlan netPlan = importer.loadNetworkDesign(getSelectedFile());
	return netPlan;
    }

    public void saveNetPlans(List<NetPlan> netPlans)
    {
        try
        {
            __IOImporterExporter exporter = getCurrentIOFilter();

            File[] files = new File[netPlans.size()];

            File file = getSelectedFile();
            String fileName = file.getName();

            int pos = fileName.lastIndexOf(".");
            String fileNameWithoutExtension = pos == -1 ? fileName : fileName.substring(0, pos);
            String extension = pos == -1 ? "" : fileName.substring(pos+1);

            ListIterator<NetPlan> it = netPlans.listIterator();
            while(it.hasNext())
            {
                int layerId = it.nextIndex();
                NetPlan aux = it.next();

                String layerName = aux.getNetworkName();
                if (layerName == null) layerName = "layer" + layerId;

                String layerFileName = fileNameWithoutExtension + "-" + layerName;

                if (extension.isEmpty())
                    files[layerId] = new File(file.getAbsoluteFile().getParentFile().getCanonicalPath() + SystemUtils.getDirectorySeparator() + layerFileName);
                else
                    files[layerId] = new File(file.getAbsoluteFile().getParentFile().getCanonicalPath() + SystemUtils.getDirectorySeparator() + layerFileName + "." + extension);
            }

            exporter.saveNetworkDesignMultiLayer(files, netPlans);
        }
        catch(Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    public void saveNetPlan(NetPlan netPlan)
    {
	__IOImporterExporter exporter = getCurrentIOFilter();
	exporter.saveNetworkDesign(getSelectedFile(), netPlan);
    }
}
