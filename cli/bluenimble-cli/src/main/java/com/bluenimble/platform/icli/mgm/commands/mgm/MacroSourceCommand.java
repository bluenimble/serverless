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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.Keys;
import com.bluenimble.platform.icli.mgm.utils.BuildUtils;
import com.bluenimble.platform.icli.mgm.utils.JsTool;

public class MacroSourceCommand extends AbstractCommand {

	private static final long serialVersionUID = 3523915768661531476L;

    private static final ScriptEngine Engine = new ScriptEngineManager ().getEngineByName ("JavaScript");

    private File script;
	
	public MacroSourceCommand (String context, String name, File script) {
		this.context 	= context;
		this.name 		= name;
		this.script 	= script;
	}

	@Override
	public CommandResult execute (final Tool tool, Map<String, CommandOption> options) throws CommandExecutionException {

		Reader reader = null;
		
		SimpleBindings bindings = new SimpleBindings ();
		
		String command = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (!Lang.isNullOrEmpty (command)) {
			bindings.put ("Command", command);
		}
		
		bindings.put ("Home", BlueNimble.Work);
		
		bindings.put ("Config", BlueNimble.Config);
		
		bindings.put ("Tool", new JsTool (tool));
		
		bindings.put ("Vars", tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS));
		
		bindings.put ("BuildTool", new BuildUtils ());
		
		Keys keys = BlueNimble.keys ();
		if (keys != null) {
			bindings.put ("Keys", keys.json ());
		}
		
		try {
			reader = new FileReader (script);
			Engine.eval (new FileReader (script), bindings);
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (reader);
		}
        
		return null;
	}

}
