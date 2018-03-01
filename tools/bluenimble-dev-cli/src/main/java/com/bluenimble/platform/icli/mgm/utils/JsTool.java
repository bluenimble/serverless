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
package com.bluenimble.platform.icli.mgm.utils;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.printing.Panel;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;

public class JsTool {
	
	private Tool tool;
	
	public JsTool (Tool tool) {
		this.tool = tool;
	}
	
	public void command (String cmd) throws Exception {
		tool.processCommand (cmd);
	}
	
	public void text (int margin, String text, String fc, String bc) {
		tool.printer ().text (margin, text, fc, bc);
	}
	
	public void textLn (int margin, String text, String fc, String bc) {
		tool.printer ().textLn (margin, text, fc, bc);
	}
	
	public void message (String title, String text) {
		tool.printer ().message (title, text);
	}
	public void error (String text) {
		tool.printer ().error (text);
	}
	public void info (String text) {
		tool.printer ().info (text);
	}
	public void warning (String text) {
		tool.printer ().warning (text);
	}
	public void note (String text) {
		tool.printer ().note (text);
	}
	public void important (String text) {
		tool.printer ().important (text);
	}
	public void content (String title, String text) {
		tool.printer ().content (title, text);
	}
	public void panel (Panel panel) {
		tool.printer ().panel (panel);
	}	
	public JsonObject JsonObject () {
		return new JsonObject ();
	}
	public JsonArray JsonArray () {
		return new JsonArray ();
	}
	
}
