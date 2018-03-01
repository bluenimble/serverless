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
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec.Templates;
import com.bluenimble.platform.icli.mgm.utils.CodeGenUtils;

public class CreateFunctionHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	private static final String DefaultTemplate = "default";

	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("function name required. Ex. 'create function finance/makePayment'");
		}
		
		String nsAndName = args [0].trim ();
		
		int indexOfSlash = nsAndName.indexOf (Lang.SLASH);
		if (indexOfSlash <= 0) {
			throw new CommandExecutionException ("function namespace required. Ex. 'create function finance/makePayment'");
		}
		
		String namespace 	= nsAndName.substring (0, indexOfSlash).trim ();
		if (Lang.isNullOrEmpty (namespace)) {
			throw new CommandExecutionException ("function namespace required. Ex. 'create function finance/makePayment'");
		}
		String function 	= nsAndName.substring (indexOfSlash + 1).trim ();
		if (Lang.isNullOrEmpty (function)) {
			throw new CommandExecutionException ("function name required. Ex. 'create function finance/makePayment'");
		}
		
		boolean newNs = false;
		
		File nsFolder = new File (BlueNimble.Workspace, namespace);
		if (!nsFolder.exists ()) {
			nsFolder.mkdirs ();
			newNs = true;
		}
		
		File fnFolder = new File (nsFolder, function);
		if (fnFolder.exists ()) {
			throw new CommandExecutionException ("function '" + nsAndName + "' already exists");
		} else {
			fnFolder.mkdirs ();
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		String template 	= (String)vars.get (BlueNimble.DefaultVars.TemplateFunctions);
		if (Lang.isNullOrEmpty (template)) {
			template = DefaultTemplate;
		}
		
		File templateFolder = 
				new File (new File (BlueNimble.Home, Templates.class.getSimpleName ().toLowerCase () + Lang.SLASH + Templates.Functions), template);
		
		Map<String, String> tokens = new HashMap<String, String> ();
		tokens.put (CodeGenUtils.Tokens.Function, function);
		
		// write api files
		if (newNs) {
			CodeGenUtils.writeFile (new File (templateFolder, "boot.json"), new File (nsFolder, "boot.json"), tokens);
			tool.printer ().node (1, "  namespace spec created '" + nsFolder.getName () + "/boot.json'"); 
			CodeGenUtils.writeFile (new File (templateFolder, "Boot.js"), new File (nsFolder, "Boot.js"), tokens);
			tool.printer ().node (1, "  namespace code created '" + nsFolder.getName () + "/Boot.js'"); 
		}
		
		// write function files
		CodeGenUtils.writeFile (new File (templateFolder, "function/spec.json"), new File (fnFolder, "spec.json"), tokens);
		tool.printer ().node (1, "   function spec created '" + nsFolder.getName () + "/" + fnFolder.getName () + "/code.json'"); 
		CodeGenUtils.writeFile (new File (templateFolder, "function/code.js"), new File (fnFolder, "code.js"), tokens);
		tool.printer ().node (1, "   function code created '" + nsFolder.getName () + "/" + fnFolder.getName () + "/code.js'"); 
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}
	
	@Override
	public String getName () {
		return "function";
	}

	@Override
	public String getDescription () {
		return "create a function (spec and code).\n\t'create function [NameSpace/FunctionName]'\n\tExample: 'create function finance/makePayment'";
	}

	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "function name";
					}
					@Override
					public String desc () {
						return "the full name of the function including the namespace";
					}
				}
		};
	}

}
