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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.OSGIElement;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 * @author obroesam
 *
 */
public class TreePlugin extends TreeParent {
    static final String ALL_DEPS = "All dependencies";
    static final String EARLY_STARTUP = "EarlyStartup";
    static final String EXPORTS = "Exports";
    static final String FEATURES = "Features";
    static final String IMPORTS = "Imports";
    static final String INCLUDED_IN = "Included in";
    static final String PACKAGES = "Packages";
    static final String PLUGINS = "Plugins";
    static final String REQUIRED_BY = "Required by";
    static final String REQUIRES = "Requires";

    static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            ALL_DEPS,
            EARLY_STARTUP,
            EXPORTS,
            FEATURES,
            IMPORTS,
            INCLUDED_IN,
            PACKAGES,
            PLUGINS,
            REQUIRED_BY,
            REQUIRES
            ));

    private final Plugin plugin;

    private final List<TreeParent> children;

    private final TreeParent parent;

    public TreePlugin(Plugin plugin, TreeParent treeparent) {
        super(getName(plugin), treeparent);
        children = new ArrayList<>();
        this.plugin = plugin;
        this.parent = treeparent;
    }

    @Override
    public Plugin getNamedElement() {
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
            TreeParent resPlugins = new TreeParent(REQUIRES, this);
            for (Plugin plug : plugin.getRequiredPlugins()) {
                resPlugins.addChild(new TreePlugin(plug, resPlugins));
            }
            if (resPlugins.hasChildren()) {
                this.addChild(resPlugins);
            }

            // Resolved Plugins recursive
            resPlugins = new TreeParent(ALL_DEPS, this);
            for (Plugin plug : plugin.getRecursiveResolvedPlugins()) {
                resPlugins.addChild(new TreePlugin(plug, resPlugins));
            }
            if (resPlugins.hasChildren()) {
                this.addChild(resPlugins);
            }

            // Imported Packages
            TreeParent impPackages = new TreeParent(IMPORTS, this);
            for (Package pack : plugin.getImportedPackages()) {
                impPackages.addChild(new TreePackage(pack, impPackages));
            }
            if (impPackages.hasChildren()) {
                this.addChild(impPackages);
            }

            // Exported Packages
            TreeParent expPackages = new TreeParent(EXPORTS, this);
            for (Package pack : plugin.getExportedPackages()) {
                expPackages.addChild(new TreePackage(pack, expPackages));
            }
            if (expPackages.hasChildren()) {
                this.addChild(expPackages);
            }

            // Included in Features
            TreeParent includedInFeature = new TreeParent(INCLUDED_IN, this);
            for (Feature feature : plugin.getIncludedInFeatures()) {
                includedInFeature.addChild(new TreeFeature(feature, includedInFeature));
            }
            if (includedInFeature.hasChildren()) {
                this.addChild(includedInFeature);
            }
            // Required by plugins or features
            TreeParent requiredBy = new TreeParent(REQUIRED_BY, this);
            for (OSGIElement elt : plugin.getRequiredBy()) {
                if(elt instanceof Plugin) {
                    requiredBy.addChild(new TreePlugin((Plugin) elt, requiredBy));
                } else
                if(elt instanceof Feature) {
                    requiredBy.addChild(new TreeFeature((Feature) elt, requiredBy));
                }
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
        boolean hasResolvedPlugins = !plugin.getRequiredPlugins().isEmpty();
        boolean hasExportedPackages = !plugin.getExportedPackages().isEmpty();
        boolean hasImportedPackages = !plugin.getImportedPackages().isEmpty();
        boolean isIncludedInFeatures = !plugin.getIncludedInFeatures().isEmpty();
        boolean isRequiredByPlugins = !plugin.getRequiredBy().isEmpty();

        return hasResolvedPlugins || hasExportedPackages || hasImportedPackages
                || isIncludedInFeatures || isRequiredByPlugins;
    }
}
