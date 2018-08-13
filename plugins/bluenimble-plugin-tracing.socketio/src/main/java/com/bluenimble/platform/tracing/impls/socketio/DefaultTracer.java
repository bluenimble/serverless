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
package com.bluenimble.platform.tracing.impls.socketio;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Traceable;
import com.bluenimble.platform.api.Api;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.plugins.impls.PluginClassLoader;
import com.bluenimble.platform.plugins.tracing.socketio.SocketIoTracerPlugin;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DefaultTracer implements Tracer {

	private static final long serialVersionUID = -1330865353010110633L;
	
	interface Logger {
		String Publish 		= "publish";
		String Channel 		= "channel";
		String Data 		= "data";
			String Level 		= "level";
			String Timestamp 	= "timestamp";
			String Message 		= "message";
			String Trace 		= "trace";
	}
	
	private static final Map<Level, Integer> LevelWeight = new HashMap<Level, Integer>();
	static {
		LevelWeight.put (Level.Fatal, 5);
		LevelWeight.put (Level.Error, 4);
		LevelWeight.put (Level.Warning, 3);
		LevelWeight.put (Level.Info, 2);
		LevelWeight.put (Level.Debug, 1);
	}
	
	private Socket 		socket;

	private int 		level 		= 2;
	
	private String 		uri;
	private String 		authField 	= "token";
	private String 		authValue;
	private String 		channelPrefix;
	
	private JsonArray 	channels;
	
	Tracer pluginTracer = null;
	
	public DefaultTracer () {
		PluginClassLoader pcl = (PluginClassLoader)SocketIoTracerPlugin.class.getClassLoader ();
		SocketIoTracerPlugin plugin = (SocketIoTracerPlugin)pcl.getPlugin ();
		channelPrefix = plugin.getChannelPrefix ();
		pluginTracer = plugin.tracer ();
	}
	
	@Override
	public void log (Level level, Object o, Throwable th) {
		
		if (SocketIoTracerPlugin.ShuttingDown) {
			return;
		}
		
		if (!isEnabled (level)) {
			return;
		}
		
		emit (level, o, th);
	}

	@Override
	public void log (Level level, Object o, Object... args) {
		
		if (SocketIoTracerPlugin.ShuttingDown) {
			return;
		}
		
		if (!isEnabled (level)) {
			return;
		}
		
		if (o == null) {
			o = Lang.NULL;
		}
		
		emit (level, format (o, args), null);
	}
	
	@Override
	public void onInstall (Traceable traceable) {
		
		pluginTracer.log (Level.Info, "OnInstall {0}", traceable);
		
		if (channelPrefix == null) {
			channelPrefix = Lang.BLANK;
		}
		if (traceable instanceof Api) {
			channelPrefix += ((Api)traceable).space ().getNamespace () + Lang.SLASH;
		}   
		
		channels = new JsonArray ();
		channels.add (channelPrefix + traceable.getNamespace ());
		
		IO.Options opts = new IO.Options ();
		opts.forceNew = true;
		opts.reconnection = true;
		
		// should be part of the feature config
		opts.query = authField + "=" + authValue;
		
		try {
			socket = IO.socket (uri, opts);
		} catch (URISyntaxException ex) {
			throw new RuntimeException (ex.getMessage (), ex);
		}
		
		socket.on (Socket.EVENT_CONNECT, new Emitter.Listener () {
			@Override
			public void call (Object... args) {
				pluginTracer.log (Level.Info, "{0} connected !!!", traceable);
			}
		});
		
		socket.on (Socket.EVENT_ERROR, new Emitter.Listener () {
			@Override
			public void call (Object... args) {
				pluginTracer.log (Level.Error, "From SocketIo broker -> {0}" + args [0]);
			}
		});
		
		socket.connect ();
		
	}

	@Override
	public void onShutdown (Traceable traceable) {
		if (socket == null) {
			return;
		}
		socket.disconnect ();
	}

	@Override
	public boolean isEnabled (Level level) {
		return LevelWeight.get (level) >= this.level;
	}
	
	public void setLevel (String level) {
		Level l = Level.Info;
		try {
			l = Level.valueOf (level);
		} catch (Exception ex) {
		}
		this.level = LevelWeight.get (l);
	}
	
	public String getLevel () {
		return null;
	}
	
	public String getUri() {
		return uri;
	}
	public void setUri (String uri) {
		this.uri = uri;
	}

	public String getAuthField () {
		return authField;
	}
	public void setAuthField (String authField) {
		this.authField = authField;
	}

	public String getAuthValue () {
		return authValue;
	}
	public void setAuthValue (String authValue) {
		this.authValue = authValue;
	}

	private void emit (Level level, Object message, Throwable th) {
		
		if (socket == null) {
			return;
		}
		
		JsonObject payload = new JsonObject ();
		
		payload.set (Logger.Channel, channels);
		
		JsonObject data = new JsonObject ();
		payload.set (Logger.Data, data);
		
		data.set (Logger.Level, level.name ());
		data.set (Logger.Timestamp, Lang.utc ());
		data.set (Logger.Message, message.toString ());
		if (th != null) {
			data.set (Logger.Trace, Lang.toString (th));
		}
		
		socket.emit (Logger.Publish, payload);
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
