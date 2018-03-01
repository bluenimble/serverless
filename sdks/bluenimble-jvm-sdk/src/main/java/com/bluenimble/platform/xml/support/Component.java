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
package com.bluenimble.platform.xml.support;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;

import com.bluenimble.platform.Lang;

@SuppressWarnings("rawtypes")
public class Component implements Serializable {
	
	private static final long serialVersionUID = -7398159878404678L;

	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";
	
	private static final Filter DEFAULT_FILTER = new DefaultFilter();
	
	private long ID = 0;

	private static long COUNTER = 0;

	protected String S_STAG = "<";

	protected String S_ETAG = "</";

	protected String E_TAG = ">";

	protected Filter valueFilter = DEFAULT_FILTER;

	private String name = "";

	private Object value = "";
	
	protected boolean filteringValue;

	protected boolean filteringAttributes = true;

	private ArrayList childs = new ArrayList(0);

	private ArrayList parents = new ArrayList(0);

	private ArrayList attributes = new ArrayList(0);

	public Component() {
		ID = COUNTER++;
	}

	public Component(String name) {
		this.name = name;
		ID = COUNTER++;
	}

	public Component(String name, Object value) {
		this.name = name;
		this.value = value;
		ID = COUNTER++;
	}

	public long id() {
		return (ID);
	}

	public String ssTag(String sstag) {
		String temp = S_STAG;
		S_STAG = sstag;
		return temp;
	}

	public String seTag(String setag) {
		String temp = S_ETAG;
		S_ETAG = setag;
		return temp;
	}

	public String eTag(String etag) {
		String temp = E_TAG;
		E_TAG = etag;
		return temp;
	}

	public Component getObjectAt(int obj_index) {
		return (Component) childs.get(obj_index);
	}

