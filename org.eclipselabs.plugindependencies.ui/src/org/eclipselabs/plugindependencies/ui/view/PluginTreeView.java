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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipselabs.plugindependencies.core.NamedElement;
import org.eclipselabs.plugindependencies.ui.Activator;
import org.eclipselabs.plugindependencies.ui.console.PluginDependenciesConsole;

public class PluginTreeView extends ViewPart {

    public static final String ID = "org.eclipselabs.plugindependencies.ui.views.PluginTreeView";

    private TreeViewer viewer;

    private DrillDownAdapter drillDownAdapter;

    private Action collapseAll;

    private Action doubleClickAction;

    private Set<TargetAction> targetActions;

    private Action reloadTargets;

    private Action loadTarget;

    private Action showProperties;

    private boolean disposed;

    private Action showErrors;
    private Action hideWorkspacePlugins;

    private boolean showErrorsOnly;
    private boolean isHideWorkspacePlugins;

    private Action copyToClipboardAction;

    private Action openPluginXmlAction;

    private Action showConsole;

    @Override
    public void createPartControl(Composite parent) {
        Filter filter = new Filter();
        FilteredTree tree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL, filter, true) {
            @Override
            protected void updateToolbar(boolean visible) {
                super.updateToolbar(visible);
                viewer.collapseAll();
                viewer.expandToLevel(2);
            }
        };

        viewer = tree.getViewer();
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new ViewContentProvider(this));
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setComparator(new ViewSorter());
        viewer.getTree().setToolTipText(null);
        ColumnViewerToolTipSupport.enableFor(viewer);
        getSite().setSelectionProvider(viewer);
        refresh(new Object());

        targetActions = new LinkedHashSet<>();

        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();

        readTargetDefinitions();

    }

    @Override
    public void dispose() {
        disposed = true;
        Job.getJobManager().cancel(PluginTreeView.class);
        super.dispose();
    }

    private void readTargetDefinitions() {
        targetActions.clear();
        Job job = new Job("Reading target definitions"){

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    ITargetHandle[] targets = TargetPlatformService.getDefault().getTargets(monitor);
                    for (ITargetHandle target : targets) {
                        ITargetDefinition def = target.getTargetDefinition();
                        TargetAction action = new TargetAction(def);
                        targetActions.add(action);
                    }
                } catch (CoreException e) {
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return family == PluginTreeView.class;
            }
        };
        job.setRule(getContentProvider().getRule());
        getProgressService().schedule(job);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                PluginTreeView.this.fillContextMenu(manager);
            }
        });
        if (viewer != null) {
            Menu menu = menuMgr.createContextMenu(viewer.getControl());
            viewer.getControl().setMenu(menu);
        }
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());

        IMenuManager dropDownMenu = bars.getMenuManager();
        dropDownMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillTargetMenu(manager);
            }
        });
        dropDownMenu.add(showErrors);
        dropDownMenu.add(showConsole);
        dropDownMenu.add(hideWorkspacePlugins);
        dropDownMenu.add(loadTarget);
        dropDownMenu.add(reloadTargets);
    }

    private void fillTargetMenu(IMenuManager manager) {
        manager.removeAll();
        manager.add(showErrors);
        manager.add(showConsole);
        manager.add(hideWorkspacePlugins);
        manager.add(loadTarget);
        manager.add(reloadTargets);
        if (targetActions != null) {
            manager.add(new Separator());
            for (Action targetAction : targetActions) {
                manager.add(targetAction);
            }
        }
    }

    private void fillContextMenu(IMenuManager manager) {
        if (drillDownAdapter != null) {
            manager.add(copyToClipboardAction);
            manager.add(openPluginXmlAction);
            manager.add(showProperties);
            drillDownAdapter.addNavigationActions(manager);
        }
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(collapseAll);
        manager.add(showProperties);
        manager.add(showErrors);
        manager.add(showConsole);
        manager.add(loadTarget);
        manager.add(new Separator());
        if (drillDownAdapter != null) {
            drillDownAdapter.addNavigationActions(manager);
        }
    }

    private void makeActions() {
        collapseAll = new Action() {
            @Override
            public void run() {
                viewer.collapseAll();
            }
        };
        collapseAll.setText("Collapse All");
        collapseAll.setToolTipText("Collapse All");
        collapseAll.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));

        showProperties = new Action() {
            @Override
            public void run() {
                showProperties();
            }

        };
        showProperties.setText("Properties");
        showProperties.setToolTipText("Properties");
        ImageDescriptor propsImg = AbstractUIPlugin.imageDescriptorFromPlugin(
                "org.eclipse.ui", "$nl$/icons/full/eview16/prop_ps.gif");
        showProperties.setImageDescriptor(propsImg);

        showErrors = new Action() {
            @Override
            public void run() {
                boolean toggle = !isShowErrorsOnly();
                showErrors(toggle);
            }
        };
        showErrors.setText("Show Errors Only");
        showErrors.setToolTipText("Show Errors Only");
        ImageDescriptor errImg = Activator.getImageDescriptor("icons/errorwarning_tab.gif");
        showErrors.setImageDescriptor(errImg);
        showErrors.setChecked(false);

        hideWorkspacePlugins = new Action() {
            @Override
            public void run() {
                boolean toggle = !isHideWorkspacePlugins();
                hideWorkspacePlugins(toggle);
            }
        };
        hideWorkspacePlugins.setText("Hide Workspace Plug-ins");
        hideWorkspacePlugins.setToolTipText("Hide Workspace Plug-ins");
