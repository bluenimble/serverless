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
package com.bluenimble.platform.cli.command.parser.converters;

import java.util.HashSet;
import java.util.Set;

public class BooleanValueConverter implements OptionArgumentValueConverter {

	private static final long serialVersionUID = -2505885776301563520L;

	private Set<String> noValues = new HashSet<String> ();
	private Set<String> yesValues = new HashSet<String> ();
	
	public BooleanValueConverter () {
		noValues.add ("no");
		noValues.add ("n");
		noValues.add ("0");
		noValues.add ("false");

		yesValues.add ("yes");
		yesValues.add ("y");
		yesValues.add ("1");
		yesValues.add ("true");
	}
	
	@Override
	public Object cast (String optionName, String value) throws ArgumentValueCastException {
		if (value == null) {
			return value;
		}
		value = value.toLowerCase ();
		if (!noValues.contains (value) || !yesValues.contains (value)) {
			throw new ArgumentValueCastException ("Option [" + optionName + "], Argument [" + value + "] must be a valid boolean {yes, y, 1, true, no, n, 0 or false}");
		}
		if (yesValues.contains (value)) {
			return true;
		}
		return false;
	}

}
