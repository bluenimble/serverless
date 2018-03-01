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

import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class JsonDeleteHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("json variable name required");
		}
		
		if (args.length < 2) {
			throw new CommandExecutionException ("json property required");
		}
		
		String var = args [0];
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		String prop = args [1];
		
		int indexOfDot = prop.indexOf (Lang.DOT);

		Object o = vars.get (var);
		if (o == null) {
			throw new CommandExecutionException ("variable '" + var + "' not found");
		}
		if (!(o instanceof JsonObject) && !(o instanceof JsonArray)) {
			throw new CommandExecutionException ("variable '" + var + "' isn't a valid json object or array");
		}
		
		if ((o instanceof JsonArray) && indexOfDot > 0) {
			throw new CommandExecutionException ("property '" + prop + "' should be a valid integer since the json variable is an array");
		}

		if (indexOfDot <= 0) {
			if (o instanceof JsonObject) {
				((JsonObject)o).remove (prop);
			} else {
				((JsonArray)o).remove (Integer.parseInt (prop));
			}
			return new DefaultCommandResult (CommandResult.OK, o);
		}
		
		JsonObject json = (JsonObject)o;
		
		String [] path = Lang.split (prop, Lang.DOT);
		prop = path [path.length - 1];
		path = Lang.moveRight (path, 1);
		Object child = Json.find (json, path);
		if (child == null) {
			throw new CommandExecutionException (Lang.join (path, Lang.DOT) + " not found");
		}
		if (child instanceof JsonObject) {
			((JsonObject)child).remove (prop);
			return new DefaultCommandResult (CommandResult.OK, json);
		} else if (child instanceof JsonArray) {
			int iProp = -1;
			try {
				iProp = Integer.valueOf (prop);
			} catch (Exception ex) {
				// ignore
			}
			JsonArray array = (JsonArray)child;
			if (iProp > -1 && array.count () > iProp) {
				((JsonArray)child).remove (iProp);
				return new DefaultCommandResult (CommandResult.OK, json);
			}
			
		} 
		
		return new DefaultCommandResult (CommandResult.OK, json);
	}


	@Override
	public String getName () {
		return "delete";
	}

	@Override
	public String getDescription () {
		return "delete a property. json delete aJsonVariable address.city";
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
						return "property";
					}
					@Override
					public String desc () {
						return "property name";
					}
				}
		};
	}

}