//        ImageDescriptor errImg = Activator.getImageDescriptor("icons/errorwarning_tab.gif");
//        hideWorkspacePlugins.setImageDescriptor(errImg);
        hideWorkspacePlugins.setChecked(false);

        loadTarget = new Action() {
            @Override
            public void run() {
                readTargetDefinition();
            }
        };
        loadTarget.setText("Load Target Definition");
        loadTarget.setImageDescriptor(Activator.getImageDescriptor("icons/target_profile_xml_obj.gif"));

        reloadTargets = new Action() {
            @Override
            public void run() {
                ViewContentProvider provider = getContentProvider();
                IWorkbenchSiteProgressService progressService = getProgressService();
                progressService.schedule(provider.getJob((TargetData)null));
                refresh(new Object());
                readTargetDefinitions();
            }
        };
        reloadTargets.setText("Reload Targets");
        reloadTargets.setImageDescriptor(Activator.getImageDescriptor("icons/refresh.gif"));

        showConsole = new Action() {
            @Override
            public void run() {
                PluginDependenciesConsole console = PluginDependenciesConsole.showConsole();
                console.showState(getContentProvider().getState());
            }
        };
        showConsole.setText("Show Console Output");
        showConsole.setImageDescriptor(ConsolePlugin.getImageDescriptor(IConsoleConstants.IMG_VIEW_CONSOLE));

        doubleClickAction = new Action() {
            @Override
            public void run() {
                ISelection selection = viewer.getSelection();
                if(selection.isEmpty()){
                    return;
                }
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                openEditor(obj,  "META-INF/MANIFEST.MF");

            }
        };

        openPluginXmlAction = new Action() {
            @Override
            public void run() {
                ISelection selection = viewer.getSelection();
                if(selection.isEmpty()){
                    return;
                }
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                openEditor(obj,  "plugin.xml");

            }
        };
        openPluginXmlAction.setText("Open plugin.xml");
        openPluginXmlAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

        copyToClipboardAction = new Action() {
            @Override
            public void run() {
                ISelection selection = viewer.getSelection();
                if(selection.isEmpty() || !(selection instanceof IStructuredSelection)){
                    return;
                }
                copyToClipboard((IStructuredSelection) selection);
            }
        };
        copyToClipboardAction.setText("&Copy");
        copyToClipboardAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        copyToClipboardAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyToClipboardAction);
    }

    protected void copyToClipboard(IStructuredSelection selection) {
        StringBuilder sb = new StringBuilder();
        for (Object object : (List<?>) selection.toList()) {
            if(object instanceof TreeParent){
                TreeParent tp = (TreeParent) object;
                object = tp.getNamedElement();
                if(object instanceof NamedElement){
                    NamedElement elt = (NamedElement) object;
                    sb.append(elt.getNameAndVersion()).append("\n");
                } else if(tp instanceof TreeProblem) {
                    sb.append(((TreeProblem) tp).getProblem()).append("\n");
                }
            }
        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        final Clipboard cb = new Clipboard(getSite().getShell().getDisplay());
        try {
        cb.setContents(new Object[]{sb.toString()}, new Transfer[]{textTransfer});
        } finally {
            cb.dispose();
        }
    }

    protected void readTargetDefinition() {
        OpenTargetFileDialog rd = new OpenTargetFileDialog(getSite().getShell(),
                ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
        int result = rd.open();
        if(result == Window.OK){
            Object[] selection = rd.getResult();
            if (selection == null || selection.length != 1 || !(selection[0] instanceof IFile)) {
                viewer.setInput(null);
                return;
            }
            TargetData td = new TargetData((IFile) selection[0]);

            ViewContentProvider provider = getContentProvider();
            IWorkbenchSiteProgressService progressService = getProgressService();
            progressService.schedule(provider.getJob(td));
        }

    }

    private static final class OpenTargetFileDialog extends ResourceListSelectionDialog {

        public OpenTargetFileDialog(Shell parentShell, IContainer container, int typesMask) {
            super(parentShell, container, typesMask);
            setTitle("Select Target (.t2) File");
            setMessage("Please select target file (.t2) to load");
        }

        @Override
        public void create() {
            super.create();
            getContents().getShell().getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    Control contents = getContents();
                    if(contents.isDisposed()){
                        return;
                    }
                    Control focus = contents.getShell().getDisplay().getFocusControl();
                    if(focus instanceof Text){
                        Text text = (Text) focus;
                        text.setText("*");
                    }
                }
            });
        }

        @Override
        protected boolean select(IResource resource) {
            if (resource == null) {
                return false;
            }
            String fileExtension = resource.getFileExtension();
            return "t2".equals(fileExtension) && super.select(resource);
        }
    }

    protected void showErrors(boolean on) {
        showErrorsOnly = on;
        refresh(new Object());
    }

    boolean isShowErrorsOnly() {
        return showErrorsOnly;
    }

    protected void hideWorkspacePlugins(boolean on) {
        isHideWorkspacePlugins = on;
        refresh(new Object());
    }

    boolean isHideWorkspacePlugins() {
        return isHideWorkspacePlugins;
    }

    void refresh(Object input) {
        viewer.setInput(input);
    }

    boolean isDisposed(){
        return disposed;
    }

    private void openEditor(Object obj, String pluginRelativePath) {
        IFileStore fileStore = null;

        if (obj instanceof TreeFeature) {
            TreeFeature treeFeature = (TreeFeature) obj;
            String featureXMLPath = treeFeature.getNamedElement().getPath();
            File dir = new Path(featureXMLPath).removeLastSegments(1).toFile();
            if (!dir.isDirectory()) {
                try (JarFile pluginJar = new JarFile(dir)) {
                    File tmpDir = extractFileToTmpDir(pluginJar, "feature.xml");
                    featureXMLPath = new File(tmpDir, "feature.xml").toString();
                } catch (IOException e) {
                    IStatus status = new Status(IStatus.ERROR, Activator.getPluginId(), "Can not open editor on: " + pluginRelativePath, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                    return;
                }
            }
            fileStore = EFS.getLocalFileSystem().getStore(new Path(featureXMLPath));
        } else if (obj instanceof TreePlugin) {
            TreePlugin treePlugin = (TreePlugin) obj;
            File pluginDir = new File(treePlugin.getNamedElement().getPath());
            try {
                if (!pluginDir.isDirectory()) {
                    try (JarFile pluginJar = new JarFile(pluginDir)) {
                        pluginDir = extractFileToTmpDir(pluginJar, pluginRelativePath);
                    }
                }
                if (pluginDir != null) {
                    IPath path = new Path(pluginDir.getCanonicalPath()).append(pluginRelativePath);
                    fileStore = EFS.getLocalFileSystem().getStore(path);
                }
            } catch (IOException e) {
                IStatus status = new Status(IStatus.ERROR, Activator.getPluginId(), "Can not open editor on: " + pluginRelativePath, e);
                StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                return;
            }
        }

        if (fileStore != null) {
            try {
                IDE.openEditorOnFileStore(getViewSite().getPage(), fileStore);
            } catch (PartInitException e) {
                StatusManager.getManager().handle(e.getStatus(), StatusManager.LOG | StatusManager.SHOW);
            }
        }
    }

    private void showProperties() {
        try {
            getSite().getPage().showView(IPageLayout.ID_PROP_SHEET);
        } catch (PartInitException e) {
            StatusManager.getManager().handle(e.getStatus(), StatusManager.LOG | StatusManager.SHOW);
        }
    }

    private static File extractFileToTmpDir(JarFile pluginJar, String path) throws IOException {
        JarEntry entry = pluginJar.getJarEntry(path);
        if(entry == null){
            return null;
        }

        File dir = Files.createTempDirectory("Plugin").toFile();
        File tmpFile = new File(dir.getCanonicalPath(), entry.getName());

        if (!tmpFile.getParentFile().exists() && !tmpFile.getParentFile().mkdirs()) {
            return null;
        }

        if (!tmpFile.createNewFile()) {
            return null;
        }

        try (OutputStream out = new FileOutputStream(tmpFile);
                InputStream is = pluginJar.getInputStream(entry)) {
            int data;
            while ((data = is.read()) >= 0) {
                out.write(data);
            }
        }
        return dir;
    }

    private void hookDoubleClickAction() {
        if (viewer != null) {
            viewer.addDoubleClickListener(new IDoubleClickListener() {
                @Override
                public void doubleClick(DoubleClickEvent event) {
                    doubleClickAction.run();
                }
            });
        }
    }

    class TargetAction extends Action {
        final ITargetDefinition targetDef;

        TargetAction(ITargetDefinition def) {
            super("", IAction.AS_CHECK_BOX);
            String name = def.getName();
            setText(name != null ? name : "<Unnamed target>");
            this.targetDef = def;
        }

        @Override
        public void run() {
            if (isChecked()) {
                for (TargetAction targetAction : targetActions) {
                    targetAction.setChecked(false);
                }
                ViewContentProvider provider = getContentProvider();
                IWorkbenchSiteProgressService progressService = getProgressService();
                progressService.schedule(provider.getJob(targetDef));
                setChecked(true);
            } else {
                viewer.setInput(null);
                setChecked(false);
            }
        }


    }

    private ViewContentProvider getContentProvider() {
        return (ViewContentProvider) viewer.getContentProvider();
    }

    IWorkbenchSiteProgressService getProgressService() {
        return (IWorkbenchSiteProgressService) getSite()
                .getService(IWorkbenchSiteProgressService.class);
    }

    @Override
    public void setFocus() {
        if (viewer != null) {
            viewer.getControl().setFocus();
        }
    }

}
