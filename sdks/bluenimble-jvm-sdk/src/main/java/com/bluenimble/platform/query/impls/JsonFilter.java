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
package com.bluenimble.platform.query.impls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.Filter;
import com.bluenimble.platform.query.Query.Operator;

public class JsonFilter implements Filter {

	private static final long serialVersionUID = -4482690420390364506L;
	
	protected JsonObject 	source;
	
	public JsonFilter (JsonObject source) {
		this.source = source;
	}

	@Override
	public Iterator<String> conditions () {
		if (source == null) {
			return null;
		}
		return source.keys ();
	}

	@Override
	public Object get (String field) {
		Object o = source.get (field);
		if (o instanceof JsonArray) {
			JsonArray aConditions = (JsonArray)o;
			List<Object> conditions = new ArrayList<Object> ();
			for (int i = 0; i < aConditions.count (); i++) {
				Object subCondition = aConditions.get (i);
				if (!(subCondition instanceof JsonObject)) {
					continue;
				}
				conditions.add (new JsonFilter ((JsonObject)subCondition));
			}
			return conditions;
		}
		return conditionFor (field, o);
	}
	
	private Object conditionFor (String field, Object o) {
		if (!(o instanceof JsonObject)) {
			return new ConditionImpl (field, Operator.eq, o);
		}
		JsonObject spec = (JsonObject)o;
		return new ConditionImpl (
			field, 
			Operator.valueOf (Json.getString (spec, JsonQuery.Spec.Operator, Operator.eq.name ())), 
			spec.get (JsonQuery.Spec.Value)
		);
	}

	@Override
	public void set (String field, Operator operator, Object value) {
		source.set (field, new JsonObject ().set (JsonQuery.Spec.Operator, operator.name ()).set (JsonQuery.Spec.Value, value));
	}

	@Override
	public int count () {
		if (source == null) {
			return 0;
		}
		return source.size ();
	}
	
	@Override
	public boolean isEmpty () {
		if (source == null) {
			return true;
		}
		return source.isEmpty ();
	}
	
}
