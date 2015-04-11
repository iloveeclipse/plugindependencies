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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author obroesam
 *
 */
public class Plugin extends OSGIElement {

    private static final String EXTERNAL = "external:";

    private final Set<Plugin> requiredBy;

    private final Set<Package> exportedPackages;

    private List<ManifestEntry> requiredPlugins;

    private List<ManifestEntry> requiredPackages;

    private final Set<Package> importedPackages;

    private List<String> bundleClassPath;

    private Set<Plugin> recursiveResolvedPlugins;

    private boolean isFragment;

    private ManifestEntry fragmentHostEntry;

    private Plugin host;

    private String targetDir;

    private String fullClassPaths;

    private final Set<Plugin> fragments;

    public Plugin(String symbName, String vers) {
        super(symbName, vers);
        this.exportedPackages = new LinkedHashSet<>();
        this.importedPackages = new LinkedHashSet<>();
        this.requiredBy = new LinkedHashSet<>();
        this.fragments = new LinkedHashSet<>();
    }

    public Plugin(String symbName, String vers, boolean fragment) {
        this(symbName, vers);
        this.isFragment = fragment;
    }

    public Set<Plugin> getRequiredBy() {
        return Collections.unmodifiableSet(requiredBy);
    }

    void addRequiring(Plugin requires) {
        this.requiredBy.add(requires);
    }

    public Plugin getHost() {
        return host;
    }

