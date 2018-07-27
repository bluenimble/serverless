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
package com.bluenimble.platform.icli.mgm.boot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;
import com.bluenimble.platform.http.impls.DefaultHttpClient;
import com.bluenimble.platform.http.request.impls.GetRequest;
import com.bluenimble.platform.http.response.HttpResponse;
import com.bluenimble.platform.http.utils.HttpUtils;
import com.bluenimble.platform.json.JsonObject;

public class BnMgmICli {

	private static final String InstallHome		= "BN_HOME";

	private static 	File 				Home;
	private static 	PackageClassLoader 	Scl;
	public static 	JsonObject 			Software; 
	
	private static Object 				tool;
	private static String [] 			args;
	
	private static DefaultHttpClient Http; 
	static {
		try {
			Http = new DefaultHttpClient ();
			Http.setTrustAll (true);
		} catch (Exception e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
	public static void main (String [] _args) throws Exception {
		args = _args;
		restart ();
	}
	
	public static void restart () throws Exception {
		stop ();
		start ();
	}
	
	private static void stop () {
		if (tool != null) {
			try {
				tool.getClass ().getMethod ("shutdown", new Class [] { }).invoke (tool, new Object [] { });
			} catch (Exception ex) {
				// Ignore
			}
			tool = null;
		}
		
		if (Scl != null) {
			try {
				Scl.clear ();
			} catch (Exception ex) {
				// Ignore
			}
			Scl = null;
		}
	}

	private static void start () throws Exception {
		String homePath = System.getProperty (InstallHome);
		if (homePath == null || homePath.trim ().equals ("")) {
			homePath = System.getProperty ("user.dir");
		}
		
		Home = new File (homePath);
		
		Software = Json.load (new File (Home, "update.json"));
		
		if (!Json.isNullOrEmpty (Software)) {
			upgrade ();
		}
		
		Scl = 
			new PackageClassLoader (
				"BlueNimbleCLI",
				toUrls (new File (Home, "lib")),
				(ClassLoader [])null
			);
		
		Class<?> toolClass = Scl.loadClass ("com.bluenimble.platform.icli.mgm.BlueNimble");
		Constructor<?> constructor = toolClass.getConstructor (File.class);
		tool = constructor.newInstance (Home);
		
		toolClass.getMethod ("startup", new Class [] {String [].class}).invoke (tool, new Object [] { args });
	}

	private static void upgrade () throws Exception {
		// is package ready?
		File pkg = new File (Home, Json.getString (Software, Spec.Package));
		if (pkg.exists ()) {
			File tmp = new File (Home, "tmp/" + Lang.UUID (20));
			tmp.mkdirs ();
			ArchiveUtils.decompress (pkg, tmp, true);
			
			File [] files = Home.listFiles ();
			for (File f : files) {
				if (f.getName ().equals ("boot") || f.getName ().equals ("tmp") || f.getName ().equals ("bnb.bat") || f.getName ().equals ("bnb.sh")) {
					continue;
				}
				FileUtils.delete (f);
			}
			
			files = tmp.listFiles ();
			for (File f : files) {
				if (f.getName ().equals ("boot")) {
					continue;
				}
				FileUtils.copy (f, Home, true);
			}
			
			FileUtils.delete (tmp);
			
			return;
		}
		
		if (download ()) {
			upgrade ();
		}
		
	}
	
	private static boolean download () {
		
		JsonObject endpoints = Json.getObject (Software, Spec.endpoints.class.getSimpleName ());
		
		String uVersion = Json.getString (endpoints, Spec.endpoints.Version);
		if (Lang.isNullOrEmpty (uVersion)) {
			return false;
		}
		
		String uDownload = Json.getString (endpoints, Spec.endpoints.Download);
		if (Lang.isNullOrEmpty (uDownload)) {
			return false;
		}
		
		System.out.println ("\n    Checking for a newer version of the software ...");
		
		try {
			HttpResponse response = Http.send (new GetRequest (HttpUtils.createEndpoint (new URI (uVersion))));
			if (response.getStatus () != 200) {
				System.out.println ("\n    Warning: Unable to get software version!");
				return false;
			}
			
			OutputStream out = new ByteArrayOutputStream ();
			response.getBody ().dump (out, Encodings.UTF8, null);
			
			JsonObject oVersion = new JsonObject (new String (((ByteArrayOutputStream)out).toByteArray ()));
			
			if (version (oVersion) > CliUtils.iVersion (Home)) {
				
				String newVersion = Json.getString (oVersion, Spec.version.Major) + Lang.DOT + 
				Json.getString (oVersion, Spec.version.Minor) + Lang.DOT + 
				Json.getString (oVersion, Spec.version.Patch, "0");

				System.out.println ("\n    Newer version found " + newVersion);
				
				System.out.println ("\n    Download [" + newVersion + "]");
				
				GetRequest downloadRequest = new GetRequest (HttpUtils.createEndpoint (new URI ( uDownload )));
				
				response = Http.send (downloadRequest);
								
				if (response.getStatus () != 200) {
					return false;
				}
				OutputStream os = null;
				try {
					os = new FileOutputStream (new File (Home, Json.getString (Software, Spec.Package)));
					IOUtils.copy (response.getBody ().get (0).toInputStream (), os);
				} finally {
					IOUtils.closeQuietly (os);
				}
				System.out.println ("\n    Boot using the new version ...\n");
				return true;
			}		
			System.out.println ("\n    Software is up to date!\n");
		} catch (Exception ex) {
			ex.printStackTrace ();
			return false;
		}
		return false;
		
	} 
	
	private static int version (JsonObject oVersion) {
		return 	Json.getInteger (oVersion, Spec.version.Major, 1) * 100 + 
				Json.getInteger (oVersion, Spec.version.Minor, 0) * 10	+
				Json.getInteger (oVersion, Spec.version.Patch, 0);
	}
	
	private static URL [] toUrls (File lib) throws MalformedURLException {
		
		List<File> allFiles = new ArrayList<File> ();
		
		File [] files = lib.listFiles ();
		if (files != null) {
			for (File f : files) {
				allFiles.add (f);
			}
		}
		
		allFiles.add (lib);
		
		URL [] urls = new URL [allFiles.size ()];
		
		for (int i = 0; i < allFiles.size (); i++) {
			urls [i] = allFiles.get (i).toURI ().toURL ();
		}
		
		return urls;
	}
	
}
