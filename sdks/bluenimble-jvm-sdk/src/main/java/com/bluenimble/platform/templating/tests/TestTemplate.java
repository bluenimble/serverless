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
package com.bluenimble.platform.templating.tests;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.templating.SimpleVariableResolver;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.DefaultExpressionCompiler;

public class TestTemplate {
	
	public static void main (String [] args) {
	
		String exp1 = "hello dudes";
		
		String exp2 = "Hello [model.c]";
		
		String exp3 = "[model.b] done";

		String exp4 = "simple text";
		
		String exp5 = "simple [model.b] [alpha.a] [beta | 'alpha']";

		String exp6 = "{ price: '[model.b]' }>>json";
		
		String exp7 = "[ model.alpha | '' ]>>json";

		final JsonObject model = (JsonObject)new JsonObject ().set ("a", "A Value").set ("b", 409).set ("c", "Hello");
		
		VariableResolver vr = new SimpleVariableResolver () {
			private static final long serialVersionUID = -485939153491337463L;

			@Override
			public Object resolve (String namespace, String... property) {
				System.out.println (namespace);
				if (namespace == null) {
					return null;
				}
				System.out.println (Lang.join (property, Lang.DOT));
				if (namespace == null || namespace.equals ("model")) {
					return Json.find (model, property);
				}
				return null;
			}
			
		};
		
		DefaultExpressionCompiler compiler = new DefaultExpressionCompiler ();
		
		System.out.println ("exp1: " + compiler.compile (exp1, null).eval (vr));
		System.out.println ("exp2: " + compiler.compile (exp2, null).eval (vr));
		System.out.println ("exp3: " + compiler.compile (exp3, null).eval (vr));
		System.out.println ("exp4: " + compiler.compile (exp4, null).eval (vr));
		System.out.println ("exp5: " + compiler.compile (exp5, null).eval (vr));
		System.out.println ("exp6: " + compiler.compile (exp6, null).eval (vr));
		System.out.println ("exp7: " + compiler.compile (exp7, null).eval (vr));
		

	}
	
}
