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
import org.eclipselabs.plugindependencies.core.Problem;

/**
 * @author obroesam
 *
 */
public class TreeParent {
    private final ArrayList<TreeParent> children;

    private final String name;

    private TreeParent parent;

    static final String ERRORS = "Errors";
    static final String WARNINGS = "Warnings";

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

    void addProblems() {
        NamedElement elt = getNamedElement();
        if(elt == null) {
            return;
        }
        // Errors
        TreeParent errors = new TreeParent(ERRORS, this);
        TreeParent warnings = new TreeParent(WARNINGS, this);
        for (Problem p : elt.getLog()) {
            if(p.isError()) {
                errors.addChild(new TreeProblem(p, errors));
            } else if(p.isWarning()) {
                warnings.addChild(new TreeProblem(p, warnings));
            }
        }
        if (errors.hasChildren()) {
            this.addChild(errors);
        }
        if (warnings.hasChildren()) {
            this.addChild(warnings);
        }
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
