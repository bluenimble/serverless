package com.bluenimble.platform.cli.printing.impls;

import java.util.HashSet;
import java.util.Set;

public class Markers {
	
	public static final String Status 		= "status";
	
	public static final Set<String> Yellow = new HashSet<String> ();
	static {
		Yellow.add ("runnable");
		Yellow.add ("stopped");
		Yellow.add ("paused");
	}
	
	public static final Set<String> Green = new HashSet<String> ();
	static {
		Green.add ("running");
		Green.add ("available");
	}
	
	public static final Set<String> Red = new HashSet<String> ();
	static {
		Red.add ("timed_waiting");
		Red.add ("blocked");
		Red.add ("failed");
	}
	

}
