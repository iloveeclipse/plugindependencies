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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * @author obroesam
 *
 */
public class DependencyResolver {

    private static final String ZERO_VERSION = "0.0.0";

    private final Set<Plugin> pluginSet;

    private final Set<Package> packageSet;

    private final Set<Feature> featureSet;

    public DependencyResolver(Set<Plugin> plugins, Set<Package> packages,
            Set<Feature> features) {
        pluginSet = plugins;
        packageSet = packages;
        featureSet = features;
    }

    public void resolveFeatureDependency(Feature feature) {
        for (ManifestEntry requiredFeature : feature.getRequiredFeatures()) {
            resolveRequiredFeature(feature, requiredFeature);
        }

        for (ManifestEntry requiredPlugin : feature.getRequiredPlugins()) {
            if (requiredPlugin.isLinuxgtkx86_64()) {
                resolveRequiredPlugin(feature, requiredPlugin);
            }
        }
    }

    public void resolvePluginDependency(Plugin startPlugin) {
        if (startPlugin.isFragment()) {
            searchHost(startPlugin);
        }

        for (ManifestEntry requiredPlugin : startPlugin.getRequiredPlugins()) {
            resolveRequiredPlugin(startPlugin, requiredPlugin);
        }

        for (ManifestEntry requiredPackage : startPlugin.getRequiredPackages()) {
            resolveRequiredPackage(startPlugin, requiredPackage);
        }
    }

    private void resolveRequiredFeature(Feature feature, ManifestEntry requiredFeature) {
        Set<Feature> features;
        features = searchInFeatureSet(requiredFeature);
        if (features.size() != 1) {
            feature.writeErrorLog(requiredFeature, features, "feature");
        }
        feature.addIncludedFeatures(features);
    }

    private void resolveRequiredPlugin(OSGIElement elt, ManifestEntry requiredPlugin) {
        boolean elementIsPlugin = elt instanceof Plugin;
        Plugin highVersionPlugin = null;

        Set<Plugin> plugins = searchInPluginSet(requiredPlugin, !elementIsPlugin);
        int setSize = plugins.size();
        if (setSize >= 1) {
            highVersionPlugin = getPluginWithHighestVersion(plugins);
        }

        if (setSize != 1) {
            elt.writeErrorLog(requiredPlugin, plugins, "plugin");
        }

        if (highVersionPlugin != null) {
            elt.addResolvedPlugin(highVersionPlugin);

            if (requiredPlugin.isReexport()) {
                Set<Package> reexportPackages = highVersionPlugin.getExportedPackages();
                ((Plugin) elt).addReexportedPackages(reexportPackages);
                for (Package reexported : reexportPackages) {
                    reexported.addReExportPlugin((Plugin) elt);
                }
            }
        }
    }

    private static Plugin getPluginWithHighestVersion(Set<Plugin> plugins) {
        Plugin highestPlugin = plugins.iterator().next();
        for (Plugin plugin : plugins) {
            if (compareVersions(plugin.getVersion(), highestPlugin.getVersion()) > 0) {
                highestPlugin = plugin;
            }
        }
        return highestPlugin;
    }

    private static Package getPackageWithHighestVersion(Set<Package> packages) {
        Package highestPackage = packages.iterator().next();
        for (Package pack : packages) {
            if (compareVersions(pack.getVersion(), highestPackage.getVersion()) > 0) {
                highestPackage = pack;
            }
        }
        return highestPackage;
    }

    private void resolveRequiredPackage(Plugin startPlugin, ManifestEntry requiredPackage) {
        Set<Package> packages = searchInPackageSet(requiredPackage);
        Package importedPackage = null;
        int packagesSize = packages.size();

        if (packagesSize >= 1) {
            importedPackage = getPackageWithHighestVersion(packages);
        }

        if (packagesSize != 1) {
            startPlugin.writePackageErrorLog(requiredPackage, packages);
        }

        if (importedPackage != null) {
            startPlugin.addImportedPackage(importedPackage);
        }
    }

