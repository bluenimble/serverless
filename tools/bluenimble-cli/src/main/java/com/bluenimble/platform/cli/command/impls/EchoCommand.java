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
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.json.JsonParser;

public class EchoCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;

	interface Prefix {
		String Json = "j\\";
		String Date = "d\\";
		String Time = "t\\";
	}
	
	interface EchoStatus {
		String Off 	= "off";
		String On 	= "on";
	}
	
	public EchoCommand () {
		super ("echo", "print an expression to output or to a variable if the pipe character >> is present at the end of the command");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String str = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (str)) {
			return null;
		}
		
		if (EchoStatus.On.equals (str.toLowerCase ())) {
			tool.printer ().on ();
			return null;
		} else if (EchoStatus.Off.equals (str.toLowerCase ())) {
			tool.printer ().off ();
			return null;
		}
		
		Object value = str;
		
		try {
			if (str.startsWith (Prefix.Json)) {
				value = JsonParser.parse (str.substring (2));
			} else if (str.startsWith (Prefix.Date)) {
				value = Lang.toDate (str.substring (2), Lang.DEFAULT_DATE_FORMAT);
			} else if (str.startsWith (Prefix.Time)) {
				value = Lang.toDate (str.substring (2), Lang.UTC_DATE_FORMAT);
			} 
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		return new DefaultCommandResult (CommandResult.OK, value);
		
	}

}
