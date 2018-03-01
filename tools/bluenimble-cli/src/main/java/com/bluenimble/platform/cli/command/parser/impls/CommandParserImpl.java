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
package com.bluenimble.platform.cli.command.parser.impls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandOptionCast;
import com.bluenimble.platform.cli.command.CommandUtils;
import com.bluenimble.platform.cli.command.impls.CommandOptionImpl;
import com.bluenimble.platform.cli.command.parser.CommandParser;
import com.bluenimble.platform.cli.command.parser.CommandParsingError;
import com.bluenimble.platform.cli.command.parser.converters.ArgumentValueCastException;
import com.bluenimble.platform.cli.command.parser.converters.BooleanValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.DoubleValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.FileValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.FolderValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.IntegerValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.OptionArgumentValueConverter;
import com.bluenimble.platform.cli.command.parser.converters.ZipValueConverter;

public class CommandParserImpl implements CommandParser {

	private static final long serialVersionUID = -8023242088096073837L;
	
	private Map<CommandOptionCast, OptionArgumentValueConverter> converters = new HashMap<CommandOptionCast, OptionArgumentValueConverter> ();
	
	public CommandParserImpl () {
		converters.put (CommandOptionCast.Integer, new IntegerValueConverter ());
		converters.put (CommandOptionCast.Double, new DoubleValueConverter ());
		converters.put (CommandOptionCast.Boolean, new BooleanValueConverter ());
		converters.put (CommandOptionCast.File, new FileValueConverter ());
		converters.put (CommandOptionCast.Folder, new FolderValueConverter ());
		converters.put (CommandOptionCast.Zip, new ZipValueConverter ());
	}

	@Override
	public Map<String, CommandOption> parse (Command command, String cmdLine) throws CommandParsingError {
		Map<String, CommandOption> options = command.getOptions ();
		if (cmdLine == null || cmdLine.trim ().isEmpty ()) {
			if (CommandUtils.hasRequiredOptions (options)) {
				throw new CommandParsingError ("Missing required options.\t\n" + command.describe ());
			}
			return null;
		}
		cmdLine = cmdLine.trim ();
		
		CommandOption cmdLineOpt = new CommandOptionImpl (CommandOption.CMD_LINE);
		cmdLineOpt.addArg (cmdLine.trim ());

		Map<String, CommandOption> result = new HashMap<String, CommandOption> ();
		result.put (CommandOption.CMD_LINE, cmdLineOpt);

		if (options == null || options.isEmpty ()) {
			return result;
		}
		
		Iterator<CommandOption> optionsIter = options.values ().iterator ();
		CommandOption option;
		while (optionsIter.hasNext ()) {
			option = optionsIter.next ();
			int indexOfOpt = cmdLine.indexOf (Lang.DASH + option.name ());
			if (indexOfOpt < 0) {
				if (option.isRequired ()) {
					throw new CommandParsingError ("missing option: '" + option.label () + "'");
				} 
				continue;
			}
			
			// fix with space before dash
			if (indexOfOpt > 0 && cmdLine.charAt (indexOfOpt - 1) != ' ') {
				continue;
			}
			
			CommandOption opt = null;
			result.put (option.name (), opt = option.clone ());
			readOption (cmdLine, opt, indexOfOpt, options);
		}
		return result;
	}

	private void readOption (String command, CommandOption option, int indexOfOpt, Map<String, CommandOption> options) 
			throws CommandParsingError {
		String optionArgs = null;
		int nextOptionIndex = command.indexOf (Lang.DASH, indexOfOpt + 1);
		while (true) {
			if (nextOptionIndex < 0) {
				optionArgs = command.substring (indexOfOpt);
				break;
			} else {
				if (isValidOption (command, nextOptionIndex, options)) {			
					optionArgs = command.substring (indexOfOpt, nextOptionIndex);
					break;
				} else {
					nextOptionIndex = command.indexOf (Lang.DASH, nextOptionIndex + 1);
				}
			}
		}
		setOptionArgs (option, optionArgs);
	}

	private boolean isValidOption (String command, int nextOptionIndex, Map<String, CommandOption> options) 
			throws CommandParsingError {
		int indexOfSpace = command.indexOf (Lang.SPACE, nextOptionIndex);
		String nextOption = null;
		if (indexOfSpace > 0) {
			nextOption = command.substring (nextOptionIndex + 1, indexOfSpace);
		} else {
			nextOption = command.substring (nextOptionIndex + 1);
		}
		if (nextOption != null && options.containsKey (nextOption)) {
			return true;
		}
		return false;
	}

	private void setOptionArgs (CommandOption option, String optionText) throws CommandParsingError {
		String args = optionText.substring (option.name ().length () + 1);
		if (!isNullOrEmpty (args)) {
			if (option.acceptsArgs () == CommandOption.NO_ARG) {
				throw new CommandParsingError ("'" + option.label () + "' doesn't accept arguments.");
			}
			args = args.trim ();
			if (option.acceptsArgs () == CommandOption.ONE_ARG) {
				try {
					option.addArg (castValue (option, args));
				} catch (ArgumentValueCastException e) {
					throw new CommandParsingError (e.getMessage ());
				}
			} else {
				StringTokenizer tokenizer = new StringTokenizer (args);
				while (tokenizer.hasMoreTokens ()) {
					String arg = tokenizer.nextToken ();
					if (!isNullOrEmpty (arg)) {
						try {
							option.addArg (castValue (option, arg.trim ()));
						} catch (ArgumentValueCastException e) {
							throw new CommandParsingError (e.getMessage ());
						}
					}
				}
			}
		} else if (option.acceptsArgs () != CommandOption.NO_ARG && !option.isMasked ()) {
			throw new CommandParsingError ("'" + option.label () + "' requires at least one argument.");
		}
	}
	
	private boolean isNullOrEmpty (String str) {
		return str == null || str.trim ().isEmpty ();
	}
	
	private Object castValue (CommandOption option, String arg) throws ArgumentValueCastException {
		if (option.cast () == null) {
			return arg;
		}
		OptionArgumentValueConverter converter = converters.get (option.cast ());
		if (converter == null) {
			return arg;
		}
		return converter.cast (option.name (), arg);
	}
	
}
