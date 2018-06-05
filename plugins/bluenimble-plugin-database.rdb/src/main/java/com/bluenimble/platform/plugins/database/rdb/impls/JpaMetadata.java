package com.bluenimble.platform.plugins.database.rdb.impls;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.reflect.beans.BeanMetadata;

public class JpaMetadata {

	private Map<Class<?>, BeanMetadata> metadata 	= new HashMap<Class<?>, BeanMetadata> ();
	private Map<String, Class<?>> 		entities 	= new HashMap<String, Class<?>> ();
	
	private static final BeanMetadata.MinimalFieldsSelector MinimalFieldsSelector = new BeanMetadata.MinimalFieldsSelector () {
		@Override
		public boolean select (PropertyDescriptor pd) {
			Class<?> type = pd.getPropertyType ();
			return !type.isAnnotationPresent (Entity.class) && !Collection.class.isAssignableFrom (type);
		}
	};

	public JpaMetadata (EntityManagerFactory factory) {
		Metamodel mm = factory.getMetamodel ();
		Set<EntityType<?>> mEntities = mm.getEntities ();
		for (EntityType<?> entity : mEntities) {
			metadata.put (entity.getJavaType (), new BeanMetadata (entity.getJavaType (), MinimalFieldsSelector));
			String name = entity.getName ();
			if (Lang.isNullOrEmpty (name)) {
				name = entity.getJavaType ().getSimpleName ();
			}
			entities.put (name.toLowerCase (), entity.getJavaType ());
		}
	}
	
	public BeanMetadata byType (Class<?> type) {
		return metadata.get (type);
	}
	
	public BeanMetadata byName (String entity) {
		Class<?> type = entities.get (entity.toLowerCase ());
		if (type == null) {
			return null;
		}
		return byType (type);
	}
	
}
