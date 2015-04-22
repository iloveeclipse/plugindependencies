/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>. All rights reserved.
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.ui;

import org.eclipselabs.plugindependencies.core.CommandLineInterpreter;

public class CliProxy {

    /**
     * Proxy for {@link CommandLineInterpreter#interpreteInput(String[])} which allows
     * to perform all initialization required for running {@link CommandLineInterpreter} inside Eclipse
     *
     * @param args non null arguments
     * @return status code
     * @see CommandLineInterpreter#interpreteInput(String[])
     * @see Activator#start(org.osgi.framework.BundleContext)
     */
    public static int run(String[] args){
        CommandLineInterpreter cli = new CommandLineInterpreter();
        return cli.interpreteInput(args);
    }
}
