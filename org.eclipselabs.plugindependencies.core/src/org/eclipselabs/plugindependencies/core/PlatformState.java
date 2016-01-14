/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import org.eclipselabs.plugindependencies.core.DependencyResolver.PluginElt;

/**
 */
public class PlatformState {

    private final static String DEFAULT_JAVA_HOME = System.getProperty("java.home");

    public static PlatformSpecs UNDEFINED_SPECS = new PlatformSpecs(null, null, null);

    private Set<Plugin> plugins;
    private Set<Package> packages;
    private Set<Feature> features;
    private final Map<String, List<Package>> nameToPackages;
    private final Map<String, List<Plugin>> nameToPlugins;
    private final Map<String, List<Feature>> nameToFeatures;
    private String javaHome;
    private boolean dependenciesresolved;
    private static String dummyVersion;
    private static String realVersion = NamedElement.ZERO_VERSION;

    private final Set<ManifestEntry> hiddenElements;

    private PlatformSpecs platformSpecs;

    private boolean validated;

    /**
     *
     */
    public PlatformState() {
        this(new LinkedHashSet<Plugin>(), new LinkedHashSet<Package>(), new LinkedHashSet<Feature>());
    }

    public PlatformState(Set<Plugin> plugins, Set<Package> packages, Set<Feature> features) {
        hiddenElements = new LinkedHashSet<>();
        platformSpecs = new PlatformSpecs(null, null, null);
        this.plugins = plugins == null? new LinkedHashSet<Plugin>() : plugins;
        this.packages = packages == null? new LinkedHashSet<Package>() : packages;
        this.features = features == null? new LinkedHashSet<Feature>() : features;
        nameToPackages = new LinkedHashMap<>();
        nameToPlugins = new LinkedHashMap<>();
        nameToFeatures = new LinkedHashMap<>();

        javaHome = DEFAULT_JAVA_HOME;

        if(!this.plugins.isEmpty()){
            for (Plugin plugin : this.plugins) {
                addPlugin(plugin);
            }
        }
        if(!this.packages.isEmpty()){
            for (Package pack : this.packages) {
                addPackage(pack);
            }
        }
        if(!this.features.isEmpty()){
            for (Feature feature : this.features) {
                addFeature(feature);
            }
        }
    }

    public Set<Plugin> getPlugins(){
        return plugins;
    }

    public Set<Package> getPackages(){
        return packages;
    }

    public Set<Feature> getFeatures(){
        return features;
    }

    String getJavaHome() {
        return javaHome;
    }

    public void hideElement(ManifestEntry elt){
        if(dependenciesresolved || !plugins.isEmpty() || ! features.isEmpty()){
            throw new IllegalStateException("Can't change already existing state");
        }
        hiddenElements.add(elt);
    }

    void setJavaHome(String newHome) {
        if (newHome == null || newHome.trim().isEmpty()) {
            javaHome = DEFAULT_JAVA_HOME;
        } else {
            File javaHomeLib = new File(newHome + "/lib");
            if (javaHomeLib.isDirectory()) {
                javaHome = newHome;
            } else {
                throw new IllegalArgumentException("specified $JAVA_HOME (" + newHome + ") does not exist. Changing to " + DEFAULT_JAVA_HOME);
            }
        }
    }

    public Plugin addPlugin(Plugin newOne){
        newOne = checkIfHidden(newOne);
        if(newOne == Plugin.DUMMY_PLUGIN){
            return newOne;
        }
        plugins.add(newOne);

        List<Plugin> list = nameToPlugins.get(newOne.getName());
        if(list == null){
            list = new ArrayList<>();
            nameToPlugins.put(newOne.getName(), list);
        }
        int existing = list.indexOf(newOne);
        Plugin oldOne = null;
        if(existing >= 0){
            oldOne = list.get(existing);
            if(Objects.equals(oldOne.getPath(), newOne.getPath())) {
                return oldOne;
            }
            oldOne.addDuplicate(newOne);
        }
        list.add(newOne);
        for (Package exportedPackage : newOne.getExportedPackages()) {
            /*
             * Package is exported by another plugin, package has to be found in packages
             * and plugin must be added to exportPlugins of package
             */
            addPackage(exportedPackage).addExportPlugin(newOne);
        }
        return oldOne != null? oldOne : newOne;
    }

