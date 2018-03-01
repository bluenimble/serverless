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
package com.bluenimble.platform.icli.mgm.commands.mgm;

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.icli.mgm.commands.mgm.RemoteCommand.Spec;
import com.bluenimble.platform.icli.mgm.utils.RemoteUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class RemoteCommandHandler implements CommandHandler {

	private static final long serialVersionUID = -1788979056133712219L;

	private String 		name;
	private JsonObject 	source;
	
	private Arg [] args;
	
	public RemoteCommandHandler (String name, JsonObject source) {
		this.name 	= name;
		this.source = source;
		JsonArray aArgs = Json.getArray (source, RemoteCommand.Spec.Args);
		if (aArgs == null || aArgs.isEmpty ()) {
			return;
		}
		
		args = new Arg [aArgs.count ()];
		
		for (int i = 0; i < aArgs.count (); i++) {
			final JsonObject oArg = (JsonObject)aArgs.get (i);
			args [i] = new AbstractArg () {
				@Override
				public String name () {
					return Json.getString (oArg, RemoteCommand.Spec.Arg.Name);
				}
				@Override
				public String desc () {
					return Json.getString (oArg, RemoteCommand.Spec.Arg.Desc);
				}
				@Override
				public Type type () {
					return Type.valueOf (Json.getString (oArg, RemoteCommand.Spec.Arg.Type, Type.String.name ()));
				}
				@Override
				public boolean required () {
					return Json.getBoolean (oArg, RemoteCommand.Spec.Arg.Required, true);
				}
			};
		}
	}
	
	@Override
	public CommandResult execute (Tool tool, final String... args) throws CommandExecutionException {
		
		final Map<String, String> options = new HashMap<String, String> ();
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				options.put (String.valueOf (i), args [i]);
			}
		}
		
		return RemoteUtils.processRequest (tool, source, options);
	}

	@Override
	public String getName () {
		return name;
	}

	@Override
	public String getDescription() {
		return Json.getString (source, Spec.Description);
	}

	@Override
	public Arg [] getArgs () {
		return args;
	}

}
