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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.CommandHandler.AbstractArg;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;
import com.bluenimble.platform.icli.mgm.utils.CodeGenUtils;
import com.bluenimble.platform.json.JsonObject;

public class CreateApiContextHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("api or function namespace required. ex. cli api myNamespace");
		}
		
		String namespace 	= args [0];
		
		String contextName 	= namespace;
		if (args.length > 1) {
			contextName = args [1];
		}
		
		if (contextName.equalsIgnoreCase ("global")) {
			return new DefaultCommandResult (CommandResult.KO, "Invalid context name");
		}
		
		File apiFolder = new File (BlueNimble.Workspace, namespace);
		if (!apiFolder.exists ()) {
			return new DefaultCommandResult (CommandResult.KO, "api not found in workspace");
		}
		
		File contextFolder = new File (BlueNimble.Work, contextName);
		if (contextFolder.exists ()) {
			try {
				FileUtils.delete (contextFolder);
			} catch (IOException e) {
				throw new CommandExecutionException (e.getMessage (), e);
			}
		}
		
		// create the context folder
		contextFolder.mkdir ();
		// create command files
		// add context to the icli
		// ctx namespace
		// signup 

		return new DefaultCommandResult (CommandResult.OK, null);
	}

	@Override
	public String getName () {
		return "api";
	}

	@Override
	public String getDescription () {
		return "create a commands for all your api services and functions";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "namespace";
					}
					@Override
					public String desc () {
						return "function or api namespace. In general, it's your application name";
					}
				},
				new AbstractArg () {
					@Override
					public String name () {
						return "context";
					}
					@Override
					public String desc () {
						return "context name. By default, the context name will be the same as the api namespace";
					}
					@Override
					public boolean required () {
						return false;
					}
				}
		};
	}

}
