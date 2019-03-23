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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.command.CommandHandler;
import com.bluenimble.platform.cli.command.CommandOption;

public abstract class AbstractCommand implements Command {
	
	private static final long serialVersionUID = -9158788406214378536L;

	public static final String I18N_COMMANDS = "commands.";
	
	private static final String S2 = "  ";

	private String formatedDesc = null;

	protected String name;
	protected String synonym;
	
	protected String description;
	protected String manual;
	
	protected boolean _private;
	
	protected Map<String, CommandOption> options;
	
	protected String context;
	
	protected AbstractCommand () {
		context = Tool.ROOT_CTX;
	}
	
	protected AbstractCommand (String name, String description) {
		this (name, description, (Map<String, CommandOption>)null);
	}
	
	protected AbstractCommand (String name, String description, String optionsSpec) {
		this (name, description, (Map<String, CommandOption>)null);
		makeOptions (optionsSpec);
	}
	
	protected AbstractCommand (String name, String description, Map<String, CommandOption> options) {
		this.name = name;
		this.description = description;
		this.options = options;
		context = Tool.ROOT_CTX;
	}
	
	@Override
	public String getName () {
		return name;
	}

	@Override
	public String getSynonym () {
		return synonym;
	}

	@Override
	public String describe () {
		if (formatedDesc != null) {
			return formatedDesc;
		}
		
		boolean prefixed = PrefixedCommand.class.isAssignableFrom (this.getClass ());
				
		int rem = 30 - name.length ();
		StringBuilder sb = new StringBuilder ("[").append (name.toLowerCase ()).append ("]");
		if (!Lang.isNullOrEmpty (synonym)) {
			sb.append (" ").append (" or ").append ("[").append (synonym.toLowerCase ()).append ("]");
			rem = rem - synonym.length ();
		}
		if (!prefixed) {
			for (int i = 0; i < rem; i++) {
				sb.append (" ");
			}
			sb.append (": ");
			sb.append (description);
		}
		// add options
		if (options != null && !options.isEmpty ()) {
			sb.append ("\n");
			Iterator<CommandOption> optionsIter = options.values ().iterator ();
			while (optionsIter.hasNext ()) {
				CommandOption option = optionsIter.next ();
				String acceptsArg = "No Arg";
				if (option.acceptsArgs () == CommandOption.ONE_ARG) {
					acceptsArg = "One Arg";
				} else if (option.acceptsArgs () == CommandOption.MULTI_ARG) {
					acceptsArg = "Multi Args";
				}
				String required = "Required";
				if (!option.isRequired ()) {
					required = "Optional";
				}
				sb.append ("        ").append ("-").append (option.name ()).append (" ");
				if (option.label () != null) {
					sb.append ("'").append (option.label ()).append ("'");
				}
				sb.append (" ( ")
					.append (required).append (", ")
					.append (acceptsArg);
				if (option.cast () != null) {
					sb.append (", ").append (option.cast ());
				}
				sb.append (" )").append ("\n");
			}
		}
		if (PrefixedCommand.class.isAssignableFrom (this.getClass ())) {
			PrefixedCommand pCommand = (PrefixedCommand)this;
			Map<String, CommandHandler> handlers = pCommand.handlers ();
			if (handlers != null && !handlers.isEmpty ()) {
				sb.append (Lang.ENDLN).append (Lang.ENDLN);
				for (CommandHandler ch : handlers.values ()) {
					String chName = S2 + Lang.APOS + name + Lang.SPACE + ch.getName () + Lang.APOS; 
					rem = 32 - chName.length ();
					sb.append (chName);
					for (int i = 0; i < rem; i++) {
						sb.append (" ");
					}
					sb.append (Lang.COLON).append (Lang.SPACE).append (ch.getDescription ()).append (Lang.ENDLN).append (Lang.ENDLN);
				}
			}
		}
		formatedDesc = sb.toString ();
		sb.setLength (0);
		return formatedDesc;
	}
	
	// !s++
	public void makeOptions (String sOpts) {
		if (sOpts == null || sOpts.trim ().isEmpty ()) {
			return;
		}
		String [] aOpts = sOpts.split (",");
		if (aOpts.length <= 0) {
			return;
		}
		if (options == null) {
			options = new LinkedHashMap<String, CommandOption> ();
		} else {
			options.clear ();
		}
		for (String sOpt : aOpts) {
			sOpt = sOpt.trim ();
			if (sOpt == null || sOpt.isEmpty ()) {
				continue;
			}
			boolean required = sOpt.startsWith ("!");
			if (required) {
				sOpt = sOpt.substring (1);
			}
			if (sOpt.isEmpty ()) {
				continue;
			}
			
			int acceptsArgs = CommandOption.NO_ARG;
			
			int indexOfStartBracket = sOpt.lastIndexOf ('[');
			int indexOfEndBracket   = sOpt.lastIndexOf (']');
			
			String optName = sOpt;
			
			int indexOfArg = sOpt.indexOf ('+');
			if (indexOfArg > 0) {
				optName = sOpt.substring (0, indexOfArg);
				optName = optName.trim ();
				acceptsArgs = CommandOption.ONE_ARG;
				if (sOpt.indexOf ("++") > 0) {
					acceptsArgs = CommandOption.MULTI_ARG;
				}
			} else {
				if (indexOfStartBracket >= 0 && indexOfEndBracket > 0) {
					optName = sOpt.substring (0, indexOfStartBracket);
				} else if (optName.endsWith ("?")) {
					optName = optName.substring (0, optName.length () - 1);
				}
			}
			
			String label = null;
			if (indexOfStartBracket >= 0 && indexOfEndBracket > 0) {
				label = sOpt.substring (indexOfStartBracket + 1, indexOfEndBracket);
			}
			
			CommandOptionImpl opt = new CommandOptionImpl (optName, acceptsArgs, required);
			opt.setLabel (label);
			opt.setMasked (sOpt.endsWith ("?")); 
			options.put (opt.name (), opt);
		}
		
	}
	
	@Override
	public Map<String, CommandOption> getOptions () {
		return options;
	}
	
	@Override
	public boolean isSingleton (Tool tool) {
		return true;
	}

	@Override
	public String getContext () {
		return context;
	}

	@Override
	public boolean forContext (ToolContext ctx) {
		return Tool.ROOT_CTX.equals (context) || ctx.getAlias ().equals (context);
	}

	@Override
	public void onShutdown (Tool tool) {
		// Nothing
	}
	@Override
	public void onStartup (Tool tool) {
		// Nothing
	}

	@Override
	public String manual() {
		return manual;
	}
	
	@Override
	public boolean isPrivate () {
		return _private;
	}
	
	public void setManual (String manual) {
		this.manual = manual;
	}
	
	public void setContext (String context) {
		this.context = context;
	}
	
	public void setDescription (String description) {
		if (!Lang.isNullOrEmpty (description)) {
			this.description = description;
		}
		if (Lang.isNullOrEmpty (this.description)) {
			this.description = Lang.BLANK;
		}
	}
	
	public void setPrivate (boolean _private) {
		this._private = _private;
	}
	
}
