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

import static org.eclipselabs.plugindependencies.core.PlatformState.fixVersion;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * @author obroesam
 *
 */
public class Plugin extends OSGIElement {

    public final static Plugin DUMMY_PLUGIN = new Plugin("", NamedElement.EMPTY_VERSION);

    private static final String EXTERNAL = "external:";

    private List<String> bundleClassPath;

    private List<ManifestEntry> importedPackageEntries;

    private Set<Package> exportedPackages;

    private Set<Package> reExportedPackages;

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

    private final Manifest manifest;


    public Plugin(String symbName, String vers) {
        this(null, symbName, vers, false, false);
    }

    public Plugin(String symbName, String vers, boolean fragment, boolean singleton) {
        this(null, symbName, vers, fragment, singleton);
    }

    public Plugin(Manifest manifest, String symbName, String vers, boolean fragment, boolean singleton) {
        super(symbName, fixVersion(vers));
        this.manifest = manifest;
        isSingleton = singleton;
        this.importedPackageEntries = new ArrayList<>();
        this.exportedPackages = new LinkedHashSet<>();
        this.reExportedPackages = new LinkedHashSet<>();
        this.importedPackages = new LinkedHashSet<>();
        this.fragments = new LinkedHashSet<>();
        this.isFragment = fragment;
    }



    public Plugin getHost() {
        return host;
    }

