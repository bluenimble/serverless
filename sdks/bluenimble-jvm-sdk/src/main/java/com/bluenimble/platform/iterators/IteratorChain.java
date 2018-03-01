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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class IteratorChain<E> implements Iterator<E> {

    private final Queue<Iterator<? extends E>> 	iteratorChain 		= new LinkedList<Iterator<? extends E>>();

    private Iterator<? extends E> 				currentIterator;
    private Iterator<? extends E> 				lastUsedIterator;
    private Iterator<? extends E> 				emptyIterator;
    
    private boolean isLocked = false;

    public IteratorChain () {
        super ();
        emptyIterator = new EmptyIterator<E> ();	
    }

    public IteratorChain (final Iterator<? extends E> iterator) {
        this ();
        addIterator (iterator);
    }

    public IteratorChain (final Iterator<? extends E> first, final Iterator<? extends E> second) {
    	this ();
        addIterator (first);
        addIterator (second);
    }

	public IteratorChain (@SuppressWarnings("unchecked") final Iterator<? extends E>... iteratorChain) {
    	this ();
        for (final Iterator<? extends E> element : iteratorChain) {
            addIterator (element);
        }
    }

    public IteratorChain (final Collection<Iterator<? extends E>> iteratorChain) {
    	this ();
        for (final Iterator<? extends E> iterator : iteratorChain) {
            addIterator(iterator);
        }
    }

    public void addIterator (final Iterator<? extends E> iterator) {
        checkLocked ();
        if (iterator == null) {
            throw new NullPointerException("Iterator must not be null");
        }
        iteratorChain.add (iterator);
    }

    public int size () {
        return iteratorChain.size ();
    }

    public boolean isLocked () {
        return isLocked;
    }

    private void checkLocked () {
        if (isLocked == true) {
            throw new UnsupportedOperationException(
                    "IteratorChain cannot be changed after the first use of a method from the Iterator interface");
        }
    }

    private void lockChain () {
        if (isLocked == false) {
            isLocked = true;
        }
    }

    protected void updateCurrentIterator () {
        if (currentIterator == null) {
            if (iteratorChain.isEmpty ()) {
                currentIterator = emptyIterator;
            } else {
                currentIterator = iteratorChain.remove ();
            }
            // set last used iterator here, in case the user calls remove
            // before calling hasNext() or next() (although they shouldn't)
            lastUsedIterator = currentIterator;
        }

        while (currentIterator.hasNext() == false && !iteratorChain.isEmpty()) {
            currentIterator = iteratorChain.remove ();
        }
    }

    public boolean hasNext () {
        lockChain ();
        updateCurrentIterator ();
        lastUsedIterator = currentIterator;

        return currentIterator.hasNext ();
    }

    public E next () {
        lockChain();
        updateCurrentIterator();
        lastUsedIterator = currentIterator;

        return currentIterator.next();
    }

    public void remove () {
        lockChain ();
        if (currentIterator == null) {
            updateCurrentIterator();
        }
        lastUsedIterator.remove ();
    }

}
