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
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFeatureParser  extends BaseTest {
    static String dirPath;

    static Document orgEclipseCdt;

    static Document orgEclipseCdtGdb;

    static Document orgEclipseZest;

    static DocumentBuilder builder;

    static Set<Feature> compareFeatureSet;

    Set<Feature> featureSet;

    ManifestEntry entry;

    ManifestEntry entry2;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dirPath = "testdata_Features";
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        builder = dbFactory.newDocumentBuilder();

        orgEclipseCdt = builder.parse(dirPath
                + "/org.eclipse.cdt_8.0.2.201202111925/feature.xml");
        orgEclipseCdtGdb = builder.parse(dirPath
                + "/org.eclipse.cdt.gdb_7.0.0.201202111925/feature.xml");
        orgEclipseZest = builder.parse(dirPath
                + "/org.eclipse.zest_1.5.1.201308190730/feature.xml");

        compareFeatureSet = new LinkedHashSet<>();
        compareFeatureSet.add(new Feature("org.eclipse.cdt", "8.0.2.201202111925"));
        compareFeatureSet.add(new Feature("org.eclipse.cdt.gdb", "7.0.0.201202111925"));
        compareFeatureSet.add(new Feature("org.eclipse.zest", "1.5.1.201308190730"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        dirPath = null;
        builder = null;
        orgEclipseCdt = null;
        orgEclipseCdtGdb = null;
        orgEclipseZest = null;
        compareFeatureSet = null;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        featureSet = null;
        entry = null;
        entry2 = null;
        super.tearDown();
    }

    @Test
    public void testReadFeatures() throws IOException, SAXException,
            ParserConfigurationException {
        PlatformState state = new PlatformState(null, null, new LinkedHashSet<Feature>());
        FeatureParser.createFeaturesAndAddToSet(new File(dirPath), state);
        assertEquals(compareFeatureSet, state.getFeatures());

        Feature orgEclipseCdtFeat = new Feature("", "");
        for (Feature feature : state.getFeatures()) {
            if (feature.getName().equals("org.eclipse.cdt")) {
                if (feature.getVersion().equals("8.0.2.201202111925")) {
                    orgEclipseCdtFeat = feature;
                }
            }
        }

        String path = System.getProperty("user.dir")
                + "/testdata_Features/org.eclipse.cdt_8.0.2.201202111925/feature.xml";
        assertEquals(path, orgEclipseCdtFeat.getPath());
    }

    @Test
    public void testReadFeaturesWrongArguments() throws IOException, SAXException,
            ParserConfigurationException {
        PlatformState state = new PlatformState(null, null, new LinkedHashSet<Feature>());
        try {
            FeatureParser.createFeaturesAndAddToSet(null, state);
            fail();
        } catch (NullPointerException e) {
            // expected
        }
        assertTrue(state.getFeatures().isEmpty());

        state = new PlatformState(null, null, new LinkedHashSet<Feature>());
        FeatureParser.createFeaturesAndAddToSet(new File("/folder/does/not/exist"), state);
        assertTrue(state.getFeatures().isEmpty());
    }

    @Test
    public void testParseFeatureOrgEclipseCdt() {
        Feature feature = FeatureParser.parseFeature(orgEclipseCdt);

        assertEquals("org.eclipse.cdt", feature.getName());
        assertEquals("8.0.2.201202111925", feature.getVersion());

        entry = feature.getRequiredFeatureEntries().get(0);
        entry2 = new ManifestEntry("org.eclipse.cdt.platform", "8.0.2.201202111925");
        assertEquals(entry2, entry);

        entry = feature.getRequiredFeatureEntries().get(1);
        entry2 = new ManifestEntry("org.eclipse.cdt.gnu.build", "8.0.2.201202111925");
        assertEquals(entry2, entry);

        entry = feature.getRequiredFeatureEntries().get(2);
        entry2 = new ManifestEntry("org.eclipse.cdt.gdb", "7.0.0.201202111925");
        assertEquals(entry2, entry);

        entry = feature.getRequiredFeatureEntries().get(3);
        entry2 = new ManifestEntry("org.eclipse.cdt.gnu.debug", "7.1.1.201202111925");
        assertEquals(entry2, entry);

        entry = feature.getRequiredFeatureEntries().get(4);
        entry2 = new ManifestEntry("org.eclipse.cdt.gnu.dsf", "4.0.1.201202111925");
        assertEquals(entry2, entry);

        assertTrue(feature.getRequiredPluginEntries().isEmpty());
    }

    @Test
    public void testParseFeatureOrgEclipseCdtGdb() {
        Feature feature = FeatureParser.parseFeature(orgEclipseCdtGdb);
        assertEquals("org.eclipse.cdt.gdb", feature.getName());
        assertEquals("7.0.0.201202111925", feature.getVersion());

        entry = feature.getRequiredPluginEntries().get(0);
        entry2 = new ManifestEntry("org.eclipse.cdt.gdb", "7.0.0.201202111925");
        assertEquals(entry2, entry);

        entry = feature.getRequiredPluginEntries().get(1);
        entry2 = new ManifestEntry("org.eclipse.cdt.gdb.ui", "7.0.0.201202111925");
        assertEquals(entry2, entry);

        assertTrue(feature.getRequiredFeatureEntries().isEmpty());
    }

    @Test
    public void testParseFeatureOrgEclipseZest() {
        Feature feature = FeatureParser.parseFeature(orgEclipseZest);
        assertEquals("org.eclipse.zest", feature.getName());
        assertEquals("1.5.1.201308190730", feature.getVersion());

        entry = feature.getRequiredPluginEntries().get(0);
        entry2 = new ManifestEntry("org.eclipse.zest.core", "1.5.0.201308190730");
        assertEquals(entry2, entry);

        entry = feature.getRequiredPluginEntries().get(1);
        entry2 = new ManifestEntry("org.eclipse.zest.layouts", "1.1.0.201308190730");
        assertEquals(entry2, entry);

        entry = feature.getRequiredFeatureEntries().get(0);
        entry2 = new ManifestEntry("org.eclipse.draw2d", "3.9.1.201308190730");
        assertEquals(entry2, entry);
    }

    @Test
    public void testParseFeatureNoXml() {
        assertNull(FeatureParser.parseFeature(null));
        assertEquals(null, FeatureParser.parseFeature(builder.newDocument()));
    }
}
