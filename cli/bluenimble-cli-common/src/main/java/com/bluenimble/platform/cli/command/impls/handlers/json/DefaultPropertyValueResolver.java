package com.bluenimble.platform.cli.command.impls.handlers.json;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.impls.handlers.PropertyValueResolver;

public class DefaultPropertyValueResolver implements PropertyValueResolver {

	@Override
	public Object lookup (Tool tool, String value) throws CommandExecutionException {
		return value;
	}

}
