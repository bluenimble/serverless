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

import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.printing.Panel;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class JsTool {
	
	private static final Map<String, FColor> FColors = new HashMap<String, FColor> ();
	static {
		FColors.put (FColor.YELLOW.name (), FColor.YELLOW);
		FColors.put (FColor.RED.name (), FColor.RED);
		FColors.put (FColor.BLUE.name (), FColor.BLUE);
		FColors.put (FColor.CYAN.name (), FColor.CYAN);
		FColors.put (FColor.MAGENTA.name (), FColor.MAGENTA);
		FColors.put (FColor.GREEN.name (), FColor.GREEN);
		FColors.put (FColor.WHITE.name (), FColor.WHITE);
		FColors.put (FColor.BLACK.name (), FColor.BLACK);
	}
	
	private static final Map<String, BColor> BColors = new HashMap<String, BColor> ();
	static {
		BColors.put (BColor.YELLOW.name (), BColor.YELLOW);
		BColors.put (BColor.RED.name (), BColor.RED);
		BColors.put (BColor.BLUE.name (), BColor.BLUE);
		BColors.put (BColor.CYAN.name (), BColor.CYAN);
		BColors.put (BColor.MAGENTA.name (), BColor.MAGENTA);
		BColors.put (BColor.GREEN.name (), BColor.GREEN);
		BColors.put (BColor.WHITE.name (), BColor.WHITE);
		BColors.put (BColor.BLACK.name (), BColor.BLACK);
	}
	
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
	public void success (String text) {
		tool.printer ().success (text);
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
	
	public String styled (String text, String fc, String bc) {
		return tool.printer ().getFontPrinter ().generate (
			text, Attribute.LIGHT, 
			Lang.isNullOrEmpty (fc) ? null : FColors.get (fc.toUpperCase ()), 
			Lang.isNullOrEmpty (bc) ? null : BColors.get (bc.toUpperCase ())
		);
	}
	
	public String styled (String text, String fc) {
		return styled (text, fc, null);
	}
	
	public JsonObject JsonObject () {
		return new JsonObject ();
	}
	public JsonArray JsonArray () {
		return new JsonArray ();
	}
	
	public Tool proxy () {
		return tool;
	}
	
}
