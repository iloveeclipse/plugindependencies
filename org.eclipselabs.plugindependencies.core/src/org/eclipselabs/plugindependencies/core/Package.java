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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author obroesam
 *
 */
public class Package {
    private final String name;

    private final String version;

    private final Set<Plugin> exportedBy;

    private final Set<Plugin> importedBy;

    private final Set<Plugin> reexportedBy;

    public Package(String name, String version) {
        this.name = name;
        this.version = version;
        this.exportedBy = new LinkedHashSet<>();
        this.importedBy = new LinkedHashSet<>();
        this.reexportedBy = new LinkedHashSet<>();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getInformationLine() {
        String sep = getVersion().isEmpty()? "" : " ";
        return "Package: " + getName() + sep + getVersion() + "\n" + printExportedBy(1);
    }

    public Set<Plugin> getExportedBy() {
        return exportedBy;
    }

    public void addExportPlugin(Plugin plugin) {
        this.exportedBy.add(plugin);
    }

    public Set<Plugin> getReexportedBy() {
        return reexportedBy;
    }

    public void addReExportPlugin(Plugin plugin) {
        this.reexportedBy.add(plugin);
    }

    public Set<Plugin> getImportedBy() {
        return importedBy;
    }

    public void addImportedBy(Plugin importsPackage) {
        this.importedBy.add(importsPackage);
    }

    /**
     * Print the Plugins that are Exporting this.
     *
     * @param indentation
     *            Indentation of the out string
     * @return String with Plugin information
     */
    public String printExportedBy(int indentation) {
        StringBuilder out = new StringBuilder();
        out.append("\t\tExported By:\n");
        if (exportedBy.size() == 0) {
            out.append("\t\tJRE System Library");
            return out.toString();
        }
        for (Plugin plugin : exportedBy) {
            out.append("\t\t");
            out.append(plugin.isFragment() ? "Fragment: " : "Plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    /**
     * Print the Plugins that are importing this.
     *
     * @param indentation
     *            Indentation of the out string
     * @return String with Plugin information
     */
    public String printImportedBy(int indentation) {
        StringBuilder out = new StringBuilder();
        if (importedBy.size() == 0) {
            return out.toString();
        }
        out.append("Imported By:\n");
        for (Plugin plugin : importedBy) {
            out.append("\t");
            out.append(plugin.isOptional(this) ? "*optional* for " : "");
            out.append(plugin.isFragment() ? "Fragment: " : "Plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Package other = (Package) obj;
        if (name == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!name.equals(other.getName())) {
            return false;
        }
        if (version == null) {
            if (other.getVersion() != null) {
                return false;
            }
        } else if (!version.equals(other.getVersion())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Package [name=");
        builder.append(name);
        builder.append(", version=");
        builder.append(version);
        builder.append("]");
        return builder.toString();
    }
}
