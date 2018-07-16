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
package com.bluenimble.platform.api.impls.spis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiRequest.Scope;
import com.bluenimble.platform.api.ApiResponse;
import com.bluenimble.platform.api.ApiServiceExecutionException;
import com.bluenimble.platform.api.ApiSpace.Endpoint;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.api.security.ApiConsumer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

/**
 * 
{

    "verb": "put",
    "endpoint": "/cars/:car/:driver/status/:status",
    
    "custom": {
        "flow": [{
        	"id": "alpha",
            "verb": "put", "endpoint": "/cars/[car]",
            "parameters": {
                "payload": {
                    "status": "[status]"
                }
            }
        }, {
            "verb": "post", "endpoint": "/drivers/[driver]",
            "parameters": {
                "payload": {
                    "lastStop": "[output.alpha.stopDate]"
                }
            }
        }]
    },
    
    "spi": {
        "class": "core:ComposerSpi"
    }

} 
 *
 */

public class ComposerApiServiceSpi extends AbstractApiServiceSpi {

	private static final long serialVersionUID = 4666775202589517190L;
	
	private static final DefaultExpressionCompiler ExpressionCompiler = new DefaultExpressionCompiler (true).cacheSize (100);
	
	interface ResolverPrefix {
		String Request 	= "request";
		String Consumer = "consumer";
		String Output 	= "output";
	}
	
	interface Custom {
		String Flow 	= "flow";
		String Space 	= "space";
		String Api 		= "api";
		String Id 		= "id";
		String Endpoint = "endpoint";
		String Verb 	= "verb";
	}
	
	private static final String FinalOutput = "final";
	
	@Override
	public ApiOutput execute (Api api, ApiConsumer consumer, ApiRequest request, ApiResponse response) throws ApiServiceExecutionException {
		
		JsonArray flow = Json.getArray (request.getService ().getCustom (), Custom.Flow);
		if (Json.isNullOrEmpty (flow)) {
			return null;
		}
		
		final JsonObject oOutput = new JsonObject ();
		
		final Map<String, Object> bindings = new HashMap<String, Object> ();
		bindings.put (ResolverPrefix.Request, request);
		bindings.put (ResolverPrefix.Consumer, consumer);
		bindings.put (ResolverPrefix.Output, oOutput);
		
		VariableResolver variableResolver = new VariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;
			@Override
			public Object resolve (String namespace, String... property) {
				if (Lang.isNullOrEmpty (namespace)) {
					return null;
				}
				if (namespace.equals (ResolverPrefix.Request)) {
					return request.get (Lang.join (property, Lang.DOT), Scope.Parameter, Scope.Header);
				} else if (namespace.equals (ResolverPrefix.Consumer)) {
					return consumer.get (Lang.join (property, Lang.DOT));
				} else if (namespace.equals (ResolverPrefix.Output)) {
					return Json.find (oOutput, property);
				} 
				return null;
			}
			
			@Override
			public Map<String, Object> bindings () {
				return bindings;
			}
		};
		
		ApiOutput finalOutput = null;
	
		ApiOutput output = null;
		
		for (int i = 0; i < flow.count (); i++) {
			output = processStep (api, consumer, request, flow.get (i), variableResolver, oOutput);
			if (oOutput.containsKey (FinalOutput)) {
				finalOutput = output;
			}
		}
		
		return finalOutput != null ? finalOutput : output;
	}

	private ApiOutput processStep (Api api, final ApiConsumer consumer, final ApiRequest request, Object step, 
			VariableResolver variableResolver, JsonObject oOutput) 
			throws ApiServiceExecutionException {
		
		if (!(step instanceof JsonObject)) {
			return null;
		}	
		
		final JsonObject oStep = (JsonObject)Json.resolve ((JsonObject)step, ExpressionCompiler, variableResolver);
		
		ApiRequest stepRequest = api.space ().request (request, consumer, new Endpoint () {
			@Override
			public String space () {
				return Json.getString (oStep, Custom.Space, api.space ().getNamespace ());
			}
			@Override
			public String api () {
				return Json.getString (oStep, Custom.Api, api.getNamespace ());
			}
			@Override
			public String [] resource () {
				String resource = Json.getString (oStep, Custom.Endpoint);
				if (resource.startsWith (Lang.SLASH)) {
					resource = resource.substring (1);
				}
				if (resource.endsWith (Lang.SLASH)) {
					resource = resource.substring (0, resource.length () - 1);
				}
				if (Lang.isNullOrEmpty (resource)) {
					return null;
				}
				return Lang.split (resource, Lang.SLASH);
			}
			@Override
			public ApiVerb verb () {
				try {
					return ApiVerb.valueOf (
						Json.getString (oStep, Custom.Verb, ApiVerb.POST.name ()).toUpperCase ()
					);
				} catch (Exception ex) {
					return ApiVerb.POST;
				}
			}
		});
		
		// copy parameters
		JsonObject parameters = Json.getObject (oStep, ApiRequest.Fields.Data.Parameters);
		if (!Json.isNullOrEmpty (parameters)) {
			Iterator<String> keys = parameters.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				stepRequest.set (key, parameters.get (key), Scope.Parameter);
			}
		}
		
		// copy headers
		JsonObject headers = Json.getObject (oStep, ApiRequest.Fields.Data.Headers);
		if (!Json.isNullOrEmpty (headers)) {
			Iterator<String> keys = headers.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				stepRequest.set (key, headers.get (key), Scope.Header);
			}
		}
		
		ApiOutput output = api.call (stepRequest);
		
		String id = Json.getString (oStep, Custom.Id);
		if (!Lang.isNullOrEmpty (id) && output != null) {
			oOutput.set (id, output.data ());
		}
		
		return output;
		
	}
	
}