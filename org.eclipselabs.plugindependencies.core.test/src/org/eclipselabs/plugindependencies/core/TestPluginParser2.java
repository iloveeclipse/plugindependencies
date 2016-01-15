/*******************************************************************************
 * Copyright (c) 2016 Andrey Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import static org.eclipselabs.plugindependencies.core.PluginParser.getManifest;
import static org.eclipselabs.plugindependencies.core.PluginParser.parseManifest;
import static org.eclipselabs.plugindependencies.core.PluginParser.readAttribute;
import static org.eclipselabs.plugindependencies.core.StringUtil.splitInManifestEntries;
import static org.eclipselabs.plugindependencies.core.StringUtil.splitListOfEntries;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.IOException;
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
public class TestPluginParser2  extends BaseTest {

    private static String dirPath;

    private static File xtext;

    private static File xtextGenerator;

    private static File xtextUtil;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dirPath = "testdata";
        xtext = new File(dirPath + "/org.eclipse.xtext");
        xtextGenerator = new File(dirPath + "/org.eclipse.xtext.generator");
        xtextUtil = new File(dirPath + "/org.eclipse.xtext.util");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testReadPackages() throws IOException {
        PlatformState state = new PlatformState();
        Manifest xtextMf = getManifest(xtext);
        Plugin xtextPl = parseManifest(xtextMf, state);
        assertSame(xtextMf, xtextPl.getManifest());
        String value = xtextMf.getMainAttributes().getValue("Export-Package");
        String export = readAttribute(xtextMf, "Export-Package");
        assertEquals(value,  export);
        List<ManifestEntry> packages = splitInManifestEntries(export);
        List<String> packStr = splitListOfEntries(export);
        assertEquals(packages.size(), packStr.size());
        Set<Package> exportedPackages = xtextPl.getExportedPackages();
        assertEquals(packages.size(), exportedPackages.size());

        Manifest xtextGenMf = getManifest(xtextGenerator);
        Plugin xtextGenPl = parseManifest(xtextGenMf, state);
        assertSame(xtextGenMf, xtextGenPl.getManifest());
        value = xtextGenMf.getMainAttributes().getValue("Export-Package");
        export = readAttribute(xtextGenMf, "Export-Package");
        assertEquals(value,  export);
        List<ManifestEntry> packagesGen = splitInManifestEntries(export);
        List<String> packGenStr = splitListOfEntries(export);
        assertEquals(packagesGen.size(), packGenStr.size());
        Set<Package> exportedGenPackages = xtextGenPl.getExportedPackages();
        assertEquals(packagesGen.size(), exportedGenPackages.size());
    }

    @Test
    public void testReadReexported() throws IOException {
        PlatformState state = new PlatformState();
        Manifest xtextMf = getManifest(xtext);
        Plugin xtextPl = parseManifest(xtextMf, state);
        Manifest xtextUtilMf = getManifest(xtextUtil);
        Plugin xtextUtilPl = parseManifest(xtextUtilMf, state);
        state.addPlugin(xtextPl);
        state.addPlugin(xtextUtilPl);

        state.resolveDependencies();

//        String value = xtextMf.getMainAttributes().getValue("Export-Package");
//        String export = readAttribute(xtextMf, "Export-Package");
//        export = readAttribute(xtextMf, "Require-Bundle");
        Set<Plugin> reex = xtextPl.getRequiredReexportedPlugins();
        assertEquals("Reex: " + reex, 1, reex.size());
    }


}
