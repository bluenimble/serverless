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

import java.io.File;
import java.io.FileFilter;

import com.bluenimble.platform.Lang;

public class CliUtils {
	
	public static final String VersionPrefix = "bnb.cli.version-";
	
	public static File fVersion (File home) {
		File [] files = home.listFiles (new FileFilter () {
			@Override
			public boolean accept (File file) {
				return file.isFile () && file.getName ().startsWith (VersionPrefix);
			}
		});
		if (files == null || files.length == 0) {
			return null;
		}
		return files [0];
	}
	
	public static String sVersion (File home) {
		File versionFile = fVersion (home);
		if (versionFile == null) {
			return null;
		}
		return versionFile.getName ().substring (versionFile.getName ().indexOf (VersionPrefix) + VersionPrefix.length ());
	}
	
	public static int iVersion (File home) {
		String sVersion = sVersion (home);
		if (Lang.isNullOrEmpty (sVersion)) {
			return 0;
		}
		String [] aVersion = Lang.split (sVersion, Lang.DOT);
		int patch = 0;
		if (aVersion.length >= 3) {
			String sPatch = aVersion [2];
			int indexOfDash = sPatch.indexOf (Lang.DASH);
			if (indexOfDash >= 0) {
				sPatch = sPatch.substring (0, indexOfDash);
			}
			patch = Integer.valueOf (sPatch);
		}
		try {
			return (Integer.valueOf (aVersion [0]) * 100) + (Integer.valueOf (aVersion [1]) * 10) + patch;
		} catch (Exception ex) {
			return 0;
		}
	}
	
}
