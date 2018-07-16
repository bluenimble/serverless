package com.bluenimble.platform.templating;

import java.util.Map;

public abstract class SimpleVariableResolver implements VariableResolver {

	private static final long serialVersionUID = -4438443776988576034L;

	@Override
	public Map<String, Object> bindings () {
		return null;
	}

}
