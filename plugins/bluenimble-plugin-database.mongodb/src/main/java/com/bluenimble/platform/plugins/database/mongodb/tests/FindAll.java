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
package com.bluenimble.platform.plugins.database.mongodb.tests;

import java.util.List;

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.db.query.impls.JsonQuery;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class FindAll {
	
	public static void main (String [] args) throws Exception {
		
		String query = "{ }";
		
		Database db = new DatabaseServer ().get ();
		
		List<DatabaseObject> employees = db.find (
			"Media", 
			new JsonQuery (new JsonObject (query)),
			null
		);
		
		for (DatabaseObject employee : employees) {
			System.out.println (employee.toJson (new DefaultBeanSerializer (2, 2)));
		}
		
	}
	
}
