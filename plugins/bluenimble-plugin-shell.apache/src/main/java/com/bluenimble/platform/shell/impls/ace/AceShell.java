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
package com.bluenimble.platform.shell.impls.ace;

import java.io.ByteArrayOutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.shell.Shell;
import com.bluenimble.platform.shell.ShellException;

public class AceShell implements Shell {

	private static final long serialVersionUID = 4303282790607692198L;

	private DefaultExecutor executor;
	
	public AceShell (DefaultExecutor executor) {
		this.executor = executor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JsonObject run (String command, JsonObject params) throws ShellException {
		CommandLine commandLine = Json.isNullOrEmpty (params) ? CommandLine.parse (command) : CommandLine.parse (command, params);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		executor.setStreamHandler (new PumpStreamHandler (out));
		
		JsonObject result = new JsonObject ();
		
		int exitCode = 0;
		try {
			exitCode = executor.execute (commandLine);
		} catch (ExecuteException ee) {
			result.set (Spec.Code, ee.getExitValue ());
			result.set (Spec.Code, out.toString ().trim ());
		} catch (Exception ge) {
			result.set (Spec.Code, 1000);
			result.set (Spec.Code, Lang.toString (ge));
		}
		
		result.set (Spec.Code, exitCode);
		result.set (Spec.Code, out.toString ().trim ());
		
		return result;
	}
	
}
