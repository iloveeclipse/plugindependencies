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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipselabs.plugindependencies.core.PlatformState.PlatformSpecs;
import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class CommandLineInterpreter {

    final PlatformState state;
    private String fullLog;
    private final PluginParser pp;

    public CommandLineInterpreter() {
        super();
        state = new PlatformState();
        pp = new PluginParser(state);
    }

    public PlatformState getState() {
        return state;
    }

    public int interpreteInput(String[] args) {
        int numOfArgs = args.length;
        if(numOfArgs == 0){
            printHelpPage();
            return -1;
        }
        LinkedList<String> tmp = new LinkedList<>(Arrays.asList(args));
        ListIterator<String> iterator = tmp.listIterator();
        List<List<String>> commands = new ArrayList<>();
        while (iterator.hasNext()) {
            String argument = iterator.next();
            if (argument.startsWith("-")) {
                Options option = Options.getOption(argument);
                if (option == Options.UNKNOWN) {
                    Logging.getLogger().error("unknown option: '"+ argument +"'\n");
                    printHelpPage();
                    return -1;
                } else if (option == Options.Help) {
                    printHelpPage();
                    return 0;
                } else if (!option.isCommand()) {
                    iterator.remove();
                    List<String> options = new ArrayList<>();
                    while (iterator.hasNext()){
                        String next = iterator.next();
                        if(next.startsWith("-")) {
                            iterator.previous();
                            break;
                        }
                        iterator.remove();
                        options.add(next);
                    }
                    option.handle(this, options);
                } else {
                    commands.add(new ArrayList<String>());
                    commands.get(commands.size() - 1).add(argument);
                    iterator.remove();
                    while (iterator.hasNext()){
                        String next = iterator.next();
                        if(next.startsWith("-")) {
                            iterator.previous();
                            break;
                        }
                        iterator.remove();
                        commands.get(commands.size() - 1).add(next);
                    }
                }
            }

        }

        try {
            for (List<String> list : commands) {
                Options option = Options.getOption(list.remove(0));
                int result = option.handle(this, list);
                if(result == -1){
                    return -1;
                }
            }
        } finally {
            if(getFullLog() != null){
                String logs = state.dumpLogs().toString();
                if(getFullLog().isEmpty()) {
                    Logging.writeStandardOut(logs);
                } else {
                    int result = writeErrorLogFile(new File(getFullLog()), logs);
                    if(result != 0){
                        Logging.writeStandardOut(logs);
                    }
                }
            }
        }
        return 0;
    }

    void printProvidingPackage(String packageName) {
        ManifestEntry searchedPackage = new ManifestEntry(packageName, NamedElement.EMPTY_VERSION);
        Set<Package> provided = searchPackage(state.getPackages(packageName), searchedPackage);
        for (Package pack : provided) {
            Logging.writeStandardOut(pack.getInformationLine());
        }
    }

    void printDependingOnPlugin(String pluginName) {
        Set<Plugin> dependOn = state.getPlugins(pluginName);
        for (Plugin plugin : dependOn) {
            Logging.writeStandardOut("plugin: " + plugin.getInformationLine());
            Logging.writeStandardOut(plugin.printRequiringThis());
        }
    }

    void printDependingOnPackage(String packageName) {
        ManifestEntry searchedPack = new ManifestEntry(packageName, NamedElement.EMPTY_VERSION);
        Set<Package> provid = searchPackage(state.getPackages(packageName), searchedPack);
        for (Package pack : provid) {
            Logging.writeStandardOut(pack.getInformationLine());
            Logging.writeStandardOut(pack.printImportedBy(1));
        }
    }

    int generateRequirementsFile(String path) {
        try {
            if (OutputCreator.generateRequirementsfile(path, state) == -1) {
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

    int writeErrorLogFile(File out, String logs) {
        try {
            if (out.exists() && !out.delete()) {
                Logging.getLogger().error("failed to delete file " + out);
                return -1;
            }
            if (!out.createNewFile()) {
                Logging.getLogger().error("failed to create file " + out);
                return -1;
            }

            try (FileWriter toFileOut = new FileWriter(out, true)) {
                toFileOut.write(logs);
                toFileOut.write("\n");
            }
            return 0;
        } catch (IOException e) {
            Logging.getLogger().error("failed to write: " + out, e);
            return -1;
        }
    }

    int generateBuildFile(String pluginName) {
        Set<Plugin> resultSet = state.getPlugins(pluginName);
        if (!resultSet.isEmpty()) {
            Plugin plugin = resultSet.iterator().next();
            int index = plugin.getPath().lastIndexOf(File.separatorChar);
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
        Logging.getLogger().error("plugin with symbolic name " + pluginName + "not found.");
        return -1;
    }

    int generateAllBuildFiles(String sourceDir) {
        OutputCreator.setSourceFolder(sourceDir);
        if(state.getPlugins().isEmpty()){
            Logging.getLogger().error("generation failed: no plugins found, arguments: " + sourceDir);
            return -1;
        }
        Logging.writeStandardOut("Starting to generate classpath files, platform size: " + state.getPlugins().size() + " plugins");
        boolean success = true;
        int generated = 0;
        for (Plugin plugin : state.getPlugins()) {
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

    void printFocusedOSGIElement(String arg) {
        int separatorIndex = arg.indexOf(',');
        String version = "";
        if (separatorIndex != -1) {
            version = arg.substring(separatorIndex + 1);
        } else {
            separatorIndex = arg.length();
        }
        String name = arg.substring(0, separatorIndex);

        ManifestEntry searchedElement = new ManifestEntry(name, version);
        Set<Plugin> foundPlugins = searchPlugin(state.getPlugins(name), searchedElement);
        Set<Feature> foundFeatures = searchFeature(state.getFeatures(name), searchedElement);

        for (Plugin plugin : foundPlugins) {
            StringBuilder out = plugin.dump();
            Logging.writeStandardOut(out.toString());
        }
        for (Feature feature : foundFeatures) {
            StringBuilder out = feature.dump();
            Logging.writeStandardOut(out.toString());
        }

    }

    void printAllPluginsAndFeatures() {
        StringBuilder out = state.dumpAllPluginsAndFeatures();
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

    static String printLogs(Set<? extends OSGIElement> elements, boolean showWarnings) {
        StringBuilder ret = new StringBuilder();

        for (OSGIElement element : elements) {
            List<String> log = element.getLog();
            if (!log.isEmpty() && (log.toString().contains(PREFIX_ERROR) || showWarnings)) {
                ret.append(element.getPath()).append("\n");
                ret.append(printLog(element, showWarnings, "\t"));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    static String printPackageLogs(Set<Package> elements, boolean showWarnings) {
        StringBuilder ret = new StringBuilder();

        for (Package pack : elements) {
            List<String> log = pack.getLog();
            if (!log.isEmpty() && (log.toString().contains(PREFIX_ERROR) || showWarnings)) {
                ret.append(pack.getNameAndVersion()).append("\n");
                ret.append(printLog(pack, showWarnings, "\t"));
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    private static String printLog(NamedElement element, boolean showWarnings, String prefix) {
        StringBuilder ret = new StringBuilder();
        List<String> log = element.getLog();
        for (String logEntry : log) {
            if (logEntry.contains("Error") || showWarnings) {
                ret.append(prefix).append(logEntry).append("\n");
            }
        }
        return ret.toString();
    }

    public int readInEclipseFolder(String eclipsePath)
            throws IOException, SAXException, ParserConfigurationException {
        if(eclipsePath.startsWith("#")){
            return 0;
        }
        File root = new File(eclipsePath);
        File pluginsDir = new File(root, "plugins");
        int result = 0;
        boolean hasPlugins = false;
        if (pluginsDir.exists()) {
            if (pp.createPluginsAndAddToSet(pluginsDir) == -1) {
                result = -1;
            }
            hasPlugins = true;
        }
        File featureDir = new File(root, "features");
        boolean hasFeatures = false;
        if (featureDir.exists()) {
            if (FeatureParser.createFeaturesAndAddToSet(featureDir, state) == -1) {
                result = -1;
            }
            hasFeatures = true;
        }
        if(hasPlugins && hasFeatures){
            File dropinsDir = new File(root, "dropins");
            if (dropinsDir.exists()) {
                result =  readInChildren(dropinsDir);
            }
        }
        result += readInChildren(root);
        if(result < 0){
            return -1;
        }
        return result;
    }

    public int readInChildren(File directory) throws IOException, SAXException, ParserConfigurationException {
        int result = 0;
        if (pp.createPluginsAndAddToSet(directory) == -1) {
            result = -1;
        }
        if (FeatureParser.createFeaturesAndAddToSet(directory, state) == -1) {
            result = -1;
        }
        return result;
    }

    public int readInFeature(File directory) throws IOException,
        SAXException, ParserConfigurationException {
        return FeatureParser.createFeatureAndAddToSet(directory, state);
    }

    public int readInPlugin(File directory) throws IOException {
        return pp.createPluginAndAddToSet(directory);
    }

    public String getFullLog() {
        return fullLog;
    }

    public void setFullLog(String fullLog) {
        this.fullLog = fullLog;
    }

    public void setBundleVersionDummy(String dummy) {
        PlatformState.setDummyBundleVersion(dummy);
    }

    public void setBundleVersionForDummy(String real) {
        PlatformState.setBundleVersionForDummy(real);
    }

    public void setParseEarlyStartup(boolean parseEarlyStartup) {
        pp.setParseEarlyStartup(parseEarlyStartup);
    }

    public PluginParser getPluginParser() {
        return pp;
    }

    public void setPlatformSpecs(PlatformSpecs platformSpecs) {
        state.setPlatformSpecs(platformSpecs);
    }
}
