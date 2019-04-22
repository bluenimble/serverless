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
package com.bluenimble.platform.reflect.beans;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BeanMetadata implements Serializable {
	
	private static final long serialVersionUID = 8937696560810069653L;

	private static final String Clazz = "class";
	
	public interface MinimalFieldsSelector {
		boolean select (PropertyDescriptor pd);
	}
	
	private transient Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor> ();

	private transient Set<String> allFields = new HashSet<String> ();
	private transient Set<String> minimalFields = new HashSet<String> ();
	
	private Class<?> type;
	
	public BeanMetadata (Class<?> type, MinimalFieldsSelector minimalFieldsSelector) {
		this.type = type;
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo (type);
			PropertyDescriptor [] pds = beanInfo.getPropertyDescriptors ();
			if (pds == null || pds.length == 0) {
				return;
			}
			
			for (int i = 0; i < pds.length; i++) {
				PropertyDescriptor pd = pds [i];
				
				if (pd.getName ().equals (Clazz)) {
					continue;
				}
				if (minimalFieldsSelector.select (pd)) {
					minimalFields.add (pd.getName ());
				}
				allFields.add (pd.getName ());

				properties.put (pd.getName (), pd);
			}
			
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		
	}
	
	public Class<?> type () {
		return type;
	}

	public Set<String> allFields () {
		return allFields;
	}

	public Set<String> minimalFields () {
		return minimalFields;
	}
	
	public PropertyDescriptor property (String name) {
		return properties.get (name);
	}
	
	public Field field (String name) throws Exception {
		return type.getField (name);
	}
	
	public boolean isEmpty () {
		return properties.isEmpty ();
	}

	public boolean has (String key) {
		return properties.containsKey (key);
	}

	public void set (Object bean, String key, Object value) throws Exception {
		PropertyDescriptor property = property (key);
		if (property == null) {
			return;
		}
		property.getWriteMethod ().invoke (bean, new Object [] { value });
	}

	public Object get (Object bean, String key) throws Exception {
		PropertyDescriptor property = property (key);
		if (property == null) {
			return null;
		}
		
		return property.getReadMethod ().invoke (bean, (Object [])null);
	}

}
