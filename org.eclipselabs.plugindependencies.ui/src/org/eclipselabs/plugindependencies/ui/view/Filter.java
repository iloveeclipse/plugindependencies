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
import static org.eclipselabs.plugindependencies.ui.view.TreePlugin.*;

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
        if (NAMES.contains(labelText)) {
            return true;
        }

        String parentName = leaf.getParent().getName();

        if (NAMES.contains(parentName)) {
            return wordMatches(labelText);
        }
        return true;
    }
}
