/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov. All rights reserved.
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author aloskuto
 */
public abstract class NamedElement {

    public static final String EMPTY_VERSION = "";
    protected final String name;
    protected final String version;
    protected final List<String> log;

    public static class NameComparator implements Comparator<OSGIElement>{
        @Override
        public int compare(OSGIElement o1, OSGIElement o2) {
            if(o1.getClass() != o2.getClass()){
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
            return o1.getName().compareTo(o2.getName());
        }
    }

    public NamedElement(String name, String version) {
        super();
        Objects.requireNonNull(name);
        this.name = name.trim();
        this.version = version == null || version.trim().isEmpty()? EMPTY_VERSION : version.trim();
        this.log = new ArrayList<>();
    }

    /**
     * @return never null
     */
    public final String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime + name.hashCode();
        result = prime * result + version.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamedElement)) {
            return false;
        }
        NamedElement other = (NamedElement) obj;
        if (!name.equals(other.name)) {
            return false;
        }
        if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    /**
     * @return never null. In case no version were specified, returns {@link #EMPTY_VERSION}
     */
    public final String getVersion() {
        return version;
    }

    protected void addToLog(String note) {
        if (note != null && !note.isEmpty()) {
            log.add(note);
        }
    }

    /**
     * @return never null
     */
    public final List<String> getLog() {
        return log;
    }

    public boolean matches(String id, String vers) {
        return name.equals(id) && (version.equals(vers) || vers.isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[name=");
        builder.append(name);
        builder.append(", version=");
        builder.append(version);
        builder.append("]");
        return builder.toString();
    }
}