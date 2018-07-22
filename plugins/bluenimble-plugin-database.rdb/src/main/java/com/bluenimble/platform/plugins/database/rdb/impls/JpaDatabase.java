package com.bluenimble.platform.plugins.database.rdb.impls;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.Caching.Target;
import com.bluenimble.platform.db.query.CompiledQuery;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.db.query.QueryCompiler;
import com.bluenimble.platform.db.query.Select;
import com.bluenimble.platform.db.query.impls.SqlQueryCompiler;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanMetadata;

public class JpaDatabase implements Database {
	
	private static final long serialVersionUID = 5229787937178581380L;

	private static final String 	Regexp 				= "REGEXP";
	private static final String 	QueryEntity 		= "e";
	
	interface Proprietary {
		String EntityManager 	= "entityManager";
		String Connection 		= "connection";
	}
	
	private Map<String, String> QueriesCache = new ConcurrentHashMap<String, String> ();
	
	private JpaMetadata 		metadata;
	
	EntityManager 				entityManager;
	Tracer						tracer;
	private boolean 			allowProprietaryAccess;

	private EntityTransaction 	transaction;
	
	public JpaDatabase (Tracer tracer, EntityManager entityManager, JpaMetadata metadata, boolean allowProprietaryAccess) {
		this.tracer 				= tracer;
		this.entityManager 			= entityManager;
		this.metadata 				= metadata;
		this.allowProprietaryAccess = allowProprietaryAccess;
	}

	@Override
	public void trx () {
		transaction = entityManager.getTransaction ();
		transaction.begin ();
	}

	@Override
	public void commit () throws DatabaseException {
		if (transaction == null) {
			return;
		}
		transaction.commit ();
	}

	@Override
	public void rollback () throws DatabaseException {
		if (transaction == null) {
			return;
		}
		transaction.rollback ();
	}

