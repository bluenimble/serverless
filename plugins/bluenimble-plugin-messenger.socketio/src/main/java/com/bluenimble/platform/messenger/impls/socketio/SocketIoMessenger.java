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
package com.bluenimble.platform.messenger.impls.socketio;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluenimble.platform.IOUtils;
import com.bluenimble.platform.Json;
import com.bluenimble.platform.Lang;
import com.bluenimble.platform.Recyclable;
import com.bluenimble.platform.api.ApiResource;
import com.bluenimble.platform.api.ApiStreamSource;
import com.bluenimble.platform.api.tracing.Tracer;
import com.bluenimble.platform.api.tracing.Tracer.Level;
import com.bluenimble.platform.json.JsonArray;
import com.bluenimble.platform.json.JsonObject;
import com.bluenimble.platform.messaging.Callback;
import com.bluenimble.platform.messaging.Messenger;
import com.bluenimble.platform.messaging.MessengerException;
import com.bluenimble.platform.messaging.Recipient;
import com.bluenimble.platform.messaging.Sender;
import com.bluenimble.platform.messenger.impls.socketio.utils.MessageUtils;
import com.bluenimble.platform.server.plugins.messenger.socketio.SocketIoMessengerPlugin.Spec;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;

public class SocketIoMessenger implements Messenger, Recyclable {

	private static final long serialVersionUID = 4303282790607692198L;
	
	private static final String PublishEvent 	= "publish";
	
	interface SenderFields {
		String Event 		= "event";
		String Params 		= "params";
	}

	private static final String Channel 		= "channel";
	private static final String Data 			= "data";
	private static final String Subject 		= "subject";
	private static final String Body 			= "body";
	
	private static final String DefaultAuthField= "token";
	
	private static final JsonObject GenericError	= (JsonObject)new JsonObject ().set ("status", "error").set ("reason", "Generic Error");
	private static final JsonObject GenericMessage	= (JsonObject)new JsonObject ().set ("status", "success").set ("timestamp", Lang.utc ());
 	
	protected static final Map<String, CachedTemplate> templates = new ConcurrentHashMap<String, CachedTemplate> ();
	
	private static final Handlebars engine 		= new Handlebars ();
	
	private static final String StartDelimitter 	= "[[";
	private static final String EndDelimitter 		= "]]";
	
	private Tracer 			tracer;
	private OkHttpClient 	http;
	private JsonObject 		spec;
	
	private Socket socket;
	
	public SocketIoMessenger (Tracer tracer, OkHttpClient http, JsonObject spec) {
		this.tracer = tracer;
		this.http = http;
		this.spec = spec;
		
		engine.startDelimiter (StartDelimitter);
		engine.endDelimiter (EndDelimitter);
	}
	
	@Override
	public void send (Sender sender, Recipient [] recipients, String subject, Object body, ApiStreamSource... attachments) throws MessengerException {
		
		tracer.log (Level.Info, "Send to socketio");
		
		if (sender == null) {
			throw new MessengerException ("no sender found");
		}
		
		if (Lang.isNullOrEmpty (sender.id ())) {
			throw new MessengerException ("no sender id found");
		}
		
		Object oEvent = sender.get (SenderFields.Event);
		if (oEvent == null) {
			oEvent = PublishEvent;
		}
		if (recipients == null || recipients.length == 0) {
			throw new MessengerException ("no recipients found");
		}
		
		String authField = Json.getString (spec, Spec.AuthField, DefaultAuthField);
		
		JsonArray aQuery = new JsonArray ();
		aQuery.add (authField + Lang.EQUALS + sender.id ());
		
		Object params = sender.get (SenderFields.Params);
		if (params != null && params instanceof JsonObject) {
			JsonObject oParams = (JsonObject)params;
			Iterator<String> keys = oParams.keys ();
			while (keys.hasNext ()) {
				String key = keys.next ();
				aQuery.add (key + Lang.EQUALS + oParams.get (key));
			}
		}
		
		String query = aQuery.join (Lang.AMP, false);
		
		tracer.log (Level.Info, "SocketQuery: " + query);
		
		JsonArray channels = new JsonArray ();
		for (Recipient r : recipients) {
			channels.add (r.id ());
		}
		
		JsonObject message = new JsonObject ();
		message.set (Channel, channels);
		
		if (Lang.isNullOrEmpty (subject)) {
			message.set (Data, body);
		} else {
			JsonObject data = new JsonObject ();
			data.set (Subject, subject);
			data.set (Body, body);
			message.set (Data, data);
		}
		
		tracer.log (Level.Info, "Create socket");
		
		IO.Options opts = new IO.Options ();
		opts.callFactory = http;
		opts.webSocketFactory = http;
		opts.forceNew  = Json.getBoolean (spec, Spec.ForceNew, true);
		opts.multiplex = Json.getBoolean (spec, Spec.Multiplex, true);
		opts.timeout = Json.getInteger (spec, Spec.Timeout, 60000);
		opts.transports = new String[] { WebSocket.NAME };
		
		JsonObject oReconnect = Json.getObject (spec, Spec.Reconnect.class.getSimpleName ().toLowerCase ());
		opts.reconnection 			= Json.getBoolean (oReconnect, Spec.Reconnect.Enabled, true);
		opts.reconnectionAttempts 	= Json.getInteger (oReconnect, Spec.Reconnect.Attempts, 3);
		opts.reconnectionDelay 		= Json.getInteger (oReconnect, Spec.Reconnect.Delay, Integer.MAX_VALUE);
		opts.reconnectionDelayMax 	= Json.getInteger (oReconnect, Spec.Reconnect.MaxDelay, 5000);
		
		tracer.log (Level.Info, "Socket URI: " + Json.getString (spec, Spec.Uri));
		
		opts.query = query;
		
		try {
			socket = IO.socket (Json.getString (spec, Spec.Uri), opts);
		} catch (URISyntaxException ex) {
			throw new MessengerException (ex.getMessage (), ex);
		}
		
		Socket _socket 	= socket;
		String event 	= String.valueOf (oEvent);
		
		socket.on (Socket.EVENT_CONNECT, new Emitter.Listener () {
			@Override
			public void call (Object... args) {
				tracer.log (Level.Info, "Socket Connected");
				fireCallback (sender, Sender.Callbacks.Connect, GenericMessage, args);
				emit (_socket, sender, event, message);
			}
		});
		
		socket.on (Socket.EVENT_CONNECT_ERROR, new Emitter.Listener () {
			@Override
			public void call (Object... args) {
				tracer.log (Level.Error, "Socket Connect Error {0}", args [0]);
			}
		});
		
		socket.on (Socket.EVENT_ERROR, new Emitter.Listener () {
			@Override
			public void call (Object... args) {
				tracer.log (Level.Error, "Socket Error {0}", args [0]);
				fireCallback (sender, Sender.Callbacks.Error, GenericError, args);
			}
		});
		
		tracer.log (Level.Info, "New Socket Created " + socket);
		
		socket.connect ();
		
	}

