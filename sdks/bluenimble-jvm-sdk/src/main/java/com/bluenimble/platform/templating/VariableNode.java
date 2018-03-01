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
package com.bluenimble.platform.templating;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.Lang;

public class VariableNode implements Node {

	private static final long serialVersionUID = 5233545719136265499L;
	
	private List<Property> vars;
	
	public VariableNode (String text) {
		String [] properties = Lang.split (text, Lang.PIPE, true);
		vars = new ArrayList<Property> (properties.length);
		for (String p : properties) {
			if (p.startsWith (Lang.APOS) && p.endsWith (Lang.APOS)) {
				vars.add (new Property (p.substring (1, p.length () - 1)));
				continue;
			}
			String [] keys = Lang.split (p, Lang.DOT);
			if (keys.length == 1) {
				vars.add (new Property (null, keys));
				continue;
			}
			vars.add (new Property (keys [0], Lang.moveLeft (keys, 1)));
		}
	}

	@Override
	public String token () {
		StringBuilder sb = new StringBuilder ();
		sb.append (Lang.ARRAY_OPEN);
		for (int i = 0; i < vars.size (); i++) {
			VariableNode.Property p = vars.get (i);
			if (p.value != null) {
				sb.append (p.value);
			}
			if (p.namespace != null) {
				sb.append (p.namespace).append (Lang.DOT);
			}
			if (p.keys () != null) {
				sb.append (Lang.join (p.keys (), Lang.DOT));
			}
			if (i < (vars.size () - 1)) {
				sb.append (Lang.PIPE);
			}
		}
		
		sb.append (Lang.ARRAY_CLOSE);
		
		String s = sb.toString ();
		
		sb.setLength (0); sb = null;
		
		return s;
	}
	
	public List<Property> vars () {
		return vars;
	}
	
	public class Property implements Serializable {
		private static final long serialVersionUID = 2966322053270492759L;
		
		String 		namespace;
		String [] 	keys;
		Object 		value; 
		Property (String namespace, String [] keys) {
			this.namespace 	= namespace;
			this.keys 		= keys;
		}
		Property (Object value) {
			this.value 	= value;
		}
		
		public String namespace () {
			return namespace;
		}
		
		public String [] keys () {
			return keys;
		}
		
		public Object value () {
			return value;
		}
		
	}
	
}
