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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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

    /**
     * Stack element for resolving plugin dependencies. If {@link #toVisit} is empty, plugin is resolved.
     */
    static class PluginElt {
        static final Plugin DUMMY_PLUGIN = new Plugin("", NamedElement.EMPTY_VERSION);
        static final PluginElt EMPTY = new PluginElt(null,  DUMMY_PLUGIN);
        final Plugin plugin;
        final PluginElt parent;

        List<Plugin> toVisit;

        public PluginElt(PluginElt parent, Plugin plugin) {
            super();
            this.parent = parent;
            this.plugin = plugin;
            toVisit = new LinkedList<>();
            addDirectDependencies();
        }

        void setResolved(){
            resolved(plugin);
            plugin.setResolved();
        }

        void resolved(Plugin p){
            toVisit.remove(p);
            if(p.isFragment() && plugin.equals(p.getFragHost())){
                // ignore own fragment
            } else if(plugin != p){
                plugin.addToRecursiveResolvedPlugins(p);
                Set<Plugin> rr = p.getRecursiveResolvedPlugins();
                for (Plugin child : rr) {
                    plugin.addToRecursiveResolvedPlugins(child);
                }
            }

            if(parent != null && parent != this){
                parent.resolved(p);
            }
        }

        PluginElt next(){
            if(toVisit.isEmpty()){
                return EMPTY;
            }
            return new PluginElt(this, toVisit.remove(0));
        }

        void addToVisit(Plugin p){
            if(p == null || plugin.equals(p)){
                return;
            }
            if(!toVisit.contains(p)) {
                toVisit.add(p);
            }
        }

        void addToVisit(Collection<Plugin> plugins){
            for (Plugin p : plugins) {
                addToVisit(p);
            }
        }

        void addDirectDependencies(){
            for (Plugin pluginToVisit : plugin.getResolvedPlugins()) {
                addToVisit(pluginToVisit);
            }
            if(plugin.isFragment()) {
                addToVisit(plugin.getFragHost());
            } else {
                for (Plugin fragment : plugin.getFragments()) {
                    // fragments can have extra dependencies we want to visit
                    addToVisit(fragment);
                }
            }

            // TODO throw away "duplicated" bundles with different versions, exporting same package
            Set<Plugin> allExporting = new LinkedHashSet<Plugin>();
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
            addToVisit(allExporting);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[plugin=");
            sb.append(plugin);
            sb.append(", toVisit=");
            sb.append(toVisit);
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return plugin.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PluginElt)) {
                return false;
            }
            PluginElt other = (PluginElt) obj;
            if (!plugin.equals(other.plugin)) {
                return false;
            }
            return true;
        }

    }

    public static Set<Plugin> getResolvedPluginsRecursive(final Plugin root) {
        Stack<PluginElt> stack = new Stack<>();
        stack.add(new PluginElt(null, root));
        while (!stack.isEmpty()){
            PluginElt current = stack.peek();
            PluginElt next = current.next();

            if(next == PluginElt.EMPTY) {
                stack.pop();
                // make sure we finished the iteration and replace the default empty set if no dependencies found
                current.setResolved();
                continue;
            }

            if(stack.contains(next)){
                if(!next.plugin.isRecursiveResolved()){
                    Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                    for (Plugin p : resolvedPlugins) {
                        current.resolved(p);
                    }
                }
                // avoid cyclic dependencies
                continue;
            }

            if(root.containsRecursiveResolved(next.plugin)) {
                Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                for (Plugin p : resolvedPlugins) {
                    current.resolved(p);
                }
                current.resolved(next.plugin);
            } else {
                stack.push(next);
            }
        }
        return root.getRecursiveResolvedPlugins();
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
        for (String path : plugin.getBundleClassPath()) {
            if(".".equals(path)){
                // skip plugin itself, if it has no other dependencies
                continue;
            }
            if(isExternalPath(path)){
                ret.append(path).append("\n");
            } else {
                ret.append(plugin.getPath()).append("/").append(path).append("\n");
            }
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
                        ret.append(pluginTargetFolder).append("/").append(path).append("\n");
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
