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

import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

enum Options {

    Providing("-providing") {
        @Override
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            for (String arg : args) {
                cli.printProvidingPackage(arg);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            boolean showWarnings = args != null && args.length >0 && args[0].equals("w") ? true : false;
            Logging.writeStandardOut(CommandLineInterpreter.printUnresolvedDependencies(state.getPluginSet(),
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            boolean showWarnings = args != null && args.length >0 && args[0].equals("w") ? true : false;
            Logging.writeStandardOut(CommandLineInterpreter.printUnresolvedDependencies(state.getFeatureSet(),
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            for (String arg : args) {
                cli.printDependingOnPlugin(arg);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            for (String arg : args) {
                cli.printDependingOnPackage(arg);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            for (String arg : args) {
                int result = cli.generateRequirementsFile(arg, state.getPluginSet());
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            if(args.length > 1) {
                OutputCreator.setTargetFolder(args[1]);
            }
            return cli.generateBuildFile(args[0]);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            if(args.length > 1) {
                OutputCreator.setTargetFolder(args[1]);
            }
            return cli.generateAllBuildFiles(args[0]);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            CommandLineInterpreter.printHelpPage();
            return 0;
        }

        @Override
        void printHelp(String arg) {
            // DO NOTHING
        }
    },

    FullLogFile("-fullLog") {
        @Override
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            return cli.writeErrorLogFile(args[0]);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            try {
                for (String arg : args) {
                    int result = cli.readInEclipseFolder(arg);
                    if(result != 0){
                        return result;
                    }
                }
                state.resolveDependencies();
                return 0;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                Logging.getLogger().error("failed to read from: " + Arrays.toString(args), e);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            state.setJavaHome(args[0]);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            try {
                OutputCreator.setEclipseRoot(args[0]);
            } catch (IOException e) {
                Logging.getLogger().error(" failed to resolve deployment root: " + Arrays.toString(args), e);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            for (String arg : args) {
                cli.printFocusedOSGIElement(arg);
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
        int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
            cli.printAllPluginsAndFeatures();
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

    int handle(PlatformState state, CommandLineInterpreter cli, String... args) {
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
