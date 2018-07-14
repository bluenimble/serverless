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

public interface ConfigKeys {

	interface Folders {
		String Plugins 		= "plugins";
		String Spaces 		= "spaces";
		
		String Resources	= "resources";
			String Services		= "services";
			String Messages		= "messages";
			String Keys			= "keys";
			
		String Logs			= "logs";
		String Backup 		= "backup";
		
		String Lib			= "lib";
	}
	
	String Id				= "id";
	String Type				= "type";

	String Debug			= "debug";
	String DeleteInstalledSpaces
							= "deleteInstalledSpaces";

	String Enabled 			= "enabled";
	
	String Spaces 			= "spaces";	
	
	String Classpath		= "classpath";
	String Dependencies		= "dependencies";
	String Native			= "native";
	String Spi				= "spi";
	String Namespace		= "namespace";
	String Name				= "name";
	String Title			= "title";
	String Description		= "description";
	String Version			= "version";
	String Vendor			= "vendor";
	String SystemProperties	= "systemProperties";
	
	String RootKeysFile		= "root.keys";
	String RootKeysEncrypted= "rootKeysEncrypted";
	String VariablesFile	= "variables.json";

	String Executor			= "executor";
	
	String Tracer			= "tracer";
	String StatusManager	= "statusManager";
	
	String ApiExt 			= ".api";
	String PluginExt 		= ".xpl";

	String LogExt			= ".log";
	String JsonExt			= ".json";
	String KeyStoreExt		= ".keystore";
	
	interface Descriptor {
		String Space 	= "space" + JsonExt;
		String Api 		= "api" + JsonExt;
		String Plugin 	= "plugin" + JsonExt;
	}
	
	String StatusFile 		= "status" + JsonExt;
	String KeyStoreFile 	= "space" + KeyStoreExt;

	String InstanceConfig	= "config/instance" + JsonExt;
	String DefaultMessages	= "config/messages" + JsonExt;
	String NewSpaceModel	= "config/space.new" + JsonExt;
	String SpaceModel		= "config/space" + JsonExt;
	
	String ClusterPeerFactory
							= "clusterPeerFactory";
	String PluginsRegistry	= "pluginsRegistry";
	String MediaProcessorRegistry	
							= "mediaProcessorRegistry";
	String MapProvider		= "mapProvider";
	String Interceptor 		= "interceptor";
	String Authenticator	= "authenticator";
	String ServiceValidator = "serviceValidator";
	String RequestSigner 	= "requestSigner";
	String RequestTracker	= "requestTracker";
	String RequestVisitor	= "requestVisitor";
	
	String DefaultLang 		= "en";

}