    private Plugin checkIfHidden(Plugin newOne) {
        if(!hiddenElements.isEmpty()){
            for (ManifestEntry hidden : hiddenElements) {
                if(hidden.hasDefaultVersion()){
                    if(hidden.isMatching(newOne)){
                        return Plugin.DUMMY_PLUGIN;
                    }
                } else {
                    if(hidden.exactMatch(newOne)){
                        return Plugin.DUMMY_PLUGIN;
                    }
                }
            }
        }
        return newOne;
    }

    private Feature checkIfHidden(Feature newOne) {
        if(!hiddenElements.isEmpty()){
            for (ManifestEntry hidden : hiddenElements) {
                if(hidden.hasDefaultVersion()){
                    if(hidden.isMatching(newOne)){
                        return Feature.DUMMY_FEATURE;
                    }
                } else {
                    if(hidden.exactMatch(newOne)){
                        return Feature.DUMMY_FEATURE;
                    }
                }
            }
        }
        return newOne;
    }

    public Package addPackage(Package newOne){
        if(!packages.contains(newOne)){
            packages.add(newOne);
        }

        List<Package> list = nameToPackages.get(newOne.getName());
        if(list == null){
            list = new ArrayList<>();
            nameToPackages.put(newOne.getName(), list);
        }
        int existing = list.indexOf(newOne);
        if(existing >= 0){
            return list.get(existing);
        }
        list.add(newOne);
        return newOne;
    }

    public Feature addFeature(Feature newOne){
        newOne = checkIfHidden(newOne);
        if(newOne == Feature.DUMMY_FEATURE){
            return newOne;
        }
        features.add(newOne);

        List<Feature> list = nameToFeatures.get(newOne.getName());
        if(list == null){
            list = new ArrayList<>();
            nameToFeatures.put(newOne.getName(), list);
        }
        int existing = list.indexOf(newOne);
        Feature oldOne = null;
        if(existing >= 0){
            oldOne = list.get(existing);
            oldOne.addDuplicate(newOne);
        }
        list.add(newOne);
        return oldOne != null? oldOne : newOne;
    }

    public Set<Plugin> getPlugins(String name){
        List<Plugin> list = nameToPlugins.get(name);
        if(list == null) {
            if (plugins.isEmpty()) {
                return Collections.emptySet();
            }
            // For tests only
            return Collections.unmodifiableSet(plugins);
        }
        // XXX???
        return new LinkedHashSet<>(list);
    }

    public Set<Package> getPackages(String name){
        List<Package> list = nameToPackages.get(name);
        if(list == null) {
            if (packages.isEmpty()) {
                return Collections.emptySet();
            }
            // For tests only
            return Collections.unmodifiableSet(packages);
        }
        // XXX???
        return new LinkedHashSet<>(list);
    }

    public Set<Feature> getFeatures(String name){
        List<Feature> list = nameToFeatures.get(name);
        if(list == null) {
            if (packages.isEmpty()) {
                return Collections.emptySet();
            }
            // For tests only
            return Collections.unmodifiableSet(features);
        }
        // XXX???
        return new LinkedHashSet<>(list);
    }

    public Package getPackage(String name){
        List<Package> list = nameToPackages.get(name);
        if(list == null || list.isEmpty() || list.size() > 1) {
            return null;
        }
        return list.get(0);
    }

    public Package createPackage(ManifestEntry entry){
        return createPackage(entry.getName(), entry.getVersion());
    }

