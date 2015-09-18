/*******************************************************************************
 * Copyright (c) 2015 example. All rights reserved.
 *
 * Contributors:
 *     example - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author aloskuto
 *
 */
public class TestVersions {

    @Test
    public void test() {
        ManifestEntry m1 = new ManifestEntry("a", "");
        ManifestEntry m2 = new ManifestEntry("a", "0.0.0");
        ManifestEntry m3 = new ManifestEntry("a", "[0.0.0,1.0.0)");
        ManifestEntry m4 = new ManifestEntry("a", "(0.0.0,1.0.0]");
        ManifestEntry b = new ManifestEntry("b", "");

        assertEquals(m1, m2);
        assertEquals(m2, m1);
        assertEquals(m1, m1);
        assertEquals(m2, m2);
        assertEquals(m3, m3);
        assertNotEquals(m1, m3);
        assertNotEquals(m3, m1);
        assertNotEquals(m2, m3);
        assertNotEquals(m3, m2);
        assertNotEquals(m3, m4);
        assertNotEquals(m1, b);
        assertNotEquals(m1, new Object());

        assertTrue(m1.matches("a", ""));
        assertTrue(m2.matches("a", ""));
        assertTrue(m3.matches("a", ""));
        assertFalse(m1.matches("b", ""));

        assertFalse(m1.matches("a", "[0.0.0,1.0.0)"));
        assertFalse(m2.matches("a", "[0.0.0,1.0.0)"));
        assertTrue(m3.matches("a", "[0.0.0,1.0.0)"));
        assertFalse(m1.matches("b", "[0.0.0,1.0.0)"));

        assertTrue(m1.matches("a", "0.0.0"));
        assertTrue(m2.matches("a", "0.0.0"));
        assertTrue(m3.matches("a", "0.0.0"));

        assertTrue(m1.exactMatch(m2));
        assertTrue(m1.exactMatch(m1));
        assertTrue(m3.exactMatch(m3));

        assertFalse(m1.exactMatch(m3));
        assertFalse(m2.exactMatch(m3));
        assertFalse(m1.exactMatch(b));
        assertFalse(m3.exactMatch(m4));
    }

}
