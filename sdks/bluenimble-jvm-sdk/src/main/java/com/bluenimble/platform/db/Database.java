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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.json.JsonObject;

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
	
	interface ExchangeListener {
		void onMessage (String message);
	} 

	void 					trx 		();
	void 					commit 		() 														throws DatabaseException;
	void 					rollback 	()														throws DatabaseException;

	void 					createEntity(String entity, Field... fields) 						throws DatabaseException;
	DatabaseObject 			create 		(String entity) 										throws DatabaseException;

	void 					drop 		(String entity) 										throws DatabaseException;

	void 					createIndex (String entity, IndexType type, String name, Field... fileds) 
																								throws DatabaseException;
	void 					dropIndex 	(String entity, String name) 
																								throws DatabaseException;

	DatabaseObject			get 		(String entity, Object id) 								throws DatabaseException;
	
	int 					delete 		(String entity, Object id) 								throws DatabaseException;
	int 					delete 		(Query query) 											throws DatabaseException;

	void 					add 		(DatabaseObject parent, String collection, DatabaseObject child) 		throws DatabaseException;
	void 					remove 		(DatabaseObject parent, String collection, DatabaseObject child) 		throws DatabaseException;
	
	JsonObject 				bulk 		(JsonObject data) 										throws DatabaseException;

	void 					imp 		(Set<String> entities, 
		InputStream source, Map<ExchangeOption, Boolean> options, ExchangeListener listener) 	throws DatabaseException;
	void 					exp 		(Set<String> entities, 
			OutputStream source, Map<ExchangeOption, Boolean> options, ExchangeListener listener) 	
																								throws DatabaseException;

	void 					schedule 	(String event, Query query, String cron) 				throws DatabaseException;
	void 					unschedule 	(String event) 											throws DatabaseException;
	
	boolean 				isEntity 	(Object value) 											throws DatabaseException;

	List<DatabaseObject> 	find 		(String name, Query query, Visitor visitor) 			throws DatabaseException;
	DatabaseObject 			findOne 	(String name, Query query) 								throws DatabaseException;
	
	List<DatabaseObject> 	pop 		(String name, Query query, Visitor visitor) 			throws DatabaseException;
	DatabaseObject 			popOne 		(String name, Query query) 								throws DatabaseException;
	
	int						increment 	(DatabaseObject obj, String field, int value) 			throws DatabaseException;
	
	long 					count 		(String entity) 										throws DatabaseException;
	
	JsonObject 				describe 	() 														throws DatabaseException;
	
}
