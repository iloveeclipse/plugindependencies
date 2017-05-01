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
import org.eclipselabs.plugindependencies.core.NamedElement;
import org.eclipselabs.plugindependencies.core.Problem;
import org.eclipselabs.plugindependencies.ui.Activator;

/**
 * @author obroesam
 *
 */
public class ViewLabelProvider extends ColumnLabelProvider {

    private final Image pluginImage;

    private final Image packageImage;

    private final Image featureImage;
    private final Image errorImage;
    private final Image warningImage;

    public ViewLabelProvider() {
        super();
        pluginImage = Activator.getImage("$nl$/icons/plugin_obj.gif");
        packageImage = Activator.getImage("$nl$/icons/package_obj.gif");
        featureImage = Activator.getImage("$nl$/icons/feature_obj.gif");
        errorImage = Activator.getImage("$nl$/icons/message_error.png");
        warningImage = Activator.getImage("$nl$/icons/message_warning.png");
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
        if (obj instanceof TreeProblem) {
            TreeProblem tp = (TreeProblem) obj;
            Problem p = tp.getProblem();
            return p.isError()? errorImage : warningImage;
        }
        String imageKey = ISharedImages.IMG_OBJ_FOLDER;
        return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
    }

    @Override
    public Color getForeground(Object element) {
        if (element instanceof TreeParent) {
            NamedElement elt = ((TreeParent) element).getNamedElement();
            if(elt == null){
                return null;
            }
            List<Problem> log = elt.getLog();
            if (!log.isEmpty()) {
                if (log.stream().anyMatch(x -> x.isError())) {
                    return Display.getDefault().getSystemColor(SWT.COLOR_RED);
                }
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
            }
        }
        return null;
    }

    @Override
    public String getToolTipText(Object element) {
        if(element instanceof TreeParent){
            TreeParent tp = (TreeParent) element;
            NamedElement elt = tp.getNamedElement();
            if(elt == null){
                return null;
            }
            List<Problem> log = elt.getLog();
            if(log.isEmpty()){
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Problem err : log) {
                sb.append(err).append("\n");
            }
            return sb.toString();
        }
        return super.getToolTipText(element);
    }
}
