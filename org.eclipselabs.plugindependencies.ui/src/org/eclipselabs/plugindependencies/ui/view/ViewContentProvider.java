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

import static org.eclipselabs.plugindependencies.ui.view.TreePlugin.EARLY_STARTUP;
import static org.eclipselabs.plugindependencies.ui.view.TreePlugin.FEATURES;
import static org.eclipselabs.plugindependencies.ui.view.TreePlugin.PACKAGES;
import static org.eclipselabs.plugindependencies.ui.view.TreePlugin.PLUGINS;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.ui.PlatformUI;
import org.eclipselabs.plugindependencies.core.CommandLineInterpreter;
import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.ManifestEntry;
import org.eclipselabs.plugindependencies.core.NamedElement;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.PlatformState;
import org.eclipselabs.plugindependencies.core.PlatformState.PlatformSpecs;
import org.eclipselabs.plugindependencies.core.Plugin;
import org.eclipselabs.plugindependencies.core.StringUtil;
import org.eclipselabs.plugindependencies.ui.Activator;
import org.xml.sax.SAXException;

/**
 * @author obroesam
 *
 */
public class ViewContentProvider implements ITreeContentProvider {

    private static final Object[] EMPTY = new Object[0];

    private TreeParent invisibleRoot;

    private final PluginTreeView view;

    private final LoadTarget resolveJob;

    private final ResolveRule rule;

    private PlatformState state;

    public ViewContentProvider(PluginTreeView v) {
        super();
        state = new PlatformState();
        view = v;
        resolveJob = createResolveDependenciesJob();
        rule = new ResolveRule();
        resolveJob.setRule(rule);
    }

    public Job getJob(TargetData td) {
        resolveJob.cancel();
        resolveJob.setTargetData((ITargetDefinition) null);
        resolveJob.setTargetData(td);
        return resolveJob;
    }

    public Job getJob(ITargetDefinition td) {
        resolveJob.cancel();
        resolveJob.setTargetData((TargetData) null);
        resolveJob.setTargetData(td);
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
            createRoots(newInput);
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
        return EMPTY;
    }

    @Override
    public boolean hasChildren(Object parent) {
        if (parent instanceof TreeParent) {
            return ((TreeParent) parent).hasChildren();
        }
        return false;
    }

    void createRoots(Object newInput) {
        invisibleRoot = new TreeParent("Parent", null);
        // Plugins
        TreeParent plugins = new TreeParent(PLUGINS, invisibleRoot);
        boolean errorsOnly = view.isShowErrorsOnly();
        boolean allowWorkspace = !view.isHideWorkspacePlugins();
        for (Plugin plugin : state.getPlugins()) {
            if(! errorsOnly || (plugin.hasErrors() || plugin.hasWarnings())) {
                if(allowWorkspace || !plugin.isFromWorkspace()) {
                    plugins.addChild(new TreePlugin(plugin, plugins));
                }
            }
        }
        // Packages
        TreeParent packages = new TreeParent(PACKAGES, invisibleRoot);
        for (Package pack : state.getPackages()) {
            if(! errorsOnly || (pack.hasErrors() || pack.hasWarnings())) {
                if(allowWorkspace || !isFromWorkspaceOnly(pack)) {
                    packages.addChild(new TreePackage(pack, packages));
                }
            }
        }
        // Features
        TreeParent features = new TreeParent(FEATURES, invisibleRoot);
        for (Feature feature : state.getFeatures()) {
            if(! errorsOnly || (feature.hasErrors() || feature.hasWarnings())) {
                if(allowWorkspace || !feature.isFromWorkspace()) {
                    features.addChild(new TreeFeature(feature, features));
                }
            }
        }
        if(!plugins.hasChildren() &&  !packages.hasChildren() && !features.hasChildren()) {
            return;
        }
        invisibleRoot.addChild(plugins);
        invisibleRoot.addChild(packages);
        invisibleRoot.addChild(features);
        // EarlyStartups
        TreeParent early = new TreeParent(EARLY_STARTUP, invisibleRoot);
        for (Plugin plugin : state.getPlugins()) {
            if(! errorsOnly || (plugin.hasErrors() || plugin.hasWarnings())) {
                if(plugin.isEarlyStartup()) {
                    if(allowWorkspace || !plugin.isFromWorkspace()) {
                        early.addChild(new TreePlugin(plugin, plugins));
                    }
                }
            }
        }
        invisibleRoot.addChild(early);
    }

    private static boolean isFromWorkspaceOnly(Package pack) {
        Set<Plugin> exportedBy = pack.getExportedBy();
        return exportedBy.stream().allMatch(x -> x.isFromWorkspace());
    }

    private LoadTarget createResolveDependenciesJob() {

        return new LoadTarget("Reading target platform");
    }

    private final class LoadTarget extends Job {
        private ITargetDefinition itd;
        private TargetData td;

        private LoadTarget(String name) {
            super(name);
        }

        public void setTargetData(ITargetDefinition td) {
            this.itd = td;
        }

