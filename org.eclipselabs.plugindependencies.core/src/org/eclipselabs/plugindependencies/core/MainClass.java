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

public class MainClass {

    /** public for tests */
    public static CommandLineInterpreter interpreter;

    public static void main(String[] args) {
        interpreter = new CommandLineInterpreter();
        int status = interpreter.interpreteInput(args);
        String name = MainClass.class.getClassLoader().getClass().getName();
        if(name.contains("eclipse") || name.contains("osgi")){
            return;
        }
        if(!System.getProperty("JUNIT_TESTS_RUNNING", "").isEmpty()) {
            // propagate exit code to JUnit tests
            throw new RuntimeException("" + status);
        }
        System.exit(status);
    }

}
