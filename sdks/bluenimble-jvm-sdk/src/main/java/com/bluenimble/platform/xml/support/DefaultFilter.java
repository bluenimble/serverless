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
package com.bluenimble.platform.xml.support;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;

public class DefaultFilter extends HashMap<String, Object> implements Filter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6178653047538035617L;

	{
		addKey("\"", PrinterEntities.QUOT);
		addKey("&", PrinterEntities.AMP);
		addKey("<", PrinterEntities.LT);
		addKey(">", PrinterEntities.GT);
	}

	public DefaultFilter() {
		super(4);
	}

	/** Register things to be filtered. */
	public Filter addKey(String name, Object keyObj) {
		this.put (name, keyObj);
		return this;
	}

	/** Remove things to be filtered. */
	public Filter removeKey(String name) {
		try {
			this.remove(name);
		} catch (Exception e) {
		}
		return this;
	}

	/** Check to see if something is going to be filtered. */
	public boolean hasKey(String key) {
		return (this.containsKey(key));
	}

	/** Perform the filtering operation. */
	public String scan(String toScan) {
		if (toScan == null || toScan.length() == 0)
			return "";
		StringBuilder bs = new StringBuilder(toScan.length() + 50);
		StringCharacterIterator sci = new StringCharacterIterator(toScan);
		String tmp = null;
		for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
			tmp = String.valueOf(c);
			if (hasKey(tmp))
				tmp = (String) this.get(tmp);
			bs.append(tmp);
		}
		return (bs.toString());
	}
}