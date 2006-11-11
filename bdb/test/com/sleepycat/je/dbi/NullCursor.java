/*
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Oracle Corporation.  All rights reserved.
 *
 * $Id: NullCursor.java,v 1.13 2006/09/12 19:17:16 cwl Exp $
 */

package com.sleepycat.je.dbi;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.tree.BIN;
import com.sleepycat.je.txn.Locker;

/**
 * A NullCursor is used as a no-op object by tree unit tests, which 
 * wish to speak directly to Tree methods.
 */
public class NullCursor extends CursorImpl {
    /**
     * Cursor constructor.
     */
    public NullCursor(DatabaseImpl database, Locker txn) 
        throws DatabaseException {

        super(database, txn);
    }

    public void addCursor(BIN bin) {}
    public void addCursor() {}
}

