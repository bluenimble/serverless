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
package com.bluenimble.platform.cli.command;

import java.io.Serializable;

public interface CommandOption extends Serializable {
	
	String CMD_LINE 		= "CmdLine";
	
	int 					NO_ARG = 0;
	int 					ONE_ARG = 1;
	int 					MULTI_ARG = 2;
	
	String 					name ();
	void					setLabel (String label);
	String 					label ();
	int 					getArgsCount ();
	Object 					getArg (int index);
	void 					addArg (Object arg);
	boolean					isRequired ();
	int						acceptsArgs ();
	boolean					isMasked ();
	CommandOptionCast		cast ();
	void					setCast (CommandOptionCast cast);
	CommandOption			clone (); 
}
