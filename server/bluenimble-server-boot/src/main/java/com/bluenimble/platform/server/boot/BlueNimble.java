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
package com.bluenimble.platform.server.boot;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.PackageClassLoader;

public class BlueNimble {
	
	private static final String BlueNimbleHomeKey 		= "bluenimble.home";
	private static final String BlueNimbleRuntimeKey 	= "bluenimble.runtime";
	
	private static final String InstallHome		= "BN_HOME";
	
	public static void main (String [] args) throws Exception {
		
		String installHomePath = System.getProperty (InstallHome);
		if (Lang.isNullOrEmpty (installHomePath)) {
			installHomePath = System.getProperty ("user.dir");
		}
		
		File installHome = new File (installHomePath);
		
		File runtimeHome = null;
		if (args != null && args.length > 0 && args [0] != null) {
			File customHome = new File (args [0]);
			if (!customHome.exists ()) {
				System.out.println ("BlueNimble Home " + customHome.getAbsolutePath () + " doesn't exist, It will be created ...");
				customHome.mkdirs ();
			}
			runtimeHome = customHome;
		} 
		
		if (runtimeHome == null) {
			runtimeHome = installHome;
		}
		
		File tenantHome = null;
		
		if (args != null && args.length > 1 && args [1] != null) {
			tenantHome = new File (args [1]);
			if (!tenantHome.isDirectory () || !tenantHome.exists ()) {
				tenantHome = null;
			}
		}
		
		@SuppressWarnings("resource")
		PackageClassLoader scl = 
			new PackageClassLoader (
				"BlueNimblePlatform", 
				BlueNimble.class.getClassLoader (), 
				toUrls (new File (installHome, "lib"))
			);
		
		System.setProperty (BlueNimbleHomeKey, 		installHome.getAbsolutePath ());
		System.setProperty (BlueNimbleRuntimeKey, 	runtimeHome.getAbsolutePath ());

		Class<?> serverClass = scl.loadClass ("com.bluenimble.platform.server.impls.fs.FileSystemApiServer");
		Constructor<?> constructor = serverClass.getConstructor (File.class, File.class, File.class);
		Object server = constructor.newInstance (installHome, runtimeHome, tenantHome);
		
		serverClass.getMethod ("start", new Class [] {}).invoke (server, new Object [] {});
		
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
