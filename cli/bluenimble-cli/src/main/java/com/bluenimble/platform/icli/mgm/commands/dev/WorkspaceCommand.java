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
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.json.JsonObject;

public class WorkspaceCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	protected WorkspaceCommand (String name, String description) {
		super (name, description);
	}

	public WorkspaceCommand () {
		super ("workspace", "set your work folder");
		synonym = "ws";
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String workFolder = (String)tool.currentContext ().get (ToolContext.CommandLine);
		
		try {
			if (Lang.isNullOrEmpty (workFolder)) {
				if (BlueNimble.Workspace == null) {
					tool.printer ().warning ("No workspace found!\nUse command 'ws [Your Workspace Foler Path]'.\nExample: ws /applications/bluenimble"); 
				} else {
					tool.printer ().content ("__PS__ GREEN:Workspace", BlueNimble.Workspace.getAbsolutePath ()); 
					
					String currentApi = Json.getString (BlueNimble.Config, CliSpec.Config.CurrentApi);
					JsonObject apis = Json.getObject (BlueNimble.Config, CliSpec.Config.Apis);
					if (apis != null && !apis.isEmpty ()) {
						StringBuilder sApis = new StringBuilder ();
						Iterator<String> keys = apis.keys ();
						while (keys.hasNext ()) {
							String api = keys.next ();
							String prefix = "    ";
							if (api.equals (currentApi)) {
								prefix = "(C) ";
							} 
							sApis.append (prefix + api).append (Lang.ENDLN); 
						}
						tool.printer ().content ("__PS__ GREEN:Apis", sApis.toString ());
						sApis.setLength (0);
					} else {
						tool.printer ().content ("__PS__ GREEN:Apis", "No apis found in this workspace"); 
					}
				}
				return null;
			}
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		File workspace = new File (workFolder);
		if (!workspace.exists () || !workspace.isDirectory ()) {
			throw new CommandExecutionException (workFolder + " is not a valid folder");
		}
		try {
			BlueNimble.workspace (workspace);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		return new DefaultCommandResult (CommandResult.OK, "workspace sat to '" + workspace.getAbsolutePath () + "'");
	}
}
