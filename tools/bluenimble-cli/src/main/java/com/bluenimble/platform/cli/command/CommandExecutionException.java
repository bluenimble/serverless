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

public class CommandExecutionException extends Exception {

	private static final long serialVersionUID = 445615871551158801L;
	
	private boolean showUsage;

	public CommandExecutionException () {
		super ();
	}

	public CommandExecutionException (String message) {
		super (message);
	}

	public CommandExecutionException (Throwable th) {
		super (th);
	}

	public CommandExecutionException (String message, Throwable th) {
		super (message, th);
	}
	
	public CommandExecutionException showUsage () {
		showUsage = true;
		return this;
	}

	public boolean shouldShowUsage () {
		return showUsage;
	}

}
