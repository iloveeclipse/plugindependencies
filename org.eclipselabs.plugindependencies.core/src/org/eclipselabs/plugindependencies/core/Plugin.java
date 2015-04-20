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

import static org.eclipselabs.plugindependencies.core.PlatformState.*;

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

    public final static Plugin DUMMY_PLUGIN = new Plugin("", NamedElement.EMPTY_VERSION);

    private static final String EXTERNAL = "external:";

    private List<String> bundleClassPath;

    private List<ManifestEntry> requiredPlugins;

    private List<ManifestEntry> requiredPackages;

    private Set<Plugin> requiredBy;

    private Set<Package> exportedPackages;

    private Set<Package> importedPackages;

    private Set<Plugin> fragments;

    private Set<Plugin> recursiveResolvedPlugins;

    private final boolean isFragment;

    private final boolean isSingleton;

    private ManifestEntry fragmentHostEntry;

    private Plugin host;

    private String targetDir;

    private String fullClassPaths;

    private boolean earlyStartup;


    public Plugin(String symbName, String vers) {
        this(symbName, vers, false, false);
    }

    public Plugin(String symbName, String vers, boolean fragment, boolean singleton) {
        super(symbName, fixVersion(vers));
        isSingleton = singleton;
        this.requiredPackages = new ArrayList<>();
        this.requiredPlugins = new ArrayList<>();
        this.exportedPackages = new LinkedHashSet<>();
        this.importedPackages = new LinkedHashSet<>();
        this.requiredBy = new LinkedHashSet<>();
        this.fragments = new LinkedHashSet<>();
        this.isFragment = fragment;
    }

    public Set<Plugin> getRequiredBy() {
        return requiredBy;
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

    public boolean isSingleton() {
        return isSingleton;
    }

    public boolean isHost() {
        return !isFragment() && !fragments.isEmpty();
    }

    public ManifestEntry getFragmentHost() {
        return fragmentHostEntry;
    }

    public void setFragmentHost(String fragmentHost) {
        List<ManifestEntry> entries = StringUtil.splitInManifestEntries(fragmentHost);
        if (entries.size() < 2) {
            this.fragmentHostEntry = entries.get(0);
        } else {
            addErrorToLog("fragment has more than one host");
        }
    }

    public Version getHostVersion(){
        if(!isFragment || fragmentHostEntry == null){
            return null;
        }
        return new Version(fragmentHostEntry.getVersion());
    }

    public List<ManifestEntry> getRequiredPlugins() {
        return requiredPlugins;
    }

    public void setRequiredPlugins(String requplugins) {
        requiredPlugins = Collections.unmodifiableList(StringUtil.splitInManifestEntries(requplugins));
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
        requiredPackages = Collections.unmodifiableList(StringUtil.splitInManifestEntries(requPackages));
    }

    public Set<Package> getExportedPackages() {
        return exportedPackages;
    }

    public void setExportedPackages(String expPackagesString) {
        List<ManifestEntry> entries = StringUtil.splitInManifestEntries(expPackagesString);
        for (ManifestEntry entry : entries) {
            Package pack = new Package(entry.getName(), entry.getVersion());
            pack.addExportPlugin(this);
            exportedPackages.add(pack);
        }
    }

    public void addReexportedPackages(Set<Package> reexportedPackages) {
        this.exportedPackages.addAll(reexportedPackages);
    }

    public List<String> getBundleClassPath() {
        return bundleClassPath;
    }

    public void setBundleClassPath(String bundleClassPath) {
        this.bundleClassPath = Collections.unmodifiableList(resolveExternalPath(
                StringUtil.splitListOfEntries(bundleClassPath)));
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
        return fragments;
    }

    public void addFragments(Plugin fragment) {
        if(isFragment()){
            addErrorToLog("fragment can't have additional fragments: " + fragment);
            return;
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
        if (requiredPlugins.isEmpty()) {
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
        if (requiredPackages.isEmpty()) {
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
     * Returns a string with the plugins that require this plugin. If this plugin is
     * optional for the other plugin "*optional* for " will be printed before the plugin.
     * <p>
     * String has the following form:
     * <p>
     * "is required by: <br>
     * plugin: symbolicName version path <br>
     * plugin: *optional* for symbolicName version path"
     *
     * @return string with information about plugins needing this
     */
    public String printRequiringThis() {
        StringBuilder out = new StringBuilder();
        if (requiredBy.size() == 0) {
            return out.toString();
        }
        out.append("is required by:\n");
        for (Plugin plugin : this.requiredBy) {
            out.append("\t");
            if (plugin.isOptional(this)) {
                out.append("*optional* for ");
            }
            out.append(plugin.isFragment ? "fragment: " : "plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    public void writePackageErrorLog(ManifestEntry requiredPackage, Set<Package> packages) {
        String rname = requiredPackage.getName();
        String rversion = requiredPackage.getVersion();
        String optional = requiredPackage.isOptional() ? " *optional*" : "";
        String dynamicImport = requiredPackage.isDynamicImport() ? " *dynamicImport*"
                : "";
        int packagesSize = packages.size();
        if (packagesSize > 1) {
            StringBuilder logEntry = new StringBuilder("more than one package found for ");
            logEntry.append(rname + " " + rversion + optional + dynamicImport + "\n");
            for (Package pack : packages) {
                logEntry.append("\t" + pack.getInformationLine());
            }
            addWarningToLog(logEntry.toString());
        }
        if (packagesSize == 0 ) {
            StringBuilder logEntry = new StringBuilder("package not found: ");
            logEntry.append(rname + " " + rversion + optional + dynamicImport);
            if(requiredPackage.isDynamicImport()) {
                addWarningToLog(logEntry.toString());
            } else if(optional.isEmpty()){
                addErrorToLog(logEntry.toString());
            }
        }
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
        if(isRecursiveResolved()) {
            if (plugin.containsRecursiveResolved(this)) {
                recursiveResolvedPlugins = new LinkedHashSet<>(recursiveResolvedPlugins);
                recursiveResolvedPlugins.add(plugin);
                recursiveResolvedPlugins = Collections.unmodifiableSet(recursiveResolvedPlugins);
                if(!isFragmentOrHost(plugin)){
                    addErrorToLog("plugin has cycle with: " + plugin.getInformationLine());
                    plugin.addErrorToLog("plugin has cycle with: " + getInformationLine());
                }
            } else {
                if(!isFragmentOrHost(plugin)){
                    addErrorToLog("plugin has indirect cycle with: " + plugin.getInformationLine());
                    plugin.addErrorToLog("plugin has indirect cycle with: " + getInformationLine());
                }
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

    @Override
    public void parsingDone() {
        super.parsingDone();
        requiredBy = requiredBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredBy);
        fragments = fragments.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(fragments);
        exportedPackages = exportedPackages.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(exportedPackages);
        importedPackages = importedPackages.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(importedPackages);

        if(!exportedPackages.isEmpty()){
            for (Package ip : importedPackages) {
                if(exportedPackages.contains(ip)){
                    for (ManifestEntry rp : requiredPackages) {
//                        if(rp.isMatching(ip)) {
                            if (rp.exactMatch(ip)) {
                                // the resolved packages might have different version as required (still matching however)
                                addWarningToLog("plugin imports and exports same package: " + ip);
                                break;
                            }
//                        }
                    }
                }
            }
        }
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
        return "<fragment>" + super.toString() + rs;
    }

    public void setEarlyStartup(boolean earlyStartup) {
        this.earlyStartup = earlyStartup;
    }

    public boolean isEarlyStartup() {
        return earlyStartup;
    }
}
