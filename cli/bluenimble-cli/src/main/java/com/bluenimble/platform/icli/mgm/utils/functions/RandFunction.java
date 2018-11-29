package com.bluenimble.platform.icli.mgm.utils.functions;

import com.bluenimble.platform.Lang;

public class RandFunction implements BuildFunction {

	@Override
	public Object eval (String... args) {
		int length = 16;
		if (args != null && args.length > 1 && !Lang.isNullOrEmpty (args[1])) {
			length = Integer.valueOf (args[0].trim ());
		}
		return Lang.UUID (length);
	}
	
}
