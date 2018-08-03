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
package com.bluenimble.platform.api.impls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.media.MediaTypeUtils;

public class FileApiStreamSource implements ApiStreamSource {

	private static final long serialVersionUID = 8418771218899098700L;
	
	private String 		name;
	private String 		extension;
	private File 		file;
	
	private InputStream	stream;
	
	public FileApiStreamSource (File file, String removeExtension) {
		this.file = file;
		this.name = file.getName ();
		
		this.extension = file.getName ().substring (this.name.lastIndexOf (Lang.DOT) + 1);
		
		if (Lang.isNullOrEmpty (removeExtension)) {
			return;
		}
		if (this.name.endsWith (removeExtension)) {
			this.name = this.name.substring (0, this.name.lastIndexOf (removeExtension));
		}
	}
	
	@Override
	public String contentType () {
		return MediaTypeUtils.getMediaForFile (this.extension);
	}

	@Override
	public String id () {
		return name;
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public InputStream stream () {
		if (stream == null) {
			try {
				stream = new FileInputStream (file);
			} catch (FileNotFoundException ex) {
				throw new RuntimeException (ex.getMessage (), ex);
			}
		}
		return stream;
	}

	@Override
	public void close () {
		IOUtils.closeQuietly (stream);
	}

}
