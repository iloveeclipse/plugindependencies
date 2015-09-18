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

    public final static Feature DUMMY_FEATURE = new Feature("", NamedElement.EMPTY_VERSION);


    private List<ManifestEntry> requiredFeatureEntries;

    private List<ManifestEntry> includedPluginEntries;

    private List<ManifestEntry> includedFeatureEntries;

    private Set<Feature> includedFeatures;
    private Set<Plugin> includedPlugins;
    private Set<Feature> requiredFeatures;

    public Feature(String name, String vers) {
        super(name, vers);
        includedFeatures = new LinkedHashSet<>();
        includedPlugins = new LinkedHashSet<>();

        requiredFeatureEntries = new ArrayList<>();

        includedPluginEntries = new ArrayList<>();
        includedFeatureEntries = new ArrayList<>();
        requiredFeatures = new LinkedHashSet<>();
    }

    public Set<Feature> getRequiredFeatures() {
        return requiredFeatures;
    }

    public void addRequiredFeatureEntry(ManifestEntry required) {
        this.requiredFeatureEntries.add(required);
    }

    public void addIncludedPluginEntries(NodeList requiredplugins) {
        for (int i = 0; i < requiredplugins.getLength(); i++) {
            this.includedPluginEntries.add(new ManifestEntry((Element)requiredplugins.item(i), "id"));
        }
    }

    public void addIncludedFeatureEntries(NodeList requiredplugins) {
        for (int i = 0; i < requiredplugins.getLength(); i++) {
            this.includedFeatureEntries.add(new ManifestEntry((Element)requiredplugins.item(i), "id"));
        }
    }

    public List<ManifestEntry> getRequiredFeatureEntries() {
        return requiredFeatureEntries;
    }

    public Set<Feature> getIncludedFeatures() {
        return includedFeatures;
    }
    public Set<Plugin> getIncludedPlugins() {
        return includedPlugins;
    }

    public List<ManifestEntry> getIncludedFeatureEntries() {
        return includedFeatureEntries;
    }

    public List<ManifestEntry> getIncludedPluginEntries() {
        return includedPluginEntries;
    }

    public void addRequiredFeature(Feature requires) {
        if(requires != null) {
            this.requiredFeatures.add(requires);
            requires.addRequiring(this);
        }
    }

    public void addIncludedFeature(Feature included) {
        this.includedFeatures.add(included);
        included.addIncludingFeature(this);
    }

    public void addIncludedPlugin(Plugin included) {
        this.includedPlugins.add(included);
        included.addIncludingFeature(this);
    }

    @Override
    public void parsingDone() {
        super.parsingDone();
        includedFeatures = includedFeatures.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(includedFeatures);
        includedPlugins = includedPlugins.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(includedPlugins);
        requiredFeatures = requiredFeatures.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredFeatures);

        requiredFeatureEntries = requiredFeatureEntries.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(requiredFeatureEntries);

        includedFeatureEntries = includedFeatureEntries.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(includedFeatureEntries);
        includedPluginEntries = includedPluginEntries.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(includedPluginEntries);
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
