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
package com.bluenimble.platform.api.impls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiResourcesManager;
import com.bluenimble.platform.api.ApiResourcesManagerException;
import com.bluenimble.platform.server.impls.fs.FileApiResource;
import com.bluenimble.platform.server.utils.ConfigKeys;

public class DefaultApiResourcesManager implements ApiResourcesManager {
	
	private static final long serialVersionUID = -1278408115246900633L;

	private static final Set<String> Reserved = new HashSet<String> (); 
	static {
		Reserved.add (ConfigKeys.Folders.Services);
		Reserved.add (ConfigKeys.Folders.Messages);
		Reserved.add (ConfigKeys.Folders.Keys);
		Reserved.add (ConfigKeys.Folders.Logs);
	}
	
	private static final Set<Character> Allowed = new HashSet<Character> (); 
	static {
		Allowed.add ('_');
		Allowed.add ('-');
	}
	
	private File 		resources;
	
	private String 		owner;
	
	public DefaultApiResourcesManager () {
	}
	
	@Override
	public void load (Api api) throws ApiResourcesManagerException {
		
		owner = api.space ().getNamespace () + Lang.COLON + api.getNamespace ();
		
		resources = new File (((ApiImpl)api).getHome (), ConfigKeys.Folders.Resources);
		if (!resources.exists ()) {
			resources.mkdir ();
		}
		
		File services = new File (resources, ConfigKeys.Folders.Services);
		if (!services.exists ()) {
			services.mkdir ();
		}
		
		File logs = new File (resources, ConfigKeys.Folders.Logs);
		if (!logs.exists ()) {
			logs.mkdir ();
		}
	}

	@Override
	public void onStart () throws ApiResourcesManagerException {
		
	}

	@Override
	public void onStop () throws ApiResourcesManagerException {
		
	}

	@Override
	public void delete (String [] path) throws ApiResourcesManagerException {
		if (path == null || path.length == 0) {
			throw new ApiResourcesManagerException ("invalid or null resource path");
		}
		
		if (path.length == 1 && Reserved.contains (path [0])) {
			throw new ApiResourcesManagerException ("can't delete protected resource " + path [0]);
		}
		
		String sPath = checkAndGetPath (path);
		
		if (Lang.isNullOrEmpty (sPath)) {
			throw new ApiResourcesManagerException ("invalid resource path " + Lang.join (path, Lang.SLASH));
		}
		
		File r = new File (resources, sPath);
		if (!r.exists ()) {
			throw new ApiResourcesManagerException ("resource " + sPath + " not found");
		}
		try {
			FileUtils.delete (r);
		} catch (IOException e) {
			throw new ApiResourcesManagerException (e.getMessage (), e);
		}
	}

	@Override
	public ApiResource get (String [] path)
			throws ApiResourcesManagerException {
		
		FileApiResource resource = null;
		
		if (path == null || path.length == 0) {
			resource = new FileApiResource (owner, resources, resources);
		} else {
			resource = new FileApiResource (owner, resources, new File (resources, Lang.join (path, Lang.SLASH)));
		}
		
		if (!resource.exists ()) {
			return null;
		}
		
		return resource;
	}

	@Override
	public ApiResource put (String [] path, InputStream payload, boolean overwrite)
			throws ApiResourcesManagerException {
		if (path == null || path.length == 0) {
			throw new ApiResourcesManagerException ("invalid or null resource path");
		}
		
		if (path.length == 1 && Reserved.contains (path [0])) {
			throw new ApiResourcesManagerException ("a protected resource called " + path [0] + " already exists");
		}
		
		String sPath = checkAndGetPath (path);
		
		if (Lang.isNullOrEmpty (sPath)) {
			throw new ApiResourcesManagerException ("invalid resource path " + Lang.join (path, Lang.SLASH));
		}
		
		File parent = null;
		
		String [] aParent = Lang.moveRight (path, 1);
		if (aParent == null) {
			parent = resources;
		} else {
			parent = new File (resources, Lang.join (aParent));
		}
		
		File file = new File (parent, path [path.length - 1]);
		if (file.exists ()) {
			if (file.isFile ()) {
				if (!overwrite) {
					throw new ApiResourcesManagerException ("resource " + sPath + " already exists");
				}
			} else {
				throw new ApiResourcesManagerException ("resource " + sPath + " already exists");
			}
		}
		
		if (parent.exists ()) {
			if (parent.isFile ()) {
				throw new ApiResourcesManagerException ("parent resource " + Lang.join (aParent, Lang.SLASH) + " is a file. It should be a valid folder");
			}
		} else {
			parent.mkdirs ();
		}
		
		FileApiResource resource = new FileApiResource (owner, resources, file);
		
		if (payload == null) {
			file.mkdir ();
		} else {
			try {
				resource.pipe (payload, 0, -1);
			} catch (IOException e) {
				throw new ApiResourcesManagerException (e.getMessage (), e);
			} 
		}
		
		return new FileApiResource (owner, resources, file);

	}
	
	private String checkAndGetPath (String [] path) throws ApiResourcesManagerException {
		if (path == null || path.length == 0) {
			return null;
		}
		
		for (String p : path) {
			if (!isValid (p)) {
				throw new ApiResourcesManagerException ("invalid path element " + p);
			}
		}
		
		return Lang.join (path, Lang.SLASH);
		
	}
	
	private boolean isValid (String p) {
		if (Lang.isNullOrEmpty (p)) {
			return false;
		}
		for (int i = 0; i < p.length (); i++) {
			Character c = p.charAt (i);
			if (!Character.isLetterOrDigit (c) && !Allowed.contains (c)) {
				return false;
			}
		}
		return true;
	}
	
}
