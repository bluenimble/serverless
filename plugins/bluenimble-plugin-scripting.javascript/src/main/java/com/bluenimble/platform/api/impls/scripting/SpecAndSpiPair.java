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
package com.bluenimble.platform.api.impls.scripting;

import java.io.Serializable;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class SpecAndSpiPair implements Serializable {

	private static final long serialVersionUID = -5450514957053351188L;
	
	private static final String Spi = "spi";
	
	private Object spec;
	private Object spi;
	
	public SpecAndSpiPair (Object spec, Object spi) {
		if (spec instanceof ScriptObjectMirror) {
			((ScriptObjectMirror) spec).put (Spi, spi);
		}
		this.spec = spec;
		this.spi = spi;
	}
	
	public Object spec () {
		return spec;
	}
	public Object spi () {
		return spi;
	}
	
}
