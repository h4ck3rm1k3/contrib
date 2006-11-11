/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Oracle Corporation.  All rights reserved.
 *
 * $Id: ForeignKeyTrigger.java,v 1.5 2006/09/12 19:16:43 cwl Exp $
 */

package com.sleepycat.je;

import com.sleepycat.je.txn.Locker;

class ForeignKeyTrigger implements DatabaseTrigger {

    private SecondaryDatabase secDb;

    ForeignKeyTrigger(SecondaryDatabase secDb) {

        this.secDb = secDb;
    }

    public void triggerAdded(Database db) {
    }

    public void triggerRemoved(Database db) {

        secDb.clearForeignKeyTrigger();
    }

    public void databaseUpdated(Database db,
                                Locker locker,
                                DatabaseEntry priKey,
                                DatabaseEntry oldData,
                                DatabaseEntry newData)
        throws DatabaseException {

        if (newData == null) {
            secDb.onForeignKeyDelete(locker, priKey);
        }
    }
}
