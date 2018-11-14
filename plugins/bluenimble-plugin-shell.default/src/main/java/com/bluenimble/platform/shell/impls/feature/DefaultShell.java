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
package com.bluenimble.platform.shell.impls.feature;

import java.io.File;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.shell.OsCommandExecuter;
import com.bluenimble.platform.shell.OsCommandExecuterCallbackException;
import com.bluenimble.platform.shell.OsCommandExecuterException;
import com.bluenimble.platform.shell.OsProcessHandle;
import com.bluenimble.platform.shell.Shell;
import com.bluenimble.platform.shell.impls.OsCommandExecuterStringCallback;

public class DefaultShell implements Shell {

	private static final long serialVersionUID = 4303282790607692198L;

	private File 				baseDirectory;
	private OsCommandExecuter 	executer;
	
	public DefaultShell (File baseDirectory, OsCommandExecuter executer) {
		this.baseDirectory = baseDirectory;
		this.executer = executer;
	}
	
	public DefaultShell (String baseDirectory, OsCommandExecuter executer) {
		if (baseDirectory.startsWith (Lang.TILDE)) {
			baseDirectory = System.getProperty ("user.home") + baseDirectory.substring (1);
		}
		this.baseDirectory = new File (baseDirectory);
		this.executer = executer;
	}
	
	@Override
	public JsonObject run (String command, JsonObject params) {
		
		JsonObject result = new JsonObject ();
		
		try {
			executer.execute (
				(String)Lang.template (command, params, true), 
				null, 
				baseDirectory, 
				new OsCommandExecuterStringCallback () {
					private static final long serialVersionUID = 1L;
					@Override
					public void finish (int exitValue, String response, OsProcessHandle handle)
							throws OsCommandExecuterCallbackException {
						result.set (Spec.Code, exitValue);
						result.set (Spec.Data, response);
						handle.destroy ();
					}
				}
			);
		} catch (OsCommandExecuterException ee) {
			result.set (Spec.Code, ee.getExitValue ());
			result.set (Spec.Message, ee.getMessage () != null ? ee.getMessage ().trim () : Lang.BLANK);
		} catch (Exception ge) {
			result.set (Spec.Code, OsCommandExecuterException.OtherError);
			result.set (Spec.Message, Lang.toString (ge));
		}
		
		return result;
	}
	
}
