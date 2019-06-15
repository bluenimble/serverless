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
package com.bluenimble.platform.server.visitors.impls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.ApiSpace;
import com.bluenimble.platform.api.impls.SelectiveApiRequestVisitor;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.server.ApiServer;
import com.bluenimble.platform.server.visitors.impls.actions.AppendAction;
import com.bluenimble.platform.server.visitors.impls.actions.BypassAction;
import com.bluenimble.platform.server.visitors.impls.actions.PrependAction;
import com.bluenimble.platform.server.visitors.impls.actions.ReplaceAction;
import com.bluenimble.platform.server.visitors.impls.actions.ResponseAction;
import com.bluenimble.platform.server.visitors.impls.actions.RewriteAction;
import com.bluenimble.platform.server.visitors.impls.checkers.ContainsConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.EndsConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.IsConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.IsntConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.RegExConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.RewriteConditionChecker;
import com.bluenimble.platform.server.visitors.impls.checkers.StartsConditionChecker;

/**
 * Rewrite example @ node level or space runtime
 * 
        "rewrite": {
            "path": {
                "rules": [{
                    "if": "contains: /uber-v1", "methods": ["OPTIONS"], "then": { "replace": "/uber" }
                }]
            }
        }
 * 
 **/
public class RewriteApiRequestVisitor extends SelectiveApiRequestVisitor {

	private static final long serialVersionUID = 1782406079539122227L;
	
	interface Spec {
		String Rewrite	= "rewrite";
		String Rules	= "rules";
		
		String Endpoint	= "endpoint";
		String Path		= "path";

		String If 		= "if";
		String Condition= "condition";
		String Scope 	= "scope";
		String Then 	= "then";
		String Negate 	= "negate";
		String Methods 	= "methods";
		
		interface Checkers {
			String Isnt 	= "isnt";
			String Is 		= "is";
			String Starts 	= "starts";
			String Ends 	= "ends";
			String Contains = "contains";
			String RegEx 	= "regex";
		}
		interface Actions {
			String Bypass	= "bypass";
			String Response	= "response";
			
			String Append 	= "append";
			String Prepend 	= "prepend";
			String Replace 	= "replace";
		}
	}
	
	private static final Map<String, RewriteConditionChecker> Checkers = new HashMap<String, RewriteConditionChecker> ();
	static {
		Checkers.put (Spec.Checkers.Is, new IsConditionChecker ());
		Checkers.put (Spec.Checkers.Isnt, new IsntConditionChecker ());
		Checkers.put (Spec.Checkers.Starts, new StartsConditionChecker ());
		Checkers.put (Spec.Checkers.Ends, new EndsConditionChecker ());
		Checkers.put (Spec.Checkers.Contains, new ContainsConditionChecker ());
		Checkers.put (Spec.Checkers.RegEx, new RegExConditionChecker ());
	}
	
	private static final Map<String, RewriteAction> Actions = new HashMap<String, RewriteAction> ();
	static {
		Actions.put (Spec.Actions.Bypass, new BypassAction ());
		Actions.put (Spec.Actions.Response, new ResponseAction ());

		Actions.put (Spec.Actions.Append, new AppendAction ());
		Actions.put (Spec.Actions.Prepend, new PrependAction ());
		Actions.put (Spec.Actions.Replace, new ReplaceAction ());
	}
	
	protected ApiServer server;
	
	@Override
	protected String [] endpoint (ApiRequest request, String [] endpoint) {
		JsonObject oRewrite = pickRewrite (request, Spec.Endpoint);
		if (oRewrite == null) {
			return endpoint;
		}
		
		// process rules
		String [] rewriten = applyRewrite (request, Placeholder.endpoint, oRewrite, endpoint);
		if (rewriten != null) {
			return rewriten;
		}
		
		return endpoint;
	}
	
	@Override
	protected String [] path (ApiRequest request, String [] path) {
		
		//server.tracer ().log (Level.Info, "Rewrite with Res.Value {0}", Lang.join (path, Lang.SLASH));
		
		JsonObject oRewrite = pickRewrite (request, Spec.Path);
		if (oRewrite == null) {
			return path;
		}
		
		//server.tracer ().log (Level.Info, "Rewrite Spec {0}", oRewrite);

		// process rules
		String [] rewriten = applyRewrite (request, Placeholder.path, oRewrite, path);
		if (rewriten != null) {
			return rewriten;
		}
		
		return path;
	}
	
