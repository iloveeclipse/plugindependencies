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

public record Filter (String attribute) {

    public boolean isMatching(NamedElement element) {
        // XXX this is of course wrong, but should be enough for the first time
        if(attribute.contains("osgi.os")) {
            return true;
        }
        // XXX this is of course wrong, but should be enough for the first time
        return attribute.contains(element.getName());
    }

}
