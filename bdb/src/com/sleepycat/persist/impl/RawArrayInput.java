/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Oracle Corporation.  All rights reserved.
 *
 * $Id: RawArrayInput.java,v 1.3 2006/09/22 05:28:52 mark Exp $
 */

package com.sleepycat.persist.impl;

import java.util.IdentityHashMap;

import com.sleepycat.persist.raw.RawObject;

/**
 * Extends RawAbstractInput to convert array (ObjectArrayFormat and
 * PrimitiveArrayteKeyFormat) RawObject instances.
 *
 * @author Mark Hayes
 */
class RawArrayInput extends RawAbstractInput {

    private Object[] array;
    private int index;
    private Format componentFormat;

    RawArrayInput(Catalog catalog,
                  boolean rawAccess,
                  IdentityHashMap converted,
                  RawObject raw,
                  Format componentFormat) {
        super(catalog, rawAccess, converted);
        array = raw.getElements();
        this.componentFormat = componentFormat;
    }

    @Override
    public int readArrayLength() {
        return array.length;
    }

    @Override
    Object readNext() {
        Object o = array[index++];
        return checkAndConvert(o, componentFormat);
    }
}
