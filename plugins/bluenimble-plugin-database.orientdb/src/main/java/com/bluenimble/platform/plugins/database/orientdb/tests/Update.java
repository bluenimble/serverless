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
import com.bluenimble.platform.db.impls.DefaultDatabaseObjectSerializer;
import com.bluenimble.platform.json.JsonObject;

public class Update {
	
	public static void main (String [] args) throws DatabaseException {
		
		Database db = new DatabaseServer ().get ();
		
		DatabaseObject employee = db.get ("Employees", "e0e296f0-1937-4b7c-b077-1d7fe50e2482");
				
		employee.set ("age", 43);
		employee.set ("salary", 200.54);
		employee.set ("contact", new JsonObject ().set ("phone", "4089786532").set ("email", "alpha@beta.com"));
		
		DatabaseObject city = db.create ("Cities");
		city.set ("name", "Sunnyvale");
		
		employee.set ("city", city);
		
		employee.save ();
		
		System.out.println (employee.toJson (new DefaultDatabaseObjectSerializer (2, 2)));
		
	}
	

}
