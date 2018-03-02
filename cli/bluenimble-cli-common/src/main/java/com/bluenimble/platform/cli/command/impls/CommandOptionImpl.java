/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.cli.command.impls;

import java.util.ArrayList;
import java.util.List;

import com.bluenimble.platform.cli.command.CommandOption;
import com.bluenimble.platform.cli.command.CommandOptionCast;

public class CommandOptionImpl implements CommandOption {
	
	private static final long serialVersionUID = 4728242459759882597L;
	
	protected String name;
	protected String label;
	protected List<Object> values;
	protected int acceptsArgs = MULTI_ARG;
	protected boolean required;
	protected boolean masked;
	protected CommandOptionCast cast;
	
	public CommandOptionImpl (String name) {
		this (name, MULTI_ARG);
	}
	
	public CommandOptionImpl (String name, int acceptsArgs) {
		this (name, acceptsArgs, false);
	}
	
	public CommandOptionImpl (String name, int acceptsArgs, boolean required) {
		this (name, acceptsArgs, required, null);
	}
	
	public CommandOptionImpl (String name, int acceptsArgs, boolean required, CommandOptionCast cast) {
		this.name = name;
		this.acceptsArgs = acceptsArgs;
		this.required = required;
		this.cast = cast;
	}
	
	public CommandOption clone () {
		CommandOptionImpl co = new CommandOptionImpl (name, acceptsArgs, required, cast);
		co.setLabel (label);
		co.setMasked (masked);
		return co;
	}

	@Override
	public void addArg (Object value) {
		if (value == null) {
			return;
		}
		if (values == null) {
			values = new ArrayList<Object> ();
		}
		values.add (value);
	}

	@Override
	public String name () {
		return name;
	}

	@Override
	public int getArgsCount () {
		if (values == null) {
			return 0;
		}
		return values.size ();
	}

	@Override
	public Object getArg (int index) {
		if (values == null) {
			return null;
		}
		return values.get (index);
	}

	@Override
	public int acceptsArgs () {
		return acceptsArgs;
	}
	
	@Override
	public boolean isRequired () {
		return required;
	}
	
	@Override
	public CommandOptionCast cast () {
		return cast;
	}
	
	@Override
	public void setCast (CommandOptionCast cast) {
		this.cast = cast;
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		sb.append (name).append (" (").append (label ()).append (") ").append (":").append (" {").append ("\n")
			.append ("\trequired: ").append (required).append (", ")
			.append ("\tacceptsArgs: ").append (acceptsArgs).append (", ")
			.append ("\tmasked: ").append (masked).append (", ")
			.append ("\tcast: ").append (cast).append (", ")
			.append ("\targs: ").append (values).append ("\n")
			.append ("}");
		String s = sb.toString ();
		sb.setLength (0);
		sb = null;
		return s;
	}

	@Override
	public String label () {
		if (label != null) {
			return label;
		}
		return name;
	}

	@Override
	public void setLabel (String label) {
		this.label = label;
	}

	@Override
	public boolean isMasked () {
		return masked;
	}

	public void setMasked (boolean masked) {
		this.masked = masked;
	}

}
