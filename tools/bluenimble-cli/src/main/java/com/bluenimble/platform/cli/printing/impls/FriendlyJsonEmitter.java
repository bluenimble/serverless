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
package com.bluenimble.platform.cli.printing.impls;

import java.util.HashSet;
import java.util.Set;

import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.json.AbstractEmitter;
import com.bluenimble.platform.json.JsonEmitter;
import com.bluenimble.platform.json.JsonEntity;
import com.bluenimble.platform.json.JsonObject;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class FriendlyJsonEmitter extends AbstractEmitter {
	
	private static final String Status 		= "status";
	
	private static final Set<String> YellowStatus = new HashSet<String> ();
	static {
		YellowStatus.add ("runnable");
		YellowStatus.add ("stopped");
		YellowStatus.add ("paused");
	}
	
	private static final Set<String> GreenStatus = new HashSet<String> ();
	static {
		GreenStatus.add ("running");
		GreenStatus.add ("available");
	}
	
	private static final Set<String> RedStatus = new HashSet<String> ();
	static {
		RedStatus.add ("timed_waiting");
		RedStatus.add ("blocked");
		RedStatus.add ("failed");
	}
	
	private FontPrinter fontPrinter;
	private Tool tool;
	
	public FriendlyJsonEmitter (Tool tool) {
		this.tool = tool;
		fontPrinter = tool.printer ().getFontPrinter ();
		prettify ();
		tab ("  ");
	}
	
	public JsonEmitter write (String text) {
		tool.write (text);
		return this;
	} 
	
	@Override
	public void onValue (JsonEntity p, String name, Object value) {
		
		if (value == null || fontPrinter == null) {
			super.onValue (p, name, value);
			return;
		}
		
		String color = FColor.CYAN.name ();
		if (Status.equals (name)) {
			String status = value.toString ().toLowerCase ();
			if (RedStatus.contains (status)) {
				color = FColor.RED.name ();
			} else if (GreenStatus.contains (status)) {
				color = FColor.GREEN.name ();
			} else if (YellowStatus.contains (status)) {
				color = FColor.YELLOW.name ();
			} 
		}
		tool.printer ().text (-1, Lang.QUOT + Json.escape (String.valueOf (value)) + Lang.QUOT, color, null);
	}

	@Override
	protected void onEndLn () {
		super.onEndLn ();
		tool.printer ().margin (3);
	}

	@Override
	public void onStartObject (JsonObject o, boolean root) {
		if (root) {
			tool.printer ().margin (3);
		} 
		super.onStartObject (o, root);
	}

}
