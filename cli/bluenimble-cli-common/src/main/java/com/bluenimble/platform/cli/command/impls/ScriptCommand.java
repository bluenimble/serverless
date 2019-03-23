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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;

public class ScriptCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;

	protected ScriptCommand (String name, String description) {
		super (name, description);
	}

	public ScriptCommand () {
		super ("script", I18nProvider.get (I18N_COMMANDS + "script.desc"), "f+[script file],v+[variable holding a script],sm[safe mode]");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		CommandOption f = options.get ("f");
		CommandOption v = options.get ("v");
		
		if (f == null && v == null) {
			throw new CommandExecutionException ("One of the options 'v' or 'f' must be set");
		}
		
		String name = null;
		
		BufferedReader reader = null;
		
		if (f != null) {
			name = (String)f.getArg (0);
			
			if (name.startsWith (Lang.TILDE + File.separator)) {
				name = System.getProperty ("user.home") + name.substring (1);
			}
			
			File file = new File (name);
			
			if (!file.exists ()) {
				throw new CommandExecutionException ("File " + name + " not found");
			}
			
			if (!file.isFile ()) {
				throw new CommandExecutionException (name + " is not a valid file");
			}
			try {
				reader = new BufferedReader(new InputStreamReader (new FileInputStream (file)));
			} catch (FileNotFoundException e) {
				throw new CommandExecutionException (e.getMessage (), e);
			}
		} else {
			name = (String)v.getArg (0);
			Object value = tool.currentContext ().get (name.trim().toLowerCase ());
			if (value == null) {
				return null;
			}
			StringBuilder script = new StringBuilder ("");
			if (value instanceof String []) {
				String [] commands = (String [])value;
				for (String c : commands) {
					if (c != null) {
						script.append (c).append ("\n");
					}
				}
			} else if (Iterable.class.isAssignableFrom (value.getClass ())) {
				Iterable<?> commands = (Iterable<?>)value;
				for (Object c : commands) {
					if (c != null) {
						script.append (c).append ("\n");
					}
				}
			} else {
				script.append (value);
			}
			String s = script.toString ().trim ();
			script.setLength (0);
			if (s.isEmpty ()) {
				return null;
			}
			reader = new BufferedReader (new StringReader (s));
		}

		/*
		if (reader == null) {
			return new DefaultCommandResult (CommandResult.OK, "WARN: Empty Script");
		}*/

		CommandOption sm = options.get ("sm");
		
		if (sm != null) {
			tool.writeln ("Running script [" + name + "] using safe mode");
		}
		
		long start = System.currentTimeMillis ();
		
		try {
			CommandResult result = runCommands (name, reader, tool, sm != null);
			if (result != null) {
				return result;
			}
		} finally {
			try {
				reader.close ();
			} catch (IOException ioex) {
				// IGNORE
			}
		}
		
		long end = System.currentTimeMillis ();
		
		return new DefaultCommandResult (CommandResult.OK, "\n'" + name + "' executed with success. Time ( " + ((end-start)/1000) + " seconds)");
	}
	
	private CommandResult runCommands (String name, BufferedReader reader, Tool tool, boolean safeMode) throws CommandExecutionException {
		try {
			int res;
			String s;
			while ((s = reader.readLine ()) != null) {
				if (Lang.isNullOrEmpty (s)) {
					continue;
				}
				if (s.trim ().startsWith ("#")) {
					continue;
				}
				res = tool.processCommand (s, false);
				if (res == Tool.FAILURE && safeMode) {
					return new DefaultCommandResult (CommandResult.KO, "'" + name + "' Script is stopped due to errors");
				}
			}
		} catch (IOException e) {
			throw new CommandExecutionException (e.getMessage (), e);
		} finally {
			try {
				reader.close ();
			} catch (IOException ioex) {
				// IGNORE
			}
		}
		return null;
	}

}
