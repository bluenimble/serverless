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
package com.bluenimble.platform.icli.mgm.boot;

public interface Spec {
	interface version 	{
		String Major = "major";
		String Minor = "minor";
		String Patch = "patch";
	}
	String Name 		= "name";
	String Title 		= "title";
	String Copyright	= "copyright";
	String Package 		= "package";
	
	interface endpoints 	{
		String Version 	= "version";
		String Download = "download";
		String Signup 	= "signup";
		String Login 	= "login";
		String Snippets = "snippets";
	}
}