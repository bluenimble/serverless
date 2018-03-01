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
package com.bluenimble.platform;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class SubList implements List<Object> {
	
	private List<?> source;
	
	private int		start;
	private int		end;
	
	public SubList (List<?> source, int start, int end) {
		this.source = source;
		this.start = start;
		this.end = end;
		if (this.end >= source.size ()) {
			this.end = source.size () - 1;
		}
	}

	@Override
	public int size () {
		if (source.isEmpty ()) {
			return 0;
		}
		if (start >= source.size ()) {
			return 0;
		}
		return end - start + 1;
	}

	@Override
	public boolean isEmpty () {
		return size () > 0;
	}

	@Override
	public boolean contains (Object o) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public Iterator<Object> iterator () {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public Object[] toArray () {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public <T> T[] toArray (T[] a) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean add (Object e) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean remove (Object o) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean addAll (Collection<? extends Object> c) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean addAll (int index, Collection<? extends Object> c) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public boolean retainAll (Collection<?> c) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public void clear () {
		source.clear ();
	}

	@Override
	public Object get (int index) {
		return source.get (index + start);
	}

	@Override
	public Object set (int index, Object element) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public void add(int index, Object element) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public Object remove (int index) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public int indexOf (Object o) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public int lastIndexOf (Object o) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public ListIterator<Object> listIterator () {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public ListIterator<Object> listIterator (int index) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public List<Object> subList (int fromIndex, int toIndex) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

}
