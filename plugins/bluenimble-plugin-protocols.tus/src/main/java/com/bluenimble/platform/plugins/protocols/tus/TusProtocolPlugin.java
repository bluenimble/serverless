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
package com.bluenimble.platform.plugins.protocols.tus;

import java.util.Iterator;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.protocols.tus.impls.ProcessTusSpi;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.PluginRegistryException;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.plugins.protocols.tus.impl.TusFileUploadService;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadIdFactory;
import com.bluenimble.platform.plugins.protocols.tus.impl.upload.UploadStorageService;
import com.bluenimble.platform.plugins.protocols.tus.storage.StorageFeatureLockingService;
import com.bluenimble.platform.plugins.protocols.tus.storage.StorageFeatureService;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.ApiServer.Event;
import com.bluenimble.platform.server.ApiServer.EventSubject;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.Storage;

public class TusProtocolPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	public interface Spec {
		String Tus 		= "tus";
		
		String Storage 	= "storage";
		String Uri 		= "uri";
		String Folders 	= "folders";
			String Data 	= "data";
			String Locks 	= "locks";
		String MaxSize	= "maxSize";	
		String ExpiresAfter
						= "expiresAfter";
		
		String Serializer
						= "serializer";
		
		String MultiTenant	
						= "multiTenant";
		String TenantKey	= "tenantKey";
		String TenantPlaceholder	
						= "tenantPlaceholder";
		String BypassTenantCheck
						= "bypassTenantCheck";

		String DataFile	= "dataFile";
		String MetaFile	= "metaFile";
		
