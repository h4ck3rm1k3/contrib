/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Oracle Corporation.  All rights reserved.
 *
 * $Id: BasicIndex.java,v 1.7 2006/09/21 15:34:33 mark Exp $
 */

package com.sleepycat.persist;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.compat.DbCompat;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;
import com.sleepycat.util.keyrange.KeyRange;
import com.sleepycat.util.keyrange.RangeCursor;

/**
 * Implements EntityIndex using a ValueAdapter.  This class is abstract and
 * does not implement get()/map()/sortedMap() because it doesn't have access
 * to the entity binding.
 *
 * @author Mark Hayes
 */
abstract class BasicIndex<K,E> implements EntityIndex<K,E> {

    static final DatabaseEntry NO_RETURN_ENTRY;
    static {
        NO_RETURN_ENTRY = new DatabaseEntry();
        NO_RETURN_ENTRY.setPartial(0, 0, true);
    }

    Database db;
    boolean transactional;
    Class<K> keyClass;
    EntryBinding keyBinding;
    KeyRange emptyRange;
    ValueAdapter<K> keyAdapter;
    ValueAdapter<E> entityAdapter;

    BasicIndex(Database db,
               Class<K> keyClass,
               EntryBinding keyBinding,
               ValueAdapter<E> entityAdapter)
        throws DatabaseException {

        this.db = db;
        DatabaseConfig config = db.getConfig();
        transactional = config.getTransactional();

        this.keyClass = keyClass;
        this.keyBinding = keyBinding;
        this.entityAdapter = entityAdapter;

        emptyRange = new KeyRange(config.getBtreeComparator());
        keyAdapter = new KeyValueAdapter(keyClass, keyBinding);
    }

    /*
     * Of the EntityIndex methods only get()/map()/sortedMap() are not
     * implemented here and therefore must be implemented by subclasses.
     */

    public boolean contains(K key)
        throws DatabaseException {

        return contains(null, key, null);
    }

    public boolean contains(Transaction txn, K key, LockMode lockMode)
        throws DatabaseException {

        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry dataEntry = NO_RETURN_ENTRY;
        keyBinding.objectToEntry(key, keyEntry);

        OperationStatus status = db.get(txn, keyEntry, dataEntry, lockMode);
        return (status == OperationStatus.SUCCESS);
    }

    public long count()
        throws DatabaseException {

        if (DbCompat.DATABASE_COUNT) {
            return DbCompat.getDatabaseCount(db);
        } else {
            long count = 0;
            boolean countDups = db instanceof SecondaryDatabase;
            DatabaseEntry key = NO_RETURN_ENTRY;
            DatabaseEntry data = NO_RETURN_ENTRY;
            CursorConfig cursorConfig = CursorConfig.READ_UNCOMMITTED;
            Cursor cursor = db.openCursor(null, cursorConfig);
            try {
                OperationStatus status = cursor.getFirst(key, data, null);
                while (status == OperationStatus.SUCCESS) {
                    if (countDups) {
                        count += cursor.count();
                    } else {
                        count += 1;
                    }
                    status = cursor.getNextNoDup(key, data, null);
                }
            } finally {
                cursor.close();
            }
            return count;
        }
    }

    public boolean delete(K key)
        throws DatabaseException {

        return delete(null, key);
    }

    public boolean delete(Transaction txn, K key)
        throws DatabaseException {

        DatabaseEntry keyEntry = new DatabaseEntry();
        keyBinding.objectToEntry(key, keyEntry);

        OperationStatus status = db.delete(txn, keyEntry);
        return (status == OperationStatus.SUCCESS);
    }

    public EntityCursor<K> keys()
        throws DatabaseException {

        return keys(null, null);
    }

    public EntityCursor<K> keys(Transaction txn, CursorConfig config)
        throws DatabaseException {

        return cursor(txn, emptyRange, keyAdapter, config);
    }

    public EntityCursor<E> entities()
        throws DatabaseException {

        return cursor(null, emptyRange, entityAdapter, null);
    }

    public EntityCursor<E> entities(Transaction txn,
                                    CursorConfig config)
        throws DatabaseException {
        
        return cursor(txn, emptyRange, entityAdapter, config);
    }

    public EntityCursor<K> keys(K fromKey, boolean fromInclusive,
                                K toKey, boolean toInclusive)
        throws DatabaseException {

        return cursor(null, fromKey, fromInclusive, toKey, toInclusive,
                      keyAdapter, null);
    }

    public EntityCursor<K> keys(Transaction txn,
                                K fromKey, boolean fromInclusive,
                                K toKey, boolean toInclusive,
                                CursorConfig config)
        throws DatabaseException {

        return cursor(txn, fromKey, fromInclusive, toKey, toInclusive,
                      keyAdapter, config);
    }

    public EntityCursor<E> entities(K fromKey, boolean fromInclusive,
                                    K toKey, boolean toInclusive)
        throws DatabaseException {

        return cursor(null, fromKey, fromInclusive, toKey, toInclusive,
                      entityAdapter, null);
    }

    public EntityCursor<E> entities(Transaction txn,
                                    K fromKey, boolean fromInclusive,
                                    K toKey, boolean toInclusive,
                                    CursorConfig config)
        throws DatabaseException {

        return cursor(txn, fromKey, fromInclusive, toKey, toInclusive,
                      entityAdapter, config);
    }

    /*
    public ForwardCursor<K> unsortedKeys(KeySelector<K> selector)
        throws DatabaseException {

        return unsortedKeys(null, selector, null);
    }

    public ForwardCursor<K> unsortedKeys(Transaction txn,
                                         KeySelector<K> selector,
                                         CursorConfig config)
        throws DatabaseException {
        
        throw new UnsupportedOperationException();
    }

    public ForwardCursor<E> unsortedEntities(KeySelector<K> selector)
        throws DatabaseException {
            
        return unsortedEntities(null, selector, null);
    }

    public ForwardCursor<E> unsortedEntities(Transaction txn,
                                             KeySelector<K> selector,
                                             CursorConfig config)
        throws DatabaseException {
        
        throw new UnsupportedOperationException();
    }
    */

    private <V> EntityCursor<V> cursor(Transaction txn,
                                       K fromKey, boolean fromInclusive,
                                       K toKey, boolean toInclusive,
                                       ValueAdapter<V> adapter,
                                       CursorConfig config)
        throws DatabaseException {

        DatabaseEntry fromEntry = null;
        if (fromKey != null) {
            fromEntry = new DatabaseEntry();
            keyBinding.objectToEntry(fromKey, fromEntry);
        }
        DatabaseEntry toEntry = null;
        if (toKey != null) {
            toEntry = new DatabaseEntry();
            keyBinding.objectToEntry(toKey, toEntry);
        }
        KeyRange range = emptyRange.subRange
            (fromEntry, fromInclusive, toEntry, toInclusive);
        return cursor(txn, range, adapter, config);
    }

    private <V> EntityCursor<V> cursor(Transaction txn,
                                       KeyRange range,
                                       ValueAdapter<V> adapter,
                                       CursorConfig config)
        throws DatabaseException {

        Cursor cursor = db.openCursor(txn, config);
        RangeCursor rangeCursor = new RangeCursor(range, cursor);
        return new BasicCursor<V>(rangeCursor, adapter);
    }
}
