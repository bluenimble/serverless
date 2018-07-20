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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.templating.Expression;
import com.bluenimble.platform.templating.Node;
import com.bluenimble.platform.templating.ScriptNode;
import com.bluenimble.platform.templating.TextNode;
import com.bluenimble.platform.templating.ValueConverter;
import com.bluenimble.platform.templating.VariableNode;
import com.bluenimble.platform.templating.VariableResolver;
import com.bluenimble.platform.templating.impls.converters.JsonValueConverter;

public class DefaultExpression implements Expression {

	private static final long serialVersionUID = 7705447418999628814L;
	
	private static final String CastSeparator = ">>";
	
	private static final Map<String, ValueConverter> Converters = new HashMap<String, ValueConverter> ();
	static {
		Converters.put (Type.Json.name ().toLowerCase (), new JsonValueConverter ());
	}
	
	private String 		type;
	private String 		spec;
	
	private List<Node> 	nodes;
	
	@Override
	public String prepare (String expression) {
		
		int indexOfCast = expression.lastIndexOf (CastSeparator);
		if (indexOfCast <= 0) {
			return expression;
		}
		
		String cast = expression.substring (indexOfCast + CastSeparator.length ()).trim ();
		
		expression 	= expression.substring (0, indexOfCast);
		
		String [] typeAndSpec = Lang.split (cast, Lang.SPACE, true);
		
		String sType = typeAndSpec [0];
		
		// resolve type & spec
		try {
			this.type = sType.toLowerCase ();
		} catch (Exception ex) {
			// ignore
		}
		
		if (typeAndSpec.length > 1) {
			this.spec = typeAndSpec [1];
		}
		
		return expression;
	}

	@Override
	public void node (Node node) {
		if (node instanceof TextNode && Lang.BLANK.equals (((TextNode)node).token ())) {
			return;
		}
		if (nodes == null) {
			nodes = new ArrayList<Node> ();
		}
		nodes.add (node);
	}

	@Override
	public Node node (int index) {
		if (nodes == null) {
			return null;
		}
		return nodes.get (index);
	}

	@Override
	public int nodes () {
		if (nodes == null) {
			return 0;
		}
		return nodes.size ();
	}

	@Override
	public Object eval (VariableResolver vr) {
		if (nodes == null) {
			return null;
		}
		
		Object value = null;
		
		if (nodes.size () == 1) {
			if (node (0) instanceof VariableNode) {
				value = evalVarNode ((VariableNode)node (0), vr);
			} else if (node (0) instanceof ScriptNode) {
				value = evalScriptNode ((ScriptNode)node (0), vr);
			} else {
				value = node (0).token ();
			}
		} else {
			StringBuilder sb = new StringBuilder ();
			for (int i = 0; i < nodes (); i ++) {
				Node node = node (i);
				if (node instanceof TextNode) {
					sb.append (node.token ());
				} else if (node instanceof VariableNode) {
					sb.append (evalVarNode ((VariableNode)node, vr));
				} else if (node instanceof ScriptNode) {
					sb.append (evalScriptNode ((ScriptNode)node, vr));
				}
			}
			
			value = sb.toString ();
			
			sb.setLength (0);
			sb = null;
		}
		
		if (type != null) {
			ValueConverter c = Converters.get (type);
			if (c != null) {
				return c.convert (value, spec);
			}
		}
		
		return value;
	}
	
	private Object evalVarNode (VariableNode vNode, VariableResolver vr) {
		List<VariableNode.Property> properties = vNode.vars ();
		
		Object value = null;
		
		for (VariableNode.Property p : properties) {
			if (p.value () != null) {
				value = p.value ();
				break;
			}
			value = vr.resolve (p.namespace (), p.keys ());
			if (value != null) {
				break;
			}
		}
		if (value == null) {
			value = vNode.token ();
		}
		
		return value;
		
	}

	private Object evalScriptNode (ScriptNode sNode, VariableResolver vr) {
		try {
			return sNode.eval (vr);
		} catch (ScriptException e) {
			throw new RuntimeException (e.getMessage (), e);
		}
	}
	
}
