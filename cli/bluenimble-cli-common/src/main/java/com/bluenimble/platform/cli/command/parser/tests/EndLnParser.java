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
package com.bluenimble.platform.cli.command.parser.tests;

import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.parser.CommandParser;
import com.bluenimble.platform.cli.command.parser.CommandParsingError;
import com.bluenimble.platform.cli.command.parser.impls.CommandParserImpl;

public class EndLnParser {

	public static void main (String [] args) throws CommandParsingError {
		String cmdLine = "  -s   -f   argF -a rg2  -q select * \n from \n where 1=1";
		
		AbstractCommand command = new AbstractCommand ("abc", "desc", "s,f++,!q+") {
			private static final long serialVersionUID = 0L;
			@Override
			public CommandResult execute (Tool tool,
					Map<String, CommandOption> options)
					throws CommandExecutionException {
				
				return null;
			}
		};
		
		CommandParser wrapper = new CommandParserImpl ();
		Map<String, CommandOption> res = wrapper.parse (command, cmdLine);
		
		Iterator<CommandOption> optionsIter = res.values ().iterator ();
		while (optionsIter.hasNext ()) {
			System.out.print (optionsIter.next ());
		}
		
	}
	
}
