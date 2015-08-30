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

import java.util.StringTokenizer;

/**
 * @author obroesam
 *
 */
public class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int micro;
    private final String qualifier;

    public Version(String version) {
        int m = 0;
        String q = "";
        try {
            StringTokenizer st = new StringTokenizer(version, ".");
            this.major = Integer.parseInt(st.nextToken());
            if (st.hasMoreTokens()) {
                this.minor = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) {
                    m = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens()) {
                        q = st.nextToken("");
                    }
                }
            } else {
                this.minor = 0;
            }
            this.micro = m;
            this.qualifier = q;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid version: " + version, e);
        }
    }

    private Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.qualifier = qualifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + micro;
        result = prime * result + minor;
        result = prime * result + qualifier.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        if (major != other.major) {
            return false;
        }
        if (micro != other.micro) {
            return false;
        }
        if (minor != other.minor) {
            return false;
        }
        if (!qualifier.equals(other.qualifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Version other) {
        if (this == other) {
            return 0;
        }

        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        return qualifier.compareTo(other.qualifier);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(major);
        s.append('.').append(minor);
        s.append('.').append(micro);
        if (!qualifier.isEmpty()) {
            s.append('.').append(qualifier);
        }
        return s.toString();
    }

    static String createCompatibleRightBound(String str) {
        Version v = new Version(str);
        return new Version(v.major + 1, 0, 0, "").toString();
    }

    static String createEquivalentRightBound(String str) {
        Version v = new Version(str);
        return new Version(v.major, v.minor + 1, 0, "").toString();
    }


}
