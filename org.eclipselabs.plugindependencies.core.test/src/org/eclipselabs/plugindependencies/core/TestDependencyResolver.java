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

import static org.junit.Assert.*;

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
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p1.setExportedPackages("hello");
        p2.setExportedPackages("hello");

        p3.setRequiredPackages("hello");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();

        assertEquals("[Warning: package contributed by multiple plugins]", ps.getPackage("hello").getLog().toString());
        assertEquals("[Warning: this plugin is one of 2 plugins contributing package 'hello']", p1.getLog().toString());
        assertEquals("[Warning: this plugin is one of 2 plugins contributing package 'hello']", p2.getLog().toString());
        assertEquals("[Warning: this plugin uses package 'hello' contributed by multiple plugins]", p3.getLog().toString());
    }

    @Test
    public void testImportAmbiguousPackages2() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p1.setExportedPackages("hello;version=0.5.0");
        p2.setExportedPackages("hello;version=0.5.0");

        p3.setRequiredPackages("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();


        assertEquals("[Warning: package contributed by multiple plugins]", ps.getPackage("hello").getLog().toString());
        assertEquals("[Warning: this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p1.getLog().toString());
        assertEquals("[Warning: this plugin is one of 2 plugins contributing package 'hello 0.5.0']", p2.getLog().toString());
        assertEquals("[Warning: this plugin uses package 'hello 0.5.0' contributed by multiple plugins]", p3.getLog().toString());

    }

    @Test
    public void testImportNonAmbiguousPackages() {
        Plugin p1 = new Plugin("p1", "1.0.0", false, false);
        Plugin p2 = new Plugin("p2", "1.0.0", false, false);
        Plugin p3 = new Plugin("p3", "1.0.0", false, false);

        p1.setExportedPackages("hello;version=0.5.0");
        p2.setExportedPackages("hello");

        p3.setRequiredPackages("hello;version=\"[0.5.0,1.0.0]\"");

        Set<Plugin> plugins = new LinkedHashSet<>();
        plugins.add(p1);
        plugins.add(p2);
        plugins.add(p3);

        PlatformState ps = new PlatformState(plugins, null, null);
        ps.computeAllDependenciesRecursive();

        assertEquals("[]", p1.getLog().toString());
        assertEquals("[]", p2.getLog().toString());
        assertEquals("[]", p3.getLog().toString());
        assertEquals(2,  ps.getPackages("hello").size());
    }

}