    public Package createPackage(String name, String version) {
        Package pack = new Package(name, version);
        pack = addPackage(pack);
        return pack;
    }

    public Plugin getPlugin(String name){
        List<Plugin> list = nameToPlugins.get(name);
        if(list == null || list.isEmpty() || list.size() > 1) {
            return null;
        }
        return list.get(0);
    }

    public void computeAllDependenciesRecursive() {
        if(!dependenciesresolved){
            resolveDependencies();
        }
        for (Plugin plugin : plugins) {
            computeAllDependenciesRecursive(plugin);
        }
        validate();
    }

   public void validate() {
       if(validated){
           return;
       }
       validated = true;
       // validate same package contributed by different plugins in same dependency chain
       for (Package pack : packages) {
           if(pack.getExportedBy().size() > 1){
               Set<Plugin> exportedBy = new HashSet<>(pack.getExportedBy());

               Iterator<Plugin> iterator = exportedBy.iterator();
               Set<Plugin> toRemove = new HashSet<>();
               while (iterator.hasNext()) {
                   Plugin p1 = iterator.next();
                   for (Plugin p2 : exportedBy) {
                       // ignore packages from same plugin with different version
                       // ignore packages from fragments and hosts
                       if(p1 != p2 && (p1.getName().equals(p2.getName()) || p1.isFragmentOrHost(p2))){
                           toRemove.add(p2);
                           toRemove.add(p1);
                       }
                   }
               }
               exportedBy.removeAll(toRemove);

               // exclude all plugins which might have dependencies to each other
               Map<Plugin, Plugin> required = new HashMap<>();
               for (Plugin plugin : exportedBy) {
                   Set<Plugin> resolvedPlugins = plugin.getRecursiveResolvedPlugins();
                   for (Plugin p : resolvedPlugins) {
                       // only exclude if there is no cycle
                       if(!p.containsRecursiveResolved(plugin)){
                           required.put(p, plugin);
                       }
                   }
               }
               for (Entry<Plugin, Plugin> entry : required.entrySet()) {
                   Plugin p1 = entry.getKey();
                   if(exportedBy.contains(p1)){
                       exportedBy.remove(p1);
                       exportedBy.remove(entry.getValue());
                   }
               }

               if(exportedBy.size() > 1){
                   pack.addWarningToLog("package contributed by multiple, not related plugins");
                   for (Plugin plugin : exportedBy) {
                       plugin.addWarningToLog("this plugin is one of " + exportedBy.size() + " plugins contributing package '" + pack.getNameAndVersion() + "'");
                   }

                   Set<Plugin> importedBy = pack.getImportedBy();
                   for (Plugin plugin : importedBy) {
                       plugin.addWarningToLog("this plugin uses package '" + pack.getNameAndVersion() + "' contributed by multiple plugins");
                   }
               }
           }
       }
       for (Plugin plugin : plugins) {
           List<OSGIElement> dups = plugin.getDuplicates();
           if(!dups.isEmpty()){
               logDuplicates(plugin, dups);
           }
       }
       for (Feature feature : features) {
           List<OSGIElement> dups = feature.getDuplicates();
           if(!dups.isEmpty()){
               logDuplicates(feature, dups);
           }
       }
       // TODO validate packages with different versions used by different plugins in same dependency chain
       // TODO validate singleton plugins with different versions used by different plugins in same dependency chain
   }

   private static void logDuplicates(OSGIElement plugin, List<OSGIElement> dups) {
       StringBuilder sb = new StringBuilder();
       sb.append((dups.size() + 1));
       if(plugin instanceof Feature){
           sb.append(" features ");
       } else {
           sb.append(" plugins ");
       }
       sb.append("with equal symbolic name and version, located at:\n\t").append(plugin.getPath());
       for (OSGIElement elt : dups) {
           sb.append("\n\t").append(elt.getPath());
       }
       plugin.addErrorToLog(sb.toString());
   }