        public void setTargetData(TargetData td) {
            this.td = td;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(getName(), 5);
            monitor.subTask("Resolving target platform");
            if(itd != null && !itd.isResolved()) {
                itd.resolve(monitor);
            }
            monitor.internalWorked(1);

            monitor.subTask("Reading platform plugins");
            CommandLineInterpreter parser = new CommandLineInterpreter();
            state = parser.getState();
            parser.setParseEarlyStartup(true);
            parser.setPlatformSpecs(new PlatformSpecs(
                    Platform.getOS(),
                    Platform.getWS(),
                    Platform.getOSArch()));

            IStatus ms;
            boolean cleanOnly = false;
            if(itd != null){
                ms = loadTargetPlatform(monitor, parser, itd);
            } else if(td != null){
                ms = loadTargetPlatform(monitor, parser, td);
            } else {
                ms = Status.OK_STATUS;
                cleanOnly = true;
            }
            monitor.internalWorked(1);

            if (!cleanOnly) {
                monitor.subTask("Resolving dependencies");
                parser.getState().computeAllDependenciesRecursive();
                state = parser.getState();
            }
            monitor.internalWorked(1);

            monitor.done();
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if(view.isDisposed()){
                        return;
                    }
                    view.refresh(new Object());
                }
            });

            if(ms.getChildren().length > 0){
                return ms;
            }
            return Status.OK_STATUS;
        }

    }

    MultiStatus loadTargetPlatform(IProgressMonitor monitor, CommandLineInterpreter parser, ITargetDefinition target) {
        MultiStatus ms = new MultiStatus(Activator.getPluginId(), 0, "Error while reading plugins", null);
        TargetBundle[] bundles = target.getBundles();

        for (TargetBundle bundle : bundles) {
            if(monitor.isCanceled()){
                return ms;
            }
            URI location = bundle.getBundleInfo().getLocation();
            if(location == null){
                ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading plugin: " + bundle, null));
                continue;
            }
            String pluginPath = location.getPath();
            try {
                parser.readInPlugin(new File(pluginPath), false);
            } catch (IOException e) {
                ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading plugin: " + pluginPath, e));
            }
        }
        monitor.internalWorked(1);

        monitor.subTask("Reading platform features");
        TargetFeature[] features = target.getAllFeatures();
        // TODO At the moment all the Features are read in, also when their
        // are not chosen in target platform

        for (TargetFeature feature : features) {
            if(monitor.isCanceled()){
                return ms;
            }
            String featurePath = feature.getLocation();
            try {
                parser.readInFeature(new File(featurePath), false);
            } catch (IOException | SAXException | ParserConfigurationException e) {
                ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading feature: " + featurePath, e));
            }
        }
        monitor.internalWorked(1);
        if(!view.isHideWorkspacePlugins()) {
            readWorkspace(monitor, parser, ms);
        }
        return ms;
    }

    MultiStatus loadTargetPlatform(IProgressMonitor monitor, CommandLineInterpreter parser, TargetData target) {
        MultiStatus ms = new MultiStatus(Activator.getPluginId(), 0, "Error while reading target", null);
        List<String> paths = target.getPaths();

        for (String somePath : paths) {
            if(somePath.startsWith("-") && somePath.length() > 1){
                List<String> idAndVers = StringUtil.split(somePath.substring(1), ' ');
                ManifestEntry me = null;
                if(idAndVers.size() > 1){
                    me = new ManifestEntry(idAndVers.get(0), idAndVers.get(1));
                } else if(idAndVers.size() > 0){
                    me = new ManifestEntry(idAndVers.get(0), NamedElement.EMPTY_VERSION);
                }
                if(me != null) {
                    parser.getState().hideElement(me);
                }
            }
        }

        for (String somePath : paths) {
            if(monitor.isCanceled()){
                return ms;
            }
            if(isNotValidPath(somePath)){
                continue;
            }
            try {
                parser.readInEclipseFolder(somePath);
            } catch (IOException | SAXException | ParserConfigurationException e) {
                ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading: " + somePath, e));
            }
        }
        monitor.internalWorked(2);
        if(!view.isHideWorkspacePlugins()) {
            readWorkspace(monitor, parser, ms);
        }
        return ms;
    }

    private static boolean isNotValidPath(String somePath) {
        return somePath.trim().isEmpty() || somePath.startsWith("#") || somePath.startsWith("-");
    }

    private static void readWorkspace(IProgressMonitor monitor, CommandLineInterpreter parser, MultiStatus ms) {
        monitor.subTask("Reading workspace projects");
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            if(monitor.isCanceled()){
                return;
            }
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
                    parser.readInPlugin(location.toFile(), true);
                } else {
                    parser.readInFeature(location.toFile(), true);
                }
            } catch (IOException | SAXException | ParserConfigurationException e) {
                ms.add(new Status(IStatus.ERROR, Activator.getPluginId(), "Error while reading project: " + location, e));
            }
        }
        monitor.internalWorked(1);
    }

    public PlatformState getState() {
        return state;
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
