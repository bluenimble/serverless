/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.iterators;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterator<E> implements Iterator<E> {

    private final Collection<? super E> collection;
    private Enumeration<? extends E> 	enumeration;
    private E 							last;

    public EnumerationIterator () {
        this (null, null);
    }

    public EnumerationIterator (final Enumeration<? extends E> enumeration) {
        this (enumeration, null);
    }

    public EnumerationIterator (final Enumeration<? extends E> enumeration, final Collection<? super E> collection) {
        super();
        this.enumeration = enumeration;
        this.collection = collection;
        this.last = null;
    }

    public boolean hasNext () {
        return enumeration.hasMoreElements();
    }

    public E next () {
        last = enumeration.nextElement ();
        return last;
    }

    public void remove () {
        if (collection != null) {
            if (last != null) {
                collection.remove (last);
            } else {
                throw new IllegalStateException("next() must have been called for remove() to function");
            }
        } else {
            throw new UnsupportedOperationException ("No Collection associated with this Iterator");
        }
    }

}