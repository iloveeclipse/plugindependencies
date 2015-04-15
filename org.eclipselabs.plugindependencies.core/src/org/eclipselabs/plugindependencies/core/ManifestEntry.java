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
import java.util.List;

import org.w3c.dom.Element;

/**
 * @author obroesam
 *
 */
public class ManifestEntry extends NamedElement {

    final List<String> attributes;
    private boolean linuxgtkx86_64;

    public ManifestEntry(String name, String vers) {
        super(fixName(name), vers);
        attributes = Collections.emptyList();
    }

    /**
     * @param manifestEntries
     *            non empoty array with at least one element. The first element is plugin
     *            or package id, other elements are not parsed plugin/package attributes
     */
    public ManifestEntry(List<String> manifestEntries) {
        super(fixName(manifestEntries.get(0)), getVersion(manifestEntries));
        attributes = createAttributes(manifestEntries);
    }

    /**
     * @param xmlElement
     *            xmlElement/xmlNode containing information about a plugin/feature.
     *            Minimal xmlElement must contain an id and a version. xmlElement comes
     *            from feature.xml
     */
    public ManifestEntry(Element xmlElement) {
        super(fixName(xmlElement.getAttribute("id")), xmlElement.getAttribute("version"));
        attributes = Collections.emptyList();
        computeSystemFlags(xmlElement);
    }

    private static List<String> createAttributes(List<String> manifestEntries) {
        if (manifestEntries.size() <= 1) {
            return Collections.emptyList();
        }
        manifestEntries.remove(0);
        return Collections.unmodifiableList(manifestEntries);
    }

    private void computeSystemFlags(Element xmlElement) {
        String os = xmlElement.getAttribute("os");
        String ws = xmlElement.getAttribute("ws");
        String arch = xmlElement.getAttribute("arch");
        // XXX this should not be hard coded here
        if ((os.isEmpty() || os.equals("linux")) && (ws.isEmpty() || ws.equals("gtk"))
                && (arch.isEmpty() || arch.equals("x86_64"))) {
            linuxgtkx86_64 = true;
        } else {
            linuxgtkx86_64 = false;
        }
    }

    private static String fixName(String name) {
        if ("system.bundle".equals(name)) {
            return "org.eclipse.osgi";
        }
        return name.trim();
    }

    private boolean attributesContain(String text) {
        for (String attr : attributes) {
            if (attr.contains(text)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOptional() {
        return attributesContain("optional");
    }

    public boolean isDynamicImport() {
        return attributesContain("dynamicImport");
    }

    public boolean isReexport() {
        return attributesContain("visibility:=\"reexport\"");
    }

    public boolean isLinuxgtkx86_64() {
        return linuxgtkx86_64;
    }

    /**
     *
     * @return never null, empty string if no version information is available, otherwise
     *         valid OSGI version OR version range
     */
    private static String getVersion(List<String> manifestEntries) {
        for (String attr : manifestEntries) {
            if (attr.contains("version=")) {
                return StringUtil.extractVersionOrRange(attr);
            }
        }
        return EMPTY_VERSION;
    }

    /**
     * Checks if the Plugin has the right SymbolicName and the right version to fit the
     * ManifestEntry.
     *
     * @param element
     *            Plugin to check
     * @return true for fit
     */
    public boolean isMatching(NamedElement element) {
        // DO NOT extract to local variables, takes 10 times longer
        return getName().equals(element.getName())
                && DependencyResolver.isCompatibleVersion(getVersion(), element.getVersion());
    }

    @Override
    public String toString() {
        return super.toString() + attributes;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + attributes.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof ManifestEntry)) {
            return false;
        }
        ManifestEntry other = (ManifestEntry) obj;
        if (!attributes.equals(other.attributes)) {
            return false;
        }
        return true;
    }

}
