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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
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

    /**
     * Parses all Features located in featureDirectoryPath and adds them to the features
     * Set. Features are parsed through the feature.xml file which is located in each
     * feature Folder.
     *
     * @param featureDirectoryPath
     *            Path to Folder where Features are located
     * @param features
     *            Set to add the parsed Features
     * @throws IOException
     *             From reading file system
     * @throws SAXException
     *             if any parse Errors occur
     * @throws ParserConfigurationException
     *             if a DocumentBuilder cannot be created which satisfies the
     *             configuration requested.
     */
    public static int readFeatures(String featureDirectoryPath, Set<Feature> features)
            throws IOException, SAXException, ParserConfigurationException {
        if (featureDirectoryPath == null) {
            Logging.writeErrorOut("Given directory path is null");
            return 1;
        }
        File rootDir = new File(featureDirectoryPath);
        if (!rootDir.exists()) {
            Logging.writeErrorOut("Given directory does not exist: "
                    + featureDirectoryPath);
            return 2;
        }

        File[] dirArray = rootDir.listFiles();
        if(dirArray == null){
            Logging.writeErrorOut("Given directory is not a directory or is not readable: "
                    + featureDirectoryPath);
            return 3;
        }
        PluginParser.sortFiles(dirArray);

        for (File featureFolder : dirArray) {
            if (createFeatureAndAddToSet(featureFolder, features) != 0) {
                return -1;
            }
        }
        return 0;
    }

    public static int createFeatureAndAddToSet(File featureFolder, Set<Feature> features)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        if (!featureFolder.isHidden()) {
            String path = featureFolder.getCanonicalPath() + "/feature.xml";
            File featureXMLFile = new File(path);
            if (featureXMLFile.exists()) {
                Feature feature = parseFeature(dBuilder.parse(featureXMLFile));
                if (feature != null) {
                    feature.setPath(path);
                    if (!features.add(feature)) {
                        Logging.writeErrorOut("FATAL Error: Two Features with equal Id and Version.");
                        Logging.writeErrorOut(feature.getName() + " "
                                + feature.getVersion());
                        List<String> equalFeaturePaths = new ArrayList<>();
                        equalFeaturePaths.add(feature.getPath());
                        for (Feature feat : features) {
                            if (feat.equals(feature)) {
                                equalFeaturePaths.add(feat.getPath());
                            }
                        }
                        Collections.sort(equalFeaturePaths);
                        for (String featurePath : equalFeaturePaths) {
                            Logging.writeErrorOut(featurePath);
                        }
                        return -1;
                    }
                }
            }
        }
        return 0;
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
