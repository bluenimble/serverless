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

import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.json.JsonObject;

public class VarsCommand extends AbstractCommand {

	private static final long serialVersionUID = 1852287917060945550L;
	
	public VarsCommand () {
		super ("vars", I18nProvider.get (I18N_COMMANDS + "vars.desc"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {

		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		String varName = (String)tool.currentContext ().get (ToolContext.CommandLine);
		
		Object result = "";
		
		if (!Lang.isNullOrEmpty (varName)) {
			Object value = vars.get (varName);
			if (value != null) {
				if (value instanceof JsonObject) {
					result = value;
				} else {
					result = varName + Lang.EQUALS + value;
				}
			} else {
				result = "variable " + varName + " not found";
			}
		} else {
			if (vars != null && !vars.isEmpty ()) {
				StringBuilder sb = new StringBuilder ();
				
				Iterator<String> names = vars.keySet ().iterator ();
				while (names.hasNext ()) {
					String name = names.next ();
					sb.append (name).append (Lang.EQUALS).append (vars.get (name)).append (Lang.ENDLN);
				}
				
				result = sb.toString ();
				
				sb.setLength (0);
				
			}
		}

		return new DefaultCommandResult (CommandResult.OK, result);
	}

}
