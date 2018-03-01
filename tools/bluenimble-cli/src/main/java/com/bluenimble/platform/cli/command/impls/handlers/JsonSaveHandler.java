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
import java.io.IOException;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonObject;

public class JsonSaveHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("json variable name required");
		}
		
		if (args.length < 2) {
			throw new CommandExecutionException ("file path required");
		}
		
		String var = args [0];
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		Object o = vars.get (var);
		if (o == null) {
			throw new CommandExecutionException ("variable '" + var + "' not found");
		}
		if (!(o instanceof JsonObject)) {
			throw new CommandExecutionException ("variable '" + var + "' isn't a valid json object");
		}
		
		JsonObject json = (JsonObject)o;
		
		String sFile = args [1];
		File file = new File (sFile);
		
		boolean overwrite = true;
		
		if (args.length > 2) {
			overwrite = !("-check".equals (args [2]));
		}
		
		if (file.exists ()) {
			if (file.isDirectory ()) {
				throw new CommandExecutionException ("file '" + sFile + "' is a folder!");
			}
			if (!overwrite) {
				throw new CommandExecutionException ("file '" + sFile + "' already exist");
			}
		}
		
		try {
			Json.store (json, file);
		} catch (IOException e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		return new DefaultCommandResult (CommandResult.OK, "'" + var + "' saved to " + file.getAbsolutePath ());
	}


	@Override
	public String getName () {
		return "save";
	}

	@Override
	public String getDescription () {
		return "save a json variable to a file. save json_var /home/me/var.json -check";
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
						return "file path";
					}
					@Override
					public String desc () {
						return "a valid file path";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "-check";
					}
					@Override
					public String desc () {
						return "check is this file already exists. If yes, raise an error";
					}
					@Override
					public boolean required () {
						return false;
					}
				}
		};
	}

}
