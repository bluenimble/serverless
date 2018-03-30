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
package com.bluenimble.platform.tracing.impls.log4j;

import java.text.MessageFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.ApiRequest;
import com.bluenimble.platform.api.impls.SpaceThread;
import com.bluenimble.platform.api.tracing.Tracer;

public class DefaultTracer implements Tracer {

	private static final long serialVersionUID = -1330865353010110633L;
	
	interface Loggers {
		String All 		= "All";
		String Requests = "Requests";
	}
	
	private Logger log;

	private String namespace;
	
	private String name;
	
	public DefaultTracer () {
	}
	
	@Override
	public void log (Level level, Object o, Throwable th) {
		
		// add namespace to the context
		ThreadContext.put ("namespace", namespace);
		
		try {
			switch (level) {
				case Fatal:
					log.fatal (o, th);
					break;
				case Error:
					log.error (o, th);
					break;
				case Warning:
					log.warn (o, th);
					break;
				case Info:
					log.info (o, th);
					break;
				case Debug:
					log.debug (o, th);
					break;
		
				default:
					break;
			}
		} finally {
			ThreadContext.clearMap ();
		}
	}

	@Override
	public void log (Level level, Object o, Object... args) {
		
		// add namespace to the context
		ThreadContext.put ("namespace", namespace);
		
		if (Thread.currentThread () instanceof SpaceThread) {
			ApiRequest request = ((SpaceThread)Thread.currentThread ()).getRequest ();
			if (request != null) {
				ThreadContext.put ("request", request.getId ());
			}
		}
		
		try {
			if (o == null) {
				o = Lang.NULL;
			}
			
			switch (level) {
				case Fatal:
					if (log.isFatalEnabled ()) {
						log.fatal (format (o, args));
					}
					break;
				case Error:
					if (log.isErrorEnabled ()) {
						log.error (format (o, args));
					}
					break;
				case Warning:
					if (log.isWarnEnabled ()) {
						log.warn (format (o, args));
					}
					break;
				case Info:
					if (log.isInfoEnabled ()) {
						log.info (format (o, args));
					}
					break;
				case Debug:
					if (log.isDebugEnabled ()) {
						log.debug (format (o, args));
					}
					break;
		
				default:
					break;
			}
		} finally {
			ThreadContext.clearMap ();
		}
	}
	
	@Override
	public void onInstall (Traceable traceable) {
		
		String loggerName = name;
		
		if (Lang.isNullOrEmpty (loggerName)) {
			loggerName = traceable.getNamespace ();
		}
		
		if (!LogManager.exists (loggerName)) {
			String prefix = Loggers.All;
			
			if (traceable instanceof Api) {
				prefix = Loggers.Requests;
			} 
			loggerName = prefix + Lang.DOT + loggerName;
		}
		
		log = LogManager.getLogger (loggerName);
		
		namespace = traceable.getClass ().getSimpleName () + " | " + loggerName;
	}

	@Override
	public void onShutdown (Traceable traceable) {
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

	@Override
	public boolean isEnabled (Level level) {
		switch (level) {
			case Fatal:
				return log.isFatalEnabled ();
			case Error:
				return log.isErrorEnabled ();
			case Warning:
				return log.isWarnEnabled ();
			case Info:
				return log.isInfoEnabled ();
			case Debug:
				return log.isDebugEnabled ();
			default:
				return false;
		}
	}
	
	private Object format (Object message, Object... args) {
		if (message == null) {
			return null;
		}
		if (args == null || args.length == 0) {
			return message;
		}
		return MessageFormat.format (String.valueOf (message), args);
	}

}
