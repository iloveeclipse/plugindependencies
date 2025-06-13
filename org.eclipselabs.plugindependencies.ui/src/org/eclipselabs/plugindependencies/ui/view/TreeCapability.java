/*******************************************************************************
 * Copyright (c) 2025 Andrey Loskutov
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

import org.eclipselabs.plugindependencies.core.Capability;
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 *
 */
public class TreeCapability extends TreeParent {
    private final ArrayList<TreeParent> children;

    private final Capability pack;

    private final TreeParent parent;

    public TreeCapability(Capability pack, TreeParent treeparent) {
        super(pack.getName() + " " + pack.getVersion(), treeparent);
        this.children = new ArrayList<>();
        this.pack = pack;
        this.parent = treeparent;
    }

    @Override
    public Capability getNamedElement() {
        return pack;
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
            // Exported By
            TreeParent providedBy = new TreeParent("Provided by", this);
            for (Plugin plugin : getNamedElement().getProvidedBy()) {
                providedBy.addChild(new TreePlugin(plugin, providedBy));
            }
            if (providedBy.hasChildren()) {
                this.addChild(providedBy);
            }
            // Imported By
            TreeParent requiredBy = new TreeParent("Required by", this);
            for (Plugin plugin : getNamedElement().getRequiredBy()) {
                requiredBy.addChild(new TreePlugin(plugin, requiredBy));
            }
            if (requiredBy.hasChildren()) {
                this.addChild(requiredBy);
            }
            addProblems();
        }
        return children.toArray(new TreeParent[children.size()]);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean hasChildren() {
        boolean hasProvidedBy = !getNamedElement().getProvidedBy().isEmpty();
        boolean hasRequiredBy = !getNamedElement().getRequiredBy().isEmpty();
        boolean hasProblems = !getNamedElement().getLog().isEmpty();
        return hasProvidedBy || hasRequiredBy || hasProblems;
    }
}