	@Override
	public DatabaseObject create (String entity) throws DatabaseException {
		try {
			return new JpaObject (this, resolve (entity).newInstance ());
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

	@Override
	public List<DatabaseObject> createList () {
		return new JpaObjectList<DatabaseObject> (this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clear (String entity) throws DatabaseException {
		
		checkNotNull (entity);
		
		try {
			Class<?> entitycls = resolve (entity);
			CriteriaBuilder builder = entityManager.getCriteriaBuilder ();
		    @SuppressWarnings("rawtypes")
			CriteriaDelete query = builder.createCriteriaDelete (resolve (entity));
		    query.from (entitycls);
		    entityManager.createQuery (query).executeUpdate ();
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
	}

	@Override
	public DatabaseObject get (String entity, Object id) throws DatabaseException {
		
		checkNotNull (entity);
		
		Object bean = null;
		try {
			bean = entityManager.find (resolve (entity), id);
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
		if (bean == null) {
			return null;
		}
		return new JpaObject (this, bean);
	}

	@Override
	public int delete (String entity, Object id) throws DatabaseException {
		
		checkNotNull (entity);
		
		Object bean = null;
		try {
			bean = entityManager.find (resolve (entity), id);
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
		if (bean == null) {
			return 0;
		}
		entityManager.remove (bean);
		return 1;
	}

	@Override
	public boolean isEntity (Object value) {
		if (value == null) {
			return false;
		}
		return value.getClass ().isAnnotationPresent (Entity.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DatabaseObject> find (String entity, Query query, Visitor visitor) throws DatabaseException {
		List<Object> result;
		try {
			result = (List<Object>)_query (entity, Query.Construct.select, query);
		} catch (Exception e) {
			throw new DatabaseException (e.getMessage (), e);
		}
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return toList (entity, result, visitor);
	}

	@Override
	public DatabaseObject findOne (String entity, Query query) throws DatabaseException {
		// force count to 1
		query.count (1);
		
		List<DatabaseObject> result = find (entity, query, null);
		if (result == null || result.isEmpty ()) {
			return null;
		}
		
		return result.get (0);
	}

	@Override
	public long count (String entity) throws DatabaseException {
		
		checkNotNull (entity);
		
		CriteriaBuilder qb = entityManager.getCriteriaBuilder ();
		CriteriaQuery<Long> cq = qb.createQuery (Long.class);
		try {
			cq.select (qb.count (cq.from (resolve (entity))));
		} catch (Exception ex) {
			throw new DatabaseException (ex.getMessage (), ex);
		}
		return entityManager.createQuery (cq).getSingleResult ();
	}

	@Override
	public int delete (String entity, Query query) throws DatabaseException {
		return (int)_query (entity, Query.Construct.delete, query);
	}

	@Override
	public List<DatabaseObject> pop (String entity, Query query, Visitor visitor) throws DatabaseException {
		List<DatabaseObject> list = find (entity, query, visitor);
		
		delete (entity, query);
		
		return list;
	}

	@Override
	public DatabaseObject popOne (String entity, Query query) throws DatabaseException {
		DatabaseObject dbo = findOne (entity, query);
		
		dbo.delete ();
		
		return dbo;
	}

	@Override
	public int increment (DatabaseObject dbo, String field, int value) throws DatabaseException {
		
		Integer current = (Integer)dbo.get (field);
		if (current == null) {
			current = 0;
		}
		int next = current + value;
		
		dbo.set (field, next); 
		
		return next;
	}

	@Override
	public JsonObject bulk (JsonObject data) throws DatabaseException {
		// TODO 
		return null;
	}

	@Override
	public JsonObject describe () throws DatabaseException {
		// TODO 
		return null;
	}

	@Override
	public void recycle () {
		entityManager.close ();
	}

	@Override
	public Object proprietary (String name) {
		if (!allowProprietaryAccess) {
			return null;
		} 
		if (Proprietary.EntityManager.equalsIgnoreCase (name)) {
			return entityManager;
		} else if (Proprietary.Connection.equalsIgnoreCase (name)) {
			return entityManager.unwrap (Connection.class);
		}
		return null;
	}
	
	Object lookup (String entity, Object id) throws Exception {
		return entityManager.find (resolve (entity), id);
	}

	private Class<?> resolve (String entity) throws Exception {
		BeanMetadata bm = metadata.byName (entity);
		if (bm == null) {
			throw new Exception (entity + " not registered");
		}
		Class<?> type = bm.type ();
		if (!type.isAnnotationPresent (Entity.class)) {
			throw new Exception (entity + " isn't a valid database entity");
		}
		return type;
	}

	private void checkNotNull (String eType) throws DatabaseException {
		if (Lang.isNullOrEmpty (eType)) {
			throw new DatabaseException ("entity name is null");
		}
	}
	
	private Object _query (String entity, Query.Construct construct, final Query query) throws DatabaseException {
		
		if (query == null) {
			return null;
		}
		
		boolean queryHasEntity = true;
		
		if (Lang.isNullOrEmpty (query.entity ())) {
			queryHasEntity = false;
		} else {
			entity = query.entity ();
		}
		
		checkNotNull (entity);
		
		tracer.log (Tracer.Level.Debug, "Query Entity {0}", entity);
		
		String cacheKey = construct.name () + query.name ();
		
		String 				sQuery 		= null;
		Map<String, Object> bindings 	= query.bindings ();
		
		if (queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
			sQuery 		= (String)QueriesCache.get (cacheKey);
			tracer.log (Tracer.Level.Debug, "Query meta loaded from cache {0}", sQuery);
		} 
		
		if (sQuery == null) {
			
			CompiledQuery cQuery = compile (entity, construct, query);
			
			sQuery 		= (String)cQuery.query 		();
			bindings	= cQuery.bindings 	();
			
			if (queryHasEntity && query.caching ().cache (Target.meta) && !Lang.isNullOrEmpty (query.name ())) {
				QueriesCache.put (cacheKey, sQuery);
				tracer.log (Tracer.Level.Debug, "Query meta stored in cache {0}", sQuery);
			} 
		}
		
		tracer.log (Tracer.Level.Debug, "\tQuery {0}", sQuery);
		tracer.log (Tracer.Level.Debug, "\tBindings: {0}", bindings);
		
		javax.persistence.Query jpaQuery = entityManager.createQuery (sQuery).setFirstResult (query.start ()).setMaxResults (query.count ());
		if (bindings != null && !bindings.isEmpty ()) {
			for (String parameter : bindings.keySet ()) {
				jpaQuery.setParameter (parameter, bindings.get (parameter));
			}
		}
		
		if (Query.Construct.select.equals (construct)) {
			return jpaQuery.getResultList ();
		} else {
			return jpaQuery.executeUpdate ();
		}

	}
	
	private CompiledQuery compile (String entity, Query.Construct construct, final Query query) throws DatabaseException {
		final String fEntity = entity;
		QueryCompiler compiler = new SqlQueryCompiler (construct) {
			private static final long serialVersionUID = -1248971549807669897L;
			
			@Override
			protected void onSelect (Timing timing, Select select)
					throws DatabaseException {
				if (Timing.start.equals (timing)) {
					buff.append (dml.name ());
				} else {
					if (select.count () == 0) {
						buff.append (Lang.SPACE).append (QueryEntity);
					}
					buff.append (Lang.SPACE).append (Sql.From).append (Lang.SPACE);
					entity ();
				} 
			}

			@Override
			protected String operatorFor (Operator operator) {
				/*if (Operator.ftq.equals (operator)) {
					return Lucene;
				} else*/ if (Operator.regex.equals (operator)) {
					return Regexp;
				}
				return super.operatorFor (operator);
			}
			
			@Override
			protected void entity () {
				buff.append (fEntity).append (Lang.SPACE).append (QueryEntity);
			}
			
			@Override
			protected void field (String field) {
				buff.append (QueryEntity).append (Lang.DOT).append (field);
			}
			
		}; 
		
		return compiler.compile (query);
		
	}

	private List<DatabaseObject> toList (String type, List<Object> objects, Visitor visitor) throws DatabaseException {
		if (visitor == null) {
			return new JpaObjectList<DatabaseObject> (this, objects);
		}
		
		JpaObject entity = null;
		if (visitor.optimize ()) {
			try {
				entity = new JpaObject (this, resolve (type));
			} catch (Exception ex) {
				throw new DatabaseException (ex.getMessage (), ex);
			}
		}
		
		for (Object object : objects) {
			if (visitor.optimize ()) {
				entity.bean = object;
			} else {
				entity = new JpaObject (this, object);
			}
			boolean cancel = visitor.onRecord (entity);
			if (cancel) {
				return null;
			}
		}
		return null;
	}
	
	BeanMetadata metadata (Class<?> entity) {
		return metadata.byType (entity);
	}

}
