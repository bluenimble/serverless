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
package com.bluenimble.platform.cli.command.impls;

import com.bluenimble.platform.cli.command.impls.handlers.JsonCountHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonCreateHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonDeleteHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonJoinHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonLoadHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonSaveHandler;
import com.bluenimble.platform.cli.command.impls.handlers.JsonSetHandler;

public class JsonCommand extends PrefixedCommand {

	private static final long serialVersionUID = 8809252448144097989L;
	
	interface Subject {
		String Create 	= "create";
		String Set 		= "set";
		String Delete 	= "delete";
		String Save 	= "save";
		String Load 	= "load";
		String Join 	= "join";
		String Count 	= "count";
	}
	
	public JsonCommand () {
		super ("json", "json utility command");
		addHandler (Subject.Create, new JsonCreateHandler ());
		addHandler (Subject.Set, new JsonSetHandler ());
		addHandler (Subject.Delete, new JsonDeleteHandler ());
		addHandler (Subject.Save, new JsonSaveHandler ());
		addHandler (Subject.Load, new JsonLoadHandler ());
		addHandler (Subject.Join, new JsonJoinHandler ());
		addHandler (Subject.Count, new JsonCountHandler ());
	}

}
