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

public interface ApiContentTypes {

	String 				Text		= "text/plain";
	String 				Json		= "application/json";
	String 				Yaml		= "application/yaml";
	String 				Xml			= "application/xml";
	String 				Html		= "text/html";

	String 				Css			= "text/css";
	String 				Javascript	= "application/javascript";

	String 				Pdf         = "application/pdf";
	
	String 				Multipart 	= "multipart/form-data";
	
	String 				Webp		= "image/webp";
	String 				Bmp			= "image/bmp";
	String 				Tiff		= "image/tiff";
	String 				Jpeg		= "image/jpeg";
	String 				Png			= "image/png";
	String 				Gif			= "image/gif";
	
	String 				MpegAudio	= "audio/x-mpeg";
	String 				Mpeg2Audio	= "audio/x-mpeg-2";
	String 				Mp3			= "audio/mp3";
	
	String 				MpegVideo	= "video/mpeg";
	String 				Mpeg2Video	= "video/mpeg-2";
	
	String 				Stream		= "application/octet-stream";

	String 				Custom		= "bn/custom";

}
