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

import static org.eclipselabs.plugindependencies.core.MainClass.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author obroesam
 *
 */
public class OutputCreator {
    private static File eclipseFolder = new File(".");

    private static String bundleVersion = "0.0.0";

    private static String sourceFolder = "./";

    static String targetFolder = "eclipse/plugins";

    public static void setEclipseRoot(String eclipseRoot) throws IOException {
        eclipseFolder = new File(eclipseRoot).getCanonicalFile();
    }

    public static void setBundleVersion(String bundleVersion) {
        OutputCreator.bundleVersion = bundleVersion;
    }

    private static int writeToFile(String fileName, StringBuilder toWrite)
            throws IOException {
        File out = new File(fileName);
        if (out.exists() && !out.delete()) {
            Logging.writeErrorOut("Error: can not delete file " + fileName);
            return -1;
        }
        if (!out.createNewFile()) {
            Logging.writeErrorOut("Error: can not create File " + fileName);
            return -1;
        }
        try (FileWriter toFileOut = new FileWriter(out, false)) {
            toFileOut.write(toWrite.toString());
        }
        Logging.writeStandardOut("\t" + fileName);
        return 0;
    }

    public static int generateRequirementsfile(String outfile) throws IOException {
        StringBuilder dependencyBuilder = new StringBuilder();
        List<String> sortedDependencyList;

        for (Plugin plugin : pluginSet) {
            sortedDependencyList = getSortedDependencyList(plugin);
            String elementPath = plugin.getPath();
            for (String path : sortedDependencyList) {
                dependencyBuilder.append(elementPath + ":" + path + "\n");
            }
        }

        return writeToFile(outfile, dependencyBuilder);
    }

    private static List<String> getSortedDependencyList(Plugin plugin) {
        List<Plugin> dependencyList = new ArrayList<>();
        List<String> dependencyPathList = new ArrayList<>();

        dependencyList.addAll(getResolvedPluginsRecursive(plugin));

        for (Plugin dependsOn : dependencyList) {
            dependencyPathList.add(dependsOn.getPath());
        }
        Collections.sort(dependencyPathList);
        return dependencyPathList;
    }

    public static Set<Plugin> getResolvedPluginsRecursive(Plugin plugin) {
        return computeResolvedPlugins(plugin);
    }

    private static Set<Plugin> computeResolvedPlugins(Plugin plugin) {
        if (plugin.getRecursiveResolvedPlugins() != Collections.EMPTY_SET) {
            return plugin.getRecursiveResolvedPlugins();
        }
        for (Plugin pluginToVisit : plugin.getResolvedPlugins()) {
            if(plugin.equals(pluginToVisit)){
                continue;
            }
            plugin.addToRecursiveResolvedPlugins(pluginToVisit);
            Set<Plugin> resolved = computeResolvedPlugins(pluginToVisit);
            for (Plugin pl : resolved) {
                plugin.addToRecursiveResolvedPlugins(pl);
            }
        }
        if(plugin.isFragment() && plugin.getFragHost() != null){
            // fragment inherits all dependencies from host
            for (Plugin pluginToVisit : plugin.getFragHost().getResolvedPlugins()) {
                if(plugin.equals(pluginToVisit)){
                    continue;
                }
                plugin.addToRecursiveResolvedPlugins(pluginToVisit);
                Set<Plugin> resolved = computeResolvedPlugins(pluginToVisit);
                for (Plugin pl : resolved) {
                    plugin.addToRecursiveResolvedPlugins(pl);
                }
            }
        }

        Set<Plugin> result = computeResolvedFragments(plugin);
        for (Plugin pl : result) {
            plugin.addToRecursiveResolvedPlugins(pl);
        }
        result = getPluginsForImportedPackages(plugin);
        for (Plugin pl : result) {
            plugin.addToRecursiveResolvedPlugins(pl);
        }

        // make sure we finished the iteration and replace the default empty set if no dependencies found
        plugin.addToRecursiveResolvedPlugins(plugin);
        return plugin.getRecursiveResolvedPlugins();
    }

    private static Set<Plugin> computeResolvedFragments(Plugin plugin) {
        if(plugin.isFragment()){
            return Collections.emptySet();
        }
        Set<Plugin> result = new LinkedHashSet<Plugin>();
        for (Plugin fragment : plugin.getFragments()) {
            if(plugin.equals(fragment) || result.contains(fragment)){
                continue;
            }
            // ??? fragments of a plugin seems to be always added by PDE
            result.add(fragment);
            Set<Plugin> rpSet = computeResolvedPlugins(fragment);
            result.addAll(rpSet);
        }
        result.remove(plugin);
        return result;
    }

