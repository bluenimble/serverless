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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.json.JsonObject;

public interface ApiResponse extends Serializable {
	
	String 			RequestID 		= "request";
	interface 		Error {
		String Code 	= "code";
		String Message 	= "message";
		String Trace 	= "trace";
			String Clazz 	= "class";
			String Line 	= "line";
			String File 	= "file";
			String Function = "function";
	}
	
	/** 
	 * 
	 * Informational : 1xx 
	 * 
	 **/
	public static final Status CONTINUE 						= new Status (100, "Continue");
	public static final Status SWITCHING_PROTOCOLS 				= new Status (101, "Switching Protocols");
	public static final Status PROCESSING   					= new Status (102, "Processing");

	/** 
	 * 
	 * Success : 2xx 
	 * 
	 **/
	public static final Status OK 								= new Status (200, "Ok");
	public static final Status CREATED 							= new Status (201, "Created");
	public static final Status ACCEPTED 						= new Status (202, "Accepted");
	public static final Status NON_AUTHORITATIVE_INFORMATION 	= new Status (203, "Non Authoritative Information");
	public static final Status NO_CONTENT 						= new Status (204, "No Content");
	public static final Status RESET_CONTENT					= new Status (205, "Reset Content");
	public static final Status PARTIAL_CONTENT					= new Status (206, "Partial Content");
	public static final Status MULTI_STATUS						= new Status (207, "Multi-Status");
	
	/** 
	 * 
	 * Redirection : 3xx 
	 * 
	 **/
	public static final Status MULTIPLE_CHOICES					= new Status (300, "Mutliple Choices");
	public static final Status MOVED_PERMANENTLY				= new Status (301, "Moved Permanently");
	public static final Status MOVED_TEMPORARILY				= new Status (302, "Moved Temporarily");
	public static final Status SEE_OTHER						= new Status (303, "See Other");
	public static final Status NOT_MODIFIED						= new Status (304, "Not Modified");
	public static final Status USE_PROXY						= new Status (305, "Use Proxy");
	public static final Status TEMPORARY_REDIRECT				= new Status (307, "Temporary Redirect");

	/** 
	 * 
	 * Client Errors : 4xx 
	 * 
	 **/
	public static final Status BAD_REQUEST						= new Status (400, "Bad Request");
	public static final Status UNAUTHORIZED						= new Status (401, "Unauthorized");
	public static final Status PAYMENT_REQUIRED					= new Status (402, "Payment Required");
	public static final Status FORBIDDEN						= new Status (403, "Forbidden");
	public static final Status NOT_FOUND						= new Status (404, "Not Found");
	public static final Status METHOD_NOT_ALLOWED				= new Status (405, "Method Not Allowed");
	public static final Status NOT_ACCEPTABLE					= new Status (406, "Not Acceptable");
	public static final Status PROXY_AUTHENTICATION_REQUIRED	= new Status (407, "Proxy Authentication Required");
	public static final Status REQUEST_TIMEOUT					= new Status (408, "Request Timeout");
	public static final Status CONFLICT							= new Status (409, "Conflict");
	public static final Status GONE								= new Status (410, "Gone");
	public static final Status LENGTH_REQUIRED					= new Status (411, "Length Required");
	public static final Status PRECONDITION_FAILED				= new Status (412, "Precondition Failed");
	public static final Status REQUEST_TOO_LONG					= new Status (413, "Request Entity Too Large");
	public static final Status REQUEST_URI_TOO_LONG				= new Status (414, "Request-URI Too Long");
	public static final Status UNSUPPORTED_MEDIA_TYPE			= new Status (415, "Unsupported Media Type");
	public static final Status REQUESTED_RANGE_NOT_SATISFIABLE	= new Status (416, "Requested Range Not Satisfiable");
	public static final Status EXPECTATION_FAILED				= new Status (417, "Expectation Failed");
	public static final Status INSUFFICIENT_SPACE_ON_RESOURCE	= new Status (419, "Insufficient Space on Resource");
	public static final Status METHOD_FAILURE					= new Status (420, "Method Failure");
	public static final Status UNPROCESSABLE_ENTITY				= new Status (422, "Unprocessable Entity");
	public static final Status LOCKED							= new Status (423, "Locked");
	public static final Status FAILED_DEPENDENCY				= new Status (424, "Failed Dependency");

	/** 
	 * 
	 * Server Errors : 5xx 
	 * 
	 **/
	public static final Status INTERNAL_SERVER_ERROR			= new Status (500, "Server Error");
	public static final Status NOT_IMPLEMENTED					= new Status (501, "Not Implemented");
	public static final Status BAD_GATEWAY						= new Status (502, "Bad Gateway");
	public static final Status SERVICE_UNAVAILABLE				= new Status (503, "Service Unavailable");
	public static final Status GATEWAY_TIMEOUT					= new Status (504, "Gateway Timeout");
	public static final Status HTTP_VERSION_NOT_SUPPORTED		= new Status (505, "HTTP Version Not Supported");
	public static final Status INSUFFICIENT_STORAGE				= new Status (507, "Insufficient Storage");

	class Status implements Serializable {
		
		private static final long serialVersionUID = -2128228864466624663L;
		
		private int code;
		private String message;
		
		public int getCode() {
			return code;
		}

		public String getMessage() {
			return message;
		}

		public Status (int code, String message) {
			this.code = code;
			this.message = message;
		}
		
		public Status (int code) {
			this (code, null);
		}
		
		public String toString () {
			return code + (message != null ? Lang.SPACE + message : Lang.BLANK);
		}
		
	}
	
	String 			getId 		();
	
	ApiResponse 	set 		(String name, Object value);
	
	void			setStatus  	(Status status);
	Status			getStatus  	();
	
	void			setBuffer  	(int size);
	
	OutputStream 	toOutput 	() throws IOException;
	Writer 			toWriter 	() throws IOException;

	ApiResponse 	write 		(byte [] buff, int offset, int length) throws IOException;
	ApiResponse 	write 		(Object buff) throws IOException;
	
	ApiResponse 	error 		(Status status, Object message);

	JsonObject		getError	();
	
	void 			reset 		();
	void 			close 		() throws IOException;
	
	void			commit 		();
	boolean			isCommitted	();
	
	void			flushHeaders ();

}
