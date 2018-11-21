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
package com.bluenimble.platform.plugins.keystores;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.AbstractPlugin;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.impls.FileSystemKeyStoreManager;

public class KeyStorePlugin extends AbstractPlugin {

	private static final long serialVersionUID = -7715328225346939289L;
	
	private static final String DefaultKeystoreFile = "space.keystore";
	
	interface Spec {
		String Delay 		= "delay";
		String Period 		= "period";

		String File 		= "file";
		String FlushRate 	= "flushRate";
		
		String ReadOnly		= "readOnly";
	}
	
	private String 	file = DefaultKeystoreFile;
	private int  	flushRate = 10;
	private boolean readOnly;
	
	private JsonObject listener;

	@Override
	public void init (ApiServer server) throws Exception {
		server.setKeyStoreManager (
			new FileSystemKeyStoreManager (
				Json.getLong 	(listener, Spec.Delay, 5),
				Json.getLong 	(listener, Spec.Period, 300),
				file,
				flushRate,
				readOnly
			)
		);
	}

	public JsonObject getListener () {
		return listener;
	}
	public void setListener (JsonObject listener) {
		this.listener = listener;
	}

	public String getFile () {
		return file;
	}
	public void setFile (String file) {
		this.file = file;
	}

	public int getFlushRate () {
		return flushRate;
	}
	public void setFlushRate (int flushRate) {
		this.flushRate = flushRate;
	}

	public boolean isReadOnly () {
		return readOnly;
	}
	public void setReadOnly (boolean readOnly) {
		this.readOnly = readOnly;
	}

}
