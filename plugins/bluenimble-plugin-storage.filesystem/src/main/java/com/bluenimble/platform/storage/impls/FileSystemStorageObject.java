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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;

import com.bluenimble.platform.FileUtils;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiContext;
import com.bluenimble.platform.api.ApiOutput;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.impls.DefaultApiStreamSource;
import com.bluenimble.platform.api.media.MediaTypeUtils;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.storage.Folder;
import com.bluenimble.platform.storage.StorageException;
import com.bluenimble.platform.storage.StorageObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class FileSystemStorageObject implements StorageObject {

	private static final long serialVersionUID = 2187711542318846311L;

	protected File 		source;
	protected String 	extension;
	
	private boolean 	isRoot;

	protected 	int 	buffer = 8 * 1024;
	
	private String		reader = Lang.UUID (30);	
	private String		writer = Lang.UUID (30);	
	
	protected FileSystemStorageObject () {
	}
	
	public FileSystemStorageObject (File source, boolean isRoot, int buffer) {
		setSource (source);
		this.isRoot = isRoot;
		this.buffer = buffer;
	}
	
	@Override
	public void copy (Folder folder, boolean move)
			throws StorageException {
		if (isRoot) {
			throw new StorageException ("can't copy root root folder");
		}
		if (move) {
			source.renameTo (new File (((FileSystemFolder)folder).getSource (), name ()));
		} else {
			File newParent = ((FileSystemFolder)folder).getSource ();
			try {
				FileUtils.copy (source, ((FileSystemFolder)folder).getSource (), true);
			} catch (IOException e) {
				throw new StorageException (e.getMessage (), e);
			}
			setSource (new File (newParent, name ()));
		}
	}

	@Override
	public boolean delete () throws StorageException {
		if (isRoot) {
			throw new StorageException ("can't delete root folder");
		}
		try {
			return FileUtils.delete (source);
		} catch (IOException e) {
			throw new StorageException (e.getMessage (), e);
		}
	}

	@Override
	public boolean isFolder () {
		return source.isDirectory ();
	}

	@Override
	public String name () {
		return source.getName ();
	}

	@Override
	public long count () {
		if (source.isFile ()) {
			return 0;
		}
		try {
			return Files.list (source.toPath ()).count ();
		} catch (IOException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}

	@Override
	public boolean exists () {
		return source.exists ();
	}

	@Override
	public InputStream reader (ApiContext context) throws StorageException {
		if (isFolder ()) {
			throw new StorageException (name () + " is a folder");
		}
		
		RecyclableInputStream ris = (RecyclableInputStream)context.getRecyclable (reader);
		if (ris != null) {
			return ris;
		}
		
		InputStream is = null;
		try {
			is = new FileInputStream (source);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} 
		
		ris = new RecyclableInputStream (is);
		context.addRecyclable (reader, ris);
		
		return ris;
	}

	@Override
	public OutputStream writer (ApiContext context) throws StorageException {
		if (isFolder ()) {
			throw new StorageException (name () + " is a folder");
		}
		
		RecyclableOutputStream ros = (RecyclableOutputStream)context.getRecyclable (writer);
		if (ros != null) {
			return ros;
		}
		
		OutputStream os = null;
		try {
			os = new FileOutputStream (source);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} 
		
		ros = new RecyclableOutputStream (os);
		context.addRecyclable (writer, ros);
		
		return ros;
	}

	@Override
	public void rename (String name) throws StorageException {
		if (isRoot) {
			throw new StorageException ("can't rename root folder");
		}
		validateName (name);
		source.renameTo (new File (source.getParentFile (), name));
		if (!name.equals (name ())) {
			throw new StorageException ("unable rename object '" + name () + "' to '" + name + "'. Maybe the object is open by another device.");
		}
	}

	@Override
	public long length () throws StorageException {
		if (source.isFile ()) {
			return source.length ();
		}
		long length = 0;
		Stream<Path> walkStream = null;
		try {
			walkStream = Files.walk (source.toPath ());
		    length = walkStream.filter (p -> p.toFile ().isFile ())
     				.mapToLong (p -> p.toFile ().length ())
     				.sum ();
		} catch (Exception e) {
			throw new StorageException  (e.getMessage (), e);
		} finally {
			if (walkStream != null) walkStream.close ();
		}
		return length;
	}

	@Override
	public Date timestamp () {
		return new Date (source.lastModified ());
	}

	@Override
	public ApiOutput toOutput (Folder.Filter filter, String altName, String altContentType) throws StorageException {
		return new ApiFileOutput (this, filter, altName, altContentType);
	}

	@Override
	public ApiStreamSource toStreamSource (String altName, String altContentType)
			throws StorageException {
		if (isFolder ()) {
			throw new StorageException ("can't acquire stream source from a folder");
		}
		if (Lang.isNullOrEmpty (altName)) {
			altName = name ();
		}
		if (Lang.isNullOrEmpty (altContentType)) {
			altContentType = contentType ();
		}
		try {
			return new DefaultApiStreamSource (altName, altName, altContentType, new FileInputStream (source));
		} catch (FileNotFoundException e) {
			throw new StorageException (e.getMessage (), e);
		}
	}

	@Override
	public long update (InputStream input, boolean append) throws StorageException {
		if (isFolder ()) {
			throw new StorageException ("this is a folder. Can't update content");
		}
		OutputStream os = null;
		try {
			os = new FileOutputStream (source, append);
			return IOUtils.copy (input, os, buffer);
		} catch (IOException ioex) {
			throw new StorageException (ioex.getMessage (), ioex);
		} finally {
			IOUtils.closeQuietly (os);
		}
	}

	@Override
	public String contentType () {
		if (isFolder ()) {
			return null;
		}
		return MediaTypeUtils.getMediaForFile (this.extension);
	}
	
	@Override
	public JsonObject toJson () {
		return toJson (null, true);
	}
	
	public JsonObject toJson (Folder.Filter filter, boolean fetchChildren) {
		JsonObject data = (JsonObject)new JsonObject ()
				.set (StorageObject.Fields.Name, name ())
				.set (StorageObject.Fields.Timestamp, Lang.toUTC (timestamp ()));
		try {
			if (isFolder ()) {
				long count = new FileSystemFolder (source, false, buffer).count ();
				data.set (ApiOutput.Defaults.Count, count);
			    data.set (StorageObject.Fields.Length, length ());
			    		
				// get files
				if (count > 0 && fetchChildren) {
					final JsonArray items = new JsonArray ();
					data.set (ApiOutput.Defaults.Items, items);
					
					Folder folder = toFolder ();
					
					folder.list (new Folder.Visitor () {
						@Override
						public void visit (StorageObject o) {
							items.add (((FileSystemStorageObject)o).toJson (null, false));
						}
					}, filter);
				}
			} else {			
				data.set (StorageObject.Fields.Length, length ())
					.set (StorageObject.Fields.ContentType, contentType ());
			}
		} catch (Exception ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
	
		return data;	
	}

	protected File getSource () {
		return source;
	}
	
	protected void setSource (File source) {
		this.source = source;
		int indexOfDot = source.getName ().lastIndexOf (Lang.DOT);
		if (source.isFile () && indexOfDot >= 0) {
			extension = source.getName ().substring (indexOfDot + 1);
		}
	}
	
	protected void validatePath (String path) throws StorageException {
		if (Lang.isNullOrEmpty (path)) {
			throw new StorageException ("invalid object path 'null'");
		}
		if (path.startsWith (Lang.SLASH) || path.endsWith (Lang.SLASH)) {
			throw new StorageException ("invalid object path '" + path + "'. It shouldn't start with slashes or contains '.', '..' or '~' such as alpha/../beta or ./gamma or ~/omega");
		}
		String [] aPath = Lang.split (path, Lang.SLASH, true);
		for (String p : aPath) {
			if (p.equals (Lang.DOT) || p.equals (Lang.DOT + Lang.DOT) || p.equals (Lang.TILDE)) {
				throw new StorageException ("invalid object path '" + path + "'. It shouldn't start with slashes or contains '.', '..' or '~' such as alpha/../beta or ./gamma or ~/omega");
			}
		}
	} 

	protected void validateName (String name) throws StorageException {
		if (Lang.isNullOrEmpty (name)) {
			throw new StorageException ("invalid object name 'null'");
		}
		if (name.indexOf (Lang.SLASH) >= 0) {
			throw new StorageException ("invalid object name '" + name + "'. It shouldn't contain a '/' (slash) character");
		}
	}
	
	protected FileSystemFolder toFolder () throws StorageException {
		if (this instanceof FileSystemFolder) {
			return (FileSystemFolder)this;
		}
		if (!isFolder ()) {
			throw new StorageException ("object '" + name () + "' isn't a valid folder");
		}
		return new FileSystemFolder (source, isRoot, buffer);
	}


	@Override
	public void pipe (final OutputStream os, long position, long length) throws StorageException {
		if (!source.isFile ()) {
			throw new StorageException ("can't pipe a folder object");
		}
		if (length == 0) {
			return;
		}
		if (position < 0) {
			position = 0;
		}
		
		FileInputStream in = null;
		try {
			in = new FileInputStream (source);
			FileChannel channel = in.getChannel ();
			channel.transferTo (position, length > 0 ? length : channel.size (), Channels.newChannel (os));
		} catch (IOException ex) {
			throw new StorageException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (in);
		}
	}

	@Override
	public void pipe (InputStream is, long position, long length) throws StorageException {
		if (!source.isFile ()) {
			throw new StorageException ("can't pipe a folder object");
		}
		if (length == 0) {
			return;
		}
		if (position < 0) {
			position = 0;
		}
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream (source);
			FileChannel outChannel = out.getChannel ();
			if (length < 0) {
				ReadableByteChannel inChannel = Channels.newChannel (is);
				long skipped = IOUtils.skip (inChannel, position);
				if (skipped < position) {
					return;
				}
				ByteBuffer buffer = ByteBuffer.allocateDirect (32 * 1024);
				while (inChannel.read (buffer) != -1) {
					buffer.flip ();
					outChannel.write (buffer);
					buffer.compact ();
				}
				buffer.flip ();
				while (buffer.hasRemaining ()) {
					outChannel.write (buffer);
				}
			} else {
				ReadableByteChannel inChannel = Channels.newChannel (is);
				outChannel.transferFrom (inChannel, position, length);
			}
		} catch (IOException ex) {
			throw new StorageException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (out);
		}
	}

	@Override
	public void pipe (OutputStream os, StreamDecorator decorator, Chunk... chunks) throws StorageException {
		if (!source.isFile ()) {
			throw new StorageException ("can't pipe a folder object");
		}
		FileInputStream in = null;
		try {
			in = new FileInputStream (source);
			FileChannel channel = in.getChannel ();
			
			if (decorator != null) {
				decorator.start ();
			}
			for (int i = 0; i < chunks.length; i++) {
				Chunk chunk = chunks [i];
				if (decorator != null) {
					decorator.pre (chunk, i);
				}
				channel.transferTo (chunk.start (), chunk.end () == Chunk.Infinite ? (chunk.end () - chunk.start () + 1) : channel.size (), Channels.newChannel (os));
				if (decorator != null) {
					decorator.post (chunk, i);
				}
			}
			if (decorator != null) {
				decorator.end ();
			}
		
		} catch (IOException ex) {
			throw new StorageException (ex.getMessage (), ex);
		} finally {
			IOUtils.closeQuietly (in);
		}
	}

	@Override
	public Channel channel (OpenOption... options) throws StorageException {
		if (isFolder ()) {
			throw new StorageException (name () + " is a folder");
		}
		
		try {
			return FileChannel.open (source.toPath (), options);
		} catch (IOException ex) {
			throw new StorageException (ex.getMessage (), ex);
		}
	}

	
}
