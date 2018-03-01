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
package com.bluenimble.platform.json;

public class JsonException extends Exception {

	private static final long serialVersionUID = 3778652114497335944L;
	
	private int lineNumber;
	private String lineFragment;
	
	public JsonException (int lineNumber, String lineFragment) {
		this ();
		this.lineNumber = lineNumber;
		this.lineFragment = lineFragment;
	}

	public JsonException (int lineNumber) {
		this (lineNumber, null);
	}

	public JsonException () {
		super ();
	}

	public JsonException (String message, int lineNumber, String lineFragment) {
		this (message);
		this.lineNumber = lineNumber;
		this.lineFragment = lineFragment;
	}

	public JsonException (String message) {
		super (message);
	}

	public JsonException (Throwable throwable, String message) {
		super (message, throwable);
	}

	public JsonException (Throwable throwable, String message, int lineNumber, String lineFragment) {
		this (throwable, message);
		this.lineNumber = lineNumber;
		this.lineFragment = lineFragment;
	}

	public JsonException (Throwable throwable, int lineNumber, String lineFragment) {
		this (throwable);
		this.lineNumber = lineNumber;
		this.lineFragment = lineFragment;
	}

	public JsonException (Throwable throwable) {
		super (throwable);
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getLineFragment() {
		return lineFragment;
	}

	public void setLineFragment(String lineFragment) {
		this.lineFragment = lineFragment;
	}

}
