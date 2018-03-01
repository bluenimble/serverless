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

public class SimpleParser {

	public static void main (String [] args) throws CommandParsingError {
		String cmdLine = "-k teta -p beta";
		
		AbstractCommand command = new AbstractCommand ("use", "desc", "k+,p+") {
			private static final long serialVersionUID = 0L;
			@Override
			public CommandResult execute (Tool tool,
					Map<String, CommandOption> options)
					throws CommandExecutionException {
				
				return null;
			}
		};
		
		CommandParser parser = new CommandParserImpl ();
		Iterator<CommandOption> optionsIter = 
			parser.parse (command, cmdLine).values ().iterator ();
		
		while (optionsIter.hasNext ()) {
			System.out.println (optionsIter.next ());
		}
		
	}
	
}
