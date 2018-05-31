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
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.utils.SpecUtils;
import com.bluenimble.platform.json.JsonObject;

public class CreateApiFromModelHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private static final String Model 		= "model";
	
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
		
		return new DefaultCommandResult (CommandResult.OK, null);
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
