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

import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;

public class ManCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;

	public ManCommand () {
		super ("man", I18nProvider.get (I18N_COMMANDS + "man.desc"));
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		CommandOption co = options.get (CommandOption.CMD_LINE);
		
		if (co == null || co.getArg (0) == null) {
			return new DefaultCommandResult (CommandResult.KO, "Specify a command");
		}
		
		String commandName = (String)co.getArg (0);
		
		commandName = commandName.trim ();
		
		int indexOfSpace = commandName.indexOf (Lang.SPACE);
		if (indexOfSpace > 0) {
			commandName = commandName.substring (0, indexOfSpace);
		}

		Command command = tool.getCommand (commandName);
		
		String man = null;
		
		if (command == null) {
			man = tool.getManual (commandName);
			if (man == null) {
				return new DefaultCommandResult (CommandResult.KO, "Command not found in any context");
			}
		}
		
		if (command != null && !command.forContext (tool.currentContext ())) {
			return new DefaultCommandResult (CommandResult.KO, "Command not accessible by the current context");
		}
		
		if (man == null) {
			man = command.manual ();
			if (man == null || man.trim ().isEmpty ()) {
				man = command.describe ();
			}
		}
		
		return new DefaultCommandResult (CommandResult.OK, man);
	}

}
