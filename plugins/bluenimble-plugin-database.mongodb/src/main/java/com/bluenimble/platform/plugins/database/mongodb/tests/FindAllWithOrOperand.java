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
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.reflect.beans.BeanSerializer;
import com.bluenimble.platform.reflect.beans.impls.JsonBeanSerializer;

public class FindAllWithOrOperand {
	
	public static void main (String [] args) throws Exception {
		
		String query = "{ where = {\n" + 
				"				or: [{\n" + 
				"					'createdBy.id': '5ca3cdb9894e04330ad8bbf9',\n" + 
				"					'recipient.id': '5ca3cf2b894e04330ad8bbfb'\n" + 
				"				}, {\n" + 
				"					'createdBy.id': '5ca3cf2b894e04330ad8bbfb',\n" + 
				"					'recipient.id': '5ca3cdb9894e04330ad8bbf9'\n" + 
				"				}] \n" + 
				"			} }";
		
		Database db = new DatabaseServer ().get ();
		
		List<DatabaseObject> messages = db.find (
			"Message",
			new JsonQuery (new JsonObject (query)),
			null
		);
		
		// {_fields:simple, recipient: { id: true, firstName: true, lastName: true }}
		
		BeanSerializer serializer = new JsonBeanSerializer (new JsonObject (" {_fields:simple}" ));
		
		for (DatabaseObject message : messages) {
			System.out.println (message.toJson (serializer));
		}
		
	}
	
}
