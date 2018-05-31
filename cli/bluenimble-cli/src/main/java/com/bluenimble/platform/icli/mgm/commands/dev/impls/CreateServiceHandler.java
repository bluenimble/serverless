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
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.cli.impls.AbstractTool;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.utils.CodeGenUtils;
import com.bluenimble.platform.icli.mgm.utils.SpecUtils;
import com.bluenimble.platform.json.JsonObject;

public class CreateServiceHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 2) {
			throw new CommandExecutionException ("service verb and model required. Ex. 'create service get user' or 'create service * offer'");
		}
		
		String currentApi = Json.getString (BlueNimble.Config, CliSpec.Config.CurrentApi);
		if (Lang.isNullOrEmpty (currentApi)) {
			throw new CommandExecutionException ("target api not set. Set target using the 'api' command. Ex. api myapi");
		}

		String apiPath = Json.getString (Json.getObject (BlueNimble.Config, CliSpec.Config.Apis), currentApi);
		if (Lang.isNullOrEmpty (apiPath)) {
			throw new CommandExecutionException ("api path not found for '" + currentApi + "'");
		}
		File apiFolder 			= new File (BlueNimble.Workspace, apiPath);
		if (!apiFolder.exists () || !apiFolder.isDirectory ()) {
			throw new CommandExecutionException ("invalid api folder '" + apiPath + "'");
		}
		
		String verb 	= args [0];
		String model 	= args [1];
		
		File resourcesFolder 	= new File (SpecUtils.specFolder (apiFolder), "resources");
		File servicesFolder 	= new File (resourcesFolder, "services");
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		JsonObject meta = (JsonObject)vars.get (BlueNimble.DefaultVars.UserMeta);
		
		String userName 	= Json.getString (meta, BlueNimble.DefaultVars.UserName);
		if (Lang.isNullOrEmpty (userName)) {
			userName = System.getProperty ("user.name");
		}
		
		String userPackage 	= Json.getString (meta, BlueNimble.DefaultVars.UserPackage);
		if (Lang.isNullOrEmpty (userPackage)) {
			userPackage = "com." + userName.toLowerCase ();
		}
		
		String functionsPackage = userPackage + Lang.DOT + currentApi + Lang.DOT + "functions";
		
		CodeGenUtils.writeService ((AbstractTool)tool, verb, model, functionsPackage, servicesFolder, CodeGenUtils.functionsFolder (tool, apiFolder, functionsPackage));
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}
	
	@Override
	public String getName () {
		return "service";
	}

	@Override
	public String getDescription () {
		return "create a service (spec and script) in the current api.\n\t'create service [*|get|post|put|delete|find]'\n\tExample: 'create service get user' or create service * offer." + 
				"\n\tYou can also write semicolon-separated commands like 'create service * user; post contact; * travel'";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "verb";
					}
					@Override
					public String desc () {
						return "a valid service verb. get, find, post, put or delete. You can use (*) which is equivalent to all verbs";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "model";
					}
					@Override
					public String desc () {
						return "model name. The model is the name of an entity such as user, contact, airline, ... To create a root service,  you can use / as the name of your model. create service get /";
					}
				}
		};
	}

}
