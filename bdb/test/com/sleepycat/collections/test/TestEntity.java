/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *	Oracle Corporation.  All rights reserved.
 *
 * $Id: TestEntity.java,v 1.12 2006/09/12 19:17:10 cwl Exp $
 */

package com.sleepycat.collections.test;

/**
 * @author Mark Hayes
 */
class TestEntity {

    int key;
    int value;

    TestEntity(int key, int value) {

        this.key = key;
        this.value = value;
    }

    public boolean equals(Object o) {

        try {
            TestEntity e = (TestEntity) o;
            return e.key == key && e.value == value;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {

        return key;
    }

    public String toString() {

        return "[key " + key + " value " + value + ']';
    }
}
