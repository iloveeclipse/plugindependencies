/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>. All rights reserved.
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 */
public class PlatformState {

    private static Set<Plugin> pluginSet = new LinkedHashSet<>();
    private static Set<Package> packageSet = new LinkedHashSet<>();
    private static Set<Feature> featureSet = new LinkedHashSet<>();
    private static String javaHome;

    public static Set<Plugin> getPluginSet(){
        return PlatformState.pluginSet;
    }

    public static Set<Package> getPackageSet(){
        return PlatformState.packageSet;
    }

    public static Set<Feature> getFeatureSet(){
        return PlatformState.featureSet;
    }

    public static void cleanup(){
        pluginSet = new LinkedHashSet<>();
        packageSet = new LinkedHashSet<>();
        featureSet = new LinkedHashSet<>();
        PlatformState.javaHome = "";
    }

    static String getJavaHome() {
        return javaHome;
    }

    static void setJavaHome(String newHome) {
        javaHome = newHome;
    }

}
