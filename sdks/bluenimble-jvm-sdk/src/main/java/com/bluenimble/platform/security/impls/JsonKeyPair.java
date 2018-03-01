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
package com.bluenimble.platform.security.impls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.EncryptionProvider;
import com.bluenimble.platform.security.EncryptionProvider.Mode;
import com.bluenimble.platform.security.KeyPair;

public class JsonKeyPair implements KeyPair {

	private static final long serialVersionUID = 173411864947108343L;
	
	private static final String Salt = "H4fd)$MM#a=%v!.-";
	
	private JsonObject 	source;
	
	private Date 		expiryDate;

	public JsonKeyPair (File file) throws Exception {
		this (Json.load (file));
	}
	
	public JsonKeyPair (JsonObject source) throws Exception {
		init (source);
	}
	
	public JsonKeyPair (File file, boolean decrypt) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream ();
		InputStream is = null;
		try {
			is = new FileInputStream (file);
			EncryptionProvider.Default.crypt (is, os, Salt, Mode.Decrypt);
		} finally {
			IOUtils.closeQuietly (is);
		}
		init (new JsonObject (new String (os.toByteArray ())));
	}
	
	private void init (JsonObject source) throws Exception {
		this.source = source;
		String sExpiryDate = Json.getString (source, Fields.ExpiryDate);
		if (!Lang.isNullOrEmpty (sExpiryDate)) {
			try {
				expiryDate = Lang.toDate (sExpiryDate, Lang.DEFAULT_DATE_FORMAT);
			} catch (ParseException e) {
				throw new Exception ("Invalid KeyPair Expiration Date " + sExpiryDate);
			}
		}
	}
	
	public void store (File file, boolean encrypt) throws IOException {
		if (encrypt) {
			Json.store (source, file, Salt);
		} else {
			Json.store (source, file);
		}
	}
	
	public static JsonKeyPair create () throws Exception {
		String [] keys = Lang.keys (240, 240);
		return new JsonKeyPair ((JsonObject) new JsonObject ().set (Fields.AccessKey, keys [0]).set (Fields.SecretKey, keys [1]));
	}
	
	@Override
	public String accessKey () {
		return Json.getString (source, Fields.AccessKey);
	}

	@Override
	public String secretKey () {
		return Json.getString (source, Fields.SecretKey);
	}

	@Override
	public JsonObject toJson () {
		return source;
	}

	@Override
	public Date expiryDate () {
		return expiryDate;
	}

	@Override
	public Object property (String name) {
		return Json.find (source, Fields.Properties, name);
	}

	@Override
	public Iterator<String> properties () {
		JsonObject props = Json.getObject (source, Fields.Properties);
		if (props == null) {
			return null;
		}
		return props.keys ();
	}
	
}
