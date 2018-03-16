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

import java.io.File;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;

public class FeaturesCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	protected FeaturesCommand (String name, String description) {
		super (name, description);
	}

	public FeaturesCommand () {
		super ("features", "list feature templates");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		File features = new File (BlueNimble.Home, "templates/features");
		
		File [] files = features.listFiles ();
		
		StringBuilder sb = new StringBuilder ();
		for (File file : files) {
			sb.append (file.getName ().substring (0, file.getName ().lastIndexOf (Lang.DOT))).append (Lang.ENDLN);
		}
		
		tool.printer ().content (features.getName (), sb.toString ());
		
		sb.setLength (0);
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}
}
