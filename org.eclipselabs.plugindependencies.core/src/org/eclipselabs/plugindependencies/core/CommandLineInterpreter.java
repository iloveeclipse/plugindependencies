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

import static org.eclipselabs.plugindependencies.core.Logging.PREFIX_ERROR;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class CommandLineInterpreter {

    public static int interpreteInput(String[] args) {
        Options option = null;
        int numOfArgs = args.length;
        if(numOfArgs == 0){
            printHelpPage();
            return -1;
        }
        for (int j = 0; j < numOfArgs; j++) {
            String argument = args[j];
            if (argument.startsWith("-")) {
                option = Options.getOption(argument);
                if (option == Options.UNKNOWN) {
                    Logging.writeStandardOut("Unknown option\n");
                    printHelpPage();
                } else {
                    if (j + 1 >= numOfArgs || args[j + 1].startsWith("-")) {
                        if (option.handle(null, "") == -1) {
                            return -1;
                        }
                    } else {
                        List<String> argList = new ArrayList<>();
                        while (j + 1 < numOfArgs && !args[j + 1].startsWith("-")) {
                            argList.add(args[++j]);
                        }
                        if (option.handle(null, argList.toArray(new String[argList.size()])) == -1) {
                            return -1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    static void printProvidingPackage(String packageName) {
        ManifestEntry searchedPackage = new ManifestEntry(packageName, "");
        Set<Package> provided = searchPackage(PlatformState.getPackageSet(), searchedPackage);
        for (Package pack : provided) {
            Logging.writeStandardOut(pack.getInformationLine());
        }
    }

    static void printDependingOnPlugin(String pluginName) {
        ManifestEntry searchedPlugin = new ManifestEntry(pluginName, "");
        Set<Plugin> dependOn = searchPlugin(PlatformState.getPluginSet(), searchedPlugin);
        for (Plugin plugin : dependOn) {
            Logging.writeStandardOut("plugin: " + plugin.getInformationLine());
            Logging.writeStandardOut(plugin.printRequiringThis());
        }
    }

    static void printDependingOnPackage(String packageName) {
        ManifestEntry searchedPack = new ManifestEntry(packageName, "");
        Set<Package> provid = searchPackage(PlatformState.getPackageSet(), searchedPack);
        for (Package pack : provid) {
            Logging.writeStandardOut(pack.getInformationLine());
            Logging.writeStandardOut(pack.printImportedBy(1));
        }
    }

    static int generateRequirementsFile(String path, Set<Plugin> plugins) {
        try {
            if (OutputCreator.generateRequirementsfile(path, plugins) == -1) {
                return -1;
            }
        } catch (IOException e) {
            Logging.getLogger().error("failed to write dependencies file to " + path, e);
            return -1;
        }
        return 0;
    }

    static void printHelpPage() {
        String help = "Help page\njava plugin_dependencies [-javaHome path] -eclipsePath folder1 folder2.. [Options]:";
        Logging.writeStandardOut(help);
        for (Options opt : Options.values()) {
            if (!opt.toString().equals("UNKNOWN")) {
                opt.printHelp("");
            }
        }
    }

    static int writeErrorLogFile(String path) {
        StringBuilder errorLog = new StringBuilder();
        File out = new File(path);
        try {
            if (out.exists() && !out.delete()) {
                Logging.writeErrorOut("failed to delete file " + path);
                return -1;
            }
            if (!out.createNewFile()) {
                Logging.writeErrorOut("failed to create file " + path);
                return -1;
            }

            try (FileWriter toFileOut = new FileWriter(out, true)) {
                errorLog.append("feature error log:\n");
                errorLog.append(printUnresolvedDependencies(PlatformState.getFeatureSet(), true));
                errorLog.append("plugin error log:\n");
                errorLog.append(printUnresolvedDependencies(PlatformState.getPluginSet(), true));
                toFileOut.write(errorLog.toString());
            }
            return 0;
        } catch (IOException e) {
            Logging.getLogger().error("failed to write: " + path, e);
            return -1;
        }
    }

    static int generateBuildFile(String pluginName) {
        Set<Plugin> resultSet = searchPlugin(PlatformState.getPluginSet(), new ManifestEntry(pluginName, ""));
        if (!resultSet.isEmpty()) {
            Plugin plugin = resultSet.iterator().next();
            int index = plugin.getPath().lastIndexOf('/');
            String sourceFolder = plugin.getPath().substring(0, index);
            OutputCreator.setSourceFolder(sourceFolder);
            try {
                if (OutputCreator.generateBuildFile(plugin) == -1) {
                    return -1;
                }
            } catch (IOException e) {
                Logging.getLogger().error("writing build file failed:" + plugin.getInformationLine(), e);
                return -1;
            }
            return 0;
        }
        Logging.writeErrorOut("plugin with symbolic name " + pluginName + "not found.");
        return -1;
    }

    static int generateAllBuildFiles(String sourceDir) {
        OutputCreator.setSourceFolder(sourceDir);
        if(PlatformState.getPluginSet().isEmpty()){
            Logging.getLogger().error("generation failed: no plugins found, arguments: " + sourceDir);
            return -1;
        }
        Logging.writeStandardOut("Starting to generate classpath files, platform size: " + PlatformState.getPluginSet().size() + " plugins");
        boolean success = true;
        int generated = 0;
        for (Plugin plugin : PlatformState.getPluginSet()) {
            if (plugin.getPath().contains(sourceDir)) {
                try {
                    if (OutputCreator.generateBuildFile(plugin) == -1) {
                        success = false;
                        Logging.getLogger().error("generation failed for: " + plugin.getPath() + ", " + plugin.getInformationLine());
                    } else {
                        generated ++;
                    }
                } catch (IOException e) {
                    success = false;
                    Logging.getLogger().error("generation failed for: " + plugin.getPath() + ", " + plugin.getInformationLine(), e);
                }
            }
        }
        if(success) {
            Logging.writeStandardOut("Successfully generated " + generated + " classpath files");
            return 0;
        }
        return -1;
    }

    static void printFocusedOSGIElement(String arg) {
        int separatorIndex = arg.indexOf(',');
        String version = "";
        if (separatorIndex != -1) {
            version = arg.substring(separatorIndex + 1);
        } else {
            separatorIndex = arg.length();
        }
        String name = arg.substring(0, separatorIndex);

        ManifestEntry searchedElement = new ManifestEntry(name, version);
        Set<Plugin> foundPlugins = searchPlugin(PlatformState.getPluginSet(), searchedElement);
        Set<Feature> foundFeatures = searchFeature(PlatformState.getFeatureSet(), searchedElement);

        for (Plugin plugin : foundPlugins) {
            StringBuilder out = new StringBuilder();
            out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
            out.append(plugin.getName() + " " + plugin.getVersion() + "\n");
            out.append("required plugins:\n");
            for (ManifestEntry requiredPlugin : plugin.getRequiredPlugins()) {
                String sep = requiredPlugin.getVersion().isEmpty()? "" : " ";
                out.append("\t" + requiredPlugin.getName() + sep + requiredPlugin.getVersion());
                if (requiredPlugin.isOptional()) {
                    out.append(" *optional*");
                }
                out.append("\n");
                for (Plugin resolvedPlugin : plugin.getResolvedPlugins()) {
                    if (requiredPlugin.isMatching(resolvedPlugin)) {
                        out.append("\t-> " + resolvedPlugin.getName() + " "
                                + resolvedPlugin.getVersion() + "\n");
                    }
                }
            }
            out.append("required packages:\n");
            for (ManifestEntry requiredPackage : plugin.getRequiredPackages()) {
                String sep = requiredPackage.getVersion().isEmpty()? "" : " ";
                out.append("\t" + requiredPackage.getName() + sep + requiredPackage.getVersion());
                if (requiredPackage.isDynamicImport()) {
                    out.append(" *dynamicImport*");
                }
                if (requiredPackage.isOptional()) {
                    out.append(" *optional*");
                }
                out.append("\n");
                for (Package resolvedPackage : plugin.getImportedPackages()) {
                    if (requiredPackage.isMatching(resolvedPackage)) {
                        String sep2 = resolvedPackage.getVersion().isEmpty()? "" : " ";
                        out.append("\t->package: " + resolvedPackage.getName() + sep2
                                + resolvedPackage.getVersion() + "\n");
                        out.append("\t\texported by:\n");
                        Set<Plugin> exportedBy = resolvedPackage.getExportedBy();
                        if (exportedBy.size() == 0) {
                            out.append("\t\tJRE System Library");

                        } else {
                            for (Plugin plug : exportedBy) {
                                out.append("\t\t");
                                out.append(plug.isFragment() ? "fragment: " : "plugin: ");
                                out.append(plug.getName() + " " + plug.getVersion()
                                + "\n\n");
                            }
                        }
                    }
                }
            }
            out.append("included in feature:\n");
            for (Feature feature : plugin.getIncludedInFeatures()) {
                out.append("\t" + feature.getName() + " " + feature.getVersion() + "\n");
            }
            out.append("required by plugins:\n");
            for (Plugin neededBy : plugin.getRequiredBy()) {
                out.append("\t");
                if (neededBy.isOptional(plugin)) {
                    out.append("*optional* for ");
                }
                out.append(neededBy.getName() + " " + neededBy.getVersion() + "\n");
            }
            out.append("exported packages:\n");
            for (Package exportedPackage : plugin.getExportedPackages()) {
                String sep = exportedPackage.getVersion().isEmpty()? "" : " ";
                out.append("\t" + exportedPackage.getName() + sep
                        + exportedPackage.getVersion());
                if (exportedPackage.getReexportedBy().contains(plugin)) {
                    out.append(" *reexport*");
                }
                out.append("\n");
            }
            if (plugin.isFragment()) {
                out.append("fragment host:\n");
                Plugin fragmentHost = plugin.getHost();
                out.append(fragmentHost.getName() + " " + fragmentHost.getVersion()
                + "\n");
            } else {
                out.append("fragments:\n");
                for (Plugin fragment : plugin.getFragments()) {
                    out.append("\t" + fragment.getName() + " " + fragment.getVersion()
                    + "\n");
                }
            }
            Logging.writeStandardOut(out.toString());
        }
        for (Feature feature : foundFeatures) {
            StringBuilder out = new StringBuilder();
            out.append("feature: " + feature.getName() + " " + feature.getVersion()
            + "\n");
            out.append("included features:\n");
            for (Feature included : feature.getIncludedFeatures()) {
                out.append("\t" + included.getName() + " " + included.getVersion() + "\n");
            }
            out.append("included plugins:\n");
            for (Plugin included : feature.getResolvedPlugins()) {
                out.append("\t");
                out.append(included.isFragment() ? "fragment: " : "plugin: ");
                out.append(included.getName() + " " + included.getVersion() + "\n");
            }
            out.append("included in features:\n");
            for (Feature includedIn : feature.getIncludedInFeatures()) {
                out.append("\t" + includedIn.getName() + " " + includedIn.getVersion()
                + "\n");
            }
            Logging.writeStandardOut(out.toString());
        }

    }

    static void printAllPluginsAndFeatures() {
        StringBuilder out = new StringBuilder();
        List<Plugin> plugins = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        List<Plugin> fragments = new ArrayList<>();

        for (Plugin plugin : PlatformState.getPluginSet()) {
            if (plugin.isFragment()) {
                fragments.add(plugin);
            } else {
                plugins.add(plugin);
            }
        }
        features.addAll(PlatformState.getFeatureSet());

        Comparator<OSGIElement> comp = new Comparator<OSGIElement>() {
            @Override
            public int compare(OSGIElement o1, OSGIElement o2) {
                int diff = o1.getName().compareTo(o2.getName());
                if (diff != 0) {
                    return diff;
                }
                Version v1 = new Version(o1.getVersion());
                Version v2 = new Version(o2.getVersion());
                return v1.compareTo(v2);
            }
        };

        Collections.sort(plugins, comp);
        Collections.sort(features, comp);
        Collections.sort(fragments, comp);

        out.append("features:\n");
        for (Feature feature : features) {
            out.append("\t" + feature.getInformationLine() + "\n");
        }
        out.append("plugins:\n");
        for (Plugin plugin : plugins) {
            out.append("\t" + plugin.getInformationLine() + "\n");
        }
        out.append("fragments:\n");
        for (Plugin fragment : fragments) {
            out.append("\t" + fragment.getInformationLine() + "\n");
        }
        Logging.writeStandardOut(out.toString());
    }

    static Set<Plugin> searchPlugin(Set<Plugin> searchIn, ManifestEntry entry) {
        Set<Plugin> returnSet = new LinkedHashSet<Plugin>();
        for (Plugin plugin : searchIn) {
            if (entry.isMatching(plugin)) {
                returnSet.add(plugin);
            }
        }
        return returnSet;
    }

    static Set<Feature> searchFeature(Set<Feature> searchIn, ManifestEntry entry) {
        Set<Feature> returnSet = new LinkedHashSet<>();
        for (Feature feature : searchIn) {
            if (entry.isMatching(feature)) {
                returnSet.add(feature);
            }
        }
        return returnSet;
    }

    static Set<Package> searchPackage(Set<Package> searchIn, ManifestEntry entry) {
        Set<Package> returnSet = new LinkedHashSet<Package>();
        for (Package pack : searchIn) {
            if (entry.isMatching(pack)) {
                returnSet.add(pack);
            }
        }
        return returnSet;
    }

    static String printUnresolvedDependencies(Set<? extends OSGIElement> elements, boolean showWarnings) {
        StringBuilder ret = new StringBuilder();

        for (OSGIElement element : elements) {
            List<String> log = element.getLog();
            if (!log.isEmpty() && (log.toString().contains(PREFIX_ERROR) || showWarnings)) {
                ret.append(element instanceof Plugin ? "plugin: " : "feature: ");
                ret.append(element.getInformationLine() + "\n");
                ret.append(printLog(element, showWarnings));
                ret.append(backtrace(element, 0));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    private static String printLog(NamedElement element, boolean showWarnings) {
        StringBuilder ret = new StringBuilder();
        List<String> log = element.getLog();
        for (String logEntry : log) {
            if (logEntry.contains("Error") || showWarnings) {
                ret.append("\t" + logEntry + "\n");
            }
        }
        return ret.toString();
    }

    private static String backtrace(OSGIElement element, int indentation) {
        String indent = StringUtil.multiplyString("\t", indentation);
        Set<Feature> containedIn = element.getIncludedInFeatures();
        StringBuilder ret = new StringBuilder();
        if (!containedIn.isEmpty()) {
            ret.append(indent + "contained in feature:\n");
        }
        for (Feature feat : containedIn) {
            ret.append(indent + "\t");
            ret.append(feat.getInformationLine() + "\n");
            ret.append(backtrace(feat, indentation + 1));
        }

        if (element instanceof Plugin) {
            String needingThis = ((Plugin) element).printRequiringThis();
            ret.append(needingThis);
        }
        return ret.toString();
    }

    static int readInEclipseFolder(String eclipsePath)
            throws IOException, SAXException, ParserConfigurationException {
        File root = new File(eclipsePath);
        File pluginsDir = new File(root, "plugins");
        boolean hasPlugins = false;
        if (pluginsDir.exists()) {
            if (PluginParser.createPluginsAndAddToSet(pluginsDir, PlatformState.getPluginSet(), PlatformState.getPackageSet()) == -1) {
                return -1;
            }
            hasPlugins = true;
        }
        File featureDir = new File(root, "features");
        boolean hasFeatures = false;
        if (featureDir.exists()) {
            if (FeatureParser.createFeaturesAndAddToSet(featureDir, PlatformState.getFeatureSet()) == -1) {
                return -1;
            }
            hasFeatures = true;
        }
        if(hasPlugins && hasFeatures){
            File dropinsDir = new File(root, "dropins");
            if (dropinsDir.exists()) {
                return readInChildren(dropinsDir);
            }
        }
        return readInChildren(root);
    }

    private static int readInChildren(File directory) throws IOException, SAXException, ParserConfigurationException {
        if (PluginParser.createPluginsAndAddToSet(directory, PlatformState.getPluginSet(), PlatformState.getPackageSet()) == -1) {
            return -1;
        }
        if (FeatureParser.createFeaturesAndAddToSet(directory, PlatformState.getFeatureSet()) == -1) {
            return -1;
        }
        return 0;
    }

    public static int readInFeature(File directory) throws IOException,
        SAXException, ParserConfigurationException {
        return FeatureParser.createFeatureAndAddToSet(directory, PlatformState.getFeatureSet());
    }

    public static int readInPlugin(File directory) throws IOException {
        return PluginParser.createPluginAndAddToSet(directory, PlatformState.getPluginSet(), PlatformState.getPackageSet());
    }
}
