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

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;

public class FindOne {
	
	public static void main (String [] args) throws Exception {
		
		String query = "{ where: { id: '5ced0144d3345d2d349e724d', '!target.id': '5ccdd357d3345d6c53db844b' } }";
		
		Database db = new DatabaseServer ().get ();
		
		DatabaseObject brodcast = db.findOne (
			"Broadcast", 
			new JsonQuery (new JsonObject (query))
		);
		
		System.out.println (brodcast);
		
	}
	
}
