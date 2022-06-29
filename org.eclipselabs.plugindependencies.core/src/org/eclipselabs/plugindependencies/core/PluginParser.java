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
import static org.eclipselabs.plugindependencies.core.PlatformState.fixName;
import static org.eclipselabs.plugindependencies.core.PlatformState.fixVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author obroesam
 *
 */
public class PluginParser {

    private boolean parseEarlyStartup;
    private final PlatformState state;

    public PluginParser(PlatformState state) {
        super();
        this.state = state;
    }

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
    public int createPluginsAndAddToSet(File rootDir) throws IOException {

        if (!rootDir.exists()) {
            Logging.getLogger().error("given directory does not exist: " + rootDir);
            return RC_RUNTIME_ERROR;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.getLogger().error("given directory is not a directory or is not readable: " + rootDir);
            return RC_RUNTIME_ERROR;
        }
        sortFiles(dirArray);

        int result = RC_OK;
        for (File pluginOrDirectory : dirArray) {
            try {
                result = Math.min(result, createPluginAndAddToSet(pluginOrDirectory, false));
            } catch (Throwable t) {
                Logging.getLogger().error("Error while discovering plugins from: " + pluginOrDirectory, t);
            }
        }
        return result;
    }

    public int createPluginAndAddToSet(File pluginOrDirectory, boolean workspace) throws IOException {
        Manifest manifest = getManifest(pluginOrDirectory);
        Plugin plugin;
        String pluginXml = null;
        if (manifest == null) {
            pluginXml = getPluginXml(pluginOrDirectory);
            if(pluginXml == null){
                return RC_OK;
            }
        }
        plugin = parseManifest(manifest, state);
        if (plugin == null) {
            if (manifest == null && pluginXml == null) {
                return RC_OK;
            }
            if(pluginXml == null){
                pluginXml = getPluginXml(pluginOrDirectory);
                if (pluginXml == null) {
                    return RC_OK;
                }
            }
            plugin = parsePluginPromXml(pluginXml);
            if (plugin == null) {
                return RC_OK;
            }
        }
        plugin.setFromWorkspace(workspace);
        plugin.setPath(pluginOrDirectory.getCanonicalPath());
        if(parseEarlyStartup){
            plugin.setEarlyStartup(parseEarlyStartup(pluginOrDirectory));
        }
        Plugin addedPlugin = state.addPlugin(plugin);
        if (addedPlugin == plugin) {
            return RC_OK;
        }
        return RC_ANALYSIS_ERROR;
    }

    private static Plugin parsePluginPromXml(String pluginXml) {
        Document doc;
        try {
            doc = FeatureParser.DB_FACTORY.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(pluginXml.getBytes()));
        } catch (Exception e) {
            Logging.getLogger().error("Failed to parse plugin.xml: " + pluginXml, e);
            return null;
        }

        // <plugin>
        NodeList plugins = doc.getElementsByTagName("plugin");
        if(plugins == null || plugins.getLength() != 1){
            return null;
        }
        Node pluginNode = plugins.item(0);
        if(pluginNode == null){
            return null;
        }
        NamedNodeMap attributes = pluginNode.getAttributes();
        if(attributes == null){
            return null;
        }
        Node idNode = attributes.getNamedItem("id");
        if(idNode == null){
            return null;
        }
        String id = idNode.getTextContent();
        Node versionNode = attributes.getNamedItem("version");
        String version;
        if(versionNode == null){
            version = "";
        } else {
            version = versionNode.getTextContent();
        }
        Plugin plugin = new Plugin(null, id, version, false, true);

        NodeList imports = doc.getElementsByTagName("import");
        for (int i = 0; i < imports.getLength(); i++) {
            Element e = (Element)imports.item(i);
            String plug = e.getAttribute("plugin").trim();
            if(plug.isEmpty()){
                continue;
            }
            String pv = FeatureParser.createVersion(e);
            ManifestEntry imported = new ManifestEntry(fixName(plug), fixVersion(pv));
            plugin.addRequiredPluginEntry(imported);
        }
        return plugin;
    }

    private static boolean parseEarlyStartup(File pluginOrDirectory) {
        // <extension point="org.eclipse.ui.startup">
        try {
            String pluginXml = getPluginXml(pluginOrDirectory);
            if(pluginXml == null){
                return false;
            }
            // TODO use real xml parser
            return pluginXml.contains("\"org.eclipse.ui.startup\"");
        } catch (Exception e) {
            Logging.getLogger().error("Error while parsing plugin.xml from: " + pluginOrDirectory, e);
        }
        return false;
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
    public static Plugin parseManifest(Manifest mf, PlatformState ps) {
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
        Plugin extractedPlugin = new Plugin(mf, StringUtil.firstEntry(symbolicName, ';'), version, fragment, symbolicName.contains("singleton:=true"));

        extractedPlugin.setRequiredPlugins(readAttribute(mf, "Require-Bundle"));

        extractedPlugin.setImportedPackageEntries(readCompleteImport(mf));

        extractedPlugin.setExportedPackages(readAttribute(mf, "Export-Package"), ps);

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

    public static String readAttribute(Manifest mf, String name) {
        return mf.getMainAttributes().getValue(name);
    }

    private static String readDynamicImport(Manifest mf, String name) {
        String dynamicImports = readAttribute(mf, name);
        if (dynamicImports != null) {
            List<String> entries = StringUtil.splitListOfEntries(dynamicImports);
            StringBuilder sb = new StringBuilder();
            for (String entry : entries) {
                sb.append(entry).append(";dynamicImport,");
            }
            dynamicImports = sb.toString();
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
            } catch(RuntimeException e) {
                throw new IOException(e);
            }
        }
        Path path = Paths.get(pluginOrFolder.getPath(), "/META-INF/MANIFEST.MF");
        if (path.toFile().exists() && !pluginOrFolder.isHidden()) {
            try (InputStream stream = Files.newInputStream(path)) {
                return new Manifest(stream);
            } catch(RuntimeException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    /**
     * Gets the plugin.xml of the pluginFolder. pluginFolder could be folder or Jar Archive.
     * In both cases the plugin.xml is located at pluginFolder/plugin.xml .
     *
     * @param pluginOrFolder
     *            Folder or Jar Archive in which the plugin.xml could be found
     * @return plugin.xml that is found in the folder or JAR Archive
     * @throws IOException
     *             From reading file system
     */
    public static String getPluginXml(File pluginOrFolder) throws IOException {
        if (pluginOrFolder.getName().endsWith(".jar")) {
            try (JarFile jarfile = new JarFile(pluginOrFolder)) {
                ZipEntry entry = jarfile.getEntry("plugin.xml");
                if(entry != null) {
                    InputStream is = jarfile.getInputStream(entry);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[16384];
                    int nRead;
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    return new String(buffer.toByteArray());
                }
            } catch(RuntimeException e) {
                throw new IOException(e);
            }
        }
        Path path = Paths.get(pluginOrFolder.getPath(), "plugin.xml");
        if (path.toFile().exists() && !pluginOrFolder.isHidden()) {
            return new String(Files.readAllBytes(path));
        }
        return null;
    }

    /**
     * @return Returns the parseEarlyStartup.
     */
    public boolean isParseEarlyStartup() {
        return parseEarlyStartup;
    }

    /**
     * @param parseEarlyStartup The parseEarlyStartup to set.
     */
    public void setParseEarlyStartup(boolean parseEarlyStartup) {
        this.parseEarlyStartup = parseEarlyStartup;
    }
}
