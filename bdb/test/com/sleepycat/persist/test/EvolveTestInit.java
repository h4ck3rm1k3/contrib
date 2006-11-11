/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2000-2006
 *      Oracle Corporation.  All rights reserved.
 *
 * $Id: EvolveTestInit.java,v 1.2 2006/09/22 05:28:52 mark Exp $
 */
package com.sleepycat.persist.test;

import java.io.IOException;

import junit.framework.Test;

import com.sleepycat.je.util.TestUtils;

/**
 * Runs part one of the EvolveTest.  This part is run with the old/original
 * version of EvolveClasses in the classpath.  It creates a fresh environment
 * and store containing instances of the original class.  When EvolveTest is
 * run, it will read/write/evolve these objects from the store created here.
 *
 * @author Mark Hayes
 */
public class EvolveTestInit extends EvolveTestBase {

    public static Test suite()
        throws Exception {

        return getSuite(EvolveTestInit.class);
    }

    @Override
    public void setUp()
        throws IOException {

        envHome = getTestInitHome();
        envHome.mkdir();
        TestUtils.removeLogFiles("Setup", envHome, false);
    }

    public void testInit()
        throws Exception {

        openEnv();
        if (!openStoreReadWrite()) {
            fail();
        }
        caseObj.writeObjects(store);
        caseObj.checkUnevolvedModel(store.getModel(), env);
        closeAll();
    }
}
