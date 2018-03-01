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
package com.bluenimble.platform.icli.mgm.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.icli.mgm.CliSpec;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.EncryptionProvider;
import com.bluenimble.platform.security.impls.AESEncryptionProvider;

public class KeysUtils {

	public static JsonObject read (String name, String password, String secProvider) throws CommandExecutionException {
		File bluenimbleKeys = new File (System.getProperty ("user.home"), "bluenimble-dev-cli/secrets");
		if (!bluenimbleKeys.exists ()) {
			throw new CommandExecutionException ("secrets not found");
		}
		
		File store = new File (bluenimbleKeys, name + CliSpec.KeysExt);
		if (!store.exists ()) {
			throw new CommandExecutionException ("secrets '" + name + "' not found");
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream ();
		
		int length = password.length ();  
        if (length > 16 && length != 16){  
        	password = password.substring (0, 15);  
        }  
        if (length < 16 && length != 16){  
             for(int i = 0; i < 16 - length; i++){  
            	 password = password + "0";  
             }  
        }  

        AESEncryptionProvider provider = new AESEncryptionProvider (secProvider);
		
		JsonObject o;
		try {
			provider.crypt (new FileInputStream (store), os, password, EncryptionProvider.Mode.Decrypt);
			o = new JsonObject (new String (os.toByteArray ()));
		} catch (Exception e) {
			throw new CommandExecutionException (e.getMessage (), e);
		}
		
		return o;
	}
	
}
