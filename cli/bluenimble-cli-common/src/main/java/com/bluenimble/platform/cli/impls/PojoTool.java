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
package com.bluenimble.platform.cli.impls;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.InstallI18nException;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.command.impls.ChangeContextCommand;
import com.bluenimble.platform.cli.command.impls.ClearCommand;
import com.bluenimble.platform.cli.command.impls.DebugCommand;
import com.bluenimble.platform.cli.command.impls.DelimeterCommand;
import com.bluenimble.platform.cli.command.impls.EchoCommand;
import com.bluenimble.platform.cli.command.impls.ExitCommand;
import com.bluenimble.platform.cli.command.impls.HelpCommand;
import com.bluenimble.platform.cli.command.impls.HistoCommand;
import com.bluenimble.platform.cli.command.impls.HistofCommand;
import com.bluenimble.platform.cli.command.impls.JsonCommand;
import com.bluenimble.platform.cli.command.impls.ManCommand;
import com.bluenimble.platform.cli.command.impls.QuitCommand;
import com.bluenimble.platform.cli.command.impls.ScriptCommand;
import com.bluenimble.platform.cli.command.impls.SetCommand;
import com.bluenimble.platform.cli.command.impls.SplitCommand;
import com.bluenimble.platform.cli.command.impls.UnSetCommand;
import com.bluenimble.platform.cli.command.impls.VarsCommand;
import com.bluenimble.platform.cli.command.impls.WGetCommand;

public abstract class PojoTool extends AbstractTool {
	
	private static final long serialVersionUID = -5818667146115900912L;

	protected Map<String, String> synonyms = new HashMap<String, String> (); 
	protected Map<String, Command> commands = new LinkedHashMap<String, Command> ();
	protected Map<String, ToolContext> availableContexts;
	
	protected Map<String, String> manuals = new HashMap<String, String> (); 
	
	protected PojoTool () throws InstallI18nException {
		super ();
		// add basic commands
		addCommand (new QuitCommand ());
		addCommand (new ExitCommand ());
		addCommand (new ClearCommand ());
		addCommand (new HelpCommand ());
		addCommand (new HistoCommand ());
		addCommand (new HistofCommand ());
		addCommand (new ChangeContextCommand ());
		addCommand (new ManCommand ());
		addCommand (new SetCommand ());
		addCommand (new UnSetCommand ());
		addCommand (new VarsCommand ());
		addCommand (new JsonCommand ());
		addCommand (new ScriptCommand ());
		addCommand (new WGetCommand ());
		addCommand (new DebugCommand ());
		addCommand (new DelimeterCommand ());
		addCommand (new EchoCommand ());
		addCommand (new SplitCommand ());
	}

	@Override
	public Command getCommand (String commandName) {
		return _getCommand (commandName, true);
	}
	public Command _getCommand (String commandName, boolean checkSynonym) {
		if (commandName == null) {
			return null;
		}
		String commandKey = currentContext ().getAlias () + Lang.SLASH + commandName.toLowerCase ();
		Command command = commands.get (commandKey);
		if (command == null) {
			command = commands.get (ROOT_CTX + Lang.SLASH + commandName.toLowerCase ());
		}
		if (command != null) {
			return command;
		}
		if (!checkSynonym) {
			return null;
		}
		String commandForSyn = getCommandForSynonym (commandKey);
		if (commandForSyn == null) {
			return null;
		}
		return getCommand (commandForSyn);
	}

	@Override
	public void addCommand (Command command) {
		if (command == null || command.getName () == null) {
			return;
		}
		
		String synonym = command.getSynonym ();
		
		String context = (command.getContext () == null ? ROOT_CTX : command.getContext ());
		
		String commandKey = context + Lang.SLASH + command.getName ().toLowerCase ();
		if (!Lang.isNullOrEmpty (synonym)) {
			synonym = context + Lang.SLASH + synonym;
		}
		commands.put (commandKey, command);
		if (!Lang.isNullOrEmpty (synonym)) {
			synonyms.put (synonym, command.getName ().toLowerCase ());
		}
	}

	@Override
	public Iterable<Command> getCommands () {
		return commands.values ();
	}

	@Override
	public void shutdown () {
		super.shutdown ();
		commands.clear ();
		availableContexts.clear ();
	}

	@Override
	public ToolContext getContext (String ctxName) {
		if (ctxName == null) {
			return null;
		}
		return availableContexts.get (ctxName.toLowerCase ());
	}

	@Override
	public void addContext (ToolContext ctx) {
		if (ctx == null || ctx.getAlias () == null) {
			return;
		}
		if (availableContexts == null) {
			availableContexts = new HashMap<String, ToolContext> (); 
		}
		availableContexts.put (ctx.getAlias ().toLowerCase (), ctx);
	}

	public String getCommandForSynonym (String synonym) {
		return synonyms.get (synonym);
	}
	
	public void addManual (String name, String manual) {
		manuals.put (name, manual);
	}

	@Override
	public String getManual (String name) {
		return manuals.get (name);
	}

	@Override
	public String getName () {
		return "tool";
	}

	@Override
	public void onReady () {
	}

	@Override
	public String getDescription () {
		return "Example Tool demonstrating how can we implement tool based softwares using devbite CLI";
	}

}
