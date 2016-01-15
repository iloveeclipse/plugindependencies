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
 * @author obroesam
 *
 */
public class Package extends NamedElement {

    private Set<Plugin> exportedBy;

    private Set<Plugin> importedBy;

    private Set<Plugin> reexportedBy;

    private Set<Plugin> split;

    public Package(String name, String version) {
        super(name, version);
        this.exportedBy = new LinkedHashSet<>();
        this.split = new LinkedHashSet<>();
        this.importedBy = new LinkedHashSet<>();
        this.reexportedBy = new LinkedHashSet<>();
    }

    public String getInformationLine() {
        String sep = getVersion().isEmpty()? "" : " ";
        return "package: " + getName() + sep + getVersion() + "\n" + printExportedBy(1);
    }

    public Set<Plugin> getExportedBy() {
        return exportedBy;
    }

    /**
     * @return Returns plugins contributing to the "split" package.
     */
    public Set<Plugin> getSplit() {
        return split;
    }

    public void addSplitPlugin(Plugin plugin) {
        this.split.add(plugin);
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
        out.append("\t\texported by:\n");
        if (exportedBy.size() == 0) {
            out.append("\t\tJRE System Library\n");
            return out.toString();
        }
        for (Plugin plugin : exportedBy) {
            out.append("\t\t");
            out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
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
        out.append("imported by:\n");
        for (Plugin plugin : importedBy) {
            out.append("\t");
            out.append(plugin.isOptional(this) ? "*optional* for " : "");
            out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
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
        return obj instanceof Package;
    }

    public void parsingDone() {
        importedBy = importedBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(importedBy);
        reexportedBy = reexportedBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(reexportedBy);
        exportedBy = exportedBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(exportedBy);
        split = split.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(split);
    }

}
