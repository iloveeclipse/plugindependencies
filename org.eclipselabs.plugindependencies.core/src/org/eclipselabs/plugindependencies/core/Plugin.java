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
    private List<ManifestEntry> requiredCapabilityEntries;

    private Set<Package> exportedPackages;
    private final Set<Capability> providedCapabilities;

    private Set<Package> reExportedPackages;

    private Set<Package> importedPackages;
    private Set<Capability> requiredCapabilities;

    private Set<Plugin> fragments;

    private Set<Plugin> recursiveResolvedPlugins;

    private Set<Plugin> visibleOnCompilePlugins;

    private Set<Plugin> reexportedBy;

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
        this.requiredCapabilityEntries = new ArrayList<>();
        this.exportedPackages = new LinkedHashSet<>();
        this.providedCapabilities = new LinkedHashSet<>();
        this.reExportedPackages = new LinkedHashSet<>();
        this.importedPackages = new LinkedHashSet<>();
        this.requiredCapabilities = new LinkedHashSet<>();
        this.fragments = new LinkedHashSet<>();
        this.visibleOnCompilePlugins = new LinkedHashSet<>();
        this.reexportedBy = new LinkedHashSet<>();
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
        if (entries.size() > 0) {
            this.fragmentHostEntry = entries.get(0);
            if (entries.size() > 1) {
                addErrorToLog("fragment has more than one host", entries);
            }
        } else {
            addErrorToLog("fragment has no host", entries);
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

    public Set<Capability> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void addImportedPackage(Package importedPackage) {
        this.importedPackages.add(importedPackage);
        importedPackage.addImportedBy(this);
    }

    public void addRequiredCapability(Capability requiredCapability) {
        this.requiredCapabilities.add(requiredCapability);
        requiredCapability.addRequiredBy(this);
    }

    public List<ManifestEntry> getImportedPackageEntries() {
        return importedPackageEntries;
    }

    public void setImportedPackageEntries(String requPackages) {
        importedPackageEntries = Collections.unmodifiableList(StringUtil.splitInManifestEntries(requPackages));
    }

    public List<ManifestEntry> getRequiredCapabilityEntries() {
        return requiredCapabilityEntries;
    }

    public void setRequiredCapabilityEntries(String reqCapabilities) {
        // Found in jakarta.activation-api, version=2.1.3:
        // osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.processor)(version>=1.0.0)(!(version>=2.0.0)))";resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=jakarta.activation.spi.MailcapRegistryProvider)";osgi.serviceloader="jakarta.activation.spi.MailcapRegistryProvider";cardinality:=multiple;resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=jakarta.activation.spi.MimeTypeRegistryProvider)";osgi.serviceloader="jakarta.activation.spi.MimeTypeRegistryProvider";cardinality:=multiple;resolution:=optional,osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"

        // Found in jakarta.xml.bind-api, version=4.0.2;
        // osgi.extender;filter:="(&(osgi.extender=osgi.serviceloader.processor)(version>=1.0.0)(!(version>=2.0.0)))";resolution:=optional,osgi.serviceloader;filter:="(osgi.serviceloader=jakarta.xml.bind.JAXBContextFactory)";osgi.serviceloader="jakarta.xml.bind.JAXBContextFactory";cardinality:=multiple;resolution:=optional,osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=11))"

        // Found in junit-jupiter-api, version=5.12.2:
        // org.junit.platform.engine;filter:="(&(org.junit.platform.engine=junit-jupiter)(version>=5.12.2)(!(version>=6)))";effective:=active,osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
        List<ManifestEntry> splitInManifestEntries = StringUtil.splitInManifestEntries(reqCapabilities);
        requiredCapabilityEntries = Collections.unmodifiableList(splitInManifestEntries);
    }

    public Set<Package> getExportedPackages() {
        return exportedPackages;
    }

    public Set<Capability> getProvidedCapabilities() {
        return providedCapabilities;
    }

    public Set<Package> getReExportedPackages() {
        return reExportedPackages;
    }

    public Set<Plugin> getVisibleOnCompilePlugins() {
        return visibleOnCompilePlugins;
    }

    public Set<Plugin> getReexportedBy() {
        return reexportedBy;
    }

    public void addReExportPlugin(Plugin plugin) {
        this.reexportedBy.add(plugin);
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

    public void setProvidedCapabilities(String providedCapabilityString, PlatformState state) {
        List<ManifestEntry> entries = StringUtil.splitInManifestEntries(providedCapabilityString);
        for (ManifestEntry entry : entries) {
            Capability cap = state.createCapability(entry);
            cap.addProvidingPlugin(this);
            providedCapabilities.add(cap);
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
            File makefileLocal = new File(getPath() + "/build.properties");
            if (!makefileLocal.canRead()) {
                return targetDir;
            }
            try (FileReader makefileReader = new FileReader(makefileLocal)) {
                Properties props = new Properties();
                props.load(makefileReader);
                targetDir = props.getProperty("bundleDestination");
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

    /**
     * Searches the capability in required and returns if the capability is optional.
     *
     * @param capability
     *            capability that should be checked for optional
     * @return true if capability is optional
     */
    public boolean isOptional(Capability capability) {
        if (requiredCapabilityEntries.isEmpty()) {
            return false;
        }
        for (ManifestEntry entry : requiredCapabilityEntries) {
            if (entry.isMatching(capability) && entry.isOptional()) {
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

    public void writeCapabilityErrorLog(ManifestEntry requiredCapability, Set<Capability> capabilities) {
        String rname = requiredCapability.getName();
        String rversion = requiredCapability.getVersion();
        String optional = requiredCapability.isOptional() ? " *optional*" : "";
        int capabilitiesSize = capabilities.size();

        if (capabilitiesSize == 0 ) {
            StringBuilder logEntry = new StringBuilder("capability not found: ");
            logEntry.append(rname + " " + rversion + optional);
            String capabilityFilter = requiredCapability.getCapabilityFilter();
            if(capabilityFilter != null) {
                logEntry.append(" " + capabilityFilter);
                if(capabilityFilter.contains("osgi.ee")) {
                    // TODO check for EE version
                } else {
                    addWarningToLog(logEntry.toString(), requiredCapability);
                }
            } else if(optional.isEmpty()){
                addErrorToLog(logEntry.toString(), requiredCapability);
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
                if(state.shouldIgnoreCycleError(this.getName())) {
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
                    if(state.shouldIgnoreCycleError(plugin.getName(), this.getName())) {
                        addWarningToLog("plugin has cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addWarningToLog("plugin has cycle with: " + getInformationLine(), this);
                    } else {
                        addErrorToLog("plugin has cycle with: " + plugin.getInformationLine(), plugin);
                        plugin.addErrorToLog("plugin has cycle with: " + getInformationLine(), this);
                    }
                }
            } else {
                if(!isFragmentOrHost(plugin)){
                    if(state.shouldIgnoreCycleError(plugin.getName(), this.getName())) {
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
        requiredCapabilities = requiredCapabilities.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredCapabilities);
        reexportedBy = reexportedBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(reexportedBy);

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

        computeClasspath();

        if(recursiveResolvedPlugins == null || recursiveResolvedPlugins.isEmpty()){
            recursiveResolvedPlugins = Collections.emptySet();
        } else {
            recursiveResolvedPlugins = Collections.unmodifiableSet(recursiveResolvedPlugins);
        }

        if(isHost()) {
            Set<Plugin> frs = getFragments();
            for (Plugin fragment : frs) {
                if(!fragment.isRecursiveResolved()) {
                    fragment.setResolved(state);
                }
            }
        }
    }

    /**
     * Collect compilation visible dependencies:
     * <ol>
     * <li>all direct required plugins including fragments
     * <li>plus all plugins that are re-exported by direct dependencies.
     * <li>including host if it is a fragment
     * <li>plus plugins that host packages that are imported directly
     * <li>plus all plugins that contribute split packages to packages exported by all plugins we had already
     * </ol>
     *
     * @see DependencyResolver.PluginElt#addDirectDependencies()
     */
    private void computeClasspath() {

        // org.eclipse.ui.workbench.texteditor exports org.eclipse.ui.texteditor *split* package
        // the same package exported by org.eclipse.ui.editors bundle
        // org.eclipse.ui.editors bundle is exported by org.eclipse.xtext.ui
        // and plugin that requires only XtextEditor.getEditorSite() must see super class StatusTextEditor
        // that is inside org.eclipse.ui.workbench.texteditor

        // 1 all direct required plugins including fragments
        // 2 plus all plugins that are re-exported by direct dependencies.
        for (Plugin required : getRequiredPlugins()) {
            addPluginWithAndReexported(visibleOnCompilePlugins, required);
        }

        // 3 including host if it is a fragment
        if(isFragment() && getHost() != null) {
            visibleOnCompilePlugins.add(getHost());
        }

        // 4 plus plugins that host packages that are imported directly
        addPluginsForImportedPackages(this, visibleOnCompilePlugins);

        // plus all plugins that contribute split packages to packages exported by all plugins we had already
        addPluginsForExportedPackages(this, visibleOnCompilePlugins);

        // 4 plus plugins that host packages that are imported directly
        addPluginsForRequiredCapabilities(this, visibleOnCompilePlugins);

        // paranoia
        visibleOnCompilePlugins.remove(this);

        visibleOnCompilePlugins = visibleOnCompilePlugins.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(visibleOnCompilePlugins);
    }

    void addPluginWithAndReexported(Set<Plugin> plugins, Plugin toAdd) {
        plugins.add(toAdd);
        if(toAdd.isHost()) {
            Set<Plugin> fragmentSet = toAdd.getFragments();
            for (Plugin fr : fragmentSet) {
                plugins.add(fr);
            }
        }
        Set<Plugin> requiredReexportedPlugins = toAdd.getRequiredReexportedPlugins();
        for (Plugin req : requiredReexportedPlugins) {
            if(!plugins.contains(req)) {
                addPluginWithAndReexported(plugins, req);
            }
        }
    }

    // TODO throw away "duplicated" bundles with different versions, exporting same package
    static void addPluginsForImportedPackages(Plugin p, Set<Plugin> exporting) {
        for (Package imported : p.getImportedPackages()) {
            Set<Plugin> exportedBy = imported.getExportedBy();
            if(exportedBy.contains(p)){
                // do not add dependencies for packages the plugin exports by itself
                continue;
            }
            if(!exportedBy.isEmpty()) {
                exporting.addAll(exportedBy);
            } else {
                Set<Plugin> reexportedBy = imported.getReexportedBy();
                exporting.addAll(reexportedBy);
            }
            Set<Plugin> split = imported.getSplit();
            exporting.addAll(split);
        }

        // fragment inherits all dependencies from host
        if(p.isFragment() && p.getHost() != null){
            addPluginsForImportedPackages(p.getHost(), exporting);
        }
        // paranoia, to avoid cycles
        exporting.remove(p);
    }

    // TODO throw away "duplicated" bundles with different versions, exporting same package
    static void addPluginsForRequiredCapabilities(Plugin p, Set<Plugin> exporting) {
        for (Capability required : p.getRequiredCapabilities()) {
            Set<Plugin> exportedBy = required.getProvidedBy();
            if(exportedBy.contains(p)){
                // do not add dependencies for packages the plugin exports by itself
                continue;
            }
            if(!exportedBy.isEmpty()) {
                exporting.addAll(exportedBy);
            }
        }

        // fragment inherits all dependencies from host
        if(p.isFragment() && p.getHost() != null){
            addPluginsForRequiredCapabilities(p.getHost(), exporting);
        }
        // paranoia, to avoid cycles
        exporting.remove(p);
    }

    static void addPluginsForExportedPackages(Plugin p, Set<Plugin> exporting) {
        LinkedHashSet<Plugin> extra = new LinkedHashSet<>();
        for (Plugin pl : exporting) {
            Set<Package> packages = pl.getExportedPackages();
            Set<Plugin> requiredByPl = pl.getRequiredPlugins();

            for (Package pack : packages) {
                Set<Plugin> split = pack.getSplit();
                for (Plugin plugin : split) {
                    if(pl == plugin) {
                        continue;
                    }
                    if(!exporting.contains(plugin)) {
                        extra.add(plugin);
                    }
                }
                Set<Plugin> exportedBy = pack.getExportedBy();
                for (Plugin plugin : exportedBy) {
                    if(pl == plugin) {
                        continue;
                    }
                    if(!exporting.contains(plugin) && requiredByPl.contains(plugin)) {
                        extra.add(plugin);
                    }
                }
            }
        }
        exporting.addAll(extra);
        // paranoia, to avoid cycles
        exporting.remove(p);
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
        for (ManifestEntry requiredCapability : getRequiredCapabilityEntries()) {
            String sep = requiredCapability.getVersion().isEmpty()? "" : " ";
            out.append("\t" + requiredCapability.getName() + sep + requiredCapability.getVersion());
            if (requiredCapability.isOptional()) {
                out.append(" *optional*");
            }
            out.append("\n");
            for (Capability resolvedCapability : getRequiredCapabilities()) {
                if (requiredCapability.isMatching(resolvedCapability)) {
                    String sep2 = resolvedCapability.getVersion().isEmpty()? "" : " ";
                    out.append("\t->capability: " + resolvedCapability.getName() + sep2
                            + resolvedCapability.getVersion() + "\n");
                    out.append("\t\tprovided by:\n");
                    Set<Plugin> exportedBy = resolvedCapability.getProvidedBy();
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
