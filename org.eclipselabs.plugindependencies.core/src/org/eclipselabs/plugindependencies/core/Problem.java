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
package org.eclipselabs.plugindependencies.core;

import java.util.Collection;

public class Problem {

    public static final int ERROR = 1;
    public static final int WARN = 2;

    private final String message;
    private final int severity;
    private final Collection<? extends NamedElement> related;
    private final NamedElement owner;

    public Problem(String message, int severity, NamedElement owner, Collection<? extends NamedElement> related) {
        super();
        this.message = message;
        this.severity = severity;
        this.owner = owner;
        this.related = related;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime + message.hashCode();
        result = prime * result + owner.hashCode();
        result = prime * result + related.hashCode();
        result = prime * result + severity;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Problem)) {
            return false;
        }
        Problem other = (Problem) obj;
        if (severity != other.severity) {
            return false;
        }
        if (!message.equals(other.message)) {
            return false;
        }
        if (!owner.equals(other.owner)) {
            return false;
        }
        if (!related.equals(other.related)) {
            return false;
        }
        return true;
    }

    public String getMessage() {
        return message;
    }

    public String getLogMessage() {
        String note;
        if(Logging.prefixLogWithId){
            note = getPrefix() + "[" + owner.getNameAndVersion() + "] " + getMessage();
        } else {
            note = getPrefix() + getMessage();
        }
        return note;
    }

    String getPrefix() {
        return getSeverity() == ERROR? NamedElement.PREFIX_ERROR : NamedElement.PREFIX_WARN;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isError() {
        return getSeverity() == ERROR;
    }

    public boolean isWarning() {
        return getSeverity() == WARN;
    }

    public Collection<? extends NamedElement> getRelated() {
        return related;
    }

    @Override
    public String toString() {
        return getLogMessage();
    }
}