		String Events	= "events";
			String Create	= "create";
			String Append	= "append";
			String Done		= "done";
		
	}
	
	public enum OwnerPlaceholder {
		consumer,
		request,
		header
	}
	
	interface Defaults {
		String 	Uri 		= "/storage";
		String 	DataFolder 	= "tus-files";
		String 	LocksFolder = "tus-locks";
		Long 	MaxSize 	= (long)(1024 * 1024 * 1024); // 1G
		Long 	ExpiresAfter= (long)(5 * 60); // 5 minutes
		boolean MultiTenant	= false;
		
		UploadStorageService.MetaSerializer 	
				Serializer	= UploadStorageService.MetaSerializer.JSON;
		
		String 	DataFile	= "data";
		String 	MetaFile	= "meta";
	}
	
	interface Registered {
		String TusSpi 		= "TusSpi";
	}
	
	private JsonArray dependencies;

	@Override
	public void init (ApiServer server) throws Exception {
		PackageClassLoader pcl = (PackageClassLoader)TusProtocolPlugin.class.getClassLoader ();
		
		if (!Json.isNullOrEmpty (dependencies)) {
			for (Object dependency : dependencies) {
				pcl.addDependency (server.getPluginsRegistry ().lockup (String.valueOf (dependency)).getClass ().getClassLoader ());
			}
		}
		
		pcl.registerObject (Registered.TusSpi, new ProcessTusSpi (this));
	}

	public String createKey (String name) {
		return Spec.Tus + Lang.DOT + getNamespace () + Lang.DOT + name;
	}
	
	@Override
	public void onEvent (Event event, Manageable target, Object... args) throws PluginRegistryException {
		if (!ApiSpace.class.isAssignableFrom (target.getClass ())) {
			return;
		}
		
		ApiSpace space = (ApiSpace)target;
		
		switch (event) {
			case Ready:
				createClients (space);
				break;
			case Update:
				if (args != null && args.length > 0 && EventSubject.Runtime.equals (args [0])) {
					updateClients (space);
				}
				break;
			default:
				break;
		}
	}
	
	private void createClients (ApiSpace space) throws PluginRegistryException {
		JsonObject allTus = (JsonObject)space.getRuntime (Spec.Tus);
		tracer.log (Level.Info, "All Tus -> " + allTus);
		if (Json.isNullOrEmpty (allTus)) {
			return;
		}
		
		Iterator<String> keys = allTus.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			createClient (space, allTus, key);
		}
	}
	
	private void createClient (ApiSpace space, JsonObject allTus, String name) throws PluginRegistryException {
		JsonObject oTus = Json.getObject (allTus, name);
		
		String storageFeature = Json.getString (oTus, Spec.Storage);
		if (Lang.isNullOrEmpty (storageFeature)) {
			return;
		}
		String dataFolder = (String)Json.find (oTus, Spec.Folders, Spec.Data);
		if (Lang.isNullOrEmpty (dataFolder)) {
			dataFolder = Defaults.DataFolder;
		}
		String locksFolder = (String)Json.find (oTus, Spec.Folders, Spec.Locks);
		if (Lang.isNullOrEmpty (locksFolder)) {
			locksFolder = Defaults.LocksFolder;
		}
		
		Storage storage = null;
		try {
			storage = space.feature (Storage.class, storageFeature, ApiContext.Instance);
		} catch (Exception ex) {
			throw new PluginRegistryException (ex.getMessage (), ex);
		}
		
		if (storage == null) {
			throw new PluginRegistryException ("storage feature " + storageFeature + " not found");
		}
		
		try {
			Folder root = storage.root ();
			root.add (dataFolder, true);
			root.add (locksFolder, true);
		} catch (Exception ex) {
			throw new PluginRegistryException (ex.getMessage (), ex);
		}
		
		UploadIdFactory idFactory = new UploadIdFactory ();
		idFactory.setUploadURI (Json.getString (oTus, Spec.Uri, Defaults.Uri));

		StorageFeatureService storageService = new StorageFeatureService (idFactory, storage, dataFolder);
		storageService.setMultiTenant (Json.getBoolean (oTus, Spec.MultiTenant, Defaults.MultiTenant));
		storageService.setDataFile (Json.getString (oTus, Spec.DataFile, Defaults.DataFile));
		storageService.setMetaFile (Json.getString (oTus, Spec.MetaFile, Defaults.MetaFile));
		
		UploadStorageService.MetaSerializer serializer = Defaults.Serializer;
		
		String sSerializer = Json.getString (oTus, Spec.Serializer);
		if (!Lang.isNullOrEmpty (sSerializer)) {
			try {
				serializer = UploadStorageService.MetaSerializer.valueOf (sSerializer.toUpperCase ());
			} catch (Exception ex) {
				// ignore
			}
		}
		storageService.setMetaSerializer (serializer);
		
		TusFileUploadService service = 
			new TusFileUploadService ()
				.withUploadStorageService (storageService)
				.withUploadLockingService (new StorageFeatureLockingService (idFactory, storage, locksFolder))
				.withMaxUploadSize (Json.getLong (oTus, Spec.MaxSize, Defaults.MaxSize))
				.withUploadExpirationPeriod (Json.getLong (oTus, Spec.ExpiresAfter, Defaults.ExpiresAfter) * 1000);
				
		// set multi-tenancy params
		
		String tenantKey = Json.getString (oTus, Spec.TenantKey, ApiConsumer.Fields.Id);
		
		OwnerPlaceholder tenantPlaceholder = 
			OwnerPlaceholder.valueOf (
				Json.getString (oTus, Spec.TenantPlaceholder, OwnerPlaceholder.consumer.name ()).toLowerCase ()
			);
		
		space.addRecyclable (createKey (name), new RecyclableTusService (service, tenantKey, tenantPlaceholder, tracer));
		
		oTus.set (ApiSpace.Spec.Installed, true);
	}
	
	private void updateClients (ApiSpace space) throws PluginRegistryException {
		// remove clients
		Set<String> keys = space.getRecyclables ();
		if (keys != null && !keys.isEmpty ()) {
			for (String key : keys) {
				removeClient (space, key);
			}
		}
		createClients (space);
	}
	
	private void removeClient (ApiSpace space, String key) {
		Recyclable recyclable = space.getRecyclable (key);
		if (!(recyclable instanceof RecyclableTusService)) {
			return;
		}
		// remove from recyclables
		space.removeRecyclable (key);
		// recycle
		recyclable.recycle ();
	}
	
	public JsonArray getDependencies () {
		return dependencies;
	}

	public void setDependencies (JsonArray dependencies) {
		this.dependencies = dependencies;
	}

}
