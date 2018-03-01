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
package com.bluenimble.platform.db.query.impls;

import com.bluenimble.platform.db.query.Condition;
import com.bluenimble.platform.db.query.Query;
import com.bluenimble.platform.db.query.Query.Operator;
import com.bluenimble.platform.json.JsonObject;

public class ConditionImpl implements Condition {

	private static final long serialVersionUID = 3510682856493272840L;
	
	private String field;
	private Query.Operator operator;
	private Object value;
	
	public ConditionImpl (String field, Query.Operator operator, Object value) {
		this.field 		= field;
		this.operator 	= operator;
		if (value instanceof JsonObject) {
			value = new JsonQuery ((JsonObject)value);
		}
		this.value 		= value;
	}

	@Override
	public String field () {
		return field;
	}

	@Override
	public Object value () {
		return value;
	}

	@Override
	public Operator operator () {
		return operator;
	}
	
}
