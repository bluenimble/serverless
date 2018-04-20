package com.bluenimble.platform.api.impls.media;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bluenimble.platform.api.media.ApiMediaProcessor;
import com.bluenimble.platform.api.media.ApiMediaProcessorRegistry;

public class DefaultApiMediaProcessorRegistry implements ApiMediaProcessorRegistry {

	private static final long serialVersionUID = -3729310270507983491L;
	
	protected Map<String, ApiMediaProcessor> processors;
	
	protected ApiMediaProcessor defaultProcessor;
	
	@Override
	public void register (String name, ApiMediaProcessor processor, boolean _default) {
		if (processors == null) {
			processors = new ConcurrentHashMap<String, ApiMediaProcessor> ();
		}
		processors.put (name, processor);
		
		if (_default) {
			defaultProcessor = processor;
		}
	}

	@Override
	public ApiMediaProcessor lockup (String name) {
		if (processors == null) {
			return null;
		}
		ApiMediaProcessor processor = processors.get (name);
		if (processor == null) {
			processor = defaultProcessor;
		}
		
		return processor;
	}

	@Override
	public ApiMediaProcessor getDefault () {
		return defaultProcessor;
	}

}
