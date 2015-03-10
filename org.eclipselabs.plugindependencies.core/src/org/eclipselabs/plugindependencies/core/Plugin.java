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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author obroesam
 *
 */
public class Plugin extends OSGIElement {

    private final Set<Plugin> requiredBy;

    private final Set<Package> exportedPackages;

    private List<ManifestEntry> requiredPlugins;

    private List<ManifestEntry> requiredPackages;

    private final Set<Package> importedPackages;

    private List<String> bundleClassPath;

    Set<Plugin> recursivResolvedPlugins;

    private boolean isFragment;

    private ManifestEntry fragmentHostEntry;

    private Plugin fragmentHost;

    private String targetDir;

    private String fullClassPaths;

    private final Set<Plugin> fragments;

    public Plugin(String symbName, String vers) {
        super(symbName, vers);
        this.exportedPackages = new LinkedHashSet<>();
        this.importedPackages = new LinkedHashSet<>();
        this.requiredBy = new LinkedHashSet<>();
        this.fragments = new LinkedHashSet<>();
        this.recursivResolvedPlugins = new LinkedHashSet<>();
    }

    public Set<Plugin> getRequiredBy() {
        return requiredBy;
    }

    void addRequiring(Plugin requires) {
        this.requiredBy.add(requires);
    }

    public Plugin getFragHost() {
        return fragmentHost;
    }

    void setFragHost(Plugin fragHost) {
        this.fragmentHost = fragHost;
    }

    public boolean isFragment() {
        return isFragment;
    }

    void setFragment(boolean isFragment) {
        this.isFragment = isFragment;
    }

    public ManifestEntry getFragmentHost() {
        return fragmentHostEntry;
    }

    public void setFragmentHost(String fragmentHost) {
        List<ManifestEntry> entries = StringUtil.splitInManifestEntries(fragmentHost);
        if (entries.size() < 2) {
            this.fragmentHostEntry = entries.get(0);
        } else {
            addToLog("Error: Fragment has more than one Host");
        }
    }

    public List<ManifestEntry> getRequiredPlugins() {
        return requiredPlugins;
    }

    public void setRequiredPlugins(String requplugins) {
        requiredPlugins = StringUtil.splitInManifestEntries(requplugins);
    }

    public Set<Package> getImportedPackages() {
        return importedPackages;
    }

    public void addImportedPackage(Package importedPackage) {
        this.importedPackages.add(importedPackage);
        importedPackage.addImportedBy(this);
    }

    public List<ManifestEntry> getRequiredPackages() {
        return requiredPackages;
    }

    public void setRequiredPackages(String requPackages) {
        requiredPackages = StringUtil.splitInManifestEntries(requPackages);
    }

    public Set<Package> getExportedPackages() {
        return exportedPackages;
    }

    private void addExportedPackage(Package exportedPackage) {
        this.exportedPackages.add(exportedPackage);
    }

    public void setExportedPackages(String expPackagesString) {
        List<ManifestEntry> entries = StringUtil
                .splitInManifestEntries(expPackagesString);
        Package pack;
        for (ManifestEntry entry : entries) {
            pack = new Package(entry.id, entry.getVersion());
            pack.addExportPlugin(this);
            addExportedPackage(pack);
        }
    }

    public void addReexportedPackages(Set<Package> reexportedPackages) {
        this.exportedPackages.addAll(reexportedPackages);
    }

    public List<String> getBundleClassPath() {
        return bundleClassPath;
    }

    public void setBundleClassPath(String bundleClassPath) {
        this.bundleClassPath = StringUtil.splitListOfEntries(bundleClassPath);
    }

    public String getFullClassPaths() {
        return fullClassPaths;
    }

    public void setFullClassPaths(String classPaths) {
        this.fullClassPaths = classPaths;
    }

    public String getTargetDirectory() throws IOException {
        // lazy getter
        if (targetDir == null) {
            File makefileLocal = new File(getPath() + "/Makefile.local");
            if (!makefileLocal.canRead()) {
                Logging.writeErrorOut("Can not read Makefile.local.");
                return null;
            }
            try (BufferedReader makefileReader = new BufferedReader(new FileReader(
                    makefileLocal))) {
                Properties props = new Properties();
                props.load(makefileReader);
                targetDir = props.getProperty("ECLIPSE_DEST");
            }
            if (targetDir == null) {
                targetDir = OutputCreator.targetFolder;
            }
            if (targetDir == null) {
                targetDir = "eclipse/plugins";
            }
            return targetDir;
        }
        return targetDir;
    }

    public Set<Plugin> getFragments() {
        return fragments;
    }

    public void addFragments(Plugin fragment) {
        this.fragments.add(fragment);
    }

    /**
     * Searches the reqPlugin in requiredPlugins and returns if the reqPlugin is optional.
     *
     * @param reqPlugin
     *            reqPlugin that should be checked for optional
     * @return true if reqPlugin is optional
     */
    public boolean isOptional(Plugin reqPlugin) {
        if (requiredPlugins == null) {
            return false;
        }
        for (ManifestEntry entry : requiredPlugins) {
            if (entry.isMatching(reqPlugin) && entry.isOptional()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches the reqPack in requiredPackages and returns if the reqPack is optional.
     *
     * @param reqPack
     *            reqPack that should be checked for optional
     * @return true if reqPlugin is optional
     */
    public boolean isOptional(Package reqPack) {
        if (requiredPackages == null) {
            return false;
        }
        for (ManifestEntry entry : requiredPackages) {
            if (entry.isMatching(reqPack) && entry.isOptional()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string with the Plugins that require this Plugin. If this Plugin is
     * optional for the other Plugin "*optional* for " will be printed before the Plugin.
     * <p>
     * String has the following Form:
     * <p>
     * "Is Required By: <br>
     * Plugin: SymbolicName Version Path <br>
     * Plugin: *optional* for SymbolicName Version Path"
     *
     * @return String with information about Plugins needing this
     */
    public String printRequiringThis() {
        StringBuilder out = new StringBuilder();
        if (requiredBy.size() == 0) {
            return out.toString();
        }
        out.append("Is Required By:\n");
        for (Plugin plugin : this.requiredBy) {
            out.append("\t");
            if (plugin.isOptional(this)) {
                out.append("*optional* for ");
            }
            out.append(plugin.isFragment ? "Fragment: " : "Plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    public void writePackageErrorLog(ManifestEntry requiredPackage, Set<Package> packages) {
        StringBuilder logEntry = new StringBuilder();
        String name = requiredPackage.id.trim();
        String version = requiredPackage.getVersion();
        String optional = requiredPackage.isOptional() ? " *optional*" : "";
        String dynamicImport = requiredPackage.isDynamicImport() ? " *dynamicImport*"
                : "";
        int packagesSize = packages.size();
        if (packagesSize > 1) {
            logEntry.append("Warning: More than one Package found for ");
            logEntry.append(name + " " + version + optional + dynamicImport + "\n");
            for (Package pack : packages) {
                logEntry.append("\t" + pack.getInformationLine());
            }
        }
        if (packagesSize == 0) {
            String errortype = optional.isEmpty() ? "Error: " : "Warning: ";
            logEntry.append(errortype + "Package not found: ");
            logEntry.append(name + " " + version + optional + dynamicImport);
        }
        addToLog(logEntry.toString());
    }

    @Override
    public int hashCode() {
        String symbolicName = getName();
        String version = getVersion();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
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
        String symbolicName = getName();
        String version = getVersion();
        Plugin other = (Plugin) obj;
        if (symbolicName == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!symbolicName.equals(other.getName())) {
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
}
