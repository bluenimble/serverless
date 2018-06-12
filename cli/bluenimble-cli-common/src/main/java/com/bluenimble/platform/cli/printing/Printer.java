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

import java.io.Serializable;

import com.bluenimble.platform.cli.printing.impls.FontPrinter;

public interface Printer extends Serializable {

	interface Colors {
		String Yellow 	= "YELLOW";
		String Red 		= "RED";
		String Black 	= "BLACK";
		String Blue 	= "BLUE";
		String Green 	= "GREEN";
		String Magenta 	= "MAGENTA";
		String Cyan 	= "CYAN";
		String White 	= "WHITE";
	}
	
	interface PrintSpec {
		String Start 	= "__PS__";
		String Split 	= "_|_";
		String TextSep 	= ":";
		String ColorSep = "/";
		interface Style {
			String Background = "B";
			String Foreground = "F";
		}
	}
	
	enum Label {
		Success,
		Info,
		Note,
		Warning,
		Error,
		Important,
		Custom
	}
	
	FontPrinter getFontPrinter ();
	
	void message 	(String label, String msg, Object... args);

	void important 	(String msg, Object...args);
	
	void info 		(String msg, Object...args);

	void success 	(String msg, Object...args);

	void warning 	(String msg, Object...args);

	void error 		(String msg, Object...args);

	void note 		(String msg, Object...args);

	void node 		(int level, String text);

	void content 	(String tile, String text);

	void panel 		(Panel panel);
	
	void text 		(int margin, String text, String fColor, String bColor);
	void textLn 	(int margin, String text, String fColor, String bColor);
	
	void fprint		(String spec);

	void margin 	(int extra);
	
	void clear		();
	
	void off		();
	void on			();
	boolean isOn	();

}
