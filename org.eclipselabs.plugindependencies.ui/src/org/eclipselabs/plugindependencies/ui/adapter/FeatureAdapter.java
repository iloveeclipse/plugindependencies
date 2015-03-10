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
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.ui.view.TreeFeature;

/**
 * @author obroesam
 *
 */
public class FeatureAdapter implements IPropertySource {
    private final Feature feature;

    public FeatureAdapter(TreeFeature treeFeature) {
        this.feature = treeFeature.getFeature();
    }

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<>();

        PropertyDescriptor name = new PropertyDescriptor("Id", "Id");
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
        PropertyDescriptor incPlugins = new PropertyDescriptor("IncludedPlugins",
                "Included Plugins");
        incPlugins.setCategory("Requirements");
        PropertyDescriptor incFeatures = new PropertyDescriptor("IncludedFeatures",
                "Included Features");
        incFeatures.setCategory("Requirements");

        PropertyDescriptor includedIn = new PropertyDescriptor("IncludedIn",
                "Included In");

        Collections.addAll(list, name, version, path, log, incPlugins, incFeatures,
                includedIn);

        return list.toArray(new IPropertyDescriptor[list.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (id.equals("Id")) {
            return feature.getName();
        }
        if (id.equals("Version")) {
            return feature.getVersion();
        }
        if (id.equals("Path")) {
            return feature.getPath();
        }
        if (id.equals("Log")) {
            return feature.getLog();
        }
        if (id.equals("IncludedPlugins")) {
            return new IPropertySourceList(feature.getResolvedPlugins());
        }
        if (id.equals("IncludedFeatures")) {
            return new IPropertySourceList(feature.getIncludedFeatures());
        }
        if (id.equals("IncludedIn")) {
            return new IPropertySourceList(feature.getIncludedInFeatures());
        }
        return null;
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(Object id) {
        //
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        //
    }
}
