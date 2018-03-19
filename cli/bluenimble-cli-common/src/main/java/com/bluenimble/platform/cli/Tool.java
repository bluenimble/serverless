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
package com.bluenimble.platform.cli;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.bluenimble.platform.cli.command.Command;
import com.bluenimble.platform.cli.printing.Printer;

public interface Tool extends Serializable {
	
	String				ROOT_CTX 		= ".";
	File 				Home 			= new File (System.getProperty ("user.home"));
	
	String 				ParaPhraseVar 	= "paraphrase";
	
	
	int 				FAILURE   		= 0;
	int 				SUCCESS 		= 1;
	int 				UNTERMINATED 	= 2;
	int 				MULTIPLE 		= 3;
	
	void 				startup (String [] args) throws ToolStartupException;
	void 				shutdown ();

	String 				getName ();
	String 				getDescription ();

	String 				getDefaultDelimiter ();

	Iterable<Command> 	getCommands ();
	Command 			getCommand (String commandName);
	void    			addCommand (Command command);
	
	String				getCommandForSynonym (String synonym);
	
	ToolContext 		getContext (String ctxName);
	void        		addContext (ToolContext ctx);
	
	int					processCommand (String cmdLine) throws IOException;
	
	void 				usage (boolean all);
	List<String>		getHistory ();
	
	void				changeContext (ToolContext ctx);
	ToolContext 		currentContext ();
	
	String 				readLine () throws IOException;
	Tool 				write (String str);
	Tool 				writeln (String str);
	
	Date 				getStartTime ();
	
	String				getManual (String name);
	
	boolean				isTestMode ();
	
	boolean 			isAllowed ();
	
	void 				onReady ();
	
	ToolClient			getClient 		();
	
	Printer				printer 		();
	
	void				saveVariable 	(String name, Object value) throws IOException;
	
	void				setParaphrase 	(String paraphrase, boolean encrypt) throws Exception;
	String				getParaphrase 	(boolean decrypt) throws Exception;
	
}
