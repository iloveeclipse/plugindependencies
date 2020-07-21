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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    static final String COMPILE_DEPS = "Compilation dependencies";
    static final String EARLY_STARTUP = "EarlyStartup";
    static final String EXPORTS = "Exports";
    static final String REEXPORTS = "Reexports";
    static final String REEXPORTS_BY = "Reexported by";
    static final String FEATURES = "Features";
    static final String IMPORTS = "Imports";
    static final String INCLUDED_IN = "Included in";
    static final String PACKAGES = "Packages";
    static final String PLUGINS = "Plugins";
    static final String REQUIRED_BY = "Required by";
    static final String REQUIRES = "Requires";

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

            // Needed for compilation Plugins
            TreeParent compPlugins = new TreeParent(COMPILE_DEPS, this);
            for (Plugin plug : plugin.getVisibleOnCompilePlugins()) {
                compPlugins.addChild(new TreePlugin(plug, compPlugins));
            }
            if (compPlugins.hasChildren()) {
                this.addChild(compPlugins);
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

            // Re-exported plugins
            TreeParent reexp = new TreeParent(REEXPORTS, this);
            for (Plugin plug : plugin.getRequiredReexportedPlugins()) {
                reexp.addChild(new TreePlugin(plug, reexp));
            }
            // Re-exported packages
            for (Package pack : plugin.getReExportedPackages()) {
                reexp.addChild(new TreePackage(pack, reexp));
            }
            if (reexp.hasChildren()) {
                this.addChild(reexp);
            }

            // Re-exported by plugins
            TreeParent reexpby = new TreeParent(REEXPORTS_BY, this);
            for (Plugin plug : plugin.getReexportedBy()) {
                reexpby.addChild(new TreePlugin(plug, reexpby));
            }
            if (reexpby.hasChildren()) {
                this.addChild(reexpby);
            }

            // Resolved Plugins recursive
            TreeParent rresPlugins = new TreeParent(ALL_DEPS, this);
            Set<Package> allImpPack = new HashSet<>();
            for (Plugin plug : plugin.getRecursiveResolvedPlugins()) {
            	rresPlugins.addChild(new TreePlugin(plug, rresPlugins));
                allImpPack.addAll(plug.getImportedPackages());
            }
            for (Package pack : allImpPack) {
            	rresPlugins.addChild(new TreePackage(pack, rresPlugins));
            }
            if (rresPlugins.hasChildren()) {
                this.addChild(rresPlugins);
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

            addProblems();
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
        boolean hasProblems = !plugin.getLog().isEmpty();

        return hasResolvedPlugins || hasExportedPackages || hasImportedPackages
                || isIncludedInFeatures || isRequiredByPlugins || hasProblems;
    }
}
