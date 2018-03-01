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
package com.bluenimble.platform.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.bluenimble.platform.Lang;

public class JsonArray extends JsonAbstractEntity implements List<Object> {

	private static final long serialVersionUID = 5969290028072204587L;

	public static final JsonArray 	Blank 		= new JsonArray (Collections.unmodifiableList (new ArrayList<Object> ()));

	private List<Object> values;

	public JsonArray () {
	}

	public JsonArray (String json) throws JsonException {
		this (new JsonParser (json));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JsonArray (List<Object> values) {
		if (values == null) {
			return;
		}
		this.values = new ArrayList<Object> ();
		for (Object o : values) {
			if (o instanceof JsonObject || o instanceof JsonArray) {
				this.values.add (o);
			} else if (o instanceof Map) {
				this.values.add (new JsonObject ((Map)o, true));
			} else if (o instanceof List) {
				this.values.add (new JsonArray ((List)o));
			} else {
				this.values.add (o);
			}
		}
	}

	public JsonArray (JsonParser jt) throws JsonException {
		char c = jt.nextClean();
		char q;
		if (c == '[') {
			q = ']';
		} else if (c == '(') {
			q = ')';
		} else {
			throw jt.syntaxError("A JsonArray text must start with '['");
		}
		if (jt.nextClean() == ']') {
			return;
		}
		jt.back();
		for (;;) {
			if (jt.nextClean() == ',') {
				jt.back();
				add (JsonObject.NULL);
			} else {
				jt.back();
				add (jt.nextValue());
			}
			c = jt.nextClean();
			switch (c) {
			case ';':
			case ',':
				if (jt.nextClean() == ']') {
					return;
				}
				jt.back();
				break;
			case ']':
			case ')':
				if (q != c) {
					throw jt.syntaxError("Expected a '" + new Character(q)
							+ "'");
				}
				return;
			default:
				throw jt.syntaxError("Expected a ',' or ']'");
			}
		}
	}
	
	public int count () {
		if (values == null) {
			return 0;
		}
		return values.size ();
	}
	
	public boolean add (Object value) {
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		return values.add (value);
	}
	
	public JsonArray push (Object value) {
		add (value);
		return this;
	}
	
	public JsonEntity set (String name, Object value) {
		add (value);
		return this;
	}

	public Object get (int index) {
		if (values == null) {
			return null;
		}
		Object v = values.get (index);
		if (v == NULL) {
			return null;
		}
		return v;
	}

	public void clear () {
		if (values == null) {
			return;
		}
		
		for (int i = 0; i < count (); i++) {
			Object value = values.get (i);
			if (value instanceof JsonObject) {
				((JsonObject)value).clear ();
			} else if (value instanceof JsonArray) {
				((JsonArray)value).clear ();
			}
		}
		values.clear ();
		values = null;
		
	}
	
	public boolean remove (Object obj) {
		if (values == null) {
			return false;
		}
		return values.remove (obj);
	}
	
	public Object remove (int index) {
		if (values == null) {
			return null;
		}
		return values.remove (index);
	}
	
	public boolean isNull (int index) {
		return JsonObject.NULL.equals (get (index));
	}

    public String toString () {
        try {
            return '[' + join (",") + ']';
        } catch (Exception e) {
            return null;
        }
    }

    public String join (String separator) {
        int len = count ();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i += 1) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append (valueToString (get (i)));
        }
        String s = sb.toString();
        sb.setLength (0);
        sb = null;
        return s;
    }
    
    public JsonObject toObject (String field, String altField) {
    	if (Lang.isNullOrEmpty (field) || isEmpty ()) {
    		return new JsonObject ();
    	}
    	
    	JsonObject result = new JsonObject ();
    	
    	for (int i = 0; i < count (); i++) {
    		Object v = get (i);
    		if (v instanceof JsonObject) {
    			JsonObject o = (JsonObject)v;
    			
    			String keyField = field;
    			
    			Object key = o.get (field);
    			if (key == null && !Lang.isNullOrEmpty (altField)) {
        			key = o.get (altField);
        			keyField = altField;
    			}
    			if (key == null) {
        			continue;
    			}
    			
    			result.set (key.toString (), o);
    			o.remove (keyField);
    		}
    	}
    	
    	return result;
    	
    }

    public String toString (int indentFactor) {
		StringBuilder buff = new StringBuilder ();
		StringEmitter emitter = new StringEmitter (buff);
		if (indentFactor > 0) {
			emitter.prettify ();
		}
		if (indentFactor == 1) {
			emitter.tab ("  ");
		} 
		write (emitter);
		String s = buff.toString ();
		buff.setLength (0);
		return s;
   }

	public void write (JsonEmitter emitter) {
		write (this, emitter);
	}

	void write (JsonArray a, JsonEmitter emitter) {
		
		if (a == null) {
			return;
		}
		
		emitter.onStartArray (a);
		
		if (!a.isEmpty ()) {
			for (int i = 0; i < a.count (); i++) {
				Object child = a.get (i);
				
				emitter.onStartArrayValue (a, child, (i + 1) == a.count ());
				
				if (child instanceof JsonObject) {
					((JsonObject)child).write (emitter, false);
				} else if (child instanceof JsonArray) {
					((JsonArray)child).write (emitter);
				} else {
					emitter.onValue (a, null, child);
				}
				
				emitter.onEndArrayValue (a, child, (i + 1) == a.count ());

			}
		}
		
		emitter.onEndArray (a);
		
	}

	public void add (int index, Object element) {
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		values.add (index, element);
	}

	public boolean addAll (Collection<? extends Object> c) {
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		return values.addAll (c);
	}

	public boolean addAll (int index, Collection<? extends Object> c) {
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		return values.addAll (index, c);
	}

	public boolean contains (Object o) {
		if (values == null) {
			return false;
		}
		return values.contains (o);
	}

	public boolean containsAll (Collection<?> c) {
		if (values == null) {
			return false;
		}
		return values.containsAll (c);
	}

	public int indexOf (Object o) {
		if (values == null) {
			return -1;
		}
		return values.indexOf (o);
	}

	public boolean isEmpty () {
		if (values == null) {
			return true;
		}
		return values.isEmpty ();
	}

	public Iterator<Object> iterator () {
		if (values == null) {
			return null;
		}
		return values.iterator ();
	}

	public int lastIndexOf (Object o) {
		if (values == null) {
			return -1;
		}
		return values.lastIndexOf (o);
	}

	public ListIterator<Object> listIterator () {
		if (values == null) {
			return null;
		}
		return values.listIterator ();
	}

	public ListIterator<Object> listIterator (int index) {
		if (values == null) {
			return null;
		}
		return values.listIterator (index);
	}

	public boolean removeAll (Collection<?> c) {
		if (values == null) {
			return false;
		}
		return values.removeAll (c);
	}

	public boolean retainAll (Collection<?> c) {
		if (values == null) {
			return false;
		}
		return values.retainAll (c);
	}

	public Object set (int index, Object element) {
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		return values.set (index, element);
	}

	public int size () {
		if (values == null) {
			return 0;
		}
		return values.size ();
	}

	public List<Object> subList (int fromIndex, int toIndex) {
		if (values == null) {
			return null;
		}
		return values.subList (fromIndex, toIndex);
	}

	public Object [] toArray () {
		if (values == null) {
			return null;
		}
		return values.toArray ();
	}

	public <T> T [] toArray (T [] a) {
		if (values == null) {
			return null;
		}
		return values.toArray (a);
	}

}