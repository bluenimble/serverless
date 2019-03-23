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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;

public class ScriptSourceCommand extends AbstractCommand {

	private static final long serialVersionUID = 3523915768661531476L;
	
	private static final Pattern SplitPattern = Pattern.compile ("([^\"]\\S*|\".+?\")\\s*");
	
	private File script;
	
	public ScriptSourceCommand (String context, String name, File script) {
		this.context 	= context;
		this.name 		= name;
		this.script 	= script;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options) throws CommandExecutionException {
		BufferedReader reader = null;
		
		if (!script.exists ()) {
			throw new CommandExecutionException ("Script file " + script.getAbsolutePath () + " not found");
		}
		
		String cmd = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (!Lang.isNullOrEmpty (cmd)) {
			Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
			String [] args = args (cmd);
			for (int i = 0; i < args.length; i++) {
				vars.put ("arg." + i, args [i]);
			}
		}
		
		try {
			reader = new BufferedReader (new InputStreamReader (new FileInputStream (script)));
			CommandResult result = runCommands (name, reader, tool);
			if (result != null) {
				return result;
			}
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (reader);
		}
		
		return null;
	}

	private CommandResult runCommands (String name, BufferedReader reader, Tool tool) throws CommandExecutionException {
		try {
			int res;
			String s;
			while ((s = reader.readLine ()) != null) {
				if (Lang.isNullOrEmpty (s)) {
					continue;
				}
				if (s.trim ().startsWith ("#")) {
					continue;
				}
				res = tool.processCommand (s, false);
				if (res == Tool.FAILURE) {
					return new DefaultCommandResult (CommandResult.KO, "'" + name + "' Script is stopped due to errors");
				}
			}
		} catch (IOException e) {
			throw new CommandExecutionException (e.getMessage (), e);
		} finally {
			try {
				reader.close ();
			} catch (IOException ioex) {
				// IGNORE
			}
		}
		return null;
	}

	protected String [] args (String cmd) {
		List<String> lArgs = new ArrayList<String> ();
		Matcher m = SplitPattern.matcher (cmd);
		while (m.find ()) {
			String value = m.group (1);
			if (value.startsWith (Lang.QUOTE) && value.endsWith (Lang.QUOTE)) {
				value = value.substring (1, value.length () - 1);
			}
			lArgs.add (value); 
		}
		return lArgs.toArray (new String [lArgs.size ()]);
	}

}
