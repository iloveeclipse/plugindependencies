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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author obroesam
 *
 */
public class TestBuildFileGen {
    Path pluginRootDir = Paths.get("testdata_dependencies/eclipse/plugins");

    File[] pluginDirs = PluginParser.sortFiles(pluginRootDir.toFile().listFiles());
    Path root = Paths.get("testdata_OutputGeneration");
    File workspace = new File(root.toFile(), "/workspace");

    File[] workspacePlugins = PluginParser.sortFiles(workspace.listFiles());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setSecurityManager(new SecurityMan());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.setSecurityManager(null);
    }

    @After
    public void tearDown() throws Exception {
        int undeletedFiles = 0;
        for (File plugin : pluginDirs) {
            String path = plugin.getCanonicalPath();
            File classpathfile = new File(path + "/.classpath-gen");
            if (classpathfile.exists() && !classpathfile.delete()) {
                undeletedFiles++;
            }
        }
        for (File plugin : workspacePlugins) {
            String path = plugin.getCanonicalPath();
            File classpathfile = new File(path + "/.classpath-gen");
            if (classpathfile.exists() && !classpathfile.delete()) {
                undeletedFiles++;
            }
        }
        File dependencies = new File("testdata_OutputGeneration/dependencies.txt");
        if (dependencies.exists() && !dependencies.delete()) {
            undeletedFiles++;
        }
        dependencies = new File("testdata_dependencies/dependencies.txt");
        if (dependencies.exists() && !dependencies.delete()) {
            undeletedFiles++;
        }
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

    @Test
    public void testBuildFileGeneration() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-generateAllBuild", "testdata_OutputGeneration/workspace", "company/eclipse/plugins",
                "-generateReqFile", "testdata_OutputGeneration/dependencies.txt" };

        assertEquals(0, SecurityMan.runMain(args));

        for (File plugin : workspacePlugins) {
            String path = plugin.getCanonicalPath();
            Path expected = Paths.get(path, "classpathfile_expected");
            Path actual = Paths.get(path, ".classpath.generated");
            List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
            expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(actual, StandardCharsets.UTF_8);
            outputList = TestCLI.addNewlineToAllStrings(outputList);

            assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                    expectedOutputList.toString(), outputList.toString());
        }

        String folder = "testdata_OutputGeneration";
        Path expected = Paths.get(folder, "dependencies_expected.txt");
        File act = new File(folder + "/dependencies.txt");
        List<String> expectedOutputList = Files.readAllLines(expected, StandardCharsets.UTF_8);
        expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(act.toPath(), StandardCharsets.UTF_8);
        outputList = TestCLI.addNewlineToAllStrings(outputList);

        assertEquals("Expected file " + TestCLI.truncate(root, expected) + " does not match actual one",
                expectedOutputList.toString(), outputList.toString());
    }

    @Test
    public void testSingleBuildFileGen() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-generateBuildFile", "com.company.itee.maint.common", "company/eclipse/plugins" };

        assertEquals(0, SecurityMan.runMain(args));

        String pluginFolder = "testdata_OutputGeneration/workspace/com.company.itee.maint.common";
        File expected = new File(pluginFolder + "/classpathfile_expected");
        File actual = new File(pluginFolder + "/.classpath.generated");
        List<String> expectedOutputList = Files.readAllLines(expected.toPath(),
                StandardCharsets.UTF_8);
        expectedOutputList = TestCLI.addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(actual.toPath(),
                StandardCharsets.UTF_8);
        outputList = TestCLI.addNewlineToAllStrings(outputList);

        assertEquals(expectedOutputList.toString(), outputList.toString());
    }
}
