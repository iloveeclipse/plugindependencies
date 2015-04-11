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
    public static final String PREFIX_ERROR = "Error: ";
    public static final String PREFIX_WARN = "Warning: ";

    private static AbstractLogger logger;
    static {
        setLogger(new SimpleLogger(System.out, System.err));
    }

    public static void writeStandardOut(String output) {
        logger.debug(output);
    }

    public static void writeErrorOut(String output) {
        logger.error(output);
    }

    public static void setLogger(AbstractLogger logger){
        Logging.logger = logger;
    }

    public static AbstractLogger getLogger() {
        return logger;
    }

    public static abstract class AbstractLogger {
        final PrintStream out;
        final PrintStream err;
        public AbstractLogger() {
            this(null, null);
        }
        public AbstractLogger(PrintStream out) {
            this(out, out);
        }
        public AbstractLogger(PrintStream out, PrintStream err) {
            super();
            this.out = out;
            this.err = err;
        }
        public abstract void error(String message, Throwable ... t);
        public abstract void warning(String message, Throwable ... t);
        public abstract void debug(String message, Throwable ... t);
    }

    public static class SimpleLogger extends AbstractLogger {

        public SimpleLogger(PrintStream out) {
            this(out, out);
        }
        public SimpleLogger(PrintStream out, PrintStream err) {
            super(out, err);
        }

        @Override
        public void error(String message, Throwable ... t) {
            err.print(PREFIX_ERROR);
            err.println(message);
            if(t != null && t.length > 0){
                t[0].printStackTrace(err);
            }
        }

        @Override
        public void warning(String message, Throwable ... t) {
            err.print(PREFIX_WARN);
            out.println(message);
            if(t != null && t.length > 0){
                t[0].printStackTrace(out);
            }
        }

        @Override
        public void debug(String message, Throwable ... t) {
            out.println(message);
            if(t != null && t.length > 0){
                t[0].printStackTrace(out);
            }
        }

    }
}
