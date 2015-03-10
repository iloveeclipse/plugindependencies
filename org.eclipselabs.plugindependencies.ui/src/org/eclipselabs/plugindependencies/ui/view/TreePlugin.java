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
package org.eclipselabs.plugindependencies.ui.view;

import java.util.ArrayList;

import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 * @author obroesam
 *
 */
public class TreePlugin extends TreeParent {
    private final Plugin plugin;

    private final ArrayList<TreeParent> children;

    private final TreeParent parent;

    public TreePlugin(Plugin plugin, TreeParent treeparent) {
        super(plugin.getName() + " " + plugin.getVersion(), treeparent);
        children = new ArrayList<>();
        this.plugin = plugin;
        this.parent = treeparent;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public TreeParent getParent() {
        return parent;
    }

    @Override
    public void addChild(TreeParent child) {
        children.add(child);
    }

    @Override
    public TreeParent[] getChildren() {
        if (children.isEmpty()) {
            // Resolved Plugins
            TreeParent resPlugins = new TreeParent("Resolved Plugins", this);
            for (Plugin plug : plugin.getResolvedPlugins()) {
                resPlugins.addChild(new TreePlugin(plug, resPlugins));
            }
            if (resPlugins.hasChildren()) {
                this.addChild(resPlugins);
            }
            // Exported Packages
            TreeParent expPackages = new TreeParent("Exported Packages", this);
            for (Package pack : plugin.getExportedPackages()) {
                expPackages.addChild(new TreePackage(pack, expPackages));
            }
            if (expPackages.hasChildren()) {
                this.addChild(expPackages);
            }
            // Imported Packages
            TreeParent impPackages = new TreeParent("Imported Packages", this);
            for (Package pack : plugin.getImportedPackages()) {
                impPackages.addChild(new TreePackage(pack, impPackages));
            }
            if (impPackages.hasChildren()) {
                this.addChild(impPackages);
            }
            // Included in Features
            TreeParent includedInFeature = new TreeParent("Included in Feature", this);
            for (Feature feature : plugin.getIncludedInFeatures()) {
                includedInFeature.addChild(new TreeFeature(feature, includedInFeature));
            }
            if (includedInFeature.hasChildren()) {
                this.addChild(includedInFeature);
            }
            // Required by Plugins
            TreeParent requiredBy = new TreeParent("Required By Plugins", this);
            for (Plugin plug : plugin.getRequiredBy()) {
                requiredBy.addChild(new TreePlugin(plug, requiredBy));
            }
            if (requiredBy.hasChildren()) {
                this.addChild(requiredBy);
            }
        }
        return children.toArray(new TreeParent[children.size()]);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean hasChildren() {
        boolean hasResolvedPlugins = !plugin.getResolvedPlugins().isEmpty();
        boolean hasExportedPackages = !plugin.getExportedPackages().isEmpty();
        boolean hasImportedPackages = !plugin.getImportedPackages().isEmpty();
        boolean isIncludedInFeatures = !plugin.getIncludedInFeatures().isEmpty();
        boolean isRequiredByPlugins = !plugin.getRequiredBy().isEmpty();

        return hasResolvedPlugins || hasExportedPackages || hasImportedPackages
                || isIncludedInFeatures || isRequiredByPlugins;
    }
}