    public void searchHost(Plugin fragment) {
        ManifestEntry entry = fragment.getFragmentHost();
        Set<Plugin> resultSet = searchInPluginSet(entry, false);
        int setSize = resultSet.size();

        if (setSize >= 1) {
            Plugin fragmentHost = getPluginWithHighestVersion(resultSet);
            fragment.setHost(fragmentHost);
            fragmentHost.addFragments(fragment);
        }
        if (setSize != 1) {
            fragment.writeErrorLog(entry, resultSet, "fragment host");
        }
    }

    public Set<Package> searchInPackageSet(ManifestEntry requiredPackage) {
        if (requiredPackage == null) {
            return Collections.emptySet();
        }
        Set<Package> ret = new LinkedHashSet<Package>();
        for (Package pack : packageSet) {
            if (requiredPackage.isMatching(pack)) {
                ret.add(pack);
            }
        }

        if (ret.isEmpty()) {
            try {
                Package p = searchInJavaHomeJar(requiredPackage);
                if (p != null) {
                    ret.add(p);
                }
            } catch (IOException e) {
                Logging.getLogger().error(" failed to read libraries from '$JAVA_HOME' (" + PlatformState.getJavaHome() + ").", e);
            }
        }
        return ret;
    }

    private final static String DEFAULT_JAVA_HOME = System.getProperty("java.home");

