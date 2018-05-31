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

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.utils.SpecUtils;
import com.bluenimble.platform.json.JsonObject;

public class ApiCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	protected ApiCommand (String name, String description) {
		super (name, description);
	}

	public ApiCommand () {
		super ("api", "set current api namespace");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		JsonObject config = BlueNimble.Config;

		String apiNs = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (apiNs)) {
			String currentApi = Json.getString (config, CliSpec.Config.CurrentApi);
			try {
				if (Lang.isNullOrEmpty (currentApi)) {
					tool.printer ().info ("use ' api YourApi ' or create a new one using ' create api YourApi '"); 
				} else {
					
					String apiPath = Json.getString (Json.getObject (BlueNimble.Config, CliSpec.Config.Apis), currentApi);
					
					File apiFolder = new File (BlueNimble.Workspace, apiPath);
					
					ValueHolder<Integer> counter = new ValueHolder<Integer>(0);
					
					SpecUtils.visitService (SpecUtils.servicesFolder (apiFolder), (file) -> {
						counter.set (counter.get () + 1);
						return null;
					});
					
					tool.printer ().content (
						"__PS__ GREEN:Current Api", 
						" namespace: " + currentApi + Lang.ENDLN +
						"      path: $ws/ " + Json.getString (Json.getObject (config, CliSpec.Config.Apis), currentApi) + Lang.ENDLN +
						"# services: " + counter.get ()
					);
				}
				return null;
			} catch (Exception ex) {
				throw new CommandExecutionException (ex.getMessage (), ex);
			}
		}
		
		apiNs = apiNs.trim ();
		
		String apiPath = Json.getString (Json.getObject (config, CliSpec.Config.Apis), apiNs);
		if (Lang.isNullOrEmpty (apiPath)) {
			tool.printer ().info ("api '" + apiNs + "' not found in workspace");
			return null;
		}
		
		config.set (CliSpec.Config.CurrentApi, apiNs);

		try {
			BlueNimble.saveConfig ();
			tool.printer ().content ("__PS__ GREEN:Current Api", "namespace: " + apiNs + "\npath: $ws/ " + Json.getString (Json.getObject (config, CliSpec.Config.Apis), apiNs));
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		return new DefaultCommandResult (CommandResult.OK, null);
	}
}
