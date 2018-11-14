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
package com.bluenimble.platform.server.impls.fs;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.impls.FileApiStreamSource;
import com.bluenimble.platform.api.media.MediaTypeUtils;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.streams.Chunk;
import com.bluenimble.platform.streams.StreamDecorator;

public class FileApiResource implements ApiResource {

	private static final long serialVersionUID = 5666771960773607533L;
	
	private String 		owner;
	
	private File 		root;

	private File 		file;
	private String 		path;
		
	public FileApiResource (String owner, File root) {
		this (owner, root, root);
	}
	
	public FileApiResource (String owner, File root, File file) {
		this.owner = owner;
		this.root 	= root;
		this.file 	= file;
		if (root.equals (file)) {
			return;
		}
		path = file.getAbsolutePath ();
		path = path.substring (root.getAbsolutePath ().length () + 1).replace ('\\', '/');
	}
	
	@Override
	public String owner () {
		return owner;
	}

	@Override
	public String name () {
		return file.getName ();
	}

	@Override
	public String path () {
		return path;
	}

	@Override
	public String extension () {
		if (!file.isFile ()) {
			return null;
		}
		return file.getName ().substring (file.getName ().lastIndexOf (Lang.DOT) + 1);
	}

	@Override
	public String contentType () {
		return MediaTypeUtils.getMediaForFile (extension ());
	}

	@Override
	public void pipe (final OutputStream os, long position, long length) throws IOException {
		if (!file.isFile ()) {
			throw new IOException ("can't pipe a folder resource");
		}
		if (length == 0) {
			return;
		}
		if (position < 0) {
			position = 0;
		}
		
		FileInputStream in = null;
		try {
			in = new FileInputStream (file);
			FileChannel channel = in.getChannel ();
			channel.transferTo (position, length > 0 ? length : channel.size (), Channels.newChannel (os));
		} finally {
			if (in != null) {
				in.close ();
			}
		}
	}

	@Override
	public void pipe (InputStream is, long position, long length) throws IOException {
		if (!file.isFile ()) {
			throw new IOException ("can't pipe a folder resource");
		}
		if (length == 0) {
			return;
		}
		if (position < 0) {
			position = 0;
		}
		
		FileOutputStream out = null;
		try {
			out = new FileOutputStream (file);
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
		} finally {
			if (out != null) {
				out.close ();
			}
		}
	}

	@Override
	public void pipe (OutputStream os, StreamDecorator decorator, Chunk... chunks) throws IOException {
		if (!file.isFile ()) {
			throw new IOException ("can't pipe a folder resource");
		}
		FileInputStream in = null;
		try {
			in = new FileInputStream (file);
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
		
		} finally {
			if (in != null) {
				in.close ();
			}
		}
	}

	@Override
	public Date timestamp () {
		return new Date (file.lastModified ());
	}

	@Override
	public InputStream toInput () throws IOException {
		return new FileInputStream (file);
	}

	@Override
	public List<ApiResource> children (final Selector selector) {
		if (file.isFile ()) {
			return null;
		}
		File [] files = file.listFiles (new FileFilter () {
			@Override
			public boolean accept (File kid) {
				return selector.select (kid.getName (), kid.isDirectory ());
			}
		}); 
		if (files == null || files.length == 0) {
			return null;
		}
		
		List<ApiResource> list = new ArrayList<ApiResource> ();
		for (File f : files) {
			list.add (new FileApiResource (owner, root, f));
		}
		
		return list;
	}

	@Override
	public long length () {
		if (file.isFile ()) {
			return file.length ();
		}
		return -1;
	}

	@Override
	public ApiStreamSource toStreamSource () {
		if (file.isFile ()) {
			return new FileApiStreamSource (file, null); 
		}
		return null;
	}

	public boolean exists () {
		return file.exists ();
	}

	@Override
	public Object template (JsonObject data) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream (file);
			return Lang.template (IOUtils.toString (is), data, true);
		} finally {
			IOUtils.closeQuietly (is);
		}
	}

}
