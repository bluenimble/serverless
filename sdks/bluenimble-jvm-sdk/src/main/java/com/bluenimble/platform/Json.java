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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonEntity;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.EncryptionProvider;
import com.bluenimble.platform.security.EncryptionProvider.Mode;
import com.bluenimble.platform.security.EncryptionProviderException;
import com.bluenimble.platform.templating.ExpressionCompiler;
import com.bluenimble.platform.templating.SimpleVariableResolver;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.BasicVariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class Json {

	public static JsonObject object () {
		return new JsonObject ();
	}
	
	public static JsonArray array () {
		return new JsonArray ();
	}
	
	public static JsonObject load (String name) throws Exception {
		return load (Thread.currentThread ().getContextClassLoader (), name);
	}
	
	public static JsonObject load (InputStream stream) throws Exception {
		return load (stream, null);
	}
	
	public static JsonObject load (InputStream stream, String salt) throws Exception {
		if (!Lang.isNullOrEmpty (salt)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream ();
			EncryptionProvider.Default.crypt (stream, out, salt, Mode.Decrypt);
			stream = new ByteArrayInputStream (out.toByteArray ());
		}
		return new JsonObject (IOUtils.toString (stream));
	}
    
	public static JsonObject load (File file) throws Exception {
		return load (file, null);
	}
	
	public static JsonObject load (File file, String salt) throws Exception {
		
		if (!file.exists ()) {
			return null;
		}
		
		InputStream stream = null;
		
		try {
			stream = new FileInputStream (file);
			return load (stream, salt);
		} finally {
			IOUtils.closeQuietly (stream);
		}
		
	}
    
	public static JsonObject load (ClassLoader loader, String name) throws Exception {
		if (Lang.isNullOrEmpty (name)) {
			throw new NullPointerException ("load with no parameters");
		}
		InputStream is = null;
		try { 
			
			if (name.indexOf (Lang.URL_ACCESSOR) > 0) {
				URL url = new URL (name);
				is = url.openStream ();
			} else {
				is = loader.getResourceAsStream (name);
			}
			
			return new JsonObject (IOUtils.toString (is));
			
		} finally {
			if (is != null) {
				try {
					is.close ();
				} catch (IOException ex) {
					ex.printStackTrace (System.err);
				}
			}
		}
	}
    
    public static void store (JsonObject source, File file) throws IOException {
    	store (source, file, false);
    }
    
    public static void store (JsonObject source, File file, boolean cast) throws IOException {
    	store (source, file, null, false, cast);
    }
    
    public static void store (JsonObject source, File file, String paraphrase) throws IOException {
    	store (source, file, paraphrase, false);
    }

    public static void store (JsonObject source, File file, String paraphrase, boolean base64) throws IOException {
    	store (source, file, paraphrase, base64, false);
    }
    public static void store (JsonObject source, File file, String paraphrase, boolean base64, boolean cast) throws IOException {
    	if (source == null) {
    		source = new JsonObject ();
    	}
    	OutputStream os = null;
    	try {
    		os = Lang.isNullOrEmpty (paraphrase) ? new FileOutputStream (file) : new ByteArrayOutputStream ();
    		IOUtils.copy (new ByteArrayInputStream (source.toString (2, cast).getBytes ()), os);
    	} finally {
    		if (Lang.isNullOrEmpty (paraphrase)) {
        		IOUtils.closeQuietly (os);
    		}
    	}
    	
    	if (!Lang.isNullOrEmpty (paraphrase)) {
    		OutputStream out = null;
    		try {
    			out = base64 ? new ByteArrayOutputStream () : new FileOutputStream (file);
    			EncryptionProvider.Default.crypt (
    				new ByteArrayInputStream (((ByteArrayOutputStream)os).toByteArray ()), 
    				out, pad (paraphrase), Mode.Encrypt
    			);
    			
    			if (base64) {
    				byte [] bytes = Base64.encodeBase64 (((ByteArrayOutputStream)out).toByteArray ());
    				out = new FileOutputStream (file);
    				out.write (bytes);
    				out.flush ();
    			}
    		} catch (EncryptionProviderException e) {
    			throw new IOException (e.getMessage (), e);
			} finally {
        		IOUtils.closeQuietly (out);
        	}
    	}
    }
    
    public static void encrypt (JsonObject source, String paraphrase, OutputStream out) throws EncryptionProviderException {
    	
    	EncryptionProvider.Default.crypt (
    		new ByteArrayInputStream (source.toString ().getBytes ()), 
			out, pad (paraphrase), Mode.Encrypt
		);
    	
    }

    public static JsonObject getObject (JsonObject source, String name) {
    	if (source == null || Lang.isNullOrEmpty (name)) {
    		return null;
    	}
    	return (JsonObject)source.get (name);
    }

    public static JsonArray getArray (JsonObject source, String name) {
    	if (source == null) {
    		return null;
    	}
    	return (JsonArray)source.get (name);
    }

    public static String getString (JsonObject source, String name) {
    	return getString (source, name, null);
    }

    public static String getString (JsonObject source, String name, String defaultValue) {
    	if (source == null) {
    		return defaultValue;
    	}
    	String v = source.getString (name);
    	if (v == null) {
    		return defaultValue;
    	}
    	return v;
    }

    public static int getInteger (JsonObject source, String name, int defaultValue) {
    	
    	int value = defaultValue;
    	
    	if (source == null || source.get (name) == null) {
    		return value;
    	}

    	String sInteger = String.valueOf (source.get (name));
		if (!Lang.isNullOrEmpty (sInteger)) {
			try {
				value = Integer.parseInt (sInteger.trim ());
			} catch (NumberFormatException nfex) {
				// Ignore
			}
		}
		return value;
    }

    public static long getLong (JsonObject source, String name, long defaultValue) {
    	
    	long value = defaultValue;
    	
    	if (source == null || source.get (name) == null) {
    		return value;
    	}

    	String sLong = String.valueOf (source.get (name));
		if (!Lang.isNullOrEmpty (sLong)) {
			try {
				value = Long.parseLong (sLong.trim ());
			} catch (NumberFormatException nfex) {
				// Ignore
			}
		}
		return value;
    }

    public static boolean getBoolean (JsonObject source, String name, boolean defaultValue) {
    	
    	boolean value = defaultValue;
    	
    	if (source == null || source.get (name) == null) {
    		return value;
    	}

    	String sBoolean = String.valueOf (source.get (name));
		if (!Lang.isNullOrEmpty (sBoolean)) {
			value = Lang.TrueValues.contains (sBoolean.trim ().toLowerCase ());
		}
		return value;
    }

    public static double getDouble (JsonObject source, String name, double defaultValue) {
    	
    	double value = defaultValue;
    	
    	if (source == null || source.get (name) == null) {
    		return value;
    	}

    	String sDouble = String.valueOf (source.get (name));
		if (!Lang.isNullOrEmpty (sDouble)) {
			try {
				value = Double.parseDouble (sDouble.trim ());
			} catch (NumberFormatException nfex) {
				// Ignore
			}
		}
		return value;
    }

    public static float getFloat (JsonObject source, String name, float defaultValue) {
    	
    	float value = defaultValue;
    	
    	if (source == null || source.get (name) == null) {
    		return value;
    	}

    	String sFloat = String.valueOf (source.get (name));
		if (!Lang.isNullOrEmpty (sFloat)) {
			try {
				value = Float.parseFloat (sFloat.trim ());
			} catch (NumberFormatException nfex) {
				// Ignore
			}
		}
		return value;
    }

    public static Date getDate (JsonObject source, String name) throws ParseException {
    	
    	if (source == null || source.get (name) == null) {
    		return null;
    	}
    	
    	Object oDate = source.get (name);
    	if (oDate instanceof Date) {
    		return (Date)oDate;
    	}
    	
		return Lang.toUTC (String.valueOf (oDate));
		
    }

    public static String escape (String value) {
		if (value == null || value.length () == 0) {
			return Lang.BLANK;
		}
		char b;
		char c = 0;
		int i;
		int len = value.length();
		
        StringBuilder sb = new StringBuilder(len + 4);
		String t;
        
		for (i = 0; i < len; i += 1) {
			b = c;
			c = value.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			case '/':
				if (b == '<') {
					sb.append('\\');
				}
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					t = "000" + Integer.toHexString(c);
					sb.append("\\u" + t.substring(t.length() - 4));
				} else {
					sb.append(c);
				}
			}
		}
		String s = sb.toString();
        sb.setLength (0);
        sb = null;
        return s;
    }
    
    public static Object find (JsonObject target, String... path) {
    	
    	if (path == null || path.length == 0) {
    		return null;
    	}

    	Object value = target;
    	
    	for (String name : path) {
			if (value instanceof JsonObject) {
				value = ((JsonObject)value).get (name);
			} else if (value instanceof JsonArray) {
				int index = 0;
				try {
					index = Integer.valueOf (name.trim ());
					value = ((JsonArray)value).get (index);
				} catch (NumberFormatException nfex) {
					value = null;
				}
			} 
			if (value == null) {
				break;
			}
			if (!JsonEntity.class.isAssignableFrom (value.getClass ())) {
				break;
			}
		}
		
		return value;
    }
    
    public static void set (JsonObject target, String property, Object value) {
    	
    	if (value == null || Lang.isNullOrEmpty (property)) {
    		return;
    	}
    	
    	String [] path = Lang.split (property, Lang.DOT);
    	if (path.length == 1) {
    		target.set (path [0], value);
    		return;
    	}
    	
    	JsonObject current = target;
    	
    	for (int i = 0; i < path.length - 1; i++) {
    		
    		String name = path [i];
    		
    		Object next = current.get (name);
    		if (next == null) {
    			next = new JsonObject ();
    			current.set (name, next);
    		} else if (!(next instanceof JsonObject)) {
    			return;
    		}
    		current = (JsonObject)next; 
		}
    	
    	current.set (path [path.length - 1], value);
    }
    
    public static void remove (JsonObject target, String... path) {
		
    	if (path == null || path.length == 0) {
    		return;
    	}
    	
    	if (path.length == 1) {
    		target.remove (path [0]);
    		return;
    	}
    	
    	Object value = target;
    	
    	for (int i = 0; i < path.length - 1; i++) {
    		
    		String name = path [i];
    		
			if (value instanceof JsonObject) {
				value = ((JsonObject)value).get (name);
			} else if (value instanceof JsonArray) {
				int index = 0;
				try {
					index = Integer.valueOf (name.trim ());
					value = ((JsonArray)value).get (index);
				} catch (NumberFormatException nfex) {
					value = null;
				}
			} 
			if (value == null) {
				break;
			}
			if (!JsonEntity.class.isAssignableFrom (value.getClass ())) {
				break;
			}
		}
    	
    	if (value == null || !JsonEntity.class.isAssignableFrom (value.getClass ())) {
			return;
		}
    	
    	if (value instanceof JsonObject) {
			((JsonObject)value).remove (path [path.length - 1]);
		} else if (value instanceof JsonArray) {
			int index = 0;
			try {
				index = Integer.valueOf (path [path.length - 1].trim ());
				((JsonArray)value).remove (index);
			} catch (NumberFormatException nfex) {
			}
		} 
		
    }
    
    public static JsonObject template (JsonObject template, JsonObject data, boolean withScripting) {
    	if (Json.isNullOrEmpty (template) || Json.isNullOrEmpty (data)) {
    		return template;
    	}
    	return (JsonObject)resolve (
    		template.duplicate (), 
    		withScripting ? Lang.ScriptedExpressionCompiler : Lang.ExpressionCompiler, 
    		new BasicVariableResolver (data)
    	);
    }
    
	public static Object resolve (Object obj, ExpressionCompiler compiler, VariableResolver vr) {
		if (obj == null || vr == null) {
			return obj;
		}
		if (obj instanceof JsonObject) {
			JsonObject o = (JsonObject)obj;
			if (o.isEmpty ()) {
				return o;
			}
			Set<String> removed = null;
			Iterator<String> keys = o.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				Object v = resolve (o.get (key), compiler, vr);
				if (v == null) {
					if (removed == null) {
						removed = new HashSet<String> ();
					}
					removed.add (key);
				} else {
					o.set (key, v);
				}
			}
			// remove nulls
			if (removed != null && !removed.isEmpty ()) {
				for (String k : removed) {
					o.remove (k);
				}
			}
			return o;
		} else if (obj instanceof JsonArray) {
			JsonArray array = (JsonArray)obj;
			for (int i = 0; i < array.count (); i++) {
				Object resolved = resolve (array.get (i), compiler, vr);
				array.remove (i); 
				array.add (i, resolved); 
			}
			return array;
		} else if (obj instanceof String) {
			String exp = String.valueOf (obj);
			if (Lang.isNullOrEmpty (exp)) {
				return obj;
			}
			return compiler.compile (exp, null).eval (vr);
		} 
		return obj;
	}
	
	public static boolean isNullOrEmpty (JsonObject o) {
		return o == null || o.isEmpty ();
	}
	
	public static boolean isNullOrEmpty (JsonArray a) {
		return a == null || a.isEmpty ();
	}
	
	public static boolean areEqual (Object leftValue, Object rightValue) {
		if (leftValue == null && rightValue == null) {
			return true;
		}
		if (leftValue == null && rightValue != null) {
			return false;
		}
		if (leftValue != null && rightValue == null) {
			return false;
		}
		
		if (leftValue instanceof JsonObject) {
			if (! (rightValue instanceof JsonObject) ) {
				return false;
			}
			boolean areEqual = jsonEqual ((JsonObject)leftValue, (JsonObject)rightValue);
			if (!areEqual) {
				return false;
			}
		} else if (leftValue instanceof JsonArray) {
			if (! (rightValue instanceof JsonArray) ) {
				return false;
			}
			JsonArray aLeftValue 	= (JsonArray)leftValue;
			JsonArray aRightValue 	= (JsonArray)rightValue;
			if (aLeftValue.count () != aRightValue.count ()) {
				return false;
			}
			for (int i = 0; i < aLeftValue.count (); i++) {
				boolean areEqual = areEqual (aLeftValue.get (i), aRightValue.get (i));
				if (!areEqual) {
					return false;
				}
			}
		} else {
			return leftValue.equals (rightValue);
		}
		return true;
	}
	
	public static boolean jsonEqual (JsonObject left, JsonObject right) {
		if (Json.isNullOrEmpty (left) && Json.isNullOrEmpty (right)) {
			return true;
		}
		if (Json.isNullOrEmpty (left) && !Json.isNullOrEmpty (right)) {
			return false;
		}
		if (!Json.isNullOrEmpty (left) && Json.isNullOrEmpty (right)) {
			return false;
		}
		
		Iterator<String> leftKeys = left.keys ();
		while (leftKeys.hasNext ()) {
			String key = leftKeys.next ();
			Object leftValue 	= left.get (key);
			
			Object rightValue 	= right.get (key);
			if (rightValue == null) {
				return false;
			}
			
			boolean areEqual = areEqual (leftValue, rightValue);
			if (!areEqual) {
				return false;
			}
		}
		
		Iterator<String> rightKeys = right.keys ();
		while (rightKeys.hasNext ()) {
			boolean hasKey = left.containsKey (rightKeys.next ());
			if (!hasKey) {
				return false;
			}
		}
		
		return true;
	}
	
	public static String toUrlParameters (JsonObject object) {
		if (Json.isNullOrEmpty (object)) {
			return Lang.BLANK;
		}
		boolean empty = true;
		StringBuilder result = new StringBuilder ();
		Iterator<String> keys = object.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			Object child = object.get (key);
			if ((child instanceof JsonObject) || (child instanceof JsonArray)) {
				continue;
			}
			if (!empty) {
				result.append (Lang.AMP);
			}
			empty = false;
			result.append (key).append (Lang.EQUALS).append (child);
		}
		String sResult = result.toString ();
		result.setLength (0);
		return sResult;
	}
	
	public static JsonObject toStructuredObject (JsonObject nameValuePairs) {
		if (Json.isNullOrEmpty (nameValuePairs)) {
			return null;
		}
		JsonObject result = new JsonObject ();
		Iterator<String> accessors = nameValuePairs.keys ();
		while (accessors.hasNext ()) {
			String accessor = accessors.next ();
			Json.set (result, accessor, nameValuePairs.get (accessor));
		}
		return result;
	}
	
	private static String pad (String paraphrase) {
		if (paraphrase.length () == 16) {
			return paraphrase;
		} else if (paraphrase.length () > 16) {
			return paraphrase.substring (0, 15);
		} else {
			StringBuilder sb = new StringBuilder (paraphrase);
			
			for (int i = 0; i < (16 - paraphrase.length ()); i++) {
				sb.append ("0");
			}
			String s = sb.toString ();
			sb.setLength (0);
			return s;
		}
	}

    public static void main (String [] args) throws Exception {
    	ExpressionCompiler ECompiler = new DefaultExpressionCompiler ();
    	
    	JsonObject master = new JsonObject ("{ provider: 'remote.default', spec: { endpoint: \"https://[scheduler | 'scheduler'].display.stream:9335/schedulers/default\" } }") ;
    	
    	JsonObject rdata = new JsonObject ();
    	//rdata.set ("scheduler", "scheduler-001");
    	
    	Json.resolve (master, ECompiler, new SimpleVariableResolver () {
			private static final long serialVersionUID = 1L;
			@Override
			public Object resolve (String namespace, String... property) {
				Object v = Json.find (rdata, property);
				Json.remove (rdata, property);
				return v;
			}
		});
    	System.out.println (master);
    }
    
}
