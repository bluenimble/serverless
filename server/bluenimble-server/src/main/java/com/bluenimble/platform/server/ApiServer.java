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

import java.util.Collection;
import java.util.Map;

import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.ApiManagementException;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.CodeExecutor;
import com.bluenimble.platform.api.DescribeOption;
import com.bluenimble.platform.api.Manageable;
import com.bluenimble.platform.api.media.ApiMediaProcessorRegistry;
import com.bluenimble.platform.api.security.ApiConsumerResolver;
import com.bluenimble.platform.api.security.ApiRequestSigner;
import com.bluenimble.platform.api.validation.ApiServiceValidator;
import com.bluenimble.platform.cluster.ClusterPeer;
import com.bluenimble.platform.cluster.ClusterSerializer;
import com.bluenimble.platform.cluster.ClusterTask;
import com.bluenimble.platform.instance.InstanceDescriber;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.PluginsRegistry;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.server.interceptor.ApiInterceptor;
import com.bluenimble.platform.server.maps.MapProvider;
import com.bluenimble.platform.server.tracking.ServerRequestTracker;

public interface ApiServer extends Traceable, Manageable {
	
	enum InstallType {
		New,
		Upgrade
	}

	enum Event {
		// space events
		Create,
		Destroy,
		AddFeature,
		DeleteFeature,
		// api events
		Install,
		Uninstall,
		Stop,
		Start,
		Pause
	}
	
	interface Maps {
		String Spaces 		= "spaces";
		String Recyclables 	= "recyclables";
	}
	
	interface ResolverPrefix {
		String Sys 			= "sys";
		String Server 		= "server";
		String Vars 		= "vars";
		String This 		= "this";
	}
	
	interface EventTarget {
	}
	
	String 					id 						();
	String 					type 					();
	String 					version 				();
	
	InstanceDescriber		getInstanceDescriber 	();
	void 					setInstanceDescriber	(InstanceDescriber instanceDescriber);
	
	JsonObject				getDescriptor			();
	
	JsonObject				describe				(DescribeOption... opts);

	ClusterPeer				getPeer 				();
	
	PluginsRegistry 		getPluginsRegistry 		();
	
	KeyPair					getKeys 				();
	
	ApiInterceptor 			getInterceptor			();
	void 					setInterceptor 			(ApiInterceptor interceptor);
	
	MapProvider 			getMapProvider			();
	void 					setMapProvider 			(MapProvider mapProvider);
	
	ApiConsumerResolver 	getConsumerResolver 	(String name);
	void 					addConsumerResolver 	(ApiConsumerResolver consumerResolver);

	ApiServiceValidator 	getServiceValidator 	();
	void 					setServiceValidator 	(ApiServiceValidator serviceValidator);

	ApiRequestSigner 		getRequestSigner 		();
	void 					setRequestSigner 		(ApiRequestSigner requestSigner);
	
	ServerRequestTracker	getRequestTracker		(String id);
	void 					addRequestTracker 		(String id, ServerRequestTracker requestTracker);

	ApiRequestVisitor		getRequestVisitor		();
	void 					setRequestVisitor 		(ApiRequestVisitor requestVisitor);
	
	KeyStoreManager			getKeyStoreManager		();
	void					setKeyStoreManager		(KeyStoreManager keyStoreManager);

	void 					start 					() throws ServerStartupException;
	void 					stop 					();

	ApiSpace		  		create 					(JsonObject oSpace) throws ApiManagementException;
	void		  			drop 					(String namespace) throws ApiManagementException;

	Collection<ApiSpace>	spaces 					();
	ApiSpace		  		space 					(String space);

	void  					execute 				(ApiRequest request, ApiResponse response, CodeExecutor.Mode mode);
	
	void					addFeature				(ServerFeature feature);
	Object					getFeature				(ApiSpace space, Class<?> type, String provider);

	ApiMediaProcessorRegistry		
							getMediaProcessorRegistry	
													();

	void					registerSerializer		(ClusterSerializer serializer);
	ClusterSerializer 
							getSerializer			(String name);

	void					registerTask			(ClusterTask task);
	ClusterTask 			getTask					(String name);

	String 					message 				(String lang, String key, Object... args);
	
	JsonObject 				resolve 				(JsonObject descriptor, Map<String, String []> varsMapping);

}
