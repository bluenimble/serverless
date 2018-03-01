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

/**
 * INetStudio main class attribute defenition
 */
public class ComponentAttribute {
	private String NAME;

	private String VALUE;

	private String QUOTE = "\"";

	private String EQUALS = "=";

	public ComponentAttribute() {
	}

	public ComponentAttribute(String name, String value) {
		NAME = name;
		VALUE = value;
	}

	public ComponentAttribute(String name, int value) {
		NAME = name;
		VALUE = "" + value;
	}

	public ComponentAttribute(String name) {
		NAME = name;
	}

	public ComponentAttribute convert_To(String name, String value) {
		NAME = name;
		VALUE = value;
		return this;
	}
	
	public void clear () {
		NAME = null;
		VALUE = null;
	}

	public ComponentAttribute copy() {
		return (new ComponentAttribute(this.NAME, this.VALUE));
	}

	public String value() {
		return VALUE;
	}

	public String name() {
		return NAME;
	}

	public String value(String value) {
		String temp = VALUE;
		VALUE = value;
		return temp;
	}

	public String name(String name) {
		String temp = NAME;
		NAME = name;
		return temp;
	}

	public String quote(String quote) {
		String temp = QUOTE;
		QUOTE = quote;
		return temp;
	}

	public String equals(String equals) {
		String temp = EQUALS;
		EQUALS = equals;
		return temp;
	}

	public String print() {
		return (VALUE == null) ? NAME : (NAME + EQUALS + QUOTE + VALUE + QUOTE);
	}

	public String print(Filter filter) {
		if (filter != null)
			return (VALUE == null) ? NAME : (NAME + EQUALS + QUOTE
					+ filter.scan(VALUE) + QUOTE);
		else
			return (VALUE == null) ? NAME
					: (NAME + EQUALS + QUOTE + VALUE + QUOTE);
	}
}