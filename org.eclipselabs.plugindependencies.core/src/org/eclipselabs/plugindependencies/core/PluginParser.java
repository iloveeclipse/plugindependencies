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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
     * @param pluginDirectoryPath
     *            Path to directory where Plugins are located
     * @param plugins
     *            Set to add the parsed Plugins
     * @param packages
     *            Set to add extracted Packages
     * @throws IOException
     *             Reading in file system throws IOException
     */
    public static int readManifests(String pluginDirectoryPath, Set<Plugin> plugins,
            Set<Package> packages) throws IOException {

        if (pluginDirectoryPath == null) {
            Logging.writeErrorOut("Given directory path is null");
            return 1;
        }
        File rootDir = new File(pluginDirectoryPath);
        if (!rootDir.exists()) {
            Logging.writeErrorOut("Given directory does not exist: "
                    + pluginDirectoryPath);
            return 2;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.writeErrorOut("Given directory is not a directory or is not readable: "
                    + pluginDirectoryPath);
            return 3;
        }
        sortFiles(dirArray);

        for (File pluginFile : dirArray) {
            if (createPluginAndAddToSet(pluginFile, plugins, packages) != 0) {
                return -1;
            }
        }
        return 0;
    }

    public static int createPluginAndAddToSet(File pluginFile, Set<Plugin> plugins,
            Set<Package> packages) throws IOException {
        Plugin plugin;
        Manifest manifest = getManifest(pluginFile);
        if (manifest != null) {
            plugin = parseManifest(manifest);
            if (plugin != null) {
                plugin.setPath(pluginFile.getCanonicalPath());
                if (!plugins.add(plugin)) {
                    List<String> equalPluginPaths = new ArrayList<>();
                    for (Plugin plug : plugins) {
                        if (plug.equals(plugin)) {
                            if(plugin.getPath().equals(plug.getPath())){
                                continue;
                            }
                            equalPluginPaths.add(plug.getPath());
                        }
                    }
                    if(!equalPluginPaths.isEmpty()){
                        Logging.writeErrorOut("FATAL Error: Two Plugins with equal Symbolic Name and Version.");
                        Logging.writeErrorOut(plugin.getName() + " " + plugin.getVersion());

                        equalPluginPaths.add(plugin.getPath());
                        Collections.sort(equalPluginPaths);
                        for (String path : equalPluginPaths) {
                            Logging.writeErrorOut(path);
                        }
                        return -1;
                    }
                    return 0;
                }

                addToPackageList(packages, plugin);
            }
        }
        return 0;
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

    private static void addToPackageList(Set<Package> packageList, Plugin plugin) {
        for (Package exportedPackage : plugin.getExportedPackages()) {
            /*
             * Package is exported by another plugin, package has to be found in packages
             * and plugin must be added to exportPlugins of package
             */
            if (!packageList.add(exportedPackage)) {
                Iterator<Package> packSet = packageList.iterator();
                Package doubleExportedPackage = new Package("", "");
                while (!doubleExportedPackage.equals(exportedPackage)) {
                    doubleExportedPackage = packSet.next();
                }
                doubleExportedPackage.addExportPlugin(plugin);
            }
        }
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
        Plugin extractedPlugin = new Plugin(symbolicName.split(";")[0], version);

        extractedPlugin.setRequiredPlugins(readAttribute(mf, "Require-Bundle"));

        extractedPlugin.setRequiredPackages(readCompleteImport(mf));

        extractedPlugin.setExportedPackages(readAttribute(mf, "Export-Package"));

        extractedPlugin.setBundleClassPath(readAttribute(mf, "Bundle-ClassPath"));

        String fragmentHost = readAttribute(mf, "Fragment-Host");
        if (fragmentHost != null) {
            extractedPlugin.setFragment(true);
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
     * @param pluginFolder
     *            Folder or Jar Archive in which the Manifest could be found
     * @return Manifest that is found in the folder or JAR Archive
     * @throws IOException
     *             From reading file system
     */
    public static Manifest getManifest(File pluginFolder) throws IOException {
        Manifest manifest = null;
        if (pluginFolder.isHidden()) {
            return null;
        }
        if (pluginFolder.isDirectory()) {
            Path path = Paths.get(pluginFolder.getCanonicalPath(),
                    "/META-INF/MANIFEST.MF");
            if (path.toFile().exists()) {
                try (BufferedInputStream stream = new BufferedInputStream(
                        Files.newInputStream(path))) {
                    manifest = new Manifest(stream);
                }
            }
        } else {
            if (pluginFolder.getCanonicalPath().endsWith(".jar")) {
                try (JarFile jarfile = new JarFile(pluginFolder)) {
                    manifest = jarfile.getManifest();
                }
            }
        }

        return manifest;
    }
}