    void setHost(Plugin host) {
        if(this == host) {
            throw new IllegalArgumentException("Bundle can't be a host of itself");
        }
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
            addErrorToLog("fragment has more than one host", entries);
        }
    }

    public Version getHostVersion(){
        if(!isFragment || fragmentHostEntry == null){
            return null;
        }
        return new Version(fragmentHostEntry.getVersion());
    }

    public Set<Package> getImportedPackages() {
        return importedPackages;
    }

    public void addImportedPackage(Package importedPackage) {
        this.importedPackages.add(importedPackage);
        importedPackage.addImportedBy(this);
    }

    public List<ManifestEntry> getImportedPackageEntries() {
        return importedPackageEntries;
    }

    public void setImportedPackageEntries(String requPackages) {
        importedPackageEntries = Collections.unmodifiableList(StringUtil.splitInManifestEntries(requPackages));
    }

    public Set<Package> getExportedPackages() {
        return exportedPackages;
    }

    public Set<Package> getReExportedPackages() {
        return reExportedPackages;
    }

    public void setExportedPackages(String expPackagesString, PlatformState state) {
        List<ManifestEntry> entries = StringUtil.splitInManifestEntries(expPackagesString);
        for (ManifestEntry entry : entries) {
            Package pack = state.createPackage(entry);
            pack.addExportPlugin(this);
            if(entry.isSplit()){
                pack.addSplitPlugin(this);
            }
            exportedPackages.add(pack);
        }
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
            addErrorToLog("fragment can't have additional fragments: " + fragment, fragment);
            return;
        }
        this.fragments.add(fragment);
    }

    /**
     * Searches the reqPack in requiredPackages and returns if the reqPack is optional.
     *
     * @param reqPack
     *            reqPack that should be checked for optional
     * @return true if reqPlugin is optional
     */
    public boolean isOptional(Package reqPack) {
        if (importedPackageEntries.isEmpty()) {
            return false;
        }
        for (ManifestEntry entry : importedPackageEntries) {
            if (entry.isMatching(reqPack) && entry.isOptional()) {
                return true;
            }
        }
        return false;
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
            addWarningToLog(logEntry.toString(), packages);
        }
        if (packagesSize == 0 ) {
            StringBuilder logEntry = new StringBuilder("package not found: ");
            logEntry.append(rname + " " + rversion + optional + dynamicImport);
            if(requiredPackage.isDynamicImport()) {
                addWarningToLog(logEntry.toString(), requiredPackage);
            } else if(optional.isEmpty()){
                addErrorToLog(logEntry.toString(), requiredPackage);
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

    public void addToRecursiveResolvedPlugins(Plugin plugin, PlatformState state) {
        if(plugin == null){
            return;
        } else if (this.equals(plugin)) {
            if(isRecursiveResolved()) {
                Problem problem;
                if(state.getIgnoredBundlesWithCycles().contains(plugin.getName())) {
                    problem = new Problem("Self-dependency cycle detected", Problem.WARN, this, Collections.EMPTY_LIST);
                } else {
                    problem = new Problem("Self-dependency cycle detected", Problem.ERROR, this, Collections.EMPTY_LIST);
                }
                if(!log.contains(problem)) {
                    log.add(problem);
                }
            }
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
                    if(state.getIgnoredBundlesWithCycles().contains(plugin.getName())) {
                        addWarningToLog("plugin has cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addWarningToLog("plugin has cycle with: " + getInformationLine(), this);
                    } else {
                        addErrorToLog("plugin has cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addErrorToLog("plugin has cycle with: " + getInformationLine(), this);
                    }
                }
            } else {
                if(!isFragmentOrHost(plugin)){
                    if(state.getIgnoredBundlesWithCycles().contains(plugin.getName())) {
                        addWarningToLog("plugin has indirect cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addWarningToLog("plugin has indirect cycle with: " + getInformationLine(), this);
                    } else {
                        addErrorToLog("plugin has indirect cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addErrorToLog("plugin has indirect cycle with: " + getInformationLine(), this);
                    }
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
        fragments = fragments.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(fragments);
        reExportedPackages = computeReexportedPackages();
        exportedPackages = exportedPackages.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(exportedPackages);
        importedPackages = importedPackages.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(importedPackages);

        for (Package rp : reExportedPackages) {
            if(!exportedPackages.contains(rp)){
                for (Package ep : exportedPackages) {
                    if (ep.exactMatch(rp)) {
                        addWarningToLog("plugin exports already exported package: " + ep, ep);
                        break;
                    }
                }
            }
        }
    }

    private Set<Package> computeReexportedPackages() {
        Set<Plugin> reex = resolveRequiredReexportedRecursively(this);
        for (Plugin plugin : reex) {
            Set<Package> ex = plugin.getExportedPackages();
            reExportedPackages.addAll(ex);
        }
        if (reExportedPackages.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        for (Package pack : reExportedPackages) {
            pack.addReExportPlugin(this);
        }
        exportedPackages.addAll(reExportedPackages);
        return Collections.unmodifiableSet(reExportedPackages);
    }

    private static Set<Plugin> resolveRequiredReexportedRecursively(Plugin start) {
        Set<Plugin> reexportedPlugins = start.getRequiredReexportedPlugins();
        if(reexportedPlugins.isEmpty()){
            return Collections.EMPTY_SET;
        }
        Set<Plugin> reex = new LinkedHashSet<>();
        Set<Plugin> toTraverse = new LinkedHashSet<>(reexportedPlugins);
        while(!toTraverse.isEmpty()) {
            Plugin plugin = toTraverse.iterator().next();
            if(reex.contains(plugin) || start.equals(plugin)){
                toTraverse.remove(plugin);
                continue;
            }
            reex.add(plugin);
            toTraverse.remove(plugin);
            Set<Plugin> rrp = plugin.getRequiredReexportedPlugins();
            for (Plugin p2 : rrp) {
                if(toTraverse.contains(p2) || reex.contains(p2) || start.equals(p2)){
                    continue;
                }
                toTraverse.add(p2);
            }
        }
        return reex;
    }

    public void setResolved(PlatformState state){
        if(isRecursiveResolved()){
            return;
        }
        if(isFragment()) {
            if (host != null) {
                if (!host.isRecursiveResolved()) {
                    return;
                }
                addToRecursiveResolvedPlugins(host, state);
            }
        }
        if(recursiveResolvedPlugins == null || recursiveResolvedPlugins.isEmpty()){
            recursiveResolvedPlugins = Collections.emptySet();
        } else {
            recursiveResolvedPlugins = Collections.unmodifiableSet(recursiveResolvedPlugins);
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

    public StringBuilder dump() {
        StringBuilder out = new StringBuilder();
        out.append(isFragment() ? "fragment: " : "plugin: ");
        out.append(getName() + " " + getVersion() + "\n");
        out.append("required plugins:\n");
        for (ManifestEntry requiredPlugin : getRequiredPluginEntries()) {
            String sep = requiredPlugin.getVersion().isEmpty()? "" : " ";
            out.append("\t" + requiredPlugin.getName() + sep + requiredPlugin.getVersion());
            if (requiredPlugin.isOptional()) {
                out.append(" *optional*");
            }
            out.append("\n");
            for (Plugin resolvedPlugin : getRequiredPlugins()) {
                if (requiredPlugin.isMatching(resolvedPlugin)) {
                    out.append("\t-> " + resolvedPlugin.getName() + " "
                            + resolvedPlugin.getVersion() + "\n");
                }
            }
        }
        out.append("required packages:\n");
        for (ManifestEntry requiredPackage : getImportedPackageEntries()) {
            String sep = requiredPackage.getVersion().isEmpty()? "" : " ";
            out.append("\t" + requiredPackage.getName() + sep + requiredPackage.getVersion());
            if (requiredPackage.isDynamicImport()) {
                out.append(" *dynamicImport*");
            }
            if (requiredPackage.isOptional()) {
                out.append(" *optional*");
            }
            out.append("\n");
            for (Package resolvedPackage : getImportedPackages()) {
                if (requiredPackage.isMatching(resolvedPackage)) {
                    String sep2 = resolvedPackage.getVersion().isEmpty()? "" : " ";
                    out.append("\t->package: " + resolvedPackage.getName() + sep2
                            + resolvedPackage.getVersion() + "\n");
                    out.append("\t\texported by:\n");
                    Set<Plugin> exportedBy = resolvedPackage.getExportedBy();
                    if (exportedBy.size() == 0) {
                        out.append("\t\tJRE System Library\n");

                    } else {
                        for (Plugin plug : exportedBy) {
                            out.append("\t\t");
                            out.append(plug.isFragment() ? "fragment: " : "plugin: ");
                            out.append(plug.getName() + " " + plug.getVersion()
                            + "\n\n");
                        }
                    }
                }
            }
        }
        out.append("included in feature:\n");
        for (Feature feature : getIncludedInFeatures()) {
            out.append("\t" + feature.getName() + " " + feature.getVersion() + "\n");
        }
        out.append("required by:\n");
        for (OSGIElement neededBy : getRequiredBy()) {
            out.append("\t");
            if (neededBy.isOptional(this)) {
                out.append("*optional* for ");
            }
            out.append(neededBy.getName() + " " + neededBy.getVersion()
                    + ((neededBy instanceof Feature)? " (feature)" : "") + "\n");
        }
        out.append("exported packages:\n");
        for (Package exportedPackage : getExportedPackages()) {
            String sep = exportedPackage.getVersion().isEmpty()? "" : " ";
            out.append("\t" + exportedPackage.getName() + sep
                    + exportedPackage.getVersion());
            if (exportedPackage.getReexportedBy().contains(this)) {
                out.append(" *reexport*");
            }
            out.append("\n");
        }
        if (isFragment()) {
            out.append("fragment host:\n");
            Plugin fragmentHost = getHost();
            if(fragmentHost == null){
                out.append("<missing>\n");
            } else {
                out.append(fragmentHost.getName() + " " + fragmentHost.getVersion()
                + "\n");
            }
        } else {
            out.append("fragments:\n");
            for (Plugin fragment : getFragments()) {
                out.append("\t" + fragment.getName() + " " + fragment.getVersion()
                + "\n");
            }
        }
        return out;
    }

    /**
     * @return can return null (not all plugins have manifests!)
     */
    public Manifest getManifest() {
        return manifest;
    }
}
