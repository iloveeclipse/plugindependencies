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

import static org.eclipselabs.plugindependencies.core.Logging.PREFIX_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class MainClass {

    public static String printUnresolvedDependencies(Set<? extends OSGIElement> elements,
            boolean showWarnings) {
        StringBuilder ret = new StringBuilder();

        for (OSGIElement element : elements) {
            List<String> log = element.getLog();
            if (!log.isEmpty() && (log.toString().contains(PREFIX_ERROR) || showWarnings)) {
                ret.append(element instanceof Plugin ? "plugin: " : "feature: ");
                ret.append(element.getInformationLine() + "\n");
                ret.append(printLog(element, showWarnings));
                ret.append(backtrace(element, 0));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    private static String printLog(NamedElement element, boolean showWarnings) {
        StringBuilder ret = new StringBuilder();
        List<String> log = element.getLog();
        for (String logEntry : log) {
            if (logEntry.contains("Error") || showWarnings) {
                ret.append("\t" + logEntry + "\n");
            }
        }
        return ret.toString();
    }

    static Set<Plugin> searchPlugin(Set<Plugin> searchIn, ManifestEntry entry) {
        Set<Plugin> returnSet = new LinkedHashSet<Plugin>();
        for (Plugin plugin : searchIn) {
            if (entry.isMatching(plugin)) {
                returnSet.add(plugin);
            }
        }
        return returnSet;
    }

    static Set<Feature> searchFeature(Set<Feature> searchIn, ManifestEntry entry) {
        Set<Feature> returnSet = new LinkedHashSet<>();
        for (Feature feature : searchIn) {
            if (entry.isMatching(feature)) {
                returnSet.add(feature);
            }
        }
        return returnSet;
    }

    static Set<Package> searchPackage(Set<Package> searchIn, ManifestEntry entry) {
        Set<Package> returnSet = new LinkedHashSet<Package>();
        for (Package pack : searchIn) {
            if (entry.isMatching(pack)) {
                returnSet.add(pack);
            }
        }
        return returnSet;
    }

    private static String backtrace(OSGIElement element, int indentation) {
        String indent = StringUtil.multiplyString("\t", indentation);
        Set<Feature> containedIn = element.getIncludedInFeatures();
        StringBuilder ret = new StringBuilder();
        if (!containedIn.isEmpty()) {
            ret.append(indent + "contained in feature:\n");
        }
        for (Feature feat : containedIn) {
            ret.append(indent + "\t");
            ret.append(feat.getInformationLine() + "\n");
            ret.append(backtrace(feat, indentation + 1));
        }

        if (element instanceof Plugin) {
            String needingThis = ((Plugin) element).printRequiringThis();
            ret.append(needingThis);
        }
        return ret.toString();
    }

    public static int readInEclipseFolder(String eclipsePath) throws IOException,
            SAXException, ParserConfigurationException {
        File root = new File(eclipsePath);
        File pluginsDir = new File(root, "plugins");
        boolean hasPlugins = false;
        if (pluginsDir.exists()) {
            if (PluginParser.createPluginsAndAddToSet(pluginsDir, pluginSet, packageSet) == -1) {
                return -1;
            }
            hasPlugins = true;
        }
        File featureDir = new File(root, "features");
        boolean hasFeatures = false;
        if (featureDir.exists()) {
            if (FeatureParser.createFeaturesAndAddToSet(featureDir, featureSet) == -1) {
                return -1;
            }
            hasFeatures = true;
        }
        if(hasPlugins && hasFeatures){
            File dropinsDir = new File(root, "dropins");
            if (dropinsDir.exists()) {
                return readInChildren(dropinsDir);
            }
        }
        return readInChildren(root);
    }

    public static int readInChildren(File directory) throws IOException,
        SAXException, ParserConfigurationException {
        if (PluginParser.createPluginsAndAddToSet(directory, pluginSet, packageSet) == -1) {
            return -1;
        }
        if (FeatureParser.createFeaturesAndAddToSet(directory, featureSet) == -1) {
            return -1;
        }
        return 0;
    }

    public static int readInPlugin(File directory) throws IOException {
        return PluginParser.createPluginAndAddToSet(directory, pluginSet, packageSet);
    }

    public static int readInFeature(File directory) throws IOException,
        SAXException, ParserConfigurationException {
        return FeatureParser.createFeatureAndAddToSet(directory, featureSet);
    }

    public static void resolveDependencies() {
        DependencyResolver depres = new DependencyResolver(pluginSet, packageSet, featureSet);

        for (Plugin plugin : pluginSet) {
            depres.resolvePluginDependency(plugin);
        }
        for (Feature feature : featureSet) {
            depres.resolveFeatureDependency(feature);
        }
        for (Plugin plugin : pluginSet) {
            plugin.parsingDone();
        }
        for (Feature feature : featureSet) {
            feature.parsingDone();
        }
    }

    static String getJavaHome() {
        return javaHome;
    }

    static void setJavaHome(String newHome) {
        javaHome = newHome;
    }

    public static Set<Plugin> getPluginSet(){
        return pluginSet != null ? pluginSet : new LinkedHashSet<Plugin>();
    }

    public static Set<Package> getPackageSet(){
        return packageSet != null ? packageSet : new LinkedHashSet<Package>();
    }

    public static Set<Feature> getFeatureSet(){
        return featureSet != null ? featureSet : new LinkedHashSet<Feature>();
    }

    static Set<Plugin> pluginSet;

    static Set<Package> packageSet;

    static Set<Feature> featureSet;

    static String javaHome;

    public static void cleanup(){
        pluginSet = new LinkedHashSet<Plugin>();
        packageSet = new LinkedHashSet<Package>();
        featureSet = new LinkedHashSet<Feature>();
        javaHome = "";

    }

    public static int run(String[] args) {
        cleanup();
        return CommandLineInterpreter.interpreteInput(args);
    }

    public static void main(String[] args) {
        int status = run(args);
        String name = MainClass.class.getClassLoader().getClass().getName();
        if(name.contains("eclipse") || name.contains("osgi")){
            return;
        }
        System.exit(status);
    }
}
