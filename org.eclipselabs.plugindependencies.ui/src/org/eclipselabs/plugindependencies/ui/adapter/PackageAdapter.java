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
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.ui.view.TreePackage;

/**
 * @author obroesam
 *
 */
public class PackageAdapter implements IPropertySource {
    private final Package pack;

    public PackageAdapter(TreePackage treePackage) {
        this.pack = treePackage.getNamedElement();
    }

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> list = new ArrayList<>();

        PropertyDescriptor name = new PropertyDescriptor("Name", "Name");
        name.setCategory("General");
        PropertyDescriptor version = new PropertyDescriptor("Version", "Version");
        version.setCategory("General");
        PropertyDescriptor exporter = new PropertyDescriptor("Export", "Exported By");
        exporter.setCategory("General");
        PropertyDescriptor reexporter = new PropertyDescriptor("Reexport",
                "Reexported By");
        reexporter.setCategory("General");
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

        PropertyDescriptor importedBy = new PropertyDescriptor("Import", "Imported By");

        Collections.addAll(list, name, version, exporter, importedBy, reexporter, log);

        return list.toArray(new IPropertyDescriptor[list.size()]);
    }

    @Override
    public Object getPropertyValue(Object id) {
        if (id.equals("Name")) {
            return pack.getName();
        }
        if (id.equals("Version")) {
            return pack.getVersion();
        }
        if (id.equals("Export")) {
            return new IPropertySourceList(pack.getExportedBy());
        }
        if (id.equals("Reexport")) {
            return new IPropertySourceList(pack.getReexportedBy());
        }
        if (id.equals("Import")) {
            return new IPropertySourceList(pack.getImportedBy());
        }
        if (id.equals("Log")) {
            return pack.getLog();
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
