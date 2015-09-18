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
    public static final String ZERO_VERSION = "0.0.0";

    protected final String name;
    private final String versionStr;
    private final Version version;
    private boolean versionRange;

    protected final List<String> log;
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

    protected void addToLog(String note) {
        if (note != null && !note.isEmpty() && !log.contains(note)) {
            log.add(note);
        }
    }

    protected void addErrorToLog(String note) {
        if (note == null || note.isEmpty()) {
            return;
        }
        if(Logging.prefixLogWithId){
            note = PREFIX_ERROR + "[" + getNameAndVersion() + "] " + note;
        } else {
            note = PREFIX_ERROR + note;
        }
        if (!log.contains(note)) {
            log.add(note);
        }
    }

    protected void addWarningToLog(String note) {
        if (note == null || note.isEmpty()) {
            return;
        }
        if(Logging.prefixLogWithId){
            note = PREFIX_WARN + "[" + getNameAndVersion() + "] " + note;
        } else {
            note = PREFIX_WARN + note;
        }
        if (!log.contains(note)) {
            log.add(note);
        }
    }

    /**
     * @return never null
     */
    public final List<String> getLog() {
        return log;
    }

    /**
     * @param vers check the version if the given version is not empty
     */
    public boolean matches(String id, String vers) {
        if(!name.equals(id)){
            return false;
        }
        if(vers.isEmpty()){
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
        for (String string : log) {
            if(string.startsWith(PREFIX_WARN)){
                return true;
            }
        }
        return false;
    }

    public boolean hasErrors(){
        for (String string : log) {
            if(string.startsWith(PREFIX_ERROR)){
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
        builder.append("]");
        return builder.toString();
    }
}
