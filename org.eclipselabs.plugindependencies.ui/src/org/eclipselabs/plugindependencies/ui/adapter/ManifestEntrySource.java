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
import org.eclipselabs.plugindependencies.core.ManifestEntry;

/**
 * @author obroesam
 *
 */
public class ManifestEntrySource implements IPropertySource {
    private final ManifestEntry entry;

    public ManifestEntrySource(ManifestEntry entry) {
        this.entry = entry;
    }

    @Override
    public String toString() {
        return entry.id + " " + entry.getVersion();
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
        PropertyDescriptor optional = new PropertyDescriptor("Optional", "Optional");
        PropertyDescriptor dynamicImport = new PropertyDescriptor("DynamicImport",
                "DynamicImport");
        PropertyDescriptor reexport = new PropertyDescriptor("Reexport", "Reexport");

        Collections.addAll(list, name, version, optional, dynamicImport, reexport);

        return list.toArray(new IPropertyDescriptor[list.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (id.equals("Name")) {
            return entry.id;
        }
        if (id.equals("Version")) {
            return entry.getVersion();
        }
        if (id.equals("Optional")) {
            return entry.isOptional() ? "Yes" : "";
        }
        if (id.equals("DynamicImport")) {
            return entry.isDynamicImport() ? "Yes" : "";
        }
        if (id.equals("Reexport")) {
            return entry.isReexport() ? "Yes" : "";
        }
        return null;
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(Object id) {
        // Should not delete values
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        // Should not edit values
    }

}
