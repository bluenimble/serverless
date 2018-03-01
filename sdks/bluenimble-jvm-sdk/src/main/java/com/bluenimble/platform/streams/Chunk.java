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
package com.bluenimble.platform.streams;

public class Chunk {

	public static final long Infinite = -1;
	
	private long start;
	private long end;
	
	public Chunk (long start, long end) {
		if (start < 0) {
			start = 0;
		}
		if (end < start && end != Infinite) {
			throw new RuntimeException ("chunk end should be greater than " + start);
		}
		this.start 	= start;
		this.end 	= end;
	}
	
	public long start () {
		return start;
	}
	
	public long end () {
		return end;
	}
	
	public long size () {
		return end - start + 1;
	}
	
}
