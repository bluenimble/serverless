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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.json.JsonArray;

public class SplitCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;

	public SplitCommand () {
		super ("split", "split a string using the default separator (see sep.split variable). default to space (' ')");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String str = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (Lang.isNullOrEmpty (str)) {
			return null;
		}
		
		JsonArray arr = new JsonArray ();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher (str);
		while (m.find ()) {
			String s = m.group (1);
		    if (s.startsWith (Lang.QUOTE)) {
		    	s = s.substring (1);
		    }
		    if (s.endsWith (Lang.QUOTE)) {
		    	s = s.substring (0, s.length () - 1);
		    }
		    arr.add (s); 
		}
		
		return new DefaultCommandResult (CommandResult.OK, arr);
		
	}

}
