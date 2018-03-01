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
package com.bluenimble.platform.server.maps.impls;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.server.maps.DistributedMap;
import com.bluenimble.platform.server.maps.MapProvider;

public class DefaultMapProvider implements MapProvider {

	private static final long serialVersionUID = -697111085841838176L;
	
	@Override
	public <U, V> DistributedMap<U, V> get (String name) {
		return create (new ConcurrentHashMap<U, V> ());
	}
	
	protected <U, V> DistributedMap<U, V> create (final Map<U, V> proxy) {
		return new DistributedMap<U, V> () {

			private static final long serialVersionUID = 7568736600329652993L;

			@Override
			public void put (U k, V v) {
				proxy.put (k, v);
			}

			@Override
			public V get (U k) {
				return proxy.get (k);
			}

			@Override
			public void remove (U k) {
				proxy.remove (k);
			}

			@Override
			public boolean containsKey (U k) {
				return proxy.containsKey (k);
			}

			@Override
			public Set<U> keySet () {
				return proxy.keySet ();
			}

			@Override
			public Collection<V> values () {
				return proxy.values ();
			}

			@Override
			public void clear () {
				proxy.clear ();
			}

			@Override
			public boolean isEmpty () {
				return proxy.isEmpty ();
			}
		
		};
	}

}
