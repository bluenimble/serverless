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
package com.bluenimble.platform.plugins.datasource.impls;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.datasource.RemoteDataSource;

public class CustomEntityManager implements EntityManager, RemoteDataSource {
	
	private static final long serialVersionUID = 4640805690991659752L;
	
	private EntityManager proxy;
	
	public CustomEntityManager (EntityManager proxy) {
		this.proxy = proxy;
	}

	@Override
	public Object get () {
		return null;
	}

	@Override
	public void set (ApiSpace space, ClassLoader classLoader, Object... args) {
	}
	
	@Override
	public void recycle () {
		proxy.close ();
	}

	@Override
	public void clear () {
		proxy.clear ();
	}

	@Override
	public void close () {
		proxy.close ();
	}

	@Override
	public boolean contains (Object obj) {
		return proxy.contains (obj);
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph (Class<T> cls) {
		return proxy.createEntityGraph (cls);
	}

	@Override
	public EntityGraph<?> createEntityGraph (String name) {
		return proxy.createEntityGraph (name);
	}

	@Override
	public Query createNamedQuery (String name) {
		return proxy.createNamedQuery (name);
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery (String name, Class<T> cls) {
		return proxy.createNamedQuery (name, cls);
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery (String name) {
		return proxy.createNamedStoredProcedureQuery (name);
	}

	@Override
	public Query createNativeQuery (String name) {
		return proxy.createNativeQuery (name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Query createNativeQuery (String name, Class cls) {
		return proxy.createNativeQuery (name, cls);
	}

	@Override
	public Query createNativeQuery (String name, String str) {
		return proxy.createNativeQuery (name, str);
	}

	@Override
	public Query createQuery (String name) {
		return proxy.createQuery (name);
	}

	@Override
	public <T> TypedQuery<T> createQuery (CriteriaQuery<T> query) {
		return proxy.createQuery (query);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Query createQuery(CriteriaUpdate update) {
		return proxy.createQuery (update);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Query createQuery (CriteriaDelete delete) {
		return proxy.createQuery (delete);
	}

	@Override
	public <T> TypedQuery<T> createQuery (String name, Class<T> cls) {
		return proxy.createQuery (name, cls);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String name) {
		return proxy.createStoredProcedureQuery (name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public StoredProcedureQuery createStoredProcedureQuery (String name, Class... classes) {
		return proxy.createStoredProcedureQuery (name, classes);
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery (String name, String... args) {
		return proxy.createStoredProcedureQuery (name, args);
	}

	@Override
	public void detach (Object obj) {
		proxy.detach (obj);
	}

	@Override
	public <T> T find (Class<T> cls, Object obj) {
		return proxy.find (cls, obj);
	}

	@Override
	public <T> T find (Class<T> cls, Object obj, Map<String, Object> values) {
		return proxy.find (cls, obj, values);
	}

	@Override
	public <T> T find (Class<T> cls, Object obj, LockModeType lock) {
		return proxy.find (cls, obj, lock);
	}

	@Override
	public <T> T find (Class<T> cls, Object obj, LockModeType lock, Map<String, Object> values) {
		return proxy.find (cls, obj, lock, values);
	}

	@Override
	public void flush () {
		proxy.flush ();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder () {
		return proxy.getCriteriaBuilder ();
	}

	@Override
	public Object getDelegate () {
		return proxy.getDelegate ();
	}

	@Override
	public EntityGraph<?> getEntityGraph (String name) {
		return proxy.getEntityGraph (name);
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs (Class<T> cls) {
		return proxy.getEntityGraphs (cls);
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory () {
		return proxy.getEntityManagerFactory ();
	}

	@Override
	public FlushModeType getFlushMode () {
		return proxy.getFlushMode ();
	}

	@Override
	public LockModeType getLockMode (Object obj) {
		return proxy.getLockMode (obj);
	}

	@Override
	public Metamodel getMetamodel () {
		return proxy.getMetamodel ();
	}

	@Override
	public Map<String, Object> getProperties () {
		return proxy.getProperties ();
	}

	@Override
	public <T> T getReference (Class<T> cls, Object obj) {
		return proxy.getReference (cls, obj);
	}

	@Override
	public EntityTransaction getTransaction () {
		return proxy.getTransaction ();
	}

	@Override
	public boolean isJoinedToTransaction () {
		return proxy.isJoinedToTransaction ();
	}

	@Override
	public boolean isOpen () {
		return proxy.isOpen ();
	}

	@Override
	public void joinTransaction () {
		proxy.joinTransaction ();
	}

	@Override
	public void lock (Object obj, LockModeType lock) {
		proxy.lock (obj, lock);
	}

	@Override
	public void lock (Object obj, LockModeType lock, Map<String, Object> values) {
		proxy.lock (obj, lock, values);
	}

	@Override
	public <T> T merge (T obj) {
		return proxy.merge (obj);
	}

	@Override
	public void persist (Object obj) {
		proxy.persist (obj);
	}

	@Override
	public void refresh (Object obj) {
		proxy.refresh (obj);
	}

	@Override
	public void refresh (Object obj, Map<String, Object> values) {
		proxy.refresh (obj, values);
	}

	@Override
	public void refresh (Object obj, LockModeType lock) {
		proxy.refresh (obj, lock);
	}

	@Override
	public void refresh (Object obj, LockModeType lock, Map<String, Object> values) {
		proxy.refresh (obj, lock, values);
	}

	@Override
	public void remove (Object obj) {
		proxy.remove (obj);
	}

	@Override
	public void setFlushMode(FlushModeType mode) {
		proxy.setFlushMode (mode);
	}

	@Override
	public void setProperty (String name, Object value) {
		proxy.setProperty (name, value);
		
	}

	@Override
	public <T> T unwrap (Class<T> type) {
		return unwrap (type);
	}

}
