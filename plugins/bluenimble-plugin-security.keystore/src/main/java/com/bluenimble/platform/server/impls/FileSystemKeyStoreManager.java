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
package com.bluenimble.platform.server.impls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bluenimble.platform.Encodings;
import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.AbstractApiSpace;
import com.bluenimble.platform.api.impls.ApiSpaceImpl;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStore;
import com.bluenimble.platform.server.KeyStoreManager;

public class FileSystemKeyStoreManager implements KeyStoreManager {

	private static final long serialVersionUID = 1782406079539122227L;
	
	private String keyStoreFile;
	
	private ConcurrentLinkedQueue<ApiSpace> updates = new ConcurrentLinkedQueue<ApiSpace> ();
	
	private long						delay;
	private long						period;
	private ScheduledExecutorService 	listener;
	
	private int							flushRate;

	public FileSystemKeyStoreManager (long delay, long period, String keyStoreFile, int flushRate) {
		this.delay 			= delay;
		this.period 		= period;
		this.keyStoreFile 	= keyStoreFile;
		this.flushRate 		= flushRate;
	}
	
	@Override
	public SpaceKeyStore read (ApiSpace space) throws IOException {
		
		SpaceKeyStoreImpl ks = new SpaceKeyStoreImpl (this, space);
		
		// load default keys
		JsonArray aKeys = space.keys ();
		if (aKeys != null && !aKeys.isEmpty ()) {
			for (int i = 0; i < aKeys.count (); i++) {
				KeyPair skp = toKeyPair ((String)aKeys.get (i));
				ks._put (skp);
			}
		}
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader (new InputStreamReader (new FileInputStream (keyStoreFile (space, true)), Encodings.UTF8));
			String line = reader.readLine ();
			while (line != null) {
				KeyPair skp = toKeyPair (line);
				ks._put (skp);
				line = reader.readLine ();
			}
		} finally {
			IOUtils.closeQuietly (reader);
		}
		
		return ks;
	}

	@Override
	public void write (ApiSpace space) throws IOException {
		
		SpaceKeyStoreImpl iKeystore = (SpaceKeyStoreImpl)((AbstractApiSpace)space).keystore ();
		
		Map<String, KeyPair> all = iKeystore.all ();
				
		if (all == null) {
			return;
		}
		
		StringBuilder sb = new StringBuilder ();
		
		Writer wr = null;
		try {
			wr = new OutputStreamWriter (new FileOutputStream (keyStoreFile (space, true)));
			
			int counter = 0;
			
			Collection<KeyPair> values = all.values ();
			
			for (KeyPair skp : values) {
				
				counter++;
				
				wr.write (skp.accessKey ()); wr.write (Lang.SEMICOLON); 
				wr.write (skp.secretKey ()); wr.write (Lang.SEMICOLON); 
				if (skp.expiryDate () != null) {
					 wr.write (Lang.toString (skp.expiryDate (), Lang.DEFAULT_DATE_FORMAT));
				}
				
				Iterator<String> props = skp.properties ();
				if (props != null) {
					
					wr.write (Lang.SEMICOLON); 
					
					int pCounter = 0;
					while (props.hasNext ()) {
						pCounter++;
						if (pCounter > 1) {
							sb.append (Lang.COMMA);
						}
						String name = props.next ();
						sb.append (name).append (Lang.EQUALS).append (skp.property (name));
					}
					
					wr.write (sb.toString ());
					sb.setLength (0);
					
				}
				
				if (counter < values.size ()) {
					wr.write (Lang.ENDLN); 
				}
				
				if (counter % flushRate == 0) {
					wr.flush (); 
				}
				
			}
			
			wr.flush (); 
			
			sb.setLength (0);
			
		} finally {
			IOUtils.closeQuietly (wr);
		}
		
	}
	
	private static KeyPair toKeyPair (String line) {
		if (Lang.isNullOrEmpty (line)) {
			return null;
		}
		
		final String [] tokens = line.split (Lang.SEMICOLON);
		
		Map<String, Object> props = new HashMap<String, Object> (5);
		
		if (tokens.length > 3 && !Lang.isNullOrEmpty (tokens [3])) {
			String [] aProps = Lang.split (tokens [3], Lang.COMMA, true);
			for (String pv : aProps) {
				String p = pv;
				Object v = true;
				int indexOfEquals = pv.indexOf (Lang.EQUALS);
				if (indexOfEquals > 0) {
					p = pv.substring (0, indexOfEquals);
					v = pv.substring (indexOfEquals + 1);
				}
				props.put (p, v);
			}
		}
		
		return new KeyPair () {

			private static final long serialVersionUID = 2787981500577507959L;

			@Override
			public String accessKey () {
				return tokens [0];
			}

			@Override
			public String secretKey () {
				return tokens [1];
			}

			@Override
			public Date expiryDate () {
				if (tokens.length < 3 || Lang.isNullOrEmpty (tokens [2])) {
					return null;
				}
				
				try {
					return Lang.toDate (tokens [2], Lang.DEFAULT_DATE_FORMAT);
				} catch (ParseException e) {
					return null;
				}
			}

			@Override
			public Object property (String name) {
				if (props.isEmpty ()) {
					return null;
				}
				return props.get (name);
			}

			@Override
			public Iterator<String> properties () {
				if (props.isEmpty ()) {
					return null;
				}
				return props.keySet ().iterator ();
			}
			
			@Override
			public JsonObject toJson () {
				
				JsonObject out = new JsonObject ();
				
				out	.set (KeyPair.Fields.AccessKey, accessKey ())
					.set (KeyPair.Fields.SecretKey, secretKey ());
				if (expiryDate () != null) {
					out.set (KeyPair.Fields.ExpiryDate, Lang.toUTC (expiryDate ()));
				}
				if (!props.isEmpty ()) {
					out.set (KeyPair.Fields.Properties, props);
				}
				
				return out;
			}
			
			@Override
			public String toString () {
				return toJson ().toString ();
			}
			
		};
	}
	
	void notifyUpdate (ApiSpace space) {
		if (updates.contains (space)) {
			return;
		}
		updates.offer (space);
	}

	@Override
	public void start () {
		listener = Executors.newScheduledThreadPool (1);
		listener.scheduleAtFixedRate (new Runnable () {
			@Override
			public void run () {
				ApiSpace space = null;
				while ((space = updates.poll ()) != null) {
					space.tracer ().log (Tracer.Level.Info, "update space {0} keys", space.getNamespace ());
					try {
						write (space);
					} catch (Exception e) {
						space.tracer ().log (Tracer.Level.Error, Lang.BLANK, e);
					}
				}
			}
		}, delay, period, TimeUnit.SECONDS);
	}

	@Override
	public void stop () {
		if (listener != null) {
			listener.shutdown ();
		}
	}

	private File keyStoreFile (ApiSpace space, boolean create) throws IOException {
		File ksf = new File (((ApiSpaceImpl)space).home (), keyStoreFile);
		if (!ksf.exists () && create) {
			ksf.createNewFile ();
		}
		return ksf;
	}

}
