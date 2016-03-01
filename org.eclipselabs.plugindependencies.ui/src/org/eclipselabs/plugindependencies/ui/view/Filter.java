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
        TreeParent p = ((TreeParent) element).getParent();
        if(p == null){
            return true;
        }
        return isLeafMatch(viewer, element);
    }

    @Override
    protected boolean isLeafMatch(Viewer viewer, Object element) {
        TreeParent leaf = (TreeParent) element;
        String labelText = leaf.getName();
        if (labelText == null) {
            return false;
        }
        TreeParent parent = leaf.getParent();
        if(parent == null || parent.getParent() == null){
            // always show root elements
            return true;
        }
        if(parent.getParent() != null && parent.getParent().getParent() != null){
            // always show third level elements
            return true;
        }
        // check only second level elements, so that we can see their dependencies
        return wordMatches(labelText);
    }
}
