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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipselabs.plugindependencies.core.OSGIElement;

/**
 * @author obroesam
 *
 */
public class PluginFeatureElement implements IPropertySource {
    private final OSGIElement element;

    public PluginFeatureElement(OSGIElement element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return element.getName() + " " + element.getVersion();
    }

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<>();

        PropertyDescriptor name = new PropertyDescriptor("Name", "Name");
        PropertyDescriptor version = new PropertyDescriptor("Version", "Version");
        PropertyDescriptor path = new PropertyDescriptor("Path", "Path");

        Collections.addAll(list, name, version, path);

        return list.toArray(new IPropertyDescriptor[list.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (id.equals("Name")) {
            return element.getName();
        }
        if (id.equals("Version")) {
            return element.getVersion();
        }
        if (id.equals("Path")) {
            return element.getPath();
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
