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
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class JsonSearchHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 3) {
			throw new CommandExecutionException ("wrong number of arguments");
		}
		
		String var = args [0];
		String prop = args [1];
		String search = args [2];
		
		int arraySeparator = prop.indexOf (Lang.SLASH);
		
		if (arraySeparator < 0) {
			throw new CommandExecutionException ("invalid argument '" + prop + "'. It should be in this format YourArray/fruits.banana");
		}
		
		String 		array 		= prop.substring (0, arraySeparator).trim ();
		String [] 	accessors 	= Lang.split (prop.substring (arraySeparator + 1).trim (), Lang.DOT);
		
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
		
		Object oArray = json.get (array);
		if (!(oArray instanceof JsonArray)) {
			throw new CommandExecutionException ("'" + array + "' isn't a valid json array");
		}
		
		JsonObject result 	= new JsonObject ();
		JsonArray  found 	= new JsonArray ();
		result.set (ApiOutput.Defaults.Items, found);
				
		JsonArray a = (JsonArray)oArray;
		if (a.count () == 0) {
			return new DefaultCommandResult (CommandResult.OK, result);
		}
		
		for (int i = 0; i < a.count (); i++) {
			Object record = a.get (i);
			if (!(record instanceof JsonObject)) {
				continue;
			}
			
			JsonObject oRecord = (JsonObject)record;
			
			Object pValue = Json.find (oRecord, accessors);
			if (pValue == null) {
				continue;
			}
			
			String sValue = String.valueOf (pValue);
			
			if (search.equals (sValue) || sValue.contains (search) || sValue.matches (search)) {
				found.add (record);
			}
		}
		
		return new DefaultCommandResult (CommandResult.OK, result);
	}

	@Override
	public String getName () {
		return "set";
	}

	@Override
	public String getDescription () {
		return "get all objects in an array in the given json object where a property containts the string in argument. search aJsonVar anArray/address.city New\n";
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
						return "the property path, ie. array.property";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "value";
					}
					@Override
					public String desc () {
						return "the search pattern";
					}
				}
		};
	}

}
