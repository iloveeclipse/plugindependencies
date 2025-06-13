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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDepResSearchMethodPack extends BaseTest {
    Set<Package> packageSet;

    Set<Plugin> pluginSet;

    Set<Feature> featureSet;

    Package package1;

    Package package2;

    Package package3;

    DependencyResolver depres;

    Set<Capability> capabilitiesSet;

    @Before
    public void setUpBeforeClass() throws Exception {
        package1 = new Package("com.company.core", "3.4.5");
        package2 = new Package("com.company.core", "1.2.0");
        package3 = new Package("org.eclipse.core.net.proxy", null);
        packageSet = new LinkedHashSet<Package>();
        packageSet.add(new Package("org.eclipse.p1", "1.2.3"));
        packageSet.add(new Package("org.eclipse.p2", "4.5.6"));
        packageSet.add(new Package("com.example.itee.ate", "3.5.8"));
        packageSet.add(new Package("com.example.result", "99.0.0"));
        packageSet.add(new Package("com.company.itee.core", "3.8.2"));
        packageSet.add(new Package("com.company.tables", "9.2.0"));
        packageSet.add(new Package("javax.crypto", ""));
        packageSet.add(package1);
        packageSet.add(package2);
        packageSet.add(package3);
        pluginSet = new LinkedHashSet<Plugin>();
        Plugin plugin = new Plugin("org.eclipse.core.net", "1.2.0");
        plugin.getExportedPackages().add(package3);
        package3.addExportPlugin(plugin);
        pluginSet.add(plugin);
        featureSet = new LinkedHashSet<Feature>();
        capabilitiesSet = new LinkedHashSet<>();
    }

    @After
    public void tearDownAfterClass() throws Exception {
        packageSet = null;
        package1 = null;
        package2 = null;
        package3 = null;
        pluginSet = null;
        featureSet = null;
        capabilitiesSet = null;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        depres = null;
        super.tearDown();
    }

    @Test
    public void testSearchInJavaHomeJar() throws IOException {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet, capabilitiesSet);
        ManifestEntry entry = new ManifestEntry("javax.crypto", "");
        Package result = new Package("javax.crypto", "");

        assertEquals(result, depres.searchInJavaHomeJar(entry).iterator().next());

        entry = new ManifestEntry("Package.can.not.be.found", "");
        assertTrue(depres.searchInJavaHomeJar(entry).isEmpty());

        assertTrue(depres.searchInJavaHomeJar(null).isEmpty());

        entry = new ManifestEntry("", "");
        assertTrue(depres.searchInJavaHomeJar(entry).isEmpty());
    }

    @Test
    public void testSearchPackage() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet, capabilitiesSet);
        Set<Package> resultSet = new LinkedHashSet<Package>();
        ManifestEntry entry;

        resultSet.add(package1);

        entry = new ManifestEntry("com.company.core", "3.2");
        assertEquals(resultSet.toString(), depres.searchInPackageSet(entry).toString());

        entry = new ManifestEntry("com.company.core", "3.5");
        assertNotEquals(resultSet.toString(), depres.searchInPackageSet(entry));

        entry = new ManifestEntry("com.company.core", "[3.2.0,4.0.0)");
        assertEquals(resultSet.toString(), depres.searchInPackageSet(entry).toString());

        resultSet.add(package2);

        entry = new ManifestEntry("com.company.core", "");
        assertEquals(resultSet.toString(), depres.searchInPackageSet(entry).toString());

        resultSet.remove(package1);

        entry = new ManifestEntry("com.company.core", "[1.2,2.0.0)");
        assertEquals(resultSet.toString(), depres.searchInPackageSet(entry).toString());

        resultSet.clear();
        resultSet.add(new Package("javax.crypto", ""));

        entry = new ManifestEntry("javax.crypto", "[1.2,2.0.0)");
        assertEquals("[]", depres.searchInPackageSet(entry).toString());

        resultSet.clear();
        resultSet.add(package3);

        entry = new ManifestEntry(new ArrayList<>(Arrays.asList("org.eclipse.core.net.proxy", "bundle-version=\"[1.1.0,2.0.0)\"")));
        assertEquals(resultSet.toString(), depres.searchInPackageSet(entry).toString());
    }

    @Test
    public void testSearchPackageWrongPara() {
        ManifestEntry entry;
        depres = new DependencyResolver(pluginSet, packageSet, featureSet, capabilitiesSet);

        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPackageSet(null).toString());

        entry = new ManifestEntry("", "");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPackageSet(entry).toString());

        entry = new ManifestEntry("Package.can.not.be.found", "");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPackageSet(entry).toString());

        entry = new ManifestEntry("com.company.core", "Wrong.Version.Format");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInPackageSet(entry).toString());
    }
}
