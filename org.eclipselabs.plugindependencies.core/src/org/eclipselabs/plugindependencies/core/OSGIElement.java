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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @class OSGi_Element
 * This class is superclass of Feature and Plugin.
 */
public abstract class OSGIElement extends NamedElement {

    private final Set<Plugin> resolvedPlugins;

    private final Set<Feature> includedInFeatures;

    private String elementPath;

    public OSGIElement(String name, String version) {
        super(name, version);
        this.resolvedPlugins = new LinkedHashSet<>();
        this.includedInFeatures = new LinkedHashSet<>();
    }

    public Set<Plugin> getResolvedPlugins() {
        return Collections.unmodifiableSet(resolvedPlugins);
    }

    public void addResolvedPlugin(Plugin plugin) {
        if (plugin != null) {
            resolvedPlugins.add(plugin);
        }
    }

    public Set<Feature> getIncludedInFeatures() {
        return Collections.unmodifiableSet(includedInFeatures);
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

    public String getInformationLine() {
        if(version.isEmpty()){
            return name + " " + elementPath;
        }
        return name + " " + version + " " + elementPath;
    }

    public void writeErrorLog(ManifestEntry requiredElement,
            Set<? extends OSGIElement> elements, String type) {
        StringBuilder logEntry = new StringBuilder();
        String id = requiredElement.getName().trim();
        String vers = requiredElement.getVersion();
        String optional = requiredElement.isOptional() ? " *optional*" : EMPTY_VERSION;
        int setSize = elements.size();

        if (setSize > 1) {
            logEntry.append("Warning: More than one " + type + " found for ");
            logEntry.append(id + " " + vers + optional + "\n");
            for (OSGIElement element : elements) {
                logEntry.append("\t" + element.getInformationLine() + "\n");
            }
        }
        if (setSize == 0) {
            String errortype = optional.isEmpty() ? "Error: " : "Warning: ";
            logEntry.append(errortype + type + " not found: ");
            logEntry.append(id + " " + vers + optional);
        }
        addToLog(logEntry.toString());
    }
}
