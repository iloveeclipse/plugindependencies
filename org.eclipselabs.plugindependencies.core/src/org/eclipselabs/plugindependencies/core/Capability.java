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
package org.eclipselabs.plugindependencies.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 */
public class Capability extends NamedElement {

    private Set<Plugin> providedBy;

    private Set<Plugin> requiredBy;


    public Capability(String name, String version) {
        super(name, version);
        this.providedBy = new LinkedHashSet<>();
        this.requiredBy = new LinkedHashSet<>();
    }

    public String getInformationLine() {
        String sep = getVersion().isEmpty()? "" : " ";
        return "capability: " + getName() + sep + getVersion() + "\n" + printProvidedBy(1);
    }

    public Set<Plugin> getProvidedBy() {
        return providedBy;
    }

    public void addProvidingPlugin(Plugin plugin) {
        this.providedBy.add(plugin);
    }

    public Set<Plugin> getRequiredBy() {
        return requiredBy;
    }

    public void addRequiredBy(Plugin requiresCapability) {
        this.requiredBy.add(requiresCapability);
    }

    /**
     * Print the Plugins that are Exporting this.
     *
     * @param indentation
     *            Indentation of the out string
     * @return String with Plugin information
     */
    public String printProvidedBy(int indentation) {
        StringBuilder out = new StringBuilder();
        out.append("\t\tprovided by:\n");
        if (providedBy.size() == 0) {
            out.append("\t\tJRE System Library\n");
            return out.toString();
        }
        for (Plugin plugin : providedBy) {
            out.append("\t\t");
            out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    /**
     * Print the Plugins that are importing this.
     *
     * @param indentation
     *            Indentation of the out string
     * @return String with Plugin information
     */
    public String printRequiredBy(int indentation) {
        StringBuilder out = new StringBuilder();
        if (requiredBy.size() == 0) {
            return out.toString();
        }
        out.append("required by:\n");
        for (Plugin plugin : requiredBy) {
            out.append("\t");
            out.append(plugin.isOptional(this) ? "*optional* for " : "");
            out.append(plugin.isFragment() ? "fragment: " : "plugin: ");
            out.append(plugin.getInformationLine() + "\n");
        }
        return out.toString();
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        return obj instanceof Capability;
    }

    public void parsingDone() {
        requiredBy = requiredBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(requiredBy);
        providedBy = providedBy.isEmpty()? Collections.EMPTY_SET : Collections.unmodifiableSet(providedBy);
    }

}
