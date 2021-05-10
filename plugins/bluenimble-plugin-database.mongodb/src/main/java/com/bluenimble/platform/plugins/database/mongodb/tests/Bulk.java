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

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.db.DatabaseException;
import com.bluenimble.platform.db.DatabaseObject;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class Bulk {
	
	public static void main (String [] args) throws DatabaseException {
		
		Database db = new DatabaseServer ().get ();
		
		DatabaseObject giftcard = db.create ("GiftCard");
		
		giftcard.set ("quantity", 3);
		giftcard.set ("value", 1000);
		giftcard.save ();
		
		JsonArray codes = new JsonArray ();
		JsonObject bulk = (JsonObject)new JsonObject ().set ("GiftCardCode", codes);
		
		for (int i = 0; i < 3; i++) {
			JsonObject oCode = (JsonObject)new JsonObject ()
				.set ("id", Lang.oid ())
				.set ("$card", new JsonObject ().set ("$entity", "GiftCard").set ("id", giftcard.getId ()));
			codes.add (oCode);
		}
		
		db.bulk (bulk);
		
	}
	
}
