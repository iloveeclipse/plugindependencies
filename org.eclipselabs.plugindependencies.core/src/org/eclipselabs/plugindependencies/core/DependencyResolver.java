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
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author obroesam
 *
 */
public class DependencyResolver {
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
            feature.writeErrorLog(requiredFeature, features, "Feature");
        }
        feature.addIncludedFeatures(features);
    }

    private void resolveRequiredPlugin(OSGIElement elementRequiresPlugin,
            ManifestEntry requiredPlugin) {
        boolean elementIsPlugin = elementRequiresPlugin instanceof Plugin;
        Plugin highVersionPlugin = null;

        Set<Plugin> plugins = searchInPluginSet(requiredPlugin, !elementIsPlugin);
        int setSize = plugins.size();
        if (setSize >= 1) {
            highVersionPlugin = getPluginWithHighestVersion(plugins);
        }

        if (setSize != 1) {
            elementRequiresPlugin.writeErrorLog(requiredPlugin, plugins, "Plugin");
        }

        if (highVersionPlugin != null) {
            elementRequiresPlugin.addResolvedPlugin(highVersionPlugin);

            if (requiredPlugin.isReexport()) {
                Set<Package> reexportPackages = highVersionPlugin.getExportedPackages();
                ((Plugin) elementRequiresPlugin).addReexportedPackages(reexportPackages);
                for (Package reexported : reexportPackages) {
                    reexported.addReExportPlugin((Plugin) elementRequiresPlugin);
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
            fragment.setFragHost(fragmentHost);
            fragmentHost.addFragments(fragment);
        }
        if (setSize != 1) {
            fragment.writeErrorLog(entry, resultSet, "Fragment-Host");
        }
    }

    public Set<Package> searchInPackageSet(ManifestEntry requiredPackage) {
        if (requiredPackage == null) {
            return new LinkedHashSet<Package>();
        }
        Set<Package> ret = new LinkedHashSet<Package>();

        for (Package pack : packageSet) {
            if (requiredPackage.isMatching(pack)) {
                ret.add(pack);
            }
        }

        if (ret.isEmpty()) {
            Package p = null;
            try {
                p = searchInJavaHomeJar(requiredPackage);
            } catch (IOException e) {
                Logging.writeErrorOut("Error while reading in JavaHome");
            }
            if (p != null) {
                ret.add(p);
            }
        }
        return ret;
    }

    private final String runningJavaHome = System.getProperty("java.home");

    public Package searchInJavaHomeJar(ManifestEntry requiredPackage) throws IOException {
        if (requiredPackage == null || requiredPackage.getName().isEmpty()) {
            return null;
        }

        String packageName = requiredPackage.getName().trim();
        String javaHome = MainClass.getJavaHome();

        if (javaHome == null || javaHome.isEmpty()) {
            javaHome = runningJavaHome;
            MainClass.setJavaHome(javaHome);
        }
        File javaHomeLib = new File(javaHome + "/lib");
        if (!javaHomeLib.exists()) {
            Logging.writeErrorOut("Specified JavaHome(" + javaHome
                    + ") does not exist. Changing to " + runningJavaHome);
            javaHome = runningJavaHome;
            MainClass.setJavaHome(javaHome);
        }

        File[] jarList = javaHomeLib.listFiles(new JarFilter());
        if(jarList == null){
            return null;
        }
        for (File jar : jarList) {
            try (JarFile jarfile = new JarFile(jar)) {
                Enumeration<JarEntry> jarEntries = jarfile.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry entry = jarEntries.nextElement();
                    if (entry.getName().contains(packageName.replace(".", "/") + "/")) {
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

    public Set<Plugin> searchInPluginSet(ManifestEntry requiredPlugin,
            boolean exactVersion) {
        if (requiredPlugin == null) {
            return new LinkedHashSet<Plugin>();
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
            return new LinkedHashSet<Feature>();
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

    static final String NUM = "[0-9]+([.][0-9]+)?([.][0-9]+)?([.][0-9a-zA-Z-_]+)?";

    static final Pattern NUMBER = Pattern.compile(NUM);

    static final Pattern RANGE = Pattern.compile("[(|\\[]" + NUMBER + "[,]" + NUMBER
            + "[)|\\]]");

    public static boolean isCompatibleVersion(String rangeOrLowerLimit,
            String givenVersion) {
        if (rangeOrLowerLimit == null || givenVersion == null) {
            Logging.writeErrorOut("Version can not be null");
            return false;
        }
        if (givenVersion.isEmpty()) {
            givenVersion = "0.0.0";
        }
        if (rangeOrLowerLimit.isEmpty()) {
            rangeOrLowerLimit = "0.0.0";
        }

        Matcher matchNumber = NUMBER.matcher(rangeOrLowerLimit);
        Matcher matchRange = RANGE.matcher(rangeOrLowerLimit);

        boolean isNumber = matchNumber.matches();
        boolean isRange = matchRange.matches();

        if (givenVersion.matches(NUM)) {
            if (isRange) {
                return isVersionInRange(givenVersion, rangeOrLowerLimit);
            }

            if (isNumber) {
                return compareVersions(rangeOrLowerLimit, givenVersion) <= 0 ? true
                        : false;
            }
        }
        return false;
    }

    private static boolean isVersionInRange(String version, String versionRange) {
        String lowerBorder = versionRange.split(",")[0];
        String upperBorder = versionRange.split(",")[1];

        char leftBorderType = lowerBorder.charAt(0);
        char rightBorderType = upperBorder.charAt(upperBorder.length() - 1);

        lowerBorder = lowerBorder.substring(1);
        upperBorder = upperBorder.substring(0, upperBorder.length() - 1);

        int lowerBorderCheck = compareVersions(lowerBorder, version);
        int upperBorderCheck = compareVersions(version, upperBorder);

        if (lowerBorderCheck < 0 || (lowerBorderCheck == 0 && leftBorderType == '[')) {
            if (upperBorderCheck < 0 || (upperBorderCheck == 0 && rightBorderType == ']')) {
                return true;
            }
        }
        return false;
    }

    private static int compareVersions(String v1, String v2) {
        if (v1.isEmpty()) {
            v1 = "0.0.0";
        }
        if (v2.isEmpty()) {
            v2 = "0.0.0";
        }
        Version version1 = new Version(v1);
        Version version2 = new Version(v2);
        return version1.compareTo(version2);
    }
}
