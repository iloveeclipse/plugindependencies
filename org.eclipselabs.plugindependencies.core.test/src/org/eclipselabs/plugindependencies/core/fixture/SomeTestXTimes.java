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

import org.eclipselabs.plugindependencies.core.TestBuildFileGen;
import org.eclipselabs.plugindependencies.core.TestBuildFileGen2;
import org.eclipselabs.plugindependencies.core.TestCLI;
import org.eclipselabs.plugindependencies.core.TestDepResSearchMethodFeature;
import org.eclipselabs.plugindependencies.core.TestDepResSearchMethodPack;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author andrey
 */
@RunWith(Suite.class)
@SuiteClasses({
    TestBuildFileGen.class,
    TestBuildFileGen2.class,
    TestCLI.class,
//    TestDepResIsRightVersion.class,
//    TestDepResResolving.class,
    TestDepResSearchMethodFeature.class,
    TestDepResSearchMethodPack.class,
//    TestDepResSearchMethodPlugin.class,
//    TestExceptions.class,
//    TestFeatureParser.class,
//    TestPlugin.class,
//    TestPluginParser.class,
//    TestStringUtil.class
})
@Repeat(50)
public class SomeTestXTimes {

    @ClassRule
    public static RepeatRule repeatRule = new RepeatRule();
}
