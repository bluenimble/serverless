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
package com.bluenimble.platform.templating.impls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.templating.Expression;
import com.bluenimble.platform.templating.ExpressionCompiler;
import com.bluenimble.platform.templating.TextNode;
import com.bluenimble.platform.templating.VariableNode;

public class DefaultExpressionCompiler implements ExpressionCompiler {
	
	private static final long serialVersionUID = 4204895240965684512L;
	
	protected int 		cacheSize 		= 50;
	
	protected char 		expStart 		= '[';
	protected char 		expEnd 			= ']';
	
	public DefaultExpressionCompiler () {
		
	}
	
	public DefaultExpressionCompiler (int cacheSize, char expStart, char expEnd) {
		this (expStart, expEnd);
		this.cacheSize 	= cacheSize;
	}
	
	public DefaultExpressionCompiler (char expStart, char expEnd) {
		this.expStart 	= expStart;
		this.expEnd		= expEnd;
	}
	
	protected Map<String, Expression> cached; 
	
	@Override
	public Expression compile (String text, String id) {
		if (Lang.isNullOrEmpty (text)) {
			return null;
		}
		
		String key = id == null ? text : id;
		if (cached != null && cached.containsKey (key)) {
			return cached.get (key);
		}
		
		// compile
		Expression expression = compile (text);
		
		// cache
		if (cacheSize > 0 && expression != null) {
			if (cached == null) {
				cached = new ConcurrentHashMap<String, Expression> ();
			}

			if (cached.size () < cacheSize) {
				cached.put (key, expression);
			}
		}
		
		return expression;
	}
	
	private Expression compile (String text) {
		
		Expression expression = new DefaultExpression ();
		
		text = expression.prepare (text);
		
		int indexOfStart = text.indexOf (expStart);
		if (indexOfStart < 0) {
			expression.node (new TextNode (text));
			return expression;
		} else {
			// create a text node for the starting part of the text 
			expression.node (new TextNode (text.substring (0, indexOfStart)));
			text = text.substring (indexOfStart + 1);
		}
		
		while (indexOfStart > -1) {
			int indexOfEnd = text.indexOf (expEnd);
			if (indexOfEnd <= -1) {
				expression.node (new TextNode (expStart + text));
				return expression;
			}
			
			// add a var node
			String var = text.substring (0, indexOfEnd);
			expression.node (new VariableNode (var));
			
			text = text.substring (indexOfEnd + 1);

			indexOfStart = text.indexOf (expStart);
			
			// add text before var
			if (indexOfStart < 0) {
				expression.node (new TextNode (text));
				break;
			} else {
				expression.node (new TextNode (text.substring (0, indexOfStart)));
				text = text.substring (indexOfStart + 1);
			}
			
		}
		
		return expression;
	}
	
	public DefaultExpressionCompiler cacheSize (int cacheSize) {
		this.cacheSize = cacheSize;
		return this;
	}

}
