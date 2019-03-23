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

import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;

public class ChangeContextCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;

	public ChangeContextCommand () {
		super ("ctx", I18nProvider.get (I18N_COMMANDS + "ctx.desc"));
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		CommandOption co = options.get (CommandOption.CMD_LINE);
		
		if (co == null || co.getArg (0) == null) {
			return new DefaultCommandResult (CommandResult.KO, "Specify a valid context name");
		}
		
		String contextAlias = (String)co.getArg (0);
		
		contextAlias = contextAlias.toLowerCase ();
		ToolContext ctx = tool.getContext (contextAlias);
		if (ctx == null) {
			return new DefaultCommandResult (CommandResult.KO, "Context [" + contextAlias + "] not registered with this tool");
		}
		tool.changeContext (ctx);
		return new DefaultCommandResult (CommandResult.OK, "Context changed to [" + ctx.getName () + "]");
	}

}