    /**
     * Return the given set with additional plugins which were required via
     * "Import-Package" directives
     */
    private static Set<Plugin> getPluginsForImportedPackages(Plugin plugin) {
        // TODO throw away "duplicated" bundles with different versions, exporting same package
        Set<Plugin> allExporting = new LinkedHashSet<Plugin>(); // new TreeSet<>(new OSGIElement.NameComparator());
        for (Package imported : plugin.getImportedPackages()) {
            Set<Plugin> exportedBy = imported.getExportedBy();
            if(!exportedBy.isEmpty()) {
                allExporting.addAll(exportedBy);
            } else {
                Set<Plugin> reexportedBy = imported.getReexportedBy();
                allExporting.addAll(reexportedBy);
            }
        }
        // fragment inherits all dependencies from host
        if(plugin.isFragment() && plugin.getFragHost() != null){
            for (Package imported : plugin.getFragHost().getImportedPackages()) {
                Set<Plugin> exportedBy = imported.getExportedBy();
                if(!exportedBy.isEmpty()) {
                    allExporting.addAll(exportedBy);
                } else {
                    Set<Plugin> reexportedBy = imported.getReexportedBy();
                    allExporting.addAll(reexportedBy);
                }
            }
        }

        allExporting.remove(plugin);
        Set<Plugin> result = new LinkedHashSet<Plugin>();
        for (Plugin pl : allExporting) {
            result.add(pl);
            Set<Plugin> rpSet = computeResolvedPlugins(pl);
            result.addAll(rpSet);
        }
        result.remove(plugin);
        return result;
    }

    public static int generateBuildFile(Plugin plugin) throws IOException {
        Set<Plugin> resolvedPlugins = new LinkedHashSet<>();
        resolvedPlugins.addAll(getResolvedPluginsRecursive(plugin));
        return writeClassPathsToFile(plugin, resolvedPlugins);
    }

    private static int writeClassPathsToFile(Plugin plugin, Set<Plugin> resolvedPlugins)
            throws IOException {
        StringBuilder classPathList = new StringBuilder();
        for (Plugin resolvedPlugin : resolvedPlugins) {
            String classPaths = getClassPaths(resolvedPlugin, false);
            if (classPaths == null) {
                Logging.writeErrorOut("Error: can not resolve classpath for " + plugin);
                return -1;
            }
            classPathList.append(classPaths);
        }
        String classPaths = getClassPaths(plugin, true);
        if (classPaths == null) {
            Logging.writeErrorOut("Error: can not resolve classpath for " + plugin);
            return -1;
        }
        classPathList.append(classPaths);
        return writeToFile(plugin.getPath() + "/.classpath.generated", classPathList);
    }

    private static String getClassPaths(Plugin plugin, boolean pluginLocalPaths) throws IOException {
        StringBuilder ret = new StringBuilder();
        if(pluginLocalPaths){
            // append possible libraries from the plugin itself
            appendLocalClasspath(plugin, ret);
        } else {
            String fullClassPaths = plugin.getFullClassPaths();
            if (fullClassPaths == null) {
                String elementPath = plugin.getPath();
                if (elementPath.contains(sourceFolder)) {
                    String targetLocation = getTargetLocation(plugin);
                    if (targetLocation == null) {
                        return null;
                    }
                    ret.append(targetLocation);
                } else if (elementPath.endsWith(".jar")) {
                    ret.append(elementPath + "\n");
                } else {
                    appendLocalClasspath(plugin, ret);
                }
                plugin.setFullClassPaths(ret.toString());
            } else {
                ret.append(fullClassPaths);
            }
        }
        return ret.toString();
    }

    private static void appendLocalClasspath(Plugin plugin, StringBuilder ret) {
        for (String classpath : plugin.getBundleClassPath()) {
            if(".".equals(classpath)){
                // skip plugin itself, if it has no other dependencies
                continue;
            }
            ret.append(plugin.getPath() + "/" + classpath + "\n");
        }
    }

    private static String getTargetLocation(Plugin plugin) throws IOException {
        StringBuilder ret = new StringBuilder();
        String targetDir = plugin.getTargetDirectory();
        if (targetDir == null) {
            return null;
        }
        List<String> bundleClassPathList = plugin.getBundleClassPath();

        String pluginTargetFolder;
        if(Paths.get(targetFolder).toFile().exists()){
            pluginTargetFolder = targetDir + "/" + plugin.getName() + "_" + bundleVersion;
        } else {
            pluginTargetFolder = eclipseFolder + "/" + targetDir + "/" + plugin.getName() + "_" + bundleVersion;
        }

        if (bundleClassPathList.isEmpty() || bundleClassPathList.get(0).equals(".")) {
            ret.append(pluginTargetFolder + ".jar" + "\n");
        } else {
            for (String path : bundleClassPathList) {
                if (!path.contains("external:")) {
                    ret.append(pluginTargetFolder + "/" + path + "\n");
                } else {
                    ret.append(resolveExternalPath(path) + "\n");
                }
            }
        }

        return ret.toString();
    }

    private static String resolveExternalPath(String path) {
        int pathbegin = path.indexOf("external:") + "external:".length();
        String extractedPath = path.substring(pathbegin);

        if (extractedPath.contains("$")) {
            int firstDollar = extractedPath.indexOf('$');
            int secondDollar = extractedPath.lastIndexOf('$');
            String envVariable = extractedPath.substring(firstDollar + 1, secondDollar);
            String value = System.getenv(envVariable);
            if(value == null){
                value = "$" + envVariable + "$";
            }
            extractedPath = value + extractedPath.substring(secondDollar + 1);
        }
        return extractedPath;
    }

    public static void setSourceFolder(String sourceDir) {
        sourceFolder = sourceDir;
    }

    public static void setTargetFolder(String targetDir) {
        targetFolder = targetDir;
    }
}
