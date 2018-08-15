/*******************************************************************************
 * Copyright (c) 2015 example. All rights reserved.
 *
 * Contributors:
 *     example - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.core.fixture;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;

/**
 * @author andrey
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaseTest {

    @Before
    public void setup() throws Exception {
        //
    }

    @After
    public void tearDown() throws Exception {
        //
    }

    protected static String fix(String s){
        if(s == null){
            return null;
        }
        return s.replace('\\', '/');
    }

    @Rule
    public TestFailReporter failReporter= new TestFailReporter();

    public class TestFailReporter extends TestWatcher {

        @Override
        protected void starting(Description description) {
            System.out.println("STARTING " + description.toString());
        }
        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("FAIL in " + description.toString());
            e.printStackTrace(System.out);
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("PASS in " + description.toString());
        }
    }
}
