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

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDepResResolving  extends BaseTest {
    /**
     *
     */
    private static final String HOME = System.getProperty("user.dir");

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
        pluginSet = new LinkedHashSet<Plugin>();
        packageSet = new LinkedHashSet<Package>();
        featureSet = new LinkedHashSet<Feature>();
        FeatureParser.createFeaturesAndAddToSet(new File("testdata_dependencies/eclipse/features"), featureSet);
        PlatformState state = new PlatformState(pluginSet, packageSet, featureSet);
        PluginParser.createPluginsAndAddToSet(new File("testdata_dependencies/eclipse/plugins"), state);
        featureLeft = new Feature("org.example.left", "2.1.1.v20120113-1346");
        featureRight = new Feature("org.company.right", "1.6.0");
        featureRR = new Feature("org.eclipse.right.right",
                "3.7.2.r37x_v20111213-7Q7xALDPb32vCjY6UACVPdFTz-icPtJkUadz0lMmk4z-8");
        plugin1 = new Plugin("org.eclipse.plugin1", "2.0.0.201306111332", true, false);
        plugin2 = new Plugin("org.eclipse.adv", "1.2.3", true, false);
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
        depres = state.resolveDependencies();
//        for (Feature feature : featureSet) {
//            depres.resolveFeatureDependency(feature);
//        }
//        for (Plugin plugin : pluginSet) {
//            depres.resolvePluginDependency(plugin);
//        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        pluginSet = null;
        packageSet = null;
        featureSet = null;
        super.tearDown();
    }

    @Test
    public void testFeatureLinking() {
        Set<Feature> compareSetFeature = new LinkedHashSet<Feature>();
        Set<Plugin> compareSetPlugin = new LinkedHashSet<Plugin>();

        Feature topFeat = getFeature("org.eclipse.topFeature", featureSet);
        Feature leftFeat = getFeature("org.example.left", featureSet);
        Feature rightFeat = getFeature("org.company.right", featureSet);
        Feature rightrightFeat = getFeature("org.eclipse.right.right", featureSet);

        // topFeature
        compareSetFeature.add(featureLeft);
        compareSetFeature.add(featureRight);
        assertEquals(compareSetFeature, topFeat.getIncludedFeatures());

        compareSetPlugin.add(plugin1);
        assertEquals(compareSetPlugin.toString(), topFeat.getResolvedPlugins().toString());

        compareSetFeature.clear();
        compareSetFeature.add(topFeat);
        for (Plugin plugin : topFeat.getResolvedPlugins()) {
            assertEquals(compareSetFeature, plugin.getIncludedInFeatures());
        }

        assertEquals(new LinkedHashSet<Feature>().toString(), topFeat.getIncludedInFeatures().toString());

        // topFeature end

        // featureLeft
        compareSetFeature.clear();
        compareSetFeature.add(topFeat);
        assertEquals(compareSetFeature, leftFeat.getIncludedInFeatures());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin2);
        compareSetPlugin.add(plugin3);
        assertEquals(compareSetPlugin.toString(), leftFeat.getResolvedPlugins().toString());

        assertEquals(new LinkedHashSet<Feature>().toString(), leftFeat.getIncludedFeatures().toString());

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
        assertEquals(compareSetPlugin.toString(), rightFeat.getResolvedPlugins().toString());

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

        assertEquals(new LinkedHashSet<>().toString(), rightrightFeat.getIncludedFeatures().toString());

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

        compareLog.add("Error: fragment host not found: host.not.found 2.0.0");
        compareLog.add("Error: plugin not found: plugin.not.found.test 1.1.1");
        assertEquals(compareLog.toString(), forCompare.getLog().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getResolvedPlugins().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getImportedPackages().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getRequiredBy().toString());

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
            assertEquals(compareSetPlugin.toString(), pack.getExportedBy().toString());
        }

        // plugin2
        forCompare = getPlugin("org.eclipse.adv", pluginSet);

        assertEquals(plugin4, forCompare.getHost());

        assertEquals("[]", forCompare.getLog().toString());

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
            assertEquals(compareSetPlugin.toString(), pack.getImportedBy().toString());
        }

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getRequiredBy().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getExportedPackages().toString());

        // plugin3
        forCompare = getPlugin("org.eclipse.adv.core", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin7);
        assertEquals(compareSetPlugin, forCompare.getResolvedPlugins());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getImportedPackages().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getRequiredBy().toString());

        compareSetPackage.clear();
        compareSetPackage.add(package4);
        assertEquals(compareSetPackage, forCompare.getExportedPackages());

        // plugin4
        forCompare = getPlugin("org.company.corePlugin", pluginSet);

        compareLog.clear();
        compareLog
                .add("Warning: more than one package found for org.adv.core  *optional*\n"
                        + "\tpackage: org.adv.core\n"
                        + "\t\texported by:\n"
                        + "\t\tplugin: org.eclipse.adv.core 4.0.1.v93_k "
                        + HOME
                        + "/testdata_dependencies/eclipse/plugins/org.eclipse.adv.core\n"
                        + "\tpackage: org.adv.core 3.2.1\n"
                        + "\t\texported by:\n"
                        + "\t\tfragment: org.eclipse.plugin1 2.0.0.201306111332 "
                        + HOME
                        + "/testdata_dependencies/eclipse/plugins/org.eclipse.plugin1\n");

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
        assertEquals(compareSetPlugin.toString(), forCompare.getRequiredBy().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getExportedPackages().toString());

        // plugin5
        forCompare = getPlugin("org.company.test.framework", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getResolvedPlugins().toString());

        compareSetPackage.clear();
        compareSetPackage.add(package5);
        compareSetPackage.add(package6);
        compareSetPackage.add(package7);
        compareSetPackage.add(package8);
        compareSetPackage.add(package9);
        compareSetPackage.add(package10);
        compareSetPackage.add(package11);
        assertEquals(compareSetPackage, forCompare.getImportedPackages());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin6);
        compareSetPlugin.add(plugin2);
        assertEquals(compareSetPlugin.toString(), forCompare.getRequiredBy().toString());

        String output = "is required by:\n"
                + "\tplugin: org.company.corePlugin 3.2.0.v2014 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.company.corePlugin\n"
                + "\tplugin: org.company.workcenter 6.3.1 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.company.workcenter\n"
                + "\tfragment: org.eclipse.adv 1.2.3 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.eclipse.adv\n";
        assertEquals(output, forCompare.printRequiringThis());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getExportedPackages().toString());

        // plugin6
        forCompare = getPlugin("org.company.workcenter", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin5);
        assertEquals(compareSetPlugin.toString(), forCompare.getResolvedPlugins().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getImportedPackages().toString());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin2);
        assertEquals(compareSetPlugin.toString(), forCompare.getRequiredBy().toString());

        String output2 = "is required by:\n"
                + "\tplugin: org.company.corePlugin 3.2.0.v2014 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.company.corePlugin\n"
                + "\t*optional* for fragment: org.eclipse.adv 1.2.3 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.eclipse.adv\n";
        assertEquals(output2, forCompare.printRequiringThis());

        compareSetPackage.clear();
        compareSetPackage.add(package12);
        compareSetPackage.add(package13);
        compareSetPackage.add(package14);
        assertEquals(compareSetPackage.toString(), forCompare.getExportedPackages().toString());

        // plugin7
        forCompare = getPlugin("org.eclipse.equinox.core", pluginSet);

        assertTrue(forCompare.getLog().isEmpty());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getResolvedPlugins().toString());

        assertEquals(compareSetPackage.toString(), forCompare.getImportedPackages().toString());

        compareSetPlugin.clear();
        compareSetPlugin.add(plugin4);
        compareSetPlugin.add(plugin3);
        assertEquals(compareSetPlugin.toString(), forCompare.getRequiredBy().toString());

        assertEquals(new LinkedHashSet<>().toString(), forCompare.getExportedPackages().toString());
    }

    @Test
    public void testPackageOutput() {
        String importedBy;
        String exportedBy;
        Plugin plugin;

        Package pack = new Package("com.package.test", "4.11.0");

        exportedBy = "\t\texported by:\n\t\tJRE System Library";
        assertEquals(exportedBy, pack.printExportedBy(0));

        assertEquals("", pack.printImportedBy(0));

        plugin = getPlugin("org.company.workcenter", pluginSet);
        exportedBy = "\t\texported by:\n\t\tplugin: org.company.workcenter 6.3.1 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.company.workcenter\n";
        importedBy = "imported by:\n\tplugin: org.eclipse.equinox.core 1.0.0 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.eclipse.equinox.core\n";
        for (Package pack2 : plugin.getExportedPackages()) {
            assertEquals(exportedBy, pack2.printExportedBy(0));
            assertEquals(importedBy, pack2.printImportedBy(0));
        }

        plugin = getPlugin("org.company.corePlugin", pluginSet);
        importedBy = "imported by:\n"
                + "\t*optional* for plugin: org.company.corePlugin 3.2.0.v2014 "
                + HOME
                + "/testdata_dependencies/eclipse/plugins/org.company.corePlugin\n";
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
