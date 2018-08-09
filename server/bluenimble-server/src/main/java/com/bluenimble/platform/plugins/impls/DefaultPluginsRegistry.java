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
package com.bluenimble.platform.plugins.impls;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.Plugin;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.PluginsRegistry;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.reflect.ClassLoaderRegistry;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ApiServer.Resolver;
import com.bluenimble.platform.server.utils.ConfigKeys;
import com.bluenimble.platform.server.utils.InstallUtils;

public class DefaultPluginsRegistry implements PluginsRegistry, ClassLoaderRegistry {
	
	private static final long serialVersionUID = 2152070202673863193L;
	
	private static final String PluginsHomePrefix	= "bluenimble.plugins.";
	private static final String PluginsHomePostfix	= ".home";
	
	private static final Comparator<Plugin> Comparator = new Comparator<Plugin>() {
	    @Override
	    public int compare (Plugin left, Plugin right) {
	        return left.getWeight () - right.getWeight (); // use your logic
	    }
	};
	
	private static boolean isWindows;

	private Map<String, Plugin> 	plugins  	= new LinkedHashMap<String, Plugin> ();
	
	private Map<String, String> 	references 	= new HashMap<String, String> ();
	private Map<String, PackageClassLoader> 		
									classLoaders
												= new HashMap<String, PackageClassLoader> ();
	
	private Map<String, JsonObject> tracers 	= new HashMap<String, JsonObject>();
	
	private static String OsFamily;
	private static String OsArc;
	
	static {
		String os 	= System.getProperty ("os.name").toLowerCase ();
		if (os.indexOf ("win") != -1) {
			OsFamily = "win";
			isWindows = true;
		} else if (os.indexOf ("linux") != -1) {
			OsFamily = "linux";
		} else if (os.indexOf ("hpux") != -1) {
			OsFamily = "hpux";
		} else if (os.indexOf ("aix") != -1) {
			OsFamily = "aix";
		} else if (os.indexOf ("freebsd") != -1) {
			OsFamily = "freebsd";
		} else if (os.indexOf ("solaris") != -1) {
			OsFamily = "solaris";
		} else if (os.indexOf ("nix") != -1) {
			OsFamily = "unix";
		}
		
		OsArc	= System.getProperty ("os.arch");
		
	}
	
	private List<String> libraries = Collections.synchronizedList(new ArrayList<String>()); 
	
	private ApiServer 	server;
	
