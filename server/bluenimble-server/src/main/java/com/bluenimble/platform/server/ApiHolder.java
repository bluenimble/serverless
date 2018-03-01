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
package com.bluenimble.platform.server;

import java.io.File;
import java.io.Serializable;

import com.bluenimble.platform.api.Api;

public class ApiHolder implements Serializable {

	private static final long serialVersionUID = -160715240794516657L;
	
	private Api 	api;
	
	private File	file; 
	private File	home; 

	private long	installedOn; 
	private long	startedOn; 
	
	public ApiHolder (File file, File home, Api api, long installedOn, long startedOn) {
		this.file = file;
		this.home = home;
		this.api = api;
		this.installedOn = installedOn;
		this.startedOn = startedOn;
	}
	
	public Api getApi () {
		return api;
	}
	
	public long getInstalledOn () {
		return installedOn;
	}
	
	public long getStartedOn () {
		return startedOn;
	}
	
	public File getFile () {
		return file;
	}
	
	public File getHome () {
		return home;
	}
	
}
