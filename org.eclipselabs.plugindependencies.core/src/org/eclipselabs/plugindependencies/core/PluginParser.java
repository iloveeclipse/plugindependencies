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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author obroesam
 *
 */
public class PluginParser {
    /**
     * Parses all Plugins located in pluginDirectoryPath and adds them to the Plugin set.
     * Parsing is done through reading of Manifest located in each Plugin at
     * .../META-INF/MANIFEST.MF . Exported Packages are added to the package set.
     *
     * @param rootDir
     *            Path to directory where Plugins are located
     * @throws IOException
     *             Reading in file system throws IOException
     */
    public static int createPluginsAndAddToSet(File rootDir, PlatformState state) throws IOException {

        if (!rootDir.exists()) {
            Logging.writeErrorOut("given directory does not exist: " + rootDir);
            return 2;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.writeErrorOut("given directory is not a directory or is not readable: " + rootDir);
            return 3;
        }
        sortFiles(dirArray);

        for (File pluginOrDirectory : dirArray) {
            if (createPluginAndAddToSet(pluginOrDirectory, state) != 0) {
                return -1;
            }
        }
        return 0;
    }

    public static int createPluginAndAddToSet(File pluginOrDirectory, PlatformState state) throws IOException {
        Plugin plugin;
        Manifest manifest = getManifest(pluginOrDirectory);
        if (manifest == null) {
            return 0;
        }
        plugin = parseManifest(manifest);
        if (plugin == null) {
            return 0;
        }
        plugin.setPath(pluginOrDirectory.getCanonicalPath());
        Plugin addedPlugin = state.addPlugin(plugin);
        if (addedPlugin == plugin) {
            return 0;
        }
        Set<Plugin> plugins = state.getPlugins(plugin.getName());
        List<String> equalPluginPaths = new ArrayList<>();
        for (Plugin plug : plugins) {
            if (plug.equals(plugin)) {
                if(plugin.getPath().equals(plug.getPath())){
                    continue;
                }
                equalPluginPaths.add(plug.getPath());
            }
        }
        if(equalPluginPaths.isEmpty()) {
            return 0;
        }
        StringBuilder output = new StringBuilder();
        output.append("two plugins with equal symbolic name and version: ");
        output.append(plugin.getNameAndVersion());

        equalPluginPaths.add(plugin.getPath());
        Collections.sort(equalPluginPaths);
        for (String path : equalPluginPaths) {
            output.append("\n").append(path);
        }
        Logging.writeErrorOut(output.toString());
        return -1;
    }

    public static File[] sortFiles(File[] dirArray) {
        Arrays.sort(dirArray, new Comparator<File>() {

            @Override
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
            }
        });
        return dirArray;
    }

    /**
     * Manifest is parsed to Plugin and returned.
     *
     * @param mf
     *            Manifest is parsed to extract the Plugin data out of it.
     * @return Parsed Plugin
     */
    public static Plugin parseManifest(Manifest mf) {
        if (mf == null) {
            return null;
        }
        String symbolicName = readAttribute(mf, "Bundle-SymbolicName");
        String version = readAttribute(mf, "Bundle-Version");
        if (symbolicName == null || version == null) {
            return null;
        }
        String fragmentHost = readAttribute(mf, "Fragment-Host");
        boolean fragment = fragmentHost != null;
        Plugin extractedPlugin = new Plugin(symbolicName.split(";")[0], version, fragment, symbolicName.contains("singleton:=true"));

        extractedPlugin.setRequiredPlugins(readAttribute(mf, "Require-Bundle"));

        extractedPlugin.setRequiredPackages(readCompleteImport(mf));

        extractedPlugin.setExportedPackages(readAttribute(mf, "Export-Package"));

        extractedPlugin.setBundleClassPath(readAttribute(mf, "Bundle-ClassPath"));

        if (fragment) {
            extractedPlugin.setFragmentHost(fragmentHost);
        }

        return extractedPlugin;
    }

    private static String readCompleteImport(Manifest mf) {
        String imports = readAttribute(mf, "Import-Package");
        String dynImports = readDynamicImport(mf, "DynamicImport-Package");
        String completeImport = null;
        if (imports != null && dynImports != null) {
            completeImport = imports + "," + dynImports;
        }
        if (imports != null && dynImports == null) {
            completeImport = imports;
        }
        if (imports == null && dynImports != null) {
            completeImport = dynImports;
        }
        return completeImport;
    }

    private static String readAttribute(Manifest mf, String name) {
        return mf.getMainAttributes().getValue(name);
    }

    private static String readDynamicImport(Manifest mf, String name) {
        String dynamicImports = readAttribute(mf, name);
        if (dynamicImports != null) {
            dynamicImports = dynamicImports.replaceAll(",", ";dynamicImport,");
            dynamicImports += ";dynamicImport";
        }
        return dynamicImports;
    }

    /**
     * Gets the Manifest of the pluginFolder. pluginFolder could be folder or Jar Archive.
     * In both cases the Manifest is located at pluginFolder/META-INF/MANIFEST.MF .
     *
     * @param pluginOrFolder
     *            Folder or Jar Archive in which the Manifest could be found
     * @return Manifest that is found in the folder or JAR Archive
     * @throws IOException
     *             From reading file system
     */
    public static Manifest getManifest(File pluginOrFolder) throws IOException {
        if (pluginOrFolder.getName().endsWith(".jar")) {
            try (JarFile jarfile = new JarFile(pluginOrFolder)) {
                return jarfile.getManifest();
            }
        }
        Path path = Paths.get(pluginOrFolder.getPath(), "/META-INF/MANIFEST.MF");
        if (path.toFile().exists() && !pluginOrFolder.isHidden()) {
            try (InputStream stream = Files.newInputStream(path)) {
                return new Manifest(stream);
            }
        }
        return null;
    }
}
