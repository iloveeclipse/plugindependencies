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
            if (!log.isEmpty() && (log.toString().contains("Error") || showWarnings)) {
                ret.append(element instanceof Plugin ? "Plugin: " : "Feature: ");
                ret.append(element.getInformationLine() + "\n");
                ret.append(printLog(element, showWarnings));
                ret.append(backtrace(element, 0));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    private static String printLog(OSGIElement element, boolean showWarnings) {
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
            ret.append(indent + "Contained in Feature:\n");
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
        String pluginDir = eclipsePath + "/plugins";
        String featureDir = eclipsePath + "/features";
        if (new File(pluginDir).exists()) {
            if (PluginParser.readManifests(pluginDir, pluginSet, packageSet) == -1) {
                return -1;
            }
        }
        if (new File(featureDir).exists()) {
            if (FeatureParser.readFeatures(featureDir, featureSet) == -1) {
                return -1;
            }
        }
        if (PluginParser.readManifests(eclipsePath, pluginSet, packageSet) == -1) {
            return -1;
        }
        if (FeatureParser.readFeatures(eclipsePath, featureSet) == -1) {
            return -1;
        }
        return 0;
    }

    public static void resolveDependencies() {
        DependencyResolver depres = new DependencyResolver(pluginSet, packageSet,
                featureSet);

        for (Plugin plugin : pluginSet) {
            depres.resolvePluginDependency(plugin);
        }

        for (Feature feature : featureSet) {
            depres.resolveFeatureDependency(feature);
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

    public static void initVariables(){
        pluginSet = new LinkedHashSet<Plugin>();
        packageSet = new LinkedHashSet<Package>();
        featureSet = new LinkedHashSet<Feature>();
        javaHome = "";

    }

    private static int init(String[] args) {
        initVariables();

        int status = CommandLineInterpreter.interpreteInput(args);
        return status;
    }

    public static void main(String[] args) {
        int status = init(args);

        System.exit(status);
    }
}
