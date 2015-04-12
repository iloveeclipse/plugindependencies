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
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
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
public class TestPlugin extends BaseTest {

    Plugin plugin;

    String requPlugins;

    String requPackages;

    String exportedPackages;

    @Before
    public void generateEnv() throws Exception {
        plugin = new Plugin("myPlugin", "2.3.4");
        requPlugins = "org.eclipse.core.runtime,org.eclipse.core.resources,org.eclipse.core.filesystem,org.eclipse.core.expressions,JSR305-ri;resolution:=optional";
        requPackages = "javax.xml.parsers,org.eclipse.core.runtime.jobs;resolution:=optional,org.eclipse.osgi.framework.console;resolution:=optional,org.eclipse.osgi.service.datalocation,org.eclipse.osgi.service.debug,org.eclipse.osgi.service.environment;resolution:=optional,org.eclipse.osgi.service.localization;version=\"1.1.0\",org.eclipse.osgi.service.resolver;resolution:=optional,org.eclipse.osgi.storagemanager,org.eclipse.osgi.util,org.osgi.framework,org.osgi.service.packageadmin,org.osgi.util.tracker,org.xml.sax,org.xml.sax.helpers";
        exportedPackages = "org.eclipse.core.internal.adapter;x-internal:=true,org.eclipse.core.internal.registry;x-friends:=\"org.eclipse.core.runtime\",org.eclipse.core.internal.registry.osgi;x-friends:=\"org.eclipse.core.runtime\",org.eclipse.core.internal.registry.spi;x-internal:=true,org.eclipse.core.runtime;registry=split;version=\"3.4.0\";mandatory:=registry,org.eclipse.core.runtime.dynamichelpers;version=\"3.4.0\",org.eclipse.core.runtime.spi;version=\"3.4.0\"";
    }

    @Override
    @After
    public void tearDown() throws Exception {
        plugin = null;
        super.tearDown();
    }

    @Test
    public void testSetRequiredPlugins() {
        plugin.setRequiredPlugins(requPlugins);
        ManifestEntry entry1 = new ManifestEntry("org.eclipse.core.runtime", null);
        ManifestEntry entry2 = new ManifestEntry("org.eclipse.core.resources", null);
        ManifestEntry entry3 = new ManifestEntry("org.eclipse.core.filesystem", null);
        ManifestEntry entry4 = new ManifestEntry("org.eclipse.core.expressions", null);
        ManifestEntry entry5 = new ManifestEntry(new String[] { "JSR305-ri",
                "resolution:=optional" });

        List<ManifestEntry> compareReqPlugins = new ArrayList<>();
        Collections.addAll(compareReqPlugins, entry1, entry2, entry3, entry4, entry5);

        assertEquals(compareReqPlugins, plugin.getRequiredPlugins());
    }

    @Test
    public void testSetRequiredPackages() {
        plugin.setRequiredPackages(requPackages);

        ManifestEntry entry1 = new ManifestEntry("javax.xml.parsers", null);
        ManifestEntry entry2 = new ManifestEntry(new String[] {
                "org.eclipse.core.runtime.jobs", "resolution:=optional" });
        ManifestEntry entry3 = new ManifestEntry(new String[] {
                "org.eclipse.osgi.framework.console", "resolution:=optional" });
        ManifestEntry entry4 = new ManifestEntry("org.eclipse.osgi.service.datalocation",
                null);
        ManifestEntry entry5 = new ManifestEntry("org.eclipse.osgi.service.debug", null);
        ManifestEntry entry6 = new ManifestEntry(new String[] {
                "org.eclipse.osgi.service.environment", "resolution:=optional" });
        ManifestEntry entry7 = new ManifestEntry(new String[] {
                "org.eclipse.osgi.service.localization", "version=\"1.1.0\"" });
        ManifestEntry entry8 = new ManifestEntry(new String[] {
                "org.eclipse.osgi.service.resolver", "resolution:=optional" });
        ManifestEntry entry9 = new ManifestEntry("org.eclipse.osgi.storagemanager", null);
        ManifestEntry entry10 = new ManifestEntry("org.eclipse.osgi.util", null);
        ManifestEntry entry11 = new ManifestEntry("org.osgi.framework", null);
        ManifestEntry entry12 = new ManifestEntry("org.osgi.service.packageadmin", null);
        ManifestEntry entry13 = new ManifestEntry("org.osgi.util.tracker", null);
        ManifestEntry entry14 = new ManifestEntry("org.xml.sax", null);
        ManifestEntry entry15 = new ManifestEntry("org.xml.sax.helpers", null);

        List<ManifestEntry> compareReqPackages = new ArrayList<>();
        Collections.addAll(compareReqPackages, entry1, entry2, entry3, entry4, entry5,
                entry6, entry7, entry8, entry9, entry10, entry11, entry12, entry13,
                entry14, entry15);

        assertEquals(compareReqPackages, plugin.getRequiredPackages());
    }

    @Test
    public void testSetExportedPackages() {
        plugin.setExportedPackages(exportedPackages);

        Set<Package> expPack = new LinkedHashSet<>();
        expPack.add(new Package("org.eclipse.core.internal.adapter", ""));
        expPack.add(new Package("org.eclipse.core.internal.registry", ""));
        expPack.add(new Package("org.eclipse.core.internal.registry.osgi", ""));
        expPack.add(new Package("org.eclipse.core.internal.registry.spi", ""));
        expPack.add(new Package("org.eclipse.core.runtime", "3.4.0"));
        expPack.add(new Package("org.eclipse.core.runtime.dynamichelpers", "3.4.0"));
        expPack.add(new Package("org.eclipse.core.runtime.spi", "3.4.0"));

        assertEquals(expPack, plugin.getExportedPackages());

        Package reexportedPackage = new Package("org.pack.reexport", "");
        reexportedPackage.addReExportPlugin(plugin);

        Set<Package> reexport = new LinkedHashSet<>();
        reexport.add(reexportedPackage);
        expPack.add(reexportedPackage);
        plugin.addReexportedPackages(reexport);

        assertEquals(expPack, plugin.getExportedPackages());

        Set<Plugin> packageReexporter = new LinkedHashSet<>();
        packageReexporter.add(plugin);
        assertEquals(packageReexporter, reexportedPackage.getReexportedBy());
    }

    @Test
    public void testOptionalWrongPara() {
        assertFalse(plugin.isOptional((Plugin) null));
        assertFalse(plugin.isOptional((Package) null));
    }

    @Test
    public void testSetMoreThanOneFragmentHost() {
        plugin.setFragmentHost("org.frag.host1,org.frag.host2");
        assertEquals("Error: fragment has more than one host", plugin.getLog().get(0));
    }

    @Test
    public void testClassPath() {
        String testString = "abc\nabc\tblablabla";
        Plugin plug = new Plugin("test.Plugin", "1.2.3");
        plug.setFullClassPaths(testString);
        assertEquals(testString, plug.getFullClassPaths());
    }
}
