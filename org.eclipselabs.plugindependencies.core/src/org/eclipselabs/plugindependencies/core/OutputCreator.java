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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    private static int writeClassPathsToFile(Plugin plugin, Set<Plugin> resolvedPlugins)
            throws IOException {
        StringBuilder classPathList = new StringBuilder();
        for (Plugin resolvedPlugin : resolvedPlugins) {
            String classPaths = getClassPaths(resolvedPlugin, false);
            if (classPaths == null) {
                resolvedPlugin.addErrorToLog("can't resolve classpath", plugin);
                Logging.getLogger().error("can't resolve classpath for " + resolvedPlugin);
                return RC_ANALYSIS_ERROR;
            }
            classPathList.append(classPaths);
        }
        String classPaths = getClassPaths(plugin, true);
        if (classPaths == null) {
            Logging.getLogger().error("can't resolve classpath for " + plugin);
            return RC_ANALYSIS_ERROR;
        }
        classPathList.append(classPaths);
        return writeToFile(plugin.getPath() + SEP + ".classpath.generated", classPathList);
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

                // the "sourceFolder" does not match the one from current plugin?
                if(ret.length() == 0 && !elementPath.endsWith(".jar")){
                    String targetLocation = getTargetLocation(plugin);
                    if (targetLocation == null) {
                        return null;
                    }
                    ret.append(targetLocation);
                }
                plugin.setFullClassPaths(ret.toString());
            } else {
                ret.append(fullClassPaths);
            }
        }
        return ret.toString();
    }

    private static void appendLocalClasspath(Plugin plugin, StringBuilder ret) {
        for (String path : plugin.getBundleClassPath()) {
            if(".".equals(path)){
                // skip plugin itself, if it has no other dependencies
                continue;
            }
            if(isExternalPath(path)){
                ret.append(path).append("\n");
            } else {
                ret.append(plugin.getPath()).append(SEP).append(path).append("\n");
            }
        }
    }

    private static String getTargetLocation(Plugin plugin) throws IOException {
        String versionForDummy = PlatformState.getBundleVersionForDummy();
        StringBuilder ret = new StringBuilder();
        String targetDir = plugin.getTargetDirectory();
        if (targetDir == null) {
            return plugin.getPath() + "\n";
        }
        List<String> bundleClassPathList = plugin.getBundleClassPath();

        String pluginTargetFolder;
        if(Paths.get(targetFolder).toFile().exists()){
            pluginTargetFolder = targetDir + SEP + plugin.getName() + "_" + versionForDummy;
        } else {
            pluginTargetFolder = eclipseFolder.toString() + SEP + targetDir + SEP + plugin.getName() + "_" + versionForDummy;
        }

        if (bundleClassPathList.isEmpty()) {
            ret.append(pluginTargetFolder + ".jar" + "\n");
        } else {
            for (String path : bundleClassPathList) {
                if(isExternalPath(path)){
                    ret.append(path + "\n");
                } else {
                    if(path.equals(".")){
                        ret.append(pluginTargetFolder).append(".jar").append("\n");
                    } else {
                        ret.append(pluginTargetFolder).append(SEP).append(path).append("\n");
                    }
                }
            }
        }

        return ret.toString();
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
