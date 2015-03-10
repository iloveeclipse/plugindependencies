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

import org.eclipselabs.plugindependencies.core.DependencyResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDepResIsRightVersion {
    static String unimportant;

    static String version1;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        unimportant = "The content of this string is not important, but not correct version";
        version1 = "1.2.3";
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        unimportant = null;
        version1 = null;
    }

    @Test
    public void testWrongParameters() {
        assertFalse(DependencyResolver.isCompatibleVersion(null, null));
        assertFalse(DependencyResolver.isCompatibleVersion(null, unimportant));
        assertFalse(DependencyResolver.isCompatibleVersion(unimportant, null));
        assertFalse(DependencyResolver.isCompatibleVersion(unimportant, unimportant));
        assertFalse(DependencyResolver.isCompatibleVersion(unimportant, version1));
        assertFalse(DependencyResolver.isCompatibleVersion(version1, unimportant));
    }

    @Test
    public void testStringNoContent() {
        assertTrue(DependencyResolver.isCompatibleVersion("", ""));
        assertTrue(DependencyResolver.isCompatibleVersion("", version1));
        assertFalse(DependencyResolver.isCompatibleVersion(version1, ""));
        assertFalse(DependencyResolver.isCompatibleVersion("", unimportant));
        assertFalse(DependencyResolver.isCompatibleVersion(unimportant, ""));
    }

    @Test
    public void testVersions() {
        assertTrue(DependencyResolver.isCompatibleVersion("1.0.0", version1));
        assertTrue(DependencyResolver.isCompatibleVersion("1.0", version1));
        assertTrue(DependencyResolver.isCompatibleVersion("1", version1));

        assertFalse(DependencyResolver.isCompatibleVersion("2", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("2.0", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("2.0.0", version1));

        assertTrue(DependencyResolver.isCompatibleVersion("[1.2.3,2)", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("(1.2.3,2)", version1));

        assertTrue(DependencyResolver.isCompatibleVersion("[1.0.0,1.2.3]", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("[1.0.0,1.2.3)", version1));

        assertTrue(DependencyResolver.isCompatibleVersion("(1.0.0,1.2.3]", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("(1.0.0,1.2.3)", version1));

        assertTrue(DependencyResolver.isCompatibleVersion("(1.2.2,2)", version1));

        assertTrue(DependencyResolver.isCompatibleVersion("[1.0.0,1.2.4)", version1));

        assertFalse(DependencyResolver.isCompatibleVersion("[1.3,1.5]", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("(1.3,1.5]", version1));

        assertFalse(DependencyResolver.isCompatibleVersion("(1.0.0,1.2]", version1));
        assertFalse(DependencyResolver.isCompatibleVersion("(1.0.0,1.2)", version1));
    }

}
