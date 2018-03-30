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
package com.bluenimble.platform.icli.mgm.commands.dev;

import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.impls.AbstractCommand;
import com.bluenimble.platform.cli.command.impls.DefaultCommandResult;
import com.bluenimble.platform.cli.printing.Printer.Colors;
import com.bluenimble.platform.cli.printing.Printer.PrintSpec;
import com.bluenimble.platform.icli.mgm.BlueNimble;
import com.bluenimble.platform.icli.mgm.Keys;

public class KeysCommand extends AbstractCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	protected KeysCommand (String name, String description) {
		super (name, description);
	}

	public KeysCommand () {
		super ("keys", "list or reload keys. Ex. keys (print out all secrets). keys reload (reload all keys and print them out)");
	}

	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String cmd = (String)tool.currentContext ().get (ToolContext.CommandLine);

		try {
			if ("reload".equals (cmd)) {
				BlueNimble.loadKeys (tool);
			}
			Map<String, Keys> secrets = BlueNimble.allKeys ();
			if (secrets == null || secrets.isEmpty ()) {
				tool.printer ().info ("No keys found!"); 
				return null;
			}
			
			Iterator<String> secIter = secrets.keySet ().iterator ();
			while (secIter.hasNext ()) {
				String sname = secIter.next ();
				Keys s = secrets.get (sname);
				
				boolean current = BlueNimble.keys () != null && sname.equals (BlueNimble.keys ().alias ());
				
				tool.printer ().content (
					//  __PS__YELLOW:(C) _|_keyAlias	
					current ? PrintSpec.Start + Colors.Yellow + PrintSpec.TextSep + "(C) " + PrintSpec.Split + sname : sname, 
					(Lang.isNullOrEmpty (s.name ()) ? Lang.BLANK :       "      Name | " + s.name () + Lang.ENDLN) + 
					(Lang.isNullOrEmpty (s.domain ()) ? Lang.BLANK :     "  Endpoint | " + s.domain () + Lang.ENDLN) + 
					(Lang.isNullOrEmpty (s.issuer ()) ? Lang.BLANK :     "    Issuer | " + s.issuer () + Lang.ENDLN) +
					(Lang.isNullOrEmpty (s.whenIssued ()) ? Lang.BLANK : "    Issued | " + s.whenIssued () + Lang.ENDLN) + 
					(Lang.isNullOrEmpty (s.expiresOn ()) ? Lang.BLANK :  "   Expires | " + s.expiresOn () + Lang.ENDLN) + 
					(Lang.isNullOrEmpty (s.stamp ()) ? Lang.BLANK :  	 "     Stamp | " + s.stamp () + Lang.ENDLN) + 
					                                                     "Access Key | " + s.accessKey ()
				);
			}
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
		
		return new DefaultCommandResult (CommandResult.OK, null);
	}
}
