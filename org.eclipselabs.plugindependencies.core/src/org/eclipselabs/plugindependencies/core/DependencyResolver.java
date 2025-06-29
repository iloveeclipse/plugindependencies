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

import static org.eclipselabs.plugindependencies.core.NamedElement.ZERO_VERSION;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author obroesam
 *
 */
public class DependencyResolver {

    private final PlatformState state;

    public DependencyResolver(PlatformState state) {
        this.state = state;
    }

    /**
     * For tests only!
     */
    public DependencyResolver(Set<Plugin> pluginSet, Set<Package> packageSet, Set<Feature> featureSet, Set<Capability> capabilities) {
        this(new PlatformState(pluginSet, packageSet, featureSet, capabilities));
        state.resolveDependencies();
    }

    public void resolveFeatureDependency(Feature feature) {
        for (ManifestEntry included : feature.getIncludedFeatureEntries()) {
            resolveIncludedFeature(feature, included);
        }
        for (ManifestEntry included : feature.getIncludedPluginEntries()) {
            if (included.isMatchingPlatform(state.getPlatformSpecs())) {
                resolveIncludedPlugin(feature, included);
            }
        }
        for (ManifestEntry requiredFeature : feature.getRequiredFeatureEntries()) {
            resolveRequiredFeature(feature, requiredFeature);
        }
        for (ManifestEntry requiredPlugin : feature.getRequiredPluginEntries()) {
            if (requiredPlugin.isMatchingPlatform(state.getPlatformSpecs())) {
                resolveRequiredPlugin(feature, requiredPlugin);
            }
        }
    }

    public void resolvePluginDependency(Plugin startPlugin) {
        if (startPlugin.isFragment()) {
            searchHost(startPlugin);
        }

        checkDuplicates(startPlugin);

        for (ManifestEntry requiredPlugin : startPlugin.getRequiredPluginEntries()) {
            resolveRequiredPlugin(startPlugin, requiredPlugin);
        }

        for (ManifestEntry requiredPackage : startPlugin.getImportedPackageEntries()) {
            resolveRequiredPackage(startPlugin, requiredPackage);
        }

        for (ManifestEntry requiredCapability : startPlugin.getRequiredCapabilityEntries()) {
            resolveRequiredCapability(startPlugin, requiredCapability);
        }
    }

    private void checkDuplicates(Plugin startPlugin) {
        Set<Plugin> plugins = searchInPluginSet(startPlugin);
        if (plugins.size() > 1) {
            StringBuilder logStr = new StringBuilder();
            logStr.append("more than one plugin found for ");
            logStr.append(startPlugin.getName() + "\n");
            for (Plugin element : plugins) {
                logStr.append("\t" + element.getInformationLine() + "\n");
            }
            startPlugin.addWarningToLog(logStr.toString(), plugins);
        }
    }

    private void resolveRequiredFeature(Feature feature, ManifestEntry requiredFeature) {
        Set<Feature> features;
        features = searchInFeatureSet(requiredFeature, false);
        if (features.size() == 0) {
            feature.logBrokenEntry(requiredFeature, features, "feature");
        }
        int setSize = features.size();
        Feature highVersionFeature = null;
        if (setSize > 0) {
            highVersionFeature = getFeatureWithHighestVersion(features);
        }
        if(highVersionFeature != null) {
            feature.addRequiredFeature(highVersionFeature);
        }
    }

    private void resolveIncludedFeature(Feature feature, ManifestEntry includedFeature) {
        Set<Feature> features;
        features = searchInFeatureSet(includedFeature, true);
        if (features.size() != 1) {
            feature.logBrokenEntry(includedFeature, features, "feature");
        }
        int setSize = features.size();
        Feature highVersionFeature = null;
        if (setSize >= 1) {
            highVersionFeature = getFeatureWithHighestVersion(features);
        }
        if(highVersionFeature != null) {
            feature.addIncludedFeature(highVersionFeature);
        }
    }

    private void resolveRequiredPlugin(OSGIElement elt, ManifestEntry requiredPlugin) {
        Plugin highVersionPlugin = null;

        Set<Plugin> plugins = searchInPluginSet(requiredPlugin, false);
        int setSize = plugins.size();
        if (setSize >= 1) {
            highVersionPlugin = getPluginWithHighestVersion(plugins);
        }

        if (setSize != 1) {
            elt.logBrokenEntry(requiredPlugin, plugins, "plugin");
        }

        if (highVersionPlugin != null) {
            boolean reexport = requiredPlugin.isReexport();
            elt.addRequiredPlugin(highVersionPlugin, reexport);
            if(reexport && elt instanceof Plugin) {
                highVersionPlugin.addReExportPlugin((Plugin)elt);
            }
        }
    }

