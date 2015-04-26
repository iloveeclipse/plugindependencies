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
 * @author obroesam
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBuildFileGen2 extends BaseTest {

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
        root = Paths.get("testdata_dependencies");
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
        File dependencies = new File("testdata_dependencies/dependencies.txt");
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
    public void testGeneration() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_dependencies/eclipse",
                "-deploymentRoot", "testdata_dependencies/eclipse",
                "-bundleVersion", "99.0.0", "-generateAllBuild",
                "testdata_dependencies/eclipse/plugins", "company/eclipse/plugins"};

        assertEquals(0, SecurityMan.runMain(args));

        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            Path expected = Paths.get(path, "/classpathfile_expected");
            File actual = new File(path + "/.classpath.generated");
            List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
            expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(actual.toPath(), StandardCharsets.UTF_8);
            outputList = TestCLI.addNewlineToAllStrings(outputList);

            assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                    expectedOutputList.toString(), outputList.toString());
        }

    }

    @Test
    public void testBrokenGenerationDueCircularClasspaths() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_circular/eclipse",
                "-deploymentRoot", "testdata_circular/eclipse",
                "-bundleVersion", "99.0.0", "-generateAllBuild",
                "testdata_circular/eclipse/plugins", "company/eclipse/plugins"};

        assertEquals(-1, SecurityMan.runMain(args));

        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            Path expected = Paths.get(path, "/classpathfile_expected");
            File actual = new File(path + "/.classpath.generated");
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
        String args[] = new String[] { "-eclipsePaths", "testdata_dependencies/eclipse",
                "-generateReqFile", "testdata_dependencies/dependencies.txt" };

        assertEquals(0, SecurityMan.runMain(args));

        String folder = "testdata_dependencies";
        Path expected = Paths.get(folder, "dependencies_expected.txt");
        Path act = Paths.get(folder, "dependencies.txt");
        List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
        expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(act, StandardCharsets.UTF_8);
        outputList = TestCLI.addNewlineToAllStrings(outputList);

        assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                expectedOutputList.toString(), outputList.toString());
    }


}
