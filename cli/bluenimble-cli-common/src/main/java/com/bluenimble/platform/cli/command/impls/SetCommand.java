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
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.impls.YamlObject;

public class SetCommand extends AbstractCommand {

	private static final long serialVersionUID = 1852287917060945550L;
	
	public SetCommand () {
		super ("set", I18nProvider.get (I18N_COMMANDS + "set.desc"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {

		String cmd = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (cmd)) {
			throw new CommandExecutionException ("command '" + name + "' missing arguments").showUsage ();
		}
		
		String [] nameValue = Lang.split (cmd, Lang.SPACE, true);
		
		String varName 	= nameValue [0];
		Object value = Lang.BLANK;
		
		if (nameValue.length > 1) {
			value = nameValue [1];
		}
		
		if (value == null) {
			value = Lang.BLANK;
		}
		
		final Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		try {
			
			if (varName.equals (Tool.ParaPhraseVar)) {
				tool.setParaphrase ((String)value, true);
				tool.printer ().content ("Security", "Paraphase Updated");
			}
			
			if (value instanceof YamlObject) {
				value = ((YamlObject)value).toJson ();
			}
			
			value = varName.equals (Tool.ParaPhraseVar) ? tool.getParaphrase (false) : value;
			
			vars.put (varName, value);
			
			tool.saveVariable (varName, value);
			
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		tool.printer ().content ("__PS__ GREEN:" + varName, String.valueOf (vars.get (varName)));
		
		return null;
	}

}
