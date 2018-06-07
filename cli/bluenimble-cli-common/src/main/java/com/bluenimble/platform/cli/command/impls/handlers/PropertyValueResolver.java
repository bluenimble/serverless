package com.bluenimble.platform.cli.command.impls.handlers;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;

public interface PropertyValueResolver {

	Object lookup (Tool tool, String value) throws CommandExecutionException;
	
}
