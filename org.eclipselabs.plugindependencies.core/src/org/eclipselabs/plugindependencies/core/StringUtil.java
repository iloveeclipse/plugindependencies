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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author obroesam
 *
 */
public class StringUtil {
    /**
     * This function gets a string with comma separated plugins/packages and creates a
     * ManifestEntry for each plugin/package.
     *
     * @param stringOfEntries
     *            is a string with comma separated plugin OR package descriptions. Every
     *            plugin/package can have additional attributes, which where splitted by
     *            semicolon.
     * @return list of ManifestEntry. For every plugin/package one entry.
     */
    public static List<ManifestEntry> splitInManifestEntries(String stringOfEntries) {
        List<String> splittedEntries = splitListOfEntries(stringOfEntries);
        List<ManifestEntry> entries = new ArrayList<>();
        for (String entry : splittedEntries) {
            entries.add(new ManifestEntry(split(entry, ';')));
        }
        return entries;
    }

    /**
     * For example: Parameter:
     * <p>
     * org.eclipse.osgi.framework.console;resolution:=optional,
     * org.eclipse.osgi.framework.log
     * ,org.eclipse.osgi.service.datalocation,org.eclipse.osgi.service.debug,
     * org.eclipse.osgi.service.environment;version="1.1"
     *
     * <p>
     * Return: List:
     * <ul>
     * <li>element 0: org.eclipse.osgi.framework.console;resolution:=optional
     * <li>element 1: org.eclipse.osgi.framework.log
     * <li>element 2: org.eclipse.osgi.service.datalocation
     * <li>element 3: org.eclipse.osgi.service.debug
     * <li>element 4: org.eclipse.osgi.service.environment;version="1.1"
     * </ul>
     *
     * @param stringOfEntries
     *            is a string with comma separated plugin OR package descriptions.
     * @return list of plugin/package descriptions(now separated).
     */
    public static List<String> splitListOfEntries(String stringOfEntries) {
        if (stringOfEntries == null) {
            return new ArrayList<String>();
        }
        List<String> listOfEntries = split(stringOfEntries, ',');
        String collectEntry = "";
        List<String> entries = new ArrayList<>();
        for (String entry : listOfEntries) {
            collectEntry += entry;
            if (isEvenAmountOfChar(collectEntry, '"')) {
                entries.add(collectEntry);
                collectEntry = "";
            } else {
                collectEntry += ",";
            }
        }
        return entries;
    }

    private static boolean isEvenAmountOfChar(String string, char toSearch) {
        int counter = 0;
        for (char c : string.toCharArray()) {
            if (c == toSearch) {
                counter++;
            }
        }
        return counter % 2 == 0;
    }

    public static String extractBundleVersionOrRange(String versionString) {
        return extractVersionOrRange(versionString, "bundle-version=");
    }

    public static String extractVersionOrRange(String versionString) {
        return extractVersionOrRange(versionString, "version=");
    }

    private static String extractVersionOrRange(String versionString, String versionPrefix) {
        if (versionString == null) {
            return NamedElement.EMPTY_VERSION;
        }

        int positionOfVersion = versionString.indexOf(versionPrefix);
        if (positionOfVersion == -1) {
            return NamedElement.EMPTY_VERSION;
        }

        int versionStart = positionOfVersion + versionPrefix.length();
        versionString = versionString.substring(versionStart);
        versionStart = 0;

        int versionEnd;
        if (versionString.charAt(0) == '\"') {
            versionEnd = versionString.lastIndexOf('\"');
            versionStart++;
        } else if (versionString.indexOf(')') > 0) {
            // filter:="(&(osgi.ee=JavaSE)(version=17))"
            versionEnd = versionString.indexOf(')');
        } else {
            versionEnd = versionString.length();
        }

        String version = versionString.substring(versionStart, versionEnd);
        version = version.replaceAll("\\s", "");
        return version;
    }

    public static String multiplyString(String toMultiply, int xTimes) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < xTimes; i++) {
            ret.append(toMultiply);
        }
        return ret.toString();
    }

    public static List<String> split(String s, char separator){
        int i = s.indexOf(separator);
        List<String> list = new ArrayList<>();
        if(i < 0){
            list.add(s);
            return list;
        }
        for (; i >= 0 && i < s.length(); ) {
            String sub = s.substring(0, i).trim();
            if(!sub.isEmpty()) {
                list.add(sub);
            }
            s = s.substring(i + 1).trim();
            i = s.indexOf(separator);
            if(i < 0) {
                if (!s.isEmpty()) {
                    list.add(s);
                }
                break;
            }
        }
        return list;
    }

    public static String firstEntry(String s, char separator){
        int i = s.indexOf(separator);
        if(i < 0){
            return s;
        }
        for (; i >= 0 && i < s.length(); ) {
            String sub = s.substring(0, i).trim();
            if(!sub.isEmpty()) {
                return sub;
            }
            s = s.substring(i + 1).trim();
            i = s.indexOf(separator);
            if(i < 0) {
                if (!s.isEmpty()) {
                    return s;
                }
                break;
            }
        }
        return null;
    }

    public static List<String> asList(String ... strings) {
        return new ArrayList<>(Arrays.asList(strings));
    }
}
