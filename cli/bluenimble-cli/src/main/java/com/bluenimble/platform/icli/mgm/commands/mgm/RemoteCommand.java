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
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.CommandHandler.AbstractArg;
import com.bluenimble.platform.cli.command.CommandHandler.Arg;
import com.bluenimble.platform.cli.command.impls.PrefixedCommand;
import com.bluenimble.platform.icli.mgm.utils.RemoteUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class RemoteCommand extends PrefixedCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	public interface Spec {
		String Name 		= "name";
		String Synonym 		= "synonym";
		String Description 	= "description";
		String OkMessage	= "okMessage";
		String Handlers		= "handlers";
		String Args			= "args";
		interface Arg {
			String Name 		= "name";
			String Desc 		= "desc";
			String Required 	= "required";
			String Type			= "type";
		}
		interface delegate {
			String If 			= "if";
			String Equals 		= "equals";
			String Then 		= "then";
		}
		interface Option {
			String Required 	= "required";
			String Label 		= "label";
			String ArgsCount 	= "argsCount";
			String Protected 	= "protected";
			String Mapping 		= "mapping";
		}
		interface request {
			String Endpoint		= "endpoint";
			String Method 		= "method";
			String Service 		= "service";
			String ContentType 	= "contentType";
			String Headers 		= "headers";
			String Parameters 	= "params";
			String Body 		= "body";
			String Value 		= "value";
			String Expression	= "#value";
			String Index		= "index";
			String Sign			= "sign";
			interface Parameter {
			}
			interface Header {
			}
			interface Body {
			}
		}
		interface response {
			interface Unzip {
				String Dest 	= "dest";
				String Folder 	= "folder";
			}
			interface Replace {
				String File 	= "file";
				String Token 	= "token";
				String By 		= "by";
			}
		}
	}
	
	private JsonObject 	source;
	
	private Arg [] args;
	
	public RemoteCommand (String context, String name, JsonObject source) {
		this.context 		= context;
		this.name 			= name;
		this.synonym 		= Json.getString (source, Spec.Synonym);
		this.description 	= Json.getString (source, Spec.Description);
		
		this.source 		= source;

		JsonObject handlers = Json.getObject (source, Spec.Handlers);
		if (handlers == null || handlers.isEmpty ()) {
			return;
		}
		Iterator<String> keys = handlers.keys ();
		while (keys.hasNext ()) {
			String key = keys.next ();
			addHandler (key, new RemoteCommandHandler (key, Json.getObject (handlers, key)));
		}
		
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
				public boolean required () {
					return Json.getBoolean (oArg, RemoteCommand.Spec.Arg.Required, true);
				}
			};
		}

	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		if (handlers == null || handlers.isEmpty ()) {
			Map<String, String> data = new HashMap<String, String> ();
			if (options != null && !options.isEmpty ()) {
				for (CommandOption o : options.values ()) {
					data.put (o.name (), (String)o.getArg (0));
				}
			} 
			
			String cmd = (String)tool.currentContext ().get (ToolContext.CommandLine);
			
			String [] aArgs = args (args, cmd);
			
			if (aArgs != null && aArgs.length > 0) {
				for (int i = 0; i < aArgs.length; i++) {
					data.put (String.valueOf (i), aArgs [i]);
				}
			}
			return RemoteUtils.processRequest (tool, source, data);
		}
		return super.execute (tool, options);
	}

}
