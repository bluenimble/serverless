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

import java.time.LocalDateTime;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.Database.Visitor;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.reflect.beans.impls.DefaultBeanSerializer;

public class Find {
	
	public static void main (String [] args) throws Exception {
		
		JsonObject query = new JsonObject ("{ where: { 'receiver': 'alpha', expires: { op: 'gt' } }, orderBy: { timestamp: 'desc' } }");
		
		Json.set (query, "where.expires.value", LocalDateTime.now ());
		
		Database db = new DatabaseServer ().get ();
		
		db.find (
			"Notification", 
			new JsonQuery (query),
			new Visitor () {
				@Override
				public boolean optimize () {
					return true;
				}
				@Override
				public boolean onRecord (DatabaseObject dbo) {
					System.out.println (dbo.toJson (new DefaultBeanSerializer (1, 2)));
					return false;
				}
			}
		);
		
	}
	
}
