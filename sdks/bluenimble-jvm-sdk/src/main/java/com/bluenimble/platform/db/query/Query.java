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
package com.bluenimble.platform.db.query;

import java.io.Serializable;
import java.util.Map;

public interface Query extends Serializable {

	enum Construct {
		select,
		update,
		insert,
		delete,
		where,
		orderBy,
		groupBy,
		having
	}
	
	enum Conjunction {
		or,
		and
	}
	
	enum Operator {
		eq,
		neq,
		gt,
		lt,
		gte,
		lte,
		like,
		nlike,
		btw,
		nbtw,
		in,
		nin,
		nil,
		nnil,
		ftq,
		within,
		near
	}
	
	String				name 		();

	String				entity 		();
	void				entity 		(String entity);
	
	Construct			construct 	();

	Caching				caching		();
	Select 				select 		();
	Where 				where 		();
	GroupBy 			groupBy 	();
	OrderBy 			orderBy 	();
	Having 				having 		();

	int 				start 		();
	int 				count 		();
	void 				count 		(int page);
	
	Map<String, Object>	bindings 	();
	
}
