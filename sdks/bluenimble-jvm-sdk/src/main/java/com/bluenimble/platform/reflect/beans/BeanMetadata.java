package com.bluenimble.platform.reflect.beans;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanMetadata implements Serializable {
	
	private static final long serialVersionUID = 8937696560810069653L;

	private static final String Clazz = "class";
	
	public interface MinimalFieldsSelector {
		boolean select (PropertyDescriptor pd);
	}
	
	private transient Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor> ();

	private transient String [] allFields;
	private transient String [] minimalFields;
	
	private Class<?> type;
	
	public BeanMetadata (Class<?> type, MinimalFieldsSelector minimalFieldsSelector) {
		this.type = type;
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo (type);
			PropertyDescriptor [] pds = beanInfo.getPropertyDescriptors ();
			if (pds == null || pds.length == 0) {
				return;
			}
			
			List<String> all = new ArrayList<String> ();
			List<String> minimal = new ArrayList<String> ();
			
			for (int i = 0; i < pds.length; i++) {
				PropertyDescriptor pd = pds [i];
				
				if (pd.getName ().equals (Clazz)) {
					continue;
				}
				if (minimalFieldsSelector.select (pd)) {
					minimal.add (pd.getName ());
				}
				all.add (pd.getName ());

				properties.put (pd.getName (), pd);
			}
			
			// all fields
			if (!all.isEmpty ()) {
				allFields = new String [all.size ()];
				all.toArray (allFields);
			}
			
			// minimal fields
			if (!minimal.isEmpty ()) {
				minimalFields = new String [minimal.size ()];
				all.toArray (minimalFields);
			}
			
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		
	}
	
	public Class<?> type () {
		return type;
	}

	public String [] allFields () {
		return allFields;
	}

	public String [] minimalFields () {
		return minimalFields;
	}
	
	public PropertyDescriptor property (String name) {
		return properties.get (name);
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
