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
package com.bluenimble.platform.cli.printing;

import com.bluenimble.platform.Lang;

public abstract class AbstractPrinter implements Printer {

	private static final long serialVersionUID = 2496594448833876760L;
	
	protected int 		margin = 4;
	protected boolean 	on = true;
	
	public 	void info 		(String msg, Object...args) {
		message 	(Label.Info.name (), msg, args);
	}

	public 	void success 		(String msg, Object...args) {
		message 	(Label.Success.name (), msg, args);
	}

	public 	void warning 	(String msg, Object...args) {
		message 	(Label.Warning.name (), msg, args);
	}

	public 	void error 		(String msg, Object...args) {
		message 	(Label.Error.name (), msg, args);
	}

	public 	void note 		(String msg, Object...args) {
		message 	(Label.Note.name (), msg, args);
	}

	public 	void important 	(String msg, Object...args) {
		message 	(Label.Important.name (), msg, args);
	}

	@Override
	public void off () {
		on = false;
	}

	@Override
	public void on () {
		on = true;
	}
	
	@Override
	public boolean isOn () {
		return on;
	}
	

	@Override
	public void fprint (String spec) {
		
		int margin = -1 * this.margin;
		
		// __PS__ B/COLOR1:text1 _|_ COLOR2/F:text2 _|_ text3
		
		if (!spec.startsWith (PrintSpec.Start)) {
			text (margin, spec, null, null);
			return;
		} 
		
		spec = spec.substring (PrintSpec.Start.length ());
		
		String [] chunks = Lang.split (spec, PrintSpec.Split);
		
		for (String c : chunks) {
			int indexOfText = c.indexOf (PrintSpec.TextSep);
			if (indexOfText <= 0) {
				text (margin, c, null, null);
				continue;
			}
			
			String fcolor 	= c.substring (0, indexOfText);
			String text 	= c.substring (indexOfText + PrintSpec.TextSep.length ());
			
			String bcolor = null;
			
			int indexOfColor = fcolor.indexOf (PrintSpec.ColorSep);
			if (indexOfColor > 0) {
				String cStyle = fcolor.substring (0, indexOfColor).toUpperCase ();
				if (cStyle.equals (PrintSpec.Style.Background)) {
					bcolor = fcolor.substring (indexOfColor + PrintSpec.ColorSep.length ()).toUpperCase ();
					fcolor = null;
				} else {
					fcolor = fcolor.substring (indexOfColor + PrintSpec.ColorSep.length ()).toUpperCase ();
				}
			}
			
			text (margin, text, fcolor.trim (), bcolor);
			
		}
		
	}
	
}
