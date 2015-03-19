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

import static org.eclipselabs.plugindependencies.core.MainClass.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class CommandLineInterpreter {

    enum Options {
        Providing("-providing") {
            @Override
            int handle(String... args) {
                for (String arg : args) {
                    printProvidingPackage(arg);
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-providing package" + "\t\t\t"
                        + "Prints the plugin(s), which export the specified package";
                Logging.writeStandardOut(help);
            }
        },

        UnresolvedDependenciesPlugins("-unresPlugin") {
            @Override
            int handle(String... args) {
                boolean showWarnings = args != null && args.length >0 && args[0].equals("w") ? true : false;
                Logging.writeStandardOut(printUnresolvedDependencies(pluginSet,
                        showWarnings));
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-unresPlugin [w]" + "\t\t\t"
                        + "Prints unresolved plugins with error logs."
                        + " With optional w, warnings can be enabled for printing.";
                Logging.writeStandardOut(help);
            }
        },

        UnresolvedDependenciesFeatures("-unresFeature") {
            @Override
            int handle(String... args) {
                boolean showWarnings = args != null && args.length >0 && args[0].equals("w") ? true : false;
                Logging.writeStandardOut(printUnresolvedDependencies(featureSet,
                        showWarnings));
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-unresFeature [w]" + "\t\t\t"
                        + "Prints unresolved features with error logs."
                        + " With optional w, warnings can be enabled for printing.";
                Logging.writeStandardOut(help);
            }
        },

        DependOnPlugin("-dependOnPlugin") {
            @Override
            int handle(String... args) {
                for (String arg : args) {
                    printDependingOnPlugin(arg);
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-dependOnPlugin plugin" + "\t\t"
                        + "Prints all plugins which need the specified plugin.";
                Logging.writeStandardOut(help);
            }
        },

        DependOnPackage("-dependOnPackage") {
            @Override
            int handle(String... args) {
                for (String arg : args) {
                    printDependingOnPackage(arg);
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-dependOnPackage package"
                        + "\t\t"
                        + "Prints all plugins which need the specified package.";
                Logging.writeStandardOut(help);
            }
        },

        GenerateRequirementsFile("-generateReqFile") {
            @Override
            int handle(String... args) {
                for (String arg : args) {
                    int result = generateRequirementsFile(arg);
                    if(result != 0){
                        return result;
                    }
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-generateReqFile file" + "\t\t"
                        + "Writes requirements of each plugin in the specified file."
                        + " The file has the form pluginA:pluginB in each line."
                        + " This just means pluginA depends on pluginB."
                        + " The plugins are written in the file as canonical paths.";
                Logging.writeStandardOut(help);
            }
        },

        GenerateBuildFile("-generateBuildFile") {
            @Override
            int handle(String... args) {
                if(args.length > 1) {
                    OutputCreator.setTargetFolder(args[1]);
                }
                return generateBuildFile(args[0]);
            }

            @Override
            void printHelp(String arg) {
                String help = "-generateBuildFile sourceFolder [targetFolder]"
                        + "\t\t"
                        + "Generates the build file with all classpaths of the specified plugin."
                        + " Optionally path to the extra plugins directory can be specified with targetFolder."
                        + " Generated file is saved in the plugin folder."
                        + " Eclipse root and bundle version have to be set before.";
                Logging.writeStandardOut(help);
            }
        },

        GenerateAllBuildFiles("-generateAllBuild") {
            @Override
            int handle(String... args) {
                if(args.length > 1) {
                    OutputCreator.setTargetFolder(args[1]);
                }
                return generateAllBuildFiles(args[0]);
            }

            @Override
            void printHelp(String arg) {
                String help = "-generateAllBuild sourceFolder [targetFolder]"
                        + "\t\t"
                        + "Generates a build file with all classpaths of every plugin in the specified folder."
                        + " Optionally path to the extra plugins directory can be specified with targetFolder."
                        + " Generated files are saved in each plugin folder."
                        + " Eclipse root and bundle version have to be set before.";
                Logging.writeStandardOut(help);
            }
        },

        Help("-h") {
            @Override
            int handle(String... args) {
                printHelpPage();
                return 0;
            }

            @Override
            void printHelp(String arg) {
                // DO NOTHING
            }
        },

        FullLogFile("-fullLog") {
            @Override
            int handle(String... args) {
                return writeErrorLogFile(args[0]);
            }

            @Override
            void printHelp(String arg) {
                String help = "-fullLog file"
                        + "\t\t\t"
                        + "Writes the full error log to the specified file."
                        + " Warnings are included.";
                Logging.writeStandardOut(help);
            }
        },

        EclipsePaths("-eclipsePaths") {
            @Override
            int handle(String... args) {
                try {
                    for (String arg : args) {
                        int result = readInEclipseFolder(arg);
                        if(result != 0){
                            return result;
                        }
                    }
                    resolveDependencies();
                    return 0;
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    Logging.writeErrorOut("Error while reading from folder " + Arrays.toString(args));
                }
                return -1;
            }

            @Override
            void printHelp(String arg) {
                String help = "-eclipsePaths folder1 folder2 ..."
                        + "\t"
                        + "Eclipse target platform plugin and feature folders are specified here (canonical paths)."
                        + " Either Eclipse folder with subfolders \"plugins\" and \"features\" "
                        + "or a folder just containing plugins is possible.";
                Logging.writeStandardOut(help);
            }
        },

        JavaHome("-javaHome") {
            @Override
            int handle(String... args) {
                setJavaHome(args[0]);
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-javaHome path" + "\t\t\t\t"
                        + "Changes the Java home path to the specified path."
                        + " Default is the Java home of the running Java.";
                Logging.writeStandardOut(help);
            }
        },

        EclipseRoot("-deploymentRoot") {
            @Override
            int handle(String... args) {
                try {
                    OutputCreator.setEclipseRoot(args[0]);
                } catch (IOException e) {
                    Logging.writeErrorOut("Error resolving deployment root: " + Arrays.toString(args));
                    return -1;
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-deploymentRoot path" + "\t\t\t"
                        + "Changes the target deployment root path used for final plugins deployment to the specified path."
                        + " Default is empty string.";
                Logging.writeStandardOut(help);
            }
        },

        BundleVersion("-bundleVersion") {
            @Override
            int handle(String... args) {
                OutputCreator.setBundleVersion(args[0]);
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-bundleVersion version" + "\t\t\t"
                        + "Changes the default bundle version to the specified version."
                        + " Default is 0.0.0.";
                Logging.writeStandardOut(help);
            }
        },

        Focus("-focus") {
            @Override
            int handle(String... args) {
                for (String arg : args) {
                    printFocusedOSGIElement(arg);
                }
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-focus name[,version] name[,version]..." + "\t"
                        + "Focus on plugin/feature with given name."
                        + " Optionally you can specify a version."
                        + " It is possible to focus on more than one element.";
                Logging.writeStandardOut(help);
            }
        },

        PrintAllPluginsAndFeatures("-printAll") {
            @Override
            int handle(String... args) {
                printAllPluginsAndFeatures();
                return 0;
            }

            @Override
            void printHelp(String arg) {
                String help = "-printAll"
                        + "\t\t\t\t"
                        + "Prints all plugins and features found in the specified folders.";
                Logging.writeStandardOut(help);
            }
        },

        UNKNOWN("");

        private final String optionName;

        Options(String name) {
            optionName = name;
        }

        int handle(String... args) {
            throw new UnsupportedOperationException();
        }

        void printHelp(String arg) {
            throw new UnsupportedOperationException();
        }

        static Options getOption(String arg) {
            for (Options opt : values()) {
                if (opt.optionName.equals(arg)) {
                    return opt;
                }
            }
            return UNKNOWN;
        }
    }

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
                        if (option.handle("") == -1) {
                            return -1;
                        }
                    } else {
                        List<String> argList = new ArrayList<>();
                        while (j + 1 < numOfArgs && !args[j + 1].startsWith("-")) {
                            argList.add(args[++j]);
                        }
                        if (option.handle(argList.toArray(new String[argList.size()])) == -1) {
                            return -1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    private static void printProvidingPackage(String packageName) {
        ManifestEntry searchedPackage = new ManifestEntry(packageName, "");
        Set<Package> provided = searchPackage(packageSet, searchedPackage);
        for (Package pack : provided) {
            Logging.writeStandardOut(pack.getInformationLine());
        }
    }

    private static void printDependingOnPlugin(String pluginName) {
        ManifestEntry searchedPlugin = new ManifestEntry(pluginName, "");
        Set<Plugin> dependOn = searchPlugin(pluginSet, searchedPlugin);
        for (Plugin plugin : dependOn) {
            Logging.writeStandardOut("Plugin: " + plugin.getInformationLine());
            Logging.writeStandardOut(plugin.printRequiringThis());
        }
    }

    private static void printDependingOnPackage(String packageName) {
        ManifestEntry searchedPack = new ManifestEntry(packageName, "");
        Set<Package> provid = searchPackage(packageSet, searchedPack);
        for (Package pack : provid) {
            Logging.writeStandardOut(pack.getInformationLine());
            Logging.writeStandardOut(pack.printImportedBy(1));
        }
    }

    private static int generateRequirementsFile(String path) {
        try {
            if (OutputCreator.generateRequirementsfile(path) == -1) {
                return -1;
            }
        } catch (IOException e) {
            Logging.writeErrorOut("Error while writing dependencies-file to " + path);
            return -1;
        }
        return 0;
    }

    private static void printHelpPage() {
        String help = "Help page\njava plugin_dependencies [-javaHome path] -eclipsePath folder1 folder2.. [Options]:";
        Logging.writeStandardOut(help);
        for (Options opt : Options.values()) {
            if (!opt.toString().equals("UNKNOWN")) {
                opt.printHelp("");
            }
        }
    }

    private static int writeErrorLogFile(String path) {
        StringBuilder errorLog = new StringBuilder();
        File out = new File(path);
        try {
            if (out.exists() && !out.delete()) {
                Logging.writeErrorOut("Can not delete file " + path);
                return -1;
            }
            if (!out.createNewFile()) {
                Logging.writeErrorOut("Can not create file " + path);
                return -1;
            }

            try (FileWriter toFileOut = new FileWriter(out, true)) {
                errorLog.append("Feature error log:\n");
                errorLog.append(printUnresolvedDependencies(featureSet, true));
                errorLog.append("Plugin error log:\n");
                errorLog.append(printUnresolvedDependencies(pluginSet, true));
                toFileOut.write(errorLog.toString());
            }
            return 0;
        } catch (IOException e) {
            Logging.writeErrorOut("Error while writing data to file.");
            return -1;
        }
    }

    private static int generateBuildFile(String pluginName) {
        Set<Plugin> resultSet = searchPlugin(pluginSet, new ManifestEntry(pluginName, ""));
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
                Logging.writeErrorOut("Writing build file failed:");
                Logging.writeErrorOut(plugin.getInformationLine());
                return -1;
            }
            return 0;
        }
        Logging.writeErrorOut("Plugin with symbolic name " + pluginName + "not found.");
        return -1;
    }

    private static int generateAllBuildFiles(String sourceDir) {
        OutputCreator.setSourceFolder(sourceDir);
        for (Plugin plugin : pluginSet) {
            if (plugin.getPath().contains(sourceDir)) {
                try {
                    if (OutputCreator.generateBuildFile(plugin) == -1) {
                        return -1;
                    }
                } catch (IOException e) {
                    Logging.writeErrorOut("Writing build file failed:");
                    Logging.writeErrorOut(plugin.getInformationLine());
                    return -1;
                }
            }
        }
        return 0;
    }

    private static void printFocusedOSGIElement(String arg) {
        int separatorIndex = arg.indexOf(',');
        String version = "";
        if (separatorIndex != -1) {
            version = arg.substring(separatorIndex + 1);
        } else {
            separatorIndex = arg.length();
        }
        String name = arg.substring(0, separatorIndex);

        ManifestEntry searchedElement = new ManifestEntry(name, version);
        Set<Plugin> foundPlugins = searchPlugin(pluginSet, searchedElement);
        Set<Feature> foundFeatures = searchFeature(featureSet, searchedElement);

        for (Plugin plugin : foundPlugins) {
            StringBuilder out = new StringBuilder();
            out.append(plugin.isFragment() ? "Fragment: " : "Plugin: ");
            out.append(plugin.getName() + " " + plugin.getVersion() + "\n");
            out.append("Required Plugins:\n");
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
            out.append("Required Packages:\n");
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
                        out.append("\t->Package: " + resolvedPackage.getName() + sep2
                                + resolvedPackage.getVersion() + "\n");
                        out.append("\t\tExported By:\n");
                        Set<Plugin> exportedBy = resolvedPackage.getExportedBy();
                        if (exportedBy.size() == 0) {
                            out.append("\t\tJRE System Library");

                        } else {
                            for (Plugin plug : exportedBy) {
                                out.append("\t\t");
                                out.append(plug.isFragment() ? "Fragment: " : "Plugin: ");
                                out.append(plug.getName() + " " + plug.getVersion()
                                        + "\n\n");
                            }
                        }
                    }
                }
            }
            out.append("Included in Feature:\n");
            for (Feature feature : plugin.getIncludedInFeatures()) {
                out.append("\t" + feature.getName() + " " + feature.getVersion() + "\n");
            }
            out.append("Required by Plugins:\n");
            for (Plugin neededBy : plugin.getRequiredBy()) {
                out.append("\t");
                if (neededBy.isOptional(plugin)) {
                    out.append("*optional* for ");
                }
                out.append(neededBy.getName() + " " + neededBy.getVersion() + "\n");
            }
            out.append("Exported Packages:\n");
            for (Package exportedPackage : plugin.getExportedPackages()) {
                String sep = exportedPackage.getVersion().isEmpty()? "" : " ";
                out.append("\t" + exportedPackage.getName() + sep
                        + exportedPackage.getVersion());
                if (exportedPackage.getReexportedBy().contains(plugin)) {
                    out.append(" *Reexport*");
                }
                out.append("\n");
            }
            if (plugin.isFragment()) {
                out.append("Fragment-Host:\n");
                Plugin fragmentHost = plugin.getFragHost();
                out.append(fragmentHost.getName() + " " + fragmentHost.getVersion()
                        + "\n");
            } else {
                out.append("Fragments:\n");
                for (Plugin fragment : plugin.getFragments()) {
                    out.append("\t" + fragment.getName() + " " + fragment.getVersion()
                            + "\n");
                }
            }
            Logging.writeStandardOut(out.toString());
        }
        for (Feature feature : foundFeatures) {
            StringBuilder out = new StringBuilder();
            out.append("Feature: " + feature.getName() + " " + feature.getVersion()
                    + "\n");
            out.append("Included Features:\n");
            for (Feature included : feature.getIncludedFeatures()) {
                out.append("\t" + included.getName() + " " + included.getVersion() + "\n");
            }
            out.append("Included Plugins:\n");
            for (Plugin included : feature.getResolvedPlugins()) {
                out.append("\t");
                out.append(included.isFragment() ? "Fragment: " : "Plugin: ");
                out.append(included.getName() + " " + included.getVersion() + "\n");
            }
            out.append("Included in Features:\n");
            for (Feature includedIn : feature.getIncludedInFeatures()) {
                out.append("\t" + includedIn.getName() + " " + includedIn.getVersion()
                        + "\n");
            }
            Logging.writeStandardOut(out.toString());
        }

    }

    private static void printAllPluginsAndFeatures() {
        StringBuilder out = new StringBuilder();
        List<Plugin> plugins = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        List<Plugin> fragments = new ArrayList<>();

        for (Plugin plugin : pluginSet) {
            if (plugin.isFragment()) {
                fragments.add(plugin);
            } else {
                plugins.add(plugin);
            }
        }
        features.addAll(featureSet);

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

        out.append("Features:\n");
        for (Feature feature : features) {
            out.append("\t" + feature.getInformationLine() + "\n");
        }
        out.append("Plugins:\n");
        for (Plugin plugin : plugins) {
            out.append("\t" + plugin.getInformationLine() + "\n");
        }
        out.append("Fragments:\n");
        for (Plugin fragment : fragments) {
            out.append("\t" + fragment.getInformationLine() + "\n");
        }
        Logging.writeStandardOut(out.toString());
    }
}
