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

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.json.JsonObject;

public class JsonShrinkHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("wrong number of arguments");
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
		json.shrink ();
		
		tool.printer ().content (
			"__PS__ GREEN:" + var, 
			"shrinked successfuly"
		);
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}

	@Override
	public String getName () {
		return "set";
	}

	@Override
	public String getDescription () {
		return "get a property value. get aJsonVar address.city\n";
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
				}
		};
	}

}
