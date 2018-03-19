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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.security.KeyPair;
import com.bluenimble.platform.security.SpaceKeyStore;
import com.bluenimble.platform.security.SpaceKeyStoreException;

public class SpaceKeyStoreImpl implements SpaceKeyStore {

	private static final long serialVersionUID = 1L;
	
	private Map<String, KeyPair> keys = new ConcurrentHashMap<String, KeyPair> ();
	
	private FileSystemKeyStoreManager 	manager;
	private ApiSpace 					space;
	
	public SpaceKeyStoreImpl (FileSystemKeyStoreManager manager, ApiSpace space) {
		this.manager 	= manager;
		this.space 		= space;
	}
	
	@Override
	public void delete (Object ak) {
		keys.remove (ak);
	}

	@Override
	public boolean exists (Object ak) {
		return keys.containsKey (ak);
	}

	@Override
	public KeyPair get (Object ak, boolean notIfExpired) {
		KeyPair skp = keys.get (ak);
		if (skp == null) {
			return null;
		}
		if (!notIfExpired) {
			return skp;
		}
		
		// check expiry
		if (skp.expiryDate () == null) {
			return skp;
		}
		if (skp.expiryDate ().before (new Date ())) {
			return skp;
		}
		return null;
	}

	@Override
	public List<KeyPair> list (int offset, int length, ListFilter... filters) {
		if (keys.isEmpty ()) {
			return null;
		}
		
		List<KeyPair> list = new ArrayList<KeyPair>();
		
		int counter = 0;
		Collection<KeyPair> kps = keys.values ();
		for (KeyPair kp : kps) {
			if (counter < offset) {
				continue;
			}
			counter++;
			
			if (check (kp, filters)) {
				list.add (kp);
			}
			
			if (list.size () == length) {
				break;
			}
		}		
		return list;
	}

	@Override
	public void put (KeyPair kp) {
		_put (kp);
		manager.notifyUpdate (space);
	}
	
	void _put (KeyPair kp) {
		if (kp == null) {
			return;
		}
		keys.put (kp.accessKey (), kp);
	}
	
	Map<String, KeyPair> all () {
		return keys;
	}

	@Override
	public List<KeyPair> create (int pack, final Date expiryDate, final Map<String, Object> properties) throws SpaceKeyStoreException {
		if (pack < 1) {
			return null;
		}
		List<KeyPair> keys = null;
		try {
			keys = new ArrayList<KeyPair> ();
			for (int i = 0; i < pack; i++) {
				final String [] aKeys = Lang.keys ();
				KeyPair skp = new KeyPair () {
					private static final long serialVersionUID = -1855450550265796892L;
					@Override
					public String accessKey () {
						return aKeys [0];
					}
					@Override
					public String secretKey () {
						return aKeys [1];
					}
					@Override
					public Date expiryDate () {
						return expiryDate;
					}
					@Override
					public Iterator<String> properties () {
						if (properties == null) {
							return null;
						}
						return properties.keySet ().iterator ();
					}
					@Override
					public Object property (String name) {
						if (properties == null) {
							return null;
						}
						return properties.get (name);
					}
					@Override
					public JsonObject toJson () {
						JsonObject out = new JsonObject ();
						out	.set (KeyPair.Fields.AccessKey, accessKey ())
							.set (KeyPair.Fields.SecretKey, secretKey ());
						if (expiryDate () != null) {
							out.set (KeyPair.Fields.ExpiryDate, expiryDate ());
						}
						if (properties != null) {
							out.set (KeyPair.Fields.Properties, properties);
						}
						return out;
					}
				};
				put (skp);
				keys.add (skp);
			}
		} catch (Exception ex) {
			throw new SpaceKeyStoreException (ex.getMessage (), ex);
		}
		return keys;
	}
	
	private boolean check (KeyPair kp, ListFilter... filters) {
		if (filters == null || filters.length == 0) {
			return true;
		}
		
		Date today = new Date ();
		
		boolean include = true;
		
		for (ListFilter f : filters) {
			String name 	= f.name ();
			Object value 	= f.value ();
			switch (f.operator ()) {
				case eq:
					if (KeyPair.Fields.AccessKey.equals (name)) {
						include = kp.accessKey ().equals (value);
					} else {
						if (value == null) {
							include = kp.property (name) == null;
						} else {
							include = value.equals (kp.property (name));
						}
					}
					break;
	
				case neq:
					if (KeyPair.Fields.AccessKey.equals (name)) {
						include = !kp.accessKey ().equals (value);
					} else {
						if (value == null) {
							include = kp.property (name) != null;
						} else {
							include = !value.equals (kp.property (name));
						}
					}
					break;
	
				case like:
					if (KeyPair.Fields.AccessKey.equals (name)) {
						include = kp.accessKey ().indexOf (String.valueOf (value)) > -1;
					} else {
						if (value == null) {
							include = false;
						} else {
							include = String.valueOf (value).indexOf (String.valueOf (kp.property (name))) > -1;
						}
					}
					break;
	
				case exp:
					include = kp.expiryDate ().getTime () < today.getTime ();
					break;
	
				case nexp:
					include = kp.expiryDate ().getTime () > today.getTime ();
					break;
	
				case alexp:
					if (value == null) {
						value = "0";
					}
					int days = 0;
					try {
						days = Integer.valueOf (String.valueOf (value));
					} catch (NumberFormatException nfex) {
						days = 0;
					}
					include = kp.expiryDate ().getTime () >= (today.getTime () + (days * 24 * 60 * 60 * 1000));
					break;
	
				default:
					break;
				
			}
			
			if (!include) {
				return false;
			}
		}
		
		return include;
		
	}

}
