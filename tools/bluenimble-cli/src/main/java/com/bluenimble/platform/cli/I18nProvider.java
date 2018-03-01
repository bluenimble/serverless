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
package com.bluenimble.platform.cli;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18nProvider {
	
	private static final Map<String, String> i18n = new HashMap<String, String> (); 
	
	public static final Locale DEFAULT_LANG = Locale.ENGLISH;
	
	public static String get (String key) {
		if (key == null) {
			return null;
		}
		String text = i18n.get (key);
		if (text == null) {
			return key;
		}
		return text;
	}
	
	public static void install (String base, boolean override) throws InstallI18nException {
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle (base);
		} catch (MissingResourceException mrex) {
			rb = ResourceBundle.getBundle (base, DEFAULT_LANG);
		}
		Iterator<?> names = rb.keySet().iterator ();
		while (names.hasNext ()) {
			String key = (String)names.next ();
			if (i18n.containsKey (key) && !override) {
				throw new InstallI18nException ("Text [" + key + "] already installed");
			}
			i18n.put (key, rb.getString (key));
		}
	}
	
}
