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

import com.bluenimble.platform.cli.Tool;

import print.color.Ansi.Attribute;
import print.color.Ansi.BColor;
import print.color.Ansi.FColor;

public class NixFontPrinter implements FontPrinter {
	
	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_CLEAR = "\033[H\033[2J";
	
	private static final String Prefix = "\u001B[";
	private static final String Postfix = "m";
	
	public NixFontPrinter (FColor fc, BColor bc) {
	}

	@Override
	public void print (String text, Attribute attr, FColor fc, BColor bc) {
		System.out.print (Prefix + fc.getCode () + Postfix + text + ANSI_RESET);
	}

	@Override
	public void println (String text, Attribute attr, FColor fc, BColor bc) {
		System.out.println (Prefix + fc.getCode () + Postfix + text + ANSI_RESET);
	}

	@Override
	public String generate (String text, Attribute attr, FColor fc, BColor bc) {
		return Prefix + fc.getCode () + Postfix + text;
	}

	@Override
	public void clear (Tool tool) {
		tool.write (ANSI_CLEAR);
	}

}
