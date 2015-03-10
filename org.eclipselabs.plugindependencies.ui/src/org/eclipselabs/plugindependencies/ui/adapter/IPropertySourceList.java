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
import java.util.List;
import java.util.Set;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.ManifestEntry;
import org.eclipselabs.plugindependencies.core.OSGIElement;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 * @author obroesam
 *
 */
public class IPropertySourceList implements IPropertySource {
    private final List<IPropertySource> list;

    public IPropertySourceList(List<ManifestEntry> entries) {
        list = new ArrayList<>();
        for (ManifestEntry entry : entries) {
            list.add(new ManifestEntrySource(entry));
        }
    }

    public IPropertySourceList(Set<?> elements) {
        list = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof Plugin || element instanceof Feature) {
                list.add(new PluginFeatureElement((OSGIElement) element));
            }
            if (element instanceof Package) {
                list.add(new PackageElement((Package) element));
            }
        }
    }

    public List<IPropertySource> getPropertyList() {
        return list;
    }

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> propList = new ArrayList<>();

        for (IPropertySource source : list) {
            PropertyDescriptor entry = new PropertyDescriptor(source, source.toString());
            propList.add(entry);

        }

        return propList.toArray(new IPropertyDescriptor[propList.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        for (IPropertySource source : list) {
            if (id.equals(source)) {
                return source;
            }
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
