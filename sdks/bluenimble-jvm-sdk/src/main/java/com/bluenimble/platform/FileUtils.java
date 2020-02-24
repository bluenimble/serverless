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
package com.bluenimble.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import com.bluenimble.platform.json.JsonArray;

public class FileUtils {
	
	private static final int BUFFER_SIZE = 32 * 1024;
	
	public static void copy (File source, File destFolder, boolean copyRoot) throws IOException {
		copy (source, null, destFolder, copyRoot);
	}
	
	public static void copy (File source, String altName, File destFolder, boolean copyRoot) throws IOException {
		
		if (source == null || destFolder == null) {
			throw new IOException ("null args for FileUtils.copy");
		}
		
		if (Lang.isNullOrEmpty (altName)) {
			altName = source.getName ();
		}
		
		if (source.isFile ()) {
			copyFile (source, destFolder, altName);
			return;
		}
		
		if (copyRoot) {
			destFolder = new File (destFolder, altName);
			destFolder.mkdir ();
		}
		
		File [] files = source.listFiles ();
		if (files == null) {
			return;
		}
		for (File file : files) {
			copy (file, null, destFolder, true);
		}
		
	}
	
	private static void copyFile (File f, File folder, String altName) throws IOException {
		if (f == null || !f.exists () || !f.isFile ()) {
			throw new IOException ("'" + f + "' not a valid file");
		}
		
		if (Lang.isNullOrEmpty (altName)) {
			altName = f.getName ();
		}

		File df = new File (folder, altName);
		
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream (f);
			os = new FileOutputStream (df);
			IOUtils.copy (is, os, BUFFER_SIZE);
		} finally {
			IOUtils.closeQuietly (is);
			IOUtils.closeQuietly (os);
		} 
	}
	
	public static boolean delete (File source) throws IOException {
		if (!source.exists ()) {
			return false;
		}
		if (source.isDirectory ()) {
			File [] files = source.listFiles ();
			for (File file : files) {
				delete (file);
			}
		} 
		return source.delete ();
	}
	
	public static JsonArray readStartsWith (File file, String startsWith, boolean addLineNumber) throws IOException {
		
		JsonArray list = new JsonArray ();

		Reader reader = null;
		try {
			reader = new FileReader (file);
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader (reader);
			int lineIndex = 1;
			String line = br.readLine ();
			while (line != null) {
				line = line.trim ();
				if (line.startsWith (startsWith)) {
					list.add (addLineNumber ? lineIndex + Lang.SPACE + line : line);
				}
				lineIndex++;
				line = br.readLine ();
			}
		} finally {
			IOUtils.closeQuietly (reader);
		}
		
		return list;
		
	}
	
	public static String content (File file) throws IOException {
		String content = null;
		InputStream specIs = null;
		try {
			specIs = new FileInputStream (file);
			content = IOUtils.toString (specIs);
		} catch (IOException ex) {
			throw ex;
		} finally {
			IOUtils.closeQuietly (specIs);
		}
		return content;
	}

}