    static Set<Plugin> computeAllDependenciesRecursive(final Plugin root) {
        if(root.isRecursiveResolved()){
            return root.getRecursiveResolvedPlugins();
        }
        Stack<PluginElt> stack = new Stack<>();
        stack.add(new PluginElt(null, root));
        while (!stack.isEmpty()){
            PluginElt current = stack.peek();
            PluginElt next = current.next();

            if(next == PluginElt.EMPTY) {
                stack.pop();
                // make sure we finished the iteration and replace the default empty set if no dependencies found
                current.setResolved();
                continue;
            }

            if(stack.contains(next)){
                if(!next.plugin.isRecursiveResolved()){
                    Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                    for (Plugin p : resolvedPlugins) {
                        current.resolved(p);
                    }
                }
                // avoid cyclic dependencies
                continue;
            }

            if(root.containsRecursiveResolved(next.plugin)) {
                Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                for (Plugin p : resolvedPlugins) {
                    current.resolved(p);
                }
                current.resolved(next.plugin);
            } else {
                stack.push(next);
            }
        }
        Set<Plugin> rrp = root.getRecursiveResolvedPlugins();
        for (Plugin plugin : rrp) {
            if(!plugin.isRecursiveResolved()){
                if(plugin.isFragment()){
                    plugin.setResolved();
                }
            }
            if(!plugin.isRecursiveResolved()) {
                throw new IllegalStateException("Unable to resolve: " + plugin);
            }
        }
        return rrp;
    }

    public DependencyResolver resolveDependencies() {
        DependencyResolver depres = new DependencyResolver(this);

        for (Plugin plugin : getPlugins()) {
            depres.resolvePluginDependency(plugin);
        }
        for (Feature feature : getFeatures()) {
            depres.resolveFeatureDependency(feature);
        }

        for (Plugin plugin : getPlugins()) {
            plugin.parsingDone();
        }
        for (Feature feature : getFeatures()) {
            feature.parsingDone();
        }
        for (Package pack : getPackages()) {
            pack.parsingDone();
        }
        packages = Collections.unmodifiableSet(packages);
        plugins = Collections.unmodifiableSet(plugins);
        features = Collections.unmodifiableSet(features);
        dependenciesresolved = true;
        return depres;
    }

    static String fixName(String name) {
        name = name.trim();
        if ("system.bundle".equals(name)) {
            return "org.eclipse.osgi";
        }
        return name;
    }

    static String fixVersion(String version) {
        version = version.trim();
        if(realVersion == null || dummyVersion == null){
            return version;
        }
        if (dummyVersion.equals(version)) {
            return realVersion;
        }
        return version;
    }

    public static String getDummyBundleVersion() {
        return dummyVersion;
    }

    public static void setDummyBundleVersion(String dummyBundleVersion) {
        dummyVersion = dummyBundleVersion;
    }

    public static String getBundleVersionForDummy() {
        return realVersion;
    }

    public static void setBundleVersionForDummy(String realBundleVersion) {
        realVersion = realBundleVersion;
    }

    public PlatformSpecs getPlatformSpecs() {
        return platformSpecs;
    }

    public void setPlatformSpecs(PlatformSpecs platformSpecs) {
        this.platformSpecs = platformSpecs;
    }

    public StringBuilder dumpAllPluginsAndFeatures() {
        StringBuilder out = new StringBuilder();
        List<Plugin> plugins1 = new ArrayList<>();
        List<Feature> features1 = new ArrayList<>();
        List<Plugin> fragments = new ArrayList<>();

        for (Plugin plugin : getPlugins()) {
            if (plugin.isFragment()) {
                fragments.add(plugin);
            } else {
                plugins1.add(plugin);
            }
        }
        features1.addAll(getFeatures());

        Comparator<OSGIElement> comp = new NameAndVersionComparator();

        Collections.sort(plugins1, comp);
        Collections.sort(features1, comp);
        Collections.sort(fragments, comp);

        out.append("features:\n");
        for (Feature feature : features1) {
            out.append("\t" + feature.getInformationLine() + "\n");
        }
        out.append("plugins:\n");
        for (Plugin plugin : plugins1) {
            out.append("\t" + plugin.getInformationLine() + "\n");
        }
        out.append("fragments:\n");
        for (Plugin fragment : fragments) {
            out.append("\t" + fragment.getInformationLine() + "\n");
        }
        return out;
    }


