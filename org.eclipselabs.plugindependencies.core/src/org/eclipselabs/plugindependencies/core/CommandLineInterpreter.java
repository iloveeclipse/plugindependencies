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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipselabs.plugindependencies.core.PlatformState.PlatformSpecs;

/**
 * @author obroesam
 *
 */
public class CommandLineInterpreter {

    final PlatformState state;
    private String fullLog;
    private final PluginParser pp;
    private boolean continueOnFail;

    public static final int RC_OK = 0;
    public static final int RC_RUNTIME_ERROR = -1;
    public static final int RC_ANALYSIS_WARNING = -2;
    public static final int RC_ANALYSIS_ERROR = -3;

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
            return RC_RUNTIME_ERROR;
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
                    return RC_RUNTIME_ERROR;
                } else if (option == Options.Help) {
                    printHelpPage();
                    return RC_OK;
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

        int result = RC_OK;
        try {
            for (List<String> list : commands) {
                Options option = Options.getOption(list.remove(0));
                int newResult = option.handle(this, list);
                result = Math.min(result, newResult);
                if(result < RC_OK && !continueOnFail){
                    break;
                }
            }
        } finally {
            String logPath = getFullLog();
            if(logPath != null || result <= RC_ANALYSIS_ERROR){
                if(result < RC_OK){
                    Logging.writeStandardOut("Anaylsis failed with errors, check the platform state!");
                }
                String logs = state.dumpLogs().toString();
                if(logPath == null || logPath.isEmpty()) {
                    Logging.writeStandardOut(logs);
                } else {
                    int logResult = writeErrorLogFile(new File(logPath), logs);
                    if(logResult < RC_OK){
                        Logging.writeStandardOut(logs);
                    }
                }
            }
        }
        return result;
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
            return OutputCreator.generateRequirementsfile(path, state);
        } catch (IOException e) {
            Logging.getLogger().error("failed to write dependencies file to " + path, e);
            return RC_RUNTIME_ERROR;
        }
    }

    static void printHelpPage() {
        String help = "Help page\njava plugin_dependencies [-javaHome path] -eclipsePaths folder1 folder2.. [Options]:";
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
                return RC_RUNTIME_ERROR;
            }
            if (!out.createNewFile()) {
                Logging.getLogger().error("failed to create file " + out);
                return RC_RUNTIME_ERROR;
            }

            try (FileWriter toFileOut = new FileWriter(out, true)) {
                toFileOut.write(logs);
                toFileOut.write("\n");
            }
            return RC_OK;
        } catch (IOException e) {
            Logging.getLogger().error("failed to write: " + out, e);
            return RC_RUNTIME_ERROR;
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
                return OutputCreator.generateBuildFile(state, plugin);
            } catch (IOException e) {
                Logging.getLogger().error("writing build file failed:" + plugin.getInformationLine(), e);
                return RC_RUNTIME_ERROR;
            }
        }
        Logging.getLogger().error("plugin with symbolic name " + pluginName + "not found.");
        return RC_RUNTIME_ERROR;
    }

    int generateAllBuildFiles(String sourceDir) {
        OutputCreator.setSourceFolder(sourceDir);
        if(state.getPlugins().isEmpty()){
            Logging.getLogger().error("generation failed: no plugins found, arguments: " + sourceDir);
            return RC_RUNTIME_ERROR;
        }
        Logging.writeStandardOut("Starting to generate classpath files, platform size: " + state.getPlugins().size() + " plugins");
        int result = RC_OK;
        int generated = 0;
        for (Plugin plugin : state.getPlugins()) {
            if (plugin.getPath().contains(sourceDir)) {
                try {
                    int rc = OutputCreator.generateBuildFile(state, plugin);
                    if (rc < RC_OK) {
                        result = Math.min(result, rc);
                        Logging.getLogger().error("generation failed for: " + plugin.getPath() + ", " + plugin.getInformationLine());
                    } else {
                        generated ++;
                    }
                } catch (IOException e) {
                    result = Math.min(result, RC_RUNTIME_ERROR);
                    Logging.getLogger().error("generation failed for: " + plugin.getPath() + ", " + plugin.getInformationLine(), e);
                }
            }
        }
        List<Problem> errors = state.computeAllDependenciesRecursive();
        if(!errors.isEmpty()) {
            Logging.writeStandardOut("Generated " + generated + " classpath files, but platform state has errors!");
            Logging.getLogger().error("Errors computing bundle dependencies in: " + sourceDir);
            errors.forEach(e -> Logging.getLogger().error(e.toString()));
            return RC_ANALYSIS_ERROR;
        }
        if(result == RC_OK) {
            Logging.writeStandardOut("Successfully generated " + generated + " classpath files");
        }
        return result;
    }

    int analyzeTargetState(boolean showWarnings) {
        if(state.getPlugins().isEmpty()){
            Logging.getLogger().error("no plugins found");
            return RC_RUNTIME_ERROR;
        }
        Logging.writeStandardOut("Starting to analyze, platform size: " + state.getPlugins().size() + " plugins");
        List<Problem> errors = state.computeAllDependenciesRecursive();
        if(!errors.isEmpty()) {
            Logging.getLogger().error("Errors analyzing bundle dependencies");
            errors.forEach(e -> Logging.getLogger().error(e.toString()));
        }
        List<Problem> warnings = Collections.emptyList();
        if(showWarnings && errors.isEmpty()) {
            warnings = state.collectWarnings();
            if(!warnings.isEmpty()) {
                Logging.getLogger().warning("Warnings analyzing bundle dependencies");
                warnings.forEach(e -> Logging.getLogger().warning(e.toString()));
            }
        }
        if(!errors.isEmpty()) {
            return RC_ANALYSIS_ERROR;
        }
        if(!warnings.isEmpty()) {
            return RC_ANALYSIS_WARNING;
        }
        Logging.writeStandardOut("Successfully analyzed " + state.getPlugins().size() + " plugins");
        return RC_OK;
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
            List<Problem> log = element.getLog();
            if (!log.isEmpty() && (log.stream().anyMatch(x -> x.isError()) || showWarnings)) {
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
            List<Problem> log = pack.getLog();
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
        List<Problem> log = element.getLog();
        for (Problem logEntry : log) {
            if (logEntry.isError() || showWarnings) {
                ret.append(prefix).append(logEntry.getLogMessage()).append("\n");
            }
        }
        return ret.toString();
    }

    public int readInEclipseFolder(String eclipsePath) throws IOException {
        int result = RC_OK;
        if(eclipsePath.startsWith("#")){
            return result;
        }
        File root = new File(eclipsePath);
        File pluginsDir = new File(root, "plugins");
        boolean hasPlugins = false;
        if (pluginsDir.exists()) {
            result = Math.min(result, pp.createPluginsAndAddToSet(pluginsDir));
            hasPlugins = true;
        }
        File featureDir = new File(root, "features");
        boolean hasFeatures = false;
        if (featureDir.exists()) {
            result = Math.min(result, FeatureParser.createFeaturesAndAddToSet(featureDir, state));
            hasFeatures = true;
        }
        if(hasPlugins && hasFeatures){
            File dropinsDir = new File(root, "dropins");
            if (dropinsDir.exists()) {
                result =  readInChildren(dropinsDir);
            }
        }
        result = Math.min(result, readInChildren(root));
        return result;
    }

    public int readInChildren(File directory) throws IOException {
        int result = pp.createPluginsAndAddToSet(directory);
        result = Math.min(result, FeatureParser.createFeaturesAndAddToSet(directory, state));
        return result;
    }

    public int readInFeature(File directory, boolean workspace) throws IOException {
        return FeatureParser.createFeatureAndAddToSet(directory, workspace, state);
    }

    public int readInPlugin(File directory, boolean workspace) throws IOException {
        return pp.createPluginAndAddToSet(directory, workspace);
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

    public void setBundlesWithCycles(List<String> bundleIds) {
        state.setIgnoredBundlesWithCycles(new LinkedHashSet<>(bundleIds));
    }

    public void setContinueOnFail(boolean b) {
        continueOnFail = b;
    }
}
