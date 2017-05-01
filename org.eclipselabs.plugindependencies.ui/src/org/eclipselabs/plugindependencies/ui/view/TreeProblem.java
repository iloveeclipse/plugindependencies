/*******************************************************************************
 * Copyright (c) 2017 Andrey Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.ui.view;

import java.util.ArrayList;

import org.eclipselabs.plugindependencies.core.Feature;
import org.eclipselabs.plugindependencies.core.NamedElement;
import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;
import org.eclipselabs.plugindependencies.core.Problem;

public class TreeProblem extends TreeParent {

    private final ArrayList<TreeParent> children;

    private final TreeParent parent;

    private final Problem problem;

    public TreeProblem(Problem problem, TreeParent treeparent) {
        super(problem.getMessage(), treeparent);
        this.children = new ArrayList<>();
        this.problem = problem;
        this.parent = treeparent;
    }

    @Override
    public NamedElement getNamedElement() {
        return null;
    }

    @Override
    public TreeParent getParent() {
        return parent;
    }

    @Override
    public void addChild(TreeParent child) {
        children.add(child);
    }

    @Override
    public TreeParent[] getChildren() {
        if (children.isEmpty()) {
            TreeParent related = new TreeParent("Related", this);
            for (NamedElement named : getProblem().getRelated()) {
                if(named instanceof Plugin) {
                    TreePlugin tp = new TreePlugin((Plugin) named, this);
                    related.addChild(tp);
                } else if(named instanceof Feature) {
                    TreeFeature tp = new TreeFeature((Feature) named, this);
                    related.addChild(tp);
                } else if(named instanceof Package) {
                    TreePackage tp = new TreePackage((Package) named, this);
                    related.addChild(tp);
                }
            }
            if (related.hasChildren()) {
                this.addChild(related);
            }
        }
        return children.toArray(new TreeParent[children.size()]);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean hasChildren() {
        return !getProblem().getRelated().isEmpty();
    }

    public Problem getProblem() {
        return problem;
    }
}
