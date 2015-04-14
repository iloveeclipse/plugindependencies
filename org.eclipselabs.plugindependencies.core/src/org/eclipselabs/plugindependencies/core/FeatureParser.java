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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class FeatureParser {
    static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

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
            Logging.writeErrorOut("given directory does not exist: " + rootDir);
            return 2;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.writeErrorOut("given directory is not a directory or is not readable: " + rootDir);
            return 3;
        }
        PluginParser.sortFiles(dirArray);
        int result = 0;
        for (File featureFolder : dirArray) {
            if (createFeatureAndAddToSet(featureFolder, state) != 0) {
                result = -1;
            }
        }
        return result;
    }

    public static int createFeatureAndAddToSet(File featureFolder, PlatformState state)
            throws IOException, SAXException, ParserConfigurationException {
        String path = featureFolder.getCanonicalPath() + "/feature.xml";
        File featureXMLFile = new File(path);
        if (!featureXMLFile.exists()) {
            return 0;
        }

        Feature feature = parseFeature(dbFactory.newDocumentBuilder().parse(featureXMLFile));
        if (feature == null) {
            return 0;
        }
        feature.setPath(path);
        Feature addedFeature = state.addFeature(feature);
        if (addedFeature == feature) {
            return 0;
        }
        List<String> equalFeaturePaths = new ArrayList<>();
        equalFeaturePaths.add(feature.getPath());
        for (Feature feat : state.getFeatures(feature.getName())) {
            if (feat.equals(feature)) {
                equalFeaturePaths.add(feat.getPath());
            }
        }
        StringBuilder output = new StringBuilder("two features with equal id and version: ")
                .append(feature.getName()).append(" ").append(feature.getVersion());
        Collections.sort(equalFeaturePaths);
        for (String featurePath : equalFeaturePaths) {
            output.append("\n").append(featurePath);
        }
        Logging.writeErrorOut(output.toString());
        return -1;
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
            Feature ret = new Feature(id, version);

            ret.addRequiredPlugins(root.getElementsByTagName("plugin"));
            ret.addRequiredFeatures(root.getElementsByTagName("includes"));
            return ret;
        }
        return null;
    }
}
