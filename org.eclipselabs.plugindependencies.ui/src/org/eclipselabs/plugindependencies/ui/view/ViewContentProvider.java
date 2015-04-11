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
import java.net.URI;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.ui.PlatformUI;
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.MainClass;
import org.eclipselabs.plugindependencies.core.OutputCreator;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;
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
        MainClass.cleanup();
    }

    private Job createResolveDependenciesJob() {

        return new Job("Reading target platform") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(getName(), 4);
                monitor.subTask("Reading platform plugins");
                MainClass.cleanup();
                TargetBundle[] bundles = view.getCurrentShownTarget().getBundles();

                MultiStatus ms = new MultiStatus(Activator.getPluginId(), 0, "Error while reading plugins", null);
                for (TargetBundle bundle : bundles) {
                    URI location = bundle.getBundleInfo().getLocation();
                    if(location == null){
                        ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading plugin: " + bundle, null));
                        continue;
                    }
                    String pluginPath = location.getPath();
                    try {
                        MainClass.readInPlugin(new File(pluginPath));
                    } catch (IOException e) {
                        ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading plugin: " + pluginPath, e));
                    }
                }
                monitor.internalWorked(1);

                monitor.subTask("Reading platform features");
                TargetFeature[] features = view.getCurrentShownTarget().getAllFeatures();
                // TODO At the moment all the Features are read in, also when their
                // are not chosen in target platform

                for (TargetFeature feature : features) {
                    String featurePath = feature.getLocation();
                    try {
                        MainClass.readInFeature(new File(featurePath));
                    } catch (IOException | SAXException | ParserConfigurationException e) {
                        ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading feature: " + featurePath, e));
                    }
                }
                monitor.internalWorked(1);

                monitor.subTask("Reading workspace projects");
                IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                for (IProject project : projects) {
                    if(!project.isAccessible() || project.isHidden()){
                        continue;
                    }
                    IPath location = project.getLocation();
                    if(location == null){
                        ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading project: " + project.getName(), null));
                        continue;
                    }
                    try {
                        IPluginModelBase model = PluginRegistry.findModel(project);
                        if(model != null){
                            MainClass.readInPlugin(location.toFile());
                        } else {
                            MainClass.readInFeature(location.toFile());
                        }
                    } catch (IOException | SAXException | ParserConfigurationException e) {
                        ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading project: " + location, e));
                    }
                }
                monitor.internalWorked(1);

                monitor.subTask("Resolving dependencies");
                MainClass.resolveDependencies();
                Set<Plugin> allPlugins = MainClass.getPluginSet();
                for (Plugin plugin : allPlugins) {
                    OutputCreator.computeResolvedPluginsRecursive(plugin);
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
                if(ms.getChildren().length > 0){
                    return ms;
                }
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
