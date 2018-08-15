/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.Test;

/**
 * @author aloskuto
 *
 */
public class TestDependencyResolver extends BaseTest {

    @Test
    public void testImportAmbiguousPackages() {

        Set<Plugin> plugins = new LinkedHashSet<>();
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);

        p1.setExportedPackages("hello", ps);
        p2.setExportedPackages("hello", ps);

        p3.setImportedPackageEntries("hello");
        ps.computeAllDependenciesRecursive();

        assertEquals("[Warning: [hello] package contributed by multiple, not related plugins]", ps.getPackage("hello").getLog().toString());
        assertEquals("[Warning: [p1 1.0.0] this plugin is one of 2 plugins contributing package 'hello']", p1.getLog().toString());
        assertEquals("[Warning: [p2 1.0.0] this plugin is one of 2 plugins contributing package 'hello']", p2.getLog().toString());
        assertEquals("[Warning: [p3 1.0.0] this plugin uses package 'hello' contributed by multiple plugins]", p3.getLog().toString());
    }

    @Test
    public void testImportAmbiguousPackages2() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);


        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello;version=\"0.5.0\"", ps);
        ps.computeAllDependenciesRecursive();


        assertEquals("[Warning: [hello 0.5.0] package contributed by multiple, not related plugins]", ps.getPackage("hello").getLog().toString());
        assertEquals("[Warning: [p1 1.0.0] this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p1.getLog().toString());
        assertEquals("[Warning: [p2 1.0.0] this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p2.getLog().toString());
        assertEquals("[Warning: [p3 1.0.0] this plugin uses package 'hello 0.5.0' contributed by multiple plugins]", p3.getLog().toString());
    }

    @Test
    public void testImportNonAmbiguousPackages() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);


        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello", ps);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
        assertEquals("[]", p3.getLog().toString());
        assertEquals(2,  ps.getPackages("hello").size());
        Set<Package> packages = ps.getPackages("hello");
        for (Package p : packages) {
            assertEquals("[]", p.getLog().toString());
        }
    }

    @Test
    public void testImportNonAmbiguousPackages2() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p2.setRequiredPlugins("p1;version=\"1.0.0\"");

        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello;version=\"0.5.0\"", ps);
        ps.computeAllDependenciesRecursive();

        assertEquals("[Warning: [p1 1.0.0] this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p1.getLog().toString());
        assertEquals("[Warning: [p2 1.0.0] this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p2.getLog().toString());
        assertEquals("[Warning: [p3 1.0.0] this plugin uses package 'hello 0.5.0' contributed by multiple plugins]", p3.getLog().toString());
        assertEquals(1,  ps.getPackages("hello").size());
        Set<Package> packages = ps.getPackages("hello");
        for (Package p : packages) {
            assertEquals("[Warning: [hello 0.5.0] package contributed by multiple, not related plugins]", p.getLog().toString());
        }
    }

    public void testImportNonAmbiguousPackages22() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p2.setRequiredPlugins("p1;version=\"1.0.0\";visibility:=\"reexport\"");

        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello;version=\"0.5.0\"", ps);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
        assertEquals("[]", p3.getLog().toString());
        assertEquals(1,  ps.getPackages("hello").size());
        Set<Package> packages = ps.getPackages("hello");
        for (Package p : packages) {
            assertEquals("[]", p.getLog().toString());
        }
    }

    @Test
    public void testImportNonAmbiguousPackages3() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p1", "2.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);


        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello;version=\"0.5.0\"", ps);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
        assertEquals("[]", p3.getLog().toString());
        assertEquals(1,  ps.getPackages("hello").size());
        Set<Package> packages = ps.getPackages("hello");
        for (Package p : packages) {
            assertEquals("[]", p.getLog().toString());
        }
    }

    @Test
    public void testImportNonAmbiguousPackages4() {
        Plugin p1 = new Plugin("p1", "1.0.0", true, false);
        Plugin p2 = new Plugin("p2", "2.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);


        p1.setFragmentHost("p2");

        p3.setImportedPackageEntries("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello;version=\"0.5.0\"", ps);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
        assertEquals("[]", p3.getLog().toString());
        assertEquals(1,  ps.getPackages("hello").size());
        Set<Package> packages = ps.getPackages("hello");
        for (Package p : packages) {
            assertEquals("[]", p.getLog().toString());
        }
    }


    @Test
    public void testImportSourcePlugin() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);

        p2.setRequiredPlugins("p1;version=\"1.0.0\"");
        p2.setRequiredPlugins("p1.source;version=\"1.0.0\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();

        assertEquals("[Warning: [p2 1.0.0] plugin not found: p1.source 1.0.0]", p2.getLog().toString());
    }

    @Test
    public void testSimpleCycle() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);

        p2.setRequiredPlugins("p1;version=\"1.0.0\"");
        p1.setRequiredPlugins("p2;version=\"1.0.0\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();

        assertEquals("[Error: [p1 1.0.0] Dependency cycle detected with p2 1.0.0]", p1.getLog().toString());
    }

    @Test
    public void testIndirectCycle() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p1.setRequiredPlugins("p2;version=\"1.0.0\"");
        p2.setRequiredPlugins("p3;version=\"1.0.0\"");
        p3.setRequiredPlugins("p1;version=\"1.0.0\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();

        assertEquals("[Error: [p1 1.0.0] Dependency cycle detected with p3 1.0.0]", p1.getLog().toString());
    }

    @Test
    public void testIndirectCycleViaPackages() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);


        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        p1.setExportedPackages("hello1;version=\"0.5.0\"", ps);
        p2.setExportedPackages("hello2;version=\"0.5.0\"", ps);
        p3.setExportedPackages("hello3;version=\"0.5.0\"", ps);

        p1.setImportedPackageEntries("hello2;version=\"0.5.0\"");
        p2.setImportedPackageEntries("hello3;version=\"0.5.0\"");
        p3.setImportedPackageEntries("hello1;version=\"0.5.0\"");

        ps.computeAllDependenciesRecursive();

        assertEquals("[Error: [p1 1.0.0] Dependency cycle detected with p3 1.0.0]", p1.getLog().toString());
    }

    @Test
    public void testMultipleOccurenciesOfSamePluginInPlatform() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        p1.setPath("/tmp");
        Plugin p2 = new Plugin("p1", "1.0.0", false, false);
        p2.setPath("/tmp");


        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.addPlugin(p2);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
    }
}
