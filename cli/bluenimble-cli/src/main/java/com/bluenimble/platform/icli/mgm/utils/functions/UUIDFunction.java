package com.bluenimble.platform.icli.mgm.utils.functions;

import com.bluenimble.platform.Lang;

public class UUIDFunction implements BuildFunction {

	@Override
	public Object eval (String... args) {
		return Lang.rand ();
	}
	
}
