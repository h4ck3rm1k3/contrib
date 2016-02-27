/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002,2008 Oracle.  All rights reserved.
 *
 * $Id: DebugRecordTest.java,v 1.51 2008/05/22 19:35:40 linda Exp $
 */

package com.sleepycat.je.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import junit.framework.TestCase;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.log.FileManager;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.log.SearchFileReader;
import com.sleepycat.je.recovery.RecoveryInfo;
import com.sleepycat.je.utilint.DbLsn;
import com.sleepycat.je.utilint.Tracer;
import com.sleepycat.je.utilint.TracerFormatter;

public class DebugRecordTest extends TestCase {
    private File envHome;
    private EnvironmentImpl env;

    public DebugRecordTest() {
        envHome = new File(System.getProperty(TestUtils.DEST_DIR));
        env = null;
    }

    public void setUp()
	throws IOException {

        TestUtils.removeFiles("Setup", envHome, FileManager.JE_SUFFIX);
        TestUtils.removeFiles(envHome, new InfoFileFilter());
    }

    public void tearDown()
	throws IOException {

        TestUtils.removeFiles("TearDown", envHome, FileManager.JE_SUFFIX);
        TestUtils.removeFiles(envHome, new InfoFileFilter());
    }


    public void testDebugLogging()
	throws DatabaseException, IOException {

        try {

            /*
	     * Turn on the txt file and db log logging, turn off the console.
	     */
            EnvironmentConfig envConfig = TestUtils.initEnvConfig();
            envConfig.setConfigParam
		(EnvironmentParams.JE_LOGGING_FILE.getName(), "true");
            envConfig.setConfigParam
		(EnvironmentParams.JE_LOGGING_CONSOLE.getName(),
		 "false");
            envConfig.setConfigParam
		(EnvironmentParams.JE_LOGGING_LEVEL.getName(), "CONFIG");
            envConfig.setConfigParam
		(EnvironmentParams.JE_LOGGING_DBLOG.getName(), "true");
            envConfig.setConfigParam
		(EnvironmentParams.NODE_MAX.getName(), "6");
	    envConfig.setAllowCreate(true);
            /* Disable noisy UtilizationProfile database creation. */
            DbInternal.setCreateUP(envConfig, false);
            /* Don't run the cleaner without a UtilizationProfile. */
            envConfig.setConfigParam
                (EnvironmentParams.ENV_RUN_CLEANER.getName(), "false");
	
            env = new EnvironmentImpl(envHome,
                                      envConfig,
                                      null /*sharedCacheEnv*/,
                                      false /*replicationIntended*/);

            List<Tracer> expectedRecords = new ArrayList<Tracer>();

            /* Recovery itself will log two messages. */
            RecoveryInfo info = new RecoveryInfo();
            expectedRecords.add(new Tracer("Recovery w/no files."));
            expectedRecords.add(new Tracer
				("Checkpoint 1: source=recovery" +
				 " success=true nFullINFlushThisRun=0" +
				 " nDeltaINFlushThisRun=0"));
            expectedRecords.add(new Tracer("Recovery finished: "  +
					   info.toString()));

            /* Log a message. */
            Tracer.trace(Level.INFO, env, "hi there");
            expectedRecords.add(new Tracer("hi there"));

            /* Log an exception. */
            DatabaseException e = new DatabaseException("fake exception");
            Tracer.trace(env, "DebugRecordTest", "testException", "foo", e);
            expectedRecords.add(new Tracer("foo\n" + Tracer.getStackTrace(e)));

            /* Log a split and flush the log to disk. */
            env.getLogManager().flush();
            env.getFileManager().clear();
            env.closeLogger();

            /* Verify. */
            checkDatabaseLog(expectedRecords);
            checkTextFile(expectedRecords);

        } finally {
            if (env != null) {
                env.close();
            }
        }
    }

    /**
     * Check what's in the database log.
     */
    private void checkDatabaseLog(List<Tracer> expectedList)
        throws DatabaseException, IOException {

        SearchFileReader searcher =
            new SearchFileReader(env, 1000, true, DbLsn.NULL_LSN,
				 DbLsn.NULL_LSN, LogEntryType.LOG_TRACE);

        int numSeen = 0;
        while (searcher.readNextEntry()) {
            Tracer dRec = (Tracer) searcher.getLastObject();
            assertEquals("Should see this as " + numSeen + " record: ",
			 expectedList.get(numSeen).getMessage(),
                         dRec.getMessage());
            numSeen++;
        }

        assertEquals("Should see this many debug records",
                     expectedList.size(), numSeen);
    }

    /**
     * Check what's in the text file.
     */
    private void checkTextFile(List<Tracer> expectedList)
        throws IOException {

        FileReader fr = null;
        BufferedReader br = null;
        try {
            String textFileName = envHome + File.separator + "je.info.0";
            fr = new FileReader(textFileName);
            br = new BufferedReader(fr);

            String line = br.readLine();
            int numSeen = 0;

            /*
	     * Read the file, checking only lines that start with valid Levels.
	     */
            while (line != null) {
                try {
                    /* The line should start with a valid date. */
                    ParsePosition pp = new ParsePosition(0);
                    DateFormat ff = TracerFormatter.makeDateFormat();
                    ff.parse(line, pp);

                    /* There should be a java.util.logging.level next. */
                    int dateEnd = pp.getIndex();
                    int levelEnd = line.indexOf(" ", dateEnd + 1);
                    String possibleLevel = line.substring(dateEnd + 1,
                                                          levelEnd);
                    Level.parse(possibleLevel);

                    String expected =
                        expectedList.get(numSeen).getMessage();
                    StringBuffer seen = new StringBuffer();
                    seen.append(line.substring(levelEnd + 1));
                    /*
                     * Assemble the log message by reading the right number
                     * of lines
                     */
                    StringTokenizer st =
                        new StringTokenizer(expected,
                                            Character.toString('\n'), false);

                    for (int i = 1; i < st.countTokens(); i++) {
                        seen.append('\n');
                        String l = br.readLine();
                        seen.append(l);
                        if (i == (st.countTokens() -1)) {
                            seen.append('\n');
                        }
                    }
                    /* XXX, diff of multiline stuff isn't right yet. */
                    if (st.countTokens() == 1) {
                        assertEquals("Line " + numSeen + " should be the same",
                                     expected, seen.toString());
                    }
                    numSeen++;
                } catch (Exception e) {
                    /* Skip this line, not a message. */
                }
                line = br.readLine();
            }
            assertEquals("Should see this many debug records",
                         expectedList.size(), numSeen);
        } finally {
	    if (br != null) {
		br.close();
	    }

	    if (fr != null) {
		fr.close();
	    }
	}
    }
}