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

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipselabs.plugindependencies.ui.Activator;

/**
 * @author obroesam
 *
 */
public class ViewLabelProvider extends ColumnLabelProvider {

    private final Image pluginImage;

    private final Image packageImage;

    private final Image featureImage;

    public ViewLabelProvider() {
        super();
        pluginImage = Activator.getImageDescriptor("$nl$/icons/plugin_obj.gif").createImage();
        packageImage = Activator.getImageDescriptor("$nl$/icons/package_obj.gif").createImage();
        featureImage = Activator.getImageDescriptor("$nl$/icons/feature_obj.gif").createImage();
    }

    @Override
    public String getText(Object obj) {
        return Objects.toString(obj);
    }

    @Override
    public Image getImage(Object obj) {
        if (obj instanceof TreePlugin) {
            return pluginImage;
        }
        if (obj instanceof TreePackage) {
            return packageImage;
        }
        if (obj instanceof TreeFeature) {
            return featureImage;
        }
        String imageKey = ISharedImages.IMG_OBJ_FOLDER;
        return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
    }

    @Override
    public Color getForeground(Object element) {
        if (element instanceof TreePlugin) {
            List<String> log = ((TreePlugin) element).getPlugin().getLog();
            if (!log.isEmpty()) {
                if (log.toString().contains("Error")) {
                    return Display.getDefault().getSystemColor(SWT.COLOR_RED);
                }
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
            }
        }
        if (element instanceof TreeFeature) {
            List<String> log = ((TreeFeature) element).getFeature().getLog();
            if (!log.isEmpty()) {
                if (log.toString().contains("Error")) {
                    return Display.getDefault().getSystemColor(SWT.COLOR_RED);
                }
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        pluginImage.dispose();
        packageImage.dispose();
        featureImage.dispose();
        super.dispose();
    }
}
