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

import java.text.MessageFormat;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.printing.AbstractPrinter;
import com.bluenimble.platform.cli.printing.Panel;

/*
 * 
  /--+ Helo	
	 |
	 o--+ Sub Hello 
	 |
	 o--+ Sub Hello 	 
 * 
 * 
 * 
 * 
 */

public class DefaultPrinter extends AbstractPrinter {
	
	private static final long serialVersionUID = -1755386137628965295L;
	
	private static final String Circle 	= "o";
	private static final String Tab 	= "    ";
	
	private Tool 	tool;
	private int 	margin = 4;
	
	public DefaultPrinter (Tool tool, int margin) {
		this.tool = tool;
		this.margin = margin;
	}

	@Override
	public void message (String label, String msg, Object... args) {
		if (!on) {
			return;
		}
		tool.writeln (Lang.ENDLN);
		margin ();
		tool.write (label).write (Lang.SPACE).write (Lang.PIPE).write (Lang.SPACE);
		text (label.length () + 3, format (msg, args), true, true);
	}

	@Override
	public void node (int level, String text) {
		if (!on) {
			return;
		}
		if (level < 1) {
			tool.writeln (Lang.ENDLN);
			margin ();
			tool.write (Lang.SLASH);
		} else {
			margin (3 * level);
			tool.writeln (Lang.PIPE);
			
			margin (3 * level);
			tool.write (Lang.PLUS);
		}
		tool.write (Lang.DASH).write (Lang.DASH);
		if (level < 1) {
			tool.write (Lang.GREATER);
		} else {
			tool.write (Lang.PLUS);
		}
		
		tool.write (Lang.SPACE);
		text (3 * level + 5, text, true, true);
	}

	@Override
	public void content (String title, String text) {
		if (!on) {
			return;
		}
		tool.writeln (Lang.ENDLN);
		if (!Lang.isNullOrEmpty (title)) {
			margin ();
			tool.write (Circle).write (Lang.SPACE).writeln (title);
			line (0, title.length () + 4);
		}
		text (2, text, false, true);
	}
	
	@Override
	public void margin (int extra) {
		if (!on) {
			return;
		}
		if (extra < 0) {
			return;
		}
		for (int i = 0; i < margin + extra; i++) { tool.write (Lang.SPACE); }
	}

	@Override
	public void panel (Panel panel) {
		if (!on) {
			return;
		}
		
	}
	
	private void margin () {
		margin (0);
	}
	
	@Override
	public void textLn (int margin, String text, String fColor, String bColor) {
		if (!on) {
			return;
		}
		text (margin, text, false, true);
	}
	
	@Override
	public void text (int margin, String text, String fColor, String bColor) {
		if (!on) {
			return;
		}
		text (margin, text, false, false);
	}
	
	private void text (int margin, String text, boolean notFirst, boolean endLn) {
		String [] lines = Lang.split (text, Lang.ENDLN);
		for (int i = 0; i < lines.length; i++) {
			String line = lines [i];
			if (i > 0 || !notFirst) {
				margin (margin);
			}
			if (endLn) {
				tool.writeln (Lang.replace (line, Lang.TAB, Tab));
			} else {
				tool.write (Lang.replace (line, Lang.TAB, Tab));
			}
		}
	}
	private void line (int margin, int length) {
		margin (margin);
		for (int i = 0; i < length; i++) { tool.write (Lang.DASH); }
		tool.writeln (Lang.BLANK);
	}
	private String format (String msg, Object... args) {
		if (args == null || args.length == 0) {
			return msg;
		}
		return MessageFormat.format (msg, args);
	}
	

	@Override
	public FontPrinter getFontPrinter () {
		return null;
	}	
	
	@Override
	public void clear () {
		
	}

}
