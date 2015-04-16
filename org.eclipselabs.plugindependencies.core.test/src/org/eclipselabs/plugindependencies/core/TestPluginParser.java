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

import static org.eclipselabs.plugindependencies.core.StringUtil.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPluginParser  extends BaseTest {
    Set<Plugin> pluginSet;

    Set<Package> packageSet;

    static String dirPath;

    static File noPlugin;

    static File testPlugin;

    static File orgEclipseAntOptionalJunit;

    static File orgEclipseEquinoxApp;

    static File plugin6;

    static ManifestEntry fragmentHost;

    static List<ManifestEntry> compareReqPluginsOfTestPlugin;

    static List<ManifestEntry> compareReqPackagesOfTestPlugin;

    static List<ManifestEntry> compareReqPluginsOforgEclipseAnt;

    static List<ManifestEntry> compareReqPackagesOforgEclipseAnt;

    static List<ManifestEntry> compareReqPluginsOfOrgEclipseEqu;

    static List<ManifestEntry> compareReqPackagesOfOrgEclipseEqu;

    static Set<Package> expPackagesOfTestPlugin;

    static Set<Package> expPackagesOfOrgEclipseEqu;

    static Set<Plugin> plugins;

    static Plugin plugin1;

    static Plugin plugin2;

    static Set<Plugin> checkForDoubleExport;

    static Set<Package> packages;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dirPath = "testdata_Plugins";

        noPlugin = new File(dirPath + "/noPlugin");
        testPlugin = new File(dirPath + "/testPlugin");
        orgEclipseAntOptionalJunit = new File(dirPath
                + "/org.eclipse.ant.optional.junit_3.3.0.jar");
        orgEclipseEquinoxApp = new File(dirPath
                + "/org.eclipse.equinox.app_1.3.100.v20110321.jar");

        plugin6 = new File("testdata_dependencies/eclipse/plugins/org.company.workcenter");

        ManifestEntry entry;

        compareReqPluginsOfTestPlugin = new ArrayList<>();
        entry = new ManifestEntry("org.eclipse.core.runtime", "");
        compareReqPluginsOfTestPlugin.add(entry);
        entry = new ManifestEntry("org.eclipse.core.resources", "");
        compareReqPluginsOfTestPlugin.add(entry);
        entry = new ManifestEntry("org.eclipse.core.filesystem", "");
        compareReqPluginsOfTestPlugin.add(entry);
        entry = new ManifestEntry("org.eclipse.core.expressions", "");
        compareReqPluginsOfTestPlugin.add(entry);
        entry = new ManifestEntry(asList("JSR305-ri", "resolution:=optional" ));
        compareReqPluginsOfTestPlugin.add(entry);

        compareReqPackagesOfTestPlugin = new ArrayList<>();
        entry = new ManifestEntry("org.eclipse.swt.widgets", "");
        compareReqPackagesOfTestPlugin.add(entry);

        expPackagesOfTestPlugin = new LinkedHashSet<>();
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.concurrent", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.events", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.events.internal",
                ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.internal", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.internal.log", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.internal.requests",
                ""));
        expPackagesOfTestPlugin.add(new Package(
                "com.company.itee.core.internal.responses", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.requests", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.responses", ""));
        expPackagesOfTestPlugin.add(new Package("com.company.itee.core.util", ""));
        expPackagesOfTestPlugin.add(new Package("org.eclipse.equinox.app", "1.1"));

        compareReqPluginsOforgEclipseAnt = new ArrayList<>();
        entry = new ManifestEntry(asList("org.junit", "bundle-version=4.11.0",
                "resolution:=optional" ));
        compareReqPluginsOforgEclipseAnt.add(entry);

        compareReqPackagesOforgEclipseAnt = new ArrayList<>();

        fragmentHost = new ManifestEntry(asList("org.apache.ant",
                "bundle-version=\"[1.6.5,2.0.0)\"" ));

        compareReqPluginsOfOrgEclipseEqu = new ArrayList<>();
        entry = new ManifestEntry(asList("org.eclipse.equinox.registry",
                "bundle-version=\"[3.4.0,4.0.0)\"" ));
        compareReqPluginsOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.equinox.common",
                "bundle-version=\"[3.2.0,4.0.0)\"" ));
        compareReqPluginsOfOrgEclipseEqu.add(entry);

        compareReqPackagesOfOrgEclipseEqu = new ArrayList<>();
        entry = new ManifestEntry(asList("org.eclipse.osgi.framework.console",
                "resolution:=optional" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.framework.log" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(
                asList("org.eclipse.osgi.service.datalocation" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.service.debug" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.service.environment",
                "version=\"1.1\"" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.service.runnable" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.storagemanager" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.eclipse.osgi.util" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(
                asList("org.osgi.framework", "version=\"1.3\"" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.osgi.service.condpermadmin",
                "resolution:=optional" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.osgi.service.event",
                "version=\"1.0.0\"", "resolution:=optional" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.osgi.service.packageadmin",
                "version=\"1.2\"" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.osgi.util.tracker" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);
        entry = new ManifestEntry(asList("org.osgi.service.event",
                "version=\"1.0.0\"", "dynamicImport" ));
        compareReqPackagesOfOrgEclipseEqu.add(entry);

        expPackagesOfOrgEclipseEqu = new LinkedHashSet<>();
        expPackagesOfOrgEclipseEqu.add(new Package("org.eclipse.equinox.app", "1.1"));
        expPackagesOfOrgEclipseEqu
                .add(new Package("org.eclipse.equinox.internal.app", ""));
        expPackagesOfOrgEclipseEqu
                .add(new Package("org.osgi.service.application", "1.1"));

        packages = new LinkedHashSet<>();
        packages.addAll(expPackagesOfTestPlugin);
        packages.addAll(expPackagesOfOrgEclipseEqu);

        plugin1 = new Plugin("com.company.itee.core", "99.0.0");
        plugin2 = new Plugin("org.eclipse.equinox.app", "1.3.100.v20110321");

        checkForDoubleExport = new LinkedHashSet<>();
        checkForDoubleExport.add(plugin2);
        checkForDoubleExport.add(plugin1);

        plugins = new LinkedHashSet<>();
        plugins.add(new Plugin("org.eclipse.ant.optional.junit", "3.3.0", true, false));
        plugins.addAll(checkForDoubleExport);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        noPlugin = null;

        testPlugin = null;

        orgEclipseAntOptionalJunit = null;

        orgEclipseEquinoxApp = null;

        plugin6 = null;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        pluginSet = null;
        packageSet = null;
        super.tearDown();
    }

    @Test
    public void testReadManifests() throws IOException {
        pluginSet = new LinkedHashSet<>();
        packageSet = new LinkedHashSet<>();

        PluginParser.createPluginsAndAddToSet(new File(dirPath), new PlatformState(pluginSet, packageSet, null));
        assertEquals(packages, packageSet);
        assertEquals(plugins.toString(), pluginSet.toString());

        Package doubleExported = new Package("", "");
        for (Package pack : packageSet) {
            if (pack.getName().equals("org.eclipse.equinox.app")) {
                if (DependencyResolver.isCompatibleVersion("1.1", pack.getVersion())) {
                    doubleExported = pack;
                }
            }
        }
        assertEquals(checkForDoubleExport.toString(), doubleExported.getExportedBy().toString());

        Plugin forPathCheck = new Plugin("", "");
        for (Plugin plugin : pluginSet) {
            if (plugin.getName().equals("org.eclipse.equinox.app")) {
                if (DependencyResolver
                        .isCompatibleVersion("1.3.100", plugin.getVersion())) {
                    forPathCheck = plugin;
                }
            }
        }
        String path = System.getProperty("user.dir")
                + "/testdata_Plugins/org.eclipse.equinox.app_1.3.100.v20110321.jar";
        assertEquals(path, forPathCheck.getPath());
    }

    @Test
    public void testParseManifestOfFolder() throws IOException {
        Manifest mf = PluginParser.getManifest(testPlugin);
        Plugin plugin = PluginParser.parseManifest(mf);
        assertEquals("com.company.itee.core", plugin.getName());
        assertEquals("99.0.0", plugin.getVersion());
        assertEquals(compareReqPluginsOfTestPlugin, plugin.getRequiredPlugins());
        assertEquals(compareReqPackagesOfTestPlugin, plugin.getRequiredPackages());
        assertEquals(expPackagesOfTestPlugin, plugin.getExportedPackages());
        assertFalse(plugin.isFragment());
        assertNull(plugin.getFragmentHost());
        List<String> compareClasspath = new ArrayList<>();
        compareClasspath.add("lib/lib1.jar");
        compareClasspath.add("lib/lib2.jar");
        compareClasspath.add("lib3.jar");
        assertEquals(compareClasspath, plugin.getBundleClassPath());

        Plugin plug = new Plugin("JSR305-ri", "");
        assertTrue(plugin.isOptional(plug));
        plug = new Plugin("org.eclipse.core.resources", "");
        assertFalse(plugin.isOptional(plug));
    }

    @Test
    public void testParseManifestOfJar() throws IOException {
        Manifest mf = PluginParser.getManifest(orgEclipseAntOptionalJunit);
        Plugin plugin = PluginParser.parseManifest(mf);
        assertEquals("org.eclipse.ant.optional.junit", plugin.getName());
        assertEquals("3.3.0", plugin.getVersion());
        assertEquals(compareReqPluginsOforgEclipseAnt, plugin.getRequiredPlugins());
        assertEquals(compareReqPackagesOforgEclipseAnt, plugin.getRequiredPackages());
        assertEquals(new LinkedHashSet<>().toString(), plugin.getExportedPackages().toString());
        assertTrue(plugin.isFragment());
        assertEquals(fragmentHost, plugin.getFragmentHost());
    }

    @Test
    public void testParseManifestOfJar2() throws IOException {
        Manifest mf = PluginParser.getManifest(orgEclipseEquinoxApp);
        Plugin plugin = PluginParser.parseManifest(mf);
        assertEquals("org.eclipse.equinox.app", plugin.getName());
        assertEquals("1.3.100.v20110321", plugin.getVersion());
        assertEquals(compareReqPluginsOfOrgEclipseEqu, plugin.getRequiredPlugins());
        assertEquals(compareReqPackagesOfOrgEclipseEqu.toString(), plugin.getRequiredPackages().toString());
        assertEquals(expPackagesOfOrgEclipseEqu, plugin.getExportedPackages());
        assertFalse(plugin.isFragment());
        assertTrue(plugin.isSingleton());
        assertNull(plugin.getFragmentHost());

        Package pack = new Package("org.eclipse.osgi.framework.console", "");
        assertTrue(plugin.isOptional(pack));
        pack = new Package("org.eclipse.osgi.framework.log", "");
        assertFalse(plugin.isOptional(pack));
    }

    @Test
    public void testParseManifest2() throws IOException {
        assertNull(PluginParser.getManifest(noPlugin));
        assertNull(PluginParser.parseManifest(PluginParser.getManifest(noPlugin)));
        assertNull(PluginParser.parseManifest(null));
        assertNull(PluginParser.parseManifest(new Manifest()));
    }

    @Test
    public void testParseManifest3() throws IOException {
        Manifest mf = PluginParser.getManifest(plugin6);
        Plugin plugin = PluginParser.parseManifest(mf);
        assertEquals("org.company.workcenter", plugin.getName());
        assertEquals("6.3.1", plugin.getVersion());
        Set<Package> exported = new LinkedHashSet<>();
        Package package1 = new Package("com.package.test", "4.11.0");
        Package package2 = new Package("junit.framework", "4.11.0");
        Package package3 = new Package("junit.runner", "4.11.0");
        exported.add(package1);
        exported.add(package2);
        exported.add(package3);
        assertEquals(exported, plugin.getExportedPackages());
    }

    @Test
    public void testNull() throws IOException {
        pluginSet = new LinkedHashSet<>();
        packageSet = new LinkedHashSet<>();

        try {
            PluginParser.createPluginsAndAddToSet(null, new PlatformState(pluginSet, packageSet, null));
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testFolderDoesNotExist() throws IOException {
        pluginSet = new LinkedHashSet<>();
        packageSet = new LinkedHashSet<>();

        PluginParser.createPluginsAndAddToSet(new File("/folder/does/not/exist"), new PlatformState(pluginSet, packageSet, null));
        assertTrue(pluginSet.isEmpty());
    }

    @Test
    public void testDummyManifestVersions() {
        String dummy = PlatformState.getBundleVersionForDummy();
        PlatformState.setBundleVersionForDummy("8.0.1");
        PlatformState.setDummyBundleVersion("99.0.0");
        try {
            ManifestEntry me = new ManifestEntry("a", "1.0.0");
            assertEquals("1.0.0", me.getVersion());
            me = new ManifestEntry("a", "99.0.0");
            assertEquals("8.0.1", me.getVersion());
        } finally {
            PlatformState.setBundleVersionForDummy(dummy);
        }
    }

}
