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

public class DoubleValueConverter implements OptionArgumentValueConverter {

	private static final long serialVersionUID = -2505885776301563520L;

	@Override
	public Object cast (String optionName, String value) throws ArgumentValueCastException {
		if (value == null) {
			return value;
		}
		try {
			return Double.parseDouble (value);
		} catch (NumberFormatException nfe) {
			throw new ArgumentValueCastException ("Option [" + optionName + "], Argument [" + value + "] must be a valid decimal");
		}
	}

}
