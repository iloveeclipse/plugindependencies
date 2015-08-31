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
public class TestBuildFileGen extends BaseTest {

    private Path root;
    private File workspace;
    private File[] workspacePlugins;

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
        root = Paths.get("testdata_OutputGeneration");
        workspace = new File(root.toFile(), "/workspace");
        workspacePlugins = PluginParser.sortFiles(workspace.listFiles());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        int undeletedFiles = 0;
        for (File plugin : workspacePlugins) {
            String path = plugin.getCanonicalPath();
            File classpathfile = new File(path + "/.classpath-generated");
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
        super.tearDown();

        if (undeletedFiles != 0) {
            throw new Exception("Error while deleting files: " + undeletedFiles
                    + " not deleted");
        }
    }

    @Test
    public void testBuildAndDepFileGeneration() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-generateAllBuild", "testdata_OutputGeneration/workspace", "company/eclipse/plugins",
                "-generateReqFile", "testdata_OutputGeneration/dependencies.txt"
                };

        assertEquals(0, SecurityMan.runMain(args));

        checkBuildAndDepFileResult();
    }

    @Test
    public void testBuildAndDepFileGeneration2() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-generateReqFile", "testdata_OutputGeneration/dependencies.txt",
                "-generateAllBuild", "testdata_OutputGeneration/workspace", "company/eclipse/plugins"
                };

        assertEquals(0, SecurityMan.runMain(args));

        checkBuildAndDepFileResult();
    }

    private void checkBuildAndDepFileResult() throws IOException {
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
    public void testBuildFileGeneration() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-generateAllBuild", "testdata_OutputGeneration/workspace", "company/eclipse/plugins",
                };

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
    }

    @Test
    public void testBuildFileDependencies() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-deploymentRoot",
                "testdata_OutputGeneration", "-bundleVersion", "99.0.0",
                "-platform", "linux", "gtk", "x86_64",
                "-generateReqFile", "testdata_OutputGeneration/dependencies.txt" };

        assertEquals(0, SecurityMan.runMain(args));

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
