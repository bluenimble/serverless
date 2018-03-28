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

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.api.ApiHeaders;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.icli.mgm.commands.mgm.RemoteCommand.Spec;
import com.bluenimble.platform.icli.mgm.utils.RemoteUtils;
import com.bluenimble.platform.json.JsonObject;

public class HttpHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private String verb;
	
	public HttpHandler (String verb) {
		this.verb = verb;
	}
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("keys name required. ex. use keys your-app-prod");
		}
		
		String varOrUrl = args [0];
		
		final Map<String, String> options = new HashMap<String, String> ();
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				options.put (String.valueOf (i), args [i]);
			}
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		JsonObject spec = null;
		
		Object oSpec = vars.get (varOrUrl);
		if (oSpec == null) {
			spec = (JsonObject)new JsonObject ().set (Spec.request.class.getSimpleName (), new JsonObject ().set (Spec.request.Service, varOrUrl));
		} else {
			spec = ((JsonObject)oSpec).duplicate ();
		}
		
		JsonObject request = Json.getObject (spec, Spec.request.class.getSimpleName ());
		
		if (request != null) {
			if (!request.containsKey (Spec.request.Sign)) {
				request.set (Spec.request.Sign, false);
			}
			JsonObject headers = Json.getObject (request, Spec.request.Headers);
			if (headers == null) {
				headers = new JsonObject ();
				request.set (Spec.request.Headers, headers);
			}
			if (!headers.containsKey (ApiHeaders.Accept)) {
				headers.set (ApiHeaders.Accept, "*/*");
			}
		}
		
		return RemoteUtils.processRequest (tool, spec, options);
		
	}


	@Override
	public String getName () {
		return verb;
	}

	@Override
	public String getDescription () {
		return "make an http " + verb + " request";
	}
	
	@Override
	public Arg [] getArgs () {
		return new Arg [] {
			new AbstractArg () {
				@Override
				public String name () {
					return "varOrUrl";
				}
				@Override
				public String desc () {
					return "a json variable name, holding the request spec.";
				}
			}
		};
	}

}
