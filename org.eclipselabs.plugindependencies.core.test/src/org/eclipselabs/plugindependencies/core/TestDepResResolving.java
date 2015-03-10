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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.DependencyResolver;
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.FeatureParser;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;
import org.eclipselabs.plugindependencies.core.PluginParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestDepResResolving {
    Feature featureLeft;

    Feature featureRight;

    Feature featureRR;

    Plugin plugin1;

    Plugin plugin2;

    Plugin plugin3;

    Plugin plugin4;

    Plugin plugin5;

    Plugin plugin6;

    Plugin plugin7;

    Set<Plugin> pluginSet;

    Set<Feature> featureSet;

    Set<Package> packageSet;

    DependencyResolver depres;

    Package package1;

    Package package2;

    Package package3;

    Package package4;

    Package package5;

    Package package6;

    Package package7;

    Package package8;

    Package package9;

    Package package10;

    Package package11;

    Package package12;

    Package package13;

    Package package14;

    Package package15;

    @Before
    public void setUp() throws Exception {
        pluginSet = new HashSet<Plugin>();
        packageSet = new HashSet<Package>();
        featureSet = new HashSet<Feature>();
        PluginParser.readManifests("testdata_dependencies/eclipse/plugins", pluginSet,
                packageSet);
        FeatureParser.readFeatures("testdata_dependencies/eclipse/features", featureSet);
        featureLeft = new Feature("org.example.left", "2.1.1.v20120113-1346");
        featureRight = new Feature("org.company.right", "1.6.0");
        featureRR = new Feature("org.eclipse.right.right",
                "3.7.2.r37x_v20111213-7Q7xALDPb32vCjY6UACVPdFTz-icPtJkUadz0lMmk4z-8");
        plugin1 = new Plugin("org.eclipse.plugin1", "2.0.0.201306111332");
        plugin2 = new Plugin("org.eclipse.adv", "1.2.3");
        plugin3 = new Plugin("org.eclipse.adv.core", "4.0.1.v93_k");
        plugin4 = new Plugin("org.company.corePlugin", "3.2.0.v2014");
        plugin5 = new Plugin("org.company.test.framework", "8.4.0.t2000");
        plugin6 = new Plugin("org.company.workcenter", "6.3.1");
        plugin7 = new Plugin("org.eclipse.equinox.core", "1.0.0");
        package1 = new Package("org.apache.html.dom", "");
        package2 = new Package("org.apache.wml", "");
        package3 = new Package("org.apache.wml.dom", "");
        package4 = new Package("org.adv.core", "");
        package5 = new Package("org.jdom2.transform", "");
        package6 = new Package("org.jdom2.util", "");
        package7 = new Package("org.jdom2.xpath", "");
        package8 = new Package("org.jdom2.xpath.jaxen", "");
        package9 = new Package("org.jdom2.xpath.util", "");
        package10 = new Package("org.w3c.dom", "");
        package11 = new Package("org.w3c.dom.html", "");
        package12 = new Package("com.package.test", "4.11.0");
        package13 = new Package("junit.framework", "4.11.0");
        package14 = new Package("junit.runner", "4.11.0");
        package15 = new Package("org.adv.core", "3.2.1");
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);
        for (Feature feature : featureSet) {
            depres.resolveFeatureDependency(feature);
        }
        for (Plugin plugin : pluginSet) {
            depres.resolvePluginDependency(plugin);
        }
    }

    @After
    public void tearDown() throws Exception {
        pluginSet = null;
        packageSet = null;
        featureSet = null;
    }

    @Test
    public void testFeatureLinking() {
        Set<Feature> compareSetFeature = new HashSet<Feature>();
        Set<Plugin> compareSetPlugin = new HashSet<Plugin>();

        Feature topFeat = getFeature("org.eclipse.topFeature", featureSet);
        Feature leftFeat = getFeature("org.example.left", featureSet);
        Feature rightFeat = getFeature("org.company.right", featureSet);
        Feature rightrightFeat = getFeature("org.eclipse.right.right", featureSet);

        // topFeature
        compareSetFeature.add(featureLeft);
        compareSetFeature.add(featureRight);
        assertEquals(compareSetFeature, topFeat.getIncludedFeatures());

        compareSetPlugin.add(plugin1);
        assertEquals(compareSetPlugin, topFeat.getResolvedPlugins());

        compareSetFeature.clear();
        compareSetFeature.add(topFeat);
        for (Plugin plugin : topFeat.getResolvedPlugins()) {
            assertEquals(compareSetFeature, plugin.getIncludedInFeatures());
        }

        assertEquals(new HashSet<Feature>(), topFeat.getIncludedInFeatures());

        // topFeature end

        // featureLeft
        compareSetFeature.clear();
        compareSetFeature.add(topFeat);
        assertEquals(compareSetFeature, leftFeat.getIncludedInFeatures());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin2);
        compareSetPlugin.add(plugin3);
        assertEquals(compareSetPlugin, leftFeat.getResolvedPlugins());

        assertEquals(new HashSet<Feature>(), leftFeat.getIncludedFeatures());

        compareSetFeature.clear();
        compareSetFeature.add(leftFeat);
        for (Plugin plugin : leftFeat.getResolvedPlugins()) {
            assertEquals(compareSetFeature, plugin.getIncludedInFeatures());
        }
        // featureLeft end

        // featureRight
        compareSetFeature.clear();
        compareSetFeature.add(topFeat);
        assertEquals(compareSetFeature, rightFeat.getIncludedInFeatures());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin5);
        compareSetPlugin.add(plugin6);
        assertEquals(compareSetPlugin, rightFeat.getResolvedPlugins());

        compareSetFeature.clear();
        compareSetFeature.add(featureRR);
        assertEquals(compareSetFeature, rightFeat.getIncludedFeatures());

        compareSetFeature.clear();
        compareSetFeature.add(rightFeat);
        for (Plugin plugin : rightFeat.getResolvedPlugins()) {
            assertEquals(compareSetFeature, plugin.getIncludedInFeatures());
        }
        // featureRight end

        // featureRR
        compareSetFeature.clear();
        compareSetFeature.add(rightFeat);
        assertEquals(compareSetFeature, rightrightFeat.getIncludedInFeatures());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin7);
        assertEquals(compareSetPlugin, rightrightFeat.getResolvedPlugins());

        assertEquals(new HashSet<Feature>(), rightrightFeat.getIncludedFeatures());

        compareSetFeature.clear();
        compareSetFeature.add(rightrightFeat);
        for (Plugin plugin : rightrightFeat.getResolvedPlugins()) {
            assertEquals(compareSetFeature, plugin.getIncludedInFeatures());
        }
        // featureRR end
    }

    @Test
    public void testPluginLinking() {
        Set<Plugin> compareSetPlugin = new LinkedHashSet<Plugin>();
        Set<Package> compareSetPackage = new LinkedHashSet<Package>();
        Plugin forCompare;
        List<String> compareLog = new ArrayList<String>();

        // plugin1
        forCompare = getPlugin("org.eclipse.plugin1", pluginSet);

        compareLog.add("Error: Fragment-Host not found: host.not.found 2.0.0");
        compareLog.add("Error: Plugin not found: plugin.not.found.test 1.1.1");
        assertEquals(compareLog, forCompare.getLog());

        assertEquals(new HashSet<Plugin>(), forCompare.getResolvedPlugins());

        assertEquals(new HashSet<Package>(), forCompare.getImportedPackages());

        assertEquals(new HashSet<Plugin>(), forCompare.getRequiredBy());

        assertEquals("", forCompare.printRequiringThis());

        compareSetPackage.add(package1);
        compareSetPackage.add(package2);
        compareSetPackage.add(package3);
        compareSetPackage.add(package5);
        compareSetPackage.add(package6);
        compareSetPackage.add(package7);
        compareSetPackage.add(package8);
        compareSetPackage.add(package9);
        compareSetPackage.add(package10);
        compareSetPackage.add(package11);
        compareSetPackage.add(package15);
        assertEquals(compareSetPackage, forCompare.getExportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin1);
        for (Package pack : forCompare.getExportedPackages()) {
            assertEquals(compareSetPlugin, pack.getExportedBy());
        }

        // plugin2
        forCompare = getPlugin("org.eclipse.adv", pluginSet);

        assertEquals(plugin4, forCompare.getFragHost());

        assertTrue(forCompare.getLog().isEmpty());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin5);
        compareSetPlugin.add(plugin6);
        assertEquals(compareSetPlugin, forCompare.getResolvedPlugins());

        compareSetPackage.clear();
        compareSetPackage.add(package1);
        compareSetPackage.add(package2);
        compareSetPackage.add(package3);
        assertEquals(compareSetPackage, forCompare.getImportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin2);

        for (Package pack : forCompare.getImportedPackages()) {
            assertEquals(compareSetPlugin, pack.getImportedBy());
        }

        assertEquals(new HashSet<Plugin>(), forCompare.getRequiredBy());

        assertEquals(new HashSet<Package>(), forCompare.getExportedPackages());

        // plugin3
        forCompare = getPlugin("org.eclipse.adv.core", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin7);
        assertEquals(compareSetPlugin, forCompare.getResolvedPlugins());

        assertEquals(new HashSet<Package>(), forCompare.getImportedPackages());

        assertEquals(new HashSet<Plugin>(), forCompare.getRequiredBy());

        compareSetPackage.clear();
        compareSetPackage.add(package4);
        assertEquals(compareSetPackage, forCompare.getExportedPackages());

        // plugin4
        forCompare = getPlugin("org.company.corePlugin", pluginSet);

        compareLog.clear();
        compareLog
                .add("Warning: More than one Package found for org.adv.core  *optional*\n"
                        + "\tPackage: org.adv.core\n"
                        + "\t\tExported By:\n"
                        + "\t\tPlugin: org.eclipse.adv.core 4.0.1.v93_k "
                        + System.getProperty("user.dir")
                        + "/testdata_dependencies/eclipse/plugins/plugin3\n"
                        + "\tPackage: org.adv.core 3.2.1\n"
                        + "\t\tExported By:\n"
                        + "\t\tFragment: org.eclipse.plugin1 2.0.0.201306111332 "
                        + System.getProperty("user.dir")
                        + "/testdata_dependencies/eclipse/plugins/plugin1\n");
        assertEquals(compareLog.toString(), forCompare.getLog().toString());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin5);
        compareSetPlugin.add(plugin6);
        compareSetPlugin.add(plugin7);
        assertEquals(compareSetPlugin, forCompare.getResolvedPlugins());

        compareSetPackage.clear();
        compareSetPackage.add(package15);
        assertEquals(compareSetPackage, forCompare.getImportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin2);
        assertEquals(compareSetPlugin, forCompare.getRequiredBy());

        assertEquals(new HashSet<Package>(), forCompare.getExportedPackages());

        // plugin5
        forCompare = getPlugin("org.company.test.framework", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        assertEquals(new HashSet<Plugin>(), forCompare.getResolvedPlugins());

        compareSetPackage.clear();
        compareSetPackage.add(package5);
        compareSetPackage.add(package6);
        compareSetPackage.add(package7);
        compareSetPackage.add(package8);
        compareSetPackage.add(package9);
        compareSetPackage.add(package10);
        compareSetPackage.add(package11);
        assertEquals(compareSetPackage, forCompare.getImportedPackages());

        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin6);
        assertEquals(compareSetPlugin, forCompare.getRequiredBy());

        String output = "Is Required By:\n" + "\tFragment: org.eclipse.adv 1.2.3 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin2\n"
                + "\tPlugin: org.company.corePlugin 3.2.0.v2014 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin4\n"
                + "\tPlugin: org.company.workcenter 6.3.1 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin6\n";
        assertEquals(output, forCompare.printRequiringThis());

        assertEquals(new HashSet<Package>(), forCompare.getExportedPackages());

        // plugin6
        forCompare = getPlugin("org.company.workcenter", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin5);
        assertEquals(compareSetPlugin, forCompare.getResolvedPlugins());

        assertEquals(new HashSet<Package>(), forCompare.getImportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin2);
        assertEquals(compareSetPlugin, forCompare.getRequiredBy());

        String output2 = "Is Required By:\n"
                + "\t*optional* for Fragment: org.eclipse.adv 1.2.3 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin2\n"
                + "\tPlugin: org.company.corePlugin 3.2.0.v2014 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin4\n";
        assertEquals(output2, forCompare.printRequiringThis());

        compareSetPackage.clear();
        compareSetPackage.add(package12);
        compareSetPackage.add(package13);
        compareSetPackage.add(package14);
        assertEquals(compareSetPackage, forCompare.getExportedPackages());

        // plugin7
        forCompare = getPlugin("org.eclipse.equinox.core", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        assertEquals(new HashSet<Plugin>(), forCompare.getResolvedPlugins());

        assertEquals(compareSetPackage, forCompare.getImportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin3);
        assertEquals(compareSetPlugin, forCompare.getRequiredBy());

        assertEquals(new HashSet<Package>(), forCompare.getExportedPackages());
    }

    @Test
    public void testPackageOutput() {
        String importedBy;
        String exportedBy;
        Plugin plugin;

        Package pack = new Package("com.package.test", "4.11.0");

        exportedBy = "\t\tExported By:\n\t\tJRE System Library";
        assertEquals(exportedBy, pack.printExportedBy(0));

        assertEquals("", pack.printImportedBy(0));

        plugin = getPlugin("org.company.workcenter", pluginSet);
        exportedBy = "\t\tExported By:\n" + "\t\tPlugin: org.company.workcenter 6.3.1 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin6\n";
        importedBy = "Imported By:\n" + "\tPlugin: org.eclipse.equinox.core 1.0.0 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin7\n";
        for (Package pack2 : plugin.getExportedPackages()) {
            assertEquals(exportedBy, pack2.printExportedBy(0));
            assertEquals(importedBy, pack2.printImportedBy(0));
        }

        plugin = getPlugin("org.company.corePlugin", pluginSet);
        importedBy = "Imported By:\n"
                + "\t*optional* for Plugin: org.company.corePlugin 3.2.0.v2014 "
                + System.getProperty("user.dir")
                + "/testdata_dependencies/eclipse/plugins/plugin4\n";
        for (Package pack3 : plugin.getImportedPackages()) {
            assertEquals(importedBy, pack3.printImportedBy(0));
        }
    }

    private static Plugin getPlugin(String name, Set<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if (plugin.getName().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    private static Feature getFeature(String name, Set<Feature> features) {
        for (Feature feature : features) {
            if (feature.getName().equals(name)) {
                return feature;
            }
        }
        return null;
    }
}
