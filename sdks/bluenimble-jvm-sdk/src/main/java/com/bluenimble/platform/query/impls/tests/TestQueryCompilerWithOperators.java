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
package com.bluenimble.platform.query.impls.tests;

import java.io.File;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.query.CompiledQuery;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.QueryCompiler;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.query.impls.SqlQueryCompiler;

public class TestQueryCompilerWithOperators {

	public static void main (String [] args) throws Exception {
		
		Query query = new JsonQuery (Json.load (new File ("tests/queries/with-operators.json")));
		
		System.out.println ("Select==>");

		QueryCompiler sc = new SqlQueryCompiler (Query.Construct.select);
		
		CompiledQuery cq = sc.compile (query);
		
		System.out.println ("   query: " + cq.query ());
		System.out.println ();
		System.out.println ("bindings: " + cq.bindings ());
		
		System.out.println ("Delete==>");

		QueryCompiler dc = new SqlQueryCompiler (Query.Construct.delete);
		
		cq = dc.compile (query);
		
		System.out.println ("   query: " + cq.query ());
		System.out.println ();
		System.out.println ("bindings: " + cq.bindings ());
		
	}
	
}
