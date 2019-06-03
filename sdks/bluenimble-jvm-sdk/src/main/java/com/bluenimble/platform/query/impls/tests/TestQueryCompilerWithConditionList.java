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

import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.query.CompiledQuery;
import com.bluenimble.platform.query.Query;
import com.bluenimble.platform.query.QueryCompiler;
import com.bluenimble.platform.query.impls.JsonQuery;
import com.bluenimble.platform.query.impls.SqlQueryCompiler;

public class TestQueryCompilerWithConditionList {

	public static void main (String [] args) throws Exception {
		
		Query query = new JsonQuery (new JsonObject ("{ where = {\n" + 
				"				or: [{\n" + 
				"					'createdBy.id': '5ca3cdb9894e04330ad8bbf9',\n" + 
				"					'recipient.id': '5ca3cf2b894e04330ad8bbfb'\n" + 
				"				}, {\n" + 
				"					'createdBy.id': '5ca3cf2b894e04330ad8bbfb',\n" + 
				"					'recipient.id': '5ca3cdb9894e04330ad8bbf9'\n" + 
				"				}] \n" + 
				"			} }") );
		System.out.println ("Select==>");

		QueryCompiler sc = new SqlQueryCompiler (Query.Construct.select);
		
		CompiledQuery cq = sc.compile (query);
		
		System.out.println ("   query: " + cq.query ());
		System.out.println ();
		System.out.println ("bindings: " + cq.bindings ());
		
	}
	
}
