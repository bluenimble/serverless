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
package com.bluenimble.platform.plugins.dev.playground;

import java.io.File;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

public class PlaygroundPlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	private OServer oServer;

	@Override
	public void init (ApiServer server) throws Exception {
		// start embedded database
		database ();
	}

	@Override
	public void kill () {
		try {
			if (oServer != null) {
				oServer.shutdown ();
			}
		} catch (Throwable th) {
			// IGNORE
		}
	}

	private void database () throws Exception {
		
		System.setProperty ("ORIENTDB_HOME", home.getAbsolutePath ());
		
		File dbFile = new File (home, "playground.db");
		if (dbFile.exists ()) {
			File databases = new File (home, "databases");
			ArchiveUtils.decompress (dbFile, databases, true);
		}
		
		oServer = OServerMain.create ();
		oServer.startup (new File (home, "orientdb-server-config.xml"));
		oServer.activate ();
		oServer.removeShutdownHook ();
	}	

}