    public void dumpAllElements(PrintWriter pw) {
        List<Plugin> plugins1 = new ArrayList<>();
        List<Feature> features1 = new ArrayList<>();
        List<Plugin> fragments = new ArrayList<>();

        for (Plugin plugin : getPlugins()) {
            if (plugin.isFragment()) {
                fragments.add(plugin);
            } else {
                plugins1.add(plugin);
            }
        }
        features1.addAll(getFeatures());

        Comparator<OSGIElement> comp = new NameAndVersionComparator();

        Collections.sort(plugins1, comp);
        Collections.sort(features1, comp);
        Collections.sort(fragments, comp);

        List<Package> packs = new ArrayList<>(getPackages());
        Comparator<NamedElement> comp1 = new NamedElement.NameComparator();

        Collections.sort(packs, comp1);

        pw.println("features:");
        for (Feature feature : features1) {
            pw.println("\t" + feature.getInformationLine());
        }
        pw.println("plugins:");
        for (Plugin plugin : plugins1) {
            pw.println("\t" + plugin.getInformationLine());
        }
        pw.println("fragments:");
        for (Plugin fragment : fragments) {
            pw.println("\t" + fragment.getInformationLine());
        }

        pw.println("packages:");
        for (Package pack : packs) {
            pw.print("\t" + pack.getInformationLine());
        }

        pw.println("-----------------------------------------------");
        pw.println("---            all dependencies             ---");
        pw.println("-----------------------------------------------");

        pw.println("features:");
        for (Feature feature : features1) {
            pw.println(feature.dump());
        }
        pw.println("plugins:");
        for (Plugin plugin : plugins1) {
            pw.println(plugin.dump());
        }
        pw.println("fragments:");
        for (Plugin fragment : fragments) {
            pw.println(fragment.dump());
        }

    }

    public StringBuilder dumpLogs() {
        validate();
        StringBuilder out = new StringBuilder();

        out.append("Platform state:\n");
        out.append("Features:\n");
        out.append(CommandLineInterpreter.printLogs(getFeatures(), true));
        out.append("Plugins:\n");
        out.append(CommandLineInterpreter.printLogs(getPlugins(), true));
        out.append("Packages:\n");
        out.append(CommandLineInterpreter.printPackageLogs(getPackages(), true));
        return out;
    }

    public static class PlatformSpecs {
        public final String os;
        public final String ws;
        public final String arch;

        public PlatformSpecs(String os, String ws, String arch) {
            super();
            this.os = os;
            this.ws = ws;
            this.arch = arch;
        }

        public boolean matches(PlatformSpecs p) {
            if (this == p) {
                return true;
            }
            if (p == null) {
                return true;
            }
            if (arch != null && p.arch != null && !arch.equals(p.arch)) {
                return false;
            }
            if (os != null && p.os != null && !os.equals(p.os)) {
                return false;
            }
            if (ws != null && p.ws != null && !ws.equals(p.ws)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("PlatformSpecs [");
            if (os != null) {
                builder.append("os=");
                builder.append(os);
                builder.append(", ");
            }
            if (ws != null) {
                builder.append("ws=");
                builder.append(ws);
                builder.append(", ");
            }
            if (arch != null) {
                builder.append("arch=");
                builder.append(arch);
            }
            builder.append("]");
            return builder.toString();
        }


    }

    public static final class NameAndVersionComparator implements Comparator<OSGIElement> {
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
    }
}