	@Override
	public void install (final ApiServer server, File home) throws PluginRegistryException {
		this.server = server;
		
		server.tracer ().log (Tracer.Level.Info, "Operating System: {0}", OsFamily);
		server.tracer ().log (Tracer.Level.Info, "    Architecture: {0}", OsArc);
		
		File [] plugins = home.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.isDirectory() || (file.isFile () && file.getAbsolutePath ().endsWith (ConfigKeys.PluginExt));
			}
		});
		
		if (plugins == null || plugins.length == 0) {
			return;
		}

		try {
			
			for (File pfile : plugins) {
				install (pfile);
			}
			
			addNativeLibraries ();
			
		} catch (Exception ex) {
			throw new PluginRegistryException (ex.getMessage (), ex);
		}
		
	}
		
	@Override
	public void start () throws PluginRegistryException {
		Iterator<Plugin> ip = plugins.values ().iterator ();
		try {
			while (ip.hasNext ()) {
				Plugin plugin = ip.next ();
				if (plugin.isInitOnInstall ()) {
					continue;
				}
				server.tracer ().log (Tracer.Level.Info, "Initialize plugin {0}", plugin.getNamespace ());
				
				if (plugin.isAsync ()) {
					new Thread () {
						@Override
						public void run () {
							try { 
								_init (server, plugin.getHome (), plugin); 
							} catch (Exception ex) { 
								server.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex); 
							}
						}
					}.start ();
				} else {
					_init (server, plugin.getHome (), plugin);
				}
			}
		} catch (Exception e) {
			throw new PluginRegistryException (e.getMessage (), e);
		}
	}
	
	@Override
	public void install (File file) throws PluginRegistryException {
		
		long 	timestamp 	= file.lastModified ();
		
		server.tracer ().log (Tracer.Level.Info, "\tInstall Plugin {0}", file.getName ());
		
		File home = null;
		
		try {
			
			if (file.isFile ()) {
				String folderName = file.getName ().substring (0, file.getName ().indexOf (ConfigKeys.PluginExt));
				home = new File (file.getParent (), folderName);
				
				InputStream bis = null;
				try {
					bis = new FileInputStream (file);
					ArchiveUtils.decompress (bis, home);
				} finally {
					IOUtils.closeQuietly (bis);
				}
				FileUtils.delete (file);
			} else {
				home = file;
			}
			
			JsonObject descriptor = Json.load (new File (home, ConfigKeys.Descriptor.Plugin));
			
			String pluginNs = Json.getString (descriptor, ConfigKeys.Namespace);
			if (Lang.isNullOrEmpty (pluginNs)) {
				throw new PluginRegistryException (
					"Plugin namespace not found in descriptor " + home.getAbsolutePath () + "\n" + descriptor
				);
			}
			if (!InstallUtils.isValidPluginNs (pluginNs)) {
				throw new PluginRegistryException ("Invalid plugin namespace " + pluginNs);
			}
			
			// resolve vars
			descriptor = server.resolve (descriptor, InstallUtils.varsMapping (Resolver.Prefix.This, Resolver.Namespace.Plugins, pluginNs));
			
			if (!Json.getBoolean (descriptor, ConfigKeys.Install, true)) {
				return;
			}
			
			// set system properties
			JsonObject sysprops = Json.getObject (descriptor, ConfigKeys.SystemProperties);
			if (!Json.isNullOrEmpty (sysprops)) {
				Iterator<String> props = sysprops.keys ();
				while (props.hasNext ()) {
					String p = props.next ();
					System.setProperty (p, sysprops.getString (p));
				}
			}
			
			// load native libraries
			boolean nativeLibsRegistered = registerLibrary (home);
			
			if (!nativeLibsRegistered) {
				throw new PluginRegistryException ("native libraries required by plugin [" + pluginNs + "] not found in [Plugin Home]/" + ConfigKeys.Native + Lang.SLASH + OsFamily + Lang.SLASH + OsArc);
			}
			
			// create plugin
			Plugin plugin = create (pluginNs, file, home, timestamp, descriptor);
			
			addPlugin (pluginNs, plugin);
			
			if (plugin.isInitOnInstall ()) {
				_init (server, home, plugin);
			}
			
		} catch (PluginRegistryException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new PluginRegistryException (ex.getMessage (), ex);
		}
	}
	
	private void addPlugin (String pluginNs, Plugin plugin) {
		plugins.put (
			pluginNs, 
			plugin
		);
	}

	private boolean registerLibrary (File home) {
		
		File nativeLibs = new File (home, ConfigKeys.Native);
		if (!nativeLibs.exists ()) {
			return true;
		}
		
		File perOsLibs = new File (nativeLibs, OsFamily + Lang.SLASH + OsArc);
		
		if (!perOsLibs.exists ()) {
			return false;
		}
		
		libraries.add (perOsLibs.getAbsolutePath ());
		
		return true;
		
	}

	private Plugin create (String pluginNs, File pluginFile, File pluginHome, long timestamp, JsonObject descriptor) throws Exception {
		PluginClassLoader pcl = new PluginClassLoader (
			pluginNs, 
			InstallUtils.toUrls (pluginHome, Json.getArray (descriptor, ConfigKeys.Classpath))
		);
			
		Plugin plugin = (Plugin)BeanUtils.create (pcl, Json.getObject (descriptor, ConfigKeys.Spi), this);

		if (plugin == null) {
			plugin = new MockPlugin ();
		} 
		
		if (!(plugin instanceof MockPlugin)) {
			pcl.setPlugin (plugin);
		}
		
		plugin.setNamespace (pluginNs);
		plugin.setHome (pluginHome);
		
		plugin.setName (Json.getString (descriptor, ConfigKeys.Name));
		plugin.setDescription (Json.getString (descriptor, ConfigKeys.Description));
		
		System.setProperty (PluginsHomePrefix + plugin.getNamespace () + PluginsHomePostfix, pluginHome.getAbsolutePath ());
		
		classLoaders.put (pluginNs, pcl);
		
		JsonObject oTracer = Json.getObject (descriptor, ConfigKeys.Tracer);
		if (!Json.isNullOrEmpty (oTracer)) {
			tracers.put (pluginNs, oTracer);
		}
		
		return plugin;
	}
	
	private void _init (ApiServer server, File pHome, Plugin plugin) throws Exception {
		
		if (plugin instanceof MockPlugin) {
			return;
		}
		
		// set plugin tracer
		Tracer plTracer = null;
		
		JsonObject oTracer = tracers.get (plugin.getNamespace ());
		
		// remove from map
		tracers.remove (plugin.getNamespace ());
		
		if (!Json.isNullOrEmpty (oTracer)) {
			plTracer = (Tracer)BeanUtils.create (classLoaders.get (plugin.getNamespace ()), oTracer, this);
		}
		if (plTracer == null) {
			plTracer = server.tracer ();
		} else {
			plTracer.onInstall (plugin);
		}
		
		plugin.setTracer (plTracer);

		// call plugin.init spi
		if (plugin.isIsolated ()) {
			ClassLoader serverClassLoader = Thread.currentThread ().getContextClassLoader ();
			
			Thread.currentThread ().setContextClassLoader (plugin.getClass ().getClassLoader ());
			try {
				plugin.init (server);
			} finally {
				Thread.currentThread ().setContextClassLoader (serverClassLoader);
			}
		} else {
			plugin.init (server);
		}
		
	}

	@Override
	public void uninstall (File pluginFile) throws PluginRegistryException {
		String pluginName = references.get (pluginFile.getAbsolutePath ());
		if (pluginName == null) {
			return;
		}
		Plugin plugin = lockup (pluginName);
		if (plugin == null) {
			return;
		}
		uninstall (plugin, false);
	}
	
	@Override
	public void uninstall (Plugin plugin, boolean keepBinaries) throws PluginRegistryException {
		
		ClassLoader serverClassLoader = Thread.currentThread ().getContextClassLoader ();
		
		Thread.currentThread ().setContextClassLoader (plugin.getClass ().getClassLoader ());
		try {
			plugin.kill ();
		} finally {
			Thread.currentThread ().setContextClassLoader (serverClassLoader);
		}

		if (plugin.isClosable ()) {
			try {
				find (plugin.getNamespace ()).clear ();
			} catch (IOException e) {
				server.tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
			}
		}
		
		server.tracer ().log (Tracer.Level.Info, "\tPlugin {0} destroyed", plugin.getNamespace ());
		
		if (!keepBinaries) {
			Plugin ph = plugins.get (plugin.getNamespace ());
			
			try {
				FileUtils.delete (ph.getHome ());
			} catch (IOException e) {
				throw new PluginRegistryException (e.getMessage (), e);
			}

			server.tracer ().log (Tracer.Level.Info, "\tPlugin {0} binaries deleted", plugin.getNamespace ());
		}
		
	}
	
	@Override
	public Plugin lockup (String name) {
		return plugins.get (name);
	}
	
	@Override
	public Iterator<String> getNames () {
		return plugins.keySet ().iterator ();
	}
	
	@Override
	public void shutdown () {
		if (plugins == null || plugins.isEmpty ()) {
			return;
		}
		
		server.tracer ().log (Tracer.Level.Info, "Shutting down Plugins Registry");
		
		List<Plugin> list = new ArrayList<Plugin> (plugins.values ());
		Collections.sort (list, Comparator);
		
		for (Plugin p : list) {
			try {
				uninstall (p, true);
			} catch (Exception ex) {
				server.tracer ().log (Tracer.Level.Error, Lang.BLANK, ex);
			}
		}
	}

	@Override
	public PackageClassLoader find (String name) {
		return classLoaders.get (name);
	}

	@Override
	public void onEvent (final Event event, final Manageable target, Object... args) throws PluginRegistryException {
		final Iterator<String> names = getNames ();
		while (names.hasNext ()) {
			String plugin = names.next ();
			lockup (plugin).onEvent (event, target);
		}
	}

	private void addNativeLibraries () throws Exception {
		if (libraries == null || libraries.isEmpty ()) {
			return;
		}
		
		String paths = Lang.join (libraries, isWindows ? Lang.SEMICOLON : Lang.COLON);
		
		server.tracer ().log (Tracer.Level.Info, "java.library.path ==> {0}", paths);
		
		System.setProperty ("java.library.path", paths);
		
	}

}
