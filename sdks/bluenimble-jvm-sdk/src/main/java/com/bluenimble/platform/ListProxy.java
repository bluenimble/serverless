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

public class ListProxy implements List<Object> {

	private List<?> proxy;
	
	public ListProxy (List<?> proxy) {
		this.proxy = proxy;
	}
	
	@Override
	public int size () {
		return proxy.size ();
	}

	@Override
	public boolean isEmpty () {
		return proxy.isEmpty ();
	}

	@Override
	public boolean contains (Object o) {
		return proxy.contains (o);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<Object> iterator () {
		return (Iterator<Object>)proxy.iterator ();
	}

	@Override
	public Object[] toArray () {
		return proxy.toArray ();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return proxy.toArray (a);
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
	public boolean containsAll(Collection<?> c) {
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
	public void clear() {
		proxy.clear ();
	}

	@Override
	public Object get (int index) {
		return proxy.get (index);
	}

	@Override
	public Object set (int index, Object element) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public void add (int index, Object element) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public Object remove (int index) {
		throw new UnsupportedOperationException (this.getClass ().getSimpleName () + " doesn't support this method");
	}

	@Override
	public int indexOf (Object o) {
		return proxy.indexOf (o);
	}

	@Override
	public int lastIndexOf (Object o) {
		return proxy.lastIndexOf (o);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListIterator<Object> listIterator () {
		return (ListIterator<Object>)proxy.listIterator ();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListIterator<Object> listIterator (int index) {
		return (ListIterator<Object>)proxy.listIterator (index);
	}

	@Override
	public List<Object> subList (int fromIndex, int toIndex) {
		return new SubList (this, fromIndex, toIndex);
	}

}
