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
import static org.junit.Assert.assertNotEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDepResSearchMethodPlugin extends BaseTest {
    static Set<Package> packageSet;

    static Set<Plugin> pluginSet;

    static Set<Feature> featureSet;

    static Plugin plugin1;

    static Plugin plugin2;

    static Plugin systemBundle;

    DependencyResolver depres;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        plugin1 = new Plugin("com.company.core", "3.4.5");
        plugin2 = new Plugin("com.company.core", "1.2.0");
        systemBundle = new Plugin("org.eclipse.osgi", "");
        pluginSet = new LinkedHashSet<Plugin>();
        pluginSet.add(new Plugin("org.eclipse.p1", "1.2.3"));
        pluginSet.add(new Plugin("org.eclipse.p2", "4.5.6"));
        pluginSet.add(new Plugin("com.example.itee.ate", "3.5.8"));
        pluginSet.add(new Plugin("com.example.result", "99.0.0"));
        pluginSet.add(new Plugin("com.company.itee.core", "3.8.2"));
        pluginSet.add(new Plugin("com.company.tables", "9.2.0"));
        pluginSet.add(plugin1);
        pluginSet.add(plugin2);
        pluginSet.add(systemBundle);
        packageSet = new LinkedHashSet<Package>();
        featureSet = new LinkedHashSet<Feature>();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        packageSet = null;
        plugin1 = null;
        plugin2 = null;
        pluginSet = null;
        featureSet = null;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        depres = null;
        super.tearDown();
    }

    @Test
    public void testSearchPluginWrongPara() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);
        ManifestEntry entry;

        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(null, false).toString());
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(null, true).toString());

        entry = new ManifestEntry(new String[] { "" });
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, true).toString());

        entry = new ManifestEntry("Plugin.can.not.be.found", "");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, true).toString());

        entry = new ManifestEntry("com.company.core", "Wrong.Version.Format");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, true).toString());
    }

    @Test
    public void testSearchPlugin() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);
        Set<Plugin> resultSet = new LinkedHashSet<Plugin>();
        ManifestEntry entry;
        resultSet.add(plugin1);

        entry = new ManifestEntry("com.company.core", "3.2");
        assertEquals(resultSet, depres.searchInPluginSet(entry, false));
        assertNotEquals(resultSet, depres.searchInPluginSet(entry, true));

        entry = new ManifestEntry("com.company.core", "3.5");
        assertNotEquals(resultSet, depres.searchInPluginSet(entry, false));

        entry = new ManifestEntry("com.company.core", "3.4.5");
        assertEquals(resultSet, depres.searchInPluginSet(entry, true));

        entry = new ManifestEntry("com.company.core", "[3.2.0,4.0.0)");
        assertEquals(resultSet, depres.searchInPluginSet(entry, false));

        resultSet.add(plugin2);

        entry = new ManifestEntry("com.company.core", "");
        assertEquals(resultSet, depres.searchInPluginSet(entry, true));
        assertEquals(resultSet, depres.searchInPluginSet(entry, false));

        resultSet.remove(plugin1);

        entry = new ManifestEntry("com.company.core", "[1.2,2.0.0)");
        assertEquals(resultSet, depres.searchInPluginSet(entry, false));

        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(null, false).toString());

        entry = new ManifestEntry(new String[] { "" });
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());

        entry = new ManifestEntry("Plugin.can.not.be.found", "");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());

        entry = new ManifestEntry(new String[] { "com.company.core",
                "version=\"Wrong.Version.Format\"" });
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPluginSet(entry, false).toString());
    }

    @Test
    public void testSystemBundle() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);
        Set<Plugin> resultSet = new LinkedHashSet<>();
        resultSet.add(systemBundle);
        ManifestEntry entry = new ManifestEntry("org.eclipse.osgi", "");
        ManifestEntry entry2 = new ManifestEntry("system.bundle", "");
        assertEquals(depres.searchInPluginSet(entry, false),
                depres.searchInPluginSet(entry2, false));
        assertEquals(resultSet, depres.searchInPluginSet(entry2, false));
    }

}
