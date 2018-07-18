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
package com.bluenimble.platform.icli.mgm.commands.dev.impls;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiService;
import com.bluenimble.platform.api.ApiVerb;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.utils.SpecUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.BeanUtils;

public class CreateApiFromModelHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private static final String Model 		= "model";
	private static final String Operations 	= "operations";
	private static final String Chain 		= "chain";
	private static final String On 			= "on";
	
	private static final Map<String, ApiVerb> Verbs = new HashMap<String, ApiVerb>();
	static {
		Verbs.put ("create", ApiVerb.POST);
		Verbs.put ("update", ApiVerb.PUT);
		Verbs.put ("delete", ApiVerb.DELETE);
		Verbs.put ("get", ApiVerb.GET);
		Verbs.put ("find", ApiVerb.GET);
	}
	
	private static final CommandHandler CreateApiHandler 	= new CreateApiHandler ();
	private static final CommandHandler CreateServiceHandler 	= new CreateServiceHandler ();
	
	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("api model required. ex. create api modelVar [api folder - Optional]");
		}
		
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		String modelVar = args [0];
		
		JsonObject model = (JsonObject)vars.get (modelVar);
		
		String namespace = model.getString (Api.Spec.Namespace);
		if (Lang.isNullOrEmpty (namespace)) {
			namespace = modelVar;
		}
		
		String sApiFolder 	= namespace;
		if (args.length > 1 && args [1] != null) {
			sApiFolder = args [1];
		}
		
		// create api
		CreateApiHandler.execute (tool, new String [] { namespace, sApiFolder });
		
		File apiFolder = new File (BlueNimble.Workspace, sApiFolder);
		
		JsonObject apiSpec = SpecUtils.read (apiFolder);
		apiSpec.set (Api.Spec.Name, Json.getString (model, Api.Spec.Name));
		apiSpec.set (Api.Spec.Description, Json.getString (model, Api.Spec.Description));

		// save api spec changes
		SpecUtils.write (apiFolder, apiSpec);
		
		JsonObject entities = Json.getObject (model, Model);
		if (Json.isNullOrEmpty (entities)) {
			return new DefaultCommandResult (CommandResult.OK, null);
		}
		
		Iterator<String> names = entities.keys ();
		while (names.hasNext ()) {
			
			String entity = names.next ();
			
			Object entitySpec = entities.get (entity);
			if (entitySpec != null && entitySpec instanceof JsonObject) {
				vars.put (CliSpec.ModelSpec, entitySpec);
			}
			
			// TODO: in CreateService copy spec
			try {
				CreateServiceHandler.execute (tool, new String [] { Lang.STAR, entity });
			} finally {
				vars.remove (CliSpec.ModelSpec);
			}
			
		}
		
		String specLang 	= (String)vars.get (BlueNimble.DefaultVars.SpecLanguage);
		if (Lang.isNullOrEmpty (specLang)) {
			specLang = BlueNimble.SpecLangs.Json;
		}

		// create operations
		JsonObject operations = Json.getObject (model, Operations);
		if (Json.isNullOrEmpty (operations)) {
			return new DefaultCommandResult (CommandResult.OK, null);
		}
		Iterator<String> ops = operations.keys ();
		while (ops.hasNext ()) {
			String verbAndEndpoint = ops.next ();
			
			String verb = ApiVerb.GET.name ().toLowerCase ();
			String endpoint = verbAndEndpoint;
			
			int indexOfSpace = verbAndEndpoint.indexOf (Lang.SPACE);
			if (indexOfSpace > 0) {
				verb = verbAndEndpoint.substring (0, indexOfSpace).trim ();
				endpoint = verbAndEndpoint.substring (indexOfSpace + 1).trim ();
			}
			
			JsonObject operation = Json.getObject (operations, verbAndEndpoint);
			
			JsonObject spec = new JsonObject ();
			spec.set (ApiService.Spec.Verb, verb);
			spec.set (ApiService.Spec.Endpoint, endpoint);
			spec.set (ApiService.Spec.Spec, Json.getObject (operation, ApiService.Spec.Spec));
			spec.set (
				ApiService.Spec.Meta.class.getSimpleName ().toLowerCase (), 
				Json.getObject (operation, ApiService.Spec.Meta.class.getSimpleName ().toLowerCase ())
			);
			
			JsonObject spi = new JsonObject ();
			spec.set (ApiService.Spec.Spi.class.getSimpleName ().toLowerCase (), spi);
			
			spi.set (BeanUtils.Clazz, "core:ComposerSpi");
			
			Object chain = operation.get (Chain);
			
			if (chain instanceof JsonObject) {
				JsonObject oChain = (JsonObject)chain;
				setEndpoint (oChain);
			} else if (chain instanceof JsonArray) {
				JsonArray aChain = (JsonArray)chain;
				for (int i = 0; i < aChain.count (); i++) {
					JsonObject oChain = (JsonObject)aChain.get (i);
					setEndpoint (oChain);
				}
			}
			
			spi.set (Chain, chain);
			
			String id = Json.getString (operation, ApiService.Spec.Id);
			if (Lang.isNullOrEmpty (id)) {
				if (endpoint.startsWith (Lang.SLASH)) {
					endpoint = endpoint.substring (1);
				}
				if (endpoint.endsWith (Lang.SLASH)) {
					endpoint = endpoint.substring (0, endpoint.length () - 1);
				}
				String [] parts = Lang.split (endpoint, Lang.SLASH);
				id = Lang.BLANK;
				for (String p : parts) {
					if (p.startsWith (Lang.COLON)) {
						p = p.substring (1);
					}
					id += p.length () == 1 ? p.toUpperCase () : p.substring (0, 1).toUpperCase () + p.substring (1);
				}
			}
			
			File specFile = new File (SpecUtils.servicesFolder (apiFolder), id + ".json");
			try {
				Json.store (spec, specFile);
				if (specLang.equals (BlueNimble.SpecLangs.Yaml)) {
					SpecUtils.j2y (specFile, true);
				}
			} catch (Exception e) {
				throw new CommandExecutionException (e.getMessage (), e);
			}
			
			// TODO
			// STORE Json/Yaml
			
		}
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}
	
	private void setEndpoint (JsonObject chain) {
		
		String on = Json.getString (chain, On);
		
		String model = on.toLowerCase ();
		String action = null;
		
		int indexOfColon = on.indexOf (Lang.COLON);
		if (indexOfColon > 0) {
			action = model.substring (indexOfColon + 1);
			model = model.substring (0, indexOfColon);
		}
		
		if (action != null) {
			ApiVerb verb = Verbs.get (action);
			if (verb == null) {
				verb = ApiVerb.GET;
			}
			chain.set (ApiService.Spec.Verb, verb.name ().toLowerCase ());
			String endpoint = Lang.SLASH + (model.endsWith ("y") ? (model.substring (0, model.length () - 1) + "ies") : model + "s");
			if (action.equals ("update") || action.equals ("get")) {
				endpoint += Lang.SLASH + "<% request.data.params." + model + "%>";
			} else if (action.equals ("find")) {
				endpoint += Lang.SLASH + "find";
			}
			chain.set (ApiService.Spec.Endpoint, endpoint);
		} else {
			chain.set (ApiService.Spec.Endpoint, on);
		}
		
		chain.remove (On);
		
	}
	
	@Override
	public String getName () {
		return "api";
	}

	@Override
	public String getDescription () {
		return "create an api project from a model 'create api modelVar'";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
			new AbstractArg () {
				@Override
				public String name () {
					return "model";
				}
				@Override
				public String desc () {
					return "api model";
				}
			}
		};
	}

}
