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

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.CommandResult;

public interface CommandHandler extends Serializable {
	
	interface Arg {
		enum Type {
			String,
			Integer,
			Variable
		}
		String 	name 		();
		String 	desc 		();
		Type 	type 		();
		boolean required 	();
	}
	
	abstract class AbstractArg implements Arg {
		public Type type () {
			return Type.String;
		}
		public boolean required () {
			return true;
		}
		public String desc () {
			return null;
		}
	}
	
	String 	getName 		();
	String 	getDescription 	();
	
	Arg []	getArgs 		();

	CommandResult execute (Tool tool, String... args) throws CommandExecutionException;
	
}
