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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonException;
import com.bluenimble.platform.json.JsonObject;

public class JsonJoinHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("json variable name required");
		}
		
		String sep = Lang.SPACE;
		
		if (args.length > 1) {
			sep = args [1];
		}
		
		String var = args [0];
		String prop = null;
		
		int indexOfSlash = var.indexOf (Lang.SLASH);
		if (indexOfSlash > 0) {
			prop = var.substring (indexOfSlash + 1);
			var = var.substring (0, indexOfSlash);
		}
		
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
		
		Object a = json.find (prop, Lang.SLASH);
		if (!(a instanceof JsonArray)) {
			throw new CommandExecutionException ("property '" + prop + "' isn't a valid array");
		}
		
		JsonArray array = (JsonArray)a;
		
		try {
			return new DefaultCommandResult (CommandResult.OK, join (array, sep));
		} catch (JsonException e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
	}

    private String join (JsonArray a, String separator) throws JsonException {
        int len = a.count ();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i += 1) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append (String.valueOf (a.get (i)));
        }
        String s = sb.toString();
        sb.setLength (0);
        sb = null;
        return s;
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
						return "array path";
					}
					@Override
					public String desc () {
						return "the variable name should be part of the the path.\nExample myvar/myarray";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "seperator";
					}
					@Override
					public boolean required () {
						return false;
					}
					@Override
					public String desc () {
						return "a property name. property could be new or existsing in the current json object. ";
					}
				}
		};
	}

}
