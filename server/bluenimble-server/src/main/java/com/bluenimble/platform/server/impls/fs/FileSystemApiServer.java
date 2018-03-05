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
package com.bluenimble.platform.server.impls.fs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.ApiSpace.Spec;
import com.bluenimble.platform.api.ApiStatus;
import com.bluenimble.platform.api.impls.ApiFileStreamSource;
import com.bluenimble.platform.api.impls.ApiSpaceImpl;
import com.bluenimble.platform.api.security.ApiRequestSigner;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.impls.NoTracing;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.cluster.ClusterPeerFactory;
import com.bluenimble.platform.cluster.impls.DefaultClusterPeerFactory;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.PluginsRegistry;
import com.bluenimble.platform.reflect.BeanUtils;
import com.bluenimble.platform.security.impls.JsonKeyPair;
import com.bluenimble.platform.server.ApiRequestVisitor;
import com.bluenimble.platform.server.ServerStartupException;
import com.bluenimble.platform.server.impls.AbstractApiServer;
import com.bluenimble.platform.server.impls.DefaultApiRequestVisitor;
import com.bluenimble.platform.server.interceptor.ApiInterceptor;
import com.bluenimble.platform.server.interceptor.impls.DefaultApiInterceptor;
import com.bluenimble.platform.server.maps.MapProvider;
import com.bluenimble.platform.server.maps.impls.DefaultMapProvider;
import com.bluenimble.platform.server.security.impls.DefaultApiRequestSigner;
import com.bluenimble.platform.server.utils.ConfigKeys;
import com.bluenimble.platform.server.utils.InstallUtils;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;
import com.bluenimble.platform.validation.impls.DefaultApiServiceValidator;

public class FileSystemApiServer extends AbstractApiServer {

	private static final long serialVersionUID = -1614531262910218146L;
	
	protected 					File 		installHome;
	protected 					File 		runtimeHome;
	protected 					File 		tenantHome;
	
	protected 					File 		runtimeLogs;
	
	private 					Map<String, Object> 
											failed = new LinkedHashMap<String, Object> ();
	
	private static final DefaultExpressionCompiler ExpressionCompiler = new DefaultExpressionCompiler ();
	
	private JsonObject 			variables = null;
	private VariableResolver 	variablesResolver;

	public FileSystemApiServer (File installHome, File runtimeHome, File tenantHome) {
		this.installHome 	= installHome;
		this.runtimeHome 	= runtimeHome;
		this.tenantHome 	= tenantHome;
	}
	
	@Override
	public String id () {
		return Json.getString (descriptor, ConfigKeys.Id);
	}

	@Override
	public String type () {
		return Json.getString (descriptor, ConfigKeys.Type);
	}

	@Override
	public int weight () {
		return Json.getInteger (descriptor, ConfigKeys.Weight, 2);
	}

	@Override
	public String version () {
		return Json.getString (descriptor, ConfigKeys.Version);
	}

	@Override
	public void start () throws ServerStartupException {
		
		// check under tenant
		File keysFile = null;
		
		if (tenantHome != null) {
			keysFile = new File (tenantHome, ConfigKeys.RootKeysFile);
		}
		if (keysFile == null || !keysFile.isFile () || !keysFile.exists ()) {
			// check under the runtime home
			keysFile = new File (runtimeHome, ConfigKeys.RootKeysFile);
		}

		if (!keysFile.exists ()) {
			// check under the install home
			keysFile = new File (installHome, ConfigKeys.RootKeysFile);
		}
		
		ClassLoader bluenimbleClassLoader = FileSystemApiServer.class.getClassLoader ();
		
		try {
			
			boolean rootKeysEncrypted = Json.getBoolean (descriptor, ConfigKeys.RootKeysEncrypted, true);
			
			// exit if not found
			if (!keysFile.exists ()) {
				// create
				keys = JsonKeyPair.create ();
				((JsonKeyPair)keys).store (new File (installHome, ConfigKeys.RootKeysFile), rootKeysEncrypted);
			}
			
			setup ();
			
			if (keys == null) {
				keys = new JsonKeyPair (keysFile, rootKeysEncrypted);
			}
			
			Runtime.getRuntime ().addShutdownHook (new Thread () {
				@Override
				public void run () {
					FileSystemApiServer.this.stop ();
				}
			});
			
			// messages
			File mFile = new File (installHome, ConfigKeys.DefaultMessages);
			if (mFile.exists ()) {
				messages.putAll (Json.load (mFile));
			}
			
			pluginsRegistry = (PluginsRegistry)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.PluginsRegistry));
			pluginsRegistry.init (
				this, 
				new File (installHome, ConfigKeys.Folders.Plugins)
			);
			
