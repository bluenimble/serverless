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

import org.fusesource.jansi.AnsiConsole;

import com.bluenimble.platform.cli.Tool;
import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class WinFontPrinter implements FontPrinter {
	
	private String ANSI_RESET = "\u001B[0m";
	private String ANSI_CLEAR = "\033[H\033[2J";
	
	private ColoredPrinter cp;
	
	public WinFontPrinter (FColor fc, BColor bc) {
		cp = new ColoredPrinter.Builder (1, false).foreground (fc).background (bc).build ();
	}

	@Override
	public void print (String text, Attribute attr, FColor fc, BColor bc) {
		cp.print (text, attr, fc, bc);
		cp.clear ();
	}

	@Override
	public void println (String text, Attribute attr, FColor fc, BColor bc) {
		cp.println (text, attr, fc, bc);
		cp.clear ();
	}

	@Override
	public String generate (String text, Attribute attr, FColor fc, BColor bc) {
		return cp.generateCode (attr, fc, bc) + text + ANSI_RESET;
	}

	@Override
	public void clear (Tool tool) {
		AnsiConsole.out.print (ANSI_CLEAR);
	}

}