    public Package searchInJavaHomeJar(ManifestEntry requiredPackage) throws IOException {
        if (requiredPackage == null || requiredPackage.getName().isEmpty()) {
            return null;
        }

        String packageName = requiredPackage.getName().trim();
        String javaHome = PlatformState.getJavaHome();

        if (javaHome == null || javaHome.isEmpty()) {
            javaHome = DEFAULT_JAVA_HOME;
            PlatformState.setJavaHome(javaHome);
        }
        File javaHomeLib = new File(javaHome + "/lib");
        if (!javaHomeLib.exists()) {
            Logging.writeErrorOut("specified $JAVA_HOME (" + javaHome + ") does not exist. Changing to " + DEFAULT_JAVA_HOME);
            javaHome = DEFAULT_JAVA_HOME;
            PlatformState.setJavaHome(javaHome);
        }

        File[] jarList = javaHomeLib.listFiles(new JarFilter());
        if(jarList == null){
            return null;
        }
        String packagePath = packageName.replace('.', '/') + "/";
        for (File jar : jarList) {
            try (JarFile jarfile = new JarFile(jar)) {
                Enumeration<JarEntry> jarEntries = jarfile.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry entry = jarEntries.nextElement();
                    if (entry.getName().contains(packagePath)) {
                        Package p = new Package(packageName, "");
                        packageSet.add(p);
                        return p;
                    }
                }
            }
        }
        return null;
    }

    static class JarFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".jar")) {
                return true;
            }
            return false;
        }
    }

    /**
     * Stack element for resolving plugin dependencies. If {@link #toVisit} is empty, plugin is resolved.
     */
    static class PluginElt {
        static final Plugin DUMMY_PLUGIN = new Plugin("", NamedElement.EMPTY_VERSION);
        static final PluginElt EMPTY = new PluginElt(null,  DUMMY_PLUGIN);
        final Plugin plugin;
        final PluginElt parent;

        List<Plugin> toVisit;

        public PluginElt(PluginElt parent, Plugin plugin) {
            super();
            this.parent = parent;
            this.plugin = plugin;
            toVisit = new LinkedList<>();
            addDirectDependencies();
        }

        void setResolved(){
            plugin.setResolved();
            resolved(plugin);
        }

        void resolved(Plugin p){
            toVisit.remove(p);
            if(plugin != p){
                plugin.addToRecursiveResolvedPlugins(p);
            }

            if(parent != null && parent != this){
                parent.resolved(p);
            }
        }

        PluginElt next(){
            if(toVisit.isEmpty()){
                return EMPTY;
            }
            return new PluginElt(this, toVisit.remove(0));
        }

        void addToVisit(Plugin p){
            if(p == null || plugin.equals(p)){
                return;
            }
            if(!toVisit.contains(p)) {
                toVisit.add(p);
            }
        }

        void addToVisit(Collection<Plugin> plugins){
            for (Plugin p : plugins) {
                addToVisit(p);
            }
        }

        void addDirectDependencies(){
            for (Plugin pluginToVisit : plugin.getResolvedPlugins()) {
                addToVisit(pluginToVisit);
            }
            if(plugin.isFragment()) {
                addToVisit(plugin.getHost());
            } else {
                for (Plugin fragment : plugin.getFragments()) {
                    // fragments can have extra dependencies we want to visit
                    addToVisit(fragment);
                }
            }

            // TODO throw away "duplicated" bundles with different versions, exporting same package
            Set<Plugin> allExporting = new LinkedHashSet<>();
            addPluginsForImportedPackages(plugin, allExporting);
            // fragment inherits all dependencies from host
            if(plugin.isFragment() && plugin.getHost() != null){
                addPluginsForImportedPackages(plugin.getHost(), allExporting);
            }
            allExporting.remove(plugin);
            addToVisit(allExporting);
        }

        static void addPluginsForImportedPackages(Plugin p, Set<Plugin> allExporting) {
            for (Package imported : p.getImportedPackages()) {
                Set<Plugin> exportedBy = imported.getExportedBy();
                if(!exportedBy.isEmpty()) {
                    allExporting.addAll(exportedBy);
                } else {
                    Set<Plugin> reexportedBy = imported.getReexportedBy();
                    allExporting.addAll(reexportedBy);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[plugin=");
            sb.append(plugin);
            sb.append(", toVisit=");
            sb.append(toVisit);
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return plugin.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PluginElt)) {
                return false;
            }
            PluginElt other = (PluginElt) obj;
            if (!plugin.equals(other.plugin)) {
                return false;
            }
            return true;
        }

    }

    public Set<Plugin> searchInPluginSet(ManifestEntry requiredPlugin,  boolean exactVersion) {
        if (requiredPlugin == null) {
            return Collections.emptySet();
        }
        String pluginName = requiredPlugin.getName().trim();
        String version = requiredPlugin.getVersion();
        Set<Plugin> ret = new LinkedHashSet<Plugin>();

        if (exactVersion) {
            for (Plugin plugin : pluginSet) {
                if (plugin.matches(pluginName, version)) {
                    ret.add(plugin);
                }
            }
        } else {
            for (Plugin plugin : pluginSet) {
                if (requiredPlugin.isMatching(plugin)) {
                    ret.add(plugin);
                }
            }
        }
        return ret;
    }

    public Set<Feature> searchInFeatureSet(ManifestEntry entry) {
        if (entry == null) {
            return Collections.emptySet();
        }
        String id = entry.getName();
        String version = entry.getVersion();
        Set<Feature> ret = new LinkedHashSet<Feature>();

        for (Feature feature : featureSet) {
            if (feature.matches(id, version)) {
                ret.add(feature);
            }
        }
        return ret;
    }

    public static void resolveDependencies() {
        DependencyResolver depres = new DependencyResolver(PlatformState.getPluginSet(), PlatformState.getPackageSet(), PlatformState.getFeatureSet());
    
        for (Plugin plugin : PlatformState.getPluginSet()) {
            depres.resolvePluginDependency(plugin);
        }
        for (Feature feature : PlatformState.getFeatureSet()) {
            depres.resolveFeatureDependency(feature);
        }
        for (Plugin plugin : PlatformState.getPluginSet()) {
            plugin.parsingDone();
        }
        for (Feature feature : PlatformState.getFeatureSet()) {
            feature.parsingDone();
        }
    }

    public static Set<Plugin> computeResolvedPluginsRecursive(final Plugin root) {
        if(root.isRecursiveResolved()){
            return root.getRecursiveResolvedPlugins();
        }
        Stack<DependencyResolver.PluginElt> stack = new Stack<>();
        stack.add(new DependencyResolver.PluginElt(null, root));
        while (!stack.isEmpty()){
            DependencyResolver.PluginElt current = stack.peek();
            DependencyResolver.PluginElt next = current.next();

            if(next == DependencyResolver.PluginElt.EMPTY) {
                stack.pop();
                // make sure we finished the iteration and replace the default empty set if no dependencies found
                current.setResolved();
                continue;
            }

            if(stack.contains(next)){
                if(!next.plugin.isRecursiveResolved()){
                    Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                    for (Plugin p : resolvedPlugins) {
                        current.resolved(p);
                    }
                }
                // avoid cyclic dependencies
                continue;
            }

            if(root.containsRecursiveResolved(next.plugin)) {
                Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                for (Plugin p : resolvedPlugins) {
                    current.resolved(p);
                }
                current.resolved(next.plugin);
            } else {
                stack.push(next);
            }
        }
        Set<Plugin> rrp = root.getRecursiveResolvedPlugins();
        for (Plugin plugin : rrp) {
            if(!plugin.isRecursiveResolved()){
                if(plugin.isFragment()){
                    plugin.setResolved();
                }
            }
            if(!plugin.isRecursiveResolved()) {
                throw new IllegalStateException("Unable to resolve: " + plugin);
            }
        }
        return rrp;
    }

    static final String VER_STR = "[0-9]+([.][0-9]+)?([.][0-9]+)?([.][0-9a-zA-Z-_]+)?";

    static final Pattern VERSION = Pattern.compile(VER_STR);

    static final Pattern RANGE = Pattern.compile("[(|\\[]" + VER_STR + "," + VER_STR + "[)|\\]]");

    public static boolean isCompatibleVersion(String rangeOrLowerLimit,  String givenVersion) {
        if (rangeOrLowerLimit == null || givenVersion == null) {
            Logging.writeErrorOut("version can't be null");
            return false;
        }
        if (givenVersion.isEmpty()) {
            givenVersion = ZERO_VERSION;
        } else {
            if (!VERSION.matcher(givenVersion).matches()) {
                return false;
            }
        }
        boolean isRange = false;
        if (rangeOrLowerLimit.isEmpty()) {
            rangeOrLowerLimit = ZERO_VERSION;
        } else {
            isRange = RANGE.matcher(rangeOrLowerLimit).matches();
            if (isRange) {
                return isVersionInRange(givenVersion, rangeOrLowerLimit);
            }
        }
        boolean isVersion = VERSION.matcher(rangeOrLowerLimit).matches();
        if (isVersion) {
            return compareVersions(rangeOrLowerLimit, givenVersion) <= 0 ? true : false;
        }
        return false;
    }

    private static boolean isVersionInRange(String version, String versionRange) {
        String lowerBorder = versionRange.split(",")[0];
        char leftBorderType = lowerBorder.charAt(0);
        lowerBorder = lowerBorder.substring(1);
        int lowerBorderCheck = compareVersions(lowerBorder, version);

        if (lowerBorderCheck < 0 || (lowerBorderCheck == 0 && leftBorderType == '[')) {

            String upperBorder = versionRange.split(",")[1];
            char rightBorderType = upperBorder.charAt(upperBorder.length() - 1);
            upperBorder = upperBorder.substring(0, upperBorder.length() - 1);

            int upperBorderCheck = compareVersions(version, upperBorder);
            if (upperBorderCheck < 0 || (upperBorderCheck == 0 && rightBorderType == ']')) {
                return true;
            }
        }
        return false;
    }

    private static int compareVersions(String v1, String v2) {
        if (v1.isEmpty()) {
            v1 = ZERO_VERSION;
        }
        if (v2.isEmpty()) {
            v2 = ZERO_VERSION;
        }
        Version version1 = new Version(v1);
        Version version2 = new Version(v2);
        return version1.compareTo(version2);
    }
}
