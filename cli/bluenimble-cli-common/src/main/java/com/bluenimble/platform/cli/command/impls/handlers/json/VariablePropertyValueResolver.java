package com.bluenimble.platform.cli.command.impls.handlers.json;

import java.util.Map;

import com.bluenimble.platform.cli.Tool;
import com.bluenimble.platform.cli.ToolContext;
import com.bluenimble.platform.cli.command.impls.handlers.PropertyValueResolver;

public class VariablePropertyValueResolver implements PropertyValueResolver {

	@Override
	public Object lookup (Tool tool, String value) {
		@SuppressWarnings("unchecked")
		Map<String, Object> vars = (Map<String, Object>)tool.getContext (Tool.ROOT_CTX).get (ToolContext.VARS);
		return vars.get (value);
	}

}