			// init tracer
			JsonObject oTracer = Json.getObject (descriptor, ConfigKeys.Tracer);
			if (!Json.isNullOrEmpty (oTracer)) {
				tracer = (Tracer)BeanUtils.create (bluenimbleClassLoader, oTracer, pluginsRegistry);
			}
			if (tracer != null) {
				tracer.onInstall (this);
			} else {
				tracer = NoTracing.Instance;
			}
			
			tracer.log (Tracer.Level.Info, "Instance Config:\n{0}", descriptor);
			
			mapProvider = (MapProvider)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.MapProvider), pluginsRegistry);
			if (mapProvider == null) {
				mapProvider = new DefaultMapProvider ();
			}			

			ClusterPeerFactory clusterPeerFactory = (ClusterPeerFactory)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.ClusterPeerFactory), pluginsRegistry);
			if (clusterPeerFactory == null) {
				clusterPeerFactory = new DefaultClusterPeerFactory ();
			}			
			peer = clusterPeerFactory.create ();

			interceptor = (ApiInterceptor)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.Interceptor), pluginsRegistry);
			if (interceptor == null) {
				interceptor = new DefaultApiInterceptor ();
			}			
			interceptor.init (this);
			
			serviceValidator = (ApiServiceValidator)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.ServiceValidator), pluginsRegistry);
			if (serviceValidator == null) {
				serviceValidator = new DefaultApiServiceValidator ();
			}
			
			requestSigner = (ApiRequestSigner)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.RequestSigner), pluginsRegistry);
			if (requestSigner == null) {
				requestSigner = new DefaultApiRequestSigner ();
			}
			
			requestVisitor = (ApiRequestVisitor)BeanUtils.create (bluenimbleClassLoader, Json.getObject (descriptor, ConfigKeys.RequestVisitor), pluginsRegistry);
			if (requestVisitor == null) {
				requestVisitor = new DefaultApiRequestVisitor ();
			}
			
			pluginsRegistry.start ();
			
		} catch (Throwable th) {
			throw new ServerStartupException (th.getMessage (), th);
		}
		
		installSpaces ();

		if (keyStoreManager != null) {
			keyStoreManager.start ();
		}
		
		tracer.log (Tracer.Level.Info, "Instance started @ {0}", new Date ());
		tracer.log (Tracer.Level.Info, "With Root Keys   {0}  [{1}]", 
			keys.accessKey (), 
			keys.expiryDate () == null ? "Never Expires" : Lang.toString (keys.expiryDate (), Lang.DEFAULT_DATE_FORMAT)
		);

		if (!failed.isEmpty ()) {
			Iterator<String> names = failed.keySet ().iterator ();
			while (names.hasNext ()) {
				String name = names.next ();
				printFailed (name, failed.get (name));
			}
		}
		
		// clear failed exceptions
		failed.clear ();

	}
	
	public File tenant () {
		return tenantHome;
	}
	
	public File install () {
		return installHome;
	}
	
	public File runtime () {
		return runtimeHome;
	}
	
	public File runtimeLogs () {
		return runtimeLogs;
	}
	
	private void installSpaces () throws ServerStartupException {
		// installing spaces
		File spacesHome = new File (runtimeHome, ConfigKeys.Folders.Spaces);
		
		File [] spaces = spacesHome.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.isDirectory ();
			}
		});
		for (File fSpace : spaces) {
			installSpace (fSpace);
		}
	}

	private void installSpace (File spaceHome) throws ServerStartupException {
		
		tracer.log (Tracer.Level.Info, "Install Space {0}", spaceHome.getName ());
		
		JsonObject oSpace = null;
		
		File fDescriptor = new File (spaceHome, ConfigKeys.Descriptor.Space);
		if (fDescriptor.exists ()) {
			try {
				oSpace = resolve (Json.load (fDescriptor));
			} catch (Exception ex) {
				failed.put ("unnable to load space '" + spaceHome.getName () + "'", ex);
				return;
			} 
		}
		if (oSpace == null) {
			throw new ServerStartupException ("space descriptor for " + spaceHome.getName () + " not found");
		}
		if (!oSpace.containsKey (ApiSpace.Spec.Namespace)) {
			oSpace.set (ApiSpace.Spec.Namespace, spaceHome.getName ());
		}
		
		ApiSpaceImpl space;
		try {
			space = (ApiSpaceImpl)create (oSpace, false);
			// save space descriptor, change maybe made by plugins onEvent/Create
			// Json.store (oSpace, fDescriptor);
		} catch (Exception ex) {
			throw new ServerStartupException (ex.getMessage (), ex);
		}
		
		File [] apis = spaceHome.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.isDirectory () || (file.isFile () && file.getAbsolutePath ().endsWith (ConfigKeys.ApiExt));
			}
		});
		
		if (apis == null || apis.length == 0) {
			tracer.log (Tracer.Level.Info, "\tno apis found in space [{0}]", spaceHome.getName ());
			return;
		}
		
		tracer.log (Tracer.Level.Info, "\tfound ({0}) Api(s) in [{1}]", apis.length, spaceHome.getName ());

		for (File aFile : apis) {
			Api api = null;
			if (aFile.isDirectory ()) {
				try {
					api = space.install (aFile);
				} catch (Exception ex) {
					failed.put (space.getNamespace () + " > " + aFile.getName (), ex);
					continue;
				} 
			} else if (aFile.isFile ()) {
				ApiFileStreamSource is = new ApiFileStreamSource (aFile, ConfigKeys.ApiExt);
				try {
					api = space._install (is);
				} catch (Exception ex) {
					failed.put (space.getNamespace () + " > " + aFile.getName (), ex);
					continue;
				} finally {
					IOUtils.closeQuietly (is.stream ());
				}
				
				try {
					FileUtils.delete (aFile);
				} catch (IOException ex) {
					tracer.log (Tracer.Level.Error, "\tcan't delete api file {0} / {1} > ", spaceHome.getName (), aFile.getName ());
				}
			}
			
			if (api != null && ApiStatus.Failed.equals (api.status ())) {
				failed.put (space.getNamespace () + "/" + aFile.getName (), api.getFailure ());
			}

		}
	}

	@Override
	public ApiSpace create (JsonObject oSpace) throws ApiManagementException {
		return create (oSpace, true);
	}
	
	private ApiSpace create (JsonObject oSpace, boolean save) throws ApiManagementException {
		String spaceNs = Json.getString (oSpace, Spec.Namespace);
		if (Lang.isNullOrEmpty (spaceNs)) {
			throw new ApiManagementException ("space namespace not found");
		}
		
		if (!InstallUtils.isValidSpaceNs (spaceNs)) {
			throw new ApiManagementException ("invalid space namespace '" + spaceNs + "'");
		}
		
		File spacesHome = new File (runtimeHome, ConfigKeys.Folders.Spaces);
		File spaceHome = new File (spacesHome, spaceNs);
		if (!spaceHome.exists ()) {
			spaceHome.mkdir ();
		}
		
		ApiSpace space = null;
		try {
			space = new ApiSpaceImpl (this, oSpace, spaceHome);
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		}
		
		addSpace (space);
		
		if (save) {
			try {
				Json.store (oSpace, new File (spaceHome, ConfigKeys.Descriptor.Space));
			} catch (Exception ex) {
				throw new ApiManagementException (ex.getMessage (), ex);
			} 
		}
		
		// notify space creation
		try {
			if (space.isStarted ()) {
				getPluginsRegistry ().onEvent (Event.Create, space);
			}
		} catch (Exception ex) {
			throw new ApiManagementException (ex.getMessage (), ex);
		} 
		
		return space;
	}

	@Override
	public void stop () {
		
		tracer.log (Tracer.Level.Info, "Shutting down BlueNimble Node");
		
		Collection<ApiSpace> spaces = spaces ();
		
		if (spaces != null) {
			for (ApiSpace space : spaces) {
				((ApiSpaceImpl)space).shutdown ();
			}
		}

		if (pluginsRegistry != null) {
			pluginsRegistry.shutdown ();
		}
		
		if (keyStoreManager != null) {
			keyStoreManager.stop ();
		}
		
		tracer.onShutdown (this);
		
	}
	
	@Override
	public JsonObject resolve (JsonObject descriptor) {
		return (JsonObject)Json.resolve (descriptor, ExpressionCompiler, variablesResolver);
	}

	private void setup () throws Exception {
		
		if (tenantHome != null) {
			File varsFile = new File (tenantHome, ConfigKeys.VariablesFile);
			if (varsFile.exists () && varsFile.isFile ()) {
				variables = Json.load (new File (tenantHome, ConfigKeys.VariablesFile));
			}
		}
		
		variablesResolver = new VariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;

			@Override
			public Object resolve (String namespace, String... property) {
				if (Lang.isNullOrEmpty (namespace)) {
					return null;
				}
				if (namespace.equals ("server")) {
					return Json.find (descriptor, property);
				} else if (namespace.equals ("sys")) {
					return System.getProperty (Lang.join (property, Lang.DOT));
				} else if (namespace.equals ("vars")) {
					if (variables == null) {
						return null;
					}
					return Json.find (variables, property);
				}
				return null;
			}
			
		};
		
		if (!runtimeHome.exists ()) {
			runtimeHome.mkdir ();
		}
		
		if (tenantHome != null) {
			runtimeLogs = new File (tenantHome, ConfigKeys.Folders.Logs);
		} else {
			runtimeLogs = new File (runtimeHome, ConfigKeys.Folders.Logs);
		}
		if (!runtimeLogs.exists ()) {
			runtimeLogs.mkdirs ();
		}
		
		descriptor = null;
		
		File bluenimbleFile = new File (installHome, ConfigKeys.InstanceConfig);
		if (bluenimbleFile.exists ()) {
			descriptor = resolve ((JsonObject)Json.load (bluenimbleFile));
		} else {
			descriptor = new JsonObject ();
		}
		
		// resolve descriptor
		
		Lang.setDebugMode (Json.getBoolean (descriptor, ConfigKeys.Debug, false));
		
		File pluginsFolder = new File (installHome, ConfigKeys.Folders.Plugins);
		if (!pluginsFolder.exists ()) {
			pluginsFolder.mkdir ();
		}
		
		File backup = new File (runtimeHome, ConfigKeys.Folders.Backup);
		if (!backup.exists ()) {
			backup.mkdir ();
		}
		
		File logs = new File (runtimeHome, ConfigKeys.Folders.Logs);
		if (!logs.exists ()) {
			logs.mkdir ();
		}
		
		File spacesFolder = new File (runtimeHome, ConfigKeys.Folders.Spaces);
		if (!spacesFolder.exists ()) {
			spacesFolder.mkdir ();
		}

		// copy pre-installed spaces
		copySpacesToRuntime ();
		
	}
	
	private void copySpacesToRuntime () throws Exception {
		
		File spacesInRuntime = new File (runtimeHome, ConfigKeys.Folders.Spaces);
		if (!spacesInRuntime.exists ()) {
			spacesInRuntime.mkdir ();
		}		

		// Copy space from base install
		
		File spacesInInstall = new File (installHome, ConfigKeys.Folders.Spaces);
		if (!spacesInInstall.exists () || !spacesInInstall.isDirectory ()) {
			return;
		}
		
		copySpacesToRuntime (spacesInInstall);
		
		// if spaces folder found in config
		String externalSpacesPath = Json.getString (descriptor, ConfigKeys.Spaces);
		if (Lang.isNullOrEmpty (externalSpacesPath)) {
			return;
		}
				
		File externalSpaces = new File (externalSpacesPath);
		if (!externalSpaces.exists () || !externalSpaces.isDirectory ()) {
			return;
		}
		
		copySpacesToRuntime (externalSpaces);
		
	}
	
	private void copySpacesToRuntime (File spacesFolder) throws Exception {
		File [] spacesFound = spacesFolder.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.isDirectory ();
			}
		});
		if (spacesFound == null || spacesFound.length == 0) {
			return;
		}
		
		for (int i = 0; i < spacesFound.length; i++) {
			copySpaceToRuntime (spacesFound [i]);
		}
	}
	
	private void copySpaceToRuntime (File fSpace) throws Exception {

		File spaceDescriptor = new File (fSpace, ConfigKeys.Descriptor.Space);
		if (!spaceDescriptor.exists () || !spaceDescriptor.isFile ()) {
			return;
		}
		
		File spaceInRuntime = new File (new File (runtimeHome, ConfigKeys.Folders.Spaces), fSpace.getName ());
		if (!spaceInRuntime.exists ()) {
			spaceInRuntime.mkdir ();
		}
		
		File [] apis = fSpace.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.getName ().endsWith (ConfigKeys.ApiExt) || file.isDirectory ();
			}
		});
		if (apis != null && apis.length > 0) {
			for (File fApi : apis) {
				String name = fApi.getName ();
				File apiInRuntime = new File (spaceInRuntime, name);
				
				if (fApi.isFile ()) {
					String folderName = name.substring (0, name.lastIndexOf (ConfigKeys.ApiExt));
					apiInRuntime = new File (spaceInRuntime, folderName);
				}
				
				if (apiInRuntime.exists ()) {
					FileUtils.delete (apiInRuntime);
				}
				
				if (fApi.isFile ()) {
					apiInRuntime.mkdirs ();
					// decompress in apiInRuntime
					ArchiveUtils.decompress (fApi, apiInRuntime, false);
				} else {
					FileUtils.copy (fApi, spaceInRuntime, true);
				}
			}
		}
		
		// copy space.json
		FileUtils.copy (spaceDescriptor, spaceInRuntime, true);
		
		// copy status.json
		File spaceStatus = new File (fSpace, ConfigKeys.StatusFile);
		if (spaceStatus.exists () && spaceStatus.isFile ()) {
			FileUtils.copy (spaceStatus, spaceInRuntime, true);
		}
		
		// copy space.keystore if any - this is valid only if defaut security applied
		File spaceKeyStore = new File (fSpace, ConfigKeys.KeyStoreFile);
		if (spaceKeyStore.exists () && spaceKeyStore.isFile ()) {
			FileUtils.copy (spaceKeyStore, spaceInRuntime, true);
		}
		
		if (Json.getBoolean (descriptor, ConfigKeys.DeleteInstalledSpaces, true)) {
			FileUtils.delete (fSpace);
		}
		
	}
	
	private void printFailed (String name, Object error) {
		if (error instanceof Throwable) {
			tracer.log (Tracer.Level.Error, "\n<{0}>\n{1}", name, Lang.toString ((Throwable)error));
		} else {
			tracer.log (Tracer.Level.Error, "\n<{0}>\n{1}", name, error);
		}
	}

}
