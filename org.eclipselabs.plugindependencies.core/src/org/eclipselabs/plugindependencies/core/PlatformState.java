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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Set<String> ignoredBundlesWithCycles;

    private PlatformSpecs platformSpecs;

    private boolean validated;


    static Map<File, Set<String>> knownSystemPackages = new HashMap<>();

    /**
     *
     */
    public PlatformState() {
        this(new LinkedHashSet<Plugin>(), new LinkedHashSet<Package>(), new LinkedHashSet<Feature>());
    }

    public PlatformState(Set<Plugin> plugins, Set<Package> packages, Set<Feature> features) {
        hiddenElements = new LinkedHashSet<>();
        platformSpecs = new PlatformSpecs(null, null, null);
        this.plugins = plugins == null? new LinkedHashSet<>() : plugins;
        this.packages = packages == null? new LinkedHashSet<>() : packages;
        this.features = features == null? new LinkedHashSet<>() : features;
        nameToPackages = new LinkedHashMap<>();
        nameToPlugins = new LinkedHashMap<>();
        nameToFeatures = new LinkedHashMap<>();
        ignoredBundlesWithCycles = new LinkedHashSet<>();

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

    public List<Problem> computeAllDependenciesRecursive() {
        if(!dependenciesresolved){
            resolveDependencies();
        }
        for (Plugin plugin : plugins) {
            computeAllDependenciesRecursive(plugin);
        }
        return validate();
    }

    public List<Problem> validate() {
        if(validated){
            return collectErrors();
        }
        validated = true;
        // validate same package contributed by different plugins in same dependency chain
        for (Package pack : packages) {
            if(pack.getExportedBy().size() > 1){
                Set<Plugin> exportedBy = new HashSet<>(pack.getExportedBy());

                Iterator<Plugin> exportedByIter = exportedBy.iterator();
                Set<Plugin> toRemove = new HashSet<>();
                while (exportedByIter.hasNext()) {
                    Plugin p1 = exportedByIter.next();
                    if(pack.getSplit().contains(p1)){
                        exportedByIter.remove();
                        continue;
                    }
                    // plugins which import and export same package are most likely
                    // just forwarding that dependency to clients
                    if(p1.getImportedPackages().contains(pack)){
                        exportedByIter.remove();
                        continue;
                    }

                    // plugins which exports a package already reexported by re-exporting required bundle is most likely
                    // just forwarding that dependency to clients
                    if (p1.getReExportedPackages().contains(pack)) {
                        exportedByIter.remove();
                        continue;
                    }

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

                if(exportedBy.size() > 1){
                    pack.addWarningToLog("package contributed by multiple, not related plugins", exportedBy);
                    for (Plugin plugin : exportedBy) {
                        plugin.addWarningToLog("this plugin is one of " + exportedBy.size() + " plugins contributing package '" + pack.getNameAndVersion() + "'", pack);
                    }

                    Set<Plugin> importedBy = pack.getImportedBy();
                    for (Plugin plugin : importedBy) {
                        plugin.addWarningToLog("this plugin uses package '" + pack.getNameAndVersion() + "' contributed by multiple plugins", pack);
                    }
                }
            }
        }
        for (Plugin plugin : plugins) {
            List<OSGIElement> dups = plugin.getDuplicates();
            if(!dups.isEmpty()){
                if(!hasOnlyWorkspaceDup(dups)) {
                    logDuplicates(plugin, dups);
                }
            }
        }
        for (Feature feature : features) {
            List<OSGIElement> dups = feature.getDuplicates();
            if(!dups.isEmpty()){
                if(!hasOnlyWorkspaceDup(dups)) {
                    logDuplicates(feature, dups);
                }
            }
        }
        // TODO validate packages with different versions used by different plugins in same dependency chain
        // TODO validate singleton plugins with different versions used by different plugins in same dependency chain
        List<Problem> errors = collectErrors();
        return errors;
    }

    private List<Problem> collectErrors() {
        List<Problem> errors = new ArrayList<>();
        Consumer<? super Problem> collectErrors = x -> {
            if (x.isError()) {
                errors.add(x);
            }
        };
        for (Plugin plugin : plugins) {
            plugin.getLog().forEach(collectErrors);
        }
        for (Feature feature : features) {
            feature.getLog().forEach(collectErrors);
        }
        return errors;
    }

    private static boolean hasOnlyWorkspaceDup(List<OSGIElement> dups) {
        if(dups.size() != 2) {
            return false;
        }
        return dups.stream().anyMatch(x -> x.isFromWorkspace());
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
        plugin.addErrorToLog(sb.toString(), dups);
    }

    Set<Plugin> computeAllDependenciesRecursive(final Plugin root) {
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
                current.setResolved(this);
                continue;
            }

            if(stack.contains(next)){
                if(!next.plugin.isRecursiveResolved()){
                    Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                    for (Plugin p : resolvedPlugins) {
                        current.resolved(p, this);
                    }
                }
                // avoid (but report) cyclic dependencies
                if(current.plugin != next.plugin.getHost()
                        && next.plugin != current.plugin.getHost()) {
                    if(getIgnoredBundlesWithCycles().contains(next.plugin.getName())) {
                        next.plugin.addWarningToLog("Dependency cycle detected with " + current.plugin.getNameAndVersion(), next.toVisit);
                    } else {
                        next.plugin.addErrorToLog("Dependency cycle detected with " + current.plugin.getNameAndVersion(), next.toVisit);
                    }
                }
                continue;
            }

            if(root.containsRecursiveResolved(next.plugin)) {
                Set<Plugin> resolvedPlugins = next.plugin.getRecursiveResolvedPlugins();
                for (Plugin p : resolvedPlugins) {
                    current.resolved(p, this);
                }
                current.resolved(next.plugin, this);
            } else {
                stack.push(next);
            }
        }
        Set<Plugin> rrp = root.getRecursiveResolvedPlugins();
        for (Plugin plugin : rrp) {
            if(!plugin.isRecursiveResolved()){
                if(plugin.isFragment()){
                    plugin.setResolved(this);
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

    public Set<String> checkJrtFsPackages(File jar) {
        if (!jar.getName().equals("jrt-fs.jar")) {
            return Collections.emptySet();
        }
        Set<String> set = knownSystemPackages.get(jar);
        if (set != null) {
            return set;
        }
        try(URLClassLoader loader = new URLClassLoader(new URL[] { jar.toPath().toUri().toURL() })) {
            FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), Collections.emptyMap(), loader);
            Path top = fs.getPath("/packages");
            Stream<Path> list = Files.list(top).filter(Files::isDirectory).map(n -> n.getFileName()).filter(n -> n != null);
            set = list.map(p -> p.toString()).filter(n -> n.indexOf('.') > 0).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
            set = Collections.unmodifiableSet(set);
            knownSystemPackages.put(jar, set);
            return set;
        } catch (IOException e) {
            Logging.getLogger().error(" failed to read system packages from '" + jar + "'.", e);
            return Collections.emptySet();
        }
    }

    public Set<String> checkRtPackages(File jar) {
        if (!jar.getName().equals("rt.jar")) {
            return Collections.emptySet();
        }
        Set<String> set = knownSystemPackages.get(jar);
        if (set != null) {
            return set;
        }
        try (JarFile jarfile = new JarFile(jar)) {
            Stream<String> stream = jarfile.stream().map(e -> e.getName());
            stream = stream.filter(e -> e.endsWith(".class")).map(e -> e.substring(0, e.lastIndexOf('/')));
            stream = stream.map(e -> e.replace('/', '.'));
            set = stream.sorted().collect(Collectors.toCollection(LinkedHashSet::new));
            set = Collections.unmodifiableSet(set);
            knownSystemPackages.put(jar, set);
            return set;
        } catch (IOException e) {
            Logging.getLogger().error(" failed to read system packages from '" + jar + "'.", e);
            return Collections.emptySet();
        }
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

    public Set<String> getIgnoredBundlesWithCycles() {
        return ignoredBundlesWithCycles;
    }

    public void setIgnoredBundlesWithCycles(Set<String> ignoredBundlesWithCycles) {
        Objects.requireNonNull(ignoredBundlesWithCycles);
        this.ignoredBundlesWithCycles = ignoredBundlesWithCycles;
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
