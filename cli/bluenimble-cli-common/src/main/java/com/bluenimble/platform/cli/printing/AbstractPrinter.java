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

public abstract class AbstractPrinter implements Printer {

	private static final long serialVersionUID = 2496594448833876760L;
	
	protected boolean on = true;
	
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
	
}
