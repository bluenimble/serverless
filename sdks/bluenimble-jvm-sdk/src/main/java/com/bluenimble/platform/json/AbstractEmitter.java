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

import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;

public abstract class AbstractEmitter implements JsonEmitter {
	
	protected static final Set<Class<?>> CastTypes = new HashSet<Class<?>> ();
	static {
		CastTypes.add (Integer.class); CastTypes.add (Integer.TYPE);
		CastTypes.add (Long.class); CastTypes.add (Long.TYPE);
		CastTypes.add (Double.class); CastTypes.add (Double.TYPE);
		CastTypes.add (Float.class); CastTypes.add (Float.TYPE);
		CastTypes.add (Boolean.class); CastTypes.add (Boolean.TYPE);
	}
	
	private boolean		pretty;
	
	private String 		tab 	= Lang.TAB;
	private String 		space 	= Lang.SPACE;
	
	protected int 		indent;
	protected boolean	cast; 
	
	@Override
	public void onStartObject (JsonObject o, boolean root) {
		write (Lang.OBJECT_OPEN);
		if (pretty) {
			onEndLn ();
			if (o.count () > 0) {
				indent++;
			}
		} 
	}

	@Override
	public void onEndObject (JsonObject o, boolean root) {
		if (pretty) {
			onEndLn ();
		} 
		if (pretty && o.count () > 0) {
			indent--;
		}
		indent ();
		write (Lang.OBJECT_CLOSE);
	}

	@Override
	public void onStartProperty (JsonObject p, String name, boolean isLast) {
		indent ();
		writeName (name);
		if (pretty) {
			write (space);
		}
	}

	@Override
	public void onEndProperty (JsonObject p, String name, boolean isLast) {
		if (!isLast) {
			write (Lang.COMMA);
			if (pretty) {
				onEndLn ();
			} 
		}
	}

	@Override
	public void onStartArray (JsonArray a) {
		write (Lang.ARRAY_OPEN);
		if (pretty) {
			onEndLn ();
			if (a.count () > 0) {
				indent++;
			}
		} 
	}

	@Override
	public void onEndArray (JsonArray a) {
		if (pretty) {
			onEndLn ();
		} 
		if (pretty && a.count () > 0) {
			indent--;
		}
		indent ();
		write (Lang.ARRAY_CLOSE);
	}

	@Override
	public void onValue (JsonEntity p, String name, Object value) {
		if (value == null) {
			write (Lang.NULL);
		} else {
			if (!cast || !CastTypes.contains (value.getClass ())) {
				write (Lang.QUOTE);
			}
			
			write (Json.escape (String.valueOf (value)));
			
			if (!cast || !CastTypes.contains (value.getClass ())) {
				write (Lang.QUOTE);
			}
		}
	}

	protected void writeName (String name) {
		write (Lang.QUOTE);
		write (Json.escape (name));
		write (Lang.QUOTE);
		write (Lang.COLON);
	}

	protected void onEndLn () {
		write (Lang.ENDLN);
	}

	@Override
	public void onStartArrayValue (JsonArray array, Object value, boolean isLast) {
		indent ();
	}

	@Override
	public void onEndArrayValue (JsonArray array, Object value, boolean isLast) {
		if (!isLast) {
			write (Lang.COMMA);
			if (pretty) {
				onEndLn ();
			} 
		}
	}
	
	public AbstractEmitter prettify () {
		pretty = true;
		return this; 
	}
	
	public AbstractEmitter tab (String tab) {
		this.tab = tab;
		return this;
	}
	
	public AbstractEmitter cast (boolean cast) {
		this.cast = cast;
		return this;
	}
	
	private void indent () {
		if (indent <= 0) {
			return;
		}
		for (int i = 0; i < indent; i++) {
			write (tab);	
		}
	}
	
}
