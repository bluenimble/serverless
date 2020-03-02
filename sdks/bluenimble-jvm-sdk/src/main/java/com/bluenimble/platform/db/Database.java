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
package com.bluenimble.platform.db;

import java.util.List;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.Query;

@Feature ( name = "database" )
public interface Database extends Recyclable {
	
	interface Field {

		enum Type {
			Byte,
			Boolean,
			Integer,
			Short,
			Long,
			Float,
			Double,
			Decimal,
			String,
			Date,
			DateTime,
			Binary
		}
		
		String 	name 		();
		Type 	type 		();
		boolean required 	();
		boolean unique 		();
		
	}
	
	interface Visitor {
		boolean optimize ();
		boolean onRecord (DatabaseObject dbo);
	}
	
	enum IndexType {
		Unique,
		NotUnique,
		Text,
		Spacial
	}
	
	enum ExchangeOption {
		info,
        entities,
        schema,
        indexes,
        ids
	}

	interface Fields {
		String Entity 		= "$entity";
		String Id 			= "id";
		String Timestamp 	= "timestamp";
		String Total 		= "totalCount";
	}
	
	interface Keywords {
		String Set 			= "set";
		String Unset 		= "unset";
	}
	
	interface Proprietary {
		String Database = "database";
	}
	
	Object 					proprietary (String name);
	long 					size 		() 														throws DatabaseException;

	Database 				trx 		();
	Database 				commit 		() 														throws DatabaseException;
	Database 				rollback 	()														throws DatabaseException;
	
	void 					createEntity(String entity, Field... fields)						throws DatabaseException;

	DatabaseObject 			create 		(String entity) 										throws DatabaseException;
	List<DatabaseObject>	createList 	() 														throws DatabaseException;

	void 					clear 		(String entity) 										throws DatabaseException;

	DatabaseObject			get 		(String entity, Object id) 								throws DatabaseException;
	
	int 					delete 		(String entity, Object id) 								throws DatabaseException;
	int 					delete 		(String entity, Query query) 							throws DatabaseException;

	long 					update 		(String entity, Query query, JsonObject data) 			throws DatabaseException;

	JsonObject 				bulk 		(JsonObject data) 										throws DatabaseException;

	boolean 				isEntity 	(Object value) 											throws DatabaseException;

	List<DatabaseObject> 	find 		(String entity, Query query, Visitor visitor) 			throws DatabaseException;
	DatabaseObject 			findOne 	(String entity, Query query) 							throws DatabaseException;
	
	List<DatabaseObject> 	pop 		(String entity, Query query, Visitor visitor) 			throws DatabaseException;
	DatabaseObject 			popOne 		(String entity, Query query) 							throws DatabaseException;
	
	int						increment 	(DatabaseObject obj, String field, int value) 			throws DatabaseException;
	
	long 					count 		(String entity) 										throws DatabaseException;
	
	JsonObject 				describeEntity 	
										(String enity) 											throws DatabaseException;
	
	JsonObject 				describe 	() 														throws DatabaseException;
	
}