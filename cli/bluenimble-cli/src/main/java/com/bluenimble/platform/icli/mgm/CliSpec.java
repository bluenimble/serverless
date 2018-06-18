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
package com.bluenimble.platform.icli.mgm;

public interface CliSpec {

	String Endpoint 	= "endpoint";
	String AccessKey 	= "accessKey";
	String SecretKey 	= "secretKey";
	
	String HomeVar		= "BlueNimble.Home";
	
	String KeysVar		= "BlueNimble.Session.Keys";
	String KeysExt		= ".keys";
	
	String ConfigVar	= "BlueNimble.Session.Config";
	String CliConfig	= "config.json";
	
	String Processing	= "__currently.processing.command.__";
	String ModelSpec	= "ModelSpec";
	
	interface Templates	{
		String Apis 		= "apis";
		String Services 	= "services";
		String Functions 	= "functions";
		String Models 		= "models";
		String Helpers 		= "helpers";
		String Comments		= "comments";	
	}

	interface Config {
		String Variables		= "variables";
		String Endpoint			= "endpoint";
		String Keys				= "keys";
		String Workspace		= "workspace";
		String Apis				= "apis";
		String CurrentApi		= "current";
		String CurrentKeys		= "currentKeys";
		String ResponseReaders	= "responseReaders";
	}

}
