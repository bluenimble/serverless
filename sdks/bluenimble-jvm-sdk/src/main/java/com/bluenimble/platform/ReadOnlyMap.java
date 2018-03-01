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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ReadOnlyMap<K, V> implements Map<K, V> {

	@Override
	public boolean containsValue (Object value) {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public V put (K key, V value) {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public V remove (Object key) {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public void putAll (Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public void clear () {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public Collection<V> values () {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException ("ReadOnlyMap doesn't provide this method");
	}

}