	private JsonObject pickRewrite (ApiRequest request, String target) {
		String spaceNs = request.getSpace ();
		
		// at the node level
		if (Lang.isNullOrEmpty (spaceNs)) {
			return (JsonObject)Json.find (spec, Spec.Rewrite, target);
		}
		
		// space ns resolved
		
		ApiSpace space = null;
		try {
			space = server.space (spaceNs);
		} catch (Exception ex) {
			// ignore. Space not found
		}	
		if (space == null) {
			return null;
		}
		
		Object rewrite = space.getRuntime (Spec.Rewrite);

		//server.tracer ().log (Level.Info, "Space Rewrite {0}", rewrite);
		
		if (rewrite == null || !(rewrite instanceof JsonObject)) {
			return null;
		}
		
		JsonObject oRewrite = (JsonObject)rewrite;
		if (Json.isNullOrEmpty (oRewrite)) {
			return null;
		}
		
		JsonObject oTargetRewrite = Json.getObject (oRewrite, target);
		if (Json.isNullOrEmpty (oTargetRewrite)) {
			return null;
		}
		
		return oTargetRewrite;
	}
	
	private String [] applyRewrite (ApiRequest request, Placeholder placeholder, JsonObject oRewrite, String [] aTarget) {
		
		//server.tracer ().log (Level.Info, "\tApply rewrite on ", placeholder);

		JsonArray aRules = Json.getArray (oRewrite, Spec.Rules);
		if (Json.isNullOrEmpty (aRules)) {
			return aTarget;
		}
		
		//server.tracer ().log (Level.Info, "\tFound {0} rules", aRules.count ());

		for (int i = 0; i < aRules.count (); i++) {
			JsonObject oRule = (JsonObject)aRules.get (i);
			aTarget = applyRule (request, placeholder, oRule, aTarget);
		}
		
		return aTarget;
	}
	
	private String [] applyRule (ApiRequest request, Placeholder placeholder, JsonObject rule, String [] aTarget) {
		
		// check methods
		JsonArray methods = Json.getArray (rule, Spec.Methods);
		
		if (!Json.isNullOrEmpty (methods) && !methods.contains (request.getVerb ().name ())) {
			return aTarget;
		}
		
		// check condition
		Object oIf = rule.get (Spec.If);
		
		String condition = null;
		Placeholder scope = placeholder;
		
		if (oIf instanceof JsonObject) {
			condition = Json.getString ((JsonObject)oIf, Spec.Condition);
			scope = Placeholder.valueOf (Json.getString ((JsonObject)oIf, Spec.Scope, placeholder.name ()));
		} else {
			condition = Json.getString (rule, Spec.If);
		}		
		
		//server.tracer ().log (Level.Info, "\tApply rule with condition {0}", condition);

		boolean apply = true;
		
		String conditionValue = null;
		
		if (!Lang.isNullOrEmpty (condition)) {
			int indexOfColon = condition.indexOf (Lang.COLON);
			if (indexOfColon > 0) {
				String checkerId 	= condition.substring (0, indexOfColon).trim ();
				conditionValue 		= condition.substring (indexOfColon + 1).trim ();
				
				RewriteConditionChecker checker = Checkers.get (checkerId);
				server.tracer ().log (Level.Debug, "\tRule checker {0}", checker);
				
				apply = (checker == null) || 
						checker.check (scope.equals (Placeholder.endpoint) ? request.getEndpoint () : request.getPath (), conditionValue);
				
				if (Json.getBoolean (rule, Spec.Negate, false)) {
					apply = !apply;
				}
				
			}
		}
		
		//server.tracer ().log (Level.Info, "\tShould apply actions? {0}", apply);

		if (!apply) {
			return aTarget;
		}
		
		// apply actions
		Object then = rule.get (Spec.Then);
		if (then == null || !(then instanceof JsonObject)) {
			return aTarget;
		}
		
		//server.tracer ().log (Level.Info, "\tApply actions {0}", then);
		
		JsonObject actions = (JsonObject)then;
		if (Json.isNullOrEmpty (actions)) {
			return aTarget;
		}
		
		Iterator<String> actionIds = actions.keys ();
		while (actionIds.hasNext ()) {
			String actionId = actionIds.next ();
			//server.tracer ().log (Level.Info, "\tApply action {0}", actionId);
			RewriteAction action = Actions.get (actionId);
			if (action == null) {
				continue;
			}
			//server.tracer ().log (Level.Info, "\t\t with processor {0}", action);
			aTarget = action.apply (request, placeholder, aTarget, actions.get (actionId), conditionValue);
		}
		
		return aTarget;
	}

	public void setServer (ApiServer server) {
		this.server = server;
	}

}
