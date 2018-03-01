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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class PackageClassLoader extends URLClassLoader {
	
	protected static final URL[] EMPTY_URL_ARRAY = new URL[0];

	protected String		 name;
	protected ClassLoader [] dependencies;
	
	protected Object		 main;	 
	
	protected Map<String, Object> registered;
	
	protected Map<String, String> synonyms;
	
	public PackageClassLoader (String name, ClassLoader parent, URL [] urls, ClassLoader... dependencies) {
		super (urls == null ? EMPTY_URL_ARRAY : urls, parent);
		this.name = name;
		this.dependencies = dependencies;
	}

	public PackageClassLoader (String name, URL [] urls, ClassLoader... dependencies) {
		this (name, getSystemClassLoader (), urls, dependencies);
	}

	public PackageClassLoader (String name) {
		this (name, EMPTY_URL_ARRAY);
	}
	
	@Override
	public URL getResource (String name) {
		URL r = null;
		r = findResource (name);
		if (r != null) {
			return r;
		}
		ClassLoader parent = getParent ();
		if (parent != null) {
			r = parent.getResource (name);
		}
		if (r != null) {
			return  r;
		}
		if (dependencies == null || dependencies.length == 0) {
			return null;
		}
		for (ClassLoader cl : dependencies) {
			r = cl.getResource (name);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	@Override
	public InputStream getResourceAsStream (String name) {
		InputStream r = null;
		URL url = findResource (name);
		try {
		    r = url != null ? url.openStream () : null;
		} catch (IOException e) {
		    // IGNORE
		}
		if (r != null) {
			return r;
		}
		ClassLoader parent = getParent ();
		if (parent != null) {
			r = parent.getResourceAsStream (name);
		}
		if (r != null) {
			return  r;
		}
		if (dependencies == null || dependencies.length == 0) {
			return null;
		}
		for (ClassLoader cl : dependencies) {
			r = cl.getResourceAsStream (name);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	public synchronized Class<?> loadClass (String name) throws ClassNotFoundException {
		
		Class<?> c = findLoadedClass (name);

		if (c == null) {
			try {
				c = findClass (name);
			} catch (ClassNotFoundException cnfex) {
				// IGNORE
			}
		}
		
		if (c != null) {
			return c;
		}

		// look at parent before first 
		ClassLoader parent = getParent ();
		if (parent != null) {
			try {
				c = parent.loadClass (name);
			} catch (ClassNotFoundException cnfex) {
				// IGNORE
			}
		}
		if (c != null) {
			return  c;
		}

		// if we could not find it, delegate to dependencies
		if (dependencies == null || dependencies.length == 0) {
			throw new ClassNotFoundException (name);
		}
		
		for (ClassLoader cl : dependencies) {
			try {
				return cl.loadClass (name);
			} catch (ClassNotFoundException cnfex) {
				continue;
			}
		}
		
		throw new ClassNotFoundException (name);
	}

	public void clear () throws IOException {
		dependencies = null;
		try {
			Class<?> clazz = java.net.URLClassLoader.class;
			java.lang.reflect.Field ucp = clazz.getDeclaredField ("ucp");
			ucp.setAccessible (true);
			Object sun_misc_URLClassPath = ucp.get (this);
			java.lang.reflect.Field loaders = sun_misc_URLClassPath.getClass ().getDeclaredField ("loaders");
			loaders.setAccessible (true);
			Object java_util_Collection = loaders.get (sun_misc_URLClassPath);
			for (Object sun_misc_URLClassPath_JarLoader : ((java.util.Collection<?>) java_util_Collection).toArray ()) {
				try {
					java.lang.reflect.Field loader = sun_misc_URLClassPath_JarLoader.getClass ().getDeclaredField ("jar");
					loader.setAccessible (true);
					Object java_util_jar_JarFile = loader.get (sun_misc_URLClassPath_JarLoader);
					((java.util.jar.JarFile) java_util_jar_JarFile).close ();
				} catch (Throwable t) {
					// if we got this far, this is probably not a JAR loader so
					// skip it
				}
			}
		} catch (Throwable t) {
			// probably not a SUN VM
		}
	}
	
	public void addSynonym (String key, String name) {
		if (synonyms == null) {
			synonyms = new HashMap<String, String> ();
		}
		synonyms.put (key, name);
	}
	
	public String synonym (String key) {
		if (synonyms == null) {
			return null;
		}
		return synonyms.get (key);
	}
	
	public boolean hasSynonym (String key) {
		if (synonyms == null) {
			return false;
		}
		return synonyms.containsKey (key);
	}
	
	public void removeSynonym (String key) {
		if (synonyms == null) {
			return;
		}
		synonyms.remove (key);
	}
	
	public void registerObject (String name, Object object) {
		if (registered == null) {
			registered = new HashMap<String, Object> ();
		}
		registered.put (name, object);
	}
	public Object lookupObject (String name) {
		if (registered == null) {
			return null;
		}
		return registered.get (name);
	}
	public void removeObject (String name) {
		if (registered == null) {
			return;
		}
		registered.remove (name);
	}
	
}
