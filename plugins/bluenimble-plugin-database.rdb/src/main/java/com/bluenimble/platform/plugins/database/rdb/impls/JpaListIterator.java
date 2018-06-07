package com.bluenimble.platform.plugins.database.rdb.impls;

import java.util.ListIterator;

public class JpaListIterator<T> implements ListIterator<T> {
	
	@SuppressWarnings("rawtypes")
	public static final ListIterator Empty = new JpaListIterator ();

	private JpaDatabase database;
	private ListIterator<Object> proxy;

	public JpaListIterator () {
	}

	public JpaListIterator (JpaDatabase database, ListIterator<Object> proxy) {
		this.proxy = proxy;
	}

	@Override
	public boolean hasNext () {
		if (proxy == null) {
			return false;
		}
		return proxy.hasNext ();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T next () {
		if (proxy == null) {
			return null;
		}
		return (T) new JpaObject (database, proxy.next());
	}

	@Override
	public boolean hasPrevious () {
		if (proxy == null) {
			return false;
		}
		return proxy.hasPrevious ();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T previous () {
		if (proxy == null) {
			return null;
		}
		return (T) new JpaObject (database, proxy.previous());
	}

	@Override
	public int nextIndex () {
		if (proxy == null) {
			return -1;
		}
		return proxy.nextIndex ();
	}

	@Override
	public int previousIndex () {
		if (proxy == null) {
			return -1;
		}
		return proxy.previousIndex ();
	}

	@Override
	public void remove () {
		if (proxy == null) {
			return;
		}
		proxy.remove ();
	}

	@Override
	public void set (T e) {
		if (proxy == null) {
			return;
		}
		if (e == null || !isJpaObject (e)) {
			return;
		}
		proxy.set (((JpaObject) e).bean);
	}

	@Override
	public void add (T e) {
		if (proxy == null) {
			return;
		}
		if (e == null || !isJpaObject (e)) {
			return;
		}
		proxy.add (((JpaObject) e).bean);
	}

	private boolean isJpaObject (Object object) {
		return object instanceof JpaObject;
	}
}