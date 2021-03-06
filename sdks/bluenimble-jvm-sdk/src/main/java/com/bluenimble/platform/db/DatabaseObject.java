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

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.reflect.beans.BeanSerializer;

public interface DatabaseObject extends Serializable {

	String 		entity 			();

	Object 		getId 			();
	void 		setId 			(Object id);
	
	Date 		getTimestamp 	();
	
	void 		set 			(String key, Object value)	throws DatabaseException;
	Object 		get 			(String key);
	
	/*
	List<DatabaseObject> 	
				find 			(String field, Query query, Visitor visitor) 			
															throws DatabaseException;
	*/
	void 		load 			(JsonObject values)			throws DatabaseException;

	void 		remove 			(String key)				throws DatabaseException;
	void 		clear 			();
	
	Iterator<String>	
				keys 			();
	
	JsonObject 	toJson 			(BeanSerializer serializer);
	
	boolean		has 			(String key);
	
	void		save 			() 							throws DatabaseException;
	
	void		delete 			()							throws DatabaseException;
	
	void		useDefaultFields(boolean useDefaultFields);

}