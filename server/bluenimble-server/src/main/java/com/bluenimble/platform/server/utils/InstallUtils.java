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
package com.bluenimble.platform.server.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.bluenimble.platform.json.JsonArray;

public class InstallUtils {
	
	public static URL [] toUrls (File home, JsonArray classpath) throws MalformedURLException {
		
		List<File> allFiles = new ArrayList<File> ();
		
		if (classpath != null && !classpath.isEmpty ()) {
			for (int i = 0; i < classpath.count (); i++) {
				File fileOrfolder = new File (home, (String)classpath.get (i));
				
				if (!fileOrfolder.exists ()) {
					continue;
				}
				if (fileOrfolder.isFile ()) {
					allFiles.add (fileOrfolder);
				} else if (fileOrfolder.isDirectory ()) {
					File [] files = fileOrfolder.listFiles ();
					if (files != null) {
						for (File f : files) {
							allFiles.add (f);
						}
					}
				}
				allFiles.add (home);
			}
		} else {
			
			allFiles.add (home);
			
			File libFolder = new File (home, ConfigKeys.Folders.Lib);
			if (libFolder.exists ()) {
				allFiles.add (libFolder);
				File [] files = libFolder.listFiles ();
				if (files != null) {
					for (File f : files) {
						allFiles.add (f);
					}
				}
			}
			
		}
		
		URL [] urls = new URL [allFiles.size ()];
		
		for (int i = 0; i < allFiles.size (); i++) {
			urls [i] = allFiles.get (i).toURI ().toURL ();
		}
		
		return urls;
	}
	
	public static boolean isValidApiNs (String name) {
		return Pattern.matches ("^[a-zA-Z0-9_-]*$", name);
	}
	
	public static boolean isValidSpaceNs (String name) {
		return Pattern.matches ("^[a-zA-Z0-9_-]*$", name);
	}
	
	public static boolean isValidPluginNs (String name) {
		return Pattern.matches ("^[a-zA-Z0-9_-[.]]*$", name);
	}

}
