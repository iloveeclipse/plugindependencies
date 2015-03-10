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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @author obroesam
 *
 */
public class Filter extends PatternFilter {

    @Override
    public boolean isElementVisible(Viewer viewer, Object element) {
        return isLeafMatch(viewer, element);
    }

    @Override
    protected boolean isLeafMatch(Viewer viewer, Object element) {
        TreeParent leaf = (TreeParent) element;
        String labelText = leaf.getName();

        if (labelText == null) {
            return false;
        }
        if (labelText.equals("Plugins") || labelText.equals("Packages")
                || labelText.equals("Features")) {
            return true;
        }

        boolean isPlugin = leaf.getParent().getName().equals("Plugins");
        boolean isPackage = leaf.getParent().getName().equals("Packages");
        boolean isFeature = leaf.getParent().getName().equals("Features");

        if (isPlugin || isPackage || isFeature) {
            return wordMatches(labelText);
        }
        return true;
    }
}
