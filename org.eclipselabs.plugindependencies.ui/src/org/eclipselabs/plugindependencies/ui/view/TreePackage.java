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

import org.eclipselabs.plugindependencies.core.Package;
import org.eclipselabs.plugindependencies.core.Plugin;

/**
 * @author obroesam
 *
 */
public class TreePackage extends TreeParent {
    private final ArrayList<TreeParent> children;

    private final Package pack;

    private final TreeParent parent;

    public TreePackage(Package pack, TreeParent treeparent) {
        super(pack.getName() + " " + pack.getVersion(), treeparent);
        this.children = new ArrayList<>();
        this.pack = pack;
        this.parent = treeparent;
    }

    @Override
    public Package getNamedElement() {
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
            TreeParent exportedBy = new TreeParent("Exported by", this);
            for (Plugin plugin : getNamedElement().getExportedBy()) {
                exportedBy.addChild(new TreePlugin(plugin, exportedBy));
            }
            if (exportedBy.hasChildren()) {
                this.addChild(exportedBy);
            }
            // Imported By
            TreeParent importedBy = new TreeParent("Imported by", this);
            for (Plugin plugin : getNamedElement().getImportedBy()) {
                importedBy.addChild(new TreePlugin(plugin, importedBy));
            }
            if (importedBy.hasChildren()) {
                this.addChild(importedBy);
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
        boolean hasExportedBy = !getNamedElement().getExportedBy().isEmpty();
        boolean hasImportedBy = !getNamedElement().getImportedBy().isEmpty();
        boolean hasProblems = !getNamedElement().getLog().isEmpty();
        return hasExportedBy || hasImportedBy || hasProblems;
    }
}
