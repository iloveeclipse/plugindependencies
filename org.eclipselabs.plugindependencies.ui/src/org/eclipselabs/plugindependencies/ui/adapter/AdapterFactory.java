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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipselabs.plugindependencies.ui.view.TreeFeature;
import org.eclipselabs.plugindependencies.ui.view.TreePackage;
import org.eclipselabs.plugindependencies.ui.view.TreePlugin;

public class AdapterFactory implements IAdapterFactory {

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IPropertySource.class && adaptableObject instanceof TreePlugin) {
            return new PluginAdapter((TreePlugin) adaptableObject);
        }
        if (adapterType == IPropertySource.class
                && adaptableObject instanceof TreeFeature) {
            return new FeatureAdapter((TreeFeature) adaptableObject);
        }
        if (adapterType == IPropertySource.class
                && adaptableObject instanceof TreePackage) {
            return new PackageAdapter((TreePackage) adaptableObject);
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[0];
    }

}
