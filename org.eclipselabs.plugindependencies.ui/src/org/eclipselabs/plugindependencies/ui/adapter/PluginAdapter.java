/*******************************************************************************
 * Copyright (c) 2015 Oliver Brösamle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Oliver Brösamle - initial API and implementation and/or initial documentation
 *    Andrey Loskutov <loskutov@gmx.de> - review, cleanup and bugfixes
 *******************************************************************************/
package org.eclipselabs.plugindependencies.ui.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipselabs.plugindependencies.core.Plugin;
import org.eclipselabs.plugindependencies.ui.view.TreePlugin;

/**
 * @author obroesam
 *
 */
public class PluginAdapter implements IPropertySource {
    private final Plugin plugin;

    public PluginAdapter(TreePlugin treePlugin) {
        this.plugin = treePlugin.getPlugin();
    }

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<>();

        PropertyDescriptor name = new PropertyDescriptor("SymbolicName", "SymbolicName");
        name.setCategory("General");
        PropertyDescriptor version = new PropertyDescriptor("Version", "Version");
        version.setCategory("General");
        PropertyDescriptor path = new PropertyDescriptor("Path", "Path");
        path.setCategory("General");
        PropertyDescriptor log = new PropertyDescriptor("Log", "Log");
        log.setCategory("General");
        log.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof List<?>) {
                    List<?> logList = (List<?>) element;
                    StringBuilder out = new StringBuilder();
                    for (Object logEntry : logList) {
                        out.append(logEntry + "\n");
                    }
                    return out.toString();
                }
                return "";
            }
        });

        PropertyDescriptor reqPlugins = new PropertyDescriptor("RequiredPlugins",
                "Required Plugins");
        reqPlugins.setCategory("Requirements");
        PropertyDescriptor reqPackages = new PropertyDescriptor("RequiredPackages",
                "Required Packages");
        reqPackages.setCategory("Requirements");
        PropertyDescriptor resPlugins = new PropertyDescriptor("ResolvedPlugins",
                "Resolved Plugins");
        resPlugins.setCategory("Resolution");
        PropertyDescriptor resPackages = new PropertyDescriptor("ImportedPackages",
                "Imported Packages");
        resPackages.setCategory("Resolution");

        PropertyDescriptor expPackages = new PropertyDescriptor("ExportedPackages",
                "Exported Packages");

        PropertyDescriptor requiredBy = new PropertyDescriptor("RequiredBy",
                "Required By");

        PropertyDescriptor includedIn = new PropertyDescriptor("IncludedIn",
                "Included In");

        Collections.addAll(list, name, version, path, log, reqPlugins, reqPackages,
                resPackages, resPlugins, expPackages, requiredBy, includedIn);

        return list.toArray(new IPropertyDescriptor[list.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (id.equals("SymbolicName")) {
            return plugin.getName();
        }
        if (id.equals("Version")) {
            return plugin.getVersion();
        }
        if (id.equals("Path")) {
            return plugin.getPath();
        }
        if (id.equals("Log")) {
            return plugin.getLog();
        }
        if (id.equals("RequiredPlugins")) {
            return new IPropertySourceList(plugin.getRequiredPlugins());
        }
        if (id.equals("RequiredPackages")) {
            return new IPropertySourceList(plugin.getRequiredPackages());
        }
        if (id.equals("ResolvedPlugins")) {
            return new IPropertySourceList(plugin.getResolvedPlugins());
        }
        if (id.equals("ImportedPackages")) {
            return new IPropertySourceList(plugin.getImportedPackages());
        }
        if (id.equals("ExportedPackages")) {
            return new IPropertySourceList(plugin.getExportedPackages());
        }
        if (id.equals("RequiredBy")) {
            return new IPropertySourceList(plugin.getRequiredBy());
        }
        if (id.equals("IncludedIn")) {
            return new IPropertySourceList(plugin.getIncludedInFeatures());
        }
        return null;
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(Object id) {
        // Values should not be changed

    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        // Values should not be changed
    }

}
