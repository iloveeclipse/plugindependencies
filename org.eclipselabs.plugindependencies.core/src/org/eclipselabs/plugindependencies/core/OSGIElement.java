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

    private Set<Plugin> resolvedPlugins;

    private Set<Feature> includedInFeatures;

    private String elementPath;

    private Set<? super OSGIElement> duplicates;

    public OSGIElement(String name, String version) {
        super(name, version);
        this.resolvedPlugins = new LinkedHashSet<>();
        this.includedInFeatures = new LinkedHashSet<>();
        this.duplicates = new LinkedHashSet<>();
    }

    public void parsingDone(){
        resolvedPlugins = resolvedPlugins.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(resolvedPlugins);
        includedInFeatures = includedInFeatures.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(includedInFeatures);
        duplicates = duplicates.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(duplicates);
    }

    public Set<Plugin> getResolvedPlugins() {
        return resolvedPlugins;
    }

    public void addResolvedPlugin(Plugin plugin) {
        if (plugin != null) {
            resolvedPlugins.add(plugin);
        }
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

    public Set<? super OSGIElement> getDuplicates() {
        return duplicates;
    }

    public String getInformationLine() {
        if(version.isEmpty()){
            return name + " " + elementPath;
        }
        return name + " " + version + " " + elementPath;
    }

    public void writeErrorLog(ManifestEntry entry, Set<? extends OSGIElement> elements, String type) {
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
            addErrorToLog(type + " not found: " + entry.getNameAndVersion() + optional);
        }
    }
}
