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

import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.db.Database;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.Caching;
import com.bluenimble.platform.query.GroupBy;
import com.bluenimble.platform.query.Having;
import com.bluenimble.platform.query.OrderBy;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.Select;
import com.bluenimble.platform.query.Where;

public class JsonQuery implements Query {

	private static final long serialVersionUID = -4482690420390364506L;
	
	public interface Spec {
		String Name			= "name";
		
		String Construct	= "construct";
		
		String Cache		= "cache";
		
		String Start 		= "start";
		String Count 		= "count";
		
		String Operator 	= "op";

		String Value 		= "value";
		
		String Native		= "native";
	}
	
	protected JsonObject 			source;
	protected Map<String, Object> 	bindings;
	
	public JsonQuery (JsonObject source, Map<String, Object> bindings) {
		this.source 	= source;
		this.bindings 	= bindings;
	}

	public JsonQuery (JsonObject source) {
		this (source, null);
	}

	@Override
	public String name () {
		return Json.getString (source, Spec.Name);
	}

	@Override
	public Construct construct () {
		String sConstruct = Json.getString (source, Spec.Construct);
		if (Lang.isNullOrEmpty (sConstruct)) {
			return null;
		}
		try {
			return Construct.valueOf (sConstruct);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public String entity () {
		return Json.getString (source, Database.Fields.Entity);
	}
	@Override
	public void entity (String entity) {
		source.set (Database.Fields.Entity, entity);
	}

	@Override
	public Map<String, Object> bindings () {
		return bindings;
	}

	@Override
	public int start () {
		return Json.getInteger (source, Spec.Start, 0);
	}

	@Override
	public int count () {
		return Json.getInteger (source, Spec.Count, 100);
	}
	@Override
	public void count (int count) {
		source.set (Spec.Count, count);
	}

	@Override
	public Caching caching () {
		return new JsonCaching (Json.getObject (source, Spec.Cache));
	}

	@Override
	public Select select () {
		JsonArray fields = Json.getArray (source, Construct.select.name ());
		if (fields == null) {
			return null;
		}
		return new JsonSelect (fields);
	}

	@Override
	public Where where () {
		JsonObject oWhere = Json.getObject (source, Construct.where.name ());
		if (oWhere == null) {
			return null;
		}
		return new JsonWhere (oWhere);
	}

	@Override
	public GroupBy groupBy () {
		JsonArray aGroupBy = Json.getArray (source, Construct.groupBy.name ());
		if (aGroupBy == null) {
			return null;
		}
		return new JsonGroupBy (aGroupBy);
	}

	@Override
	public OrderBy orderBy () {
		JsonObject oOrderBy = Json.getObject (source, Construct.orderBy.name ());
		if (oOrderBy == null) {
			return null;
		}
		return new JsonOrderBy (oOrderBy);
	}

	@Override
	public Having having () {
		JsonObject oHaving = Json.getObject (source, Construct.having.name ());
		if (oHaving == null) {
			return null;
		}
		return new JsonHaving (oHaving);
	}

	@Override
	public boolean isNative () {
		return Json.getBoolean (source, Spec.Native, false);
	}

	@Override
	public JsonObject toJson () {
		return source;
	}
	
	@Override
	public String toString () {
		if (source == null) {
			return null;
		}
		return source.toString ();
	}

}
