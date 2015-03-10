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
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author obroesam
 *
 */
public class ManifestEntry {
    public final String id;

    final List<String> attributes;

    private String version;

    private boolean linuxgtkx86_64;

    public ManifestEntry(String name, String vers) {
        String tmpId = name;
        if (tmpId.equals("system.bundle")) {
            tmpId = "org.eclipse.osgi";
        }
        id = tmpId;
        version = vers;
        attributes = new ArrayList<>();
    }

    /**
     * @param manifestEntries
     *            non empoty array with at least one element. The first element is plugin
     *            or package id, other elements are not parsed plugin/package attributes
     */
    public ManifestEntry(String[] manifestEntries) {
        String tmpId = manifestEntries[0];
        if (tmpId.equals("system.bundle")) {
            tmpId = "org.eclipse.osgi";
        }
        id = tmpId.trim();
        ArrayList<String> tmpAttributes = new ArrayList<>();
        if (manifestEntries.length > 1) {
            for (int i = 1; i < manifestEntries.length; i++) {
                tmpAttributes.add(manifestEntries[i]);
            }
        }
        attributes = Collections.unmodifiableList(tmpAttributes);
    }

    /**
     * @param xmlNode
     *            xmlElement/xmlNode containing information about a plugin/feature.
     *            Minimal xmlElement must contain an id and a version. xmlElement comes
     *            from feature.xml
     */
    public ManifestEntry(Node xmlNode) {
        Element xmlElement = (Element) xmlNode;

        id = xmlElement.getAttribute("id");
        version = xmlElement.getAttribute("version");
        String os = xmlElement.getAttribute("os");
        String ws = xmlElement.getAttribute("ws");
        String arch = xmlElement.getAttribute("arch");
        if ((os.isEmpty() || os.equals("linux")) && (ws.isEmpty() || ws.equals("gtk"))
                && (arch.isEmpty() || arch.equals("x86_64"))) {
            linuxgtkx86_64 = true;
        } else {
            linuxgtkx86_64 = false;
        }
        attributes = Collections.unmodifiableList(new ArrayList<String>());

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
    public String getVersion() {
        String versionsString = "";
        for (String attr : attributes) {
            if (attr.contains("version=")) {
                versionsString = attr;
            }
        }
        if (version == null) {
            version = StringUtil.extractVersionOrRange(versionsString);
        }
        return version;
    }

    /**
     * Checks if the Plugin has the right SymbolicName and the right version to fit the
     * ManifestEntry.
     *
     * @param element
     *            Plugin to check
     * @return true for fit
     */
    public boolean isMatching(OSGIElement element) {
        // DO NOT extract to local variables, takes 10 times longer
        return this.id.equals(element.getName())
                && DependencyResolver.isCompatibleVersion(this.getVersion(),
                        element.getVersion());
    }

    /**
     * Checks if the Package has the right Name and the right version to fit the
     * ManifestEntry.
     *
     * @param pack
     *            Package to check
     * @return true for fit
     */
    public boolean isMatching(Package pack) {
        // DO NOT extract to local variables, takes 10 times longer
        return id.equals(pack.getName())
                && DependencyResolver
                        .isCompatibleVersion(getVersion(), pack.getVersion());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ManifestEntry other = (ManifestEntry) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }
}
