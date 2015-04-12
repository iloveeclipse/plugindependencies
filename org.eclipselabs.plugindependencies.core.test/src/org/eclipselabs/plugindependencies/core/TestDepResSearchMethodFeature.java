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

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDepResSearchMethodFeature  extends BaseTest {
    static Set<Package> packageSet;

    static Set<Plugin> pluginSet;

    static Set<Feature> featureSet;

    static Feature feature1;

    static Feature feature2;

    DependencyResolver depres;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        feature1 = new Feature("com.company.core", "3.4.5");
        feature2 = new Feature("com.company.core", "1.2.0");
        featureSet = new LinkedHashSet<Feature>();
        featureSet.add(new Feature("org.eclipse.p1", "1.2.3"));
        featureSet.add(new Feature("org.eclipse.p2", "4.5.6"));
        featureSet.add(new Feature("com.example.itee.ate", "3.5.8"));
        featureSet.add(new Feature("com.example.result", "99.0.0"));
        featureSet.add(new Feature("com.company.itee.core", "3.8.2"));
        featureSet.add(new Feature("com.company.tables", "9.2.0"));
        featureSet.add(feature1);
        featureSet.add(feature2);
        packageSet = new LinkedHashSet<Package>();
        pluginSet = new LinkedHashSet<Plugin>();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        packageSet = null;
        feature1 = null;
        feature2 = null;
        pluginSet = null;
        featureSet = null;
    }

    @Test
    public void testSearchFeatureWrongPara() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInFeatureSet(null).toString());

        ManifestEntry featureEntry = new ManifestEntry("WrongElement", "");
        assertEquals(new LinkedHashSet<>().toString(), depres.searchInFeatureSet(featureEntry).toString());
    }

    @Test
    public void testSearchFeature() {
        depres = new DependencyResolver(pluginSet, packageSet, featureSet);

        Set<Feature> resultSet = new LinkedHashSet<>();

        resultSet.add(feature1);
        String id = feature1.getName();
        String version = feature1.getVersion();
        assertEquals(resultSet, depres.searchInFeatureSet(new ManifestEntry(id, version)));

        String version2 = "99.0.0";
        assertNotEquals(resultSet,
                depres.searchInFeatureSet(new ManifestEntry(id, version2)));
    }

}
