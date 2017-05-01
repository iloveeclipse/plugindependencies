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
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 * @author obroesam
 *
 */
public class TreeFeature extends TreeParent {
    private final Feature feature;

    private final ArrayList<TreeParent> children;

    private final TreeParent parent;

    public TreeFeature(Feature feat, TreeParent treeparent) {
        super(feat.getName() + " " + feat.getVersion(), treeparent);
        this.children = new ArrayList<>();
        this.feature = feat;
        this.parent = treeparent;
    }

    @Override
    public Feature getNamedElement() {
        return feature;
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
            // Required plugins
            TreeParent resPlugins = new TreeParent("Required Plugins", this);
            for (Plugin plug : feature.getRequiredPlugins()) {
                resPlugins.addChild(new TreePlugin(plug, resPlugins));
            }
            if (resPlugins.hasChildren()) {
                this.addChild(resPlugins);
            }

            // Resolved features
            TreeParent reqFeatures = new TreeParent("Required Features", this);
            for (Feature feat : feature.getRequiredFeatures()) {
                reqFeatures.addChild(new TreeFeature(feat, reqFeatures));
            }
            if (reqFeatures.hasChildren()) {
                this.addChild(reqFeatures);
            }

//            // TODO Resolved plugins recursive
//            resPlugins = new TreeParent(TreePlugin.ALL_REQUIRED_PLUGINS, this);
//            for (Plugin plug : feature.getRecursiveResolvedPlugins()) {
//                resPlugins.addChild(new TreePlugin(plug, resPlugins));
//            }
//            if (resPlugins.hasChildren()) {
//                this.addChild(resPlugins);
//            }

            // Included Plugins
            TreeParent inclPlugins = new TreeParent("Included Plugins", this);
            for (Plugin plug : feature.getIncludedPlugins()) {
                inclPlugins.addChild(new TreePlugin(plug, inclPlugins));
            }
            if (inclPlugins.hasChildren()) {
                this.addChild(inclPlugins);
            }
            // Included Features
            TreeParent inclFeatures = new TreeParent("Included Features", this);
            for (Feature feat : feature.getIncludedFeatures()) {
                inclFeatures.addChild(new TreeFeature(feat, inclFeatures));
            }
            if (inclFeatures.hasChildren()) {
                this.addChild(inclFeatures);
            }
            // Included In Features
            TreeParent includedInFeature = new TreeParent("Included in Feature", this);
            for (Feature feat : feature.getIncludedInFeatures()) {
                includedInFeature.addChild(new TreeFeature(feat, includedInFeature));
            }
            if (includedInFeature.hasChildren()) {
                this.addChild(includedInFeature);
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
        boolean hasRequiredPlugins = !feature.getRequiredPlugins().isEmpty();
        boolean hasRrequiredFeatures = !feature.getRequiredFeatures().isEmpty();
        boolean hasIncludedPlugins = !feature.getIncludedPlugins().isEmpty();
        boolean hasIncludedFeatures = !feature.getIncludedFeatures().isEmpty();
        boolean isIncludedInFeatures = !feature.getIncludedInFeatures().isEmpty();
        boolean hasProblems = !feature.getLog().isEmpty();

        return hasRequiredPlugins || hasRrequiredFeatures || hasIncludedPlugins
                || hasIncludedFeatures || isIncludedInFeatures || hasProblems;
    }
}
