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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.eclipselabs.plugindependencies.core.DependencyResolver.PluginElt;

/**
 */
public class PlatformState {

    private final Set<Plugin> pluginSet;
    private final Set<Package> packageSet;
    private final Set<Feature> featureSet;
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

    public void computeAllDependenciesRecursive() {
        for (Plugin plugin : pluginSet) {
            computeAllDependenciesRecursive(plugin);
        }
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

    public void resolveDependencies() {
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
    }

}
