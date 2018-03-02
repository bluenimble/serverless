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
import com.bluenimble.platform.json.JsonParser;

public class JsonSetHandler implements CommandHandler {

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
		
		Object o = vars.get (var);
		if (o == null) {
			throw new CommandExecutionException ("variable '" + var + "' not found");
		}
		if (!(o instanceof JsonObject)) {
			throw new CommandExecutionException ("variable '" + var + "' isn't a valid json object");
		}
		
		JsonObject json = (JsonObject)o;
		
		String prop = args [1];
		
		// it's a delete
		int indexOfDot = prop.indexOf (Lang.DOT);
		String value = args [2];
		
		boolean isJson 	= false;
		boolean isVar 	= false;
		
		Object oValue = null;
		
		if (value.startsWith ("j\\")) {
			value = value.substring (3);
			isJson = true;
		}
		
		if (value.startsWith ("v\\")) {
			value = value.substring (3);
			isVar = true;
		}
		
		if (Lang.isNullOrEmpty (value)) {
			isJson = false;
		}
		
		oValue = value;
		
		if (isJson) {
			try {
				oValue = JsonParser.parse (value);
			} catch (Exception ex) {
				throw new CommandExecutionException (ex.getMessage (), ex);
			}
		}
		
		if (isVar) {
			oValue = vars.get (value);
		}
		
		if (indexOfDot <= 0) {
			json.set (prop, oValue);
		} else {
			String [] path = Lang.split (prop, Lang.DOT);
			prop = path [path.length - 1];
			path = Lang.moveRight (path, 1);
			Object child = Json.find (json, path);
			if (child == null) {
				throw new CommandExecutionException (Lang.join (path, Lang.DOT) + " not found");
			}
			if (child instanceof JsonObject) {
				((JsonObject)child).set (prop, oValue);
			} else if (child instanceof JsonArray) {
				boolean append = false;
				int iProp = -1;
				if (prop.equals (Lang.UNDERSCORE)) {
					append = true;
				} else {
					try {
						iProp = Integer.valueOf (prop);
					} catch (Exception ex) {
						// ignore
					}
				}
				JsonArray array = (JsonArray)child;
				if (append) {
					array.add (oValue);
				} else {
					if (iProp > -1 && array.count () > iProp) {
						((JsonArray)child).add (iProp, oValue);
					}
				}
			} 
		}
		
		return new DefaultCommandResult (CommandResult.OK, json);
	}


	@Override
	public String getName () {
		return "set";
	}

	@Override
	public String getDescription () {
		return "set a property/value pair. set aJsonVar address.city Sunnyvale\n" + 
				"Setting a property of type (json objbect or array)\n\tset aJsonVar user j\\{ \"name\": \"James\", \"age\": \"32\"}" +
				"\n\tset aJsonVar geoloc j\\[48.8566140, 2.3522220]";
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
						return "a property name. property could be new or existsing in the current json object. ";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "value";
					}
					@Override
					public String desc () {
						return "property value";
					}
				}
		};
	}

}
