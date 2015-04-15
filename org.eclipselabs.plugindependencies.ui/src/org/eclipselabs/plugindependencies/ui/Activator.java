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
package org.eclipselabs.plugindependencies.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipselabs.plugindependencies.core.Logging;
import org.eclipselabs.plugindependencies.core.Logging.AbstractLogger;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        Logging.prefixLogWithId = false;
        Logging.setLogger(new AbstractLogger() {

            @Override
            public void warning(String message, Throwable... t) {
                getLog().log(warningStatus(message, t));
            }

            @Override
            public void error(String message, Throwable... t) {
                getLog().log(errorStatus(message, t));
            }

            @Override
            public void debug(String message, Throwable... t) {
                getLog().log(debugStatus(message, t));
            }
        });
    }

    public IStatus errorStatus(String message, Throwable ... t) {
        return new Status(IStatus.ERROR, getPluginId(), message, t.length > 0? t[0] : null);
    }

    public IStatus warningStatus(String message, Throwable ... t) {
        return new Status(IStatus.WARNING, getPluginId(), message, t.length > 0? t[0] : null);
    }

    public IStatus debugStatus(String message, Throwable ... t) {
        return new Status(IStatus.INFO, getPluginId(), message, t.length > 0? t[0] : null);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(getPluginId(), path);
    }

    public static String getPluginId() {
        return plugin.getBundle().getSymbolicName();
    }
}
