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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

enum Options {

    Providing("-providing", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
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

    DependOnPlugin("-dependOnPlugin", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
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

    DependOnPackage("-dependOnPackage", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
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

    GenerateRequirementsFile("-generateReqFile", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            for (String arg : args) {
                int result = cli.generateRequirementsFile(arg);
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

    GenerateBuildFile("-generateBuildFile", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            if(args.size() > 1) {
                OutputCreator.setTargetFolder(args.get(1));
            }
            return cli.generateBuildFile(args.get(0));
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

    GenerateAllBuildFiles("-generateAllBuild", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            if(args.size() > 1) {
                OutputCreator.setTargetFolder(args.get(1));
            }
            return cli.generateAllBuildFiles(args.get(0));
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

    Help("-h", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            CommandLineInterpreter.printHelpPage();
            return 0;
        }

        @Override
        void printHelp(String arg) {
            // DO NOTHING
        }
    },

    FullLogFile("-fullLog", false) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            cli.setFullLog(args.size() > 0? args.get(0) : "");
            return 0;
        }

        @Override
        void printHelp(String arg) {
            String help = "-fullLog [file]"
                    + "\t\t\t"
                    + "Writes the full error log to the specified file (optional, if no file given, to standard out)."
                    + " Warnings are included.";
            Logging.writeStandardOut(help);
        }
    },

    EclipsePaths("-eclipsePaths", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            try {
                args = resolveAndSkipDuplicates(args);
                for (String arg : args) {
                    int result = cli.readInEclipseFolder(arg);
                    if(result != 0){
                        return result;
                    }
                }
                cli.getState().resolveDependencies();
                return 0;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                Logging.getLogger().error("failed to read from: " + args, e);
            }
            return -1;
        }

        @Override
        void printHelp(String arg) {
            String help = "-eclipsePaths folder1 [folder2 ...]"
                    + "\t"
                    + "Eclipse target platform plugin and feature folders are specified here (canonical paths)."
                    + " Either Eclipse folder with subfolders \"plugins\" and \"features\" "
                    + "or a folder just containing plugins is possible.";
            Logging.writeStandardOut(help);
        }
    },

    JavaHome("-javaHome", false) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            try {
                cli.getState().setJavaHome(args.get(0));
            } catch (IllegalArgumentException e) {
                Logging.getLogger().error(e.getMessage(), e);
                return -1;
            }
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

    EclipseRoot("-deploymentRoot", false) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            try {
                OutputCreator.setEclipseRoot(args.get(0));
            } catch (IOException e) {
                Logging.getLogger().error(" failed to resolve deployment root: " + args, e);
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

    BundleVersion("-bundleVersion", false) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            cli.setBundleVersionForDummy(args.get(0));
            if(args.size() > 1) {
                cli.setBundleVersionDummy(args.get(1));
            }
            return 0;
        }

        @Override
        void printHelp(String arg) {
            String help = "-bundleVersion version [dummyVersion]" + "\t\t\t"
                    + "Changes the bundle version of source plugins to the specified version."
                    + " Default is 0.0.0. If 'dummyVersion' is given, replaces this version with 'version'.";
            Logging.writeStandardOut(help);
        }
    },

    PlatformSpecs("-platform", false) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
            if(args.size() != 3) {
                String message = "Platform requires 3 arguments: OS, WS, ARCH";
                Logging.getLogger().error(message);
                return -1;
            }
            cli.setPlatformSpecs(new PlatformState.PlatformSpecs(args.get(0), args.get(1), args.get(2)));
            return 0;
        }

        @Override
        void printHelp(String arg) {
            String help = "-platform os ws arch" + "\t\t\t"
                    + "Changes the platform OS/WS/ARCH to the specified values."
                    + " Default is unset.";
            Logging.writeStandardOut(help);
        }
    },

    Focus("-focus", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
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

    PrintAllPluginsAndFeatures("-printAll", true) {
        @Override
        int handle(CommandLineInterpreter cli, List<String> args) {
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

    UNKNOWN("", false);

    private final String optionName;
    private final boolean command;

    Options(String name, boolean command) {
        optionName = name;
        this.command = command;
    }

    static List<String> resolveAndSkipDuplicates(List<String> paths) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String string : paths) {
            try {
                resolved.add(new File(string).getCanonicalPath());
            } catch (IOException e) {
                Logging.getLogger().error(e.getMessage(), e);
            }
        }
        return new ArrayList<>(resolved);
    }

    boolean isCommand(){
        return command;
    }

    int handle(CommandLineInterpreter cli, List<String> args) {
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
