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
package org.eclipselabs.plugindependencies.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author obroesam
 *
 */
public class Feature extends OSGIElement {

    private List<ManifestEntry> requiredPlugins;

    private List<ManifestEntry> requiredFeatures;

    private Set<Feature> includedFeatures;

    public Feature(String name, String vers) {
        super(name, vers);
        includedFeatures = new LinkedHashSet<>();
        requiredPlugins = new ArrayList<>();
        requiredFeatures = new ArrayList<>();
    }

    public List<ManifestEntry> getRequiredPlugins() {
        return requiredPlugins;
    }

    public void addRequiredPlugins(NodeList requiredplugins) {
        for (int i = 0; i < requiredplugins.getLength(); i++) {
            this.requiredPlugins.add(new ManifestEntry((Element)requiredplugins.item(i)));
        }
    }

    public List<ManifestEntry> getRequiredFeatures() {
        return requiredFeatures;
    }

    public void addRequiredFeatures(NodeList requiredfeatures) {
        for (int i = 0; i < requiredfeatures.getLength(); i++) {
            this.requiredFeatures.add(new ManifestEntry((Element)requiredfeatures.item(i)));
        }
    }

    public Set<Feature> getIncludedFeatures() {
        return Collections.unmodifiableSet(includedFeatures);
    }

    public void addIncludedFeatures(Set<Feature> includedFeatureSet) {
        this.includedFeatures.addAll(includedFeatureSet);
        for (Feature feature : includedFeatureSet) {
            feature.addIncludingFeature(this);
        }
    }

    @Override
    public void addResolvedPlugin(Plugin plugin) {
        super.addResolvedPlugin(plugin);
        if (plugin != null) {
            plugin.addIncludingFeature(this);
        }
    }

    @Override
    public void parsingDone() {
        super.parsingDone();
        includedFeatures = includedFeatures.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(includedFeatures);
        requiredPlugins = requiredPlugins.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(requiredPlugins);
        requiredFeatures = requiredFeatures.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(requiredFeatures);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        return obj instanceof Feature;
    }

}
