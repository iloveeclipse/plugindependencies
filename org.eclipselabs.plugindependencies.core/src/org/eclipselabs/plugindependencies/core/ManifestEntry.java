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

import static org.eclipselabs.plugindependencies.core.PlatformState.fixName;
import static org.eclipselabs.plugindependencies.core.PlatformState.fixVersion;

import java.util.Collections;
import java.util.List;

import org.eclipselabs.plugindependencies.core.PlatformState.PlatformSpecs;
import org.w3c.dom.Element;

/**
 * @author obroesam
 *
 */
public class ManifestEntry extends NamedElement {

    final List<String> attributes;
    private final PlatformSpecs platformSpecs;

    public ManifestEntry(String name, String vers) {
        super(fixName(name), fixVersion(vers));
        attributes = Collections.emptyList();
        platformSpecs = readPlatformSpecs();
    }

    /**
     * @param manifestEntries
     *            non empoty array with at least one element. The first element is plugin
     *            or package id, other elements are not parsed plugin/package attributes
     */
    public ManifestEntry(List<String> manifestEntries) {
        super(fixName(manifestEntries.get(0)), fixVersion(getVersion(manifestEntries)));
        attributes = createAttributes(manifestEntries);
        platformSpecs = readPlatformSpecs();
    }

    /**
     * @param xmlElement
     *            xmlElement/xmlNode containing information about a plugin/feature.
     *            Minimal xmlElement must contain an id and a version. xmlElement comes
     *            from feature.xml
     */
    public ManifestEntry(Element xmlElement) {
        super(fixName(xmlElement.getAttribute("id")), fixVersion(xmlElement.getAttribute("version")));
        attributes = Collections.emptyList();
        platformSpecs = computeSystemFlags(xmlElement);
    }

    private PlatformSpecs readPlatformSpecs() {
        String filter = attribute("Eclipse-PlatformFilter");
        if(filter == null) {
            return PlatformState.UNDEFINED_SPECS;
        }
        // Eclipse-PlatformFilter: (& (osgi.ws=gtk) (osgi.os=linux) (osgi.arch=x86_64))
        String os = readLdapAttr(filter, "osgi.os");
        String ws = readLdapAttr(filter, "osgi.ws");
        String arch = readLdapAttr(filter, "osgi.arch");
        return new PlatformSpecs(os, ws, arch);
    }

    private static String readLdapAttr(String filter, String key) {
        int idx = filter.indexOf(key);
        if(idx <= 0) {
            return null;
        }
        int startValueIdx = idx + key.length() + "=".length();
        int idxBrace = filter.indexOf(')', startValueIdx);
        if(idxBrace <= startValueIdx) {
            return null;
        }
        return filter.substring(startValueIdx, idxBrace).trim();
    }

    private static List<String> createAttributes(List<String> manifestEntries) {
        if (manifestEntries.size() <= 1) {
            return Collections.emptyList();
        }
        manifestEntries.remove(0);
        return Collections.unmodifiableList(manifestEntries);
    }

    private static PlatformSpecs computeSystemFlags(Element xmlElement) {
        String os = xmlElement.getAttribute("os").trim();
        os = os.isEmpty() ? null : os;
        String ws = xmlElement.getAttribute("ws").trim();
        ws = ws.isEmpty() ? null : ws;
        String arch = xmlElement.getAttribute("arch").trim();
        arch = arch.isEmpty() ? null : arch;
        return new PlatformSpecs(os, ws, arch);
    }

    private boolean attributesContain(String text) {
        for (String attr : attributes) {
            if (attr.contains(text)) {
                return true;
            }
        }
        return false;
    }

    private String attribute(String id) {
        for (String attr : attributes) {
            if (attr.startsWith(id)) {
                return attr;
            }
        }
        return null;
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

    public boolean isMatchingPlatform(PlatformSpecs p) {
        return platformSpecs.matches(p);
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
     * Checks if the given element has same symbolic name and compatible version (greater or equals)
     *
     * @param element
     *            to check
     * @return true for matching name and compatible version
     */
    public boolean isMatching(NamedElement element) {
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
