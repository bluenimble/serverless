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
package com.bluenimble.platform.icli.mgm.commands.mgm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.Keys;
import com.bluenimble.platform.icli.mgm.utils.JsTool;

public class MacroSourceCommand extends AbstractCommand {

	private static final long serialVersionUID = 3523915768661531476L;

	private static final String JavaClass 	= "JavaClass";

	private static final String Native 		= "var native = function (className) { return JavaClass (className.split ('/').join ('.')).static; };";

    private static final ScriptEngine Engine = new ScriptEngineManager ().getEngineByName ("JavaScript");

    private File script;
	
	public MacroSourceCommand (String context, String name, File script) {
		this.context 	= context;
		this.name 		= name;
		this.script 	= script;
	}

	@Override
	public CommandResult execute (final Tool tool, Map<String, CommandOption> options) throws CommandExecutionException {

		InputStream input = null;
		
		SimpleBindings bindings = new SimpleBindings ();
		
		String command = (String)tool.currentContext ().get (ToolContext.CommandLine);
		if (!Lang.isNullOrEmpty (command)) {
			bindings.put ("Command", command);
		}
		
		bindings.put ("Home", BlueNimble.Work);
		
		bindings.put ("Config", BlueNimble.Config);
		
		bindings.put ("Tool", new JsTool (tool));
		
		bindings.put (JavaClass, new Function<String, Class<?>> () {
			@Override
			public Class<?> apply (String type) {
				try {
					return MacroSourceCommand.class.getClassLoader ().loadClass (type);
				} catch (ClassNotFoundException cnfe) {
					throw new RuntimeException(cnfe);
				}
			}
		});
		
		bindings.put ("Vars", tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS));
		
		Keys keys = BlueNimble.keys ();
		if (keys != null) {
			bindings.put ("Keys", keys.json ());
		}
		
		try {
			
			input = new FileInputStream (script);
			
			List<InputStream> blocks = new ArrayList<InputStream> ();
			blocks.add (new ByteArrayInputStream (Native.getBytes ()));
			blocks.add (input);
			
			Engine.eval (new InputStreamReader (new SequenceInputStream (Collections.enumeration (blocks))), bindings);
			
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		} finally {
			IOUtils.closeQuietly (input);
		}
        
		return null;
	}

}
