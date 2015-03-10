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
package org.eclipselabs.plugindependencies.core;

import java.io.PrintStream;

/**
 * @author obroesam
 *
 */
public class Logging {
    private static PrintStream standardOut = System.out;

    private static PrintStream standardError = System.err;

    public static void setErrorOut(PrintStream newError) {
        standardError = newError;
    }

    public static void setStandardOut(PrintStream newOut) {
        standardOut = newOut;
    }

    public static void writeStandardOut(String output) {
        standardOut.println(output);
    }

    public static void writeErrorOut(String output) {
        standardError.println(output);
    }
}
