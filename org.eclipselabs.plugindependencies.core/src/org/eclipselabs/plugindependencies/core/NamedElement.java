/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov. All rights reserved.
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author aloskuto
 */
public abstract class NamedElement {

    public static final String EMPTY_VERSION = "";
    public static final String ZERO_VERSION = "0.0.0";

    protected final String name;
    private final String versionStr;
    private final Version version;
    private boolean versionRange;

    protected final List<Problem> log;

    private final Map<NamedElement, Filter> filterMap;

    public static final String PREFIX_ERROR = Logging.PREFIX_ERROR;
    public static final String PREFIX_WARN =  Logging.PREFIX_WARN;

    public static class NameComparator implements Comparator<NamedElement>{
        @Override
        public int compare(NamedElement o1, NamedElement o2) {
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
        if (version == null) {
            this.versionStr = EMPTY_VERSION;
            this.version = Version.ZERO;
            versionRange = false;
        } else {
            version = version.trim();
            if (version.isEmpty()) {
                this.versionStr = EMPTY_VERSION;
                this.version = Version.ZERO;
                versionRange = false;
            } else {
                if(Character.isDigit(version.charAt(0))){
                    this.version = new Version(version);
                    this.versionStr = this.version.toString();
                    versionRange = false;
                } else {
                    this.versionStr = version;
                    this.version = Version.ZERO;
                    versionRange = true;
                }
            }
        }
        this.log = new ArrayList<>();
        filterMap = new LinkedHashMap<>();
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
        result = prime * result + versionStr.hashCode();
        result = prime * result + version.hashCode();
        result = prime * result + (versionRange? 1 : 2);
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
        if(versionRange != other.versionRange){
            return false;
        }
        if(versionRange){
            return versionStr.equals(other.versionStr);
        }
        return version.equals(other.version);
    }

    /**
     * @return never null. In case no version were specified, returns {@link #EMPTY_VERSION}
     */
    public final String getVersion() {
        return versionStr;
    }

    protected void addErrorToLog(String note, NamedElement... related) {
        if (note == null || note.isEmpty()) {
            return;
        }
        List<NamedElement> list;
        if(related == null || related.length == 0) {
            list = Collections.EMPTY_LIST;
        } else {
            list = Arrays.asList(related);
        }
        Problem p = new Problem(note, Problem.ERROR, this, list);
        if (!log.contains(p)) {
            log.add(p);
        }
    }

    protected void addErrorToLog(String note, Collection<? extends NamedElement> related) {
        if (note == null || note.isEmpty()) {
            return;
        }
        Problem p = new Problem(note, Problem.ERROR, this, related);
        if (!log.contains(p)) {
            log.add(p);
        }
    }

    protected void addWarningToLog(String note, NamedElement... related) {
        if (note == null || note.isEmpty()) {
            return;
        }
        List<NamedElement> list;
        if(related == null || related.length == 0) {
            list = Collections.EMPTY_LIST;
        } else {
            list = Arrays.asList(related);
        }
        Problem p = new Problem(note, Problem.WARN, this, list);
        if (!log.contains(p)) {
            log.add(p);
        }
    }

    protected void addWarningToLog(String note, Collection<? extends NamedElement> related) {
        if (note == null || note.isEmpty()) {
            return;
        }
        Problem p = new Problem(note, Problem.WARN, this, related);
        if (!log.contains(p)) {
            log.add(p);
        }
    }

    /**
     * @return never null
     */
    public final List<Problem> getLog() {
        return log;
    }

    /**
     * @param vers check the version if the given version is not empty
     */
    public boolean matches(String id, String vers) {
        if(!name.equals(id)){
            return false;
        }
        // 0.0.0 matches everything
        if(vers.isEmpty() || ZERO_VERSION.equals(vers)){
            return true;
        }
        if(versionRange){
            return DependencyResolver.isCompatibleVersion(versionStr, vers);
        }
        if(Character.isDigit(vers.charAt(0))) {
            return version.equals(new Version(vers));
        }
        return versionStr.equals(vers);
    }

    public boolean exactMatch(NamedElement elt) {
        if(!name.equals(elt.name)){
            return false;
        }
        if(versionRange != elt.versionRange){
            return false;
        }
        if(versionRange){
            return versionStr.equals(elt.versionStr);
        }
        return version.equals(elt.version);
    }

    public boolean hasWarnings(){
        for (Problem p : log) {
            if(p.getSeverity() == Problem.WARN){
                return true;
            }
        }
        return false;
    }

    public boolean hasErrors(){
        for (Problem p : log) {
            if(p.getSeverity() == Problem.ERROR){
                return true;
            }
        }
        return false;
    }

    public boolean hasDefaultVersion(){
        return versionStr == EMPTY_VERSION || ZERO_VERSION.equals(versionStr);
    }

    public String getNameAndVersion(){
        if(versionStr == EMPTY_VERSION) {
            return name;
        }
        return name + " " + versionStr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[name=");
        builder.append(name);
        builder.append(", version=");
        builder.append(versionStr);
        if(hasFilter()) {
            builder.append(", filters=").append(filterMap);
        }
        builder.append("]");
        return builder.toString();
    }

    public Map<NamedElement, Filter> getFilterMap() {
        return filterMap;
    }

    public boolean hasFilter() {
        return !filterMap.isEmpty();
    }

    public boolean addFilter(NamedElement element, Filter filter) {
        if (element == null || filter == null) {
            return false;
        }
        if (filterMap.containsKey(element)) {
            return false; // already exists
        }
        filterMap.put(element, filter);
        return true;
    }

    public boolean isFiltered(NamedElement element) {
        if (element == null || filterMap.isEmpty()) {
            return false;
        }
        Filter filter = filterMap.get(element);
        if (filter == null) {
            return false; // no filter for this element
        }
        return filter.isMatching(element);
    }
}
