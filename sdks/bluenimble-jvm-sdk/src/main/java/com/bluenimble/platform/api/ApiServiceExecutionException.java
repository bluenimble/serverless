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
package com.bluenimble.platform.api;

public class ApiServiceExecutionException extends Exception {

	private static final long serialVersionUID = 1165308892883189037L;
	
	private ApiResponse.Status status;

	public ApiServiceExecutionException () {
		super ();
	}

	public ApiServiceExecutionException (String message, Throwable cause) {
		super (message, cause);
	}

	public ApiServiceExecutionException (String message) {
		super (message);
	}

	public ApiServiceExecutionException (Throwable cause) {
		super (cause);
	}
	
	public ApiServiceExecutionException status (ApiResponse.Status status) {
		this.status = status;
		return this;
	}

	public ApiResponse.Status status () {
		return status;
	}
	
	public Throwable getRootCause () {
		Throwable cause = getCause ();
		if (cause == null) {
			return this;
		}
		while (cause != null) {
			Throwable sCause = cause.getCause ();
			if (sCause == null) {
				break;
			}
			cause = sCause;
		}
		return cause;
	}

}
