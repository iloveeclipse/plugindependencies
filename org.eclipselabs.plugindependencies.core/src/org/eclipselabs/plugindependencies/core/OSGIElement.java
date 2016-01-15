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

/**
 *
 * @class OSGi_Element
 * This class is superclass of Feature and Plugin.
 */
public abstract class OSGIElement extends NamedElement /* TODO implements Comparable */ {

    private Set<Plugin> requiredPlugins;
    private Set<Plugin> requiredReexportedPlugins;

    private Set<Feature> includedInFeatures;

    private String elementPath;

    private List<OSGIElement> duplicates;

    private Set<OSGIElement> requiredBy;

    private List<ManifestEntry> requiredPluginEntries;

    public OSGIElement(String name, String version) {
        super(name, version);
        this.requiredPlugins = new LinkedHashSet<>();
        this.requiredReexportedPlugins = new LinkedHashSet<>();
        this.includedInFeatures = new LinkedHashSet<>();
        this.requiredPluginEntries = new ArrayList<>();
        this.duplicates = new ArrayList<>();
        this.requiredBy = new LinkedHashSet<>();
    }

    public Set<OSGIElement> getRequiredBy() {
        return requiredBy;
    }

    void addRequiring(OSGIElement requires) {
        this.requiredBy.add(requires);
    }

    public List<ManifestEntry> getRequiredPluginEntries() {
        return requiredPluginEntries;
    }

    public void setRequiredPlugins(String requplugins) {
        requiredPluginEntries = StringUtil.splitInManifestEntries(requplugins);
    }

    public void parsingDone(){
        requiredPlugins = requiredPlugins.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredPlugins);
        requiredReexportedPlugins = requiredReexportedPlugins.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredReexportedPlugins);
        includedInFeatures = includedInFeatures.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(includedInFeatures);
        duplicates = duplicates.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(duplicates);
        requiredBy = requiredBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredBy);
        requiredPluginEntries = requiredPluginEntries.isEmpty()? Collections.EMPTY_LIST : Collections.unmodifiableList(requiredPluginEntries);
    }

    public Set<Plugin> getRequiredPlugins() {
        return requiredPlugins;
    }

    public Set<Plugin> getRequiredReexportedPlugins() {
        return requiredReexportedPlugins;
    }

    public void addRequiredPlugin(Plugin plugin, boolean reexport) {
        if (plugin != null) {
            requiredPlugins.add(plugin);
            plugin.addRequiring(this);
            if(reexport){
                requiredReexportedPlugins.add(plugin);
            }
        }
    }

    public void addRequiredPluginEntry(ManifestEntry required) {
        this.requiredPluginEntries.add(required);
    }

    public Set<Feature> getIncludedInFeatures() {
        return includedInFeatures;
    }

    protected void addIncludingFeature(Feature includingFeature) {
        this.includedInFeatures.add(includingFeature);
    }

    /**
     * @return full absolute (canonical) path in OS file system
     */
    public String getPath() {
        return elementPath;
    }

    /**
     * @param canonicalPath full absolute (canonical) path in OS file system
     */
    public void setPath(String canonicalPath) {
        this.elementPath = canonicalPath;
    }

    public void addDuplicate(OSGIElement dup){
        duplicates.add(dup);
    }

    public List<OSGIElement> getDuplicates() {
        return duplicates;
    }

    public String getInformationLine() {
        if(getVersion().isEmpty()){
            return name + " " + elementPath;
        }
        return name + " " + getVersion() + " " + elementPath;
    }

    public void logBrokenEntry(ManifestEntry entry, Set<? extends OSGIElement> elements, String type) {
        String optional = entry.isOptional() ? " *optional*" : "";
        int setSize = elements.size();

        if (setSize > 1) {
            StringBuilder logStr = new StringBuilder();
            logStr.append("more than one " + type + " found for ");
            logStr.append(entry.getNameAndVersion() + optional + "\n");
            for (OSGIElement element : elements) {
                logStr.append("\t" + element.getInformationLine() + "\n");
            }
            addWarningToLog(logStr.toString());
        } else if (setSize == 0 && optional.isEmpty()) {
            if(entry.getName().endsWith(".source")) {
                addWarningToLog(type + " not found: " + entry.getNameAndVersion() + optional);
            } else {
                addErrorToLog(type + " not found: " + entry.getNameAndVersion() + optional);
            }
        }
    }

    /**
     * Searches the reqPlugin in requiredPlugins and returns if the reqPlugin is optional.
     *
     * @param reqPlugin
     *            reqPlugin that should be checked for optional
     * @return true if reqPlugin is optional
     */
    public boolean isOptional(OSGIElement reqPlugin) {
        if (requiredPluginEntries.isEmpty()) {
            return false;
        }
        for (ManifestEntry entry : requiredPluginEntries) {
            if (entry.isMatching(reqPlugin) && entry.isOptional()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string with the plugins that require this plugin. If this plugin is
     * optional for the other plugin "*optional* for " will be printed before the plugin.
     * <p>
     * String has the following form:
     * <p>
     * "is required by: <br>
     * plugin: symbolicName version path <br>
     * plugin: *optional* for symbolicName version path"
     *
     * @return string with information about plugins needing this
     */
    public String printRequiringThis() {
        StringBuilder out = new StringBuilder();
        if (requiredBy.size() == 0) {
            return out.toString();
        }
        out.append("is required by:\n");
        for (OSGIElement elt : this.requiredBy) {
            out.append("\t");
            if (elt.isOptional(this)) {
                out.append("*optional* for ");
            }
            if(elt instanceof Plugin) {
                Plugin plugin = (Plugin) elt;
                out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
                out.append(plugin.getInformationLine() + "\n");
            } else {
                out.append("feature: ");
                out.append(elt.getInformationLine() + "\n");
            }
        }
        return out.toString();
    }
}
