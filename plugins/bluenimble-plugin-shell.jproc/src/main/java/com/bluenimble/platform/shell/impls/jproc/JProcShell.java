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
package com.bluenimble.platform.shell.impls.jproc;

import java.io.File;

import org.buildobjects.process.ExternalProcessFailureException;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.shell.Shell;

public class JProcShell implements Shell {

	private static final long serialVersionUID = 4303282790607692198L;
	
	private static final int OtherError = 10000;

	private File 			baseDirectory;
	
	public JProcShell (File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	
	public JProcShell (String baseDirectory) {
		if (baseDirectory.startsWith (Lang.TILDE)) {
			baseDirectory = System.getProperty ("user.home") + baseDirectory.substring (1);
		}
		this.baseDirectory = new File (baseDirectory);
	}
	
	@Override
	public JsonObject run (String command, JsonObject params) {
		JsonObject result = new JsonObject ();
		
		if (!this.baseDirectory.exists ()) {
			result.set (Spec.Code, 1000);
			result.set (Spec.Message, "Shell directory not found");
			return result;
		}
		
		try {
			ProcResult pr = new ProcBuilder ("bash")
			    .withArgs ("-c", (String)Lang.template (command, params, true))
				.withTimeoutMillis (600000) // 10 minutes
				.withWorkingDirectory (baseDirectory)
				.run ();
			
			result.set (Spec.Code, pr.getExitValue ());
			result.set (Spec.Data, pr.getOutputString ());
		} catch (ExternalProcessFailureException ee) {
			result.set (Spec.Code, ee.getExitValue ());
			result.set (Spec.Message, ee.getStderr ());
		} catch (Exception ge) {
			result.set (Spec.Code, OtherError);
			result.set (Spec.Message, Lang.toString (ge));
		}
		
		return result;
	}
	
	public static void main (String [] args) {
		JProcShell shell = new JProcShell ("/Users/lilya/Downloads");
		System.err.println (shell.run ("./video-info.sh", null));
	}
	
}
