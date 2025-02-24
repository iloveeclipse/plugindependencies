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
package org.eclipselabs.plugindependencies.core.fixture;

import java.io.File;

import org.eclipselabs.plugindependencies.core.MainClass;

/**
 * @author obroesam
 *
 */
public class SecurityMan {

    protected static String unFix(String s){
        if(s == null || File.separatorChar == '/'){
            return s;
        }
        return s.replace('/', '\\');
    }

    public static int runMain(String[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i] = unFix(args[i]);
        }

        /**
         * try catch is for System.exit, otherwise test will not end
         */
        try {
            MainClass.main(args);
        } catch (Exception e) {
            // System.exit caught
            return Integer.parseInt(e.getMessage());
        }
        return 42;
    }
}
