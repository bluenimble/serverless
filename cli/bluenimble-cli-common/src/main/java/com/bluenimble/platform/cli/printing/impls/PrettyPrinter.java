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
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

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

public class PrettyPrinter extends AbstractPrinter {
	
	private static final long serialVersionUID = -1755386137628965295L;
	
	//private static final String Circle 	= "o";
	private static final String Tab 	= "    ";
	
	private Tool 	tool;
	
	private FontPrinter fontPrinter;
	
	public PrettyPrinter (Tool tool, int margin) {
		this.tool = tool;
		this.margin = margin;
		
		if (System.getProperty ("os.name").toLowerCase ().startsWith ("win")) {
			fontPrinter = new WinFontPrinter (FColor.WHITE, BColor.BLACK);
		} else {
			fontPrinter = new NixFontPrinter (FColor.WHITE, BColor.BLACK);
		}
	}

	@Override
	public void message (String label, String msg, Object... args) {
		if (!on) {
			return;
		}
		tool.writeln (Lang.ENDLN);
		margin ();
		
		Label oLabel = null;
		try {
			oLabel = Label.valueOf (label);
		} catch (Exception ex) {
			// Ignore
		}
		if (oLabel != null) {
			switch (oLabel) {
				case Info:
					fontPrinter.print (label, Attribute.LIGHT, FColor.CYAN, BColor.NONE);
					break;
				case Error:
					fontPrinter.print (label, Attribute.LIGHT, FColor.RED, BColor.NONE);
					break;
				case Important:
					fontPrinter.print (label, Attribute.LIGHT, FColor.MAGENTA, BColor.NONE);
					break;
				case Success:
					fontPrinter.print (label, Attribute.LIGHT, FColor.GREEN, BColor.NONE);
					break;
				case Note:
					fontPrinter.print (label, Attribute.NONE, FColor.GREEN, BColor.NONE);
					break;
				case Warning:
					fontPrinter.print (label, Attribute.LIGHT, FColor.YELLOW, BColor.NONE);
					break;
				default:
					break;
			}
		}
		
		tool.write (Lang.SPACE).write (Lang.PIPE).write (Lang.SPACE);
		
		text (label.length () + 3, format (msg, args), true, null, null, true);
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
		text (3 * level + 5, text, true, null, null, true);
	}

	@Override
	public void content (String title, String text) {
		if (!on) {
			return;
		}
		tool.writeln (Lang.ENDLN);
		if (!Lang.isNullOrEmpty (title)) {
			margin ();
			//tool/*.write (Circle).write (Lang.SPACE)*/.writeln (title);
			fprint (title);
			tool.writeln (Lang.BLANK);
			line (0, title.length () + 2);
		}
		text (2, text, false, null, null, true);
	}
	
	@Override
	public void margin (int extra) {
		if (!on) {
			return;
		}
		
		int totalMargin = margin + extra;
		
		if (totalMargin <= 0) {
			return;
		}
		for (int i = 0; i < totalMargin; i++) { tool.write (Lang.SPACE); }
	}

	@Override
	public void panel (Panel panel) {
		if (!on) {
			return;
		}
		
	}
	
	@Override
	public void textLn (int margin, String text, String fColor, String bColor) {
		if (!on) {
			return;
		}
		text (margin, text, false, fColor, bColor, true);
	}
	
	@Override	
	public void text (int margin, String text, String fColor, String bColor) {
		if (!on) {
			return;
		}
		text (margin, text, false, fColor, bColor, false);
	}
	
	private void margin () {
		margin (0);
	}
	
	private void text (int margin, String text, boolean notFirst, String fColor, String bColor, boolean endLn) {
		String [] lines = Lang.split (text, Lang.ENDLN);
		for (int i = 0; i < lines.length; i++) {
			String line = lines [i];
			if (i > 0 || !notFirst) {
				margin (margin);
			}
			FColor fc = null;
			BColor bc = null;
			if (fColor != null) {
				try {
					fc = FColor.valueOf (fColor);
				} catch (Exception ex) {
					// Ignor
				}
			}
			if (bColor != null) {
				try {
					bc = BColor.valueOf (bColor);
				} catch (Exception ex) {
					// Ignor
				}
			}
			
			line = Lang.replace (line, Lang.TAB, Tab);
			
			if (i == lines.length - 1) {
				if (fc != null || bc != null) {
					if (endLn) {
						fontPrinter.println (line, Attribute.LIGHT, fc == null ? FColor.NONE : fc, bc == null ? BColor.NONE : bc);
					} else {
						fontPrinter.print (line, Attribute.LIGHT, fc == null ? FColor.NONE : fc, bc == null ? BColor.NONE : bc);
					}
				} else {
					if (endLn) {
						tool.writeln (line);
					} else {
						tool.write (line);
					}
				}
			} else {
				if (fc != null || bc != null) {
					fontPrinter.println (line, Attribute.LIGHT, fc == null ? FColor.NONE : fc, bc == null ? BColor.NONE : bc);
				} else {
					tool.writeln (line);
				}
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
		return fontPrinter;
	}
	
	@Override
	public void clear () {
		fontPrinter.clear (tool);
	}

}
