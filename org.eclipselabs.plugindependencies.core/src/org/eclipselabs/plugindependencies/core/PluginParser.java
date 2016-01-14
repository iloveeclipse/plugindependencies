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
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
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
            return 2;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.getLogger().error("given directory is not a directory or is not readable: " + rootDir);
            return 3;
        }
        sortFiles(dirArray);

        int result = 0;
        for (File pluginOrDirectory : dirArray) {
            if (createPluginAndAddToSet(pluginOrDirectory) != 0) {
                result = -1;
            }
        }
        return result;
    }

    public int createPluginAndAddToSet(File pluginOrDirectory) throws IOException {
        Manifest manifest = getManifest(pluginOrDirectory);
        Plugin plugin;
        String pluginXml = null;
        if (manifest == null) {
            pluginXml = getPluginXml(pluginOrDirectory);
            if(pluginXml == null){
                return 0;
            }
        }
        plugin = parseManifest(manifest);
        if (plugin == null) {
            if (manifest == null && pluginXml == null) {
                return 0;
            }
            if(pluginXml == null){
                pluginXml = getPluginXml(pluginOrDirectory);
            }
            plugin = parsePluginPromXml(pluginXml);
            if (plugin == null) {
                return 0;
            }
        }
        plugin.setPath(pluginOrDirectory.getCanonicalPath());
        if(parseEarlyStartup){
            plugin.setEarlyStartup(parseEarlyStartup(pluginOrDirectory));
        }
        Plugin addedPlugin = state.addPlugin(plugin);
        if (addedPlugin == plugin) {
            return 0;
        }
        return -1;
    }

    private static Plugin parsePluginPromXml(String pluginXml) {
        Document doc;
        try {
            doc = FeatureParser.DB_FACTORY.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(pluginXml.getBytes()));
        } catch (SAXException | IOException | ParserConfigurationException e) {
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

    private static boolean parseEarlyStartup(File pluginOrDirectory) throws IOException {
        // <extension point="org.eclipse.ui.startup">
        String pluginXml = getPluginXml(pluginOrDirectory);
        if(pluginXml == null){
            return false;
        }
        // TODO use real xml parser
        return pluginXml.contains("\"org.eclipse.ui.startup\"");
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
        Plugin extractedPlugin = new Plugin(mf, StringUtil.firstEntry(symbolicName, ';'), version, fragment, symbolicName.contains("singleton:=true"));

        extractedPlugin.setRequiredPlugins(readAttribute(mf, "Require-Bundle"));

        extractedPlugin.setImportedPackageEntries(readCompleteImport(mf));

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

    public static String readAttribute(Manifest mf, String name) {
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
