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

import static org.eclipselabs.plugindependencies.core.CommandLineInterpreter.RC_ANALYSIS_ERROR;
import static org.eclipselabs.plugindependencies.core.CommandLineInterpreter.RC_OK;
import static org.eclipselabs.plugindependencies.core.CommandLineInterpreter.RC_RUNTIME_ERROR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * @author obroesam
 *
 */
public class OutputCreator {
    private static final char SEP = File.separatorChar;

    private static File eclipseFolder = new File(".");

    private static String sourceFolder = "./";

    static String targetFolder = "eclipse" + SEP + "plugins";

    public static void setEclipseRoot(String eclipseRoot) throws IOException {
        eclipseFolder = new File(eclipseRoot).getCanonicalFile();
    }

    private static int writeToFile(String fileName, StringBuilder toWrite)
            throws IOException {
        File out = new File(fileName);
        if (out.exists() && !out.delete()) {
            Logging.getLogger().error("can't delete file " + fileName);
            return RC_RUNTIME_ERROR;
        }
        if (!out.createNewFile()) {
            Logging.getLogger().error("can't create File " + fileName);
            return RC_RUNTIME_ERROR;
        }
        try (FileWriter toFileOut = new FileWriter(out, false)) {
            toFileOut.write(toWrite.toString());
        }
        Logging.writeStandardOut("\t" + fileName);
        return RC_OK;
    }

    public static int generateRequirementsfile(String outfile, PlatformState state) throws IOException {
        StringBuilder dependencyBuilder = new StringBuilder();
        List<String> sortedDependencyList;

        for (Plugin plugin : state.getPlugins()) {
            sortedDependencyList = getSortedDependencyList(state, plugin);
            String elementPath = plugin.getPath();
            for (String path : sortedDependencyList) {
                dependencyBuilder.append(elementPath + ":" + path + "\n");
            }
        }

        return writeToFile(outfile, dependencyBuilder);
    }

    private static List<String> getSortedDependencyList(PlatformState state, Plugin plugin) {
        List<Plugin> dependencyList = new ArrayList<>();
        List<String> dependencyPathList = new ArrayList<>();

        dependencyList.addAll(state.computeAllDependenciesRecursive(plugin));

        // build dependency file for given plugin should NOT contain it's fragments (recursive dep!)
        if(plugin.isHost()){
            dependencyList.removeAll(plugin.getFragments());
        }

        for (Plugin dependsOn : dependencyList) {
            dependencyPathList.add(dependsOn.getPath());
        }
        Collections.sort(dependencyPathList);
        return dependencyPathList;
    }

    public static int generateBuildFile(PlatformState state, Plugin plugin) throws IOException {
        Set<Plugin> resolvedPlugins = new LinkedHashSet<>();
        resolvedPlugins.addAll(state.computeCompilationDependencies(plugin));
        return writeClassPathsToFile(plugin, resolvedPlugins);
    }

    public static List<String> getRecursiveClasspaths(Plugin plugin, Set<Plugin> resolvedPlugins) throws IOException {
        List<String> classpaths = new ArrayList<>();
        Map<String, List<String>> cache = new HashMap<>();
        for (Plugin resolvedPlugin : resolvedPlugins) {
            List<String> resolvedPluginClasspaths = getClassPaths(resolvedPlugin, false, cache);
            if (resolvedPluginClasspaths.isEmpty()) {
                throw new IllegalStateException("can't resolve classpath for " + resolvedPlugin);
            }
            classpaths.addAll(resolvedPluginClasspaths);
        }
        List<String> pluginClasspaths = getClassPaths(plugin, true, cache);
        classpaths.addAll(pluginClasspaths);
        return classpaths;
    }

    private static int writeClassPathsToFile(Plugin plugin, Set<Plugin> resolvedPlugins)
            throws IOException {
        StringBuilder classPathList = new StringBuilder();
        Map<String, List<String>> cache = new HashMap<>();
        for (Plugin resolvedPlugin : resolvedPlugins) {
            List<String> classPaths = getClassPaths(resolvedPlugin, false, cache);
            if (classPaths.isEmpty()) {
                resolvedPlugin.addErrorToLog("can't resolve classpath", plugin);
                Logging.getLogger().error("can't resolve classpath for " + resolvedPlugin);
                return RC_ANALYSIS_ERROR;
            }
            for (String classpathEntry : classPaths) {
                classPathList.append(classpathEntry);
                classPathList.append(System.lineSeparator());
            }
        }
        List<String> classPaths = getClassPaths(plugin, true, cache);
        for (String classpathEntry : classPaths) {
            classPathList.append(classpathEntry);
            classPathList.append(System.lineSeparator());
        }
        return writeToFile(plugin.getPath() + SEP + ".classpath.generated", classPathList);
    }

