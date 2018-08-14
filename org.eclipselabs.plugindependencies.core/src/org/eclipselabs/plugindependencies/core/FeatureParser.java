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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class FeatureParser {
    public static final DocumentBuilderFactory DB_FACTORY = DocumentBuilderFactory.newInstance();

    /**
     * Parses all Features located in featureDirectoryPath and adds them to the features
     * Set. Features are parsed through the feature.xml file which is located in each
     * feature Folder.
     *
     * @param rootDir
     *            Path to Folder where Features are located
     * @throws IOException
     *             From reading file system
     * @throws SAXException
     *             if any parse Errors occur
     * @throws ParserConfigurationException
     *             if a DocumentBuilder cannot be created which satisfies the
     *             configuration requested.
     */
    public static int createFeaturesAndAddToSet(File rootDir, PlatformState state)
            throws IOException, SAXException, ParserConfigurationException {
        if (!rootDir.isDirectory()) {
            Logging.getLogger().error("given directory does not exist: " + rootDir);
            return RC_RUNTIME_ERROR;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.getLogger().error("given directory is not a directory or is not readable: " + rootDir);
            return RC_RUNTIME_ERROR;
        }
        PluginParser.sortFiles(dirArray);
        int result = RC_OK;
        for (File featureFolder : dirArray) {
            result = Math.min(result, createFeatureAndAddToSet(featureFolder, false, state));
        }
        return result;
    }

    public static int createFeatureAndAddToSet(File featureFolder, boolean workspace, PlatformState state)
            throws IOException, SAXException, ParserConfigurationException {
        File featureXMLFile = new File(featureFolder, "feature.xml").getCanonicalFile();
        Feature feature = null;
        if (!featureXMLFile.exists()) {
            // check if we have an archive here.
            // this would be usually "broken", not extracted feature
            if(!featureFolder.getName().endsWith(".jar") || !featureFolder.isFile()){
                return RC_OK;
            }

            try (JarFile jarfile = new JarFile(featureFolder)) {
                JarEntry entry = jarfile.getJarEntry("feature.xml");
                if(entry == null){
                    return RC_OK;
                }
                try (InputStream inputStream = jarfile.getInputStream(entry)){
                    feature = parseFeature(DB_FACTORY.newDocumentBuilder().parse(inputStream));
                }
            }
            if(feature == null){
                return RC_OK;
            }
            // TODO add flag if we should warn in this case
            feature.addWarningToLog("Feature is contained in a jar file: " + featureFolder + "!");
        }

        if(feature == null) {
            feature = parseFeature(DB_FACTORY.newDocumentBuilder().parse(featureXMLFile));
        }
        if (feature == null) {
            return RC_OK;
        }
        feature.setFromWorkspace(workspace);
        feature.setPath(featureXMLFile.toString());
        Feature addedFeature = state.addFeature(feature);
        if (addedFeature == feature) {
            return RC_OK;
        }
        return RC_ANALYSIS_ERROR;
    }

    /**
     * Parses the featureXml Document and returns the parsed Feature.
     *
     * @param featureXml
     *            feature.xml file that is read in and in form of Document.
     * @return parsed Feature
     */
    public static Feature parseFeature(Document featureXml) {
        if (featureXml == null) {
            return null;
        }
        if (featureXml.hasChildNodes()) {
            Element root = featureXml.getDocumentElement();
            String id = root.getAttribute("id");
            String version = root.getAttribute("version");
            Feature ret = new Feature(id, fixVersion(version));

            ret.addIncludedPluginEntries(root.getElementsByTagName("plugin"));
            ret.addIncludedFeatureEntries(root.getElementsByTagName("includes"));
            NodeList list = root.getElementsByTagName("import");
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element)list.item(i);
                String feat = e.getAttribute("feature").trim();
                String plug = e.getAttribute("plugin").trim();
                boolean optional = Boolean.parseBoolean(e.getAttribute("optional").trim());
                if(feat.isEmpty() && !plug.isEmpty()) {
                    addRequiredPlugin(e, ret, plug, optional);
                } else if(!feat.isEmpty() && plug.isEmpty()) {
                    addRequiredFeature(e, ret, feat, optional);
                }
            }
            return ret;
        }
        return null;
    }

    private static void addRequiredFeature(Element e, Feature ret, String feat, boolean optional) {
        String version = createVersion(e);
        ret.addRequiredFeatureEntry(new ManifestEntry(fixName(feat), fixVersion(version), optional));
    }

//    <import plugin="bbb"/>
//    <import plugin="aaa"  version="1.2.3" match="perfect"/>
//    <import plugin="ccc"  version="1"     match="greaterOrEqual"/>
//    <import plugin="eee"  version="1"     match="equivalent"/>
//    <import feature="ddd" version="1.2.3.qualifier" match="compatible"/>
    public static String createVersion(Element e) {
        String v = e.getAttribute("version").trim();
        if(v.isEmpty()) {
            return NamedElement.EMPTY_VERSION;
        }
        String match = e.getAttribute("match").trim();
        if(match.isEmpty()) {
            return v;
        }
        // https://dzone.com/articles/manifestmf-and-featurexml
        // http://help.eclipse.org/topic/org.eclipse.platform.doc.isv/reference/misc/feature_manifest.html
        switch (match) {
        case "perfect":
            // [1.2.3,1.2.3]
            return "[" + v + "," + v + "]";
        case "compatible":
            // [1.2.3,2.0)
            return "[" + v + "," + Version.createCompatibleRightBound(v) + ")";
        case "equivalent":
            //  [1.2.3,1.3)
            return "[" + v + "," + Version.createEquivalentRightBound(v) + ")";
        case "greaterOrEqual":
            return v;

        default:
            break;
        }
        return NamedElement.EMPTY_VERSION;
    }

    private static void addRequiredPlugin(Element e, Feature ret, String plug, boolean optional) {
        String version = createVersion(e);
        ret.addRequiredPluginEntry(new ManifestEntry(fixName(plug), fixVersion(version), optional));
    }
}
