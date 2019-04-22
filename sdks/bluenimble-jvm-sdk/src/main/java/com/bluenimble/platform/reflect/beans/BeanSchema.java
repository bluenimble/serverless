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
package com.bluenimble.platform.reflect.beans;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.api.validation.ApiServiceValidator.Spec;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.impls.AllFieldsBeanSchema;
import com.bluenimble.platform.reflect.beans.impls.JsonBeanSchema;
import com.bluenimble.platform.reflect.beans.impls.MinimalFieldsBeanSchema;

public interface BeanSchema {
	
	BeanSchema 		All 	= new AllFieldsBeanSchema ();
	BeanSchema 		Minimal = new MinimalFieldsBeanSchema ();
	BeanSchema 		Simple 	= new JsonBeanSchema ((JsonObject)new JsonObject ().set (Spec.Fields, FetchStrategy.simple.name ()));
	
	Set<String>	MinimalFields 	= new HashSet<String> (Arrays.asList (new String [] { Database.Fields.Id, Database.Fields.Timestamp }));
	
	enum FetchStrategy {
		listed,
		all,
		minimal,
		simple,
		extended
	}
	
	BeanSchema		schema 			(int level, String field);
	
	Set<String> 	fields 			(Set<String> available);
	
	FetchStrategy	fetchStrategy 	();
	
}