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
package com.bluenimble.platform.icli.mgm.commands.dev;

import java.util.Map;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.PrefixedCommand;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.commands.dev.impls.CreateApiContextHandler;

public class CreateContextCommand extends PrefixedCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	interface Subject {
		String Api 		= "api";
	}
	
	public CreateContextCommand () {
		super ("cli", "cli api [api or function namespace]");
		addHandler (Subject.Api, new CreateApiContextHandler ());
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		if (BlueNimble.Workspace == null) {
			throw new CommandExecutionException ("please set a valid workspace folder. use command 'ws <your workspace folder>' or 'ws <your workspace folder>'");
		}
		
		return super.execute (tool, options);
	}
}
