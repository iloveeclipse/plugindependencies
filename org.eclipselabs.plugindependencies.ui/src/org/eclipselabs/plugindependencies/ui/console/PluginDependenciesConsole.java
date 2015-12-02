/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>. All rights reserved.
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/

package org.eclipselabs.plugindependencies.ui.console;

import java.io.PrintWriter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.themes.ITheme;
import org.eclipselabs.plugindependencies.core.PlatformState;

public class PluginDependenciesConsole extends MessageConsole implements IPropertyChangeListener {
    private static final String CONSOLE_FONT = "PluginDependencies.consoleFont";
    static PluginDependenciesConsole console;

    boolean disposed;
    private PlatformState state;

    private static class RemoveAction extends Action {
        public RemoveAction() {
            super("Close Plugin Dependencies", PlatformUI.getWorkbench().getSharedImages()
                    .getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        }

        @Override
        public void run() {
            IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
            if (console != null) {
                manager.removeConsoles(new IConsole[] { console });
                console = null;
            }
        }
    }

    private PluginDependenciesConsole(String name, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
        super(name, imageDescriptor, autoLifecycle);

    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (CONSOLE_FONT.equals(event.getProperty())) {
            setConsoleFont();
        }
    }

    @Override
    protected void dispose() {
        if (!disposed) {
            state = null;
            disposed = true;
            ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            theme.removePropertyChangeListener(this);
            super.dispose();
        }
    }

    private void setConsoleFont() {
        if (Display.getCurrent() == null) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    setConsoleFont();
                }
            });
        } else {
            ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            Font font = theme.getFontRegistry().get(CONSOLE_FONT);
            console.setFont(font);
        }
    }

    public static class PluginDependenciesConsoleFactory implements IConsoleFactory {

        @Override
        public void openConsole() {
            showConsole();
        }

    }

    public static PluginDependenciesConsole showConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
        boolean exists = false;
        if (console != null) {
            IConsole[] existing = manager.getConsoles();
            for (int i = 0; i < existing.length; i++) {
                if (console == existing[i]) {
                    exists = true;
                }
            }
        } else {
            console = new PluginDependenciesConsole("Plug-in Dependencies", null, true);
        }
        if (!exists) {
            manager.addConsoles(new IConsole[] { console });
        }
        ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        theme.addPropertyChangeListener(console);
        console.setConsoleFont();
        manager.showConsoleView(console);
        return console;
    }

    public static MessageConsole getConsole() {
        return console;
    }

    public static class PluginDependenciesConsolePageParticipant implements IConsolePageParticipant {

        private RemoveAction removeAction;

        @Override
        public void activated() {
            // noop
        }

        @Override
        public void deactivated() {
            // noop
        }

        @Override
        public void dispose() {
            removeAction = null;
        }

        @Override
        public void init(IPageBookViewPage page, IConsole console1) {
            removeAction = new RemoveAction();
            IActionBars bars = page.getSite().getActionBars();
            bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeAction);
        }

        @Override
        public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
            return null;
        }
    }

    public void showState(PlatformState newState) {
        if(state == newState){
            return;
        }
        this.state = newState;
        IOConsoleOutputStream out = newOutputStream();
        try(PrintWriter pw = new PrintWriter(out)){
            pw.println("### Overview ###");
            state.dumpAllElements(pw);
            pw.println("### - ###\n");

            pw.println("### Warnings / errors ###");
            StringBuilder sb = state.dumpLogs();
            pw.println(sb.toString());
            pw.println("### - ###");
        }

    }
}
