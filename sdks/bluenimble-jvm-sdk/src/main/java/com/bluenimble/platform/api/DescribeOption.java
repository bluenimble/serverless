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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DescribeOption implements Serializable {
	
	private static final long serialVersionUID = 8883970109629241690L;
	
	public static final DescribeOption Info 		= new DescribeOption (Option.info);
	public static final DescribeOption Keys 		= new DescribeOption (Option.keys);
	public static final DescribeOption Hardware 	= new DescribeOption (Option.hardware);
	public static final DescribeOption Spaces 		= new DescribeOption (Option.spaces);
	public static final DescribeOption Plugins 		= new DescribeOption (Option.plugins);
	public static final DescribeOption Features 	= new DescribeOption (Option.features);
	public static final DescribeOption Apis 		= new DescribeOption (Option.apis);
	public static final DescribeOption All 			= new DescribeOption (Option.all);
	public static final DescribeOption Services 	= new DescribeOption (Option.services);
	
	interface VerboseOptions {
		DescribeOption Info 		= new DescribeOption (Option.info, true);
		DescribeOption Keys 		= new DescribeOption (Option.keys, true);
		DescribeOption Hardware 	= new DescribeOption (Option.hardware, true);
		DescribeOption Spaces 		= new DescribeOption (Option.spaces, true);
		DescribeOption Plugins 		= new DescribeOption (Option.plugins, true);
		DescribeOption Features 	= new DescribeOption (Option.features, true);
		DescribeOption Apis 		= new DescribeOption (Option.apis, true);
		DescribeOption All 			= new DescribeOption (Option.all, true);
		DescribeOption Services 	= new DescribeOption (Option.services, true);
	}

	public static final Map<DescribeOption.Option, DescribeOption> AllOptions = new HashMap<DescribeOption.Option, DescribeOption> ();
	static {
		AllOptions.put (Option.info, Info);
		AllOptions.put (Option.keys, Keys);
		AllOptions.put (Option.spaces, Spaces);
		AllOptions.put (Option.plugins, Plugins);
		AllOptions.put (Option.features, Features);
		AllOptions.put (Option.apis, Apis);
		AllOptions.put (Option.services, Services);
	} 
	
	public static final Map<DescribeOption.Option, DescribeOption> AllVerboseOptions = new HashMap<DescribeOption.Option, DescribeOption> ();
	static {
		AllOptions.put (Option.info, VerboseOptions.Info);
		AllOptions.put (Option.keys, VerboseOptions.Keys);
		AllOptions.put (Option.spaces, VerboseOptions.Spaces);
		AllOptions.put (Option.plugins, VerboseOptions.Plugins);
		AllOptions.put (Option.features, VerboseOptions.Features);
		AllOptions.put (Option.apis, VerboseOptions.Apis);
		AllOptions.put (Option.services, VerboseOptions.Services);
	} 
	
	public enum Option {
		info,
		keys,
		hardware,
		plugins,
		features,
		snapshot,
		spaces,
		workers,
		apis,
		services,
		secrets,
		security,
		logging,
		tracking,
		runtime,
		custom,
		all,
		failed
	}
	
	private Option 	option;
	private boolean verbose;
	private int 	offset = 0;
	private int 	length = -1;
	
	public DescribeOption (Option option, boolean verbose, int offset, int length) {
		if (offset < 0) {
			offset = 0;
		}
		if (length < 0) {
			length = -1;
		}
		this.option 	= option;
		this.verbose 	= verbose;
		this.offset 	= offset;
		this.length 	= length;
	}
	
	public DescribeOption (Option option, int offset, int length) {
		this (option, false, offset, -1);
	}
	
	public DescribeOption (Option option, int offset) {
		this (option, offset, -1);
	}
	
	public DescribeOption (Option option, boolean verbose) {
		this (option, verbose, 0, -1);
	}

	public DescribeOption (Option option) {
		this (option, 0);
	}

	public Option getOption () {
		return option;
	}

	public int getOffset () {
		return offset;
	}

	public int getLength () {
		return length;
	}
	
	public boolean isVerbose () {
		return verbose;
	}
	
}
