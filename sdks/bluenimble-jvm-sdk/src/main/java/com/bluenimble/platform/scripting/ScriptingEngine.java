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
package com.bluenimble.platform.scripting;

import java.io.Serializable;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiResource;

@Feature ( name = "scripting" )
public interface ScriptingEngine extends Serializable {

	enum Supported {
		Javascript
	}
	
	Object 	eval 	(Supported 	engine, Api api, ApiResource resource, ScriptContext context) throws ScriptingEngineException;
	
	Object 	invoke 	(Object 	scriptable, String function, Object... args) throws ScriptingEngineException;

	boolean has 	(Object 	scriptable, String function);

}
