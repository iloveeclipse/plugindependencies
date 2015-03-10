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

import org.eclipse.jface.viewers.ViewerComparator;

/**
 * @author obroesam
 *
 */
public class ViewSorter extends ViewerComparator {

    @Override
    public int category(Object element) {
        if(element instanceof TreeParent){
            TreeParent elt = (TreeParent) element;
            if(elt.getName().equals("Features")){
                return 0;
            }
            if(elt.getName().equals("Plugins")){
                return 1;
            }
        }
        return 2;
    }
}