    private static List<String> getClassPaths(Plugin plugin, boolean pluginLocalPaths, Map<String, List<String>> cache) throws IOException {
        List<String> classpaths = new ArrayList<>();
        if (pluginLocalPaths) {
            // append possible libraries from the plugin itself
            List<String> localClasspath = getLocalClasspath(plugin);
            classpaths.addAll(localClasspath);
        } else {
            List<String> pluginClassapths = cache.get(plugin.getName());
            if (pluginClassapths == null) {
                pluginClassapths = new ArrayList<>();
                String elementPath = plugin.getPath();
                if (elementPath.contains(sourceFolder)) {
                    List<String> targetLocations = getTargetLocations(plugin);
                    if (targetLocations.isEmpty()) {
                        throw new IllegalStateException("No target location for plug-in: " + plugin);
                    }
                    pluginClassapths.addAll(targetLocations);
                } else if (elementPath.endsWith(".jar")) {
                    pluginClassapths.add(elementPath);
                } else {
                    List<String> localClasspath = getLocalClasspath(plugin);
                    pluginClassapths.addAll(localClasspath);
                }

                // the "sourceFolder" does not match the one from current plugin?
                if (pluginClassapths.isEmpty() && !elementPath.endsWith(".jar")) {
                    List<String> targetLocations = getTargetLocations(plugin);
                    if (targetLocations.isEmpty()) {
                        throw new IllegalStateException("No target location for plug-in: " + plugin);
                    }
                    pluginClassapths.addAll(targetLocations);
                }
                cache.put(plugin.getName(), pluginClassapths);
                StringBuilder fullPluginClasspath = new StringBuilder();
                for (String classpathEntry : pluginClassapths) {
                    fullPluginClasspath.append(classpathEntry);
                    fullPluginClasspath.append(System.lineSeparator());
                }
                plugin.setFullClassPaths(fullPluginClasspath.toString());
                classpaths.addAll(pluginClassapths);
            } else {
                classpaths.addAll(pluginClassapths);
            }
        }
        return classpaths;
    }

    private static List<String> getLocalClasspath(Plugin plugin) {
        List<String> localClasspath = new ArrayList<>();
        for (String path : plugin.getBundleClassPath()) {
            if (".".equals(path)) {
                // skip plugin itself, if it has no other dependencies
                continue;
            }
            if (isExternalPath(path)) {
                localClasspath.add(path);
            } else {
                String p = plugin.getPath() + SEP + path;
                localClasspath.add(p);
            }
        }
        return localClasspath;
    }

    private static List<String> getTargetLocations(Plugin plugin) throws IOException {
        String versionForDummy = PlatformState.getBundleVersionForDummy();
        String targetDir = plugin.getTargetDirectory();
        if (targetDir == null) {
            return Arrays.asList(plugin.getPath());
        }
        List<String> locations = new ArrayList<>();
        List<String> bundleClassPathList = plugin.getBundleClassPath();

        String pluginTargetFolder;
        if (Paths.get(targetFolder).toFile().exists()) {
            pluginTargetFolder = targetDir + SEP + plugin.getName() + "_" + versionForDummy;
        } else {
            pluginTargetFolder = eclipseFolder.toString() + SEP + targetDir + SEP + plugin.getName() + "_" + versionForDummy;
        }

        if (bundleClassPathList.isEmpty()) {
            locations.add(pluginTargetFolder + ".jar");
        } else {
            for (String path : bundleClassPathList) {
                if (isExternalPath(path)) {
                    locations.add(path);
                } else {
                    if (path.equals(".")) {
                        locations.add(pluginTargetFolder + ".jar");
                    } else {
                        locations.add(pluginTargetFolder + SEP + path);
                    }
                }
            }
        }

        return locations;
    }

    private static boolean isExternalPath(String path) {
        return path.startsWith("/") || path.startsWith("$");
    }

    public static void setSourceFolder(String sourceDir) {
        sourceFolder = sourceDir;
    }

    public static void setTargetFolder(String targetDir) {
        targetFolder = targetDir;
    }
}
