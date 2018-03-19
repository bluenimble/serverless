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

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;

public class WGetCommand extends AbstractCommand {

	private static final long serialVersionUID = 1852287917060945550L;
	
	public WGetCommand () {
		super ("wget", I18nProvider.get (I18N_COMMANDS + "get.desc"), "!u+[url]+,v+[variable to use to store the content],f+[target file]");
	}

	@SuppressWarnings("unchecked")
	@Override
	public CommandResult execute (Tool tool, Map<String, CommandOption> options)
			throws CommandExecutionException {
		
		String sUrl = (String)options.get ("u").getArg (0);
		
		CommandOption varOpt = options.get ("v");
		CommandOption fileOpt = options.get ("f");
		
		InputStream in = null;
		Writer writer = null;
		
		try {
			
			if (varOpt != null) {
				writer = new StringWriter ();
			} else if (fileOpt != null) {
				writer = new FileWriter (new File ((String)fileOpt.getArg (0)));
			} else {
				writer = new PrintWriter (System.out);
			}
			
			in = new URL (sUrl).openStream ();
			IOUtils.copy (in, writer);

			if (varOpt != null) {
				Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
				vars.put ((String)varOpt.getArg (0), writer.toString ());
			}
		
		} catch (Throwable th) {
			throw new CommandExecutionException (th.getMessage (), th);
		} finally {
			IOUtils.closeQuietly (writer);
			IOUtils.closeQuietly (in);
		}
		
		return new DefaultCommandResult (CommandResult.OK, "Command Executed with success");
		
	}

}
