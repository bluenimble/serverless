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
package com.bluenimble.platform.storage.impls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.ValueHolder;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;

public class FileSystemFolder extends FileSystemStorageObject implements Folder {

	private static final long serialVersionUID = 2756236507680103819L;
	
	public FileSystemFolder (File source, boolean isRoot) {
		super (source, isRoot);
	}
	
	@Override
	public Folder add (String path, boolean ignoreIfExists) throws StorageException {
		validatePath (path); 
		File folder = new File (source, path);
		if (folder.exists () && !ignoreIfExists) {
			throw new StorageException ("folder '" + path + "' already exists under " + name ());
		}
		folder.mkdirs ();
		if (!folder.exists ()) {
			throw new StorageException ("unbale to create folder '" + path + "' under " + name ());
		}
		return new FileSystemFolder (folder, false);
	}

	@Override
	public StorageObject add (ApiStreamSource ss, String altName, boolean overwrite)
			throws StorageException {
		
		if (Lang.isNullOrEmpty (altName) && ss == null) {
			throw new StorageException ("object name is required");
		}
		
		String name = altName != null ? altName : ss.name ();
		
		validateName (name); 
		
		File file = new File (source, name);
		if (file.exists () && !overwrite) {
			throw new StorageException ("object '" + name + "' already exists under " + name ());
		}
		
		if (ss == null && !file.exists ()) {
			try {
				file.createNewFile ();
			} catch (IOException ioex) {
				throw new StorageException (ioex.getMessage (), ioex);
			}
			return new FileSystemStorageObject (file, false);
		}
		
		OutputStream os = null;
		try {
			os = new FileOutputStream (file);
			IOUtils.copy (ss.stream (), os);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} finally {
			IOUtils.closeQuietly (os);
		}
		
		return new FileSystemStorageObject (file, false);
	}

	@Override
	public StorageObject get (String path) throws StorageException {
		
		validatePath (path);
		
		File file = new File (source, path);
		if (!file.exists ()) {
			throw new StorageException ("object '" + path + "' not found under " + name ());
		}
		
		if (file.isDirectory ()) {
			return new FileSystemFolder (file, false);
		}
		
		return new FileSystemStorageObject (file, false);
	}

	@Override
	public boolean contains (String name) throws StorageException {
		return new File (source, name).exists ();
	}

	@Override
	public void list (Visitor visitor, Filter filter) throws StorageException {
		if (visitor == null) {
			return;
		}
		
		final ValueHolder<FileSystemStorageObject> vh = new ValueHolder<FileSystemStorageObject> ();
		vh.set (new FileSystemStorageObject ());
		
		DirectoryStream<Path> stream = null;
		
		try {
			
			stream = Files.newDirectoryStream (source.toPath (), new DirectoryStream.Filter<Path>() {
			    public boolean accept (Path path) throws IOException {
			    	vh.get ().setSource (path.toFile ());
					return filter == null ? true : filter.accept (vh.get ());
			    }
			});
			if (stream == null) {
				return;
			}
			
			for (Path path: stream) {
		    	vh.get ().setSource (path.toFile ());
				if (vh.get ().isFolder ()) {
					try {
						vh.set (vh.get ().toFolder ());
					} catch (StorageException e) {
						throw new IOException (e.getMessage (), e);
					}
				}
				visitor.visit (vh.get ());
			}
			
		} catch (IOException e) {
			throw new StorageException (e.getMessage (), e);
		} finally {
			try { if (stream != null) stream.close (); } catch (IOException ex) {}
		}

	}

}
    
