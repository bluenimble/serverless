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
import java.util.Set;


import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.printing.impls.FriendlyJsonEmitter;
import com.bluenimble.platform.json.JsonObject;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class VarsCommand extends AbstractCommand {

	private static final long serialVersionUID = 1852287917060945550L;
	
	private static final String Line = "      +----------------------------------+-------------------------------------------+";
	
	public VarsCommand () {
		super ("vars", I18nProvider.get (I18N_COMMANDS + "vars.desc"));
		synonym = "var";
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {

		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		
		String varName = (String)tool.currentContext ().get (ToolContext.CommandLine);
		
		if (Lang.isNullOrEmpty (varName)) {
			// print vars table
			Set<String> keys = vars.keySet ();
			for (String key : keys) {
				tool.writeln (Line);
				tool.write ("      | " + pad (key, 32));
				Object value = vars.get (key);
				String sValue = null;
				if (value instanceof JsonObject) {
					sValue = ((JsonObject)value).toString (0);
				} else {
					sValue = String.valueOf (value);
				}
				tool.writeln (" | " + tool.printer ().getFontPrinter ().generate (pad (sValue, 42), Attribute.LIGHT, FColor.YELLOW, null) + "|");
			}
			tool.writeln (Line);
			return null;
		}	
		
		Object value = vars.get (varName);
		
		if (value == null) {
			tool.printer ().content ("__PS__ YELLOW:" + varName, tool.printer ().getFontPrinter ().generate ("Not found!", Attribute.LIGHT, FColor.RED, null));
			return null;
		}
		
		if (value instanceof JsonObject) {
			tool.printer ().content ("__PS__ GREEN:" + varName, null);
			if (tool.printer ().isOn ()) {
				((JsonObject)value).write (new FriendlyJsonEmitter (tool));
				tool.writeln (Lang.BLANK);
			}
		} else {
			tool.printer ().content ("__PS__ GREEN:" + varName, String.valueOf (value));
		}

		return null;
	}
	
	private String pad (String str, int length) {
		str = Lang.replace (str, Lang.ENDLN, Lang.BLANK);
		
		int rem = length - str.length ();
		if (rem <= 0) {
			return str.substring (0, length - 4) + " ...";
		}
		char [] spaces = new char [rem];
		for (int i = 0; i < rem; i++) {
			spaces [i] = ' ';
		}
		return str + new String (spaces);
	}

}
