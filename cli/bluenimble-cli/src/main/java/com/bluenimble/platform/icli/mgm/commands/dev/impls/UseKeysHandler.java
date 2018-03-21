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

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;

public class UseKeysHandler implements CommandHandler {

	private static final long serialVersionUID = 7185236990672693349L;
	
	@Override
	public CommandResult execute (Tool tool, String... args) throws CommandExecutionException {
		
		if (args == null || args.length < 1) {
			throw new CommandExecutionException ("keys name required. ex. use keys your-app-prod");
		}
		
		String alias = args [0];
		
		try {
			BlueNimble.useKeys (alias);
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		return new DefaultCommandResult (CommandResult.OK, "keys " + alias + (alias.equals (BlueNimble.keys ().alias ()) ? " is" : " isn't") + " current");
	}


	@Override
	public String getName () {
		return "keys";
	}

	@Override
	public String getDescription () {
		return "use some keys provided to you by a bluenimble peer";
	}
	
	@Override
	public Arg [] getArgs () {
		return new Arg [] {
				new AbstractArg () {
					@Override
					public String name () {
						return "name";
					}
					@Override
					public String desc () {
						return "keys name. Keys are detected automatically by the iCLI. The name of the keys is exactly the same as the name of the .keys file";
					}
				}
		};
	}

}
