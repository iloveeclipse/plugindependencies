/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core.fixture;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RepeatRule implements TestRule {

    private static class RepeatStatement extends Statement {
        private final Statement statement;
        private final int repeat;

        public RepeatStatement(Statement statement, int repeat) {
            this.statement = statement;
            this.repeat = repeat;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < repeat; i++) {
                statement.evaluate();
            }
        }
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation(Repeat.class);
        if (repeat != null) {
            int times = repeat.value();
            result = new RepeatStatement(statement, times);
        }
        return result;
    }
}
