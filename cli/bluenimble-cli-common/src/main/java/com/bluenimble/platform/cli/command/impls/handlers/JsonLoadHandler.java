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
package com.bluenimble.platform.cli.command.impls.handlers;

import java.io.File;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonObject;

public class JsonLoadHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("json variable name required");
		}
		
		String var = args [0];
		String sFile = null;
		
		if (args.length > 1) {
			sFile = args [1];
		}
		
		JsonObject json = null;

		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		if (sFile == null) {
			Object vValue = vars.get (var);
			if (vValue == null) {
				throw new CommandExecutionException ("variable '" + var + "' not found!");
			}
			try {
				json = new JsonObject (String.valueOf (vValue));
				vars.put (var, json);
			} catch (Exception e) {
				throw new CommandExecutionException (e.getMessage (), e);
			}
			return new DefaultCommandResult (CommandResult.OK, json);
		}
		
		File file = new File (sFile);
		
		if (!file.exists ()) {
			throw new CommandExecutionException ("file " + sFile + " not found!");
		}
		
		if (!file.isFile ()) {
			throw new CommandExecutionException ("invalid file " + sFile + ". Are you sure is a file not a folder?");
		}
		
		try {
			json = Json.load (file);
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		vars.put (var, json);
		
		return new DefaultCommandResult (CommandResult.OK, "Json load " + var + " <- " + file.getAbsolutePath ());
	}


	@Override
	public String getName () {
		return "load";
	}

	@Override
	public String getDescription () {
		return "load a json file into a variable or parse a variable content to a json object";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "name";
					}
					@Override
					public String desc () {
						return "variable name";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "var name of file path";
					}
					@Override
					public String desc () {
						return "an existing string typed variable name of a valid path to a json formatted file";
					}
				}
		};
	}

}