    void setHost(Plugin host) {
        this.host = host;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public boolean isHost() {
        return !isFragment() && !fragments.isEmpty();
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

    public Version getHostVersion(){
        if(!isFragment || fragmentHostEntry == null){
            return null;
        }
        return new Version(fragmentHostEntry.getVersion());
    }

    public List<ManifestEntry> getRequiredPlugins() {
        return Collections.unmodifiableList(requiredPlugins);
    }

    public void setRequiredPlugins(String requplugins) {
        requiredPlugins = StringUtil.splitInManifestEntries(requplugins);
    }

    public Set<Package> getImportedPackages() {
        return Collections.unmodifiableSet(importedPackages);
    }

    public void addImportedPackage(Package importedPackage) {
        this.importedPackages.add(importedPackage);
        importedPackage.addImportedBy(this);
    }

    public List<ManifestEntry> getRequiredPackages() {
        return Collections.unmodifiableList(requiredPackages);
    }

    public void setRequiredPackages(String requPackages) {
        requiredPackages = StringUtil.splitInManifestEntries(requPackages);
    }

    public Set<Package> getExportedPackages() {
        return Collections.unmodifiableSet(exportedPackages);
    }

    private void addExportedPackage(Package exportedPackage) {
        this.exportedPackages.add(exportedPackage);
    }

    public void setExportedPackages(String expPackagesString) {
        List<ManifestEntry> entries = StringUtil
                .splitInManifestEntries(expPackagesString);
        Package pack;
        for (ManifestEntry entry : entries) {
            pack = new Package(entry.getName(), entry.getVersion());
            pack.addExportPlugin(this);
            addExportedPackage(pack);
        }
    }

    public void addReexportedPackages(Set<Package> reexportedPackages) {
        this.exportedPackages.addAll(reexportedPackages);
    }

    public List<String> getBundleClassPath() {
        return Collections.unmodifiableList(bundleClassPath);
    }

    public void setBundleClassPath(String bundleClassPath) {
        this.bundleClassPath = resolveExternalPath(StringUtil.splitListOfEntries(bundleClassPath));
    }

    private static List<String> resolveExternalPath(List<String> paths) {
        List<String> filteredPaths = new ArrayList<>();
        for (String path : paths) {
            filteredPaths.add(resolveExternalPath(path));
        }
        return filteredPaths;
    }

    private static String resolveExternalPath(String path) {
        if(!path.contains(EXTERNAL)){
            return path;
        }
        int pathbegin = path.indexOf(EXTERNAL) + EXTERNAL.length();
        String extractedPath = path.substring(pathbegin);

        if (extractedPath.contains("$")) {
            int firstDollar = extractedPath.indexOf('$');
            int secondDollar = extractedPath.lastIndexOf('$');
            String envVariable = extractedPath.substring(firstDollar + 1, secondDollar);
            String value = System.getenv(envVariable);
            if(value == null){
                value = "$" + envVariable + "$";
            }
            extractedPath = value + extractedPath.substring(secondDollar + 1);
        }
        return extractedPath;
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
            try (FileReader makefileReader = new FileReader(makefileLocal)) {
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
        return Collections.unmodifiableSet(fragments);
    }

    public void addFragments(Plugin fragment) {
        if(isFragment()){
            throw new IllegalStateException("Fragment can not have additional fragments!");
        }
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
        String rname = requiredPackage.getName();
        String rversion = requiredPackage.getVersion();
        String optional = requiredPackage.isOptional() ? " *optional*" : "";
        String dynamicImport = requiredPackage.isDynamicImport() ? " *dynamicImport*"
                : "";
        int packagesSize = packages.size();
        if (packagesSize > 1) {
            logEntry.append("Warning: More than one Package found for ");
            logEntry.append(rname + " " + rversion + optional + dynamicImport + "\n");
            for (Package pack : packages) {
                logEntry.append("\t" + pack.getInformationLine());
            }
        }
        if (packagesSize == 0) {
            String errortype = optional.isEmpty() && !requiredPackage.isDynamicImport() ? "Error: " : "Warning: ";
            logEntry.append(errortype + "Package not found: ");
            logEntry.append(rname + " " + rversion + optional + dynamicImport);
        }
        addToLog(logEntry.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + (isFragment ? 0 : 1);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if(!(obj instanceof Plugin)){
            return false;
        }
        Plugin other = (Plugin) obj;
        if(isFragment != other.isFragment){
            return false;
        }
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        return true;
    }

    public Set<Plugin> getRecursiveResolvedPlugins() {
        if(recursiveResolvedPlugins == null){
            return Collections.emptySet();
        }
        if(recursiveResolvedPlugins instanceof LinkedHashSet){
            return Collections.unmodifiableSet(recursiveResolvedPlugins);
        }
        return recursiveResolvedPlugins;
    }

    public boolean isRecursiveResolved() {
         return recursiveResolvedPlugins != null && !(recursiveResolvedPlugins instanceof LinkedHashSet);
    }

    public boolean containsRecursiveResolved(Plugin p) {
        if(recursiveResolvedPlugins == null){
            return false;
        }
        return recursiveResolvedPlugins.contains(p);
    }

    public void addToRecursiveResolvedPlugins(Plugin plugin) {
        if(plugin == null || this.equals(plugin)){
            return;
        }
        if(recursiveResolvedPlugins == null){
            recursiveResolvedPlugins = new LinkedHashSet<>();
        }
        if(recursiveResolvedPlugins.contains(plugin)){
            return;
        }
        if(isRecursiveResolved() && plugin.containsRecursiveResolved(this)){
            recursiveResolvedPlugins = new LinkedHashSet<>(recursiveResolvedPlugins);
            recursiveResolvedPlugins.add(plugin);
            recursiveResolvedPlugins = Collections.unmodifiableSet(recursiveResolvedPlugins);
            if(!isFragmentOrHost(plugin)){
                addToLog("Error: plugin has cycle with: " + plugin.getInformationLine());
                plugin.addToLog("Error: plugin has cycle with: " + getInformationLine());
            }
            return;
        }
        recursiveResolvedPlugins.add(plugin);
        Set<Plugin> rrp = plugin.getRecursiveResolvedPlugins();
        for (Plugin child : rrp) {
            if(this != child) {
                recursiveResolvedPlugins.add(child);
            }
        }
    }

    boolean isFragmentOrHost(Plugin other){
        if(isFragment() && getHost() == other){
            return true;
        }
        return other.isFragment() && other.getHost() == this;
    }

    public void setResolved(){
        if(isRecursiveResolved()){
            return;
        }
        if(isFragment()) {
            if (host != null) {
                if (!host.isRecursiveResolved()) {
                    return;
                }
                addToRecursiveResolvedPlugins(host);
            }
        }
        if(recursiveResolvedPlugins == null || recursiveResolvedPlugins.isEmpty()){
            recursiveResolvedPlugins = Collections.emptySet();
        } else {
            recursiveResolvedPlugins = Collections.unmodifiableSet(recursiveResolvedPlugins);
        }
    }

    @Override
    public void addResolvedPlugin(Plugin plugin) {
        super.addResolvedPlugin(plugin);
        if (plugin != null) {
            plugin.addRequiring(this);
        }
    }

    @Override
    public String toString() {
        boolean resolved = isRecursiveResolved();
        String rs;
        if(resolved){
            rs = " resolved";
        } else {
            rs = " unresolved";
        }

        if(!isFragment) {
            return super.toString() + rs;
        }
        return "<Fragment>" + super.toString() + rs;
    }

}
