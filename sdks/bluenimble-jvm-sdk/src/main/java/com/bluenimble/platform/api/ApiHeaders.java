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

	String 	Accept 				= "Accept";
	
	String 	ContentType 		= "Content-Type";
	String 	ContentLength		= "Content-Length";
	String 	ContentDisposition	= "Content-Disposition";
	String 	ContentEncoding		= "Content-Encoding";
	String 	ContentLanguage		= "Content-Language";
	String 	AcceptEncoding		= "Accept-Encoding";
	
	String 	UserAgent 			= "User-Agent";
	
	String 	Authorization		= "Authorization";
	
	String 	AllowOrigin			= "Access-Control-Allow-Origin";
	
	String 	IfRange				= "If-Range";
	String 	Range				= "Range";
	String 	AcceptRanges		= "Accept-Ranges";
	String 	ContentRange		= "Content-Range";
	
	String 	CacheControl		= "Cache-Control";

	String 	IfMatch				= "If-Match";
	String 	IfNoneMatch			= "If-None-Match";
	
	String 	LastModified		= "Last-Modified";
	String 	IfModifiedSince		= "If-Modified-Since";
	String 	IfUnmodifiedSince	= "If-Unmodified-Since";
	
	String 	Expires				= "Expires";
	String 	Pragma				= "Pragma";
	String 	ETag				= "ETag";
	String 	Cookie				= "Cookie";
	String 	SetCookie			= "Set-Cookie";

	String 	Location			= "Location";

	String 	NodeID 				= "BNB-Node-ID";
	String 	NodeType 			= "BNB-Node-Type";
	String 	NodeVersion 		= "BNB-Node-Version";
	String 	ExecutionTime 		= "BNB-Execution-Time";
	String 	Timestamp 			= "BNB-Timestamp";
	String 	GeoLocation 		= "BNB-GeoLocation";
	
	interface Tus {
		String 	TusVersion 			= "Tus-Version";
		String 	TusResumable 		= "Tus-Resumable";
		String 	TusExtension 		= "Tus-Extension";
		String 	TusMaxSize 			= "Tus-Max-Size";

		String 	UploadOffset 		= "Upload-Offset";
		String 	UploadLength 		= "Upload-Length";
		String 	UploadMetadata 		= "Upload-Metadata";
		
		String 	XHTTPMethodOverride = "X-HTTP-Method-Override";
	}
	
}
