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
package com.bluenimble.platform.cli.command.parser.converters;

import java.io.File;
import java.io.IOException;

import com.bluenimble.platform.ArchiveUtils;
import com.bluenimble.platform.Lang;

public class ZipValueConverter implements OptionArgumentValueConverter {

	private static final long serialVersionUID = -2505885776301563520L;

	public ZipValueConverter () {
	}
	
	@Override
	public Object cast (String optionName, String value) throws ArgumentValueCastException {
		if (value == null) {
			return value;
		}
		File folder = new File (value);
		if (!folder.exists ()) {
			throw new ArgumentValueCastException ("Option [" + optionName + "] folder " + value + " not found");
		}
		if (!folder.isDirectory ()) {
			throw new ArgumentValueCastException ("Option [" + optionName + "] " + value + " is not a valid folder");
		}
		
		File temp = new File (System.getProperty ("user.home"), Lang.UUID (20) + ".zip");
		
		try {
			ArchiveUtils.compress (folder, temp, true);
			return new StreamPointer (temp, true);
		} catch (IOException e) {
			throw new ArgumentValueCastException ("Option [" + optionName + "] enable to compress folder " + value + " due to > " + e.getMessage ());
		}
		
	}

}