    private void resolveIncludedPlugin(Feature elt, ManifestEntry includedPlugin) {
        Plugin highVersionPlugin = null;

        Set<Plugin> plugins = searchInPluginSet(includedPlugin, true);
        int setSize = plugins.size();
        if (setSize >= 1) {
            highVersionPlugin = getPluginWithHighestVersion(plugins);
        }

        if (setSize != 1) {
            elt.logBrokenEntry(includedPlugin, plugins, "plugin");
        }

        if (highVersionPlugin != null) {
            elt.addIncludedPlugin(highVersionPlugin);
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

    private static Feature getFeatureWithHighestVersion(Set<Feature> features) {
        Feature highestFeature = features.iterator().next();
        for (Feature feature : features) {
            if (compareVersions(feature.getVersion(), highestFeature.getVersion()) > 0) {
                highestFeature = feature;
            }
        }
        return highestFeature;
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

    private static Capability getCapabilityWithHighestVersion(Set<Capability> capabilities) {
        Capability highestCapability = capabilities.iterator().next();
        for (Capability cap : capabilities) {
            if (compareVersions(cap.getVersion(), highestCapability.getVersion()) > 0) {
                highestCapability = cap;
            }
        }
        return highestCapability;
    }

    private void resolveRequiredPackage(Plugin startPlugin, ManifestEntry requiredPackage) {
        Set<Package> packages = searchInPackageSet(requiredPackage);
        if(packages.isEmpty()){
            packages = searchInJavaHomeJar(requiredPackage);
            if(!packages.isEmpty()) {
                state.addPackage(packages.iterator().next());
            }
        }
        int packagesSize = packages.size();

        if (packagesSize > 0) {
            Package importedPackage;
            if (packagesSize == 1) {
                importedPackage = packages.iterator().next();
            } else {
                importedPackage = getPackageWithHighestVersion(packages);
            }
            startPlugin.addImportedPackage(importedPackage);
        } else {
            startPlugin.writePackageErrorLog(requiredPackage, packages);
        }
    }

    private void resolveRequiredCapability(Plugin startPlugin, ManifestEntry requiredCapabilityEntry) {
        // required capability with same name might have different version and filters added
        // so we need to create a new Capability instance with all extra data from the ManifestEntry
        // Write a function to create new Capability instance with extra data from ManifestEntry
        Capability requiredCapability = null;
        if (requiredCapabilityEntry != null) {
            requiredCapability = new Capability(requiredCapabilityEntry.getName(), requiredCapabilityEntry.getVersion());
            Filter filter = new Filter(requiredCapabilityEntry.getCapabilityFilter());
            startPlugin.getFilterMap().put(requiredCapability, filter);
        }
        Set<Capability> capabilities = searchInCapabilitiesSet(requiredCapability);
//        TODO this could something coming from p2 metadata in the config area
//        if(packages.isEmpty()){
//            packages = searchInJavaHomeJar(requiredPackage);
//            if(!packages.isEmpty()) {
//                state.addPackage(packages.iterator().next());
//            }
//        }
        int capabilitiesSize = capabilities.size();

        if (requiredCapability != null) {
            // TODO find *matching* capability (filter?)
//            if (capabilitiesSize == 1) {
//                requiredCapability = capabilities.iterator().next();
//            } else {
//                requiredCapability = getCapabilityWithHighestVersion(capabilities);
//            }
            startPlugin.addRequiredCapability(requiredCapability);
        } else {
            startPlugin.writeCapabilityErrorLog(requiredCapabilityEntry, capabilities);
        }
    }

    public void searchHost(Plugin fragment) {
        ManifestEntry entry = fragment.getFragmentHost();
        Set<Plugin> resultSet = searchInPluginSet(entry, false);
        int setSize = resultSet.size();

        Plugin fragmentHost = null;
        if (setSize == 1) {
            fragmentHost = resultSet.iterator().next();
        } else {
            if(setSize > 1){
                fragmentHost = getPluginWithHighestVersion(resultSet);
            }
            fragment.logBrokenEntry(entry, resultSet, "fragment host");
        }
        if(fragmentHost != null){
            fragment.setHost(fragmentHost);
            fragmentHost.addFragments(fragment);
        }
    }

    public Set<Package> searchInPackageSet(ManifestEntry requiredPackage) {
        if (requiredPackage == null) {
            return Collections.emptySet();
        }
        Set<Package> ret = new LinkedHashSet<>();
        for (Package pack : state.getPackages(requiredPackage.getName())) {
            if (requiredPackage.isMatching(pack)) {
                ret.add(pack);
            }
        }
        return ret;
    }

    public Set<Capability> searchInCapabilitiesSet(Capability requiredCapability) {
        if (requiredCapability == null) {
            return Collections.emptySet();
        }
        Set<Capability> ret = new LinkedHashSet<>();
        for (Capability cap : state.getCapabilities(requiredCapability.getName())) {
            if (requiredCapability.getName().equals(cap.getName())) {
                ret.add(cap);
            }
        }
        return ret;
    }

    public Set<Package> searchInJavaHomeJar(ManifestEntry requiredPackage) {
        if (requiredPackage == null || requiredPackage.getName().isEmpty()) {
            return Collections.emptySet();
        }

        String packageName = requiredPackage.getName().trim();
        return state.searchInJavaHome(packageName);
    }


    /**
     * Stack element for resolving plugin dependencies. If {@link #toVisit} is empty, plugin is resolved.
     */
    static class PluginElt {
        static final PluginElt EMPTY = new PluginElt(null, Plugin.DUMMY_PLUGIN);
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

        void setResolved(PlatformState state){
            plugin.setResolved(state);
            if(plugin.getRequiredPlugins().contains(plugin)) {
                plugin.addToRecursiveResolvedPlugins(plugin, state);
            }
            resolved(plugin, state);
        }

        void resolved(Plugin p, PlatformState state){
            toVisit.remove(p);
            if(plugin != p){
                plugin.addToRecursiveResolvedPlugins(p, state);
            }

            if(parent != null && parent != this){
                parent.resolved(p, state);
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
            for (Plugin pluginToVisit : plugin.getRequiredPlugins()) {
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
            Set<Plugin> requiredByImportPackage = new LinkedHashSet<>();
            Plugin.addPluginsForImportedPackages(plugin, requiredByImportPackage);

            addToVisit(requiredByImportPackage);
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
            for (Plugin plugin : state.getPlugins(pluginName)) {
                if (plugin.matches(pluginName, version)) {
                    ret.add(plugin);
                }
            }
        } else {
            for (Plugin plugin : state.getPlugins(pluginName)) {
                if (requiredPlugin.isMatching(plugin)) {
                    ret.add(plugin);
                }
            }
        }
        return ret;
    }

    public Set<Plugin> searchInPluginSet(Plugin requiredPlugin) {
        String pluginName = requiredPlugin.getName().trim();
        Set<Plugin> plugins = state.getPlugins(pluginName);
        if (plugins.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Plugin> ret = new LinkedHashSet<Plugin>();
        for (Plugin plugin : plugins) {
            ret.add(plugin);
        }
        return ret;
    }

    public Set<Feature> searchInFeatureSet(ManifestEntry entry, boolean exact) {
        if (entry == null) {
            return Collections.emptySet();
        }
        String id = entry.getName();
        String version = entry.getVersion();
        Set<Feature> ret = new LinkedHashSet<Feature>();

        for (Feature feature : state.getFeatures(id)) {
            if(exact) {
                if (feature.matches(id, version)) {
                    ret.add(feature);
                }
            } else {
                if(entry.isMatching(feature)) {
                    ret.add(feature);
                }
            }
        }
        return ret;
    }

    static final String VER_STR = "[0-9]+([.][0-9]+)?([.][0-9]+)?([.][0-9a-zA-Z-_]+)?";

    static final Pattern VERSION = Pattern.compile(VER_STR);

    static final Pattern RANGE = Pattern.compile("[(|\\[]" + VER_STR + "," + VER_STR + "[)|\\]]");

    /**
     * @return true if the 'givenVersion' greater or equals to the 'rangeOrLowerLimit'
     */
    public static boolean isCompatibleVersion(String rangeOrLowerLimit,  String givenVersion) {
        if (rangeOrLowerLimit == null || givenVersion == null) {
            return false;
        }
        if (givenVersion.isEmpty()) {
            givenVersion = ZERO_VERSION;
        } else {
            if (!VERSION.matcher(givenVersion).matches()) {
                // version range?
                if(!RANGE.matcher(givenVersion).matches()){
                    return false;
                }
                if (rangeOrLowerLimit.isEmpty() || !RANGE.matcher(rangeOrLowerLimit).matches()) {
                    return isVersionInRange(ZERO_VERSION, givenVersion);
                }
                // TODO compare two compatible ranges
                return rangeOrLowerLimit.equals(givenVersion);
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
        boolean isVersion = rangeOrLowerLimit == ZERO_VERSION || VERSION.matcher(rangeOrLowerLimit).matches();
        if (isVersion) {
            return compareVersions(rangeOrLowerLimit, givenVersion) <= 0 ? true : false;
        }
        return false;
    }

    private static boolean isVersionInRange(String version, final String versionRange) {
        List<String> strings = StringUtil.split(versionRange, ',');
        if(strings.size() < 2){
            return false;
        }
        String lowerBorder = strings.get(0);
        char leftBorderType = lowerBorder.charAt(0);
        lowerBorder = lowerBorder.substring(1);
        int lowerBorderCheck = compareVersions(lowerBorder, version);

        if (lowerBorderCheck < 0 || (lowerBorderCheck == 0 && leftBorderType == '[')) {

            String upperBorder = strings.get(1);
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
