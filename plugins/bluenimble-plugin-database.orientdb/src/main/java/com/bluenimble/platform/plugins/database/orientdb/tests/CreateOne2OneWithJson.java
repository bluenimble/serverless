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
package com.bluenimble.platform.plugins.database.orientdb.tests;

import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonObject;

public class CreateOne2OneWithJson {
	
	public static void main (String [] args) throws DatabaseException {
		
		Database db = new DatabaseServer ().get ();
		
		// create driver
		DatabaseObject driver = db.create ("Drivers");
		driver.set ("name", "One2One-New-Json");
		driver.set ("salary", 50.50);
		
		driver.set ("car", new JsonObject ().set (Database.Fields.Entity, "Cars").set (Database.Fields.Id, "ef8dd593-948f-4c22-a467-c110348e6636"));
		
		driver.save ();
		
		System.out.println (driver.toJson (null));
		
	}
	
}
