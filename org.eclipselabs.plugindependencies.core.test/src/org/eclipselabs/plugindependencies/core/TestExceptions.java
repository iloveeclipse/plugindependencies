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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author obroesam
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestExceptions extends BaseTest {
    @Test
    public void testDoublePluginWarning() throws IOException {
        Set<Plugin> plugins = new LinkedHashSet<>();
        Set<Package> packages = new LinkedHashSet<>();
        Set<Feature> features = new LinkedHashSet<>();
        Set<Capability> capabilities = new LinkedHashSet<>();
        String dir = "testdata_exceptions/plugins";

        PlatformState state = new PlatformState(plugins, packages, features, capabilities);
        new PluginParser(state).createPluginsAndAddToSet(new File(dir));
        state.resolveDependencies();
        Plugin orgExpect = new Plugin("", "");
        for (Plugin plugin : plugins) {
            if (plugin.getName().equals("org.xpect")) {
                orgExpect = plugin;
            }
        }

//        depres.resolvePluginDependency(orgExpect);
        List<String> log = new ArrayList<>();
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.apache.log4j 1.2.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.junit 4.11.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.antlr.runtime 3.2.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.eclipse.xtext 2.4.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.eclipse.xtext.util 2.4.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.eclipse.emf.ecore 2.7.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.eclipse.emf.common 2.7.0");
        log.add("Error: [org.xpect 0.1.0.201408281304] plugin not found: org.eclipse.xtext.common.types 2.4.0");
        log.add("Warning: [org.xpect 0.1.0.201408281304] more than one plugin found for org.objectweb.asm [3.0.0,6.0.0) *optional*");
        log.add("Error: [org.xpect 0.1.0.201408281304] package not found: org.apache.log4j ");

        assertEquals(fix(log.toString()), fix(orgExpect.getLog().toString()));
    }

}
