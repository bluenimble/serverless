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

public interface ApiHeaders {

	public static final String 	Accept 				= "Accept";
	
	public static final String 	ContentType 		= "Content-Type";
	public static final String 	ContentLength		= "Content-Length";
	public static final String 	ContentDisposition	= "Content-Disposition";
	public static final String 	ContentEncoding		= "Content-Encoding";
	public static final String 	AcceptEncoding		= "Accept-Encoding";
	
	public static final String 	UserAgent 			= "User-Agent";
	
	public static final String 	Authorization		= "Authorization";
	
	public static final String 	AllowOrigin			= "Access-Control-Allow-Origin";
	
	public static final String 	IfRange				= "If-Range";
	public static final String 	Range				= "Range";
	public static final String 	AcceptRanges		= "Accept-Ranges";
	public static final String 	ContentRange		= "Content-Range";
	
	public static final String 	CacheControl		= "Cache-Control";

	public static final String 	IfMatch				= "If-Match";
	public static final String 	IfNoneMatch			= "If-None-Match";
	
	public static final String 	LastModified		= "Last-Modified";
	public static final String 	IfModifiedSince		= "If-Modified-Since";
	public static final String 	IfUnmodifiedSince	= "If-Unmodified-Since";
	
	public static final String 	Expires				= "Expires";
	public static final String 	ETag				= "ETag";
	public static final String 	Cookie				= "Cookie";
	public static final String 	SetCookie			= "Set-Cookie";

	public static final String 	Location			= "Location";

	public static final String 	NodeID 				= "BN-Node-ID";
	public static final String 	NodeType 			= "BN-Node-Type";
	public static final String 	NodeVersion 		= "BN-Node-Version";
	public static final String 	ExecutionTime 		= "BN-Execution-Time";
	public static final String 	Timestamp 			= "BN-Timestamp";
	public static final String 	GeoLocation 		= "BN-GeoLocation";
	
}
