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
package org.eclipselabs.plugindependencies.ui.view;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.ui.PlatformUI;
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.FeatureParser;
import org.eclipselabs.plugindependencies.core.MainClass;
import org.eclipselabs.plugindependencies.core.OutputCreator;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;
import org.eclipselabs.plugindependencies.core.PluginParser;
import org.eclipselabs.plugindependencies.ui.Activator;
import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class ViewContentProvider implements ITreeContentProvider {
    private TreeParent invisibleRoot;

    private final PluginTreeView view;

    private final Job resolveJob;

    private final ResolveRule rule;

    public ViewContentProvider(PluginTreeView v) {
        super();
        view = v;
        resolveJob = createResolveDependenciesJob();
        rule = new ResolveRule();
        resolveJob.setRule(rule);
    }

    public Job getJob() {
        return resolveJob;
    }

    /**
     * @return Returns the rule.
     */
    public ResolveRule getRule() {
        return rule;
    }

    @Override
    public void dispose() {
        resolveJob.cancel();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if(newInput != null) {
            createRoots();
        }
    }

    public TreeParent getRoot() {
        return invisibleRoot;
    }

    @Override
    public Object[] getElements(Object parent) {
        if(parent != null && parent.getClass() == Object.class){
            return getChildren(invisibleRoot);
        }
        return getChildren(parent);
    }

    @Override
    public Object getParent(Object child) {
        if (child instanceof TreeParent) {
            return ((TreeParent) child).getParent();
        }
        return null;
    }

    @Override
    public Object[] getChildren(Object parent) {
        if (parent instanceof TreeParent) {
            return ((TreeParent) parent).getChildren();
        }
        return new Object[0];
    }

    @Override
    public boolean hasChildren(Object parent) {
        if (parent instanceof TreeParent) {
            return ((TreeParent) parent).hasChildren();
        }
        return false;
    }

    void createRoots() {
        invisibleRoot = new TreeParent("Parent", null);
        // Plugins
        TreeParent plugins = new TreeParent("Plugins", invisibleRoot);
        for (Plugin plugin : MainClass.getPluginSet()) {
            plugins.addChild(new TreePlugin(plugin, plugins));
        }
        // Packages
        TreeParent packages = new TreeParent("Packages", invisibleRoot);
        for (Package pack : MainClass.getPackageSet()) {
            packages.addChild(new TreePackage(pack, packages));
        }
        // Features
        TreeParent features = new TreeParent("Features", invisibleRoot);
        for (Feature feature : MainClass.getFeatureSet()) {
            features.addChild(new TreeFeature(feature, features));
        }
        if(!plugins.hasChildren() &&  !packages.hasChildren() && !features.hasChildren()) {
            return;
        }
        invisibleRoot.addChild(plugins);
        invisibleRoot.addChild(packages);
        invisibleRoot.addChild(features);
        MainClass.initVariables();
    }

    private Job createResolveDependenciesJob() {
        return new Job("Reading target platform") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(getName(), 3);
                monitor.subTask("Reading plugins");
                MainClass.initVariables();
                try {
                    TargetBundle[] bundles = view.getCurrentShownTarget().getBundles();

                    for (TargetBundle bundle : bundles) {
                        String pluginPath = bundle.getBundleInfo().getLocation()
                                .getPath();
                        PluginParser.createPluginAndAddToSet(new File(pluginPath),
                                MainClass.getPluginSet(), MainClass.getPackageSet());
                    }
                } catch (IOException e) {
                    return new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading plugins", e);
                }
                monitor.internalWorked(1);
                monitor.subTask("Reading features");
                try {
                    TargetFeature[] features = view.getCurrentShownTarget()
                            .getAllFeatures();
                    // TODO At the moment all the Features are read in, also when their
                    // are not chosen in target platform

                    for (TargetFeature feature : features) {
                        String featurePath = feature.getLocation();
                        FeatureParser.createFeatureAndAddToSet(new File(featurePath),
                                MainClass.getFeatureSet());
                    }
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    return new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading features", e);
                }
                monitor.internalWorked(1);
                monitor.subTask("Resolving dependencies");
                MainClass.resolveDependencies();
                Set<Plugin> allPlugins = MainClass.getPluginSet();
                for (Plugin plugin : allPlugins) {
                    OutputCreator.getResolvedPluginsRecursive(plugin);
                }

                monitor.internalWorked(1);
                monitor.done();
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if(view.isDisposed()){
                            return;
                        }
                        view.refresh();
                    }
                });
                return Status.OK_STATUS;
            }
        };
    }

    static class ResolveRule implements ISchedulingRule {
        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
        }

    }
}
