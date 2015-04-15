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

import static org.eclipselabs.plugindependencies.core.StringUtil.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipselabs.plugindependencies.core.fixture.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestStringUtil extends BaseTest {
    String testString1;

    String testString2;

    String testString3;

    String testString123;

    String testString4;

    String testString5;

    String testString6;

    String testString7;

    String testString8;

    String testString9;

    String testString10;

    List<String> resultList1;

    List<String> resultList2;

    List<String> resultList3;

    List<ManifestEntry> resultList;

    @Override
    @Before
    public void setup() throws Exception {
        testString1 = "bundle-version=\"[3.2.0,4.0.0)\"";
        testString2 = "bundle-version=\"1.1.0\"";
        testString3 = "bundle-version=\"(3.2.0,4.0.0]\"";
        testString123 = "org.eclipse.ui;bundle-version=\"[3.2.0,4.0.0)\",org.hamcrest.core;bundle-version=\"1.1.0\";visibility:=reexport,org.eclipse.core.resources;bundle-version=\"(3.2.0,4.0.0]\"";
        testString4 = "This text is not important, its just a text without any vers-ion and some comma \"abc,abc\" in quotation mark";
        testString5 = "bundle-version=4.11.0";
        testString6 = "bundle-version=\"99.0.0\"";
        testString7 = "version=3.3.1";
        testString8 = "version=\"[2.4, 3.0)\"";
        testString9 = "bundle-version=\"[3.2.0,4.                0.0)\"";
        testString10 = "bundle-version=\"99              .0.0\"";
        resultList1 = new ArrayList<>();
        resultList1.add("org.eclipse.ui;bundle-version=\"[3.2.0,4.0.0)\"");
        resultList1
                .add("org.hamcrest.core;bundle-version=\"1.1.0\";visibility:=reexport");
        resultList1.add("org.eclipse.core.resources;bundle-version=\"(3.2.0,4.0.0]\"");

        resultList2 = new ArrayList<>();
        resultList2.add(testString1);
        resultList2.add(testString3);
        resultList2.add(testString2);
        resultList2.add(testString3);
        resultList2.add(testString1);

        resultList3 = new ArrayList<>();
        resultList3.add("This text is not important");
        resultList3.add("its just a text without any vers-ion and some comma \"abc,abc\" in quotation mark");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        resultList1 = null;
        resultList2 = null;
        super.tearDown();
    }

    @Test
    public void testGetVersionOfString() {
        assertEquals("", extractVersionOrRange(null));
        assertEquals("", extractVersionOrRange(""));
        assertEquals("[3.2.0,4.0.0)", extractVersionOrRange(testString1));
        assertEquals("1.1.0", extractVersionOrRange(testString2));
        assertEquals("(3.2.0,4.0.0]", extractVersionOrRange(testString3));
        assertEquals("", extractVersionOrRange(testString4));
        assertEquals("4.11.0", extractVersionOrRange(testString5));
        assertEquals("99.0.0", extractVersionOrRange(testString6));
        assertEquals("3.3.1", extractVersionOrRange(testString7));
        assertEquals("[2.4,3.0)", extractVersionOrRange(testString8));
        assertEquals("[3.2.0,4.0.0)", extractVersionOrRange(testString9));
        assertEquals("99.0.0", extractVersionOrRange(testString10));
        assertEquals("[1.6,1.7)", extractVersionOrRange("org.apache.felix.scr; version=\"[1.6,1.7)\""));
    }

    @Test
    public void testSplitString() {
        assertEquals(resultList1, splitListOfEntries(testString123));
        List<String> resultList5 = new ArrayList<>();
        resultList5.add("");
        assertEquals(resultList5, splitListOfEntries(""));
        assertEquals(new ArrayList<>(), splitListOfEntries(null));

        String test = testString1 + "," + testString3 + "," + testString2 + ","
                + testString3 + "," + testString1;
        assertEquals(resultList2.toString(), splitListOfEntries(test).toString());
        assertEquals(resultList3.toString(), splitListOfEntries(testString4).toString());
    }

    @Test
    public void testSplitString2() {
        List<String> result = split("", ',');
        assertEquals("[]", result.toString());

        result = split(" ", ',');
        assertEquals("[ ]", result.toString());

        result = split(" ", ' ');
        assertEquals("[]", result.toString());

        result = split(",", ',');
        assertEquals("[]", result.toString());

        result = split(",,,,,", ',');
        assertEquals("[]", result.toString());

        result = split(" , ,", ',');
        assertEquals("[]", result.toString());

        result = split(" , ,", ',');
        assertEquals("[]", result.toString());

        result = split("a,b", ';');
        assertEquals("[a,b]", result.toString());

        result = split("a;b", ';');
        assertEquals("[a, b]", result.toString());

        result = split(";a;b;", ';');
        assertEquals("[a, b]", result.toString());

        result = split("; ; a ; b ; ;", ';');
        assertEquals("[a, b]", result.toString());

        result = split("; ; ab ; cd ; ; ", ';');
        assertEquals("[ab, cd]", result.toString());

        result = split("ab;cd", ';');
        assertEquals("[ab, cd]", result.toString());
    }

    @Test
    public void testSplitIntoAttributes() {
        resultList = new ArrayList<>();
        resultList.add(new ManifestEntry( asList("org.eclipse.ui",
                "bundle-version=\"[3.2.0,4.0.0)\"")));
        resultList.add(new ManifestEntry(asList("org.hamcrest.core",
                "bundle-version=\"1.1.0\"", "visibility:=reexport")));
        resultList.add(new ManifestEntry(asList("org.eclipse.core.resources",
                "bundle-version=\"(3.2.0,4.0.0]\"" )));
        assertEquals(resultList, splitInManifestEntries(testString123));
        resultList.clear();
        resultList.add(new ManifestEntry( asList("")));
        assertEquals(resultList, splitInManifestEntries(""));
        resultList.clear();
        assertEquals(resultList, splitInManifestEntries(null));
    }

    private static List<String> asList(String ... strings) {
        return new ArrayList<>(Arrays.asList(strings));
    }

    @Test
    public void testStringMultiply() {
        assertEquals("", multiplyString("", 0));
        assertEquals("", multiplyString("abc", 0));
        assertEquals("abc", multiplyString("abc", 1));
        assertEquals("abcabcabc", multiplyString("abc", 3));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                multiplyString("a", 400));
    }
}
