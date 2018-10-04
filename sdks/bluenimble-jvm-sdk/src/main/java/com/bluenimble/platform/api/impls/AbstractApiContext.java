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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiContext;

public abstract class AbstractApiContext implements ApiContext {

	private static final long serialVersionUID = -3224649113967307826L;
	
	protected 	Object 					reference;

	protected 	Map<String, Recyclable> recyclable;
	
	@Override
	public void	addRecyclable (String name, Recyclable r) {
		if (recyclable == null) {
			recyclable = new HashMap<String, Recyclable> ();
		}
		recyclable.put (name, r);
	}
	
	@Override
	public void recycle () {
		if (recyclable == null || recyclable.isEmpty ()) {
			return;
		}
		Iterator<Recyclable> values = recyclable.values ().iterator ();
		while (values.hasNext ()) {
			values.next ().recycle ();
		}
		recyclable.clear ();
		recyclable = null;
		
		reference = null;
	}
	
	@Override
	public void finish (boolean withError) {
		if (recyclable == null || recyclable.isEmpty ()) {
			return;
		}
		Iterator<Recyclable> values = recyclable.values ().iterator ();
		while (values.hasNext ()) {
			values.next ().finish (withError);
		}
	}
	
	@Override
	public Object getReference () {
		return reference;
	}

	@Override
	public void setReference (Object reference) {
		this.reference = reference;
	}

}
