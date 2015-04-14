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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipselabs.plugindependencies.core.DependencyResolver.PluginElt;

/**
 */
public class PlatformState {

    private final Set<Plugin> plugins;
    private final Set<Package> packages;
    private final Set<Feature> features;
    private final Map<String, List<Package>> nameToPackages;
    private final Map<String, List<Plugin>> nameToPlugins;
    private final Map<String, List<Feature>> nameToFeatures;
    private String javaHome;
    private boolean dependenciesresolved;

    /**
     *
     */
    public PlatformState() {
        this(new LinkedHashSet<Plugin>(), new LinkedHashSet<Package>(), new LinkedHashSet<Feature>());
    }

    public PlatformState(Set<Plugin> plugins, Set<Package> packages, Set<Feature> features) {
        this.plugins = plugins == null? new LinkedHashSet<Plugin>() : plugins;
        this.packages = packages == null? new LinkedHashSet<Package>() : packages;
        this.features = features == null? new LinkedHashSet<Feature>() : features;
        nameToPackages = new LinkedHashMap<>();
        nameToPlugins = new LinkedHashMap<>();
        nameToFeatures = new LinkedHashMap<>();

        javaHome = "";

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
        return Collections.unmodifiableSet(plugins);
    }

    public Set<Package> getPackages(){
        return Collections.unmodifiableSet(packages);
    }

    public Set<Feature> getFeatures(){
        return Collections.unmodifiableSet(features);
    }

    String getJavaHome() {
        return javaHome;
    }

    void setJavaHome(String newHome) {
        javaHome = newHome;
    }

    public Plugin addPlugin(Plugin newOne){
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

    public Package addPackage(Package newOne){
        packages.add(newOne);

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
        features.add(newOne);

        List<Feature> list = nameToFeatures.get(newOne.getName());
        if(list == null){
            list = new ArrayList<>();
            nameToFeatures.put(newOne.getName(), list);
        }
        int existing = list.indexOf(newOne);
        if(existing >= 0){
            return list.get(existing);
        }
        list.add(newOne);
        return newOne;
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
       // validate same package contributed by different plugins in same dependency chain
       for (Package pack : packages) {
           if(pack.getExportedBy().size() > 1){
               pack.addWarningToLog("package contributed by multiple plugins");
               Set<Plugin> exportedBy = pack.getExportedBy();
               for (Plugin plugin : exportedBy) {
                   plugin.addWarningToLog("this plugin is one of " + exportedBy.size() + " plugins contributing package '" + pack.getNameAndVersion() + "'");
               }
               Set<Plugin> importedBy = pack.getImportedBy();
               for (Plugin plugin : importedBy) {
                   plugin.addWarningToLog("this plugin uses package '" + pack.getNameAndVersion() + "' contributed by multiple plugins");
               }
           }
       }
       // TODO validate packages with different versions used by different plugins in same dependency chain
       // TODO validate singleton plugins with different versions used by different plugins in same dependency chain
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
        dependenciesresolved = true;
        return depres;
    }


}
