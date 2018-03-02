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
package com.bluenimble.platform.cli.command.impls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;

public class PrefixedCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	protected Map<String, CommandHandler> handlers = new LinkedHashMap<String, CommandHandler> ();
	
	protected PrefixedCommand () {
		super ();
	}

	protected PrefixedCommand (String name, String description) {
		super (name, description);
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String cmd = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (cmd)) {
			throw new CommandExecutionException ("Command '" + name + "' missing arguments").showUsage ();
		}
		
		String subject = cmd;
		
		int indexOfSapce = cmd.indexOf (Lang.SPACE);
		if (indexOfSapce > 0) {
			subject = cmd.substring (0, indexOfSapce).trim ();
			cmd 	= cmd.substring (indexOfSapce + 1);
		} else {
			cmd 	= null;
		}
		
		CommandHandler handler = handlers.get (subject);
		if (handler == null) {
			throw new CommandExecutionException ("'" + getName () + " " + subject + "' not found");
		}
		
		// list of commands
		String [] commands = Lang.split (cmd, Lang.SEMICOLON);
		
		if (commands != null && commands.length > 1) {
			for (String c : commands) {
				c = c.trim ();
				CommandResult result = handler.execute (tool, args (handler.getArgs (), c));
				try {
					if (result != null && result.getContent () != null) {
						if (result.getType () == CommandResult.OK) {
							tool.printer ().content (null, String.valueOf (result.getContent ()));
						} else {
							tool.printer ().error (String.valueOf (result.getContent ()));
						}
					}
				} catch (Exception ex) {
					// Ignore
				}
			}
			return null;
		}
		
		return handler.execute (tool, args (handler.getArgs (), cmd));
	}
	
	protected String [] args (CommandHandler.Arg [] args, String cmd) throws CommandExecutionException {
		String [] sArgs = args (args == null ? 0 : args.length, cmd);
		
		if (args == null) {
			return sArgs;
		}
		
		boolean atLeastOneRequired = atLeastOneRequired (args);
		
		if (atLeastOneRequired && (sArgs == null || sArgs.length == 0)) {
			throw new CommandExecutionException ("missing arguments").showUsage ();
		}
		
		for (int i = 0; i < args.length; i++) {
			CommandHandler.Arg arg = args [i];
			if (arg.required () && (sArgs.length - 1) < i) {
				throw new CommandExecutionException ("missing arguments").showUsage ();
			}
		}
		
		return sArgs;
	}
	
	private boolean atLeastOneRequired (CommandHandler.Arg [] args) {
		for (CommandHandler.Arg arg : args) {
			if (arg.required ()) {
				return true;
			}
		}
		return false;
	}

	protected String [] args (int argsCount, String cmd) {
		if (Lang.isNullOrEmpty (cmd)) {
			return null;
		}
		if (argsCount == 0) {
			return null;
		}
		if (argsCount == 1) {
			return new String [] { cmd.trim () };
		}
		List<String> lArgs = new ArrayList<String> ();
		for (int i = 0; i < argsCount - 1; i++) {
			if (Lang.isNullOrEmpty (cmd)) {
				break;
			}
			int indexOfSpace = cmd.indexOf (Lang.SPACE);
			if (indexOfSpace < 0) {
				lArgs.add (filter (cmd));
				cmd = null;
				break;
			}
			if (indexOfSpace > 0) {
				lArgs.add (filter (cmd.substring (0, indexOfSpace)));
				cmd = cmd.substring (indexOfSpace + 1);
			} 
		}
		
		if (!Lang.isNullOrEmpty (cmd)) {
			lArgs.add (filter (cmd));
		}
		
		return lArgs.toArray (new String [lArgs.size ()]);
	}
	
	private String filter (String value) {
		value = value.trim ();
		if (value.startsWith (Lang.QUOT) && value.endsWith (Lang.QUOT)) {
			value = value.substring (1, value.length () - 1);
		}
		return value;
	}

	public void addHandler (String name, CommandHandler commandHandler) {
		handlers.put (name, commandHandler);
	}
	public CommandHandler getHandler (String name) {
		return handlers.get (name);
	}
	public Map<String, CommandHandler> handlers () {
		return handlers;
	}
	
}
