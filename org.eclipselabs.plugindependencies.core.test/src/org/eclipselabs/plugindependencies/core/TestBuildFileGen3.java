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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.eclipselabs.plugindependencies.core.fixture.SecurityMan;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBuildFileGen3 extends BaseTest {

    private static final String TESTDATA_ROOT = "testdata_circular2";

    private Path pluginRootDir;
    private File[] pluginDirs;
    private Path root;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setSecurityManager(new SecurityMan());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.setSecurityManager(null);
    }

    @Override
    @Before
    public void setup() {
        root = Paths.get(TESTDATA_ROOT);
        pluginRootDir = root.resolve("eclipse/plugins");
        pluginDirs = PluginParser.sortFiles(pluginRootDir.toFile().listFiles());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        int undeletedFiles = 0;
        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            File classpathfile = new File(path + "/.classpath-generated");
            if (classpathfile.exists() && !classpathfile.delete()) {
                undeletedFiles++;
            }
        }
        File dependencies = new File(TESTDATA_ROOT, "dependencies.txt");
        if (dependencies.exists() && !dependencies.delete()) {
            undeletedFiles++;
        }

        super.tearDown();

        if (undeletedFiles != 0) {
            throw new Exception("Error while deleting files: " + undeletedFiles
                    + " not deleted");
        }
    }

    @Test
    public void testGenerationWithIgnoredCycleErrors() throws IOException {
        String args[] = new String[] { "-eclipsePaths", TESTDATA_ROOT + "/eclipse",
                "-deploymentRoot", TESTDATA_ROOT + "/eclipse",
                "-ignoreCycles", "de.cau.cs.kieler.klighd", "de.cau.cs.kieler.klighd.ui",
                "-bundleVersion", "99.0.0",
                "-generateAllBuild",
                TESTDATA_ROOT + "/eclipse/plugins", "company/eclipse/plugins"};

        assertEquals(CommandLineInterpreter.RC_OK, SecurityMan.runMain(args));

        CommandLineInterpreter cli = MainClass.interpreter;
        PlatformState state = cli.getState();

        asertNoErrors("itee.isv.ui", state);
        asertNoErrors("itee.icdiagram", state);
        asertOnlyWarnings("de.cau.cs.kieler.klighd.ui", state);
        asertOnlyWarnings("de.cau.cs.kieler.klighd.kivi", state);
        asertOnlyWarnings("de.cau.cs.kieler.klighd", state);

        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            Path expected = Paths.get(path, "classpathfile_expected");
            File actual = new File(path, ".classpath.generated");
            List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
            expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(actual.toPath(), StandardCharsets.UTF_8);
            outputList = TestCLI.addNewlineToAllStrings(outputList);

            assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                    expectedOutputList.toString(), outputList.toString());
        }

    }

    @Test
    public void testGenerationWithOneCycleError() throws IOException {
        String args[] = new String[] { "-eclipsePaths", TESTDATA_ROOT + "/eclipse",
                "-deploymentRoot", TESTDATA_ROOT + "/eclipse",
                "-ignoreCycles", "de.cau.cs.kieler.klighd",
                "-bundleVersion", "99.0.0",
                "-generateAllBuild",
                TESTDATA_ROOT + "/eclipse/plugins", "company/eclipse/plugins"};

        assertEquals(CommandLineInterpreter.RC_ANALYSIS_ERROR, SecurityMan.runMain(args));

        CommandLineInterpreter cli = MainClass.interpreter;
        PlatformState state = cli.getState();

        asertNoErrors("itee.isv.ui", state);
        asertNoErrors("itee.icdiagram", state);
        asertErrors("de.cau.cs.kieler.klighd.ui", state);
        asertErrors("de.cau.cs.kieler.klighd.kivi", state);
        asertOnlyWarnings("de.cau.cs.kieler.klighd", state);

        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            Path expected = Paths.get(path, "classpathfile_expected");
            File actual = new File(path, ".classpath.generated");
            List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
            expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(actual.toPath(), StandardCharsets.UTF_8);
            outputList = TestCLI.addNewlineToAllStrings(outputList);

            assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                    expectedOutputList.toString(), outputList.toString());
        }

    }

    @Test
    public void testRequirementsFile() throws IOException {
        String args[] = new String[] { "-eclipsePaths", TESTDATA_ROOT + "/eclipse",
                "-ignoreCycles", "de.cau.cs.kieler.klighd", "de.cau.cs.kieler.klighd.ui",
                "-generateReqFile", TESTDATA_ROOT + "/dependencies.txt" };

        assertEquals(CommandLineInterpreter.RC_OK, SecurityMan.runMain(args));

        Path expected = Paths.get(TESTDATA_ROOT, "dependencies_expected.txt");
        Path act = Paths.get(TESTDATA_ROOT, "dependencies.txt");
        List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
        expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(act, StandardCharsets.UTF_8);
        outputList = TestCLI.addNewlineToAllStrings(outputList);

        assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                expectedOutputList.toString(), outputList.toString());
    }


    void asertErrors(String id, PlatformState state) {
        List<Problem> log = state.getPlugin(id).getLog();
        assertFalse("Should have errors: " + log, log.stream().filter(p -> p.isError()).count() == 0);
    }

    void asertNoErrors(String id, PlatformState state) {
        List<Problem> log = state.getPlugin(id).getLog();
        assertFalse("Unexpected errors: " + log, log.stream().filter(p -> p.isError()).count() > 0);
    }

    void asertOnlyWarnings(String id, PlatformState state) {
        List<Problem> log = state.getPlugin(id).getLog();
        assertFalse("Unexpected errors: " + log, log.stream().filter(p -> p.isError()).count() > 0);
        assertFalse("Should have warnings: " + log, log.stream().filter(p -> p.isWarning()).count() == 0);
    }

}
