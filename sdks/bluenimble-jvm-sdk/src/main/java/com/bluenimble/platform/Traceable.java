package com.bluenimble.platform;

import java.io.Serializable;

import com.bluenimble.platform.api.tracing.Tracer;

public interface Traceable extends Serializable {
	
	String 	getNamespace 	();
	
	Tracer	tracer		();
	
}
