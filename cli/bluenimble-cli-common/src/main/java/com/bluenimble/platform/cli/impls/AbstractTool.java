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
package com.bluenimble.platform.cli.impls;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bluenimble.platform.Crypto;
import com.bluenimble.platform.Crypto.Algorithm;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.I18nProvider;
import com.bluenimble.platform.cli.InstallI18nException;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.ToolStartupException;
import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandResult;
import com.bluenimble.platform.cli.command.parser.CommandParser;
import com.bluenimble.platform.cli.command.parser.impls.CommandParserImpl;
import com.bluenimble.platform.cli.printing.Printer;
import com.bluenimble.platform.cli.printing.impls.FriendlyJsonEmitter;
import com.bluenimble.platform.cli.printing.impls.PrettyPrinter;
import com.bluenimble.platform.encoding.Base64;
import com.bluenimble.platform.json.JsonObject;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public abstract class AbstractTool implements Tool {
	
	private static final long serialVersionUID = 641095159517307685L;
	
	private static final String Token					= "a35T@,#;_%zID=!1";
	
	public static final String COMMAND_NOT_FOUND 		= "Error: command not found";
	private static final String DEFAULT_DELIMETER 		= "\n";
	
	private static final String ExpStart 				= "${";
	private static final String ExpEnd 					= "}";
	
	private static final String OUT_TOKEN 				= ">>";
	private static final String COMMAND_CHAINING_TOKEN 	= "&&";
	
	
	private static final String FilePrefix 				= "file:";
	
	public static final String CMD_OUT 					= "cmd.out";
	public static final String CMD_OUT_FILE 			= "cmd.out.file";
	
	private static final ToolContext ROOT_CONTEXT = new ToolContextImpl (ROOT_CTX);
	
	private RuningCommand runingCommand;
		
	private CommandParser commandParser = new CommandParserImpl ();
	private List<String> history = new ArrayList<String> (); 
	protected ToolContext currentContext;
	private Date startTime;
	private boolean testMode;
	
	protected Printer printer;
	
	private String paraphrase;
	
	public AbstractTool () throws InstallI18nException {
		I18nProvider.install ("com/bluenimble/platform/cli/impls/labels", false);
		addContext (ROOT_CONTEXT);
		currentContext = ROOT_CONTEXT;

		printer = new PrettyPrinter (this, 4);

		runingCommand = new RuningCommand ();
	}

	@Override
	public void startup (String [] args) throws ToolStartupException {
		for (Command command : getCommands ()) {
			command.onStartup (this);
		}
		
		startTime = new Date ();
		
		writeln (Lang.BLANK);
		printer.textLn (0, getDescription (), FColor.CYAN.name (), null);
		writeln (Lang.BLANK);
		writeln (Lang.BLANK);
		printer.textLn (0, startTime.toString (), FColor.YELLOW.name (), null);

		try {
			prompt ();

			onReady ();
		
			prompt ();
		} catch (Throwable th) {
			// IGNORE
		}

		addShutdownHook ();

		// signle run 
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				try {
					processCommand (args [i], true);
				} catch (IOException e) {
					throw new ToolStartupException (e);
				}
			}
			try {
				processCommand ("quit", true);
			} catch (IOException e) {
				throw new ToolStartupException (e);
			}
			return;
		}
		
	}
	
	@Override
	public void shutdown () {
		for (Command command : getCommands ()) {
			command.onShutdown (this);
		}
		writeln ("Goodbye !");
	}

	@Override
	public Printer printer () {
		return printer;
	}

	@Override
	public void saveVariable (String name, Object value) throws IOException {
	}

	/**
     * Add shutdown hook.
     */
    protected void addShutdownHook () {
        Runnable shutdownHook = new Runnable() {
            public void run() {
            	shutdown ();
            }
        };
        // add shutdown hook
        Runtime.getRuntime ().addShutdownHook (new Thread (shutdownHook));
    }

	@Override
	public void usage (boolean all) {
		Iterable<Command> commands = getCommands ();
		for (Command command : commands) {
			if (all) {
				if (command.forContext (currentContext)) {
					writeln (command.describe ());
				}
			} else {
				if (currentContext.getAlias ().equals (command.getContext ())) {
					writeln (command.describe ());
				}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public int processCommand (String cmdLine, boolean notIfPrivate) throws IOException {
		
		if (!isAllowed ()) {
			writeln ("not allowed to write commands.");
			return FAILURE;
		}
		
		if (cmdLine == null) {
			cmdLine = readLine ();
		}
		if (cmdLine == null || cmdLine.trim ().isEmpty ()) {
			try {
				Thread.sleep (100);
			} catch (InterruptedException e) {
			}
			return SUCCESS;
		}
		String delimeter = getDelimeter ();
		if (!delimeter.equals ("\n")) {
			runingCommand.setCmdLine (runingCommand.getCmdLine () + "\n" + cmdLine);
			if (!cmdLine.endsWith (currentContext.getDelimiter ())) {
				return UNTERMINATED;
			} else {
				cmdLine = runingCommand.getCmdLine ();
				runingCommand.setCmdLine ("");
			}
		} 
		
		if (!delimeter.equals ("\n")) {
			cmdLine = cmdLine.substring (0, cmdLine.length() - delimeter.length ());
		}
		
		String commandName;
		String params;
		String out = null;
		
		cmdLine = cmdLine.trim ();
		
		// multiple commands
		if (cmdLine.indexOf (COMMAND_CHAINING_TOKEN) > 0) {
			String [] aCommands = Lang.split (cmdLine, COMMAND_CHAINING_TOKEN, true);
			for (String cmd : aCommands) {
				processCommand (cmd, notIfPrivate);
			}
			return MULTIPLE;
		}
		
		final Map<String, Object> vars = (Map<String, Object>)getContext (Tool.ROOT_CTX).get (ToolContext.VARS);

		//Calendar now = Calendar.getInstance ();
		//vars.put ("day", now.get (Calendar.DAY_OF_MONTH));
		//vars.put ("month", now.get (Calendar.MONTH) + 1);
		//vars.put ("year", now.get (Calendar.YEAR));
		//vars.put ("hour", now.get (Calendar.HOUR));
		//vars.put ("min", now.get (Calendar.MINUTE));
		//vars.put ("sec", now.get (Calendar.SECOND));
		//vars.put ("uuid", Lang.UUID (20).toString ());

		cmdLine = Lang.resolve (cmdLine, ExpStart, ExpEnd, new Lang.VariableResolver () {
			@Override
			public String resolve (String ns, String p) {
				Object o = vars.get (ns == null ? p : ns + Lang.DOT + p);
				if (o == null) {
					return null;
				}
				return String.valueOf (o);
			}
		});
		
		int indexOfSpace = cmdLine.indexOf (' ');
		if (indexOfSpace > 0) {
			commandName = cmdLine.substring (0, indexOfSpace);
			params = cmdLine.substring (indexOfSpace + 1);
			int outIndex = params.lastIndexOf (OUT_TOKEN);
			if (outIndex >= 0) {
				out 	= 	params.substring (outIndex + OUT_TOKEN.length ());
				params 	= 	params.substring (0, outIndex);
			}
		} else {
			commandName = cmdLine;
			params = null;
		}
		if (Lang.isNullOrEmpty (out)) {
			out = null;
		} else {
			out = out.trim ();
			if (out.startsWith (FilePrefix)) {
				vars.put (CMD_OUT, CMD_OUT_FILE);
			}
		}
		
		commandName = commandName.trim ().toLowerCase ();
		
		params = onCommand (commandName, params);
		
		Command cmd = getCommand (commandName);
		if (cmd == null) {
			String synCommandName = getCommandForSynonym (commandName);
			if (synCommandName != null) {
				cmd = getCommand (synCommandName);
			}
		}
		
		if (cmd == null) {
			printer.error ("command '" + commandName + "' not found. Type in 'help' to list all available commands");
			return FAILURE;
		}
		
		if (cmd.isPrivate () && notIfPrivate) {
			printer.error ("Command [" + commandName + "] not found");
			return FAILURE;
		}
		
		if (!cmd.forContext (currentContext)) {
			printer.error ("Command [" + commandName + "] not found in current context");
			return FAILURE;
		} 
		
		history.add (cmdLine);
		
		if (!cmd.isSingleton (this)) {
			try {
				cmd = (Command)cmd.getClass ().newInstance ();
			} catch (Throwable th) {
				if (isTestMode ()) {
					printer ().error (th.getMessage (), Lang.toString (th));
				}
				onException (cmd, th); return FAILURE;
			}
		}
		
		CommandResult result = null;
		try {
			final Map<String, CommandOption> options = commandParser.parse (cmd, params);
			if (options != null && !options.isEmpty ()) {
				Iterator<CommandOption> optIter = options.values ().iterator ();
				while (optIter.hasNext ()) {
					CommandOption option = optIter.next ();
					if (option.isMasked () && 
							option.acceptsArgs() != CommandOption.NO_ARG && 
							option.getArgsCount () == 0) {
						if (System.console () != null) {
							char[] aArg = System.console ().readPassword ("\t- " + option.label () + "? ");
							if ((aArg == null || aArg.length == 0)) {
								printer.error ("'" + option.label () + "' requires at least one argument.");
							}
							option.addArg (new String (aArg));
						} else {
							printer.error ("'" + option.label () + "' requires at least one argument.");
							return FAILURE;
						}
					}
				}
			}
			if (Lang.AMP.equals (out)) {
				final Command tCmd = cmd;
				new Thread () {
					public void run () {
						try {
							tCmd.execute (AbstractTool.this, options);
					} catch (CommandExecutionException th) {
						if (isTestMode ()) {
							th.printStackTrace (System.out);
						}
						try {
							onException (tCmd, th); 
						} catch (IOException ioex) {
							// ignore
						}
					}
					}
				}.start (); 
				printer.info ("@ command is running in background ");
				return SUCCESS;
			} else {
				result = cmd.execute (this, options);
			}
		} catch (Throwable th) {
			if (isTestMode ()) {
				printer.content ("Error", Lang.toString (Lang.getRootCause (th)));
			} else {
				onException (cmd, th); 
			}
			return FAILURE;
		}
		
		if (result == null) {
			return SUCCESS;
		}
		
		Object content = result.getContent ();
		
		if (result.getContent () == null) {
			return SUCCESS;
		}
		
		if (result.getType () == CommandResult.OK) {
			if (out != null) {
				if (out.startsWith (FilePrefix)) {
					out = out.substring (FilePrefix.length ()).trim ();
					if (out.startsWith (Lang.TILDE + File.separator)) {
						out = System.getProperty ("user.home") + out.substring (1);
					}
					InputStream is = null;
					if (content instanceof ApiStreamSource) {
						is = ((ApiStreamSource)content).stream ();
					} else {
						is = new ByteArrayInputStream (content.toString ().getBytes ());
					}
					OutputStream os = null;
					try {
						os = new FileOutputStream (new File (out));
						IOUtils.copy (is, os);
					} catch (Exception e) {
						onException (cmd, e); 
						return FAILURE;
					} finally {
						IOUtils.closeQuietly (os);
						IOUtils.closeQuietly (is);
						vars.remove (CMD_OUT);
					}
				} else {
					if (content instanceof YamlObject) {
						content = ((YamlObject)content).toJson ();
					}
					vars.put (out, content);
				}
			} else {
				if (content instanceof JsonObject) {
					printer ().success (Lang.BLANK);
					if (printer ().isOn ()) {
						((JsonObject)content).write (new FriendlyJsonEmitter (this));
						writeln (Lang.BLANK);
					}
				} else if (content instanceof YamlObject) {
					printer ().success (Lang.BLANK);
					if (printer ().isOn ()) {
						YamlObject yaml = (YamlObject)content;
						yaml.print (this, 4);
						writeln (Lang.BLANK);
					}
				} else if (content instanceof ApiStreamSource) {
					printer ().success (String.valueOf (IOUtils.toString (((ApiStreamSource)content).stream ())));
					//if (printer ().isOn ()) {
					//	writeln (Lang.BLANK);
					//}
				} else {
					printer ().success (String.valueOf (content));
					//if (printer ().isOn ()) {
					//	writeln (Lang.BLANK);
					//}
				}
			}
		} else {
			if (content instanceof JsonObject) {
				printer ().error (Lang.BLANK);
				((JsonObject)content).write (new FriendlyJsonEmitter (this));
				writeln (Lang.BLANK);
			} else if (content instanceof YamlObject) {
				printer ().error (Lang.BLANK);
				if (printer ().isOn ()) {
					YamlObject yaml = (YamlObject)content;
					yaml.print (this, 4);
					writeln (Lang.BLANK);
				}
			} else {
				printer.error (String.valueOf (content));
				writeln (Lang.BLANK);
			}
		}
		
		return SUCCESS;
	}
	
	protected String onCommand (String commandName, String params) {

		ToolContext toolContext = currentContext ();
		
		toolContext.put (ToolContext.CommandName, commandName);

		if (params == null) {
			toolContext.remove (ToolContext.CommandLine);
			return params;
		}
		
		params = params.trim ();
		
		toolContext.put (ToolContext.CommandLine, params);
		return params;
	}

	private void onException (Command command, Throwable th) throws IOException {
		th = Lang.getRootCause (th);
		StackTraceElement ste = th.getStackTrace () [0];
		printer ().error (th.getMessage () == null ? (th.getClass ().getSimpleName () + Lang.SPACE + "File: " + ste.getFileName ().substring (0, ste.getFileName ().lastIndexOf (Lang.DOT)) + ", Line: " + ste.getLineNumber ()) : th.getMessage ());
		if (th instanceof CommandExecutionException) {
			CommandExecutionException ceex = (CommandExecutionException)th;
			if (ceex.shouldShowUsage ()) {
				printer ().content ("Command " + command.getName (), command.describe ());
			}
		}
	}
	
	public String getDefaultDelimiter () {
		return DEFAULT_DELIMETER;
	}
	
	private String getDelimeter () {
		if (currentContext != null && currentContext.getDelimiter () != null) {
			return currentContext.getDelimiter ();
		}
		return getDefaultDelimiter ();
	}

	public void prompt () {
		writeln (Lang.BLANK);
		printer.text (-100, getName (), FColor.CYAN.name (), null);
		write (Lang.COLON);
		if (currentContext == null) {
			printer.text (-100, Lang.DOT, FColor.GREEN.name (), null);
		} else {
			printer.text (-100, currentContext.getName (), FColor.GREEN.name (), null);
		}
		write (Lang.GREATER);
	}

	public ToolContext currentContext () {
		return currentContext;
	}

	public void changeContext (ToolContext ctx) {
		this.currentContext = ctx;
	}

	public List<String> getHistory () {
		return history;
	}

	public Date getStartTime() {
		return startTime;
	}

	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode (boolean testMode) {
		this.testMode = testMode;
	}

	@Override
	public String getParaphrase (boolean decrypt) throws Exception {
		if (Lang.isNullOrEmpty (paraphrase)) {
			return null;
		}
		return decrypt ? decrypt (paraphrase) : paraphrase;
	}

	@Override
	public void setParaphrase (String paraphrase, boolean encrypt) throws Exception {
		if (Lang.isNullOrEmpty (paraphrase)) {
			return;
		}
		this.paraphrase = encrypt ? encrypt (paraphrase) : paraphrase;
	}
	
	private String encrypt (String paraphrase) throws Exception {
		byte [] encrypted = Crypto.encrypt (paraphrase.getBytes (), Token, Algorithm.AES);
		return Base64.encodeBase64String (encrypted).trim ();		
	}

	private String decrypt (String paraphrase) throws Exception {
		byte [] encryptedDecoded = Base64.decodeBase64 (paraphrase.getBytes ());
		return pad (new String (Crypto.decrypt (encryptedDecoded, Token, Algorithm.AES)));
	}
	
	private static String pad (String paraphrase) {
		if (paraphrase.length () == 16) {
			return paraphrase;
		} else if (paraphrase.length () > 16) {
			return paraphrase.substring (0, 15);
		} else {
			StringBuilder sb = new StringBuilder (paraphrase);
			
			for (int i = 0; i < (16 - paraphrase.length ()); i++) {
				sb.append ("0");
			}
			String s = sb.toString ();
			sb.setLength (0);
			return s;
		}
		
	}

}
