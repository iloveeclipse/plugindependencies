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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author obroesam
 *
 */
public class TestCLI {
    static PrintStream console;

    File tempDir;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        console = System.out;
    }

    @Before
    public void setUp() throws Exception {
        Path tempDirPath = Files.createTempDirectory("testCLI");
        tempDir = tempDirPath.toFile();
        System.setSecurityManager(new SecurityMan());
    }

    @After
    public void tearDown() throws Exception {
        if (!removeDirectory(tempDir)) {
            throw new Exception("Can not remove Directory: " + tempDir.getCanonicalPath());
        }
        System.setSecurityManager(null);
        System.setOut(console);
    }

    @Test
    public void testUnresolvedOutput() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-unresPlugin", "w",
                "-unresPlugin", "-unresFeature", "w", "-unresFeature" };

        File expectedOutput = new File("outputs/console_unres_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_unres");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(0, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testHelp() throws IOException {
        String args[] = new String[] { "-h" };

        File expectedOutput = new File("outputs/console_help_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_help");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(0, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testDependencies() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-providing",
                "com.company.itee.maint.common.model", "-providing",
                "org.eclipse.osgi.event", "-dependOnPlugin", "org.eclipse.equinox.app",
                "-dependOnPlugin", "org.eclipse.ui", "-dependOnPlugin",
                "com.company.itee.svc.unoaccess", "-dependOnPackage",
                "org.eclipse.swt.widgets", "-dependOnPackage", "javax.xml.parsers",
                "-dependOnPackage", "org.osgi.framework" };

        File expectedOutput = new File("outputs/console_dep_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_dep");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        PrintStream out = new PrintStream(outputFile);
        Logging.setLogger(new Logging.SimpleLogger(out));

        assertEquals(0, SecurityMan.runMain(args));

        List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                StandardCharsets.UTF_8);
        expectedOutputList = addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(outputFile.toPath(),
                StandardCharsets.UTF_8);
        outputList = addNewlineToAllStrings(outputList);

        assertEquals(expectedOutputList.toString(), outputList.toString());
    }

    @Test
    public void testWrongOption() throws IOException {
        String args[] = new String[] { "abc", "-NotValidOption" };

        File expectedOutput = new File("outputs/console_help_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_wrongOption");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(0, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(), StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);
            expectedOutputList.add(0, "\n");
            expectedOutputList.add(0, "Unknown option\n");

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testFullLog() throws IOException {
        File fullLog = new File(tempDir.getCanonicalPath() + "/fullLog");

        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-fullLog",
                fullLog.getCanonicalPath() };

        assertEquals(0, SecurityMan.runMain(args));

        File expectedOutput = new File("outputs/fullLog_expected");

        List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                StandardCharsets.UTF_8);
        expectedOutputList = addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(fullLog.toPath(),
                StandardCharsets.UTF_8);
        outputList = addNewlineToAllStrings(outputList);

        assertEquals(expectedOutputList.toString(), outputList.toString());
    }

    @Test
    public void testReadEqualFeatures() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_equalFeatures" };

        File expectedOutput = new File("outputs/console_equalFeatures_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_equalFeatures");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(-1, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testReadEqualPlugins() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_equalPlugins" };

        File expectedOutput = new File("outputs/console_equalPlugins_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_equalPlugins");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(-1, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testReadEqualPlugins2() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_equalPlugins2" };

        File expectedOutput = new File("outputs/console_equalPlugins2_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_equalPlugins2");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(-1, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testFocusOption() throws IOException {
        String args[] = new String[] { "-eclipsePaths",
                "testdata_OutputGeneration/eclipseRE",
                "testdata_OutputGeneration/packages/generated/TESTS_ONLY/eclipse",
                "testdata_OutputGeneration/workspace", "-focus",
                "org.eclipse.core.runtime,3.7.0", "org.eclipse.cdt",
                "org.eclipse.swt.gtk.linux.x86_64", "org.eclipse.swt",
        "org.eclipse.cdt.gdb" };

        File expectedOutput = new File("outputs/console_focusOption_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_focusOption");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        try(PrintStream out = new PrintStream(outputFile);){
            Logging.setLogger(new Logging.SimpleLogger(out));

            assertEquals(0, SecurityMan.runMain(args));

            List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                    StandardCharsets.UTF_8);
            expectedOutputList = addNewlineToAllStrings(expectedOutputList);

            List<String> outputList = Files.readAllLines(outputFile.toPath(),
                    StandardCharsets.UTF_8);
            outputList = addNewlineToAllStrings(outputList);

            assertEquals(expectedOutputList.toString(), outputList.toString());
        }
    }

    @Test
    public void testFocusOption2() throws IOException {
        String args[] = new String[] { "-eclipsePaths", "testdata_dependencies/eclipse",
                "-focus", "org.company.test.framework", "org.company.right",
                "org.company.corePlugin", "org.company.workcenter"};

        File expectedOutput = new File("outputs/console_focusOption2_expected");
        File outputFile = new File(tempDir.getCanonicalPath() + "/console_focusOption2");
        if (!outputFile.createNewFile()) {
            fail("Output-file can not be created in " + tempDir.getCanonicalPath());
        }
        PrintStream out = new PrintStream(outputFile);
        Logging.setLogger(new Logging.SimpleLogger(out));

        assertEquals(0, SecurityMan.runMain(args));

        List<String> expectedOutputList = Files.readAllLines(expectedOutput.toPath(),
                StandardCharsets.UTF_8);
        expectedOutputList = addNewlineToAllStrings(expectedOutputList);

        List<String> outputList = Files.readAllLines(outputFile.toPath(),
                StandardCharsets.UTF_8);
        outputList = addNewlineToAllStrings(outputList);

        assertEquals(expectedOutputList.toString(), outputList.toString());
    }

    public boolean removeDirectory(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!removeDirectory(file)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static List<String> addNewlineToAllStrings(List<String> originalList)
            throws IOException {
        File testFolder = new File("");
        Path testFolderPath = testFolder.getCanonicalFile().toPath();

        List<String> newLineList = new ArrayList<>();
        for (String string : originalList) {
            string = truncate(testFolderPath, string);
            newLineList.add(string);
        }
        return newLineList;
    }

    public static String truncate(Path testFolderPath, Path pathStr) throws IOException {
        return truncate(testFolderPath, pathStr.toString());
    }
    public static String truncate(Path testFolderPath, String pathStr) throws IOException {
//        if (pathStr.contains("$SOME_PATH$/")) {
//            pathStr = pathStr.replace("$SOME_PATH$/", "");
//        }

        if (pathStr.contains("org.eclipselabs.plugindependencies.core.test")) {
            int pathBegin = pathStr.indexOf('/');
            if (pathBegin != -1) {
                Path path = Paths.get(pathStr.substring(pathBegin));
                Path relativePath;
                relativePath = testFolderPath.toRealPath().relativize(path);
                pathStr = pathStr.substring(0, pathBegin) + relativePath;
            }
            if (pathStr.contains("org.eclipselabs.plugindependencies.core.test")) {
                pathBegin = pathStr.indexOf(":/") + ":".length();
                if (pathBegin != -1) {
                    Path path = Paths.get(pathStr.substring(pathBegin));
                    Path relativePath = testFolderPath.toRealPath().relativize(path);
                    pathStr = pathStr.substring(0, pathBegin) + relativePath;
                }
            }
        }
        pathStr = pathStr + "\n";
        return pathStr;
    }
}
