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

import java.util.Date;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;

public class FindWithDate {
	
	public static void main (String [] args) throws Exception {
		
		Date date = new Date (1612552852403L);
		
		JsonObject jq = new JsonObject (
			"{ where: { 'display.id': '601a1d604c5fb67415301879', timestamp: { op: 'lte', value: 0 }, status: 2 } }"
		);
		
		Json.set (jq, "where.timestamp.value", date);
		
		Database db = new DatabaseServer ().get ();
		
		DatabaseObject display = db.findOne (
			"Display", 
			new JsonQuery (jq)
		);
		
		System.out.println (display);
		
	}
	
}
