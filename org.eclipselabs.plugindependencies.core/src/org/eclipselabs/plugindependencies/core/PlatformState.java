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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipselabs.plugindependencies.core.DependencyResolver.PluginElt;

/**
 */
public class PlatformState {

    private final Set<Plugin> pluginSet;
    private final Set<Package> packageSet;
    private final Set<Feature> featureSet;
    private final Map<String, List<Package>> nameToPackages;
    private String javaHome;

    /**
     *
     */
    public PlatformState() {
        this(new LinkedHashSet<Plugin>(), new LinkedHashSet<Package>(), new LinkedHashSet<Feature>());
    }

    public PlatformState(Set<Plugin> pluginSet, Set<Package> packageSet, Set<Feature> featureSet) {
        this.pluginSet = pluginSet;
        this.packageSet = packageSet;
        this.featureSet = featureSet;
        nameToPackages = new HashMap<>();
        javaHome = "";
    }

    public Set<Plugin> getPluginSet(){
        return pluginSet;
    }

    public Set<Package> getPackageSet(){
        return packageSet;
    }

    public Set<Feature> getFeatureSet(){
        return featureSet;
    }

    String getJavaHome() {
        return javaHome;
    }

    void setJavaHome(String newHome) {
        javaHome = newHome;
    }

    public Package addPackage(Package newOne){
        packageSet.add(newOne);

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

    public Set<Package> getPackages(String name){
        List<Package> list = nameToPackages.get(name);
        if(list == null) {
            if (packageSet.isEmpty()) {
                return Collections.emptySet();
            }
            // For tests only
            return Collections.unmodifiableSet(packageSet);
        }
        // XXX???
        return new LinkedHashSet<>(list);
    }

    public void computeAllDependenciesRecursive() {
        for (Plugin plugin : pluginSet) {
            computeAllDependenciesRecursive(plugin);
        }

        finalValidation();
    }

   void finalValidation() {
       // TODO validate packages with different versions used by different plugins in same dependency chain?

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

        for (Plugin plugin : getPluginSet()) {
            depres.resolvePluginDependency(plugin);
        }
        for (Feature feature : getFeatureSet()) {
            depres.resolveFeatureDependency(feature);
        }

        for (Plugin plugin : getPluginSet()) {
            plugin.parsingDone();
        }
        for (Feature feature : getFeatureSet()) {
            feature.parsingDone();
        }
        for (Package pack : getPackageSet()) {
            pack.parsingDone();
        }

        return depres;
    }


}
