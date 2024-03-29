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

// native imports 
var System 			= native ('java.lang.System');
var String 			= native ('java.lang.String');
var Lang 			= native ('com.bluenimble.platform.Lang');
var Json 			= native ('com.bluenimble.platform.Json');
var JsonObject 		= native ('com.bluenimble.platform.json.JsonObject');
var JsonArray 		= native ('com.bluenimble.platform.json.JsonArray');

//check if valid command args
if (typeof Command === 'undefined') {
	throw 'missing command arguments. eg. run case|api [ApiCallsJsonVar or apiNs required] [FlowJsonVar required]';
}

var tokens = Lang.split (Command, ' ', true);

if (tokens.length < 2) {
	throw 'missing command arguments. eg. run case|api ApiCallsVar|apiNs FlowJsonVar';
}

var target 		= tokens [0];
if (target != 'case' && target != 'api') {
	throw 'invalid target ' + target + '. Possible targets: [case] or [api]';
}

var callsVar 	= tokens [1];
var caseVar 	= tokens [2];

if (target == 'api') {
	var headers = Vars ['remote.headers'];
	if (!headers) {
		Vars['remote.headers'] = new JsonObject ();
		headers = Vars ['remote.headers'];
	}
	var currentAccept = headers.Accept;
	headers.Accept = 'application/spec.bnb.calls';
	Tool.command ('desc api ' + callsVar + ' all >> ' + callsVar + '_calls');
	callsVar = callsVar + '_calls';
	headers.Accept = currentAccept;
}

var oCalls = Vars [callsVar];
if (!oCalls) {
	throw 'variable ' + callsVar + ' not found';
}

if (!(oCalls instanceof JsonObject)) {
	throw 'variable ' + callsVar + " isn't a valid json object";
}

if (!oCalls.calls || !(oCalls.calls instanceof JsonArray)) {
	throw 'property ' + callsVar + ".calls doesn't exist or it isn't a valid json array";
}

var oCase = Vars [caseVar];
if (!oCase) {
	var aSteps = caseVar.split ('>');
	oCase = new JsonObject ().set ('flow', new JsonArray ());
	for (var i = 0; i < aSteps.length; i++) {
		oCase.flow.add (new JsonObject ().set ('call', aSteps [i].trim ()));
	}
}

if (!(oCase instanceof JsonObject)) {
	throw 'variable ' + caseVar + " isn't a valid json object";
}

if (!oCase.flow || !(oCase.flow instanceof JsonArray)) {
	throw 'property ' + caseVar + ".flow doesn't exist or it isn't a valid json array";
}

Tool.note ('Run (' + caseVar + ') ' + (target == 'case' ? ('using spec ' + callsVar) : ('for api ' + tokens [1])) + ' > # flows (' + oCase.flow.count () + ')');

for (var i = 0; i < oCase.flow.count (); i++) {
	var step = oCase.flow.get (i);
	if (!(step instanceof JsonObject)) {
		throw 'flow step ' + i + "isn't a valid json object";
	}
	if (!step.call) {
		throw 'flow step ' + i + ".call isn't defined";
	}
	
	Tool.command ('echo off');
	Tool.command ('json search ' + callsVar + ' calls/request.service ' + step.call + ' >> FoundCalls');
	Tool.command ('json get FoundCalls items.0 >> CurrentCall');
	Tool.command ('echo on');
	
	var oCall = Vars['CurrentCall'];
	if (!oCall) {
		throw 'flow step ' + i + ". Call " + step.call + ' not found in ' + callsVar;
	}
	oCall = oCall.duplicate ();
    oCall.shrink ();
    
    // replace
    Vars['CurrentCall'] = oCall;
	
    Tool.note ('  Prepare Call: ' + step.call);
    
	// set data
	if (step.set && (step.set instanceof JsonObject)) {
		for (var k in step.set) {
			Tool.note ('    Set ' + k + ' > ' + step.set [k]);
			Json.set (oCall, k, step.set [k]);
		}
	}
	
	Tool.content ("__PS__Sending '_|_ YELLOW:" + step.call + "_|_' request ...", oCall);
	Tool.command ('http ' + oCall.request.method + ' CurrentCall >> CurrentOut');
	
	var out = Vars['CurrentOut'];
	
	if (step.out && (step.out instanceof JsonObject)) {
		
		for (var k in step.out) {
			var oOut;
			var vOut = step.out [k];
			if (vOut instanceof String) {
				if (vOut == '*') {
					oOut = out;
				} else {
					oOut = Json.find (out, vOut.split ('.'));
				}
			} else if (vOut instanceof JsonArray) {
				oOut = new JsonObject ();
				for (var j = 0; j < vOut.length; j++) {
					Json.set (oOut, k, Json.find (out, vOut [j].split ('.')));
				}
			}
			Vars [k] = oOut;
			Tool.note ('    Var > ' + k + ' > ' + oOut);
		}
	}
}

// remove temporary variables
Vars.remove (callsVar + '_calls');
Vars.remove ('FoundCalls');
Vars.remove ('CurrentCall');
Vars.remove ('CurrentOut');
