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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @class OSGi_Element
 * This class is superclass of Feature and Plugin.
 */
public class OSGIElement {
    private final String name;

    private final String version;

    private final List<String> log;

    private final Set<Plugin> resolvedPlugins;

    private final Set<Feature> includedInFeatures;

    private String elementPath;

    public OSGIElement(String name, String version) {
        this.name = name;
        this.version = version;
        this.log = new ArrayList<>();
        this.resolvedPlugins = new LinkedHashSet<>();
        this.includedInFeatures = new LinkedHashSet<>();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    protected void addToLog(String note) {
        if (!note.equals("")) {
            log.add(note);
        }
    }

    public List<String> getLog() {
        return log;
    }

    public Set<Plugin> getResolvedPlugins() {
        return resolvedPlugins;
    }

    public void addResolvedPlugin(Plugin resolvedPlugin) {
        if (resolvedPlugin != null) {
            getResolvedPlugins().add(resolvedPlugin);
            if (this instanceof Feature) {
                resolvedPlugin.addIncludingFeature((Feature) this);
            }
            if (this instanceof Plugin) {
                resolvedPlugin.addRequiring((Plugin) this);
            }
        }
    }

    public Set<Feature> getIncludedInFeatures() {
        return includedInFeatures;
    }

    protected void addIncludingFeature(Feature includingFeature) {
        this.includedInFeatures.add(includingFeature);
    }

    /**
     *
     * @return full absolute (canonical) path in OS file system
     */
    public String getPath() {
        return elementPath;
    }

    /**
     *
     * @param canonicalPath full absolute (canonical) path in OS file system
     */
    public void setPath(String canonicalPath) {
        this.elementPath = canonicalPath;
    }

    public boolean matches(String id, String vers) {
        return name.equals(id) && (version.equals(vers) || vers.isEmpty());
    }

    public String getInformationLine() {
        return this.name + " " + this.version + " " + this.elementPath;
    }

    public void writeErrorLog(ManifestEntry requiredElement,
            Set<? extends OSGIElement> elements, String type) {
        StringBuilder logEntry = new StringBuilder();
        String id = requiredElement.id.trim();
        String vers = requiredElement.getVersion();
        String optional = requiredElement.isOptional() ? " *optional*" : "";
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[name=");
        builder.append(name);
        builder.append(", version=");
        builder.append(version);
        builder.append("]");
        return builder.toString();
    }
}