	@Override
	public void send (Sender sender, Recipient [] recipients, String subject, final ApiResource body, JsonObject data,
			ApiStreamSource... attachments) throws MessengerException {
		
		final String uuid = body.owner () + Lang.SLASH + body.name ();
		
		CachedTemplate cTemplate = templates.get (uuid);
		
		if (cTemplate == null || cTemplate.timestamp < body.timestamp ().getTime ()) {
			cTemplate = new CachedTemplate ();
			cTemplate.timestamp = body.timestamp ().getTime ();
			
			TemplateSource source = new TemplateSource () {
				@Override
				public long lastModified () {
					return body.timestamp ().getTime ();
				}
				@Override
				public String filename () {
					return uuid;
				}
				@Override
				public String content () throws IOException {
					
					InputStream is = null;
					
					try {
						is = body.toInput ();
						return IOUtils.toString (is);
					} finally {
						IOUtils.closeQuietly (is);
					}
				}
			};
			
			try {
				cTemplate.template = engine.compile (source, StartDelimitter, EndDelimitter);
			} catch (IOException e) {
				throw new MessengerException (e.getMessage (), e);
			}
			
			templates.put (uuid, cTemplate);
			
		}
		
		StringWriter sw = new StringWriter ();
		
		try {
			cTemplate.template.apply (data, sw);
		} catch (Exception e) {
			throw new MessengerException (e.getMessage (), e);
		}
		
		send (sender, recipients, subject, sw.toString (), attachments);
		
	}
	
	
	class CachedTemplate {
		Template 	template;
		long		timestamp;
	}


	@Override
	public void finish (boolean withError) {
		
	}

	@Override
	public void recycle () {
	}
	
	private void emit (Socket socket, Sender sender, String event, Object message) {
		tracer.log (Level.Info, "Send/Emit Message " + message);
		socket.emit (event, new Object [] { message }, new Ack () {
			@Override
			public void call (Object... args) {
				// disconnect
				socket.disconnect ();
				fireCallback (sender, Sender.Callbacks.Ack, GenericMessage, args);
			}
		});
	}
	
	private void fireCallback (Sender sender, String callback, JsonObject genericMessage, Object [] args) {
		Callback ccb = sender.callback (callback);
		if (ccb == null) {
			return;
		}
		JsonObject message = genericMessage;
		if (args != null && args.length > 0 && args [0] != null) {
			if (args [0] instanceof JSONObject) {
				JSONObject oMessage = (JSONObject)args [0];
				try {
					message = MessageUtils.toJson (oMessage);
				} catch (JSONException e) {
					message = (JsonObject)new JsonObject ().set ("status", "error").set ("reason", e.getMessage ());
				}
			} else {
				message = (JsonObject)new JsonObject ().set ("status", "success").set ("data", String.valueOf (args [0]));
			}
		}
		ccb.process (message);
	}
	
}
