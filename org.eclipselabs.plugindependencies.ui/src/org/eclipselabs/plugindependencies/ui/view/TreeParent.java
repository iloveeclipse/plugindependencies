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

import java.util.ArrayList;

import org.eclipselabs.plugindependencies.core.NamedElement;

/**
 * @author obroesam
 *
 */
public class TreeParent {
    private final ArrayList<TreeParent> children;

    private final String name;

    private TreeParent parent;

    public TreeParent(String name, TreeParent treeparent) {
        this.name = name;
        children = new ArrayList<>();
        this.parent = treeparent;
    }

    public String getName() {
        return name;
    }

    public void setParent(TreeParent parent) {
        this.parent = parent;
    }

    public TreeParent getParent() {
        return parent;
    }

    public void addChild(TreeParent child) {
        children.add(child);
    }

    public void removeChild(TreeParent child) {
        children.remove(child);
    }

    public TreeParent[] getChildren() {
        return children.toArray(new TreeParent[children.size()]);
    }

    public NamedElement getNamedElement(){
        return null;
    }

    static String getName(NamedElement elt) {
        String version = elt.getVersion();
        if(version.isEmpty()) {
            return elt.getName();
        }
        return elt.getName() + " " + version;
    }

    @Override
    public String toString() {
        return getName() + " (" + children.size() + ")";
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