	public Component getObjectByName(String obj_name, boolean ignore_case) {
		Component obj_return = null;
		if (ignore_case) {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				if (current_obj.name.equalsIgnoreCase(obj_name)) {
					obj_return = current_obj;
					break;
				}
			}
		} else {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				if (current_obj.name.equals(obj_name)) {
					obj_return = current_obj;
					break;
				}
			}
		}
		return obj_return;
	}

	public Component getObjectByAttr(String attr_name, String attr_value,
			boolean ignore_case) {
		if (ignore_case) {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				ComponentAttribute current_attr = current_obj
						.getAttribute(attr_name);
				if (current_attr != null
						&& current_attr.value().equalsIgnoreCase(attr_value))
					return current_obj;
			}
		} else {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				ComponentAttribute current_attr = current_obj
						.getAttribute(attr_name);
				if (current_attr != null
						&& current_attr.value().equals(attr_value))
					return current_obj;
			}
		}
		return null;
	}

	public int childIndexByAttr(String attr_name, String attr_value,
			boolean ignore_case) {
		if (ignore_case) {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				ComponentAttribute current_attr = current_obj
						.getAttribute(attr_name);
				if (current_attr != null
						&& current_attr.value().equalsIgnoreCase(attr_value))
					return i;
			}
		} else {
			for (int i = 0; i < childs.size(); i++) {
				Component current_obj = getObjectAt(i);
				ComponentAttribute current_attr = current_obj
						.getAttribute(attr_name);
				if (current_attr != null
						&& current_attr.value().equals(attr_value))
					return i;
			}
		}
		return -1;
	}

	public Component getLastObject() {
		return ((Component) childs.get(childs.size() - 1));
	}

	public final String name() {
		return (name);
	}

	public final String name(String name) {
		String temp = name;
		this.name = name;
		return (temp);
	}

	public final Object value() {
		return (value);
	}

	public final Object value(Object value) {
		Object temp = value;
		this.value = value;
		return (temp);
	}

	public void setValueFilter(Filter valueFilter) {
		this.valueFilter = valueFilter;
	}

	public Filter getValueFilter() {
		return valueFilter;
	}

	public void processValueFiltering(boolean process) {
		filteringValue = process;
	}

	public void processAttributesFiltering(boolean process) {
		filteringAttributes = process;
	}

	public boolean processValueFiltering() {
		return filteringValue;
	}

	public boolean processAttributesFiltering() {
		return filteringAttributes;
	}

	public void processFilteringForObjectStructure(boolean include_object,
			boolean value_filter, boolean attrs_filter) {
		for (int i = 0; i < length(); i++) {
			childs(i).processFilteringForObjectStructure(true, value_filter,
					attrs_filter);
		}
		if (include_object) {
			if (value_filter)
				processValueFiltering(true);
			else
				processValueFiltering(true);
			if (attrs_filter)
				processAttributesFiltering(true);
			else
				processAttributesFiltering(false);
		}
	}

	protected Object getFilteredValue (boolean useCData) {
		if (value == null) {
			return null;
		}
		if (useCData) {
			return CDATA_START + value + CDATA_END;
		}
		if (valueFilter != null && filteringValue) {
			return valueFilter.scan(value.toString());
		}
		return value;
	}

	protected Object getFilteredValue () {
		return getFilteredValue (false);
	}
	
	public ComponentAttribute getAttributeAt(int attr_index) {
		return (ComponentAttribute) attributes.get(attr_index);
	}

	public ComponentAttribute attributes(int attr_index) {
		return (ComponentAttribute) attributes.get(attr_index);
	}

	public ComponentAttribute getAttribute(String attr_name) {
		int j = -1;
		for (int i = 0; i < attributes.size(); i++) {
			ComponentAttribute attr = (ComponentAttribute) attributes.get(i);
			if (attr.name().equalsIgnoreCase(attr_name)) {
				j = i;
			}
		}
		if (j != -1) {
			return (ComponentAttribute) attributes.get(j);
		} else {
			return null;
		}
	}

	/**
	 * @deprecated use attributes()
	 */
	public ComponentAttribute getAttribute(String attr_name, boolean ignore_case) {
		int j = -1;
		if (ignore_case)
			return getAttribute(attr_name);
		else {
			for (int i = 0; i < attributes.size(); i++) {
				ComponentAttribute attr = (ComponentAttribute) attributes.get(i);
				if (attr.name().equals(attr_name)) {
					j = i;
				}
			}
			if (j != -1) {
				return (ComponentAttribute) attributes.get(j);
			} else {
				return null;
			}
		}
	}

	public ComponentAttribute attributes(String attr_name, boolean ignore_case) {
		int j = -1;
		if (ignore_case) {
			return getAttribute(attr_name);
		} else {
			for (int i = 0; i < attributes.size(); i++) {
				ComponentAttribute attr = (ComponentAttribute) attributes.get(i);
				if (attr.name().equals(attr_name)) {
					j = i;
				}
			}
			if (j != -1) {
				return (ComponentAttribute) attributes.get(j);
			} else {
				return null;
			}
		}
	}

	public ComponentAttribute attributes(String attr_name) {
		return attributes(attr_name, false);
	}

	@SuppressWarnings("unchecked")
	public ArrayList getObjects(int start_ind, int end_ind) {
		ArrayList objs = new ArrayList();
		for (int i = start_ind; i < end_ind; i++) {
			objs.add(childs.get(i));
		}
		return objs;
	}

	public ArrayList getObjects() {
		return childs;
	}

	@SuppressWarnings("unchecked")
	public ArrayList getAttributes(int start_ind, int end_ind) {
		ArrayList attrs = new ArrayList();
		for (int i = start_ind; i < end_ind; i++) {
			attrs.add (attributes.get(i));
		}
		return attrs;
	}

	@SuppressWarnings("unchecked")
	public ArrayList attributes(int start_ind, int end_ind) {
		ArrayList attrs = new ArrayList();
		for (int i = start_ind; i < end_ind; i++) {
			attrs.add(attributes.get(i));
		}
		return attrs;
	}

	public ArrayList getAttributes() {
		return attributes;
	}

	public ArrayList attributes() {
		return attributes;
	}

	public ArrayList parents() {
		return parents;
	}

	public Component parents(int index) {
		return (Component) parents.get(index);
	}

	public Component parentByID(int id) {
		Component parent = new Component();
		for (int i = 0; i < parents.size(); i++) {
			parent = parents(i);
			if (parent.ID == id) {
				break;
			}
		}
		return (parent);
	}

	public ArrayList childs() {
		return childs;
	}

	public Component childs(int index) {
		if (index < this.length() && index >= 0)
			return (Component) (childs.get(index));
		else
			return null;
	}

	public Component childs(String name, int from_pos, boolean ignore_case) {
		Component obj = null;
		int counter = 0;
		if (ignore_case) {
			for (int i = 0; i < childs.size(); i++) {
				Component curr_obj = childs(i);
				if (curr_obj.name.equalsIgnoreCase(name)) {
					if (counter >= from_pos) {
						obj = curr_obj;
						break;
					}
					counter++;
				}
			}
		} else {
			for (int i = 0; i < childs.size(); i++) {
				Component curr_obj = childs(i);
				if (curr_obj.name.equals(name)) {
					if (counter >= from_pos) {
						obj = curr_obj;
						break;
					}
					counter++;
				}
			}
		}
		return (obj);
	}

	public Component childs(String name, int index) {
		return childs(name, index, true);
	}

	public Component childs(String name) {
		return childs(name, 0, true);
	}

	public int childIndex(String name) {
		int iret = -1;
		for (int i = 0; i < length(); i++) {
			if (childs(i).name().equalsIgnoreCase(name)) {
				iret = i;
				break;
			}
		}
		return iret;
	}

	public int length() {
		return (childs.size());
	}

	public int attrLength() {
		return (attributes.size());
	}

	public Component childByID(int id) {
		Component child = null;
		for (int i = 0; i < childs.size(); i++) {
			child = childs(i);
			if (child.ID == id) {
				break;
			}
		}
		return (child);
	}

	@SuppressWarnings("unchecked")
	private void addParent(Component object) {
		parents.ensureCapacity(parents.size() + 1);
		parents.add(object);
		parents.trimToSize();
	}

	@SuppressWarnings("unchecked")
	public final void addObject(Component object) {
		childs.ensureCapacity(childs.size() + 1);
		object.addParent(this);
		childs.add(object);
		childs.trimToSize();
	}

	@SuppressWarnings("unchecked")
	public final void addObjects(ArrayList objects) {
		if (objects != null) {
			childs.ensureCapacity(childs.size() + objects.size());
			for (int i = 0; i < objects.size(); i++) {
				((Component) objects.get(i)).addParent(this);
				childs.add(objects.get(i));
			}
			childs.trimToSize();
		}
	}

	@SuppressWarnings("unchecked")
	public final void addObjects(Component[] objects) {
		if (objects != null) {
			childs.ensureCapacity(childs.size() + objects.length);
			for (int i = 0; i < objects.length; i++) {
				objects[i].addParent(this);
				childs.add(objects[i]);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public final void addObjectAt(Component object, int index) {
		childs.ensureCapacity(childs.size() + 1);
		object.addParent(this);
		childs.add(index, object);
	}

	@SuppressWarnings("unchecked")
	public final void addObjectAt(ArrayList objects, int index) {
		childs.ensureCapacity(childs.size() + objects.size());
		for (int i = 0; i < objects.size(); i++) {
			((Component) objects.get(i)).addParent(this);
			childs.add(index + i, objects.get(i));
		}
		childs.trimToSize();
	}

	@SuppressWarnings("unchecked")
	public final void replaceObjectAt(int index, Component object) {
		childs.set(index, object);
	}

	@SuppressWarnings("unchecked")
	public final void replaceObjectAt(int index, Component[] objects) {
		for (int i = objects.length - 1; i >= 0; i--) {
			childs.set(index, objects[i]);
		}
		childs.remove(index + objects.length);
	}

	public final void replaceObjects(ArrayList objs) {
		childs = objs;
	}

	@SuppressWarnings("unchecked")
	public final void replaceObjectAt(int start_index, int end_index,
			Component[] objects) {
		for (int j = start_index; j <= end_index; j++) {
			childs.remove(j);
		}
		for (int i = objects.length - 1; i >= 0; i--) {
			childs.add(start_index, objects[i]);
		}
	}

	@SuppressWarnings("unchecked")
	public final void replaceObjectAt(int index, ArrayList objects) {
		for (int i = objects.size() - 1; i >= 0; i--) {
			childs.add(index, objects.get(i));
		}
		childs.remove(index + objects.size());
	}

	@SuppressWarnings("unchecked")
	public final void replaceObjectAt(int start_index, int end_index,
			ArrayList objects) {
		for (int j = start_index; j <= end_index; j++) {
			childs.remove(j);
		}
		for (int i = objects.size() - 1; i >= 0; i--) {
			childs.add(start_index, objects.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	public final void addObjectAt(Component[] objects, int index) {
		childs.ensureCapacity(childs.size() + objects.length);
		for (int i = 0; i < objects.length; i++) {
			childs.add(index + i, objects[i]);
		}
	}

	public final void swapObjects(int obj1_index, int obj2_index) {
		Component temp = (Component) childs.get(obj1_index);
		replaceObjectAt(obj1_index, (Component) childs.get(obj2_index));
		replaceObjectAt(obj2_index, temp);
	}

	public final Component changeAttributeAt(int attr_index,
			ComponentAttribute attr) {
		getAttributeAt(attr_index).name(attr.name());
		getAttributeAt(attr_index).value(attr.value());
		return this;
	}

	public final void removeObject(Component object) {
		childs.remove(object);
		childs.trimToSize();
	}

	public final void removeObjectAt(int object_index) {
		childs.remove(object_index);
		childs.trimToSize();
	}

	public final void removeObjectAt(int[] objects_indexes) {
		for (int i = 0; i < objects_indexes.length; i++) {
			childs.remove(objects_indexes[i]);
		}
		childs.trimToSize();
	}

	public final void removeAllObject() {
		childs.clear();
		childs.trimToSize();
	}

	public final void clear () {
		if (childs != null) {
			for (int i = 0; i < length (); i++) {
				childs (i).clear ();
			}
			childs.clear ();
			childs = null;
		}
		if (parents != null) {
			parents = null;
		}
		if (attributes != null) {
			for (int i = 0; i < attributes.size (); i++) {
				attributes (i).clear ();
			}
			attributes.clear ();
			attributes = null;
		}
	}

	public void removeObjects(String name, boolean ignore_case) {
		if (ignore_case) {
			for (int i = 0; i < childs.size(); i++) {
				Component obj = (Component) childs.get(i);
				if (obj.name.equalsIgnoreCase(name)) {
					childs.remove(i);
				}
			}
		} else {
			for (int i = 0; i < childs.size(); i++) {
				Component obj = (Component) childs.get(i);
				if (obj.name.equals(name)) {
					childs.remove(i);
				}
			}
		}
		childs.trimToSize();
	}

	public void removeObjects(String name) {
		removeObjects(name, true);
	}

	public final void removeObjects(ComponentAttribute attribute, boolean cascade) {
		for (int i = 0; i < childs.size(); i++) {
			Component obj = (Component) childs.get(i);
			for (int j = 0; j < obj.attributes.size(); j++) {
				ComponentAttribute attr = (ComponentAttribute) obj.attributes
						.get(j);
				if (attr.name().equals(attribute.name())
						&& attr.value().equals(attribute.value())) {
					childs.remove(i);
				}
			}
			if (cascade) {
				obj.removeObjects(attribute, true);
			}
		}
		childs.trimToSize();
	}

	/** adding attributes to the object */
	@SuppressWarnings("unchecked")
	public final void addAttribute(ComponentAttribute attribute) {
		attributes.ensureCapacity(1);
		attributes.add(attribute);
	}

	@SuppressWarnings("unchecked")
	public final void addAttributes(ArrayList attributes) {
		if (attributes != null) {
			attributes.ensureCapacity(attributes.size() + attributes.size());
			for (int i = 0; i < attributes.size(); i++) {
				attributes.add(attributes.get(i));
			}
			attributes.trimToSize();
		}
	}

	@SuppressWarnings("unchecked")
	public final void addAttributes(ComponentAttribute[] attributes) {
		if (attributes != null) {
			this.attributes.ensureCapacity(this.attributes.size() + attributes.length);
			for (int i = 0; i < attributes.length; i++) {
				this.attributes.add(attributes[i]);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public final void addAttribute(String name, String value) {
		attributes.ensureCapacity(attributes.size() + 1);
		attributes.add(new ComponentAttribute(name, value));
	}

	@SuppressWarnings("unchecked")
	public final void addAttribute(String name) {
		attributes.ensureCapacity(attributes.size() + 1);
		attributes.add(new ComponentAttribute(name));
	}

	@SuppressWarnings("unchecked")
	public final void addAttributeAt(int attr_index, ComponentAttribute attr) {
		attributes.ensureCapacity(attributes.size() + 1);
		if (attr_index < (attributes.isEmpty() ? 0 : attributes.size())) {
			attributes.add(attr_index, attr);
		} else {
			attributes.add(attr);
		}
		attributes.trimToSize();
	}

	public final void addAttributeAt(int attr_index, String name, String val) {
		addAttributeAt(attr_index, new ComponentAttribute(name, val));
	}

	@SuppressWarnings("unchecked")
	public final void addAttributes(ArrayList attr_names, ArrayList attr_values) {
		attributes.ensureCapacity(attributes.size() + attr_names.size());
		if (attr_values.size() < attr_names.size()) {
			for (int i = 0; i < attr_values.size(); i++) {
				attributes.add(new ComponentAttribute(attr_names.get(i)
						.toString(), attr_values.get(i).toString()));
			}
			for (int i = attr_values.size(); i < attr_names.size(); i++) {
				attributes.add(new ComponentAttribute(attr_names.get(i)
						.toString()));
			}
		} else {
			for (int i = 0; i < attr_names.size(); i++) {
				attributes.add(new ComponentAttribute(attr_names.get(i)
						.toString(), attr_values.get(i).toString()));
			}
		}
	}

	public final void removeAttribute(ComponentAttribute attribute) {
		attributes.remove(attribute);
		attributes.trimToSize();
	}

	public final void removeAttribute(String attribute) {
		attributes.remove(attributes(attribute));
		attributes.trimToSize();
	}

	public final void removeAttributeAt(int attribute_index) {
		attributes.remove(attribute_index);
		attributes.trimToSize();
	}

	public final void removeAttributeAt(int[] attributes_indexes) {
		for (int i = 0; i < attributes_indexes.length; i++) {
			attributes.remove(attributes_indexes[i]);
		}
		attributes.trimToSize();
	}

	public final void removeAllAttributes() {
		attributes.clear();
		attributes.trimToSize();
	}

	public final void replaceAttributes(ArrayList attrs) {
		attributes = attrs;
	}

	public final void reloadAttributes(ComponentAttribute[] attrs) {
		attributes = new ArrayList(0);
		addAttributes(attrs);
	}

	@SuppressWarnings("unchecked")
	public final void replaceAttributeAt(int index, ComponentAttribute attr) {
		attributes.set(index, attr);
	}

	public final void replaceAttributes(String attrs) {
		attributes = new ArrayList();
		addAttribute(attrs);
	}

	public final void replaceAttributes(ArrayList attr_names,
			ArrayList attr_values) {
		attributes = new ArrayList();
		addAttributes(attr_names, attr_values);
	}

	/*
	 * public final void replaceAttributes(String[] attr_names, String[]
	 * attr_values) { ATTRIBUTES = new ArrayList(); addAttributes(attr_names,
	 * attr_values); }
	 */
	public final void mergeAttributes(ArrayList attrs, boolean replace_old,
			boolean ignore_case) {
		if (attrs != null)
			for (int i = 0; i < attrs.size(); i++) {
				mergeAttributes((ComponentAttribute) attrs.get(i), replace_old,
						ignore_case);
			}
	}

	public final void mergeAttributes(ComponentAttribute[] attrs,
			boolean replace_old, boolean ignore_case) {
		if (attrs != null)
			for (int i = 0; i < attrs.length; i++) {
				mergeAttributes(attrs[i], replace_old, ignore_case);
			}
	}

	public final void mergeAttributes(ComponentAttribute attr,
			boolean replace_old, boolean ignore_case) {
		ComponentAttribute _attr = attributes(attr.name(), ignore_case);
		if (_attr == null)
			addAttribute(attr);
		else if (replace_old)
			_attr.value(attr.value());
	}

	public final void mergeAttributes(String attr_name, String attr_value,
			boolean replace_old, boolean ignore_case) {
		ComponentAttribute _attr = attributes(attr_name, ignore_case);
		if (_attr == null)
			addAttribute(new ComponentAttribute(attr_name, attr_value));
		else if (replace_old)
			_attr.value(attr_value);
	}

	public final void swapAttributes(int attr1_index, int attr2_index) {
		ComponentAttribute temp = (ComponentAttribute) attributes
				.get(attr1_index);
		replaceAttributeAt(attr1_index, (ComponentAttribute) attributes
				.get(attr2_index));
		replaceAttributeAt(attr2_index, temp);
	}

	/** Modifing Inner objects attributes */
	public final void addInnerAttributeTo(int inner_obj, ComponentAttribute attr) {
		getObjectAt(inner_obj).attributes
				.ensureCapacity(getObjectAt(inner_obj).attributes.size() + 1);
		getObjectAt(inner_obj).addAttribute(attr);
	}

	public final void addInnerAttributeTo(int inner_obj,
			ComponentAttribute[] attrs) {
		getObjectAt(inner_obj).attributes.ensureCapacity(attrs.length
				+ getObjectAt(inner_obj).attributes.size());
		getObjectAt(inner_obj).addAttributes(attrs);
	}

	public final void addInnerAttributeTo(int inner_obj, ArrayList attrs) {
		getObjectAt(inner_obj).attributes.ensureCapacity(attrs.size()
				+ getObjectAt(inner_obj).attributes.size());
		getObjectAt(inner_obj).addAttributes(attrs);
	}

	public final void addInnerAttributeTo(int[] inner_objs,
			ComponentAttribute attr) {
		for (int i = 0; i < inner_objs.length; i++) {
			getObjectAt(inner_objs[i]).attributes
					.ensureCapacity(1 + getObjectAt(inner_objs[i]).attributes
							.size());
			getObjectAt(inner_objs[i]).addAttribute(attr);
		}
	}

	public final void addInnerAttributeTo(int[] inner_objs,
			ComponentAttribute[] attrs) {
		for (int i = 0; i < inner_objs.length; i++) {
			getObjectAt(inner_objs[i]).attributes.ensureCapacity(attrs.length
					+ getObjectAt(inner_objs[i]).attributes.size());
			getObjectAt(inner_objs[i]).addAttributes(attrs);
		}
	}

	public final void addInnerAttributeTo(int[] inner_objs, ArrayList attrs) {
		for (int i = 0; i < inner_objs.length; i++) {
			getObjectAt(inner_objs[i]).attributes.ensureCapacity(attrs.size()
					+ getObjectAt(inner_objs[i]).attributes.size());
			getObjectAt(inner_objs[i]).addAttributes(attrs);
		}
	}

	public final void addInnerAttributeTo(ComponentAttribute attr) {
		for (int i = 0; i < childs.size(); i++) {
			getObjectAt(i).attributes
					.ensureCapacity(1 + getObjectAt(i).attributes.size());
			getObjectAt(i).addAttribute(attr);
		}
	}

	public final void addInnerAttributeTo(ComponentAttribute[] attrs) {
		for (int i = 0; i < childs.size(); i++) {
			getObjectAt(i).attributes.ensureCapacity(attrs.length
					+ getObjectAt(i).attributes.size());
			getObjectAt(i).addAttributes(attrs);
		}
	}

	public final void addInnerAttributeTo(ArrayList attrs) {
		for (int i = 0; i < childs.size(); i++) {
			getObjectAt(i).attributes.ensureCapacity(attrs.size()
					+ getObjectAt(i).attributes.size());
			((Component) childs.get(i)).addAttributes(attrs);
		}
	}

	public final void removeInnerAttribute(int inner_obj, int attr_index) {
		getObjectAt(inner_obj).removeAttributeAt(attr_index);
	}

	public final void removeInnerAttributes(int inner_obj, int[] attr_indexes) {
		for (int i = 0; i < attr_indexes.length; i++) {
			getObjectAt(inner_obj).removeAttributeAt(attr_indexes[i]);
		}
	}

	public final void removeInnerAttribute(int attr_index) {
		for (int i = 0; i < childs.size(); i++) {
			getObjectAt(i).removeAttributeAt(attr_index);
		}
	}

	public final void removeInnerAttributes(int[] attr_indexes) {
		for (int i = 0; i < childs.size(); i++) {
			for (int j = 0; j < attr_indexes.length; j++) {
				getObjectAt(i).removeAttributeAt(attr_indexes[j]);
			}
		}
	}
	
	public String getAttributeValue (String attrName) {
		ComponentAttribute attribute = attributes (attrName);
		if (attribute == null) {
			return null;
		}
		return attribute.value ();
	}

	public final void exportTo(Component obj, boolean export_name,
			boolean export_value) {
		if (export_name) {
			obj.name = this.name;
		}
		if (export_value) {
			obj.value = this.value;
		}
		for (int i = 0; i < attributes.size(); i++) {
			obj.addAttribute((ComponentAttribute) attributes.get(i));
		}
	}

	public Component convertTo(Component obj, boolean import_name,
			boolean import_value, char objects_import, char attr_import,
			boolean ignore_case_attrs) {
		/**
		 * objects_import indicate the type of the objects import objects_import =
		 * '0' : indicate that the inner objects will be replaced by the new obj
		 * inner objects. objects_import = '1' : indicate that the new inner
		 * objects will be added to the old inner objects. objects_import =
		 * [other value] : refuse importing inner objects.
		 */

		/**
		 * attr_import_type indicate the type of the attributes import
		 * attr_import = '0' : indicate that the Object attributes will be
		 * replaced by the new obj attributes. attr_import = '1' : indicate that
		 * the new attributes will be added to the old Object attributes.
		 * attr_import = '2' : Doing a union of the old and new attributes by
		 * keeping the old attributes if these attributes exists in the new
		 * object. attr_import = '3' : Doing a union of the old and new
		 * attributes by replacing the old attributes if these attributes exists
		 * in the new object. attr_import = [other value] : refuse importing
		 * attributes.
		 */
		if (import_name) {
			this.name = obj.name;
		}
		if (import_value) {
			this.value = obj.value;
		}
		switch (objects_import) {
		case '0':
			this.childs = obj.childs;
			break;
		case '1':
			this.addObjects(obj.childs);
			break;
		default:
			break;
		}
		switch (attr_import) {
		case '0':
			this.attributes = obj.attributes;
			break;
		case '1':
			this.addAttributes(obj.attributes);
			break;
		case '2':
			this.mergeAttributes(obj.attributes, false, ignore_case_attrs);
			break;
		case '3':
			this.mergeAttributes(obj.attributes, true, ignore_case_attrs);
			break;
		default:
			break;
		}
		return this;
	}

	public final void exportAttributesToChilds() {
		for (int i = 0; i < childs.size(); i++) {
			((Component) childs.get(i)).addAttributes(attributes);
		}
	}

	public final void addAttributesCascade(ArrayList attrs,
			boolean include_parent) {
		if (attrs != null) {
			if (include_parent) {
				addAttributes(attrs);
				attributes.trimToSize();
			}
			for (int i = 0; i < childs.size(); i++) {
				getObjectAt(i).attributes
						.ensureCapacity(getObjectAt(i).attributes.size()
								+ attrs.size());
				getObjectAt(i).addAttributes(attrs);
				getObjectAt(i).addAttributesCascade(attrs, false);
				getObjectAt(i).attributes.trimToSize();
			}
		}
	}

	public final void addAttributesCascade(ComponentAttribute attr,
			boolean include_parent) {
		if (attr != null) {
			if (include_parent) {
				attributes.ensureCapacity(attributes.size() + 1);
				addAttribute(attr);
				attributes.trimToSize();
			}
			for (int i = 0; i < childs.size(); i++) {
				getObjectAt(i).attributes
						.ensureCapacity(getObjectAt(i).attributes.size() + 1);
				getObjectAt(i).addAttribute(attr);
				getObjectAt(i).addAttributesCascade(attr, false);
				getObjectAt(i).attributes.trimToSize();
			}
		}
	}

	public final void loadObjectIn(Component obj, int class_name_index)
			throws Exception {
		obj.name = this.name;
		obj.value = this.value;
		obj.attributes = this.attributes;
		for (int i = 0; i < childs.size(); i++) {
			Component in_obj = (Component) childs.get(i);
			String class_name = ((ComponentAttribute) in_obj.attributes
					.get(class_name_index)).value();
			Class in_class = Class.forName(class_name);
			Object obj_class = in_class.newInstance();
			in_obj.loadObjectIn((Component) obj_class, class_name_index);
			obj.addObject((Component) obj_class);
		}
	}

	public boolean contains(String name) {
		return (childIndex(name) > -1);
	}

	public void trim() {
		value(value.toString().trim());
		for (int i = 0; i < length(); i++)
			childs(i).trim();
	}

	public Component copy() {
		Component obj = new Component(this.name, this.value);
		for (int i = 0; i < attributes.size(); i++) {
			obj.addAttribute(getAttributeAt(i).copy());
		}
		for (int i = 0; i < childs.size(); i++) {
			obj.addObject(getObjectAt(i).copy());
		}
		return obj;
	}

	public StringBuilder open() {
		StringBuilder object_code = new StringBuilder("");
		if (!name.equals("")) {
			object_code.append(S_STAG);
			object_code.append(name);
			if (attributes.size() != 0) {
				object_code.append(attributes.isEmpty() ? "" : " ");
				for (int i = 0; i < attributes.size(); i++) {
					object_code.append((getAttributeAt(i).value() == null) ? getAttributeAt(
									i).name()
									: ((filteringAttributes) ? getAttributeAt(i).print(valueFilter)
											: getAttributeAt(i).print()));
					object_code.append((i == attributes.size() - 1) ? "" : " ");
				}
			}
			object_code.append(E_TAG);
			object_code.append((value == null) ? "" : getFilteredValue());
		}
		return object_code;
	}

	public void open(Writer out) throws IOException {
		if (!name.equals("")) {
			out.write(S_STAG);
			out.write(name);
			if (attributes.size() != 0) {
				out.write(attributes.isEmpty() ? "" : " ");
				for (int i = 0; i < attributes.size(); i++) {
					out.write((getAttributeAt(i).value() == null) ? getAttributeAt(i).name()
									: ((filteringAttributes) ? getAttributeAt(i).print(valueFilter)
											: getAttributeAt(i).print()));
					out.write((i == attributes.size() - 1) ? "" : " ");
				}
			}
			out.write(E_TAG);
			out.write((value == null) ? "" : getFilteredValue().toString ());
		}
	}

	public StringBuilder close() {
		StringBuilder object_code = new StringBuilder("");
		if (!name.equals("")) {
			object_code.append(S_ETAG);
			object_code.append(name);
			object_code.append(E_TAG);
		}
		return object_code;
	}

	public void close(Writer out) throws IOException {
		if (!name.equals("")) {
			out.write(S_ETAG);
			out.write(name);
			out.write(E_TAG);
		}
	}

	public StringBuilder print() {
		StringBuilder object_code = new StringBuilder("");
		object_code.append(open());
		if (childs.size() != 0) {
			for (int j = 0; j < childs.size(); j++) {
				object_code.append(getObjectAt(j).print());
			}
		}
		object_code.append(close());
		return object_code;
	}

	public void toXml (Writer os, String tabString, boolean useCData, int level) throws IOException {
		open (os, tabString, useCData, level);
		for (int i = 0; i < childs.size(); i++) {
			childs (i).toXml (os, tabString, useCData, level + 1);
		}
		close (os, tabString, level);
	}
	
	public void toXml (Writer os, String tabString, String encoding, boolean useCData) throws IOException {
		os.write ("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
		toXml (os, tabString, useCData, 0);
	}

	public void toXml (Writer os, String encoding, boolean useCData) throws IOException {
		toXml (os, "\t", encoding, useCData);
	}

	public void toXml (Writer os, boolean useCData) throws IOException {
		toXml (os, "UTF-8", useCData);
	}

	public void toXml (Writer os) throws IOException {
		toXml (os, true);
	}

	public void open (Writer os, String tabString, boolean useCData, int level) throws IOException {
		if (name == null || name.trim ().equals (Lang.BLANK)) {
			return;
		}
		if (level > 0) {
			os.write (Lang.ENDLN);
		}
		addTabString (os, tabString, level);
		os.write(S_STAG);
		os.write(name);
		if (attributes.size() != 0) {
			os.write(attributes.isEmpty() ? Lang.BLANK : Lang.SPACE);
			for (int i = 0; i < attributes.size(); i++) {
				os.write((getAttributeAt(i).value() == null) ? getAttributeAt(i).name()
								: ((filteringAttributes) ? getAttributeAt(i).print(valueFilter)
										: getAttributeAt(i).print()));
				os.write((i == attributes.size() - 1) ? Lang.BLANK : Lang.SPACE);
			}
		}
		
		if (value != null && !value.toString ().equals (Lang.BLANK)) {
			os.write(E_TAG);
			os.write (getFilteredValue (useCData).toString ());
		} else {
			if (length () > 0) {
				os.write (E_TAG);
			} else {
				os.write (Lang.SLASH);
				os.write (E_TAG);
			}
		}
	}
	
	public void close (Writer os, String tabString, int level) throws IOException {
		if (name == null || name.trim ().equals (Lang.BLANK)) {
			return;
		}
		if ((value == null || value.toString ().equals (Lang.BLANK)) && length () <= 0) {
			return;
		}
		if (length () > 0) {
			os.write (Lang.ENDLN);
			addTabString (os, tabString, level);
		}
		os.write(S_ETAG);
		os.write(name);
		os.write(E_TAG);
		os.flush ();
	}

	private void addTabString (Writer os, String tabString, int level) throws IOException {
		for (int i = 0; i < level; i++) {
			os.write (tabString);
		}
	}

	public void print(String tofile) throws IOException {
		File newfile = new File(tofile);
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(newfile)));
		out.writeBytes(print().toString());
		out.close();
		out = null;
		newfile = null;
	}

	public void print(Writer out) throws IOException {
		open(out);
		if (childs.size() != 0) {
			for (int j = 0; j < childs.size(); j++) {
				getObjectAt(j).print(out);
			}
		}
		close(out);
		out.flush();
	}
	
}