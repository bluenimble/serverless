package com.bluenimble.platform.cli.command.impls.handlers.json;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.command.CommandExecutionException;
import com.bluenimble.platform.cli.command.impls.handlers.PropertyValueResolver;
import com.bluenimble.platform.json.JsonParser;

public class JsonPropertyValueResolver implements PropertyValueResolver {

	@Override
	public Object lookup (Tool tool, String value) throws CommandExecutionException {
		try {
			return JsonParser.parse (value);
		} catch (Exception ex) {
			throw new CommandExecutionException (ex.getMessage (), ex);
		}
	}

}
